package com.xdearboy.bookkeeper.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BookApiService {
        @GET("volumes")
        Call<BookApiResponse> searchBooks(
                        @Query("q") String query,
                        @Query("maxResults") int maxResults,
                        @Query("key") String key);

        @GET("volumes")
        Call<BookApiResponse> searchBooksWithPagination(
                        @Query("q") String query,
                        @Query("maxResults") int maxResults,
                        @Query("startIndex") int startIndex,
                        @Query("key") String key);

        @GET("volumes")
        Call<BookApiResponse> searchBooksByLanguage(
                        @Query("q") String query,
                        @Query("maxResults") int maxResults,
                        @Query("langRestrict") String langRestrict,
                        @Query("key") String key);

        @GET("volumes/{volumeId}")
        Call<BookApiResponse.Item> getBookById(
                        @Path("volumeId") String volumeId,
                        @Query("key") String key);

        @GET("volumes")
        Call<BookApiResponse> searchBooksWithPaginationAndLanguage(
                        @Query("q") String query,
                        @Query("maxResults") int maxResults,
                        @Query("startIndex") int startIndex,
                        @Query("langRestrict") String langRestrict,
                        @Query("key") String key);
}