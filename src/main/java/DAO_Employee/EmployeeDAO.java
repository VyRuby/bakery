package DAO_Employee;

import app.ConnectDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Employee;

public class EmployeeDAO {

    // ================= READ (ALL / FILTER) =================
    public List<Employee> getEmployees(String status) {

        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM EMPLOYEE";

        if (!"ALL".equalsIgnoreCase(status)) {
            sql += " WHERE Status = ?";
        }

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (!"ALL".equalsIgnoreCase(status)) {
                ps.setString(1, status);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Employee e = new Employee(
                        rs.getString("EmployeeID"),
                        rs.getString("FullName"),
                        rs.getDate("DOB"),
                        rs.getString("Gender"),
                        rs.getString("Phone"),
                        rs.getString("Email"),
                        rs.getString("Address"),
                        rs.getDate("HireDate"),
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

    // ================= CREATE =================
    public boolean insert(Employee e) {

        String sql =
            "INSERT INTO EMPLOYEE " +
            "(EmployeeID, FullName, DOB, Gender, Phone, Email, Address, HireDate, Position, Status) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, e.getEmployeeID().trim());
            ps.setString(2, e.getFullName().trim());
            ps.setDate(3, e.getDob());
            ps.setString(4, e.getGender().toLowerCase()); // ⭐ QUAN TRỌNG
            ps.setString(5, e.getPhone());
            ps.setString(6, e.getEmail());
            ps.setString(7, e.getAddress());
            ps.setDate(8, e.getHireDate());
            ps.setString(9, e.getPosition());
            ps.setString(10, e.getStatus());

            ps.executeUpdate();
            return true;

        } catch (SQLIntegrityConstraintViolationException ex) {
            System.out.println("❌ EmployeeID already exists!");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    // ================= UPDATE =================
    public boolean update(Employee e) {

        String sql =
            "UPDATE EMPLOYEE SET " +
            "FullName=?, DOB=?, Gender=?, Phone=?, Email=?, Address=?, HireDate=?, Position=?, Status=? " +
            "WHERE EmployeeID=?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, e.getFullName());
            ps.setDate(2, e.getDob());
            ps.setString(3, e.getGender().toLowerCase()); // ⭐ FIX
            ps.setString(4, e.getPhone());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getAddress());
            ps.setDate(7, e.getHireDate());
            ps.setString(8, e.getPosition());
            ps.setString(9, e.getStatus());
            ps.setString(10, e.getEmployeeID());

            ps.executeUpdate();
            return true;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    // ================= SOFT DELETE =================
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
    
    // ================= CHECK EXIST =================
public boolean exists(String empId) {

    String sql = "SELECT 1 FROM EMPLOYEE WHERE EmployeeID = ?";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, empId);
        ResultSet rs = ps.executeQuery();
        return rs.next(); // có bản ghi → true

    } catch (SQLException ex) {
        ex.printStackTrace();
    }

    return false;
}

// ================= SEARCH =================
public List<Employee> search(String keyword) {

    List<Employee> list = new ArrayList<>();

    String sql =
        "SELECT * FROM EMPLOYEE " +
        "WHERE FullName LIKE ? " +
        "   OR Phone LIKE ? " +
        "   OR Email LIKE ?";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        String key = "%" + keyword + "%";
        ps.setString(1, key);
        ps.setString(2, key);
        ps.setString(3, key);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            list.add(new Employee(
                rs.getString("EmployeeID"),
                rs.getString("FullName"),
                rs.getDate("DOB"),
                rs.getString("Gender"),
                rs.getString("Phone"),
                rs.getString("Email"),
                rs.getString("Address"),
                rs.getDate("HireDate"),
                rs.getString("Position"),
                rs.getString("Status")
            ));
        }

    } catch (SQLException ex) {
        ex.printStackTrace();
    }

    return list;
}


}
