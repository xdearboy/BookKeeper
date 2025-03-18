package com.xdearboy.bookkeeper.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.adapter.BookAdapter;
import com.xdearboy.bookkeeper.databinding.FragmentDashboardBinding;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.ui.BookDetailsActivity;

import java.util.ArrayList;

public class DashboardFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private BookAdapter bookAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupToolbar();
        setupRecyclerView();
        setupProfileButton();
        observeViewModel();

        return root;
    }

    private void setupToolbar() {
        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.toolbar);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.borrowedBooksRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        bookAdapter = new BookAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(bookAdapter);
    }

    private void setupProfileButton() {
        binding.editProfileButton.setOnClickListener(v -> {
            // TODO: Открыть экран редактирования профиля
            Toast.makeText(getContext(), "Редактирование профиля", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        dashboardViewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::updateUserUI);
        dashboardViewModel.getBorrowedBooks().observe(getViewLifecycleOwner(), this::updateBorrowedBooksUI);
    }

    private void updateUserUI(User user) {
        if (user != null) {
            binding.profileName.setText(user.getName());
            binding.profileEmail.setText(user.getEmail());
            
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(binding.profileImage);
            }
        }
    }

    private void updateBorrowedBooksUI(java.util.List<Book> books) {
        bookAdapter.updateBooks(books);
        
        if (books.isEmpty()) {
            binding.emptyBorrowedBooksView.setVisibility(View.VISIBLE);
            binding.borrowedBooksRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyBorrowedBooksView.setVisibility(View.GONE);
            binding.borrowedBooksRecyclerView.setVisibility(View.VISIBLE);
        }
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
}