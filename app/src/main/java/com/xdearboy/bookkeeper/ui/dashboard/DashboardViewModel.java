package com.xdearboy.bookkeeper.ui.dashboard;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.repository.BookRepository;
import com.xdearboy.bookkeeper.repository.UserRepository;
import com.xdearboy.bookkeeper.util.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final MutableLiveData<User> currentUser;
    private final MutableLiveData<List<Book>> borrowedBooks;
    private final Map<String, LiveData<Book>> bookLiveDataMap;
    private final Map<String, Observer<Book>> bookObserversMap;

    public DashboardViewModel(Application application) {
        super(application);
        userRepository = UserRepository.getInstance(application);
        bookRepository = BookRepository.getInstance(application);
        currentUser = new MutableLiveData<>();
        borrowedBooks = new MutableLiveData<>(new ArrayList<>());
        bookLiveDataMap = new HashMap<>();
        bookObserversMap = new HashMap<>();
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        // Получаем текущего пользователя из SessionManager
        String userId = SessionManager.getInstance(getApplication()).getUserId();
        if (userId != null) {
            // Получаем LiveData с пользователем
            LiveData<User> userLiveData = userRepository.getUserById(userId);
            userLiveData.observeForever(new Observer<User>() {
                @Override
                public void onChanged(User user) {
                    currentUser.setValue(user);
                    loadBorrowedBooks();
                }
            });
        }
    }

    private void loadBorrowedBooks() {
        User user = currentUser.getValue();
        if (user != null) {
            List<String> borrowedBookIds = user.getBorrowedBookIds();
            if (borrowedBookIds == null || borrowedBookIds.isEmpty()) {
                borrowedBooks.setValue(new ArrayList<>());
                return;
            }
            
            // Очищаем предыдущие наблюдатели
            for (Map.Entry<String, Observer<Book>> entry : bookObserversMap.entrySet()) {
                LiveData<Book> bookLiveData = bookLiveDataMap.get(entry.getKey());
                if (bookLiveData != null) {
                    bookLiveData.removeObserver(entry.getValue());
                }
            }
            bookLiveDataMap.clear();
            bookObserversMap.clear();
            
            // Создаем список для хранения книг
            final List<Book> books = new ArrayList<>();
            final int[] booksToLoad = {borrowedBookIds.size()};
            
            for (String bookId : borrowedBookIds) {
                LiveData<Book> bookLiveData = bookRepository.getBookById(bookId);
                Observer<Book> bookObserver = new Observer<Book>() {
                    @Override
                    public void onChanged(Book book) {
                        if (book != null) {
                            // Добавляем книгу в список, если ее еще нет
                            if (!books.contains(book)) {
                                books.add(book);
                            }
                            
                            // Уменьшаем счетчик книг для загрузки
                            booksToLoad[0]--;
                            
                            // Если все книги загружены, обновляем LiveData
                            if (booksToLoad[0] <= 0) {
                                borrowedBooks.setValue(books);
                            }
                        }
                    }
                };
                
                // Сохраняем LiveData и Observer для последующего удаления
                bookLiveDataMap.put(bookId, bookLiveData);
                bookObserversMap.put(bookId, bookObserver);
                
                // Начинаем наблюдение
                bookLiveData.observeForever(bookObserver);
            }
        }
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<List<Book>> getBorrowedBooks() {
        return borrowedBooks;
    }

    public void returnBook(String bookId) {
        User user = currentUser.getValue();
        if (user != null) {
            bookRepository.returnBook(bookId);
            user.removeBorrowedBook(bookId);
            userRepository.updateUser(user);
            // loadBorrowedBooks будет вызван автоматически через наблюдателя за пользователем
        }
    }

    public void updateUser(User user) {
        userRepository.updateUser(user);
        // loadCurrentUser будет вызван автоматически через наблюдателя за пользователем
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Удаляем все наблюдатели при уничтожении ViewModel
        for (Map.Entry<String, Observer<Book>> entry : bookObserversMap.entrySet()) {
            LiveData<Book> bookLiveData = bookLiveDataMap.get(entry.getKey());
            if (bookLiveData != null) {
                bookLiveData.removeObserver(entry.getValue());
            }
        }
    }
}