/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO_Customer_Order;
import ConnectDB.ConnectDB;
import java.sql.*;
import model.OrderDetailItem;
/**
 *
 * @author Admin
 */
public class OrderDetailDao {
    public void insertDetail(int OrderId, OrderDetailItem item){
        String sql="INSERT INTO orderdetail (OrderID, ProductID, Quantity, Price) VALUE (?,?,?,?)";
        try(Connection con = ConnectDB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)){
            
            ps.setInt(1, OrderId);
            ps.setString(2, item.getProduct().getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setFloat(4, item.getPrice());
            
            ps.executeUpdate();
        }catch(SQLException e){
            System.out.println("Error !");
        }
        
    }
}
