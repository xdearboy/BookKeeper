package com.xdearboy.bookkeeper.ui.home;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.repository.BookRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final BookRepository bookRepository;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isSearching = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingMoreMainList = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isNetworkError = new MutableLiveData<>(false);
    private final MutableLiveData<List<Book>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<List<Book>> books = new MutableLiveData<>();
    private final MutableLiveData<Integer> mainListPage = new MutableLiveData<>(1);

    public HomeViewModel(Application application) {
        super(application);
        bookRepository = BookRepository.getInstance(application);
        loadInitialData();
    }
    
    private void loadInitialData() {
        isLoading.setValue(true);
        bookRepository.getAllBooks().observeForever(bookList -> {
            books.setValue(bookList);
            isLoading.setValue(false);
        });
    }

    public LiveData<List<Book>> getAllBooks() {
        return bookRepository.getAllBooks();
    }

    public LiveData<List<Book>> getAvailableBooks() {
        return bookRepository.getAvailableBooks();
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<Boolean> getIsSearching() {
        return isSearching;
    }

    public void setIsSearching(boolean searching) {
        isSearching.setValue(searching);
    }

    public LiveData<List<Book>> getSearchResults() {
        return searchResults;
    }
    
    public LiveData<List<Book>> getBooks() {
        return books;
    }
    
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    
    public LiveData<Boolean> isLoadingMore() {
        return isLoadingMore;
    }
    
    public LiveData<Boolean> isLoadingMoreMainList() {
        return isLoadingMoreMainList;
    }
    
    public LiveData<Boolean> isNetworkError() {
        return isNetworkError;
    }
    
    public LiveData<Integer> getMainListPage() {
        return mainListPage;
    }
    
    public void loadNextPageMainList() {
        if (isLoadingMoreMainList.getValue() != null && isLoadingMoreMainList.getValue()) {
            return;
        }
        
        isLoadingMoreMainList.setValue(true);
        int currentPage = mainListPage.getValue() != null ? mainListPage.getValue() : 1;
        int nextPage = currentPage + 1;
        
        // Simulate loading more data
        mainListPage.setValue(nextPage);
        isLoadingMoreMainList.setValue(false);
    }
    
    public void fetchBooksFromApi() {
        isLoading.setValue(true);
        isNetworkError.setValue(false);
        
        bookRepository.searchBooksFromApi("programming", result -> {
            if (result.status == com.xdearboy.bookkeeper.util.Resource.Status.SUCCESS && result.data != null) {
                List<Book> currentBooks = books.getValue();
                if (currentBooks == null) {
                    currentBooks = new ArrayList<>();
                }
                
                List<Book> newBooks = new ArrayList<>(currentBooks);
                newBooks.addAll(result.data);
                books.postValue(newBooks);
                isNetworkError.postValue(false);
            } else {
                isNetworkError.postValue(true);
            }
            isLoading.postValue(false);
        });
    }
    
    public void refreshSearchResults() {
        String query = searchQuery.getValue();
        if (query != null && !query.isEmpty()) {
            searchBooks(query);
        }
    }

    public void searchBooks(String query) {
        isSearching.setValue(true);
        bookRepository.searchBooksExtended(query, books -> {
            searchResults.postValue(books);
            isSearching.postValue(false);
        });
    }

    public void searchBooksFromApi(String query) {
        isSearching.setValue(true);
        bookRepository.searchBooksFromApi(query, result -> {
            if (result.status == com.xdearboy.bookkeeper.util.Resource.Status.SUCCESS) {
                searchResults.postValue(result.data);
            }
            isSearching.postValue(false);
        });
    }

    public void addBookToLibrary(Book book) {
        bookRepository.insert(book);
    }

    public void borrowBook(String bookId, String userId) {
        long currentTime = System.currentTimeMillis();
        long returnDate = currentTime + (14 * 24 * 60 * 60 * 1000); // 14 days in milliseconds
        bookRepository.borrowBook(bookId, userId, currentTime, returnDate);
    }

    public void returnBook(String bookId) {
        bookRepository.returnBook(bookId);
    }
}