/*
 * File: ConnectDB.java
 * Mô tả: Quản lý kết nối MySQL cho project Bakery
 * Người thực hiện: Quốc Anh (Nhóm 2)
 */
package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectDB {

    private static final String url = "jdbc:mysql://localhost:3306/bakery_db?useSSL=false&serverTimezone=UTC";

    private static final String username = "root";
    private static final String password = "";

    private static Connection connection = null;

    // Hàm lấy kết nối 
    public static Connection getConnection() {
        try {
            // Nạp driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            //connect to database
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connect Successfully !");
        } catch (ClassNotFoundException e) {
            System.out.println(" Lỗi: Không tìm thấy Driver MySQL - " + e.getMessage());
        } catch (SQLException e) {
            System.out.println(" Lỗi kết nối MySQL: " + e.getMessage());
        }
        return connection;
    }

    // Hàm đóng kết nối 
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println(" Đã đóng kết nối MySQL.");
            }
        } catch (SQLException e) {
            System.out.println(" Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

  
  
}
