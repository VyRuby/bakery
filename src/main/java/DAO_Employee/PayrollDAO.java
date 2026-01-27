package DAO_Employee;

import app.ConnectDB;
import model.Payroll;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollDAO {

    public List<Payroll> getPayroll(int month, int year) {

        List<Payroll> list = new ArrayList<>();

        String sql = """
            SELECT 
                e.EmployeeID,
                e.FullName,
                e.BaseDailySalary,
                p.Month,
                p.Year,
                p.WorkDays,
                p.Bonus,
                p.Penalty,
                p.TotalSalary
            FROM EMPLOYEE_PAYROLL p
            JOIN EMPLOYEE e ON e.EmployeeID = p.EmployeeID
            WHERE p.Month = ? AND p.Year = ?
            ORDER BY e.EmployeeID
        """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, month);
            ps.setInt(2, year);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Payroll(
                        rs.getString("EmployeeID"),
                        rs.getString("FullName"),
                        rs.getInt("WorkDays"),
                        rs.getInt("Month"),
                        rs.getInt("Year"),
                        rs.getBigDecimal("Bonus"),
                        rs.getBigDecimal("Penalty"),
                        rs.getBigDecimal("TotalSalary"),
                        rs.getInt("BaseDailySalary")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ✅ GỌI ĐÚNG PROCEDURE
    public void calculatePayroll(int month, int year) {

        String sql = "{CALL sp_RecalcPayroll_ByMonth(?, ?)}";

        try (Connection con = ConnectDB.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, month);
            cs.setInt(2, year);
            cs.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
