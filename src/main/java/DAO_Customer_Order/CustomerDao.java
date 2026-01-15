/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO_Customer_Order;

import model.Customer;
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
public class CustomerDao {
    public Customer findPhone(String phone){
        String sql =
                "Select CustomerID, FullName, Phone from customer Where Phone =? ";

              
               try(Connection con = ConnectDB.getConnection();
                       PreparedStatement ps = con.prepareStatement(sql)){
                ps.setString(1,phone);
                ResultSet rs = ps.executeQuery();
                
                if(rs.next()){
                    return new Customer(
                    rs.getInt("CustomerID"),
                            rs.getString("Phone"),
                            rs.getString("FullName")
                            
                    );
                }
                   
               }catch(SQLException e){
                   System.out.println("Error !" + e);
               }
        
        return null;
    }
    
    public List<Customer> findAll(){
        List<Customer> list = new ArrayList<>();
      
        
        String sql ="SELECT CustomerID, FullName, Phone FROM customer";
        
        
        try{Connection con = ConnectDB.getConnection();
        PreparedStatement ps= con.prepareStatement(sql);
        
        ResultSet rs= ps.executeQuery();
            while(rs.next()){
                
                list.add( new Customer(
                rs.getInt("CustomerID"),
                        rs.getString("FullName"),
                        rs.getString("Phone")       
                ));
            }
            if(list.isEmpty()){
          System.out.println("No data");
      }
        
        }catch(Exception e){
            System.out.println("Error !" + e);
        }
        return list;
    }
    public List<Customer>findByKeyword(String keyword){
        List<Customer> list = new ArrayList<>();
        
        String sql ="SELECT CustomerID, FullName, Phone From customer WHERE Phone LIKE ? OR FullName Like ? ";
        
        
        try(Connection con = ConnectDB.getConnection();
                PreparedStatement ps= con.prepareStatement(sql)){
            String key="%" + keyword+ "%";
            ps.setString(1,key);
            ps.setString(2, key);
            
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                list.add(new Customer(
                rs.getInt("CustomerID"),
                        rs.getString("FullName"),
                        rs.getString("Phone")
                ));
            }
        }catch(SQLException e){
                System.out.println("error !" +e);
                }
        return list;
        
    }
    
    public Customer findByID(int id){
        String sql = "SELECT * FROM customer WHERE CustomerID= ? ";
        
//    ConnectDB db= new ConnectDB();
    try(Connection con = ConnectDB.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){
        ps.setInt(1,id);
        ResultSet rs= ps.executeQuery();
        if(rs.next()){
            Customer.Gender gender = rs.getString("Gender").equals("Male")?Customer.Gender.Male:Customer.Gender.Female;
            
            return new Customer(
            rs.getString("FullName"),
                    rs.getString("Phone"),
                    gender,
                    rs.getDate("DOB").toLocalDate(),
                    
                    rs.getString("Email"),
                    rs.getString("Address"),
                    rs.getInt("CustomerID") 
            );
            
                    
        }
        
    }catch(SQLException e){
        System.out.println("Error " + e);
    }
    return null;            }
    
    public boolean insert (Customer c){
        String sql = "INSERT INTO customer (FullName,Phone,Email,Address,Gender,DOB) VALUES (?,?,?,?,?,?)";
    
    try(Connection con = ConnectDB.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){
        ps.setString(1, c.getName());
        ps.setString(2,c.getPhone());
        ps.setString(3, c.getEmail());
        ps.setString(4, c.getAddress());
        ps.setString(5, c.getGender().name());
        ps.setDate(6, java.sql.Date.valueOf(c.getDob()));
        
        return ps.executeUpdate()>0;
        
        
    }catch (SQLException e){
        System.out.println("Error !" + e);
    }
    return false;
    }
}
