/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO_Customer_Order;

import model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import app.ConnectDB;
/**
 *
 * @author Admin
 */
public class productDao {
    
    public List<Product> findAll(){
        
        List <Product> list = new ArrayList <>();
       String sql ="SELECT * FROM product";
       
     
       try{Connection con = ConnectDB.getConnection();
       PreparedStatement ps = con.prepareStatement(sql);
           
       ResultSet rs= ps.executeQuery();
       while(rs.next()){
           list.add(new Product(
           rs.getString("ProductID"),
           rs.getString("ProductName"),
           rs.getString("CategoryID"),
           rs.getInt("Quantity"),
           rs.getString("Unit"),
           rs.getFloat("Price"),
           rs.getString("Description"),
           rs.getString("Image")
                   ));
       }if(list.isEmpty()){
           System.out.println("No Data");
       }
       
       }catch(SQLException e){
           System.out.println("Error !" + e);
           
       }
        
        return list;
    }
    
    // ================= FIND BY ID =================
    public Product findById(String productId) {

        String sql = "SELECT * FROM product WHERE ProductID = ?";
        Product p = null;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                p = new Product(
                        rs.getString("ProductID"),
                        rs.getString("ProductName"),
                        rs.getString("CategoryID"),
                        rs.getInt("Quantity"),
                        rs.getString("Unit"),
                        rs.getFloat("Price"),
                        rs.getString("Description"),
                        rs.getString("Image")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return p;
    }
    
      // ================= CHECK ID EXISTS =================
    public static boolean exists(String productId) {

        String sql = "SELECT 1 FROM product WHERE ProductID = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // có bản ghi → trùng

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void insert(Product p) {
          String sql = "INSERT INTO product "
               + "(ProductID, ProductName, CategoryID, Quantity, Unit, Price, Description, Image) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getProductId());
            ps.setString(2, p.getProductName());
            ps.setString(3, p.getCategoryId());
            ps.setInt(4, p.getQuantity());
            ps.setString(5, p.getUnit());
            ps.setFloat(6, p.getPrice());
            ps.setString(7, p.getDescription());
            ps.setString(8, p.getImage());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Insert product failed");
        }
    }

    public void update(Product p) {

        String sql = "UPDATE product SET "
           + "ProductName=?, CategoryID=?, Quantity=?, Unit=?, Price=?, Description=?, Image=? "
           + "WHERE ProductID=?";


        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getProductName());
            ps.setString(2, p.getCategoryId());
            ps.setInt(3, p.getQuantity());
            ps.setString(4, p.getUnit());
            ps.setFloat(5, p.getPrice());
            ps.setString(6, p.getDescription());
            ps.setString(7, p.getImage());
            ps.setString(8, p.getProductId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Update product failed");
        }
    }


     public void delete(String productId) {

        String sql = "DELETE FROM product WHERE ProductID = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Delete product failed");
        }
    }

    public Product[] getAll() {
        List<Product> list = findAll();
        return list.toArray(new Product[0]);
    }
   
    
   
    
}
