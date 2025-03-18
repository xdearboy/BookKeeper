package com.xdearboy.bookkeeper.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Интерфейс для работы с API книг Google Books
 */
public interface BookApiService {
    
    /**
     * Поиск книг
     * @param query Поисковый запрос
     * @param maxResults Максимальное количество результатов
     * @param key API ключ
     * @return Ответ API
     */
    @GET("volumes")
    Call<BookApiResponse> searchBooks(
            @Query("q") String query, 
            @Query("maxResults") int maxResults,
            @Query("key") String key);
    
    /**
     * Поиск книг с пагинацией
     * @param query Поисковый запрос
     * @param maxResults Максимальное количество результатов
     * @param startIndex Начальный индекс для пагинации
     * @param key API ключ
     * @return Ответ API
     */
    @GET("volumes")
    Call<BookApiResponse> searchBooksWithPagination(
            @Query("q") String query, 
            @Query("maxResults") int maxResults, 
            @Query("startIndex") int startIndex,
            @Query("key") String key);
    
    /**
     * Поиск книг с фильтрацией по языку
     * @param query Поисковый запрос
     * @param maxResults Максимальное количество результатов
     * @param langRestrict Ограничение по языку (например, "ru" для русского)
     * @param key API ключ
     * @return Ответ API
     */
    @GET("volumes")
    Call<BookApiResponse> searchBooksByLanguage(
            @Query("q") String query, 
            @Query("maxResults") int maxResults, 
            @Query("langRestrict") String langRestrict,
            @Query("key") String key);
    
    /**
     * Получение информации о книге по ID
     * @param volumeId ID книги
     * @param key API ключ
     * @return Ответ API
     */
    @GET("volumes/{volumeId}")
    Call<BookApiResponse.Item> getBookById(
            @Path("volumeId") String volumeId,
            @Query("key") String key);
    
    /**
     * Поиск книг с пагинацией и фильтрацией по языку
     * @param query Поисковый запрос
     * @param maxResults Максимальное количество результатов
     * @param startIndex Начальный индекс для пагинации
     * @param langRestrict Ограничение по языку (например, "ru" для русского)
     * @param key API ключ
     * @return Ответ API
     */
    @GET("volumes")
    Call<BookApiResponse> searchBooksWithPaginationAndLanguage(
            @Query("q") String query, 
            @Query("maxResults") int maxResults, 
            @Query("startIndex") int startIndex,
            @Query("langRestrict") String langRestrict,
            @Query("key") String key);
} 