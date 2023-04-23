import entities.Book;
import entities.Borrow;
import entities.Card;
import org.junit.Test;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import queries.*;
import utils.ConnectConfig;
import utils.DatabaseConnector;
import utils.RandomData;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyLibraryTest {
    private DatabaseConnector connector;
    private LibraryManagementSystem library;

    private static ConnectConfig connectConfig = null;

    static {
        try {
            // parse connection config from "resources/application.yaml"
            connectConfig = new ConnectConfig();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public MyLibraryTest() {
        try {
            // connect to database
            connector = new DatabaseConnector(connectConfig);
            library = new LibraryManagementSystemImpl(connector);
            System.out.println("Successfully init class BookTest.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    @Before
    public void prepareTest() {
        boolean connStatus = connector.connect();
        Assert.assertTrue(connStatus);
        System.out.println("Successfully connect to database.");
        ApiResult result = library.resetDatabase();
        if (!result.ok) {
            System.out.printf("Failed to reset database, reason: %s\n", result.message);
            Assert.fail();
        }
        System.out.println("Successfully reset database.");
    }

    @After
    public void afterTest() {
        boolean releaseStatus = connector.release();
        if (releaseStatus) {
            System.out.println("Successfully release database connection.");
        } else {
            System.out.println("Failed to release database connection.");
        }
    }
    @Test
    public void myBookRegisterTest() {
        Book b0 = new Book("Computer Science", "Database System Concepts",
                "Machine Industry Press", 2023, "Mike", 188.88, 10);
        library.storeBook(b0);
    }
    @Test
    public void myUpdateStockTest() {
        Book b0 = new Book("Computer Science", "Database System Concepts",
                "Machine Industry Press", 2023, "Mike", 188.88, 10);
        library.storeBook(b0);
        // X = 6
        library.incBookStock(1, 6);
        ApiResult queryResult = library.queryBook(new BookQueryConditions());
        Assert.assertTrue(queryResult.ok);
        BookQueryResults selectedResults = (BookQueryResults) queryResult.payload;
        Book o1 = selectedResults.getResults().get(0);
        Assert.assertEquals(o1.getStock(),16);
        library.incBookStock(1, 1-o1.getStock());
    }
    @Test
    public void myModifyInfoTest() {
        Book b0 = new Book("Computer Science", "Database System Concepts",
                "Machine Industry Press", 2023, "Mike", 188.88, 1);
        library.storeBook(b0);
        int mask = RandomUtils.nextInt(0, 128);
        b0.setPress(RandomData.randomPress());
        b0.setPublishYear(RandomData.randomPublishYear());
        b0.setAuthor(RandomData.randomAuthor());
        b0.setPrice(RandomData.randomPrice());
        b0.setStock(RandomData.randomStock());
        library.modifyBookInfo(b0);
    }
    @Test
    public void myBulkRegisterTest() throws IOException {
        List<Book> books = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader("book.txt"));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            String category = parts[0].trim();
            String title = parts[1].trim();
            String press = parts[2].trim();
            int year = Integer.parseInt(parts[3].trim());
            String author = parts[4].trim();
            double price = Double.parseDouble(parts[5].trim());
            int stock = Integer.parseInt(parts[6].trim());
            Book book = new Book(category, title, press, year, author, price, stock);
            System.out.println(book.toString());
            books.add(book);
        }
        library.storeBook(books);
    }
    @Test
    public void myRegisterCardTest() {
        Card card = new Card();
        card.setType(Card.CardType.Student);
        card.setName("itolfy");
        card.setDepartment("CS");
        library.registerCard(card);
    }
    @Test
    public void myShowCardsTest() {
        Card card1 = new Card();
        card1.setType(Card.CardType.Student);
        card1.setName("PaperCloud");
        card1.setDepartment("CS");
        library.registerCard(card1);
        Card card2 = new Card();
        card2.setType(Card.CardType.Student);
        card2.setName("fr20011021");
        card2.setDepartment("Math");
        library.registerCard(card2);
        CardList resCardList = (CardList) library.showCards().payload;
        for (int i = 0; i < resCardList.getCount(); i++) {
            Card card = resCardList.getCards().get(i);
            System.out.println(card.toString());
        }
    }
    @Test
    public void myBorrowBookTest() {
        Card card1 = new Card();
        card1.setType(Card.CardType.Student);
        card1.setName("PaperCloud");
        card1.setDepartment("CS");
        library.registerCard(card1);
        Book b0 = new Book("Computer Science", "Database System Concepts",
                "Machine Industry Press", 2023, "Mike", 188.88, 10);
        library.storeBook(b0);
        Book b1 = new Book("Math", "Real Analysis",
                "Unknown", 2023, "Folland", 100.0, 10);
        library.storeBook(b0);
        library.storeBook(b1);
        Borrow borrow = new Borrow(1, 1);
        borrow.setBorrowTime(20230423);
        Assert.assertTrue(library.borrowBook(borrow).ok);
        Assert.assertFalse(library.borrowBook(borrow).ok);
        Borrow borrow2 = new Borrow(2, 1);
        borrow2.setBorrowTime(20230423);
        Assert.assertTrue(library.borrowBook(borrow2).ok);
    }
    @Test
    public void myReturnBookTest() {
        Card card1 = new Card();
        card1.setType(Card.CardType.Student);
        card1.setName("PaperCloud");
        card1.setDepartment("CS");
        library.registerCard(card1);
        Book b0 = new Book("Computer Science", "Database System Concepts",
                "Machine Industry Press", 2023, "Mike", 188.88, 10);
        library.storeBook(b0);
        Book b1 = new Book("Math", "Real Analysis",
                "Unknown", 2023, "Folland", 100.0, 10);
        library.storeBook(b0);
        library.storeBook(b1);
        Borrow borrow = new Borrow(1, 1);
        borrow.setBorrowTime(20230423);
        Assert.assertTrue(library.borrowBook(borrow).ok);
        Assert.assertFalse(library.borrowBook(borrow).ok);
        Borrow borrow2 = new Borrow(2, 1);
        borrow2.setBorrowTime(20230423);
        Assert.assertTrue(library.borrowBook(borrow2).ok);
        borrow.setReturnTime(20230424);
        Assert.assertTrue(library.returnBook(borrow).ok);
    }
    @Test
    public void myShowBorrowHistoryTest() {
        Card card1 = new Card();
        card1.setType(Card.CardType.Student);
        card1.setName("PaperCloud");
        card1.setDepartment("CS");
        library.registerCard(card1);
        Book b0 = new Book("Computer Science", "Database System Concepts",
                "Machine Industry Press", 2023, "Mike", 188.88, 10);
        library.storeBook(b0);
        Book b1 = new Book("Math", "Real Analysis",
                "Unknown", 2023, "Folland", 100.0, 10);
        library.storeBook(b0);
        library.storeBook(b1);
        Borrow borrow = new Borrow(1, 1);
        borrow.setBorrowTime(20230423);
        Assert.assertTrue(library.borrowBook(borrow).ok);
        Assert.assertFalse(library.borrowBook(borrow).ok);
        Borrow borrow2 = new Borrow(2, 1);
        borrow2.setBorrowTime(20230423);
        Assert.assertTrue(library.borrowBook(borrow2).ok);
        borrow.setReturnTime(20230424);
        Assert.assertTrue(library.returnBook(borrow).ok);
        ApiResult result = library.showBorrowHistory(1);
        Assert.assertTrue(result.ok);
        BorrowHistories histories = (BorrowHistories) result.payload;
        for(int i = 0; i < histories.getCount(); ++i) {
            BorrowHistories.Item item = histories.getItems().get(i);
            System.out.println(item);
        }
    }
    @Test
    public void MyQueryBookTest() {
        /* Add some books to database*/
        MyLibrary my = MyLibrary.createLibrary(library, 10, 0, 0);
        /* All books */
        System.out.println("Book set : ");
        BookQueryConditions all = new BookQueryConditions();
        BookQueryResults all_books = (BookQueryResults) library.queryBook(all).payload;
        for(int i = 0; i < all_books.getCount(); ++i) {
            Book book = all_books.getResults().get(i);
            System.out.println(book.toString());
        }
        /* Filter : publish year */
        System.out.println("2008 - ?");
        BookQueryConditions a = new BookQueryConditions();
        a.setMinPublishYear(2008);
        BookQueryResults after_2008 = (BookQueryResults) library.queryBook(a).payload;
        for(int i = 0; i < after_2008.getCount(); ++i) {
            Book book = after_2008.getResults().get(i);
            System.out.println(book.toString());
        }

        /*Filter : press (fuzzy matching) */
        /* Order by Stock DESC */
        System.out.println("Press");
        BookQueryConditions b = new BookQueryConditions();
        b.setPress("Press");
        b.setSortBy(Book.SortColumn.STOCK);
        b.setSortOrder(SortOrder.DESC);
        BookQueryResults press = (BookQueryResults) library.queryBook(b).payload;
        for(int i = 0; i < press.getCount(); ++i) {
            Book book = press.getResults().get(i);
            System.out.println(book.toString());
        }
    }
}
