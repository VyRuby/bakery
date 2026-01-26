package DAO_Report;

import app.ConnectDB;
import java.sql.*;
import java.util.*;
import model.Report;

public class ReportDAO {

    // ====== DTO cho PieChart top products ======
    public static class TopProductStat {
        private final String productName;
        private final int totalQty;

        public TopProductStat(String productName, int totalQty) {
            this.productName = productName;
            this.totalQty = totalQty;
        }

        public String getProductName() { return productName; }
        public int getTotalQty() { return totalQty; }
    }

    // ================= DOANH THU =================

    // NGÀY
    public List<Report> revenueByDay() throws SQLException {
        String sql =
            "SELECT DATE(o.OrderDate) AS period, " +
            "       COUNT(o.OrderID) AS totalOrders, " +
            "       SUM(o.Total) AS totalRevenue " +
            "FROM orders o " +
            "GROUP BY DATE(o.OrderDate) " +
            "ORDER BY period";
        return getRevenue(sql);
    }

    // TUẦN
    public List<Report> revenueByWeek() throws SQLException {
        String sql =
            "SELECT CONCAT(YEAR(o.OrderDate), '-W', WEEK(o.OrderDate,1)) AS period, " +
            "       COUNT(o.OrderID) AS totalOrders, " +
            "       SUM(o.Total) AS totalRevenue " +
            "FROM orders o " +
            "GROUP BY YEAR(o.OrderDate), WEEK(o.OrderDate,1) " +
            "ORDER BY period";
        return getRevenue(sql);
    }

    // THÁNG
    public List<Report> revenueByMonth() throws SQLException {
        String sql =
            "SELECT DATE_FORMAT(o.OrderDate,'%Y-%m') AS period, " +
            "       COUNT(o.OrderID) AS totalOrders, " +
            "       SUM(o.Total) AS totalRevenue " +
            "FROM orders o " +
            "GROUP BY DATE_FORMAT(o.OrderDate,'%Y-%m') " +
            "ORDER BY period";
        return getRevenue(sql);
    }

    // ================= LỢI NHUẬN =================

    // THÁNG
    public List<Report> profitByMonth() throws SQLException {
        String sql =
            "SELECT DATE_FORMAT(o.OrderDate,'%Y-%m') AS period, " +
            "       COUNT(DISTINCT o.OrderID) AS totalOrders, " +
            "       SUM(o.Total) AS totalRevenue, " +
            "       SUM(od.Quantity * od.CostPrice) AS totalCost " +
            "FROM orders o " +
            "JOIN orderdetail od ON o.OrderID = od.OrderID " +
            "GROUP BY DATE_FORMAT(o.OrderDate,'%Y-%m') " +
            "ORDER BY period";
        return getProfit(sql);
    }

    // QUÝ
    public List<Report> profitByQuarter() throws SQLException {
        String sql =
            "SELECT CONCAT(YEAR(o.OrderDate), '-Q', QUARTER(o.OrderDate)) AS period, " +
            "       COUNT(DISTINCT o.OrderID) AS totalOrders, " +
            "       SUM(o.Total) AS totalRevenue, " +
            "       SUM(od.Quantity * od.CostPrice) AS totalCost " +
            "FROM orders o " +
            "JOIN orderdetail od ON o.OrderID = od.OrderID " +
            "GROUP BY YEAR(o.OrderDate), QUARTER(o.OrderDate) " +
            "ORDER BY period";
        return getProfit(sql);
    }

    // NĂM
    public List<Report> profitByYear() throws SQLException {
        String sql =
            "SELECT YEAR(o.OrderDate) AS period, " +
            "       COUNT(DISTINCT o.OrderID) AS totalOrders, " +
            "       SUM(o.Total) AS totalRevenue, " +
            "       SUM(od.Quantity * od.CostPrice) AS totalCost " +
            "FROM orders o " +
            "JOIN orderdetail od ON o.OrderID = od.OrderID " +
            "GROUP BY YEAR(o.OrderDate) " +
            "ORDER BY period";
        return getProfit(sql);
    }

    // ================= TOP PRODUCT (PIE) =================

    public List<String> listMonths() throws SQLException {
        String sql = "SELECT DISTINCT DATE_FORMAT(OrderDate,'%Y-%m') AS period FROM orders ORDER BY period DESC";
        List<String> out = new ArrayList<>();
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString("period"));
        }
        return out;
    }

    public List<String> listQuarters() throws SQLException {
        String sql = "SELECT DISTINCT CONCAT(YEAR(OrderDate),'-Q',QUARTER(OrderDate)) AS period FROM orders ORDER BY period DESC";
        List<String> out = new ArrayList<>();
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString("period"));
        }
        return out;
    }

    // Top theo THÁNG: ym = "2026-01"
    public List<TopProductStat> topProductsByMonth(String ym, int limit) throws SQLException {
        String sql =
            "SELECT p.ProductName AS productName, SUM(od.Quantity) AS totalQty " +
            "FROM orders o " +
            "JOIN orderdetail od ON o.OrderID = od.OrderID " +
            "JOIN product p ON p.ProductID = od.ProductID " +
            "WHERE DATE_FORMAT(o.OrderDate,'%Y-%m') = ? " +
            "GROUP BY p.ProductID, p.ProductName " +
            "ORDER BY totalQty DESC " +
            "LIMIT ?";
        return getTopProducts(sql, ym, limit);
    }

    // Top theo QUÝ: yq = "2026-Q1"
    public List<TopProductStat> topProductsByQuarter(String yq, int limit) throws SQLException {
        String sql =
            "SELECT p.ProductName AS productName, SUM(od.Quantity) AS totalQty " +
            "FROM orders o " +
            "JOIN orderdetail od ON o.OrderID = od.OrderID " +
            "JOIN product p ON p.ProductID = od.ProductID " +
            "WHERE CONCAT(YEAR(o.OrderDate),'-Q',QUARTER(o.OrderDate)) = ? " +
            "GROUP BY p.ProductID, p.ProductName " +
            "ORDER BY totalQty DESC " +
            "LIMIT ?";
        return getTopProducts(sql, yq, limit);
    }

    // ================= COMMON =================

    private List<Report> getRevenue(String sql) throws SQLException {
        List<Report> list = new ArrayList<>();
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Report r = new Report();
                r.setPeriod(rs.getString("period"));
                r.setTotalOrders(rs.getInt("totalOrders"));
                r.setTotalRevenue(rs.getDouble("totalRevenue"));
                list.add(r);
            }
        }
        return list;
    }

    private List<Report> getProfit(String sql) throws SQLException {
        List<Report> list = new ArrayList<>();
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Report r = new Report();
                r.setPeriod(rs.getString("period"));
                r.setTotalOrders(rs.getInt("totalOrders"));
                r.setTotalRevenue(rs.getDouble("totalRevenue"));
                r.setTotalImportCost(rs.getDouble("totalCost"));
                list.add(r);
            }
        }
        return list;
    }

    private List<TopProductStat> getTopProducts(String sql, String period, int limit) throws SQLException {
        List<TopProductStat> list = new ArrayList<>();
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, period);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopProductStat(
                        rs.getString("productName"),
                        rs.getInt("totalQty")
                    ));
                }
            }
        }
        return list;
    }
}
