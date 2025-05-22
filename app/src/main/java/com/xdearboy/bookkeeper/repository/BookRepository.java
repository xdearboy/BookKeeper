package com.xdearboy.bookkeeper.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.xdearboy.bookkeeper.api.BookApiClient;
import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.database.dao.BookDao;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.util.Resource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BookRepository {
    private final BookDao bookDao;
    private final LiveData<List<Book>> allBooks;
    private final LiveData<List<Book>> availableBooks;
    private final LiveData<List<Book>> borrowedBooks;
    private final LiveData<List<Book>> booksFromApi;
    private final LiveData<List<Book>> localBooks;
    private final Executor executor;
    private final BookApiClient apiClient;

    private static volatile BookRepository INSTANCE;
    
    public static BookRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (BookRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BookRepository(application);
                }
            }
        }
        return INSTANCE;
    }
    
    public BookRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        bookDao = db.bookDao();
        allBooks = bookDao.getAllBooks();
        availableBooks = bookDao.getAvailableBooks();
        borrowedBooks = bookDao.getBorrowedBooks();
        booksFromApi = bookDao.getBooksFromApi();
        localBooks = bookDao.getLocalBooks();
        executor = Executors.newSingleThreadExecutor();
        apiClient = new BookApiClient();
    }

    public LiveData<List<Book>> getAllBooks() {
        return allBooks;
    }

    public LiveData<List<Book>> getAvailableBooks() {
        return availableBooks;
    }

    public LiveData<List<Book>> getBorrowedBooks() {
        return borrowedBooks;
    }

    public LiveData<List<Book>> getBooksFromApi() {
        return booksFromApi;
    }

    public LiveData<List<Book>> getLocalBooks() {
        return localBooks;
    }

    public LiveData<Book> getBookById(String bookId) {
        return bookDao.getBookById(bookId);
    }

    public LiveData<List<Book>> getBooksBorrowedByUser(String userId) {
        return bookDao.getBooksBorrowedByUser(userId);
    }

    public void insert(Book book) {
        if (book.getId() == null || book.getId().isEmpty()) {
            book.setId(UUID.randomUUID().toString());
        }
        executor.execute(() -> bookDao.insert(book));
    }

    public void insertAll(List<Book> books) {
        executor.execute(() -> bookDao.insertAll(books));
    }

    public void update(Book book) {
        executor.execute(() -> bookDao.update(book));
    }

    public void delete(Book book) {
        executor.execute(() -> bookDao.delete(book));
    }

    public void deleteById(String bookId) {
        executor.execute(() -> bookDao.deleteById(bookId));
    }

    public void borrowBook(String bookId, String userId, long borrowDate, long returnDate) {
        executor.execute(() -> bookDao.borrowBook(bookId, userId, borrowDate, returnDate));
    }

    public void returnBook(String bookId) {
        executor.execute(() -> bookDao.returnBook(bookId));
    }

    public void searchBooks(String query, OnSearchResultCallback callback) {
        executor.execute(() -> {
            List<Book> results = bookDao.searchBooks(query);
            callback.onSearchResult(results);
        });
    }

    public void searchBooksExtended(String query, OnSearchResultCallback callback) {
        executor.execute(() -> {
            List<Book> results = bookDao.searchBooksExtended(query);
            callback.onSearchResult(results);
        });
    }

    public void searchBooksByTitle(String title, OnSearchResultCallback callback) {
        executor.execute(() -> {
            List<Book> results = bookDao.searchBooksByTitle(title);
            callback.onSearchResult(results);
        });
    }

    public void searchBooksExact(String exactQuery, OnSearchResultCallback callback) {
        executor.execute(() -> {
            List<Book> results = bookDao.searchBooksExact(exactQuery);
            callback.onSearchResult(results);
        });
    }

    public void searchBooksStartsWith(String query, OnSearchResultCallback callback) {
        executor.execute(() -> {
            List<Book> results = bookDao.searchBooksStartsWith(query);
            callback.onSearchResult(results);
        });
    }

    public void searchBooksCaseInsensitive(String query, OnSearchResultCallback callback) {
        executor.execute(() -> {
            List<Book> results = bookDao.searchBooksCaseInsensitive(query);
            callback.onSearchResult(results);
        });
    }

    public void searchBooksStartsWithCaseInsensitive(String query, OnSearchResultCallback callback) {
        executor.execute(() -> {
            List<Book> results = bookDao.searchBooksStartsWithCaseInsensitive(query);
            callback.onSearchResult(results);
        });
    }

    public interface OnSearchResultCallback {
        void onSearchResult(List<Book> books);
    }

    public void searchBooksFromApi(String query, OnApiSearchResultCallback callback) {
        apiClient.searchBooks(query, new BookApiClient.OnSearchResultCallback() {
            public void onSuccess(List<Book> books) {
                callback.onResult(Resource.success(books));
            }

            public void onError(String message) {
                callback.onResult(Resource.error(message, null));
            }
        });
    }

    public interface OnApiSearchResultCallback {
        void onResult(Resource<List<Book>> result);
    }
}