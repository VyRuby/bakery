/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.bakery;

import java.sql.Connection;

/**
 *
 * @author vy
 */
public class TestConnect {

    public static void main(String[] args) {
        // Gọi hàm getConnection() để test
        Connection conn = ConnectDB.getConnection();

        if (conn != null) {
            System.out.println("Connect successfully");
            ConnectDB.closeConnection();
        } else {
            System.out.println("Connect fail");
        }
    }
}
