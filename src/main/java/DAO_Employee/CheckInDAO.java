package DAO_Employee;

import app.ConnectDB;
import model.CheckIn;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CheckInDAO {

    // ==================================
    // CHECK-IN (GỌI PROCEDURE)
    // ==================================
    public void checkInByEmail(String email) throws Exception {

        String sql = "{CALL sp_CheckInByEmail(?)}";

        try (Connection con = ConnectDB.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, email);
            cs.execute();
        }
    }

    // ==================================
    // CHECK-OUT (GỌI PROCEDURE)
    // ==================================
    public void checkOutByEmail(String email) throws Exception {

        String sql = "{CALL sp_CheckOutByEmail(?)}";

        try (Connection con = ConnectDB.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, email);
            cs.execute();
        }
    }

    // ==================================
    // LOAD CHECKIN THEO NGÀY
    // ==================================
    public List<CheckIn> getCheckInByDate(Date date) {

        List<CheckIn> list = new ArrayList<>();

        String sql = """
            SELECT c.CheckInID, c.EmployeeID, e.FullName,
                   c.WorkDate, c.CheckInTime, c.CheckOutTime,
                   c.IsLate, c.IsEarlyLeave
            FROM EMPLOYEE_CHECKIN c
            JOIN EMPLOYEE e ON e.EmployeeID = c.EmployeeID
            WHERE c.WorkDate = ?
            ORDER BY c.CheckInTime
        """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, date);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ==================================
    // LOAD CHECKIN THEO THÁNG / NĂM
    // (DÙNG CHO DATA 2025 + REALTIME)
    // ==================================
    public List<CheckIn> getCheckInByMonth(int month, int year) {

        List<CheckIn> list = new ArrayList<>();

        String sql = """
            SELECT c.CheckInID, c.EmployeeID, e.FullName,
                   c.WorkDate, c.CheckInTime, c.CheckOutTime,
                   c.IsLate, c.IsEarlyLeave
            FROM EMPLOYEE_CHECKIN c
            JOIN EMPLOYEE e ON e.EmployeeID = c.EmployeeID
            WHERE MONTH(c.WorkDate)=?
              AND YEAR(c.WorkDate)=?
            ORDER BY c.WorkDate, c.CheckInTime
        """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, month);
            ps.setInt(2, year);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ==================================
    // SEARCH THEO EMPLOYEE ID
    // ==================================
    public List<CheckIn> searchByEmployee(String empId) {

        List<CheckIn> list = new ArrayList<>();

        String sql = """
            SELECT c.CheckInID, c.EmployeeID, e.FullName,
                   c.WorkDate, c.CheckInTime, c.CheckOutTime,
                   c.IsLate, c.IsEarlyLeave
            FROM EMPLOYEE_CHECKIN c
            JOIN EMPLOYEE e ON e.EmployeeID = c.EmployeeID
            WHERE c.EmployeeID LIKE ?
            ORDER BY c.WorkDate DESC
        """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + empId + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ==================================
    // MAP RESULTSET → MODEL
    // ==================================
    private CheckIn map(ResultSet rs) throws SQLException {

        return new CheckIn(
            rs.getInt("CheckInID"),
            rs.getString("EmployeeID"),
            rs.getString("FullName"),
            rs.getDate("WorkDate"),
            rs.getTime("CheckInTime"),
            rs.getTime("CheckOutTime"),
            rs.getBoolean("IsLate"),
            rs.getBoolean("IsEarlyLeave")
        );
    }
}
