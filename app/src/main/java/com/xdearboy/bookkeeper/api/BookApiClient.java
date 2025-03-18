package com.xdearboy.bookkeeper.api;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.collection.LruCache;

import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.util.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Клиент для работы с API книг Google Books
 */
public class BookApiClient {
    
    private static final String TAG = "BookApiClient";
    private static final int CACHE_SIZE = 50; // Размер кэша (количество запросов)
    
    private final BookApiService apiService;
    private final LruCache<String, List<Book>> searchCache;
    private final Executor executor;
    private final SimpleDateFormat dateFormat;
    
    public interface BooksCallback {
        void onBooksReceived(List<Book> books);
    }
    
    public BookApiClient() {
        // Настраиваем OkHttpClient с таймаутами
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.SECONDS)
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.GOOGLE_BOOKS_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        apiService = retrofit.create(BookApiService.class);
        searchCache = new LruCache<>(CACHE_SIZE);
        executor = Executors.newFixedThreadPool(4); // Пул потоков для обработки данных
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }
    
    /**
     * Загружает книги из API
     * @param callback Callback для обработки результата
     */
    public void fetchBooks(BooksCallback callback) {
        // Если API ключ не указан или пустой, используем тестовые книги
        if (Constants.GOOGLE_BOOKS_API_KEY == null || Constants.GOOGLE_BOOKS_API_KEY.isEmpty()) {
            Log.w(TAG, "API ключ не указан. Используем тестовые книги для fetchBooks.");
            List<Book> allTestBooks = new ArrayList<>();
            
            // Генерируем книги разных категорий
            String[] categories = {
                "фантастика", "детектив", "роман", "история", "биография", 
                "программирование", "психология", "философия", "наука", "искусство",
                "классика", "поэзия", "приключения", "фэнтези", "триллер"
            };
            
            for (String category : categories) {
                generateTestBooks(category, books -> {
                    synchronized (allTestBooks) {
                        allTestBooks.addAll(books);
                        
                        // Когда собрали достаточно книг, возвращаем результат
                        if (allTestBooks.size() >= categories.length * 3) {
                            Log.d(TAG, "Сгенерировано тестовых книг для fetchBooks: " + allTestBooks.size());
                            callback.onBooksReceived(allTestBooks);
                        }
                    }
                });
            }
            
            return;
        }
        
        // Создаем список запросов для получения разнообразных книг
        String[] queries = {
            "фантастика", "детектив", "роман", "история", "биография", 
            "программирование", "психология", "философия", "наука", "искусство",
            "классика", "поэзия", "приключения", "фэнтези", "триллер"
        };
        int maxResults = 40; // Увеличиваем количество книг для каждого запроса
        
        List<Book> allBooks = new ArrayList<>();
        final int[] completedQueries = {0}; // Используем массив для доступа из лямбда-выражения
        final boolean[] apiError = {false}; // Флаг ошибки API
        
        Log.d(TAG, "Начинаем загрузку книг из API, категорий: " + queries.length);
        
        for (String query : queries) {
            // Очищаем кэш перед каждым запросом
            clearCache();
            
            searchBooks(query, maxResults, books -> {
                if (books != null) {
                    Log.d(TAG, "Получено книг из категории '" + query + "': " + books.size());
                    
                    // Добавляем книги, избегая дубликатов
                    synchronized (allBooks) {
                        for (Book book : books) {
                            boolean isDuplicate = false;
                            for (Book existingBook : allBooks) {
                                if (existingBook.getId().equals(book.getId()) || 
                                    (existingBook.getTitle().equals(book.getTitle()) && 
                                     existingBook.getAuthor().equals(book.getAuthor()))) {
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            
                            if (!isDuplicate) {
                                allBooks.add(book);
                            }
                        }
                    }
                }
                
                completedQueries[0]++;
                Log.d(TAG, "Завершено запросов: " + completedQueries[0] + " из " + queries.length);
                
                // Когда все запросы выполнены, возвращаем результат
                if (completedQueries[0] == queries.length) {
                    // Если возникла ошибка API и нет результатов, используем тестовые книги
                    if (apiError[0] && allBooks.isEmpty()) {
                        Log.w(TAG, "Ошибка API и нет результатов в fetchBooks. Используем тестовые книги.");
                        List<Book> allTestBooks = new ArrayList<>();
                        
                        // Генерируем книги разных категорий
                        for (String category : queries) {
                            generateTestBooks(category, testBooks -> {
                                synchronized (allTestBooks) {
                                    allTestBooks.addAll(testBooks);
                                    
                                    // Когда собрали достаточно книг, возвращаем результат
                                    if (allTestBooks.size() >= queries.length * 3) {
                                        Log.d(TAG, "Сгенерировано тестовых книг для fetchBooks: " + allTestBooks.size());
                                        callback.onBooksReceived(allTestBooks);
                                    }
                                }
                            });
                        }
                        return;
                    }
                    
                    Log.d(TAG, "Загружено всего книг: " + allBooks.size());
                    callback.onBooksReceived(allBooks);
                }
            });
        }
    }
    
    /**
     * Поиск книг с использованием LiveData с улучшенной точностью
     * @param query Поисковый запрос
     * @param maxResults Максимальное количество результатов
     * @return LiveData со списком книг
     */
    public LiveData<List<Book>> searchBooksLiveData(String query, int maxResults) {
        MutableLiveData<List<Book>> booksLiveData = new MutableLiveData<>();
        
        // Проверяем, что запрос не пустой
        if (query == null || query.trim().isEmpty()) {
            booksLiveData.setValue(new ArrayList<>());
            return booksLiveData;
        }
        
        // Нормализуем запрос
        final String normalizedQuery = query.trim();
        
        // Проверяем кэш
        String cacheKey = normalizedQuery + "_" + maxResults;
        List<Book> cachedBooks = searchCache.get(cacheKey);
        if (cachedBooks != null) {
            booksLiveData.setValue(cachedBooks);
            return booksLiveData;
        }
        
        // Если API ключ не указан или пустой, используем тестовые книги
        if (Constants.GOOGLE_BOOKS_API_KEY == null || Constants.GOOGLE_BOOKS_API_KEY.isEmpty()) {
            Log.w(TAG, "API ключ не указан. Используем тестовые книги для LiveData.");
            List<Book> testBooks = new ArrayList<>();
            generateTestBooks(normalizedQuery, books -> {
                testBooks.addAll(books);
                booksLiveData.postValue(testBooks);
            });
            return booksLiveData;
        }
        
        // Выполняем запрос в фоновом потоке
        executor.execute(() -> {
            try {
                Log.d(TAG, "Выполняем поиск по запросу: " + normalizedQuery);
                
                // Создаем запрос с параметрами для более точного поиска
                Call<BookApiResponse> call;
                
                // Если запрос содержит пробелы, используем точный поиск с кавычками
                if (normalizedQuery.contains(" ")) {
                    // Для запросов с пробелами пробуем точный поиск
                    call = apiService.searchBooksByLanguage("\"" + normalizedQuery + "\"", maxResults, "ru", Constants.GOOGLE_BOOKS_API_KEY);
                } else {
                    // Для коротких запросов без пробелов используем обычный поиск
                    call = apiService.searchBooksByLanguage(normalizedQuery, maxResults, "ru", Constants.GOOGLE_BOOKS_API_KEY);
                }
                
                Response<BookApiResponse> response = call.execute();
                
                List<Book> allBooks = new ArrayList<>();
                boolean apiError = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Book> books = convertApiResponseToBooks(response.body());
                    allBooks.addAll(books);
                    Log.d(TAG, "Получено книг от API (первый запрос): " + books.size());
                    
                    // Если результатов мало, делаем дополнительный запрос без кавычек
                    if (books.size() < 5 && normalizedQuery.contains(" ")) {
                        try {
                            // Делаем паузу, чтобы избежать ошибки 429
                            Thread.sleep(500);
                            
                            // Второй запрос без кавычек
                            Call<BookApiResponse> secondCall = apiService.searchBooksByLanguage(normalizedQuery, maxResults, "ru", Constants.GOOGLE_BOOKS_API_KEY);
                            Response<BookApiResponse> secondResponse = secondCall.execute();
                            
                            if (secondResponse.isSuccessful() && secondResponse.body() != null) {
                                List<Book> additionalBooks = convertApiResponseToBooks(secondResponse.body());
                                Log.d(TAG, "Получено книг от API (второй запрос): " + additionalBooks.size());
                                
                                // Добавляем книги, избегая дубликатов
                                for (Book book : additionalBooks) {
                                    boolean isDuplicate = false;
                                    for (Book existingBook : allBooks) {
                                        if (existingBook.getId().equals(book.getId()) || 
                                            (existingBook.getTitle().equals(book.getTitle()) && 
                                             existingBook.getAuthor().equals(book.getAuthor()))) {
                                            isDuplicate = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!isDuplicate) {
                                        allBooks.add(book);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при выполнении второго запроса: " + e.getMessage());
                        }
                    }
                    
                    // Если результатов все еще мало, пробуем поиск с дополнительными ключевыми словами
                    if (allBooks.size() < 5 && normalizedQuery.length() < 20) {
                        try {
                            // Делаем паузу, чтобы избежать ошибки 429
                            Thread.sleep(500);
                            
                            // Третий запрос с дополнительными ключевыми словами
                            String enhancedQuery = normalizedQuery + " книга";
                            Call<BookApiResponse> thirdCall = apiService.searchBooksByLanguage(enhancedQuery, maxResults, "ru", Constants.GOOGLE_BOOKS_API_KEY);
                            Response<BookApiResponse> thirdResponse = thirdCall.execute();
                            
                            if (thirdResponse.isSuccessful() && thirdResponse.body() != null) {
                                List<Book> additionalBooks = convertApiResponseToBooks(thirdResponse.body());
                                Log.d(TAG, "Получено книг от API (третий запрос): " + additionalBooks.size());
                                
                                // Добавляем книги, избегая дубликатов
                                for (Book book : additionalBooks) {
                                    boolean isDuplicate = false;
                                    for (Book existingBook : allBooks) {
                                        if (existingBook.getId().equals(book.getId()) || 
                                            (existingBook.getTitle().equals(book.getTitle()) && 
                                             existingBook.getAuthor().equals(book.getAuthor()))) {
                                            isDuplicate = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!isDuplicate) {
                                        allBooks.add(book);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при выполнении третьего запроса: " + e.getMessage());
                        }
                    }
                } else {
                    int errorCode = response.code();
                    Log.e(TAG, "API error in LiveData: " + errorCode);
                    apiError = true;
                }
                
                // Если возникла ошибка API и нет результатов, используем тестовые книги
                if ((apiError && allBooks.isEmpty()) || allBooks.isEmpty()) {
                    Log.w(TAG, "Ошибка API или нет результатов в LiveData. Используем тестовые книги.");
                    generateTestBooks(normalizedQuery, books -> {
                        // Сохраняем в кэш
                        searchCache.put(cacheKey, books);
                        // Публикуем результат в главном потоке
                        booksLiveData.postValue(books);
                    });
                    return;
                }
                
                // Сортируем результаты по релевантности
                sortBooksByRelevance(allBooks, normalizedQuery);
                
                // Сохраняем в кэш
                searchCache.put(cacheKey, allBooks);
                // Публикуем результат в главном потоке
                booksLiveData.postValue(allBooks);
                
                Log.d(TAG, "Найдено книг по запросу '" + normalizedQuery + "' в LiveData: " + allBooks.size());
            } catch (Exception e) {
                Log.e(TAG, "API call failed in LiveData", e);
                // В случае ошибки используем тестовые книги
                generateTestBooks(normalizedQuery, books -> {
                    // Сохраняем в кэш
                    searchCache.put(cacheKey, books);
                    // Публикуем результат в главном потоке
                    booksLiveData.postValue(books);
                });
            }
        });
        
        return booksLiveData;
    }
    
    /**
     * Сортирует книги по релевантности к запросу
     * @param books Список книг для сортировки
     * @param query Поисковый запрос
     */
    private void sortBooksByRelevance(List<Book> books, String query) {
        if (books == null || books.isEmpty() || query == null || query.isEmpty()) {
            return;
        }
        
        final String lowerQuery = query.toLowerCase();
        
        Collections.sort(books, (book1, book2) -> {
            int relevance1 = calculateRelevance(book1, lowerQuery);
            int relevance2 = calculateRelevance(book2, lowerQuery);
            
            // Сортируем по убыванию релевантности
            return Integer.compare(relevance2, relevance1);
        });
    }
    
    /**
     * Вычисляет релевантность книги к запросу
     * @param book Книга
     * @param lowerQuery Поисковый запрос в нижнем регистре
     * @return Оценка релевантности (чем выше, тем более релевантна)
     */
    private int calculateRelevance(Book book, String lowerQuery) {
        int relevance = 0;
        
        // Проверяем название
        String title = book.getTitle();
        if (title != null) {
            String lowerTitle = title.toLowerCase();
            
            // Точное совпадение названия
            if (lowerTitle.equals(lowerQuery)) {
                relevance += 100;
            }
            // Название начинается с запроса
            else if (lowerTitle.startsWith(lowerQuery)) {
                relevance += 50;
            }
            // Запрос содержится в названии
            else if (lowerTitle.contains(lowerQuery)) {
                relevance += 30;
            }
            // Название содержит слова из запроса
            else {
                String[] queryWords = lowerQuery.split("\\s+");
                for (String word : queryWords) {
                    if (word.length() > 2 && lowerTitle.contains(word)) {
                        relevance += 10;
                    }
                }
            }
        }
        
        // Проверяем автора
        String author = book.getAuthor();
        if (author != null) {
            String lowerAuthor = author.toLowerCase();
            
            // Точное совпадение автора
            if (lowerAuthor.equals(lowerQuery)) {
                relevance += 80;
            }
            // Автор начинается с запроса
            else if (lowerAuthor.startsWith(lowerQuery)) {
                relevance += 40;
            }
            // Запрос содержится в имени автора
            else if (lowerAuthor.contains(lowerQuery)) {
                relevance += 20;
            }
            // Имя автора содержит слова из запроса
            else {
                String[] queryWords = lowerQuery.split("\\s+");
                for (String word : queryWords) {
                    if (word.length() > 2 && lowerAuthor.contains(word)) {
                        relevance += 5;
                    }
                }
            }
        }
        
        return relevance;
    }
    
    /**
     * Поиск книг с использованием callback
     * @param query Поисковый запрос
     * @param maxResults Максимальное количество результатов
     * @param callback Callback для обработки результата
     */
    public void searchBooks(String query, int maxResults, BooksCallback callback) {
        // Проверяем кэш
        String cacheKey = query + "_" + maxResults;
        List<Book> cachedBooks = searchCache.get(cacheKey);
        if (cachedBooks != null) {
            callback.onBooksReceived(cachedBooks);
            return;
        }
        
        // Если API ключ не указан или пустой, используем тестовые книги
        if (Constants.GOOGLE_BOOKS_API_KEY == null || Constants.GOOGLE_BOOKS_API_KEY.isEmpty()) {
            Log.w(TAG, "API ключ не указан. Используем тестовые книги.");
            generateTestBooks(query, callback);
            return;
        }
        
        // Создаем список запросов для получения разнообразных книг по запросу
        // Используем только основной запрос и 2-3 дополнительных для снижения нагрузки на API
        List<String> enhancedQueries = new ArrayList<>();
        enhancedQueries.add(query); // Добавляем оригинальный запрос
        
        // Добавляем только несколько дополнительных запросов
        if (query.length() > 3) { // Если запрос достаточно длинный
            enhancedQueries.add(query + " популярное");
            
            // Определяем категорию на основе запроса
            String category = determineCategory(query);
            if (category != null && !query.toLowerCase().contains(category.toLowerCase())) {
                enhancedQueries.add(query + " " + category);
            }
        }
        
        final List<Book> allBooks = new ArrayList<>();
        final int[] completedQueries = {0};
        final boolean[] apiError = {false}; // Флаг ошибки API
        
        Log.d(TAG, "Начинаем поиск книг с " + enhancedQueries.size() + " запросами");
        
        // Вычисляем количество результатов на запрос
        int resultsPerQuery = Math.max(1, maxResults / enhancedQueries.size());
        
        for (String enhancedQuery : enhancedQueries) {
            // URL-кодируем запрос для безопасности
            String encodedQuery = enhancedQuery;
            try {
                encodedQuery = java.net.URLEncoder.encode(enhancedQuery, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                Log.e(TAG, "Ошибка кодирования запроса: " + e.getMessage());
            }
            
            // Используем поиск с фильтрацией по русскому языку
            apiService.searchBooksByLanguage(encodedQuery, resultsPerQuery, "ru", Constants.GOOGLE_BOOKS_API_KEY)
                .enqueue(new Callback<BookApiResponse>() {
                    @Override
                    public void onResponse(Call<BookApiResponse> call, Response<BookApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Обрабатываем данные в фоновом потоке
                            executor.execute(() -> {
                                List<Book> books = convertApiResponseToBooks(response.body());
                                
                                // Добавляем книги, избегая дубликатов
                                synchronized (allBooks) {
                                    for (Book book : books) {
                                        boolean isDuplicate = false;
                                        for (Book existingBook : allBooks) {
                                            if (existingBook.getId().equals(book.getId()) || 
                                                (existingBook.getTitle().equals(book.getTitle()) && 
                                                 existingBook.getAuthor().equals(book.getAuthor()))) {
                                                isDuplicate = true;
                                                break;
                                            }
                                        }
                                        
                                        if (!isDuplicate) {
                                            allBooks.add(book);
                                        }
                                    }
                                }
                                
                                completedQueries[0]++;
                                Log.d(TAG, "Завершен запрос '" + enhancedQuery + "', найдено книг: " + books.size() + 
                                      ", всего запросов: " + completedQueries[0] + "/" + enhancedQueries.size());
                                
                                // Когда все запросы выполнены, возвращаем результат
                                if (completedQueries[0] == enhancedQueries.size()) {
                                    // Сортируем книги по релевантности
                                    sortBooksByRelevance(allBooks, query);
                                    
                                    // Сохраняем результаты в кэш
                                    searchCache.put(cacheKey, allBooks);
                                    
                                    // Возвращаем результат
                                    Log.d(TAG, "Поиск завершен, всего найдено книг: " + allBooks.size());
                                    callback.onBooksReceived(allBooks);
                                }
                            });
                        } else {
                            apiError[0] = true;
                            completedQueries[0]++;
                            
                            int errorCode = response.code();
                            Log.e(TAG, "Ошибка API при запросе '" + enhancedQuery + "': " + errorCode);
                            
                            // Подробное логирование ошибки
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "API error body: " + errorBody);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка при чтении тела ошибки: " + e.getMessage());
                            }
                            
                            // Когда все запросы выполнены, возвращаем результат
                            if (completedQueries[0] == enhancedQueries.size()) {
                                // Если нет результатов, используем тестовые книги
                                if (allBooks.isEmpty()) {
                                    Log.w(TAG, "Нет результатов поиска. Используем тестовые книги.");
                                    generateTestBooks(query, callback);
                                } else {
                                    // Сортируем книги по релевантности
                                    sortBooksByRelevance(allBooks, query);
                                    
                                    // Сохраняем результаты в кэш
                                    searchCache.put(cacheKey, allBooks);
                                    
                                    // Возвращаем результат
                                    Log.d(TAG, "Поиск завершен с ошибками, найдено книг: " + allBooks.size());
                                    callback.onBooksReceived(allBooks);
                                }
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<BookApiResponse> call, Throwable t) {
                        apiError[0] = true;
                        completedQueries[0]++;
                        
                        Log.e(TAG, "Ошибка сети при запросе '" + enhancedQuery + "': " + t.getMessage());
                        
                        // Когда все запросы выполнены, возвращаем результат
                        if (completedQueries[0] == enhancedQueries.size()) {
                            // Если нет результатов, используем тестовые книги
                            if (allBooks.isEmpty()) {
                                Log.w(TAG, "Нет результатов поиска из-за ошибок сети. Используем тестовые книги.");
                                generateTestBooks(query, callback);
                            } else {
                                // Сортируем книги по релевантности
                                sortBooksByRelevance(allBooks, query);
                                
                                // Сохраняем результаты в кэш
                                searchCache.put(cacheKey, allBooks);
                                
                                // Возвращаем результат
                                Log.d(TAG, "Поиск завершен с ошибками сети, найдено книг: " + allBooks.size());
                                callback.onBooksReceived(allBooks);
                            }
                        }
                    }
                });
        }
    }
    
    /**
     * Определяет категорию на основе запроса
     * @param query Поисковый запрос
     * @return Категория или null, если не удалось определить
     */
    private String determineCategory(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Простая эвристика для определения категории
        if (lowerQuery.contains("фантаст") || lowerQuery.contains("sci-fi") || lowerQuery.contains("научн")) {
            return "фантастика";
        } else if (lowerQuery.contains("детектив") || lowerQuery.contains("крими") || lowerQuery.contains("загадк")) {
            return "детектив";
        } else if (lowerQuery.contains("роман") || lowerQuery.contains("любов") || lowerQuery.contains("отношен")) {
            return "роман";
        } else if (lowerQuery.contains("истор") || lowerQuery.contains("войн") || lowerQuery.contains("древн")) {
            return "история";
        } else if (lowerQuery.contains("биограф") || lowerQuery.contains("мемуар") || lowerQuery.contains("автобиограф")) {
            return "биография";
        } else if (lowerQuery.contains("програм") || lowerQuery.contains("код") || lowerQuery.contains("компьютер")) {
            return "программирование";
        } else if (lowerQuery.contains("психолог") || lowerQuery.contains("самопомощ") || lowerQuery.contains("развит")) {
            return "психология";
        } else if (lowerQuery.contains("филос") || lowerQuery.contains("мысл") || lowerQuery.contains("бытие")) {
            return "философия";
        } else if (lowerQuery.contains("наук") || lowerQuery.contains("физик") || lowerQuery.contains("биолог")) {
            return "наука";
        } else if (lowerQuery.contains("искусств") || lowerQuery.contains("живопис") || lowerQuery.contains("музык")) {
            return "искусство";
        }
        
        // Если не удалось определить категорию, возвращаем null
        return null;
    }
    
    /**
     * Загружает страницу книг по запросу
     * @param query Поисковый запрос
     * @param page Номер страницы (начиная с 0)
     * @param callback Callback для обработки результата
     */
    public void loadBooksPage(String query, int page, BooksCallback callback) {
        int startIndex = page * Constants.PAGE_SIZE;
        String cacheKey = query + "_page_" + page;
        
        // Проверяем кэш
        List<Book> cachedBooks = searchCache.get(cacheKey);
        if (cachedBooks != null) {
            Log.d(TAG, "Используем кэшированные книги для страницы " + page);
            callback.onBooksReceived(cachedBooks);
            return;
        }
        
        // Если API ключ не указан или пустой, используем тестовые книги
        if (Constants.GOOGLE_BOOKS_API_KEY == null || Constants.GOOGLE_BOOKS_API_KEY.isEmpty()) {
            Log.w(TAG, "API ключ не указан. Используем тестовые книги.");
            generateTestBooks(query, callback);
            return;
        }
        
        // URL-кодируем запрос для безопасности
        String encodedQuery = query;
        try {
            encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e(TAG, "Ошибка кодирования запроса: " + e.getMessage());
        }
        
        Log.d(TAG, "Загрузка страницы " + page + " для запроса: " + query);
        
        // Делаем только один запрос к API вместо трех, чтобы избежать ошибки 429
        apiService.searchBooksWithPaginationAndLanguage(
                encodedQuery, 
                Math.min(Constants.PAGE_SIZE, 10), // Ограничиваем размер страницы для снижения нагрузки
                startIndex,
                "ru",
                Constants.GOOGLE_BOOKS_API_KEY)
                .enqueue(new Callback<BookApiResponse>() {
                    @Override
                    public void onResponse(Call<BookApiResponse> call, Response<BookApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Book> books = convertApiResponseToBooks(response.body());
                            
                            // Сохраняем в кэш
                            searchCache.put(cacheKey, books);
                            
                            // Возвращаем результат
                            Log.d(TAG, "Получено книг для страницы " + page + ": " + books.size());
                            callback.onBooksReceived(books);
                        } else {
                            int errorCode = response.code();
                            Log.e(TAG, "API error: " + errorCode);
                            
                            // Подробное логирование ошибки
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "API error body: " + errorBody);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка при чтении тела ошибки: " + e.getMessage());
                            }
                            
                            // В случае ошибки используем тестовые книги
                            generateTestBooks(query, callback);
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<BookApiResponse> call, Throwable t) {
                        Log.e(TAG, "Ошибка сети при запросе '" + query + "': " + t.getMessage());
                        
                        // В случае ошибки используем тестовые книги
                        generateTestBooks(query, callback);
                    }
                });
    }
    
    /**
     * Создает callback для запроса книг с обработкой ошибок
     */
    private Callback<BookApiResponse> createCallbackWithErrorHandling(String query, List<Book> allBooks, int[] completedQueries, 
                                                    int totalQueries, String cacheKey, BooksCallback callback, boolean[] apiError) {
        return new Callback<BookApiResponse>() {
            @Override
            public void onResponse(Call<BookApiResponse> call, Response<BookApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Обрабатываем данные в фоновом потоке
                    executor.execute(() -> {
                        List<Book> books = convertApiResponseToBooks(response.body());
                        Log.d(TAG, "Получено книг для запроса '" + query + "': " + books.size());
                        
                        // Добавляем книги, избегая дубликатов
                        synchronized (allBooks) {
                            for (Book book : books) {
                                boolean isDuplicate = false;
                                for (Book existingBook : allBooks) {
                                    if (existingBook.getId().equals(book.getId()) || 
                                        (existingBook.getTitle().equals(book.getTitle()) && 
                                         existingBook.getAuthor().equals(book.getAuthor()))) {
                                        isDuplicate = true;
                                        break;
                                    }
                                }
                                
                                if (!isDuplicate) {
                                    allBooks.add(book);
                                }
                            }
                            
                            completedQueries[0]++;
                            
                            // Когда все запросы выполнены, возвращаем результат
                            if (completedQueries[0] == totalQueries) {
                                // Если возникла ошибка API и нет результатов, используем тестовые книги
                                if (apiError[0] && allBooks.isEmpty()) {
                                    Log.w(TAG, "Ошибка API и нет результатов. Используем тестовые книги.");
                                    generateTestBooks(query, callback);
                                    return;
                                }
                                
                                // Сохраняем в кэш
                                searchCache.put(cacheKey, allBooks);
                                // Возвращаем результат
                                Log.d(TAG, "Всего получено книг для страницы: " + allBooks.size());
                                callback.onBooksReceived(allBooks);
                            }
                        }
                    });
                } else {
                    int errorCode = response.code();
                    Log.e(TAG, "API error: " + errorCode);
                    
                    // Если ошибка 403, устанавливаем флаг ошибки API
                    if (errorCode == 403) {
                        apiError[0] = true;
                    }
                    
                    synchronized (allBooks) {
                        completedQueries[0]++;
                        
                        // Когда все запросы выполнены, возвращаем результат
                        if (completedQueries[0] == totalQueries) {
                            // Если возникла ошибка API и нет результатов, используем тестовые книги
                            if (apiError[0] && allBooks.isEmpty()) {
                                Log.w(TAG, "Ошибка API и нет результатов. Используем тестовые книги.");
                                generateTestBooks(query, callback);
                                return;
                            }
                            
                            // Сохраняем в кэш
                            searchCache.put(cacheKey, allBooks);
                            // Возвращаем результат
                            callback.onBooksReceived(allBooks);
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<BookApiResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                
                // Устанавливаем флаг ошибки API
                apiError[0] = true;
                
                synchronized (allBooks) {
                    completedQueries[0]++;
                    
                    // Когда все запросы выполнены, возвращаем результат
                    if (completedQueries[0] == totalQueries) {
                        // Если возникла ошибка API и нет результатов, используем тестовые книги
                        if (apiError[0] && allBooks.isEmpty()) {
                            Log.w(TAG, "Ошибка API и нет результатов. Используем тестовые книги.");
                            generateTestBooks(query, callback);
                            return;
                        }
                        
                        // Сохраняем в кэш
                        searchCache.put(cacheKey, allBooks);
                        // Возвращаем результат
                        callback.onBooksReceived(allBooks);
                    }
                }
            }
        };
    }
    
    /**
     * Генерирует тестовые книги для запроса с улучшенной релевантностью
     */
    private void generateTestBooks(String query, BooksCallback callback) {
        List<Book> testBooks = new ArrayList<>();
        
        // Нормализуем запрос
        final String normalizedQuery = query.trim().toLowerCase();
        
        // Специальные книги для конкретных запросов
        if (normalizedQuery.contains("шолохов")) {
            testBooks.add(createTestBook("test_sh1", "Тихий Дон", "Михаил Шолохов", "Роман", 
                    "Роман-эпопея Михаила Шолохова в четырёх томах. Описывает жизнь донских казаков в начале XX века.", 
                    "https://covers.openlibrary.org/b/id/8231584-L.jpg"));
            testBooks.add(createTestBook("test_sh2", "Поднятая целина", "Михаил Шолохов", "Роман", 
                    "Роман Михаила Шолохова о коллективизации на Дону и партийной работе в деревне.", 
                    "https://covers.openlibrary.org/b/id/8231585-L.jpg"));
            testBooks.add(createTestBook("test_sh3", "Судьба человека", "Михаил Шолохов", "Рассказ", 
                    "Рассказ Михаила Шолохова, написанный в 1956 году и повествующий о судьбе советского солдата.", 
                    "https://covers.openlibrary.org/b/id/8231586-L.jpg"));
        } else if (normalizedQuery.contains("булгаков")) {
            testBooks.add(createTestBook("test_b1", "Мастер и Маргарита", "Михаил Булгаков", "Роман", 
                    "Роман Михаила Булгакова, работа над которым началась в конце 1920-х годов и продолжалась вплоть до смерти писателя.", 
                    "https://covers.openlibrary.org/b/id/12700653-L.jpg"));
            testBooks.add(createTestBook("test_b2", "Собачье сердце", "Михаил Булгаков", "Повесть", 
                    "Повесть Михаила Булгакова, написанная в 1925 году.", 
                    "https://covers.openlibrary.org/b/id/8231587-L.jpg"));
            testBooks.add(createTestBook("test_b3", "Белая гвардия", "Михаил Булгаков", "Роман", 
                    "Роман Михаила Булгакова, описывающий события Гражданской войны в Киеве в конце 1918 года.", 
                    "https://covers.openlibrary.org/b/id/8231588-L.jpg"));
        } else if (normalizedQuery.contains("толстой")) {
            testBooks.add(createTestBook("test_t1", "Война и мир", "Лев Толстой", "Роман", 
                    "Роман-эпопея о России во время наполеоновских войн.", 
                    "https://covers.openlibrary.org/b/id/8231583-L.jpg"));
            testBooks.add(createTestBook("test_t2", "Анна Каренина", "Лев Толстой", "Роман", 
                    "Роман о трагической любви замужней дамы Анны Карениной и офицера Вронского.", 
                    "https://covers.openlibrary.org/b/id/8244865-L.jpg"));
            testBooks.add(createTestBook("test_t3", "Воскресение", "Лев Толстой", "Роман", 
                    "Последний роман Льва Толстого, опубликованный в 1899 году.", 
                    "https://covers.openlibrary.org/b/id/8231589-L.jpg"));
        } else if (normalizedQuery.contains("достоевский")) {
            testBooks.add(createTestBook("test_d1", "Преступление и наказание", "Федор Достоевский", "Роман", 
                    "Социально-психологический роман о студенте Родионе Раскольникове.", 
                    "https://covers.openlibrary.org/b/id/12890055-L.jpg"));
            testBooks.add(createTestBook("test_d2", "Идиот", "Федор Достоевский", "Роман", 
                    "Роман Фёдора Достоевского, впервые опубликованный в номерах журнала «Русский вестник» за 1868 год.", 
                    "https://covers.openlibrary.org/b/id/8244866-L.jpg"));
            testBooks.add(createTestBook("test_d3", "Братья Карамазовы", "Федор Достоевский", "Роман", 
                    "Последний роман Фёдора Достоевского, который автор писал два года.", 
                    "https://covers.openlibrary.org/b/id/8231590-L.jpg"));
        } else if (normalizedQuery.contains("пушкин")) {
            testBooks.add(createTestBook("test_p1", "Евгений Онегин", "Александр Пушкин", "Поэзия", 
                    "Роман в стихах Александра Пушкина, написанный в 1823—1831 годах.", 
                    "https://covers.openlibrary.org/b/id/6424280-L.jpg"));
            testBooks.add(createTestBook("test_p2", "Капитанская дочка", "Александр Пушкин", "Повесть", 
                    "Исторический роман Александра Пушкина, действие которого происходит во время восстания Емельяна Пугачёва.", 
                    "https://covers.openlibrary.org/b/id/8231591-L.jpg"));
            testBooks.add(createTestBook("test_p3", "Борис Годунов", "Александр Пушкин", "Драма", 
                    "Историческая драма Александра Пушкина, написанная в 1825 году.", 
                    "https://covers.openlibrary.org/b/id/8231592-L.jpg"));
        } else if (normalizedQuery.contains("юшка") || normalizedQuery.contains("платонов")) {
            testBooks.add(createTestBook("test_pl1", "Юшка", "Андрей Платонов", "Рассказ", 
                    "Рассказ Андрея Платонова о добром и безответном человеке, которого все обижали.", 
                    "https://covers.openlibrary.org/b/id/8231593-L.jpg"));
            testBooks.add(createTestBook("test_pl2", "Котлован", "Андрей Платонов", "Повесть", 
                    "Повесть Андрея Платонова, написанная в 1930 году.", 
                    "https://covers.openlibrary.org/b/id/8231594-L.jpg"));
            testBooks.add(createTestBook("test_pl3", "Чевенгур", "Андрей Платонов", "Роман", 
                    "Роман Андрея Платонова, написанный в 1926—1929 годах.", 
                    "https://covers.openlibrary.org/b/id/8231595-L.jpg"));
        } else {
            // Базовые категории книг
            String[] categories = {
                "фантастика", "детектив", "роман", "история", "биография", 
                "программирование", "психология", "философия", "наука", "искусство",
                "классика", "поэзия", "приключения", "фэнтези", "триллер"
            };
            
            // Определяем категорию на основе запроса
            String category = normalizedQuery;
            boolean categoryFound = false;
            
            for (String cat : categories) {
                if (category.contains(cat)) {
                    category = cat;
                    categoryFound = true;
                    break;
                }
            }
            
            if (!categoryFound) {
                category = categories[(int) (Math.random() * categories.length)];
            }
            
            // Генерируем книги в зависимости от категории
            switch (category) {
                case "фантастика":
                    testBooks.add(createTestBook("test_f1", "Дюна", "Фрэнк Герберт", "Фантастика", 
                            "Эпическая научно-фантастическая сага о далеком будущем человечества.", 
                            "https://covers.openlibrary.org/b/id/12645114-L.jpg"));
                    testBooks.add(createTestBook("test_f2", "Нейромант", "Уильям Гибсон", "Фантастика", 
                            "Классический киберпанк-роман о хакере, нанятом для выполнения последней работы.", 
                            "https://covers.openlibrary.org/b/id/12890468-L.jpg"));
                    testBooks.add(createTestBook("test_f3", "Гиперион", "Дэн Симмонс", "Фантастика", 
                            "Научно-фантастический роман о группе паломников, отправляющихся на планету Гиперион.", 
                            "https://covers.openlibrary.org/b/id/10110415-L.jpg"));
                    break;
                case "детектив":
                    testBooks.add(createTestBook("test_d1", "Убийство в Восточном экспрессе", "Агата Кристи", "Детектив", 
                            "Классический детективный роман о расследовании убийства в поезде.", 
                            "https://covers.openlibrary.org/b/id/8231990-L.jpg"));
                    testBooks.add(createTestBook("test_d2", "Шерлок Холмс: Собрание сочинений", "Артур Конан Дойл", "Детектив", 
                            "Сборник рассказов о знаменитом детективе Шерлоке Холмсе.", 
                            "https://covers.openlibrary.org/b/id/12903634-L.jpg"));
                    testBooks.add(createTestBook("test_d3", "Девушка с татуировкой дракона", "Стиг Ларссон", "Детектив", 
                            "Первая книга из серии 'Миллениум' о журналисте Микаэле Блумквисте и хакере Лисбет Саландер.", 
                            "https://covers.openlibrary.org/b/id/10389354-L.jpg"));
                    break;
                case "роман":
                    testBooks.add(createTestBook("test_r1", "Война и мир", "Лев Толстой", "Роман", 
                            "Роман-эпопея о России во время наполеоновских войн.", 
                            "https://covers.openlibrary.org/b/id/8231583-L.jpg"));
                    testBooks.add(createTestBook("test_r2", "Анна Каренина", "Лев Толстой", "Роман", 
                            "Роман о трагической любви замужней дамы Анны Карениной и офицера Вронского.", 
                            "https://covers.openlibrary.org/b/id/8244865-L.jpg"));
                    testBooks.add(createTestBook("test_r3", "Преступление и наказание", "Федор Достоевский", "Роман", 
                            "Социально-психологический роман о студенте Родионе Раскольникове.", 
                            "https://covers.openlibrary.org/b/id/12890055-L.jpg"));
                    break;
                default:
                    testBooks.add(createTestBook("test_1", "Война и мир", "Лев Толстой", "Роман", 
                            "Роман-эпопея Льва Николаевича Толстого, описывающий русское общество в эпоху войн против Наполеона в 1805—1812 годах.", 
                            "https://covers.openlibrary.org/b/id/8231583-L.jpg"));
                    testBooks.add(createTestBook("test_2", "Преступление и наказание", "Федор Достоевский", "Роман", 
                            "Социально-психологический и социально-философский роман Фёдора Михайловича Достоевского, над которым писатель работал в 1865—1866 годах.", 
                            "https://covers.openlibrary.org/b/id/12890055-L.jpg"));
                    testBooks.add(createTestBook("test_3", "Мастер и Маргарита", "Михаил Булгаков", "Фантастика", 
                            "Роман Михаила Афанасьевича Булгакова, работа над которым началась в конце 1920-х годов и продолжалась вплоть до смерти писателя.", 
                            "https://covers.openlibrary.org/b/id/12700653-L.jpg"));
                    break;
            }
            
            // Добавляем случайные книги для разнообразия
            testBooks.add(createTestBook("test_4", "Анна Каренина", "Лев Толстой", "Роман", 
                    "Роман Льва Толстого о трагической любви замужней дамы Анны Карениной и блестящего офицера Вронского на фоне счастливой семейной жизни дворян Константина Лёвина и Кити Щербацкой.", 
                    "https://covers.openlibrary.org/b/id/8244865-L.jpg"));
            testBooks.add(createTestBook("test_5", "Евгений Онегин", "Александр Пушкин", "Поэзия", 
                    "Роман в стихах Александра Сергеевича Пушкина, написанный в 1823—1831 годах, одно из самых значительных произведений русской словесности.", 
                    "https://covers.openlibrary.org/b/id/6424280-L.jpg"));
            testBooks.add(createTestBook("test_6", "Идиот", "Федор Достоевский", "Роман", 
                    "Роман Фёдора Михайловича Достоевского, впервые опубликованный в номерах журнала «Русский вестник» за 1868 год.", 
                    "https://covers.openlibrary.org/b/id/8244866-L.jpg"));
        }
        
        // Сортируем книги по релевантности к запросу
        sortBooksByRelevance(testBooks, normalizedQuery);
        
        // Сохраняем в кэш
        searchCache.put(query + "_page_0", testBooks);
        
        // Возвращаем результат
        Log.d(TAG, "Сгенерировано тестовых книг: " + testBooks.size());
        callback.onBooksReceived(testBooks);
    }
    
    /**
     * Создает тестовую книгу
     */
    private Book createTestBook(String id, String title, String author, String genre, String description, String coverUrl) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setDescription(description);
        book.setCoverImageUrl(coverUrl);
        book.setBorrowed(false);
        book.setFromApi(false);
        return book;
    }
    
    /**
     * Очистка кэша
     */
    public void clearCache() {
        searchCache.evictAll();
    }
    
    /**
     * Конвертирует ответ API в список книг
     * @param response Ответ API
     * @return Список книг
     */
    private List<Book> convertApiResponseToBooks(BookApiResponse response) {
        List<Book> books = new ArrayList<>();
        
        try {
            if (response == null) {
                Log.e(TAG, "Получен null ответ от API");
                return books;
            }
            
            if (response.getItems() != null) {
                for (BookApiResponse.Item item : response.getItems()) {
                    try {
                        if (item == null) {
                            Log.e(TAG, "Получен null элемент в ответе API");
                            continue;
                        }
                        
                        BookApiResponse.VolumeInfo info = item.getVolumeInfo();
                        if (info == null || info.getTitle() == null) {
                            Log.e(TAG, "Получена null информация о книге или заголовок");
                            continue;
                        }
                        
                        Book book = new Book();
                        book.setId("api_" + (item.getId() != null ? item.getId() : UUID.randomUUID().toString()));
                        book.setTitle(info.getTitle());
                        
                        // Безопасное получение авторов
                        List<String> authors = info.getAuthors();
                        if (authors != null && !authors.isEmpty()) {
                            StringBuilder authorBuilder = new StringBuilder();
                            for (int i = 0; i < authors.size(); i++) {
                                String author = authors.get(i);
                                if (author != null) {
                                    if (i > 0) authorBuilder.append(", ");
                                    authorBuilder.append(author);
                                }
                            }
                            book.setAuthor(authorBuilder.length() > 0 ? authorBuilder.toString() : "Неизвестный автор");
                        } else {
                            book.setAuthor("Неизвестный автор");
                        }
                        
                        // Безопасное получение категорий
                        List<String> categories = info.getCategories();
                        if (categories != null && !categories.isEmpty()) {
                            StringBuilder categoryBuilder = new StringBuilder();
                            for (int i = 0; i < categories.size(); i++) {
                                String category = categories.get(i);
                                if (category != null) {
                                    if (i > 0) categoryBuilder.append(", ");
                                    categoryBuilder.append(category);
                                }
                            }
                            book.setGenre(categoryBuilder.length() > 0 ? categoryBuilder.toString() : "Без категории");
                        } else {
                            book.setGenre("Без категории");
                        }
                        
                        book.setDescription(info.getDescription());
                        
                        // Обработка URL обложки
                        String coverUrl = null;
                        BookApiResponse.ImageLinks imageLinks = info.getImageLinks();
                        if (imageLinks != null) {
                            // Сначала пробуем получить thumbnail
                            coverUrl = imageLinks.getThumbnail();
                            
                            // Если thumbnail отсутствует, пробуем другие варианты
                            if (coverUrl == null) {
                                coverUrl = imageLinks.getSmallThumbnail();
                            }
                            
                            if (coverUrl == null) {
                                coverUrl = imageLinks.getSmall();
                            }
                            
                            if (coverUrl == null) {
                                coverUrl = imageLinks.getMedium();
                            }
                            
                            if (coverUrl == null) {
                                coverUrl = imageLinks.getLarge();
                            }
                            
                            if (coverUrl == null) {
                                coverUrl = imageLinks.getExtraLarge();
                            }
                            
                            // Заменяем http на https для избежания проблем с безопасностью
                            if (coverUrl != null && coverUrl.startsWith("http:")) {
                                coverUrl = coverUrl.replace("http:", "https:");
                            }
                        }
                        
                        // Если обложка не найдена, используем заглушку
                        if (coverUrl == null) {
                            String title = info.getTitle();
                            if (title != null && !title.isEmpty()) {
                                String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "+");
                                coverUrl = "https://via.placeholder.com/128x192.png?text=" + 
                                        safeTitle.substring(0, Math.min(safeTitle.length(), 20));
                            } else {
                                coverUrl = "https://via.placeholder.com/128x192.png?text=Book";
                            }
                        }
                        
                        book.setCoverImageUrl(coverUrl);
                        book.setBorrowed(false);
                        book.setIsbn(getIsbnFromIdentifiers(info.getIndustryIdentifiers()));
                        book.setPageCount(info.getPageCount());
                        book.setPublisher(info.getPublisher());
                        book.setPublishDate(parsePublishedDate(info.getPublishedDate()));
                        book.setLanguage(info.getLanguage());
                        book.setFromApi(true);
                        
                        books.add(book);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке элемента книги: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при конвертации ответа API: " + e.getMessage());
        }
        
        return books;
    }
    
    /**
     * Получает ISBN из списка идентификаторов
     * @param identifiers Список идентификаторов
     * @return ISBN или null
     */
    private String getIsbnFromIdentifiers(List<BookApiResponse.IndustryIdentifier> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return null;
        }
        
        // Сначала ищем ISBN_13
        for (BookApiResponse.IndustryIdentifier identifier : identifiers) {
            if ("ISBN_13".equals(identifier.getType())) {
                return identifier.getIdentifier();
            }
        }
        
        // Если не нашли ISBN_13, берем первый идентификатор
        return identifiers.get(0).getIdentifier();
    }
    
    /**
     * Парсит дату публикации
     * @param dateString Строка с датой
     * @return Дата или null
     */
    private Date parsePublishedDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        try {
            // Пробуем разные форматы даты
            if (dateString.length() == 4) {
                // Только год
                return new Date(Integer.parseInt(dateString) - 1900, 0, 1);
            } else if (dateString.length() == 7) {
                // Год и месяц
                return new SimpleDateFormat("yyyy-MM", Locale.US).parse(dateString);
            } else {
                // Полная дата
                return dateFormat.parse(dateString);
            }
        } catch (ParseException | NumberFormatException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            return null;
        }
    }
} 