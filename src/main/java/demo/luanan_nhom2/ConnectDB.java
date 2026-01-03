/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package demo.luanan_nhom2;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 *
 * @author Admin
 */
public class ConnectDB {
    Connection con;
public Connection getConnect(){
    try {
    String  url ="jdbc:mysql://localhost:3306/bakery";
    String username = "root";
    String password = "";
    
    
        //load driver
        Class.forName("com.mysql.cj.jdbc.Driver");
        //connect to database
        con = DriverManager.getConnection(url,username,password);
        System.out.println("Connect Successfully !");
    } catch (ClassNotFoundException ex) {
        System.out.println("Error ClassNotFoundException: " + ex.getMessage());
    }   catch (SQLException ex) {
            System.out.println("Error SQLException ex " + ex.getMessage());
        }
    return con;
}
   
    
}
