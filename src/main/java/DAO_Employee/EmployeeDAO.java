package DAO_Employee;

import app.ConnectDB;
import java.sql.*;
import java.util.*;
import model.Employee;

public class EmployeeDAO {

    // ===== READ =====
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
                    rs.getString("Status"),
                    rs.getInt("BaseDailySalary") // ✅
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ===== INSERT =====
    public boolean insert(Employee e) {

        String sql =
            "INSERT INTO EMPLOYEE " +
            "(EmployeeID, FullName, DOB, Gender, Phone, Email, Address, HireDate, Position, Status, BaseDailySalary) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, e.getEmployeeID());
            ps.setString(2, e.getFullName());
            ps.setDate(3, e.getDob());
            ps.setString(4, e.getGender().toLowerCase());
            ps.setString(5, e.getPhone());
            ps.setString(6, e.getEmail());
            ps.setString(7, e.getAddress());
            ps.setDate(8, e.getHireDate());
            ps.setString(9, e.getPosition());
            ps.setString(10, e.getStatus());
            ps.setInt(11, e.getBaseDailySalary());

            ps.executeUpdate();
            return true;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    // ===== UPDATE =====
    public boolean update(Employee e) {

        String sql =
            "UPDATE EMPLOYEE SET FullName=?, DOB=?, Gender=?, Phone=?, Email=?, " +
            "Address=?, HireDate=?, Position=?, Status=?, BaseDailySalary=? " +
            "WHERE EmployeeID=?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, e.getFullName());
            ps.setDate(2, e.getDob());
            ps.setString(3, e.getGender().toLowerCase());
            ps.setString(4, e.getPhone());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getAddress());
            ps.setDate(7, e.getHireDate());
            ps.setString(8, e.getPosition());
            ps.setString(9, e.getStatus());
            ps.setInt(10, e.getBaseDailySalary());
            ps.setString(11, e.getEmployeeID());

            ps.executeUpdate();
            return true;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    // ===== OTHERS (KHÔNG ĐỔI) =====
    public void deactivate(String id) {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps =
                 con.prepareStatement("UPDATE EMPLOYEE SET Status='Inactive' WHERE EmployeeID=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean exists(String id) {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps =
                 con.prepareStatement("SELECT 1 FROM EMPLOYEE WHERE EmployeeID=?")) {
            ps.setString(1, id);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ===== SEARCH =====
public List<Employee> search(String keyword) {

    List<Employee> list = new ArrayList<>();

    String sql =
        "SELECT * FROM EMPLOYEE " +
        "WHERE EmployeeID LIKE ? " +
        "OR FullName LIKE ? " +
        "OR Phone LIKE ? " +
        "OR Email LIKE ? " +
        "OR Position LIKE ?";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        String key = "%" + keyword + "%";

        for (int i = 1; i <= 5; i++) {
            ps.setString(i, key);
        }

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
                rs.getString("Status"),
                rs.getInt("BaseDailySalary") // ✅ GIỮ
            ));
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return list;
}

}
