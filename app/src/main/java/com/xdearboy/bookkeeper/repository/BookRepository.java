package com.xdearboy.bookkeeper.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.xdearboy.bookkeeper.api.BookApiClient;
import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.database.dao.BookDao;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.util.Resource;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/**
 * Репозиторий для работы с книгами
 */
public class BookRepository {
    
    private static final String TAG = "BookRepository";
    
    private final BookDao bookDao;
    private final LiveData<List<Book>> allBooks;
    private final LiveData<List<Book>> availableBooks;
    private final LiveData<List<Book>> borrowedBooks;
    private final BookApiClient apiClient;
    private final FirebaseRepository firebaseRepository;
    private final UserRepository userRepository;
    
    private static BookRepository instance;
    
    private BookRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        bookDao = db.bookDao();
        allBooks = bookDao.getAllBooks();
        availableBooks = bookDao.getAvailableBooks();
        borrowedBooks = bookDao.getBorrowedBooks();
        apiClient = new BookApiClient();
        firebaseRepository = FirebaseRepository.getInstance();
        userRepository = UserRepository.getInstance(application);
    }
    
    public static synchronized BookRepository getInstance(Application application) {
        if (instance == null) {
            instance = new BookRepository(application);
        }
        return instance;
    }
    
    /**
     * Возвращает все книги
     * @return LiveData со списком всех книг
     */
    public LiveData<List<Book>> getAllBooks() {
        return allBooks;
    }
    
    /**
     * Возвращает доступные книги
     * @return LiveData со списком доступных книг
     */
    public LiveData<List<Book>> getAvailableBooks() {
        return availableBooks;
    }
    
    /**
     * Возвращает заимствованные книги
     * @return LiveData со списком заимствованных книг
     */
    public LiveData<List<Book>> getBorrowedBooks() {
        return borrowedBooks;
    }
    
    /**
     * Возвращает книги, заимствованные пользователем
     * @param userId ID пользователя
     * @return LiveData со списком книг, заимствованных пользователем
     */
    public LiveData<List<Book>> getBooksBorrowedByUser(String userId) {
        return bookDao.getBooksBorrowedByUser(userId);
    }
    
    /**
     * Возвращает книгу по ID
     * @param bookId ID книги
     * @return LiveData с книгой
     */
    public LiveData<Book> getBookById(String bookId) {
        return bookDao.getBookById(bookId);
    }
    
    /**
     * Поиск книг по запросу
     * @param query Поисковый запрос
     * @return LiveData со списком книг, соответствующих запросу
     */
    public LiveData<List<Book>> searchBooks(String query) {
        MutableLiveData<List<Book>> searchResults = new MutableLiveData<>();
        
        if (query == null || query.trim().isEmpty()) {
            searchResults.postValue(new ArrayList<>());
            return searchResults;
        }
        
        // Нормализуем запрос
        final String normalizedQuery = query.trim();
        final String exactQuery = normalizedQuery;
        
        // Выполняем поиск в фоновом потоке
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Book> combinedResults = new ArrayList<>();
            
            // Шаг 1: Точное совпадение (приоритет наивысший)
            List<Book> exactMatches = bookDao.searchBooksExact("%" + exactQuery + "%");
            if (exactMatches != null && !exactMatches.isEmpty()) {
                combinedResults.addAll(exactMatches);
            }
            
            // Шаг 2: Поиск по началу слова (высокий приоритет)
            List<Book> startsWithMatches = bookDao.searchBooksStartsWith(exactQuery);
            if (startsWithMatches != null && !startsWithMatches.isEmpty()) {
                // Добавляем только те, которых еще нет в результатах
                for (Book book : startsWithMatches) {
                    if (!containsBook(combinedResults, book)) {
                        combinedResults.add(book);
                    }
                }
            }
            
            // Шаг 3: Поиск без учета регистра (средний приоритет)
            List<Book> caseInsensitiveMatches = bookDao.searchBooksCaseInsensitive(normalizedQuery);
            if (caseInsensitiveMatches != null && !caseInsensitiveMatches.isEmpty()) {
                // Добавляем только те, которых еще нет в результатах
                for (Book book : caseInsensitiveMatches) {
                    if (!containsBook(combinedResults, book)) {
                        combinedResults.add(book);
                    }
                }
            }
            
            // Шаг 4: Если все еще нет результатов, используем расширенный поиск (низкий приоритет)
            if (combinedResults.isEmpty()) {
                List<Book> extendedResults = bookDao.searchBooksExtended(normalizedQuery);
                if (extendedResults != null && !extendedResults.isEmpty()) {
                    combinedResults.addAll(extendedResults);
                }
            }
            
            // Возвращаем результаты
            searchResults.postValue(combinedResults);
        });
        
        return searchResults;
    }
    
    /**
     * Проверяет, содержится ли книга в списке
     * @param books Список книг
     * @param book Книга для проверки
     * @return true, если книга содержится в списке
     */
    private boolean containsBook(List<Book> books, Book book) {
        for (Book existingBook : books) {
            if (existingBook.getId().equals(book.getId()) || 
                (existingBook.getTitle().equals(book.getTitle()) && 
                 existingBook.getAuthor().equals(book.getAuthor()))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Добавляет книгу
     * @param book Книга для добавления
     */
    public void addBook(Book book) {
        if (book.getId() == null || book.getId().isEmpty()) {
            book.setId("book_" + UUID.randomUUID().toString());
        }
        AppDatabase.databaseWriteExecutor.execute(() -> bookDao.insert(book));
    }
    
    /**
     * Обновляет книгу
     * @param book Книга для обновления
     */
    public void updateBook(Book book) {
        AppDatabase.databaseWriteExecutor.execute(() -> bookDao.update(book));
    }
    
    /**
     * Удаляет книгу
     * @param bookId ID книги для удаления
     */
    public void deleteBook(String bookId) {
        AppDatabase.databaseWriteExecutor.execute(() -> bookDao.deleteById(bookId));
    }
    
    /**
     * Заимствует книгу
     * @param bookId ID книги
     * @param userId ID пользователя
     * @param returnDate Дата возврата
     */
    public void borrowBook(String bookId, String userId, Date returnDate) {
        Date borrowDate = new Date();
        AppDatabase.databaseWriteExecutor.execute(() -> 
                bookDao.borrowBook(bookId, userId, borrowDate.getTime(), returnDate.getTime()));
    }
    
    /**
     * Возвращает книгу
     * @param bookId ID книги
     */
    public void returnBook(String bookId) {
        AppDatabase.databaseWriteExecutor.execute(() -> bookDao.returnBook(bookId));
    }
    
    /**
     * Загружает книги из API
     */
    public void fetchBooksFromApi() {
        apiClient.fetchBooks(books -> {
            for (Book book : books) {
                book.setFromApi(true);
            }
            AppDatabase.databaseWriteExecutor.execute(() -> bookDao.insertAll(books));
        });
    }
    
    /**
     * Синхронизация локальной базы данных с Firebase
     */
    public void syncWithFirebase() {
        if (!firebaseRepository.isUserLoggedIn()) {
            Log.d(TAG, "Синхронизация с Firebase отменена: пользователь не авторизован");
            return;
        }
        
        Log.d(TAG, "Начинаем синхронизацию с Firebase");
        
        // Получаем все книги из локальной базы данных
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Book> localBooks = getAllBooksSync();
                
                // Используем Handler для возврата в основной поток
                new Handler(Looper.getMainLooper()).post(() -> {
                    // Синхронизируем с Firebase
                    Observer<Resource<Boolean>> observer = new Observer<Resource<Boolean>>() {
                        @Override
                        public void onChanged(Resource<Boolean> resource) {
                            try {
                                if (resource.status == Resource.Status.SUCCESS) {
                                    Log.d(TAG, "Книги успешно отправлены в Firebase");
                                    // Получаем книги из Firebase и обновляем локальную базу
                                    getFirebaseBooks();
                                    // Удаляем наблюдателя после успешного завершения
                                    firebaseRepository.syncUserBooks(localBooks).removeObserver(this);
                                } else if (resource.status == Resource.Status.ERROR) {
                                    Log.e(TAG, "Ошибка при отправке книг в Firebase: " + resource.message);
                                    // Удаляем наблюдателя при ошибке
                                    firebaseRepository.syncUserBooks(localBooks).removeObserver(this);
                                }
                                // Не удаляем наблюдателя для статуса LOADING
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка при обработке результата синхронизации", e);
                                // Удаляем наблюдателя в случае исключения
                                firebaseRepository.syncUserBooks(localBooks).removeObserver(this);
                            }
                        }
                    };
                    
                    // Добавляем наблюдателя
                    firebaseRepository.syncUserBooks(localBooks).observeForever(observer);
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при синхронизации с Firebase", e);
            }
        });
    }
    
    /**
     * Получает книги из Firebase и обновляет локальную базу данных
     */
    private void getFirebaseBooks() {
        firebaseRepository.getUserBooks().observeForever(new Observer<Resource<List<Book>>>() {
            @Override
            public void onChanged(Resource<List<Book>> resource) {
                if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                    List<Book> firebaseBooks = resource.data;
                    Log.d(TAG, "Получено книг из Firebase: " + firebaseBooks.size());
                    
                    // Обновляем локальную базу данных
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (Book book : firebaseBooks) {
                            bookDao.insert(book);
                        }
                        Log.d(TAG, "Локальная база данных обновлена книгами из Firebase");
                    });
                } else if (resource.status == Resource.Status.ERROR) {
                    Log.e(TAG, "Ошибка при получении книг из Firebase: " + resource.message);
                }
                
                // Удаляем наблюдателя после получения данных
                firebaseRepository.getUserBooks().removeObserver(this);
            }
        });
    }
    
    /**
     * Синхронно получает все книги из базы данных
     * @return Список всех книг
     */
    public List<Book> getAllBooksSync() {
        return bookDao.getAllBooksSync();
    }
    
    /**
     * Добавляет книгу и синхронизирует с Firebase
     * @param book Книга для добавления
     */
    public void addBookAndSync(Book book) {
        // Добавляем книгу в локальную базу данных
        addBook(book);
        
        // Синхронизируем с Firebase
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            firebaseRepository.addBook(book).observeForever(new Observer<Resource<Book>>() {
                @Override
                public void onChanged(Resource<Book> resource) {
                    if (resource.status == Resource.Status.SUCCESS) {
                        Log.d(TAG, "Книга успешно добавлена в Firebase: " + book.getTitle());
                    } else if (resource.status == Resource.Status.ERROR) {
                        Log.e(TAG, "Ошибка при добавлении книги в Firebase: " + resource.message);
                    }
                    
                    // Удаляем наблюдателя после получения данных
                    firebaseRepository.addBook(book).removeObserver(this);
                }
            });
        }
    }
    
    /**
     * Удаляет книгу и синхронизирует с Firebase
     * @param bookId ID книги для удаления
     */
    public void deleteBookAndSync(String bookId) {
        // Удаляем книгу из локальной базы данных
        deleteBook(bookId);
        
        // Синхронизируем с Firebase
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            firebaseRepository.deleteBook(bookId).observeForever(new Observer<Resource<Boolean>>() {
                @Override
                public void onChanged(Resource<Boolean> resource) {
                    if (resource.status == Resource.Status.SUCCESS) {
                        Log.d(TAG, "Книга успешно удалена из Firebase: " + bookId);
                    } else if (resource.status == Resource.Status.ERROR) {
                        Log.e(TAG, "Ошибка при удалении книги из Firebase: " + resource.message);
                    }
                    
                    // Удаляем наблюдателя после получения данных
                    firebaseRepository.deleteBook(bookId).removeObserver(this);
                }
            });
        }
    }
    
    /**
     * Возвращает репозиторий пользователей
     * @return Репозиторий пользователей
     */
    public UserRepository getUserRepository() {
        return userRepository;
    }
} 