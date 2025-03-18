package com.xdearboy.bookkeeper.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.xdearboy.bookkeeper.model.Book;

import java.util.List;

@Dao
public interface BookDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Book book);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Book> books);
    
    @Update
    void update(Book book);
    
    @Delete
    void delete(Book book);
    
    @Query("DELETE FROM books WHERE id = :bookId")
    void deleteById(String bookId);
    
    @Query("SELECT * FROM books WHERE id = :bookId")
    LiveData<Book> getBookById(String bookId);
    
    @Query("SELECT * FROM books")
    LiveData<List<Book>> getAllBooks();
    
    /**
     * Синхронно получает все книги из базы данных
     * @return Список всех книг
     */
    @Query("SELECT * FROM books")
    List<Book> getAllBooksSync();
    
    @Query("SELECT * FROM books WHERE borrowed = 0")
    LiveData<List<Book>> getAvailableBooks();
    
    @Query("SELECT * FROM books WHERE borrowed = 1")
    LiveData<List<Book>> getBorrowedBooks();
    
    @Query("SELECT * FROM books WHERE borrowedBy = :userId")
    LiveData<List<Book>> getBooksBorrowedByUser(String userId);
    
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%'")
    List<Book> searchBooks(String query);
    
    @Query("SELECT * FROM books WHERE isFromApi = 1")
    LiveData<List<Book>> getBooksFromApi();
    
    @Query("SELECT * FROM books WHERE isFromApi = 0")
    LiveData<List<Book>> getLocalBooks();
    
    @Query("UPDATE books SET borrowed = 1, borrowedBy = :userId, borrowDate = :borrowDate, returnDate = :returnDate WHERE id = :bookId")
    void borrowBook(String bookId, String userId, long borrowDate, long returnDate);
    
    @Query("UPDATE books SET borrowed = 0, borrowedBy = NULL, borrowDate = NULL, returnDate = NULL WHERE id = :bookId")
    void returnBook(String bookId);
    
    @Query("SELECT * FROM books WHERE " +
           "title LIKE '%' || :query || '%' OR " +
           "author LIKE '%' || :query || '%' OR " +
           "genre LIKE '%' || :query || '%' OR " +
           "description LIKE '%' || :query || '%' OR " +
           "publisher LIKE '%' || :query || '%' OR " +
           "isbn LIKE '%' || :query || '%'")
    List<Book> searchBooksExtended(String query);
    
    @Query("SELECT * FROM books WHERE title LIKE '%' || :title || '%'")
    List<Book> searchBooksByTitle(String title);
    
    @Query("SELECT * FROM books WHERE " +
           "title LIKE :exactQuery OR " +
           "author LIKE :exactQuery")
    List<Book> searchBooksExact(String exactQuery);
    
    @Query("SELECT * FROM books WHERE " +
           "title LIKE :exactQuery || '%' OR " +
           "author LIKE :exactQuery || '%'")
    List<Book> searchBooksStartsWith(String exactQuery);
    
    @Query("SELECT * FROM books WHERE " +
           "LOWER(title) LIKE LOWER('%' || :query || '%') OR " +
           "LOWER(author) LIKE LOWER('%' || :query || '%')")
    List<Book> searchBooksCaseInsensitive(String query);
    
    @Query("SELECT * FROM books WHERE " +
           "LOWER(title) LIKE LOWER(:query || '%') OR " +
           "LOWER(author) LIKE LOWER(:query || '%')")
    List<Book> searchBooksStartsWithCaseInsensitive(String query);
} 