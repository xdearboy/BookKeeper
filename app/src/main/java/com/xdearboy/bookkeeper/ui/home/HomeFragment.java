package com.xdearboy.bookkeeper.ui.home;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
    private TextView searchEmptyView; // For empty search results message (if needed)
    private boolean isLoadingMore = false;
    private int pastVisibleItems, visibleItemCount, totalItemCount;
    private boolean isLoadingMoreMainList = false;
    private int pastVisibleItemsMainList, visibleItemCountMainList, totalItemCountMainList;
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setupRecyclerView();
        setupSearchField();
        setupSwipeRefresh();
        observeViewModel();
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showNetworkError();
        }
        return root;
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home_toolbar, menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            Toast.makeText(requireContext(), "Профиль (заглушка)", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private int getCurrentPage() {
        Integer page = homeViewModel.getMainListPage().getValue();
        return page != null ? page : 0;
    }
    private void showPageChangeToast(int page) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(),
                "Переход на страницу " + (page + 1),
                Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        params.setMargins(0, 150, 0, 0);
        snackbarView.setLayoutParams(params);
        snackbar.show();
    }
    private void smoothScrollToTop() {
        binding.booksRecyclerView.smoothScrollToPosition(0);
    }
    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.booksRecyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        bookAdapter = new BookAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(bookAdapter);
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
                            isLoadingMoreMainList = true;
                            homeViewModel.loadNextPageMainList();
                        }
                    }
                }
            }
        });
    }
    private void setupSearchField() {
        // Set up search EditText and search icon click
        binding.searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("HomeFragment", "Search text changing: before=" + s);
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("HomeFragment", "Search text changed: on=" + s);
                if (s.length() > 0) {
                    performSearch(s.toString());
                } else {
                    updateBooksUI(new ArrayList<>());
                    homeViewModel.setSearchQuery("");
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {
                Log.d("HomeFragment", "Search text changed: after=" + s);
            }
        });
        // Получаем ссылку на иконку поиска внутри CardView (первый элемент LinearLayout)
        ImageView searchIconView = (ImageView) ((LinearLayout) binding.searchCard.getChildAt(0)).getChildAt(0);
        searchIconView.setOnClickListener(v -> {
            String query = binding.searchEditText.getText().toString();
            performSearch(query);
        });
    }
    private void performSearch(String query) {
        Log.d("HomeFragment", "performSearch() called with query=" + query);
        homeViewModel.setSearchQuery(query);
    }
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            homeViewModel.fetchBooksFromApi();
        });
    }
    private void observeViewModel() {
        homeViewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            bookAdapter.submitList(books);
            binding.progressBar.setVisibility(View.GONE);
        });
        homeViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        homeViewModel.getSearchResults().observe(getViewLifecycleOwner(), books -> {
            Log.d("HomeFragment", "Search results observed: " + (books != null ? books.size() : 0) + " books");
            updateBooksUI(books);
        });
        homeViewModel.isLoadingMore().observe(getViewLifecycleOwner(), isLoadingMore -> {
            this.isLoadingMore = isLoadingMore;
            Log.d("HomeFragment", "Состояние загрузки дополнительных результатов поиска: " + isLoadingMore);
        });
        homeViewModel.isLoadingMoreMainList().observe(getViewLifecycleOwner(), isLoadingMore -> {
            this.isLoadingMoreMainList = isLoadingMore;
            binding.mainListLoadMoreProgress.setVisibility(isLoadingMore ? View.VISIBLE : View.GONE);
            Log.d("HomeFragment", "Состояние загрузки дополнительных книг основного списка: " + isLoadingMore);
        });
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
    private void showNetworkError() {
        Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                R.string.network_error,
                Snackbar.LENGTH_LONG);
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
    private void showToastAtTop(String message) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(),
                message,
                Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        params.setMargins(0, 150, 0, 0);
        snackbarView.setLayoutParams(params);
        snackbar.show();
    }
}