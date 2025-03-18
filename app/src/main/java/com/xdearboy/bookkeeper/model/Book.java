package com.xdearboy.bookkeeper.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.xdearboy.bookkeeper.database.converters.DateConverter;

import java.util.Date;

/**
 * Модель данных для книги
 */
@Entity(tableName = "books")
@TypeConverters(DateConverter.class)
public class Book implements Parcelable {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String author;
    private String genre;
    private String description;
    private String coverImageUrl;
    private boolean borrowed;
    private String borrowedBy;
    private Date borrowDate;
    private Date returnDate;
    private String isbn;
    private int pageCount;
    private String publisher;
    private Date publishDate;
    private String language;
    private boolean isFromApi;

    public Book() {
        // Пустой конструктор для Room
    }

    @Ignore
    public Book(@NonNull String id, String title, String author, String genre, String description, 
                String coverImageUrl, boolean borrowed, String borrowedBy, Date borrowDate, 
                Date returnDate, String isbn, int pageCount, String publisher, Date publishDate, 
                String language, boolean isFromApi) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.borrowed = borrowed;
        this.borrowedBy = borrowedBy;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
        this.isbn = isbn;
        this.pageCount = pageCount;
        this.publisher = publisher;
        this.publishDate = publishDate;
        this.language = language;
        this.isFromApi = isFromApi;
    }

    protected Book(Parcel in) {
        id = in.readString();
        title = in.readString();
        author = in.readString();
        genre = in.readString();
        description = in.readString();
        coverImageUrl = in.readString();
        borrowed = in.readByte() != 0;
        borrowedBy = in.readString();
        long tmpBorrowDate = in.readLong();
        borrowDate = tmpBorrowDate != -1 ? new Date(tmpBorrowDate) : null;
        long tmpReturnDate = in.readLong();
        returnDate = tmpReturnDate != -1 ? new Date(tmpReturnDate) : null;
        isbn = in.readString();
        pageCount = in.readInt();
        publisher = in.readString();
        long tmpPublishDate = in.readLong();
        publishDate = tmpPublishDate != -1 ? new Date(tmpPublishDate) : null;
        language = in.readString();
        isFromApi = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(genre);
        dest.writeString(description);
        dest.writeString(coverImageUrl);
        dest.writeByte((byte) (borrowed ? 1 : 0));
        dest.writeString(borrowedBy);
        dest.writeLong(borrowDate != null ? borrowDate.getTime() : -1);
        dest.writeLong(returnDate != null ? returnDate.getTime() : -1);
        dest.writeString(isbn);
        dest.writeInt(pageCount);
        dest.writeString(publisher);
        dest.writeLong(publishDate != null ? publishDate.getTime() : -1);
        dest.writeString(language);
        dest.writeByte((byte) (isFromApi ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public boolean isBorrowed() {
        return borrowed;
    }

    public void setBorrowed(boolean borrowed) {
        this.borrowed = borrowed;
    }

    public String getBorrowedBy() {
        return borrowedBy;
    }

    public void setBorrowedBy(String borrowedBy) {
        this.borrowedBy = borrowedBy;
    }

    public Date getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(Date borrowDate) {
        this.borrowDate = borrowDate;
    }

    public Date getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isFromApi() {
        return isFromApi;
    }

    public void setFromApi(boolean fromApi) {
        isFromApi = fromApi;
    }
} 