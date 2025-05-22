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
        String userId = SessionManager.getInstance(getApplication()).getUserId();
        if (userId != null) {
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
            List<String> borrowedBookIds = user.getBorrowedBookIdsList();
            if (borrowedBookIds == null || borrowedBookIds.isEmpty()) {
                borrowedBooks.setValue(new ArrayList<>());
                return;
            }
            for (Map.Entry<String, Observer<Book>> entry : bookObserversMap.entrySet()) {
                LiveData<Book> bookLiveData = bookLiveDataMap.get(entry.getKey());
                if (bookLiveData != null) {
                    bookLiveData.removeObserver(entry.getValue());
                }
            }
            bookLiveDataMap.clear();
            bookObserversMap.clear();
            final List<Book> books = new ArrayList<>();
            final int[] booksToLoad = {borrowedBookIds.size()};
            for (String bookId : borrowedBookIds) {
                LiveData<Book> bookLiveData = bookRepository.getBookById(bookId);
                Observer<Book> bookObserver = new Observer<Book>() {
                    @Override
                    public void onChanged(Book book) {
                        if (book != null) {
                            if (!books.contains(book)) {
                                books.add(book);
                            }
                            booksToLoad[0]--;
                            if (booksToLoad[0] <= 0) {
                                borrowedBooks.setValue(books);
                            }
                        }
                    }
                };
                bookLiveDataMap.put(bookId, bookLiveData);
                bookObserversMap.put(bookId, bookObserver);
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
        }
    }
    public void updateUser(User user) {
        userRepository.updateUser(user);
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        for (Map.Entry<String, Observer<Book>> entry : bookObserversMap.entrySet()) {
            LiveData<Book> bookLiveData = bookLiveDataMap.get(entry.getKey());
            if (bookLiveData != null) {
                bookLiveData.removeObserver(entry.getValue());
            }
        }
    }
}