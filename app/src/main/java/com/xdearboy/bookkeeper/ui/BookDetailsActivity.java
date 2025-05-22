package com.xdearboy.bookkeeper.ui;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.repository.BookRepository;
import com.xdearboy.bookkeeper.repository.UserRepository;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.util.SessionManager;
import java.text.SimpleDateFormat;
import java.util.Locale;
public class BookDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_BOOK = "extra_book";
    private ImageView bookCover;
    private TextView bookTitle;
    private TextView bookAuthor;
    private TextView bookDescription;
    private TextView bookGenre;
    private TextView bookPublishDate;
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialButton borrowButton;
    private Chip bookGenreChip;
    private Book book;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_details);
        initViews();
        setupToolbar();
        book = getIntent().getParcelableExtra(EXTRA_BOOK);
        if (book != null) {
            displayBookDetails(book);
            setupBorrowButton();
        }
    }
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        bookCover = findViewById(R.id.bookCover);
        bookTitle = findViewById(R.id.bookTitle);
        bookAuthor = findViewById(R.id.bookAuthor);
        bookDescription = findViewById(R.id.bookDescription);
        bookGenre = findViewById(R.id.bookGenre);
        bookPublishDate = findViewById(R.id.bookPublishDate);
        borrowButton = findViewById(R.id.borrowButton);
        bookGenreChip = findViewById(R.id.bookGenreChip);
    }
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.book_details);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    private void displayBookDetails(Book book) {
        if (book.getCoverImageUrl() != null && !book.getCoverImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(book.getCoverImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_book_placeholder)
                            .error(R.drawable.ic_book_placeholder))
                    .into(bookCover);
        } else {
            bookCover.setImageResource(R.drawable.ic_book_placeholder);
        }
        collapsingToolbar.setTitle(book.getTitle());
        bookTitle.setText(book.getTitle());
        bookAuthor.setText(book.getAuthor());
        String description = book.getDescription();
        if (description != null && !description.isEmpty()) {
            bookDescription.setText(description);
            bookDescription.setVisibility(View.VISIBLE);
        } else {
            bookDescription.setText(R.string.no_description);
            bookDescription.setVisibility(View.VISIBLE);
        }
        bookGenre.setText(getString(R.string.genre_format, book.getGenre()));
        if (book.getPublishDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
            String formattedDate = dateFormat.format(book.getPublishDate());
            bookPublishDate.setText(getString(R.string.publish_date_format, formattedDate));
        } else {
            bookPublishDate.setText(getString(R.string.publish_date_format, "Неизвестно"));
        }
        if (book.getGenre() != null && !book.getGenre().isEmpty()) {
            bookGenreChip.setText(book.getGenre());
            bookGenreChip.setVisibility(View.VISIBLE);
        } else {
            bookGenreChip.setVisibility(View.GONE);
        }
    }
    private void setupBorrowButton() {
        String userId = SessionManager.getInstance(this).getUserId();
        BookRepository bookRepo = BookRepository.getInstance(getApplication());
        UserRepository userRepo = UserRepository.getInstance(getApplication());
        if (book.isBorrowed()) {
            borrowButton.setText(R.string.return_book);
            borrowButton.setIcon(getDrawable(R.drawable.ic_book_placeholder));
        } else {
            borrowButton.setText(R.string.borrow_book);
            borrowButton.setIcon(getDrawable(R.drawable.ic_book_placeholder));
        }
        borrowButton.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (book.isBorrowed()) {
                bookRepo.returnBook(book.getId());
                User user = userRepo.getCurrentUser();
                if (user != null) {
                    user.removeBorrowedBook(book.getId());
                    userRepo.updateUser(user);
                }
                Toast.makeText(this, "Книга возвращена", Toast.LENGTH_SHORT).show();
                borrowButton.setText(R.string.borrow_book);
            } else {
                long returnDate = now + 14L * 24 * 60 * 60 * 1000; // 14 дней
                bookRepo.borrowBook(book.getId(), userId, now, returnDate);
                User user = userRepo.getCurrentUser();
                if (user != null) {
                    user.addBorrowedBook(book.getId());
                    userRepo.updateUser(user);
                }
                Toast.makeText(this, "Книга взята", Toast.LENGTH_SHORT).show();
                borrowButton.setText(R.string.return_book);
            }
            book.setBorrowed(!book.isBorrowed());
        });
    }
} 