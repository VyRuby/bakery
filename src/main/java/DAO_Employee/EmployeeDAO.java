package DAO_Employee;

import app.ConnectDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Employee;

public class EmployeeDAO {

    // ===== READ (ALL / FILTER) =====
    public List<Employee> getEmployees(String status) {
        List<Employee> list = new ArrayList<>();

        String sql = "SELECT * FROM EMPLOYEE";
        if (!"ALL".equals(status)) {
            sql += " WHERE Status = ?";
        }

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (!"ALL".equals(status)) {
                ps.setString(1, status);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Employee e = new Employee(
                        rs.getString("EmployeeID"),
                        rs.getString("FullName"),
                        rs.getDate("DOB"),        // ✅ java.sql.Date
                        rs.getString("Gender"),
                        rs.getString("Phone"),
                        rs.getString("Email"),
                        rs.getString("Address"),
                        rs.getDate("HireDate"),  // ✅ java.sql.Date
                        rs.getString("Position"),
                        rs.getString("Status")
                );
                list.add(e);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    // ===== CREATE =====
    public void insert(Employee e) {
        String sql =
            "INSERT INTO EMPLOYEE " +
            "(EmployeeID, FullName, DOB, Gender, Phone, Email, Address, HireDate, Position, Status) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, e.getEmployeeID());
            ps.setString(2, e.getFullName());
            ps.setDate(3, e.getDob());        // ✅ KHÔNG valueOf
            ps.setString(4, e.getGender().toLowerCase());
            ps.setString(5, e.getPhone());
            ps.setString(6, e.getEmail());
            ps.setString(7, e.getAddress());
            ps.setDate(8, e.getHireDate());  // ✅ KHÔNG valueOf
            ps.setString(9, e.getPosition());
            ps.setString(10, e.getStatus());

            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ===== UPDATE =====
    public void update(Employee e) {
        String sql =
            "UPDATE EMPLOYEE SET " +
            "FullName=?, DOB=?, Gender=?, Phone=?, Email=?, Address=?, HireDate=?, Position=?, Status=? " +
            "WHERE EmployeeID=?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, e.getFullName());
            ps.setDate(2, e.getDob());        // ✅
            ps.setString(3, e.getGender());
            ps.setString(4, e.getPhone());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getAddress());
            ps.setDate(7, e.getHireDate());  // ✅
            ps.setString(8, e.getPosition());
            ps.setString(9, e.getStatus());
            ps.setString(10, e.getEmployeeID());

            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ===== SOFT DELETE =====
    public void deactivate(String empId) {
        String sql = "UPDATE EMPLOYEE SET Status='Inactive' WHERE EmployeeID=?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
 

}
