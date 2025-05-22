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

public class BookApiClient {
    private static final String TAG = "BookApiClient";
    private static final int CACHE_SIZE = 50;
    private final BookApiService apiService;
    private final LruCache<String, List<Book>> searchCache;
    private final Executor executor;
    private final SimpleDateFormat dateFormat;

    public interface BooksCallback {
        void onBooksReceived(List<Book> books);
    }
    
    public interface OnSearchResultCallback {
        void onSuccess(List<Book> books);
        void onError(String message);
    }

    public BookApiClient() {
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
        executor = Executors.newFixedThreadPool(4);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }
    
    private List<Book> convertApiResponseToBooks(BookApiResponse response) {
        if (response == null || response.getItems() == null) {
            return Collections.emptyList();
        }
        
        List<Book> books = new ArrayList<>();
        for (BookApiResponse.Item item : response.getItems()) {
            if (item.getVolumeInfo() == null) continue;
            
            Book book = new Book();
            book.setId(UUID.randomUUID().toString());
            book.setApiId(item.getId());
            book.setTitle(item.getVolumeInfo().getTitle());
            
            // Set author
            if (item.getVolumeInfo().getAuthors() != null && !item.getVolumeInfo().getAuthors().isEmpty()) {
                book.setAuthor(String.join(", ", item.getVolumeInfo().getAuthors()));
            } else {
                book.setAuthor("Unknown Author");
            }
            
            // Set description
            if (item.getVolumeInfo().getDescription() != null) {
                book.setDescription(item.getVolumeInfo().getDescription());
            } else {
                book.setDescription("");
            }
            
            // Set categories/genre
            if (item.getVolumeInfo().getCategories() != null && !item.getVolumeInfo().getCategories().isEmpty()) {
                book.setGenre(String.join(", ", item.getVolumeInfo().getCategories()));
            } else {
                book.setGenre("General");
            }
            
            // Set publish date
            if (item.getVolumeInfo().getPublishedDate() != null) {
                try {
                    Date publishDate = dateFormat.parse(item.getVolumeInfo().getPublishedDate());
                    book.setPublishDate(publishDate.getTime());
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date: " + e.getMessage());
                }
            }
            
            // Set image URL
            if (item.getVolumeInfo().getImageLinks() != null) {
                if (item.getVolumeInfo().getImageLinks().getThumbnail() != null) {
                    book.setImageUrl(item.getVolumeInfo().getImageLinks().getThumbnail().replace("http:", "https:"));
                } else if (item.getVolumeInfo().getImageLinks().getSmallThumbnail() != null) {
                    book.setImageUrl(item.getVolumeInfo().getImageLinks().getSmallThumbnail().replace("http:", "https:"));
                }
            }
            
            // Set publisher
            if (item.getVolumeInfo().getPublisher() != null) {
                book.setPublisher(item.getVolumeInfo().getPublisher());
            }
            
            // Set page count
            if (item.getVolumeInfo().getPageCount() > 0) {
                book.setPageCount(item.getVolumeInfo().getPageCount());
            }
            
            // Set ISBN
            if (item.getVolumeInfo().getIndustryIdentifiers() != null) {
                for (BookApiResponse.IndustryIdentifier identifier : item.getVolumeInfo().getIndustryIdentifiers()) {
                    if ("ISBN_13".equals(identifier.getType())) {
                        book.setIsbn(identifier.getIdentifier());
                        break;
                    } else if ("ISBN_10".equals(identifier.getType()) && book.getIsbn() == null) {
                        book.setIsbn(identifier.getIdentifier());
                    }
                }
            }
            
            // Set language
            if (item.getVolumeInfo().getLanguage() != null) {
                book.setLanguage(item.getVolumeInfo().getLanguage());
            }
            
            // Set API flag
            book.setFromApi(true);
            
            books.add(book);
        }
        
        return books;
    }
    
    public void clearCache() {
        if (searchCache != null) {
            searchCache.evictAll();
        }
    }
    
    public void searchBooks(String query, OnSearchResultCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        
        final String normalizedQuery = query.trim();
        String cacheKey = normalizedQuery + "_20";
        List<Book> cachedBooks = searchCache.get(cacheKey);
        
        if (cachedBooks != null) {
            callback.onSuccess(cachedBooks);
            return;
        }
        
        executor.execute(() -> {
            try {
                Call<BookApiResponse> call = apiService.searchBooksByLanguage(normalizedQuery, 20, "ru", Constants.GOOGLE_BOOKS_API_KEY);
                Response<BookApiResponse> response = call.execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Book> books = convertApiResponseToBooks(response.body());
                    searchCache.put(cacheKey, books);
                    callback.onSuccess(books);
                } else {
                    callback.onError("Error fetching books: " + response.message());
                }
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    public void fetchBooks(BooksCallback callback) {
        if (Constants.GOOGLE_BOOKS_API_KEY == null || Constants.GOOGLE_BOOKS_API_KEY.isEmpty()) {
            Log.w(TAG, "API key is not set. Using test books for fetchBooks.");
            List<Book> allTestBooks = new ArrayList<>();
            String[] categories = {
            };
            for (String category : categories) {
                generateTestBooks(category, books -> {
                    synchronized (allTestBooks) {
                        allTestBooks.addAll(books);
                        if (allTestBooks.size() >= categories.length * 3) {
                            Log.d(TAG, "Сгенерировано тестовых книг для fetchBooks: " + allTestBooks.size());
                            callback.onBooksReceived(allTestBooks);
                        }
                    }
                });
            }
            return;
        }
        String[] queries = {
        };
        int maxResults = 40;
        List<Book> allBooks = new ArrayList<>();
        final int[] completedQueries = { 0 };
        final boolean[] apiError = { false };
        Log.d(TAG, "Начинаем загрузку книг из API, категорий: " + queries.length);
        for (String query : queries) {
            clearCache();
            searchBooks(query, maxResults, books -> {
                if (books != null) {
                    Log.d(TAG, "Получено книг из категории '" + query + "': " + books.size());
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
                if (completedQueries[0] == queries.length) {
                    if (apiError[0] && allBooks.isEmpty()) {
                        Log.w(TAG, "Ошибка API и нет результатов в fetchBooks. Используем тестовые книги.");
                        List<Book> allTestBooks = new ArrayList<>();
                        for (String category : queries) {
                            generateTestBooks(category, testBooks -> {
                                synchronized (allTestBooks) {
                                    allTestBooks.addAll(testBooks);
                                    if (allTestBooks.size() >= queries.length * 3) {
                                        Log.d(TAG,
                                                "Сгенерировано тестовых книг для fetchBooks: " + allTestBooks.size());
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

    public LiveData<List<Book>> searchBooksLiveData(String query, int maxResults) {
        MutableLiveData<List<Book>> booksLiveData = new MutableLiveData<>();
        if (query == null || query.trim().isEmpty()) {
            booksLiveData.setValue(new ArrayList<>());
            return booksLiveData;
        }
        final String normalizedQuery = query.trim();
        String cacheKey = normalizedQuery + "_" + maxResults;
        List<Book> cachedBooks = searchCache.get(cacheKey);
        if (cachedBooks != null) {
            booksLiveData.setValue(cachedBooks);
            return booksLiveData;
        }
        if (Constants.GOOGLE_BOOKS_API_KEY == null || Constants.GOOGLE_BOOKS_API_KEY.isEmpty()) {
            Log.w(TAG, "API ключ не указан. Используем тестовые книги для LiveData.");
            List<Book> testBooks = new ArrayList<>();
            generateTestBooks(normalizedQuery, books -> {
                testBooks.addAll(books);
                booksLiveData.postValue(testBooks);
            });
            return booksLiveData;
        }
        executor.execute(() -> {
            try {
                Log.d(TAG, "Выполняем поиск по запросу: " + normalizedQuery);
                Call<BookApiResponse> call;
                if (normalizedQuery.contains(" ")) {
                    call = apiService.searchBooksByLanguage("\"" + normalizedQuery + "\"", maxResults, "ru",
                            Constants.GOOGLE_BOOKS_API_KEY);
                } else {
                    call = apiService.searchBooksByLanguage(normalizedQuery, maxResults, "ru",
                            Constants.GOOGLE_BOOKS_API_KEY);
                }
                Response<BookApiResponse> response = call.execute();
                List<Book> allBooks = new ArrayList<>();
                boolean apiError = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<Book> books = convertApiResponseToBooks(response.body());
                    allBooks.addAll(books);
                    Log.d(TAG, "Получено книг от API (первый запрос): " + books.size());
                    if (books.size() < 5 && normalizedQuery.contains(" ")) {
                        try {
                            Thread.sleep(500);
                            Call<BookApiResponse> secondCall = apiService.searchBooksByLanguage(normalizedQuery,
                                    maxResults, "ru", Constants.GOOGLE_BOOKS_API_KEY);
                            Response<BookApiResponse> secondResponse = secondCall.execute();
                            if (secondResponse.isSuccessful() && secondResponse.body() != null) {
                                List<Book> additionalBooks = convertApiResponseToBooks(secondResponse.body());
                                Log.d(TAG, "Получено книг от API (второй запрос): " + additionalBooks.size());
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
                    if (allBooks.size() < 5 && normalizedQuery.length() < 20) {
                        try {
                            Thread.sleep(500);
                            String enhancedQuery = normalizedQuery + " книга";
                            Call<BookApiResponse> thirdCall = apiService.searchBooksByLanguage(enhancedQuery,
                                    maxResults, "ru", Constants.GOOGLE_BOOKS_API_KEY);
                            Response<BookApiResponse> thirdResponse = thirdCall.execute();
                            if (thirdResponse.isSuccessful() && thirdResponse.body() != null) {
                                List<Book> additionalBooks = convertApiResponseToBooks(thirdResponse.body());
                                Log.d(TAG, "Получено книг от API (третий запрос): " + additionalBooks.size());
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
                if ((apiError && allBooks.isEmpty()) || allBooks.isEmpty()) {
                    Log.w(TAG, "Ошибка API или нет результатов в LiveData. Используем тестовые книги.");
                    generateTestBooks(normalizedQuery, books -> {
                        searchCache.put(cacheKey, books);
                        booksLiveData.postValue(books);
                    });
                    return;
                }
                sortBooksByRelevance(allBooks, normalizedQuery);
                searchCache.put(cacheKey, allBooks);
                booksLiveData.postValue(allBooks);
                Log.d(TAG, "Найдено книг по запросу '" + normalizedQuery + "' в LiveData: " + allBooks.size());
            } catch (Exception e) {
                Log.e(TAG, "API call failed in LiveData", e);
                generateTestBooks(normalizedQuery, books -> {
                    searchCache.put(cacheKey, books);
                    booksLiveData.postValue(books);
                });
            }
        });
        return booksLiveData;
    }

    private void sortBooksByRelevance(List<Book> books, String query) {
        if (books == null || books.isEmpty() || query == null || query.isEmpty()) {
            return;
        }
        final String lowerQuery = query.toLowerCase();
        Collections.sort(books, (book1, book2) -> {
            int relevance1 = calculateRelevance(book1, lowerQuery);
            int relevance2 = calculateRelevance(book2, lowerQuery);
            return Integer.compare(relevance2, relevance1);
        });
    }

    private int calculateRelevance(Book book, String lowerQuery) {
        int relevance = 0;
        String title = book.getTitle();
        if (title != null) {
            String lowerTitle = title.toLowerCase();
            if (lowerTitle.equals(lowerQuery)) {
                relevance += 100;
            } else if (lowerTitle.startsWith(lowerQuery)) {
                relevance += 50;
            } else if (lowerTitle.contains(lowerQuery)) {
                relevance += 30;
            } else {
                String[] queryWords = lowerQuery.split("\\s+");
                for (String word : queryWords) {
                    if (word.length() > 2 && lowerTitle.contains(word)) {
                        relevance += 10;
                    }
                }
            }
        }
        String author = book.getAuthor();
        if (author != null) {
            String lowerAuthor = author.toLowerCase();
            if (lowerAuthor.equals(lowerQuery)) {
                relevance += 80;
            } else if (lowerAuthor.startsWith(lowerQuery)) {
                relevance += 40;
            } else if (lowerAuthor.contains(lowerQuery)) {
                relevance += 20;
            } else {
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

    public void searchBooks(String query, int maxResults, BooksCallback callback) {
        String cacheKey = query + "_" + maxResults;
        List<Book> cachedBooks = searchCache.get(cacheKey);
        if (cachedBooks != null) {
            callback.onBooksReceived(cachedBooks);
            return;
        }
        if (Constants.GOOGLE_BOOKS_API_KEY == null || Constants.GOOGLE_BOOKS_API_KEY.isEmpty()) {
            Log.w(TAG, "API ключ не указан. Используем тестовые книги.");
            generateTestBooks(query, callback);
            return;
        }
        List<String> enhancedQueries = new ArrayList<>();
        enhancedQueries.add(query);
        if (query.length() > 3) {
            enhancedQueries.add(query);
            String category = determineCategory(query);
            if (category != null && !query.toLowerCase().contains(category.toLowerCase())) {
                enhancedQueries.add(query + " " + category);
            }
        }
        final List<Book> allBooks = new ArrayList<>();
        final int[] completedQueries = { 0 };
        final boolean[] apiError = { false };
        Log.d(TAG, "Начинаем поиск книг с " + enhancedQueries.size() + " запросами");
        int resultsPerQuery = Math.max(1, maxResults / Math.max(1, enhancedQueries.size()));
        for (String enhancedQuery : enhancedQueries) {
            Call<BookApiResponse> call = apiService.searchBooksByLanguage(enhancedQuery, resultsPerQuery, "ru",
                    Constants.GOOGLE_BOOKS_API_KEY);
            call.enqueue(new Callback<BookApiResponse>() {
                @Override
                public void onResponse(Call<BookApiResponse> call, Response<BookApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Book> books = convertApiResponseToBooks(response.body());
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
                    } else {
                        apiError[0] = true;
                        int errorCode = response.code();
                        Log.e(TAG, "Ошибка API при запросе '" + enhancedQuery + "': " + errorCode);
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e(TAG, "API error body: " + errorBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при чтении тела ошибки: " + e.getMessage());
                        }
                    }
                    synchronized (allBooks) {
                        completedQueries[0]++;
                        if (completedQueries[0] == enhancedQueries.size()) {
                            if (allBooks.isEmpty()) {
                                Log.w(TAG, "Нет результатов поиска. Используем тестовые книги.");
                                generateTestBooks(query, callback);
                            } else {
                                sortBooksByRelevance(allBooks, query);
                                searchCache.put(cacheKey, allBooks);
                                Log.d(TAG, "Поиск завершен, всего найдено книг: " + allBooks.size());
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
                    if (completedQueries[0] == enhancedQueries.size()) {
                        if (allBooks.isEmpty()) {
                            Log.w(TAG, "Нет результатов поиска из-за ошибок сети. Используем тестовые книги.");
                            generateTestBooks(query, callback);
                        } else {
                            sortBooksByRelevance(allBooks, query);
                            searchCache.put(cacheKey, allBooks);
                            Log.d(TAG, "Поиск завершен с ошибками сети, найдено книг: " + allBooks.size());
                            callback.onBooksReceived(allBooks);
                        }
                    }
                }
            });
        }
    }

    private String determineCategory(String query) {
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("фантаст") || lowerQuery.contains("sci-fi") || lowerQuery.contains("научн")) {
            return "фантастика";
        } else if (lowerQuery.contains("детектив") || lowerQuery.contains("крими") || lowerQuery.contains("загадк")) {
            return "детектив";
        } else if (lowerQuery.contains("роман") || lowerQuery.contains("любов") || lowerQuery.contains("отношен")) {
            return "роман";
        } else if (lowerQuery.contains("истор") || lowerQuery.contains("войн") || lowerQuery.contains("древн")) {
            return "история";
        } else if (lowerQuery.contains("биограф") || lowerQuery.contains("мемуар")
                || lowerQuery.contains("автобиограф")) {
            return "биография";
        } else if (lowerQuery.contains("програм") || lowerQuery.contains("код") || lowerQuery.contains("компьютер")) {
            return "программирование";
        } else if (lowerQuery.contains("психолог") || lowerQuery.contains("самопомощ")
                || lowerQuery.contains("развит")) {
            return "психология";
        } else if (lowerQuery.contains("филос") || lowerQuery.contains("мысл") || lowerQuery.contains("бытие")) {
            return "философия";
        } else if (lowerQuery.contains("наук") || lowerQuery.contains("физик") || lowerQuery.contains("биолог")) {
            return "наука";
        } else if (lowerQuery.contains("искусств") || lowerQuery.contains("живопис") || lowerQuery.contains("музык")) {
            return "искусство";
        }
        return null;
    }

    public void loadBooksPage(String query, int page, BooksCallback callback) {
        int startIndex = page * Constants.PAGE_SIZE;
        String cacheKey = query + "_page_" + page;
        List<Book> cachedBooks = searchCache.get(cacheKey);
        if (cachedBooks != null) {
            Log.d(TAG, "Используем кэшированные книги для страницы " + page);
            callback.onBooksReceived(cachedBooks);
            return;
        }
        if (Constants.GOOGLE_BOOKS_API_KEY == null || Constants.GOOGLE_BOOKS_API_KEY.isEmpty()) {
            Log.w(TAG, "API ключ не указан. Используем тестовые книги.");
            generateTestBooks(query, callback);
            return;
        }
        String encodedQuery = query;
        try {
            encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e(TAG, "Ошибка кодирования запроса: " + e.getMessage());
        }
        Log.d(TAG, "Загрузка страницы " + page + " для запроса: " + query);
        apiService.searchBooksWithPaginationAndLanguage(
                encodedQuery,
                Math.min(Constants.PAGE_SIZE, 10),
                startIndex,
                "ru",
                Constants.GOOGLE_BOOKS_API_KEY)
                .enqueue(new Callback<BookApiResponse>() {
                    @Override
                    public void onResponse(Call<BookApiResponse> call, Response<BookApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Book> books = convertApiResponseToBooks(response.body());
                            searchCache.put(cacheKey, books);
                            Log.d(TAG, "Получено книг для страницы " + page + ": " + books.size());
                            callback.onBooksReceived(books);
                        } else {
                            int errorCode = response.code();
                            Log.e(TAG, "API error: " + errorCode);
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "API error body: " + errorBody);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка при чтении тела ошибки: " + e.getMessage());
                            }
                            generateTestBooks(query, callback);
                        }
                    }

                    @Override
                    public void onFailure(Call<BookApiResponse> call, Throwable t) {
                        Log.e(TAG, "Ошибка сети при запросе '" + query + "': " + t.getMessage());
                        generateTestBooks(query, callback);
                    }
                });
    }

    private Callback<BookApiResponse> createCallbackWithErrorHandling(String query, List<Book> allBooks,
            int[] completedQueries,
            int totalQueries, String cacheKey, BooksCallback callback, boolean[] apiError) {
        return new Callback<BookApiResponse>() {
            @Override
            public void onResponse(Call<BookApiResponse> call, Response<BookApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executor.execute(() -> {
                        List<Book> books = convertApiResponseToBooks(response.body());
                        Log.d(TAG, "Получено книг для запроса '" + query + "': " + books.size());
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
                            if (completedQueries[0] == totalQueries) {
                                if (apiError[0] && allBooks.isEmpty()) {
                                    Log.w(TAG, "Ошибка API и нет результатов. Используем тестовые книги.");
                                    generateTestBooks(query, callback);
                                    return;
                                }
                                searchCache.put(cacheKey, allBooks);
                                Log.d(TAG, "Всего получено книг для страницы: " + allBooks.size());
                                callback.onBooksReceived(allBooks);
                            }
                        }
                    });
                } else {
                    int errorCode = response.code();
                    Log.e(TAG, "API error: " + errorCode);
                    if (errorCode == 403) {
                        apiError[0] = true;
                    }
                    synchronized (allBooks) {
                        completedQueries[0]++;
                        if (completedQueries[0] == totalQueries) {
                            if (apiError[0] && allBooks.isEmpty()) {
                                Log.w(TAG, "Ошибка API и нет результатов. Используем тестовые книги.");
                                generateTestBooks(query, callback);
                                return;
                            }
                            searchCache.put(cacheKey, allBooks);
                            callback.onBooksReceived(allBooks);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<BookApiResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                apiError[0] = true;
                synchronized (allBooks) {
                    completedQueries[0]++;
                    if (completedQueries[0] == totalQueries) {
                        if (apiError[0] && allBooks.isEmpty()) {
                            Log.w(TAG, "Ошибка API и нет результатов. Используем тестовые книги.");
                            generateTestBooks(query, callback);
                            return;
                        }
                        searchCache.put(cacheKey, allBooks);
                        callback.onBooksReceived(allBooks);
                    }
                }
            }
        };
    }

    private void generateTestBooks(String query, BooksCallback callback) {
        callback.onBooksReceived(new ArrayList<>());
    }
}