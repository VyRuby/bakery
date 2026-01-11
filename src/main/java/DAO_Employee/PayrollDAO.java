package DAO_Employee;

import ConnectDB.ConnectDB;
import model.Payroll;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollDAO {

   public List<Payroll> getPayrollByMonth(int month) {

    List<Payroll> list = new ArrayList<>();

    String sql =
        "SELECT EmployeeID, FullName, Month, WorkDays, BaseSalary, Bonus, Penalty, TotalSalary " +
        "FROM vw_EmployeeSalary WHERE Month = ?";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, month);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Payroll p = new Payroll(
                rs.getString("EmployeeID"),
                rs.getString("FullName"),
                rs.getInt("Month"),
                rs.getInt("WorkDays"),
                rs.getDouble("BaseSalary"),
                rs.getDouble("Bonus"),
                rs.getDouble("Penalty"),
                rs.getDouble("TotalSalary")
            );
            list.add(p);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return list;
}

}
