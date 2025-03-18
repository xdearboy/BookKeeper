package com.xdearboy.bookkeeper.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final Context context;
    private List<Category> categories;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, boolean isSelected);
    }

    public CategoryAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView categoryCard;
        private final ImageView categoryIcon;
        private final TextView categoryName;
        private final TextView categoryDescription;
        private final CheckBox categoryCheckbox;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryCard = itemView.findViewById(R.id.category_card);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            categoryName = itemView.findViewById(R.id.category_name);
            categoryDescription = itemView.findViewById(R.id.category_description);
            categoryCheckbox = itemView.findViewById(R.id.category_checkbox);

            categoryCard.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Category category = categories.get(position);
                    boolean newState = !category.isSelected();
                    category.setSelected(newState);
                    categoryCard.setChecked(newState);
                    categoryCheckbox.setChecked(newState);
                    listener.onCategoryClick(category, newState);
                }
            });
        }

        void bind(Category category) {
            categoryName.setText(category.getName());
            categoryDescription.setText(category.getDescription());
            
            // Загрузка иконки категории
            if (category.getIconUrl() != null && !category.getIconUrl().isEmpty()) {
                Glide.with(context)
                        .load(category.getIconUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_book_placeholder)
                                .error(R.drawable.ic_book_placeholder))
                        .into(categoryIcon);
            } else {
                categoryIcon.setImageResource(R.drawable.ic_book_placeholder);
            }
            
            // Установка состояния выбора
            boolean isSelected = category.isSelected();
            categoryCard.setChecked(isSelected);
            categoryCheckbox.setChecked(isSelected);
        }
    }
} 