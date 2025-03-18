package com.xdearboy.bookkeeper.ui.home;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.xdearboy.bookkeeper.api.BookApiClient;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.repository.BookRepository;
import com.xdearboy.bookkeeper.util.Constants;
import com.xdearboy.bookkeeper.util.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";
    private static final int SEARCH_RESULTS_LIMIT = Constants.PAGE_SIZE;

    private final BookRepository bookRepository;
    private final BookApiClient bookApiClient;
    private final MutableLiveData<List<Book>> books;
    private final MutableLiveData<String> searchQuery;
    private final MediatorLiveData<List<Book>> searchResults;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<Boolean> isLoadingMore;
    private final MutableLiveData<Integer> currentPage;
    private final MutableLiveData<Boolean> hasMorePages;
    private final MutableLiveData<Boolean> isNetworkError;
    private final MutableLiveData<Boolean> isLoadingMoreMainList;
    private final MutableLiveData<Integer> mainListPage;
    private final MutableLiveData<Boolean> hasMorePagesMainList;
    
    private LiveData<List<Book>> allBooksLiveData;
    private LiveData<List<Book>> currentSearchLiveData;
    private List<Book> allLoadedBooks = new ArrayList<>();

    public HomeViewModel(Application application) {
        super(application);
        bookRepository = BookRepository.getInstance(application);
        bookApiClient = new BookApiClient();
        books = new MutableLiveData<>();
        searchQuery = new MutableLiveData<>("");
        searchResults = new MediatorLiveData<>();
        isLoading = new MutableLiveData<>(false);
        isLoadingMore = new MutableLiveData<>(false);
        isLoadingMoreMainList = new MutableLiveData<>(false);
        currentPage = new MutableLiveData<>(0);
        mainListPage = new MutableLiveData<>(0);
        hasMorePages = new MutableLiveData<>(true);
        hasMorePagesMainList = new MutableLiveData<>(true);
        isNetworkError = new MutableLiveData<>(false);
        
        // Инициализируем списки
        searchResults.setValue(new ArrayList<>());
        books.setValue(new ArrayList<>());
        
        // Логируем начальные значения
        Log.d(TAG, "ViewModel создана. Начальная страница основного списка: " + 
              mainListPage.getValue() + ", страница поиска: " + currentPage.getValue());
        
        loadBooks();
        
        // Загружаем книги из API при создании ViewModel
        fetchBooksFromApi();
    }

    public LiveData<List<Book>> getBooks() {
        return books;
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public LiveData<List<Book>> getSearchResults() {
        return searchResults;
    }
    
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    
    public LiveData<Boolean> isLoadingMore() {
        return isLoadingMore;
    }
    
    public LiveData<Boolean> hasMorePages() {
        return hasMorePages;
    }
    
    public LiveData<Boolean> isNetworkError() {
        return isNetworkError;
    }

    public LiveData<Boolean> isLoadingMoreMainList() {
        return isLoadingMoreMainList;
    }
    
    public LiveData<Boolean> hasMorePagesMainList() {
        return hasMorePagesMainList;
    }

    public LiveData<Integer> getCurrentPage() {
        return currentPage;
    }

    public LiveData<Integer> getMainListPage() {
        return mainListPage;
    }

    public void setSearchQuery(String query) {
        if (query == null) {
            query = "";
        }
        
        // Если запрос не изменился, ничего не делаем
        if (query.equals(searchQuery.getValue())) {
            return;
        }
        
        searchQuery.setValue(query);
        currentPage.setValue(0);
        hasMorePages.setValue(true);
        isNetworkError.setValue(false);
        
        if (query.isEmpty()) {
            if (books.getValue() != null) {
                searchResults.setValue(books.getValue());
            } else {
                searchResults.setValue(new ArrayList<>());
            }
        } else {
            searchBooks(query);
        }
    }

    private void loadBooks() {
        allBooksLiveData = bookRepository.getAllBooks();
        allBooksLiveData.observeForever(booksObserver);
    }

    private void searchBooks(String query) {
        if (currentSearchLiveData != null) {
            searchResults.removeSource(currentSearchLiveData);
        }
        
        // Проверяем, что запрос не пустой
        if (query == null || query.trim().isEmpty()) {
            searchResults.postValue(new ArrayList<>());
            isLoading.postValue(false);
            return;
        }
        
        // Нормализуем запрос
        final String normalizedQuery = query.trim();
        
        isLoading.postValue(true);
        Log.d(TAG, "Начинаем поиск по запросу: '" + normalizedQuery + "'");
        
        // Проверяем доступность сети
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            isLoading.postValue(false);
            isNetworkError.postValue(true);
            
            // Если сеть недоступна, ищем в локальной базе данных
            currentSearchLiveData = bookRepository.searchBooks(normalizedQuery);
            searchResults.addSource(currentSearchLiveData, books -> {
                searchResults.postValue(books);
                hasMorePages.postValue(false); // Нет пагинации для локального поиска
                Log.d(TAG, "Поиск в локальной базе (без сети): найдено " + (books != null ? books.size() : 0) + " книг");
            });
            
            return;
        }
        
        // Сначала ищем в локальной базе данных
        currentSearchLiveData = bookRepository.searchBooks(normalizedQuery);
        searchResults.addSource(currentSearchLiveData, localBooks -> {
            // Если в локальной базе есть результаты, показываем их сразу
            if (localBooks != null && !localBooks.isEmpty()) {
                searchResults.postValue(localBooks);
                Log.d(TAG, "Найдено книг в локальной базе: " + localBooks.size());
            }
            
            // Выполняем поиск в API
            // Создаем несколько вариантов запроса для повышения шансов найти нужную книгу
            List<String> searchQueries = new ArrayList<>();
            
            // Основной запрос
            searchQueries.add(normalizedQuery);
            
            // Если запрос содержит пробелы, добавляем варианты с кавычками для точного поиска
            if (normalizedQuery.contains(" ")) {
                searchQueries.add("\"" + normalizedQuery + "\"");
            }
            
            // Если запрос короткий, добавляем варианты с автором/названием
            if (normalizedQuery.length() < 20) {
                searchQueries.add(normalizedQuery + " автор");
                searchQueries.add(normalizedQuery + " книга");
            }
            
            // Счетчик завершенных запросов
            final int[] completedQueries = {0};
            final List<Book> allApiBooks = new ArrayList<>();
            
            for (String searchQuery : searchQueries) {
                bookApiClient.searchBooksLiveData(searchQuery, SEARCH_RESULTS_LIMIT)
                    .observeForever(new Observer<List<Book>>() {
                        @Override
                        public void onChanged(List<Book> apiBooks) {
                            completedQueries[0]++;
                            
                            if (apiBooks != null && !apiBooks.isEmpty()) {
                                Log.d(TAG, "Найдено книг по запросу '" + searchQuery + "': " + apiBooks.size());
                                
                                // Добавляем книги, избегая дубликатов
                                synchronized (allApiBooks) {
                                    for (Book apiBook : apiBooks) {
                                        boolean isDuplicate = false;
                                        for (Book existingBook : allApiBooks) {
                                            if (existingBook.getId().equals(apiBook.getId()) || 
                                                (existingBook.getTitle().equals(apiBook.getTitle()) && 
                                                 existingBook.getAuthor().equals(apiBook.getAuthor()))) {
                                                isDuplicate = true;
                                                break;
                                            }
                                        }
                                        
                                        if (!isDuplicate) {
                                            allApiBooks.add(apiBook);
                                        }
                                    }
                                }
                            }
                            
                            // Когда все запросы выполнены, объединяем результаты
                            if (completedQueries[0] == searchQueries.size()) {
                                // Объединяем результаты из локальной базы и API
                                List<Book> combinedBooks = new ArrayList<>();
                                
                                // Добавляем книги из локальной базы
                                if (localBooks != null) {
                                    combinedBooks.addAll(localBooks);
                                }
                                
                                // Добавляем книги из API, избегая дубликатов
                                for (Book apiBook : allApiBooks) {
                                    boolean isDuplicate = false;
                                    for (Book localBook : combinedBooks) {
                                        if (localBook.getId().equals(apiBook.getId()) || 
                                            (localBook.getTitle().equals(apiBook.getTitle()) && 
                                             localBook.getAuthor().equals(apiBook.getAuthor()))) {
                                            isDuplicate = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!isDuplicate) {
                                        combinedBooks.add(apiBook);
                                    }
                                }
                                
                                // Сортируем результаты по релевантности
                                sortBooksByRelevance(combinedBooks, normalizedQuery);
                                
                                // Обновляем результаты
                                searchResults.postValue(combinedBooks);
                                Log.d(TAG, "Всего найдено книг (локальные + API): " + combinedBooks.size());
                                
                                // Если результатов меньше лимита, значит больше страниц нет
                                hasMorePages.postValue(allApiBooks.size() >= SEARCH_RESULTS_LIMIT);
                                
                                // Сохраняем книги из API в локальную базу данных
                                for (Book book : allApiBooks) {
                                    bookRepository.addBook(book);
                                }
                                
                                isLoading.postValue(false);
                            }
                        }
                    });
            }
        });
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
     * Загружает следующую страницу результатов поиска
     */
    public void loadNextPage() {
        if (!hasMorePages.getValue() || Boolean.TRUE.equals(isLoadingMore.getValue())) {
            return;
        }
        
        String query = searchQuery.getValue();
        if (query == null || query.isEmpty()) {
            return;
        }
        
        // Проверяем доступность сети
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            isNetworkError.postValue(true);
            return;
        }
        
        Integer page = currentPage.getValue();
        if (page == null) {
            page = 0;
        }
        
        final int nextPage = page + 1;
        isLoadingMore.postValue(true);
        
        // Загружаем следующую страницу
        bookApiClient.loadBooksPage(query, nextPage, books -> {
            List<Book> currentBooks = searchResults.getValue();
            if (currentBooks == null) {
                currentBooks = new ArrayList<>();
            }
            
            // Добавляем новые книги к текущим результатам
            List<Book> newList = new ArrayList<>(currentBooks);
            
            // Добавляем книги, избегая дубликатов
            for (Book book : books) {
                boolean isDuplicate = false;
                for (Book existingBook : newList) {
                    if (existingBook.getId().equals(book.getId()) || 
                        (existingBook.getTitle().equals(book.getTitle()) && 
                         existingBook.getAuthor().equals(book.getAuthor()))) {
                        isDuplicate = true;
                        break;
                    }
                }
                
                if (!isDuplicate) {
                    newList.add(book);
                }
            }
            
            // Обновляем UI
            searchResults.postValue(newList);
            
            // Обновляем состояние
            currentPage.postValue(nextPage);
            isLoadingMore.postValue(false);
            
            // Если результатов меньше лимита, значит больше страниц нет
            hasMorePages.postValue(books.size() >= SEARCH_RESULTS_LIMIT);
            
            // Сохраняем книги в локальную базу данных
            for (Book book : books) {
                bookRepository.addBook(book);
            }
            
            Log.d(TAG, "Загружена страница " + nextPage + " поиска, добавлено книг: " + books.size());
        });
    }
    
    /**
     * Обновляет результаты поиска (сбрасывает пагинацию)
     */
    public void refreshSearchResults() {
        String query = searchQuery.getValue();
        if (query == null || query.isEmpty()) {
            return;
        }
        
        // Сбрасываем состояние ошибки сети
        isNetworkError.postValue(false);
        
        currentPage.postValue(0);
        hasMorePages.postValue(true);
        searchBooks(query);
    }

    public void addBook(Book book) {
        bookRepository.addBook(book);
    }

    public void updateBook(Book book) {
        bookRepository.updateBook(book);
    }

    public void deleteBook(String bookId) {
        bookRepository.deleteBook(bookId);
    }

    public void borrowBook(String bookId, String userId, java.util.Date returnDate) {
        bookRepository.borrowBook(bookId, userId, returnDate);
    }

    public void returnBook(String bookId) {
        bookRepository.returnBook(bookId);
    }
    
    /**
     * Загружает книги из API
     */
    public void fetchBooksFromApi() {
        // Проверяем доступность сети
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            isNetworkError.postValue(true);
            return;
        }
        
        isLoading.postValue(true);
        Log.d(TAG, "Начинаем загрузку книг из API");
        
        // Очищаем кэш API клиента перед загрузкой новых данных
        bookApiClient.clearCache();
        
        // Сбрасываем страницу на первую
        mainListPage.postValue(0);
        
        // Загружаем книги из разных категорий
        String[] categories = {
            "фантастика", "детектив", "роман", "история", "биография", 
            "программирование", "психология", "философия", "наука", "искусство"
        };
        
        final List<Book> allBooks = new ArrayList<>();
        
        // Загружаем только первые 3 категории сразу, остальные будем загружать последовательно
        loadCategoriesSequentially(categories, 0, allBooks);
    }
    
    /**
     * Загружает категории последовательно, чтобы избежать ошибок API
     * @param categories Массив категорий
     * @param index Текущий индекс
     * @param allBooks Список всех книг
     */
    private void loadCategoriesSequentially(String[] categories, int index, List<Book> allBooks) {
        if (index >= categories.length) {
            // Все категории загружены
            Log.d(TAG, "Загрузка книг из API завершена. Всего книг: " + allBooks.size());
            books.postValue(allBooks);
            isLoading.postValue(false);
            return;
        }
        
        String category = categories[index];
        Log.d(TAG, "Загрузка категории " + (index + 1) + "/" + categories.length + ": " + category);
        
        bookApiClient.loadBooksPage(category, 0, categoryBooks -> {
            if (categoryBooks != null && !categoryBooks.isEmpty()) {
                Log.d(TAG, "Получено книг из категории '" + category + "': " + categoryBooks.size());
                
                // Добавляем книги, избегая дубликатов
                for (Book book : categoryBooks) {
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
                        // Сохраняем книгу в базу данных
                        bookRepository.addBook(book);
                    }
                }
                
                // Если это первая категория, сразу обновляем UI
                if (index == 0) {
                    books.postValue(new ArrayList<>(categoryBooks));
                }
            }
            
            // Загружаем следующую категорию с небольшой задержкой
            Handler handler = new Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(() -> loadCategoriesSequentially(categories, index + 1, allBooks), 500);
        });
    }
    
    /**
     * Добавляет тестовые книги в базу данных
     */
    private void addTestBooks() {
        List<Book> testBooks = new ArrayList<>();
        
        // Книга 1
        Book book1 = new Book();
        book1.setId("test_1");
        book1.setTitle("Война и мир");
        book1.setAuthor("Лев Толстой");
        book1.setGenre("Роман");
        book1.setDescription("Роман-эпопея Льва Николаевича Толстого, описывающий русское общество в эпоху войн против Наполеона в 1805—1812 годах.");
        book1.setCoverImageUrl("https://m.media-amazon.com/images/I/71KqFJwv-hL._AC_UF1000,1000_QL80_.jpg");
        book1.setBorrowed(false);
        testBooks.add(book1);
        
        // Книга 2
        Book book2 = new Book();
        book2.setId("test_2");
        book2.setTitle("Преступление и наказание");
        book2.setAuthor("Федор Достоевский");
        book2.setGenre("Роман");
        book2.setDescription("Социально-психологический и социально-философский роман Фёдора Михайловича Достоевского, над которым писатель работал в 1865—1866 годах.");
        book2.setCoverImageUrl("https://m.media-amazon.com/images/I/81XS0eEfS+L._AC_UF1000,1000_QL80_.jpg");
        book2.setBorrowed(false);
        testBooks.add(book2);
        
        // Книга 3
        Book book3 = new Book();
        book3.setId("test_3");
        book3.setTitle("Мастер и Маргарита");
        book3.setAuthor("Михаил Булгаков");
        book3.setGenre("Фантастика");
        book3.setDescription("Роман Михаила Афанасьевича Булгакова, работа над которым началась в конце 1920-х годов и продолжалась вплоть до смерти писателя.");
        book3.setCoverImageUrl("https://m.media-amazon.com/images/I/91LUbAcpACL._AC_UF1000,1000_QL80_.jpg");
        book3.setBorrowed(false);
        testBooks.add(book3);
        
        // Книга 4
        Book book4 = new Book();
        book4.setId("test_4");
        book4.setTitle("Анна Каренина");
        book4.setAuthor("Лев Толстой");
        book4.setGenre("Роман");
        book4.setDescription("Роман Льва Толстого о трагической любви замужней дамы Анны Карениной и блестящего офицера Вронского на фоне счастливой семейной жизни дворян Константина Лёвина и Кити Щербацкой.");
        book4.setCoverImageUrl("https://m.media-amazon.com/images/I/71yJLhQekBL._AC_UF1000,1000_QL80_.jpg");
        book4.setBorrowed(false);
        testBooks.add(book4);
        
        // Книга 5
        Book book5 = new Book();
        book5.setId("test_5");
        book5.setTitle("Евгений Онегин");
        book5.setAuthor("Александр Пушкин");
        book5.setGenre("Поэзия");
        book5.setDescription("Роман в стихах Александра Сергеевича Пушкина, написанный в 1823—1831 годах, одно из самых значительных произведений русской словесности.");
        book5.setCoverImageUrl("https://m.media-amazon.com/images/I/71c1LRLBTBL._AC_UF1000,1000_QL80_.jpg");
        book5.setBorrowed(false);
        testBooks.add(book5);
        
        // Книга 6
        Book book6 = new Book();
        book6.setId("test_6");
        book6.setTitle("Идиот");
        book6.setAuthor("Федор Достоевский");
        book6.setGenre("Роман");
        book6.setDescription("Роман Фёдора Михайловича Достоевского, впервые опубликованный в номерах журнала «Русский вестник» за 1868 год.");
        book6.setCoverImageUrl("https://m.media-amazon.com/images/I/81fw8IZ8UaL._AC_UF1000,1000_QL80_.jpg");
        book6.setBorrowed(false);
        testBooks.add(book6);
        
        // Книга 7
        Book book7 = new Book();
        book7.setId("test_7");
        book7.setTitle("Тихий Дон");
        book7.setAuthor("Михаил Шолохов");
        book7.setGenre("Роман");
        book7.setDescription("Роман-эпопея Михаила Шолохова в четырёх томах. Тихий Дон описывает жизнь донских казаков в начале XX века, перипетии их личных драм и любовных историй на фоне Первой мировой войны, революционных событий 1917 года и Гражданской войны в России.");
        book7.setCoverImageUrl("https://m.media-amazon.com/images/I/51jKHz3VGML._AC_UF1000,1000_QL80_.jpg");
        book7.setBorrowed(false);
        testBooks.add(book7);
        
        // Книга 8
        Book book8 = new Book();
        book8.setId("test_8");
        book8.setTitle("Братья Карамазовы");
        book8.setAuthor("Федор Достоевский");
        book8.setGenre("Роман");
        book8.setDescription("Последний роман Фёдора Михайловича Достоевского, который автор писал два года.");
        book8.setCoverImageUrl("https://m.media-amazon.com/images/I/71OZY035QKL._AC_UF1000,1000_QL80_.jpg");
        book8.setBorrowed(false);
        testBooks.add(book8);
        
        // Книга 9
        Book book9 = new Book();
        book9.setId("test_9");
        book9.setTitle("Герой нашего времени");
        book9.setAuthor("Михаил Лермонтов");
        book9.setGenre("Роман");
        book9.setDescription("Первый в русской прозе социально-психологический роман, написанный Михаилом Юрьевичем Лермонтовым в 1837—1840 годах.");
        book9.setCoverImageUrl("https://m.media-amazon.com/images/I/61KZ-ULaYQL._AC_UF1000,1000_QL80_.jpg");
        book9.setBorrowed(false);
        testBooks.add(book9);
        
        // Книга 10
        Book book10 = new Book();
        book10.setId("test_10");
        book10.setTitle("Мёртвые души");
        book10.setAuthor("Николай Гоголь");
        book10.setGenre("Роман");
        book10.setDescription("Произведение Николая Васильевича Гоголя, жанр которого сам автор обозначил как поэму.");
        book10.setCoverImageUrl("https://m.media-amazon.com/images/I/71RjCQTJLtL._AC_UF1000,1000_QL80_.jpg");
        book10.setBorrowed(false);
        testBooks.add(book10);
        
        // Сохраняем тестовые книги в базу данных
        for (Book book : testBooks) {
            bookRepository.addBook(book);
        }
        
        Log.d(TAG, "Добавлено " + testBooks.size() + " тестовых книг");
    }
    
    /**
     * Добавляет тестовые книги в базу данных (публичный метод)
     */
    public void addTestBooksToDatabase() {
        addTestBooks();
        addMoreTestBooks();
    }
    
    /**
     * Добавляет дополнительные тестовые книги в базу данных
     */
    private void addMoreTestBooks() {
        List<Book> testBooks = new ArrayList<>();
        
        // Книга 11
        Book book11 = new Book();
        book11.setId("test_11");
        book11.setTitle("Отцы и дети");
        book11.setAuthor("Иван Тургенев");
        book11.setGenre("Роман");
        book11.setDescription("Роман Ивана Сергеевича Тургенева, написанный в 1860—1861 годах и опубликованный в 1862 году.");
        book11.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book11.setBorrowed(false);
        testBooks.add(book11);
        
        // Книга 12
        Book book12 = new Book();
        book12.setId("test_12");
        book12.setTitle("Обломов");
        book12.setAuthor("Иван Гончаров");
        book12.setGenre("Роман");
        book12.setDescription("Роман Ивана Александровича Гончарова, впервые опубликованный в 1859 году.");
        book12.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book12.setBorrowed(false);
        testBooks.add(book12);
        
        // Книга 13
        Book book13 = new Book();
        book13.setId("test_13");
        book13.setTitle("Горе от ума");
        book13.setAuthor("Александр Грибоедов");
        book13.setGenre("Комедия");
        book13.setDescription("Комедия в стихах Александра Сергеевича Грибоедова. Она сочетает в себе элементы классицизма и новых для начала XIX века романтизма и реализма.");
        book13.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book13.setBorrowed(false);
        testBooks.add(book13);
        
        // Книга 14
        Book book14 = new Book();
        book14.setId("test_14");
        book14.setTitle("Ревизор");
        book14.setAuthor("Николай Гоголь");
        book14.setGenre("Комедия");
        book14.setDescription("Комедия в пяти действиях, написанная Николаем Васильевичем Гоголем в 1835 году.");
        book14.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book14.setBorrowed(false);
        testBooks.add(book14);
        
        // Книга 15
        Book book15 = new Book();
        book15.setId("test_15");
        book15.setTitle("Бесы");
        book15.setAuthor("Федор Достоевский");
        book15.setGenre("Роман");
        book15.setDescription("Роман Фёдора Михайловича Достоевского, изданный в 1871—1872 годах.");
        book15.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book15.setBorrowed(false);
        testBooks.add(book15);
        
        // Книга 16
        Book book16 = new Book();
        book16.setId("test_16");
        book16.setTitle("Вишневый сад");
        book16.setAuthor("Антон Чехов");
        book16.setGenre("Пьеса");
        book16.setDescription("Пьеса в четырёх действиях Антона Павловича Чехова, написанная в 1903 году.");
        book16.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book16.setBorrowed(false);
        testBooks.add(book16);
        
        // Книга 17
        Book book17 = new Book();
        book17.setId("test_17");
        book17.setTitle("Капитанская дочка");
        book17.setAuthor("Александр Пушкин");
        book17.setGenre("Роман");
        book17.setDescription("Исторический роман Александра Пушкина, действие которого происходит во время восстания Емельяна Пугачёва.");
        book17.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book17.setBorrowed(false);
        testBooks.add(book17);
        
        // Книга 18
        Book book18 = new Book();
        book18.setId("test_18");
        book18.setTitle("Гроза");
        book18.setAuthor("Александр Островский");
        book18.setGenre("Пьеса");
        book18.setDescription("Пьеса в пяти действиях Александра Николаевича Островского, написанная в 1859 году.");
        book18.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book18.setBorrowed(false);
        testBooks.add(book18);
        
        // Книга 19
        Book book19 = new Book();
        book19.setId("test_19");
        book19.setTitle("Собачье сердце");
        book19.setAuthor("Михаил Булгаков");
        book19.setGenre("Повесть");
        book19.setDescription("Повесть Михаила Афанасьевича Булгакова, написанная в 1925 году.");
        book19.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book19.setBorrowed(false);
        testBooks.add(book19);
        
        // Книга 20
        Book book20 = new Book();
        book20.setId("test_20");
        book20.setTitle("Белая гвардия");
        book20.setAuthor("Михаил Булгаков");
        book20.setGenre("Роман");
        book20.setDescription("Роман Михаила Афанасьевича Булгакова, описывающий события Гражданской войны в Киеве в конце 1918 года.");
        book20.setCoverImageUrl("https://m.media-amazon.com/images/I/71Yw+qXAT9L._AC_UF1000,1000_QL80_.jpg");
        book20.setBorrowed(false);
        testBooks.add(book20);
        
        // Сохраняем тестовые книги в базу данных
        for (Book book : testBooks) {
            bookRepository.addBook(book);
        }
        
        Log.d(TAG, "Добавлено " + testBooks.size() + " дополнительных тестовых книг");
    }
    
    /**
     * Загружает следующую страницу основного списка книг
     */
    public void loadNextPageMainList() {
        if (!hasMorePagesMainList.getValue() || Boolean.TRUE.equals(isLoadingMoreMainList.getValue())) {
            return;
        }
        
        // Проверяем доступность сети
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            isNetworkError.postValue(true);
            return;
        }
        
        Integer page = mainListPage.getValue();
        if (page == null) {
            page = 0;
        }
        
        final int nextPage = page + 1;
        isLoadingMoreMainList.postValue(true);
        
        // Загружаем следующую порцию книг из разных категорий
        String[] categories = {
            "фантастика", "детектив", "роман", "история", "биография", 
            "программирование", "психология", "философия", "наука", "искусство",
            "классика", "поэзия", "приключения", "фэнтези", "триллер"
        };
        String query = categories[nextPage % categories.length]; // Циклически выбираем категорию
        
        bookApiClient.loadBooksPage(query, nextPage, newBooks -> {
            List<Book> currentBooks = books.getValue();
            if (currentBooks == null) {
                currentBooks = new ArrayList<>();
            }
            
            // Добавляем новые книги к текущим результатам
            List<Book> newList = new ArrayList<>(currentBooks);
            
            // Добавляем книги, избегая дубликатов
            for (Book book : newBooks) {
                boolean isDuplicate = false;
                for (Book existingBook : newList) {
                    if (existingBook.getId().equals(book.getId()) || 
                        (existingBook.getTitle().equals(book.getTitle()) && 
                         existingBook.getAuthor().equals(book.getAuthor()))) {
                        isDuplicate = true;
                        break;
                    }
                }
                
                if (!isDuplicate) {
                    newList.add(book);
                }
            }
            
            // Обновляем список всех загруженных книг
            allLoadedBooks.addAll(newBooks);
            
            // Обновляем UI
            books.postValue(newList);
            
            // Обновляем состояние
            mainListPage.postValue(nextPage);
            isLoadingMoreMainList.postValue(false);
            
            // Если результатов меньше лимита, значит больше страниц нет
            hasMorePagesMainList.postValue(newBooks.size() >= Constants.PAGE_SIZE);
            
            // Сохраняем книги в локальную базу данных
            for (Book book : newBooks) {
                bookRepository.addBook(book);
            }
            
            Log.d(TAG, "Загружена страница " + nextPage + " основного списка, добавлено книг: " + newBooks.size());
        });
    }
    
    /**
     * Переход на указанную страницу поиска
     * @param page Номер страницы
     */
    public void goToSearchPage(int page) {
        if (page < 0) {
            return;
        }
        
        String query = searchQuery.getValue();
        if (query == null || query.isEmpty()) {
            return;
        }
        
        // Проверяем доступность сети
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            isNetworkError.postValue(true);
            return;
        }
        
        isLoading.postValue(true);
        currentPage.postValue(page);
        
        Log.d(TAG, "Переход на страницу поиска: " + page + " для запроса: " + query);
        
        // Очищаем кэш перед загрузкой новой страницы
        bookApiClient.clearCache();
        
        // Создаем новый список для хранения книг текущей страницы поиска
        final List<Book> pageBooks = new ArrayList<>();
        
        // Загружаем книги по запросу
        bookApiClient.loadBooksPage(query, 0, searchBooks -> {
            if (searchBooks != null && !searchBooks.isEmpty()) {
                Log.d(TAG, "Получено книг по запросу '" + query + "': " + searchBooks.size());
                
                // Добавляем книги в список текущей страницы
                pageBooks.addAll(searchBooks);
                
                // Обновляем результаты - заменяем текущий список на новый
                searchResults.postValue(pageBooks);
                
                // Если результатов меньше лимита, значит больше страниц нет
                hasMorePages.postValue(searchBooks.size() >= SEARCH_RESULTS_LIMIT);
                
                // Сохраняем книги в локальную базу данных
                for (Book book : searchBooks) {
                    bookRepository.addBook(book);
                }
                
                Log.d(TAG, "Загружена страница " + page + " поиска, получено книг: " + pageBooks.size());
            } else {
                // Если результатов нет, значит больше страниц нет
                hasMorePages.postValue(false);
                Log.d(TAG, "Страница " + page + " поиска пуста");
            }
            
            isLoading.postValue(false);
        });
    }
    
    /**
     * Переход на указанную страницу основного списка
     * @param page Номер страницы
     */
    public void goToMainListPage(int page) {
        if (page < 0) {
            return;
        }
        
        // Проверяем доступность сети
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            isNetworkError.postValue(true);
            return;
        }
        
        isLoadingMoreMainList.postValue(true);
        mainListPage.postValue(page);
        
        // Загружаем книги для указанной страницы
        String[] categories = {
            "фантастика", "детектив", "роман", "история", "биография", 
            "программирование", "психология", "философия", "наука", "искусство",
            "классика", "поэзия", "приключения", "фэнтези", "триллер"
        };
        
        // Выбираем категорию в зависимости от страницы
        String category = categories[page % categories.length];
        
        Log.d(TAG, "Переход на страницу " + page + " основного списка, категория: " + category);
        
        // Очищаем кэш перед загрузкой новой страницы
        bookApiClient.clearCache();
        
        // Создаем новый список для хранения книг текущей страницы
        final List<Book> pageBooks = new ArrayList<>();
        
        // Загружаем книги из выбранной категории
        bookApiClient.loadBooksPage(category, 0, categoryBooks -> {
            if (categoryBooks != null && !categoryBooks.isEmpty()) {
                Log.d(TAG, "Получено книг из категории '" + category + "': " + categoryBooks.size());
                
                // Добавляем книги в список текущей страницы
                pageBooks.addAll(categoryBooks);
                
                // Обновляем UI - заменяем текущий список книг на новый
                books.postValue(pageBooks);
                
                // Если результатов меньше лимита, значит больше страниц нет
                hasMorePagesMainList.postValue(categoryBooks.size() >= Constants.PAGE_SIZE);
                
                // Сохраняем книги в локальную базу данных
                for (Book book : categoryBooks) {
                    bookRepository.addBook(book);
                }
                
                Log.d(TAG, "Загружена страница " + page + " основного списка, получено книг: " + pageBooks.size());
            } else {
                // Если результатов нет, значит больше страниц нет
                hasMorePagesMainList.postValue(false);
                Log.d(TAG, "Страница " + page + " основного списка пуста");
            }
            
            isLoadingMoreMainList.postValue(false);
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (allBooksLiveData != null) {
            allBooksLiveData.removeObserver(booksObserver);
        }
        if (currentSearchLiveData != null) {
            searchResults.removeSource(currentSearchLiveData);
        }
        
        // Очищаем кэш при уничтожении ViewModel
        bookApiClient.clearCache();
    }
    
    private final Observer<List<Book>> booksObserver = new Observer<List<Book>>() {
        @Override
        public void onChanged(List<Book> bookList) {
            Log.d(TAG, "Получено книг из базы данных: " + (bookList != null ? bookList.size() : 0));
            books.postValue(bookList);
            if (searchQuery.getValue() == null || searchQuery.getValue().isEmpty()) {
                searchResults.postValue(bookList);
            }
        }
    };
}