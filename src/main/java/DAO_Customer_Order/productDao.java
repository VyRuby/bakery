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
import ConnectDB.ConnectDB;
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
    
}
