import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;
    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    /*
        图书入库模块。向图书库中注册(添加)一本新书，并返回新书的书号。
        如果该书已经存在于图书库中，那么入库操作将失败。
        当且仅当书的<类别, 书名, 出版社, 年份, 作者>均相同时，
        才认为两本书相同。请注意，插入完成后，
        需要根据数据库中自增列生成的book_id去更新book对象里的book_id。
    */
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try{
            String category = book.getCategory();
            String title = book.getTitle();
            String press = book.getPress();
            int publishYear = book.getPublishYear();
            String author = book.getAuthor();
            double price = book.getPrice();
            int stock = book.getStock();

            String que_exist = "SELECT * FROM book WHERE " +
                    "category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";

            PreparedStatement que_stmt = conn.prepareStatement(que_exist);
            que_stmt.setString(1, category);
            que_stmt.setString(2, title);
            que_stmt.setString(3, press);
            que_stmt.setInt(4, publishYear);
            que_stmt.setString(5, author);
            ResultSet ret = que_stmt.executeQuery();
            if(ret.next()) {
                rollback(conn);
                return new ApiResult(false, "Insertion failed : book already exists.");
            }
            String insert_book = "INSERT INTO book (category, title, press, publish_year, author, price, stock) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement insert_stmt = conn.prepareStatement(insert_book, Statement.RETURN_GENERATED_KEYS);
            insert_stmt.setString(1, category);
            insert_stmt.setString(2, title);
            insert_stmt.setString(3, press);
            insert_stmt.setInt(4, publishYear);
            insert_stmt.setString(5, author);
            insert_stmt.setDouble(6, price);
            insert_stmt.setInt(7, stock);
            int len = insert_stmt.executeUpdate();
            if(len != 1){
                rollback(conn);
                return new ApiResult(false, "store book failed");
            }
            assert(len == 1);
            commit(conn);
            ResultSet ret2 = insert_stmt.getGeneratedKeys();
            if(ret2.next()) {
                book.setBookId(ret2.getInt(1));
            }
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully storing a book.");
    }

    /*
        图书增加库存模块。为图书库中的某一本书增加库存。
        其中库存增量deltaStock可正可负，若为负数，则需要保证最终库存是一个非负数。
     */
    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            String query_sql = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement que_stmt = conn.prepareStatement(query_sql);
            que_stmt.setInt(1, bookId);
            ResultSet ret = que_stmt.executeQuery();
            if(!ret.next()) {
                rollback(conn);
                return new ApiResult(false, "Book not exist");
            }
            int new_stock = ret.getInt("stock") + deltaStock;

            if(new_stock < 0) {
                rollback(conn);
                return new ApiResult(false, "new_stock < 0");
            }
            assert(new_stock >= 0);
            String upd_sql = "UPDATE book SET stock = ? WHERE book_id = ?";
            PreparedStatement upd_stmt = conn.prepareStatement(upd_sql);
            upd_stmt.setInt(1, new_stock);
            upd_stmt.setInt(2, bookId);
            int ret2 = upd_stmt.executeUpdate();
            if(ret2 != 1) {
                rollback(conn);
                return new ApiResult(false, "Set stock failed");
            }
            commit(conn);
        }   catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully updating stock.");
    }

    /*
        图书批量入库模块。批量入库图书，
        如果有一本书入库失败，那么就需要回滚整个事务(即所有的书都不能被入库)。
     */
    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try{
            for(int i = 0; i < books.size(); ++i)
                for(int j = i + 1; j < books.size(); ++j) {
                    Book _i = books.get(i);
                    Book _j = books.get(j);
                    if(_i.getTitle() == _j.getTitle()
                      && _i.getAuthor() == _j.getAuthor()
                      && _i.getPress() == _j.getPress()
                      && _i.getCategory() == _j.getCategory()
                      && _i.getPublishYear() == _j.getPublishYear()
                    ) {
                        rollback(conn);
                        return new ApiResult(false, "Insertion failed : book already exists.");
                    }
                }
            for(int i = 0; i < books.size(); ++i) {
                Book book = books.get(i);
                String category = book.getCategory();
                String title = book.getTitle();
                String press = book.getPress();
                int publishYear = book.getPublishYear();
                String author = book.getAuthor();
                double price = book.getPrice();
                int stock = book.getStock();
                String que_exist = "SELECT * FROM book WHERE " +
                        "category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
                PreparedStatement que_stmt = conn.prepareStatement(que_exist);
                que_stmt.setString(1, category);
                que_stmt.setString(2, title);
                que_stmt.setString(3, press);
                que_stmt.setInt(4, publishYear);
                que_stmt.setString(5, author);
                ResultSet ret = que_stmt.executeQuery();
                if(ret.next()) {
                    rollback(conn);
                    return new ApiResult(false, "Insertion failed : book already exists.");
                }
                String insert_book = "INSERT INTO book (category, title, press, publish_year, author, price, stock) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement insert_stmt = conn.prepareStatement(insert_book, Statement.RETURN_GENERATED_KEYS);
                insert_stmt.setString(1, category);
                insert_stmt.setString(2, title);
                insert_stmt.setString(3, press);
                insert_stmt.setInt(4, publishYear);
                insert_stmt.setString(5, author);
                insert_stmt.setDouble(6, price);
                insert_stmt.setInt(7, stock);
                int len = insert_stmt.executeUpdate();
                assert(len == 1);
                ResultSet ret2 = insert_stmt.getGeneratedKeys();
                if(ret2.next()) {
                    book.setBookId(ret2.getInt(1));
                }
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully storing books.");
    }

    /*
        图书删除模块。从图书库中删除一本书。如果还有人尚未归还这本书，那么删除操作将失败。
     */
    @Override
    public ApiResult removeBook(int bookId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    /*
        图书修改模块。修改已入库图书的基本信息，该接口不能修改图书的书号和存量。
     */
    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            String query_sql = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement que_stmt = conn.prepareStatement(query_sql);
            que_stmt.setInt(1, book.getBookId());
            ResultSet ret = que_stmt.executeQuery();
            if(!ret.next()) {
                rollback(conn);
                return new ApiResult(false, "Book not exist");
            }

            String upd_sql = "UPDATE book SET category = ?, title = ?, " +
                    "press = ?, publish_year = ?, " +
                    "author = ?, price = ? " +
                    "WHERE book_id = ?";
            PreparedStatement upd_stmt = conn.prepareStatement(upd_sql);
            upd_stmt.setString(1, book.getCategory());
            upd_stmt.setString(2, book.getTitle());
            upd_stmt.setString(3, book.getPress());
            upd_stmt.setInt(4, book.getPublishYear());
            upd_stmt.setString(5, book.getAuthor());
            upd_stmt.setDouble(6, book.getPrice());
            upd_stmt.setInt(7, book.getBookId());
            int ret2 = upd_stmt.executeUpdate();
            if(ret2 != 1) {
                rollback(conn);
                return new ApiResult(false, "Set information failed");
            }
            commit(conn);
        }   catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully updating stock.");
    }

    /*
        图书查询模块。根据提供的查询条件查询符合条件的图书，并按照指定排序方式排序。
        查询条件包括：类别点查(精确查询)，书名点查(模糊查询)，出版社点查(模糊查询)，
        年份范围查，作者点查(模糊查询)，价格范围差。如果两条记录排序条件的值相等，
        则按book_id升序排序。
     */
    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        try {
            String search_sql = "SELECT * FROM book WHERE " +
                    (conditions.getCategory() == null ? "1=1" : "category = ?") +
                    " AND " +
                    (conditions.getTitle() == null ? "1=1" : "title LIKE ?") +
                    " AND " +
                    (conditions.getPress() == null ? "1=1" : "press LIKE ?") +
                    " AND " +
                    (conditions.getAuthor() == null ? "1=1" : "author LIKE ?") +
                    " AND " +
                    (conditions.getMinPublishYear() == null ? "1=1" : "publish_year >= ?") +
                    " AND " +
                    (conditions.getMaxPublishYear() == null ? "1=1" : "publish_year <= ?") +
                    " AND " +
                    (conditions.getMinPrice() == null ? "1=1" : "price >= ?") +
                    " AND " +
                    (conditions.getMaxPrice() == null ? "1=1" : "price <= ?")
                    + " ORDER BY " + conditions.getSortBy() + " " + conditions.getSortOrder()
                    + ", book_id ASC";

            List<Book> books = new ArrayList<>();
            int index = 0;
            PreparedStatement stmt = conn.prepareStatement(search_sql);
            if(conditions.getCategory() != null) {
                ++index;
                stmt.setString(index, conditions.getCategory());
            }
            if(conditions.getTitle() != null) {
                ++index;
                stmt.setString(index, "%" + conditions.getTitle() + "%");
            }
            if(conditions.getPress() != null) {
                ++index;
                stmt.setString(index, "%" + conditions.getPress() + "%");
            }
            if(conditions.getAuthor() != null) {
                ++index;
                stmt.setString(index, "%" + conditions.getAuthor() + "%");
            }
            if(conditions.getMinPublishYear() != null) {
                ++index;
                stmt.setInt(index, conditions.getMinPublishYear());
            }
            if(conditions.getMaxPublishYear() != null) {
                ++index;
                stmt.setInt(index, conditions.getMaxPublishYear());
            }
            if(conditions.getMinPrice() != null) {
                ++index;
                stmt.setDouble(index, conditions.getMinPrice());
            }
            if(conditions.getMaxPrice() != null) {
                ++index;
                stmt.setDouble(index, conditions.getMaxPrice());
            }
            ResultSet ret = stmt.executeQuery();
            while(ret.next()) {
                Book book = new Book();
                book.setBookId(ret.getInt("book_id"));
                book.setCategory(ret.getString("category"));
                book.setTitle(ret.getString("title"));
                book.setPublishYear(ret.getInt("publish_year"));
                book.setAuthor(ret.getString("author"));
                book.setPress(ret.getString("press"));
                book.setPrice(ret.getDouble("price"));
                book.setStock(ret.getInt("stock"));
                books.add(book);
            }
            return new ApiResult(true, null, new BookQueryResults(books));
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    /*
        借书模块。根据给定的书号、卡号和借书时间添加一条借书记录，
        然后更新库存。若用户此前已经借过这本书但尚未归还，那么借书操作将失败。
     */
    @Override
    public ApiResult borrowBook(Borrow borrow) {
        return new ApiResult(false, "Unimplemented Function");
    }

    /*
        还书模块。根据给定的书号、卡号和还书时间，查询对应的借书记录，
        并补充归还时间，然后更新库存。
     */
    @Override
    public ApiResult returnBook(Borrow borrow) {
        return new ApiResult(false, "Unimplemented Function");
    }

    /*
        借书记录查询模块。查询某个用户的借书记录，
        按照借书时间递减、书号递增的方式排序。
     */
    @Override
    public ApiResult showBorrowHistory(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    /*
        借书证注册模块。注册一个借书证，若借书证已经存在，则该操作将失败。
        当且仅当<姓名, 单位, 身份>均相同时，才认为两张借书证相同。
     */
    @Override
    public ApiResult registerCard(Card card) {
        return new ApiResult(false, "Unimplemented Function");
    }

    /*
        删除借书证模块。如果该借书证还有未归还的图书，那么删除操作将失败。
     */
    @Override
    public ApiResult removeCard(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    /*
        借书证查询模块。列出所有的借书证。
     */
    @Override
    public ApiResult showCards() {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            // 执行成功之后一定要记得提交事务
            // 如果在函数执行时题前返回，需要及时将事务回滚或提交掉
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
