package DAO_Employee;

import app.ConnectDB;
import model.Payroll;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollDAO {

    public List<Payroll> getPayrollByMonth(int month) {

        List<Payroll> list = new ArrayList<>();

        String sql
                = "SELECT EmployeeID, FullName, Month, WorkDays, BaseSalary, Bonus, Penalty, TotalSalary "
                + "FROM vw_EmployeeSalary WHERE Month = ?";

        try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, month);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Payroll p = new Payroll(
                        rs.getString("EmployeeID"),
                        rs.getString("FullName"),
                        rs.getInt("Month"),
                        rs.getInt("WorkDays"),
                        rs.getBigDecimal("BaseSalary"),
                        rs.getBigDecimal("Bonus"),
                        rs.getBigDecimal("Penalty"),
                        rs.getBigDecimal("TotalSalary")
                );
                list.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    
        // =========================
    // CALCULATE SALARY (CALL PROCEDURE)
    // =========================
    public void calculatePayroll(int month, int year) {

        String sql = "{CALL sp_calculate_payroll_by_month(?, ?)}";

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
