/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO_Customer_Order;
import app.ConnectDB;
import java.sql.*;

/**
 *
 * @author Admin
 */
public class OrderDao {
    public int insertOrder(int customerId, float total, String paymentMethod) throws SQLException{
        String sql ="INSERT INTO orders (CustomerID,OrderDate,Total,PaymentMethod) VALUES (?,NOW(),?,? )";
        
        try(Connection con = ConnectDB.getConnection(); 
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1, customerId);
            ps.setFloat(2, total);
            ps.setString(3,paymentMethod);
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if(rs.next()){
                return rs.getInt(1);
                
            }
            
        }catch(SQLException e){
           e.printStackTrace();
        }
        return -1;
    }
    
    
    
}
