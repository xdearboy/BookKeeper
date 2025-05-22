package com.xdearboy.bookkeeper.adapter;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.model.Book;
import java.util.ArrayList;
import java.util.List;
public class BookAdapter extends ListAdapter<Book, BookAdapter.BookViewHolder> {
    private final Context context;
    private final OnBookClickListener listener;
    private final List<Book> books = new ArrayList<>();
    private static final RequestOptions GLIDE_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .placeholder(R.drawable.book_placeholder)
            .error(R.drawable.book_placeholder);
    public interface OnBookClickListener {
        void onBookClick(Book book);
    }
    public BookAdapter(Context context, List<Book> books, OnBookClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
        this.books.addAll(books);
        submitList(new ArrayList<>(books));
    }
    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = getItem(position);
        if (book != null) {
            holder.bind(book);
        } else {
            Log.e("BookAdapter", "onBindViewHolder: книга на позиции " + position + " равна null");
        }
    }
    public List<Book> getBooks() {
        return getCurrentList();
    }
    public void updateBooks(List<Book> newBooks) {
        if (newBooks == null) {
            Log.e("BookAdapter", "updateBooks: получен null список");
            return;
        }
        Log.d("BookAdapter", "updateBooks: получено " + newBooks.size() + " книг");
        this.books.clear();
        this.books.addAll(newBooks);
        submitList(new ArrayList<>(newBooks));
        Log.d("BookAdapter", "updateBooks: обновлено " + this.books.size() + " книг");
    }
    class BookViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView bookCover;
        private final TextView bookTitle;
        private final TextView bookAuthor;
        private final TextView bookGenre;
        private final Chip bookStatus;
        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            bookCover = itemView.findViewById(R.id.book_cover);
            bookTitle = itemView.findViewById(R.id.book_title);
            bookAuthor = itemView.findViewById(R.id.book_author);
            bookGenre = itemView.findViewById(R.id.book_genre);
            bookStatus = itemView.findViewById(R.id.book_status);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBookClick(getItem(position));
                }
            });
        }
        public void bind(Book book) {
            bookTitle.setText(book.getTitle());
            bookAuthor.setText(book.getAuthor());
            bookGenre.setText(book.getGenre());
            if (book.isBorrowed()) {
                bookStatus.setText(R.string.book_borrowed);
                bookStatus.setChipBackgroundColorResource(R.color.borrowed);
            } else {
                bookStatus.setText(R.string.book_available);
                bookStatus.setChipBackgroundColorResource(R.color.available);
            }
            String coverUrl = book.getCoverImageUrl();
            if (coverUrl != null && coverUrl.startsWith("http")) {
                Glide.with(context)
                        .load(coverUrl)
                        .apply(GLIDE_OPTIONS)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                    Target<Drawable> target, boolean isFirstResource) {
                                Log.w("BookAdapter", "Ошибка загрузки обложки: " + coverUrl, e);
                                return false;
                            }
                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                    Target<Drawable> target, DataSource dataSource,
                                    boolean isFirstResource) {
                                Log.d("BookAdapter", "Обложка успешно загружена: " + coverUrl);
                                return false;
                            }
                        })
                        .into(bookCover);
            } else {
                bookCover.setImageResource(R.drawable.book_placeholder);
            }
        }
    }
    private static final DiffUtil.ItemCallback<Book> DIFF_CALLBACK = new DiffUtil.ItemCallback<Book>() {
        @Override
        public boolean areItemsTheSame(@NonNull Book oldItem, @NonNull Book newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Book oldItem, @NonNull Book newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getAuthor().equals(newItem.getAuthor()) &&
                    oldItem.isBorrowed() == newItem.isBorrowed() &&
                    (oldItem.getCoverImageUrl() == null && newItem.getCoverImageUrl() == null ||
                            oldItem.getCoverImageUrl() != null
                                    && oldItem.getCoverImageUrl().equals(newItem.getCoverImageUrl()));
        }
    };
}