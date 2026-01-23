package DAO_Report;

import model.Report;
import java.sql.*;
import java.util.*;

public class ReportDAO {

    private Connection conn;

    public ReportDAO(Connection conn) {
        this.conn = conn;
    }

    // ================= DOANH THU =================

    // NGÀY
    public List<Report> revenueByDay() throws SQLException {
        String sql = """
            SELECT 
                DATE(o.OrderDate) AS period,
                COUNT(o.OrderID) AS totalOrders,
                SUM(o.Total) AS totalRevenue
            FROM orders o
            GROUP BY DATE(o.OrderDate)
            ORDER BY period
        """;
        return getRevenue(sql);
    }

    // TUẦN
    public List<Report> revenueByWeek() throws SQLException {
        String sql = """
            SELECT 
                CONCAT(YEAR(o.OrderDate), '-W', WEEK(o.OrderDate,1)) AS period,
                COUNT(o.OrderID) AS totalOrders,
                SUM(o.Total) AS totalRevenue
            FROM orders o
            GROUP BY YEAR(o.OrderDate), WEEK(o.OrderDate,1)
            ORDER BY period
        """;
        return getRevenue(sql);
    }

    // THÁNG
    public List<Report> revenueByMonth() throws SQLException {
        String sql = """
            SELECT 
                DATE_FORMAT(o.OrderDate,'%Y-%m') AS period,
                COUNT(o.OrderID) AS totalOrders,
                SUM(o.Total) AS totalRevenue
            FROM orders o
            GROUP BY DATE_FORMAT(o.OrderDate,'%Y-%m')
            ORDER BY period
        """;
        return getRevenue(sql);
    }

    // ================= LỢI NHUẬN =================

    // THÁNG
    public List<Report> profitByMonth() throws SQLException {
        String sql = """
            SELECT 
                DATE_FORMAT(o.OrderDate,'%Y-%m') AS period,
                COUNT(DISTINCT o.OrderID) AS totalOrders,
                SUM(o.Total) AS totalRevenue,
                SUM(od.Quantity * od.CostPrice) AS totalCost
            FROM orders o
            JOIN orderdetail od ON o.OrderID = od.OrderID
            GROUP BY DATE_FORMAT(o.OrderDate,'%Y-%m')
            ORDER BY period
        """;
        return getProfit(sql);
    }

    // QUÝ
    public List<Report> profitByQuarter() throws SQLException {
        String sql = """
            SELECT 
                CONCAT(YEAR(o.OrderDate), '-Q', QUARTER(o.OrderDate)) AS period,
                COUNT(DISTINCT o.OrderID) AS totalOrders,
                SUM(o.Total) AS totalRevenue,
                SUM(od.Quantity * od.CostPrice) AS totalCost
            FROM orders o
            JOIN orderdetail od ON o.OrderID = od.OrderID
            GROUP BY YEAR(o.OrderDate), QUARTER(o.OrderDate)
            ORDER BY period
        """;
        return getProfit(sql);
    }

    // NĂM
    public List<Report> profitByYear() throws SQLException {
        String sql = """
            SELECT 
                YEAR(o.OrderDate) AS period,
                COUNT(DISTINCT o.OrderID) AS totalOrders,
                SUM(o.Total) AS totalRevenue,
                SUM(od.Quantity * od.CostPrice) AS totalCost
            FROM orders o
            JOIN orderdetail od ON o.OrderID = od.OrderID
            GROUP BY YEAR(o.OrderDate)
            ORDER BY period
        """;
        return getProfit(sql);
    }

    // ================= COMMON =================

    private List<Report> getRevenue(String sql) throws SQLException {
        List<Report> list = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Report r = new Report();
            r.setPeriod(rs.getString("period"));
            r.setTotalOrders(rs.getInt("totalOrders"));
            r.setTotalRevenue(rs.getDouble("totalRevenue"));
            list.add(r);
        }
        return list;
    }

    private List<Report> getProfit(String sql) throws SQLException {
        List<Report> list = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Report r = new Report();
            r.setPeriod(rs.getString("period"));
            r.setTotalOrders(rs.getInt("totalOrders"));
            r.setTotalRevenue(rs.getDouble("totalRevenue"));
            r.setTotalImportCost(rs.getDouble("totalCost"));
            list.add(r);
        }
        return list;
    }
}
