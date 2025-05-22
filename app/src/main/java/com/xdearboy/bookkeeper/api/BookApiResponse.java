package com.xdearboy.bookkeeper.api;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class BookApiResponse {
    @SerializedName("items")
    private List<Item> items;
    @SerializedName("totalItems")
    private int totalItems;
    public List<Item> getItems() {
        return items;
    }
    public int getTotalItems() {
        return totalItems;
    }
    public static class Item {
        @SerializedName("id")
        private String id;
        @SerializedName("volumeInfo")
        private VolumeInfo volumeInfo;
        public String getId() {
            return id;
        }
        public VolumeInfo getVolumeInfo() {
            return volumeInfo;
        }
    }
    public static class VolumeInfo {
        @SerializedName("title")
        private String title;
        @SerializedName("authors")
        private List<String> authors;
        @SerializedName("publisher")
        private String publisher;
        @SerializedName("publishedDate")
        private String publishedDate;
        @SerializedName("description")
        private String description;
        @SerializedName("pageCount")
        private int pageCount;
        @SerializedName("categories")
        private List<String> categories;
        @SerializedName("imageLinks")
        private ImageLinks imageLinks;
        @SerializedName("language")
        private String language;
        @SerializedName("industryIdentifiers")
        private List<IndustryIdentifier> industryIdentifiers;
        public String getTitle() {
            return title;
        }
        public List<String> getAuthors() {
            return authors;
        }
        public String getPublisher() {
            return publisher;
        }
        public String getPublishedDate() {
            return publishedDate;
        }
        public String getDescription() {
            return description;
        }
        public int getPageCount() {
            return pageCount;
        }
        public List<String> getCategories() {
            return categories;
        }
        public ImageLinks getImageLinks() {
            return imageLinks;
        }
        public String getLanguage() {
            return language;
        }
        public List<IndustryIdentifier> getIndustryIdentifiers() {
            return industryIdentifiers;
        }
    }
    public static class ImageLinks {
        @SerializedName("smallThumbnail")
        private String smallThumbnail;
        @SerializedName("thumbnail")
        private String thumbnail;
        @SerializedName("small")
        private String small;
        @SerializedName("medium")
        private String medium;
        @SerializedName("large")
        private String large;
        @SerializedName("extraLarge")
        private String extraLarge;
        public String getSmallThumbnail() {
            return smallThumbnail;
        }
        public String getThumbnail() {
            return thumbnail;
        }
        public String getSmall() {
            return small;
        }
        public String getMedium() {
            return medium;
        }
        public String getLarge() {
            return large;
        }
        public String getExtraLarge() {
            return extraLarge;
        }
    }
    public static class IndustryIdentifier {
        @SerializedName("type")
        private String type;
        @SerializedName("identifier")
        private String identifier;
        public String getType() {
            return type;
        }
        public String getIdentifier() {
            return identifier;
        }
    }
} 