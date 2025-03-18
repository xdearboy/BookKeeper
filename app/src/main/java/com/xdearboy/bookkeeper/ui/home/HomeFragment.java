package com.xdearboy.bookkeeper.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.android.material.snackbar.Snackbar;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.adapter.BookAdapter;
import com.xdearboy.bookkeeper.databinding.FragmentHomeBinding;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.util.NetworkUtils;
import com.xdearboy.bookkeeper.ui.BookDetailsActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;

public class HomeFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private BookAdapter bookAdapter;
    private BookAdapter searchResultsAdapter;
    private TextView searchEmptyView;
    private boolean isLoadingMore = false;
    private int pastVisibleItems, visibleItemCount, totalItemCount;
    private boolean isLoadingMoreMainList = false;
    private int pastVisibleItemsMainList, visibleItemCountMainList, totalItemCountMainList;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupToolbar();
        setupRecyclerView();
        setupSearchView();
        setupFab();
        setupSwipeRefresh();
        observeViewModel();
        
        // Проверяем состояние сети при запуске
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showNetworkError();
        }

        return root;
    }

    private void setupToolbar() {
        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.toolbar);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.app_name);
    }

    private void setupRecyclerView() {
        // Основной список книг
        RecyclerView recyclerView = binding.booksRecyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        bookAdapter = new BookAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(bookAdapter);

        // Добавляем слушатель прокрутки для пагинации основного списка
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (dy > 0) { // Прокрутка вниз
                    visibleItemCountMainList = layoutManager.getChildCount();
                    totalItemCountMainList = layoutManager.getItemCount();
                    pastVisibleItemsMainList = layoutManager.findFirstVisibleItemPosition();
                    
                    if (!isLoadingMoreMainList) {
                        if ((visibleItemCountMainList + pastVisibleItemsMainList) >= totalItemCountMainList - 5) {
                            // Загружаем следующую страницу, когда пользователь приближается к концу списка
                            isLoadingMoreMainList = true;
                            homeViewModel.loadNextPageMainList();
                        }
                    }
                }
            }
        });

        // Список результатов поиска
        RecyclerView searchResultsRecyclerView = binding.searchResultsRecyclerView;
        // Используем GridLayoutManager для более эффективного отображения результатов поиска
        int spanCount = 1; // По умолчанию 1 колонка
        // Определяем количество колонок в зависимости от ориентации экрана
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 2; // В ландшафтной ориентации 2 колонки
        }
        GridLayoutManager searchLayoutManager = new GridLayoutManager(getContext(), spanCount);
        searchResultsRecyclerView.setLayoutManager(searchLayoutManager);
        searchResultsAdapter = new BookAdapter(getContext(), new ArrayList<>(), this);
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);
        
        // Добавляем слушатель прокрутки для пагинации
        searchResultsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (dy > 0) { // Прокрутка вниз
                    visibleItemCount = searchLayoutManager.getChildCount();
                    totalItemCount = searchLayoutManager.getItemCount();
                    pastVisibleItems = searchLayoutManager.findFirstVisibleItemPosition();
                    
                    if (!isLoadingMore) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 5) {
                            // Загружаем следующую страницу, когда пользователь приближается к концу списка
                            isLoadingMore = true;
                            homeViewModel.loadNextPage();
                        }
                    }
                }
            }
        });
    }

    private void setupSearchView() {
        SearchBar searchBar = binding.searchBar;
        SearchView searchView = binding.searchView;
        
        // Находим TextView для пустых результатов поиска
        searchEmptyView = searchView.findViewById(R.id.search_empty_view);

        // Настройка поиска
        searchView.addTransitionListener((searchView1, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWING) {
                // При открытии поиска показываем текущие результаты поиска
                // Они будут обновлены автоматически через LiveData
            }
        });

        // Обработка ввода поискового запроса
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            String query = searchView.getText().toString();
            performSearch(query);
            return false;
        });
        
        // Обработка изменения текста в поиске
        searchView.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void performSearch(String query) {
        homeViewModel.setSearchQuery(query);
    }

    private void setupFab() {
        binding.fabAddBook.setOnClickListener(v -> {
            // TODO: Открыть экран добавления книги
            Toast.makeText(getContext(), "Добавление книги", Toast.LENGTH_SHORT).show();
        });
        
        // Настраиваем кнопку обновления
        binding.refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Обновление списка книг...", Toast.LENGTH_SHORT).show();
            homeViewModel.fetchBooksFromApi();
        });
        
        // Настраиваем кнопки пагинации для основного списка
        binding.prevPageButton.setOnClickListener(v -> {
            Integer currentPage = homeViewModel.getMainListPage().getValue();
            Log.d("HomeFragment", "Нажата кнопка предыдущей страницы. Текущая страница: " + currentPage);
            
            if (currentPage != null && currentPage > 0) {
                // Переходим на предыдущую страницу
                int newPage = currentPage - 1;
                Log.d("HomeFragment", "Переход на страницу: " + newPage);
                homeViewModel.goToMainListPage(newPage);
                showPageChangeToast(newPage + 1);
            } else {
                showToastAtTop("Вы уже на первой странице");
            }
        });
        
        binding.nextPageButton.setOnClickListener(v -> {
            // Переходим на следующую страницу
            Integer currentPage = homeViewModel.getMainListPage().getValue();
            Boolean hasMorePages = homeViewModel.hasMorePagesMainList().getValue();
            
            Log.d("HomeFragment", "Нажата кнопка следующей страницы. Текущая страница: " + currentPage + 
                    ", есть еще страницы: " + hasMorePages);
            
            if (currentPage != null) {
                int newPage = currentPage + 1;
                Log.d("HomeFragment", "Переход на страницу: " + newPage);
                homeViewModel.goToMainListPage(newPage);
                showPageChangeToast(newPage + 1);
            } else {
                showToastAtTop("Ошибка при переключении страницы");
            }
        });
        
        // Настраиваем кнопки пагинации для поиска
        binding.searchPrevPageButton.setOnClickListener(v -> {
            Integer currentPage = homeViewModel.getCurrentPage().getValue();
            Log.d("HomeFragment", "Нажата кнопка предыдущей страницы поиска. Текущая страница: " + currentPage);
            
            if (currentPage != null && currentPage > 0) {
                // Переходим на предыдущую страницу
                int newPage = currentPage - 1;
                Log.d("HomeFragment", "Переход на страницу поиска: " + newPage);
                homeViewModel.goToSearchPage(newPage);
                showPageChangeToast(newPage + 1);
            } else {
                showToastAtTop("Вы уже на первой странице");
            }
        });
        
        binding.searchNextPageButton.setOnClickListener(v -> {
            // Переходим на следующую страницу
            Integer currentPage = homeViewModel.getCurrentPage().getValue();
            Boolean hasMorePages = homeViewModel.hasMorePages().getValue();
            
            Log.d("HomeFragment", "Нажата кнопка следующей страницы поиска. Текущая страница: " + currentPage + 
                    ", есть еще страницы: " + hasMorePages);
            
            if (currentPage != null) {
                int newPage = currentPage + 1;
                Log.d("HomeFragment", "Переход на страницу поиска: " + newPage);
                homeViewModel.goToSearchPage(newPage);
                showPageChangeToast(newPage + 1);
            } else {
                showToastAtTop("Ошибка при переключении страницы");
            }
        });
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            // Обновляем список книг из локальной базы данных
            homeViewModel.fetchBooksFromApi();
        });
        
        binding.searchSwipeRefreshLayout.setOnRefreshListener(() -> {
            // Обновляем результаты поиска
            homeViewModel.refreshSearchResults();
        });
    }

    private void observeViewModel() {
        homeViewModel.getBooks().observe(getViewLifecycleOwner(), this::updateBooksUI);
        homeViewModel.getSearchResults().observe(getViewLifecycleOwner(), this::updateSearchResultsUI);
        
        // Наблюдаем за состоянием загрузки
        homeViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefreshLayout.setRefreshing(isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            Log.d("HomeFragment", "Состояние загрузки: " + isLoading);
        });
        
        homeViewModel.isLoadingMore().observe(getViewLifecycleOwner(), isLoadingMore -> {
            this.isLoadingMore = isLoadingMore;
            binding.searchSwipeRefreshLayout.setRefreshing(isLoadingMore);
            binding.loadMoreProgress.setVisibility(isLoadingMore ? View.VISIBLE : View.GONE);
            Log.d("HomeFragment", "Состояние загрузки дополнительных результатов поиска: " + isLoadingMore);
        });
        
        homeViewModel.isLoadingMoreMainList().observe(getViewLifecycleOwner(), isLoadingMore -> {
            this.isLoadingMoreMainList = isLoadingMore;
            binding.mainListLoadMoreProgress.setVisibility(isLoadingMore ? View.VISIBLE : View.GONE);
            Log.d("HomeFragment", "Состояние загрузки дополнительных книг основного списка: " + isLoadingMore);
        });
        
        // Наблюдаем за текущей страницей
        homeViewModel.getCurrentPage().observe(getViewLifecycleOwner(), page -> {
            binding.searchPageIndicator.setText(getString(R.string.page_indicator, page + 1));
            Log.d("HomeFragment", "Обновлена страница поиска: " + (page + 1));
        });
        
        homeViewModel.getMainListPage().observe(getViewLifecycleOwner(), page -> {
            binding.mainPageIndicator.setText(getString(R.string.page_indicator, page + 1));
            Log.d("HomeFragment", "Обновлена страница основного списка: " + (page + 1));
        });
        
        // Наблюдаем за ошибками сети
        homeViewModel.isNetworkError().observe(getViewLifecycleOwner(), isNetworkError -> {
            if (isNetworkError) {
                showNetworkError();
                Log.d("HomeFragment", "Обнаружена ошибка сети");
            }
        });
    }

    private void updateBooksUI(List<Book> books) {
        Log.d("HomeFragment", "Обновление UI: получено книг: " + (books != null ? books.size() : 0));
        
        if (books != null) {
            bookAdapter.updateBooks(books);
            
            if (books.isEmpty()) {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.booksRecyclerView.setVisibility(View.GONE);
                Log.d("HomeFragment", "Список книг пуст, показываем emptyView");
            } else {
                binding.emptyView.setVisibility(View.GONE);
                binding.booksRecyclerView.setVisibility(View.VISIBLE);
                Log.d("HomeFragment", "Показываем список книг");
            }
        } else {
            Log.d("HomeFragment", "Получен null вместо списка книг");
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.booksRecyclerView.setVisibility(View.GONE);
        }
    }
    
    private void updateSearchResultsUI(List<Book> books) {
        // Обновляем адаптер с новыми данными
        searchResultsAdapter.updateBooks(books);
        
        // Показываем сообщение, если результаты поиска пусты
        String query = homeViewModel.getSearchQuery().getValue();
        if (books == null || books.isEmpty()) {
            if (query != null && !query.isEmpty()) {
                // Показываем сообщение о пустых результатах только если был запрос
                searchEmptyView.setVisibility(View.VISIBLE);
                searchEmptyView.setText(getString(R.string.no_books_found_for_query, query));
                binding.searchResultsRecyclerView.setVisibility(View.GONE);
            } else {
                // Если запроса не было, просто скрываем все
                searchEmptyView.setVisibility(View.GONE);
                binding.searchResultsRecyclerView.setVisibility(View.VISIBLE);
            }
            binding.loadMoreProgress.setVisibility(View.GONE);
            binding.searchPaginationContainer.setVisibility(View.GONE);
        } else {
            // Есть результаты - показываем их
            searchEmptyView.setVisibility(View.GONE);
            binding.searchResultsRecyclerView.setVisibility(View.VISIBLE);
            binding.searchPaginationContainer.setVisibility(View.VISIBLE);
            
            // Обновляем индикатор страницы
            Integer currentPage = homeViewModel.getCurrentPage().getValue();
            if (currentPage != null) {
                binding.searchPageIndicator.setText(getString(R.string.page_indicator, currentPage + 1));
            }
            
            // Прокручиваем список вверх при обновлении результатов
            binding.searchResultsRecyclerView.smoothScrollToPosition(0);
            
            // Показываем количество найденных книг в логе
            Log.d("HomeFragment", "Найдено книг: " + books.size() + " по запросу: " + query);
        }
    }
    
    private void showNetworkError() {
        Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                R.string.network_error,
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(R.string.retry, v -> {
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                String query = homeViewModel.getSearchQuery().getValue();
                if (query != null && !query.isEmpty()) {
                    homeViewModel.refreshSearchResults();
                } else {
                    homeViewModel.fetchBooksFromApi();
                }
            } else {
                showNetworkError();
            }
        });
        snackbar.show();
    }

    @Override
    public void onBookClick(Book book) {
        Intent intent = new Intent(getContext(), BookDetailsActivity.class);
        intent.putExtra(BookDetailsActivity.EXTRA_BOOK, book);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showPageChangeToast(int page) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), 
                "Переход на страницу " + page, 
                Snackbar.LENGTH_SHORT);
        
        // Настраиваем внешний вид Snackbar
        View snackbarView = snackbar.getView();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        params.setMargins(0, 150, 0, 0);
        snackbarView.setLayoutParams(params);
        
        snackbar.show();
    }

    private void showToastAtTop(String message) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), 
                message, 
                Snackbar.LENGTH_SHORT);
        
        // Настраиваем внешний вид Snackbar
        View snackbarView = snackbar.getView();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        params.setMargins(0, 150, 0, 0);
        snackbarView.setLayoutParams(params);
        
        snackbar.show();
    }
}