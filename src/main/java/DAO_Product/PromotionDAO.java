package DAO_Product;

import app.ConnectDB;
import model.Promotion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PromotionDAO {

    /* ===================== FIND ALL ===================== */
    public List<Promotion> findAll() {
//        System.out.println("PROMO LOADED = " + list.size());

        List<Promotion> list = new ArrayList<>();

        String sqlPromo =
                "SELECT PromoID, PromoName, Description, " +
                "StartDate, EndDate, PromoType, Value, Status " +
                "FROM PROMOTION";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlPromo);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Promotion p = new Promotion(
                        rs.getString("PromoID"),
                        rs.getString("PromoName"),
                        rs.getString("Description"),
                        rs.getDate("StartDate").toLocalDate(),
                        rs.getDate("EndDate").toLocalDate(),
                        rs.getString("PromoType"),
                        rs.getDouble("Value"),
                        rs.getString("Status")
                );

                // load productIds cho promotion
                p.setProductIds(getProductIdsByPromo(p.getPromoId(), con));
                list.add(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ===================== INSERT ===================== */
    public void insert(Promotion p) throws SQLException {

        String sqlPromo =
                "INSERT INTO PROMOTION " +
                "(PromoID, PromoName, Description, StartDate, EndDate, PromoType, Value, Status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlPromo)) {
                ps.setString(1, p.getPromoId());
                ps.setString(2, p.getPromoName());
                ps.setString(3, p.getDescription());
                ps.setDate(4, Date.valueOf(p.getStartDate()));
                ps.setDate(5, Date.valueOf(p.getEndDate()));
                ps.setString(6, p.getPromoType());
                ps.setDouble(7, p.getValue());
                ps.setString(8, p.getStatus());
                ps.executeUpdate();
            }

            insertPromoProducts(p, con);
            con.commit();
        }
    }

    /* ===================== UPDATE ===================== */
    public void update(Promotion p) throws SQLException {

        String sqlPromo =
                "UPDATE PROMOTION SET " +
                "PromoName = ?, " +
                "Description = ?, " +
                "StartDate = ?, " +
                "EndDate = ?, " +
                "PromoType = ?, " +
                "Value = ?, " +
                "Status = ? " +
                "WHERE PromoID = ?";

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlPromo)) {
                ps.setString(1, p.getPromoName());
                ps.setString(2, p.getDescription());
                ps.setDate(3, Date.valueOf(p.getStartDate()));
                ps.setDate(4, Date.valueOf(p.getEndDate()));
                ps.setString(5, p.getPromoType());
                ps.setDouble(6, p.getValue());
                ps.setString(7, p.getStatus());
                ps.setString(8, p.getPromoId());
                ps.executeUpdate();
            }

            // xóa mapping cũ
            deletePromoProducts(p.getPromoId(), con);

            // insert mapping mới
            insertPromoProducts(p, con);

            con.commit();
        }
    }

    /* ===================== DELETE ===================== */
    public void delete(String promoId) throws SQLException {

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            deletePromoProducts(promoId, con);

            try (PreparedStatement ps =
                         con.prepareStatement("DELETE FROM PROMOTION WHERE PromoID = ?")) {
                ps.setString(1, promoId);
                ps.executeUpdate();
            }

            con.commit();
        }
    }

    /* ===================== EXISTS ===================== */
    public boolean existsId(String promoId) {
        String sql = "SELECT 1 FROM PROMOTION WHERE PromoID = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, promoId);
            return ps.executeQuery().next();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    

    private List<String> getProductIdsByPromo(String promoId, Connection con) throws SQLException {
        List<String> list = new ArrayList<>();

        String sql =
                "SELECT ProductID " +
                "FROM PROMOTION_PRODUCT " +
                "WHERE PromoID = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, promoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("ProductID"));
                }
            }
        }
        return list;
    }

    private void insertPromoProducts(Promotion p, Connection con) throws SQLException {
        if (p.getProductIds() == null || p.getProductIds().isEmpty()) return;

        String sql =
                "INSERT INTO PROMOTION_PRODUCT (PromoID, ProductID) " +
                "VALUES (?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String pid : p.getProductIds()) {
                ps.setString(1, p.getPromoId());
                ps.setString(2, pid);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deletePromoProducts(String promoId, Connection con) throws SQLException {
        String sql = "DELETE FROM PROMOTION_PRODUCT WHERE PromoID = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, promoId);
            ps.executeUpdate();
        }
    }

    // load product chưa có promotion (quantity > 0)
    public List<String> findAvailableProductIds(Connection con) throws SQLException {
        List<String> list = new ArrayList<>();

        String sql =
                "SELECT p.ProductID " +
                "FROM PRODUCT p " +
                "LEFT JOIN PROMOTION_PRODUCT pp ON p.ProductID = pp.ProductID " +
                "WHERE p.Quantity > 0 " +
                "AND pp.ProductID IS NULL";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(rs.getString("ProductID"));
            }
        }
        return list;
    }
    
    public void removeProductsFromPromo(String promoId, List<String> productIds) throws SQLException {
    if (productIds == null || productIds.isEmpty()) return;

    String sql = "DELETE FROM PROMOTION_PRODUCT WHERE PromoID = ? AND ProductID = ?";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        for (String pid : productIds) {
            ps.setString(1, promoId);
            ps.setString(2, pid);
            ps.addBatch();
        }
        ps.executeBatch();
    }
}

}
