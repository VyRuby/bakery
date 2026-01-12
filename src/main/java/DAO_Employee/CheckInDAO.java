package DAO_Employee;

import app.ConnectDB;
import model.CheckIn;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CheckInDAO {

    // ===============================
    // READ: CHECK-IN HÔM NAY
    // ===============================
    public List<CheckIn> getTodayCheckIn() {

        List<CheckIn> list = new ArrayList<>();

        String sql = "SELECT * FROM EMPLOYEE_CHECKIN "
           + "WHERE WorkDate = CURDATE() "
           + "ORDER BY CheckInTime";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CheckIn c = new CheckIn(
                        rs.getInt("CheckInID"),
                        rs.getString("EmployeeID"),
                        rs.getDate("WorkDate"),
                        rs.getTime("CheckInTime"),
                        rs.getTime("CheckOutTime"),
                        rs.getBoolean("IsLate")
                );
                list.add(c);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    // ===============================
    // CHECK-IN BY EMAIL (CALL PROCEDURE)
    // ===============================
    public void checkInByEmail(String email) {

        String sql = "{CALL sp_employee_checkin_by_email(?)}";

        try (Connection con = ConnectDB.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, email);
            cs.execute();

        } catch (SQLException ex) {
            // ném lỗi để Controller bắt & show Alert
            throw new RuntimeException(ex.getMessage());
        }
    }

    // ===============================
    // CHECK-OUT BY EMAIL (CALL PROCEDURE)
    // ===============================
    public void checkOutByEmail(String email) {

        String sql = "{CALL sp_employee_checkout_by_email(?)}";

        try (Connection con = ConnectDB.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, email);
            cs.execute();

        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}
