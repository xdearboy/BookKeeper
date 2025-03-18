package com.xdearboy.bookkeeper.util;

import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.model.Notification;
import com.xdearboy.bookkeeper.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Класс для инициализации базы данных тестовыми данными
 */
public class DatabaseInitializer {
    
    public static void populateDatabase(AppDatabase db) {
        // Создаем тестового пользователя
        User user = createTestUser();
        db.userDao().insert(user);
        
        // Создаем тестовые книги
        List<Book> books = createTestBooks();
        db.bookDao().insertAll(books);
        
        // Создаем тестовые уведомления
        List<Notification> notifications = createTestNotifications(user.getId(), books);
        db.notificationDao().insertAll(notifications);
    }
    
    private static User createTestUser() {
        User user = new User();
        user.setId("user_" + UUID.randomUUID().toString());
        user.setName("Иван Иванов");
        user.setEmail("ivan@example.com");
        user.setPassword(PasswordUtils.hashPassword("password123")); // Хешируем пароль
        user.setProfileImageUrl("https://i.pravatar.cc/150?img=3");
        user.setRegistrationDate(new Date());
        user.setLastLoginDate(new Date());
        user.setFavoriteGenres(Arrays.asList("Фантастика", "Детектив"));
        user.setBorrowedBookIds(new ArrayList<>());
        user.setActive(true);
        return user;
    }
    
    private static List<Book> createTestBooks() {
        List<Book> books = new ArrayList<>();
        
        // Книга 1
        Book book1 = new Book();
        book1.setId("book_" + UUID.randomUUID().toString());
        book1.setTitle("Война и мир");
        book1.setAuthor("Лев Толстой");
        book1.setGenre("Роман");
        book1.setDescription("Роман-эпопея Льва Николаевича Толстого, описывающий русское общество в эпоху войн против Наполеона в 1805—1812 годах.");
        book1.setCoverImageUrl("https://m.media-amazon.com/images/I/71TzI9Hc+GL._AC_UF1000,1000_QL80_.jpg");
        book1.setBorrowed(false);
        book1.setIsbn("9785171147112");
        book1.setPageCount(1300);
        book1.setPublisher("АСТ");
        book1.setPublishDate(new Date(70, 0, 1)); // 1970 год
        book1.setLanguage("Русский");
        book1.setFromApi(false);
        books.add(book1);
        
        // Книга 2
        Book book2 = new Book();
        book2.setId("book_" + UUID.randomUUID().toString());
        book2.setTitle("Преступление и наказание");
        book2.setAuthor("Федор Достоевский");
        book2.setGenre("Роман");
        book2.setDescription("Социально-психологический и социально-философский роман Фёдора Михайловича Достоевского, над которым писатель работал в 1865—1866 годах.");
        book2.setCoverImageUrl("https://m.media-amazon.com/images/I/81EcXiV-9WL._AC_UF1000,1000_QL80_.jpg");
        book2.setBorrowed(false);
        book2.setIsbn("9785171147129");
        book2.setPageCount(672);
        book2.setPublisher("АСТ");
        book2.setPublishDate(new Date(66, 0, 1)); // 1966 год
        book2.setLanguage("Русский");
        book2.setFromApi(false);
        books.add(book2);
        
        // Книга 3
        Book book3 = new Book();
        book3.setId("book_" + UUID.randomUUID().toString());
        book3.setTitle("Мастер и Маргарита");
        book3.setAuthor("Михаил Булгаков");
        book3.setGenre("Фантастика");
        book3.setDescription("Роман Михаила Афанасьевича Булгакова, работа над которым началась в конце 1920-х годов и продолжалась вплоть до смерти писателя.");
        book3.setCoverImageUrl("https://m.media-amazon.com/images/I/81RGKN7-KML._AC_UF1000,1000_QL80_.jpg");
        book3.setBorrowed(false);
        book3.setIsbn("9785171147136");
        book3.setPageCount(480);
        book3.setPublisher("АСТ");
        book3.setPublishDate(new Date(67, 0, 1)); // 1967 год
        book3.setLanguage("Русский");
        book3.setFromApi(false);
        books.add(book3);
        
        // Книга 4
        Book book4 = new Book();
        book4.setId("book_" + UUID.randomUUID().toString());
        book4.setTitle("Гарри Поттер и философский камень");
        book4.setAuthor("Джоан Роулинг");
        book4.setGenre("Фэнтези");
        book4.setDescription("Первый роман в серии книг про юного волшебника Гарри Поттера, написанный Дж. К. Роулинг.");
        book4.setCoverImageUrl("https://m.media-amazon.com/images/I/81iqZ2HHD-L._AC_UF1000,1000_QL80_.jpg");
        book4.setBorrowed(false);
        book4.setIsbn("9785389077881");
        book4.setPageCount(432);
        book4.setPublisher("Росмэн");
        book4.setPublishDate(new Date(97, 5, 26)); // 26 июня 1997 года
        book4.setLanguage("Русский");
        book4.setFromApi(false);
        books.add(book4);
        
        // Книга 5
        Book book5 = new Book();
        book5.setId("book_" + UUID.randomUUID().toString());
        book5.setTitle("1984");
        book5.setAuthor("Джордж Оруэлл");
        book5.setGenre("Антиутопия");
        book5.setDescription("Роман-антиутопия Джорджа Оруэлла, изданный в 1949 году.");
        book5.setCoverImageUrl("https://m.media-amazon.com/images/I/71kxa1-0mfL._AC_UF1000,1000_QL80_.jpg");
        book5.setBorrowed(false);
        book5.setIsbn("9785389097865");
        book5.setPageCount(328);
        book5.setPublisher("АСТ");
        book5.setPublishDate(new Date(49, 5, 8)); // 8 июня 1949 года
        book5.setLanguage("Русский");
        book5.setFromApi(false);
        books.add(book5);
        
        return books;
    }
    
    private static List<Notification> createTestNotifications(String userId, List<Book> books) {
        List<Notification> notifications = new ArrayList<>();
        
        // Уведомление 1
        Notification notification1 = new Notification();
        notification1.setId("notification_" + UUID.randomUUID().toString());
        notification1.setUserId(userId);
        notification1.setTitle("Добро пожаловать в BookKeeper!");
        notification1.setMessage("Спасибо за регистрацию в нашем приложении. Здесь вы можете найти и взять книги из нашей библиотеки.");
        notification1.setType(Notification.NotificationType.NEW_BOOK);
        notification1.setRead(false);
        notification1.setCreatedAt(new Date());
        notifications.add(notification1);
        
        // Уведомление 2
        if (books.size() >= 1) {
            Notification notification2 = new Notification();
            notification2.setId("notification_" + UUID.randomUUID().toString());
            notification2.setUserId(userId);
            notification2.setTitle("Новая книга в библиотеке");
            notification2.setMessage("В нашей библиотеке появилась новая книга: " + books.get(0).getTitle() + " от " + books.get(0).getAuthor());
            notification2.setType(Notification.NotificationType.NEW_BOOK);
            notification2.setRead(false);
            notification2.setCreatedAt(new Date(System.currentTimeMillis() - 86400000)); // Вчера
            notification2.setBookId(books.get(0).getId());
            notifications.add(notification2);
        }
        
        // Уведомление 3
        if (books.size() >= 2) {
            Notification notification3 = new Notification();
            notification3.setId("notification_" + UUID.randomUUID().toString());
            notification3.setUserId(userId);
            notification3.setTitle("Рекомендация книги");
            notification3.setMessage("Рекомендуем вам прочитать книгу: " + books.get(1).getTitle() + " от " + books.get(1).getAuthor());
            notification3.setType(Notification.NotificationType.RECOMMENDATION);
            notification3.setRead(true);
            notification3.setCreatedAt(new Date(System.currentTimeMillis() - 172800000)); // 2 дня назад
            notification3.setBookId(books.get(1).getId());
            notifications.add(notification3);
        }
        
        return notifications;
    }
} 