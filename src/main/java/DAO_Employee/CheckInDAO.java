package DAO_Employee;

import app.ConnectDB;
import model.CheckIn;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CheckInDAO {

    // ===== READ: CHECK-IN HÔM NAY =====
    public List<CheckIn> getTodayCheckIn() {
        List<CheckIn> list = new ArrayList<>();

       String sql = "SELECT * "
           + "FROM EMPLOYEE_CHECKIN "
           + "WHERE WorkDate = CURDATE() "
           + "ORDER BY CheckInTime";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CheckIn c = new CheckIn(
                        rs.getInt("CheckInID"),
                        rs.getString("EmployeeID"),
                        rs.getDate("WorkDate"),          // java.sql.Date
                        rs.getTime("CheckInTime"),       // java.sql.Time
                        rs.getTime("CheckOutTime"),      // java.sql.Time
                        rs.getBoolean("IsLate")
                );
                list.add(c);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    // ===== CHECK IN =====
    public void checkIn(String empId) {

        String sql = "INSERT INTO EMPLOYEE_CHECKIN "
           + "(EmployeeID, WorkDate, CheckInTime, IsLate) "
           + "VALUES (?, CURDATE(), CURTIME(), CURTIME() > '08:00:00')";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ===== CHECK OUT =====
    public void checkOut(int checkInId) {

        String sql = "UPDATE EMPLOYEE_CHECKIN "
           + "SET CheckOutTime = CURTIME() "
           + "WHERE CheckInID = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, checkInId);
            ps.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ===== KIỂM TRA ĐÃ CHECK-IN HÔM NAY CHƯA =====
    public boolean isCheckedInToday(String empId) {

       String sql = "SELECT COUNT(*) "
           + "FROM EMPLOYEE_CHECKIN "
           + "WHERE EmployeeID = ? "
           + "AND WorkDate = CURDATE()";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, empId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;
    }
}
