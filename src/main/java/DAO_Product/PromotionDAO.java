package DAO_Product;

import app.ConnectDB;
import model.Promotion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromotionDAO {

    /* ===================== FIND ALL ===================== */
    public List<Promotion> findAll() {
        List<Promotion> list = new ArrayList<>();

        String sqlPromo =
                "SELECT PromoID, PromoName, Description, PromoType, Value, Status " +
                "FROM PROMOTION";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlPromo);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Promotion p = new Promotion(
                        rs.getString("PromoID"),
                        rs.getString("PromoName"),
                        rs.getString("Description"),
                        rs.getString("PromoType"),
                        rs.getDouble("Value"),
                        rs.getString("Status")
                );

                // load productIds theo promo
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
                "(PromoID, PromoName, Description, PromoType, Value, Status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlPromo)) {
                ps.setString(1, p.getPromoId());
                ps.setString(2, p.getPromoName());
                ps.setString(3, p.getDescription());
                ps.setString(4, p.getPromoType());
                ps.setDouble(5, p.getValue());
                ps.setString(6, p.getStatus());
                ps.executeUpdate();
            }

            // insert mapping PROMOTION_PRODUCT
            insertPromoProducts(p, con);

            con.commit();
            con.setAutoCommit(true);

        } catch (SQLException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    /* ===================== UPDATE ===================== */
    public void update(Promotion p) throws SQLException {

        String sqlPromo =
                "UPDATE PROMOTION SET " +
                "PromoName = ?, " +
                "Description = ?, " +
                "PromoType = ?, " +
                "Value = ?, " +
                "Status = ? " +
                "WHERE PromoID = ?";

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlPromo)) {
                ps.setString(1, p.getPromoName());
                ps.setString(2, p.getDescription());
                ps.setString(3, p.getPromoType());
                ps.setDouble(4, p.getValue());
                ps.setString(5, p.getStatus());
                ps.setString(6, p.getPromoId());
                ps.executeUpdate();
            }

            // xóa mapping cũ theo PromoID
            deletePromoProducts(p.getPromoId(), con);

            // insert mapping mới (nếu chọn product)
            insertPromoProducts(p, con);

            con.commit();
            con.setAutoCommit(true);

        } catch (SQLException ex) {
            ex.printStackTrace();
            throw ex;
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
            con.setAutoCommit(true);
        }
    }

    /* ===================== EXISTS ===================== */
    public boolean existsId(String promoId) {
        String sql = "SELECT 1 FROM PROMOTION WHERE PromoID = ? LIMIT 1";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, promoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /* ===================== PROMOTION_PRODUCT ===================== */
    private void insertPromoProducts(Promotion p, Connection con) throws SQLException {
        if (p.getProductIds() == null || p.getProductIds().isEmpty()) return;

        String sqlInsert =
                "INSERT INTO PROMOTION_PRODUCT (PromoID, ProductID) VALUES (?, ?)";

        try (PreparedStatement psIns = con.prepareStatement(sqlInsert)) {
            for (String pid : p.getProductIds()) {
                psIns.setString(1, p.getPromoId());
                psIns.setString(2, pid);
                psIns.executeUpdate(); // nếu pid đã thuộc promo khác -> UNIQUE(ProductID) sẽ throw (đúng)
            }
        }
    }

    private void deletePromoProducts(String promoId, Connection con) throws SQLException {
        String sql = "DELETE FROM PROMOTION_PRODUCT WHERE PromoID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, promoId);
            ps.executeUpdate();
        }
    }

    private List<String> getProductIdsByPromo(String promoId, Connection con) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT ProductID FROM PROMOTION_PRODUCT WHERE PromoID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, promoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("ProductID"));
            }
        }
        return list;
    }

    // Xóa promotion khỏi nhiều product (batch)
    public void removePromotionFromProducts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        String sql = "DELETE FROM PROMOTION_PRODUCT WHERE ProductID = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (String pid : productIds) {
                ps.setString(1, pid);
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Remove promotion batch failed: " + e.getMessage());
        }
    }

    // trả về map ProductID -> PromoID
    public Map<String, String> getProductPromoMap() {
        String sql = "SELECT ProductID, PromoID FROM PROMOTION_PRODUCT";
        Map<String, String> map = new HashMap<>();

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                map.put(rs.getString("ProductID"), rs.getString("PromoID"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    // Lấy promo active theo product (không có StartTime/EndTime)
    public Promotion getActivePromoByProduct(String productId) {

        String sql =
                "SELECT p.* FROM PROMOTION p " +
                "JOIN PROMOTION_PRODUCT pp ON p.PromoID = pp.PromoID " +
                "WHERE pp.ProductID = ? AND p.Status = 'Active' " +
                "LIMIT 1";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Promotion(
                            rs.getString("PromoID"),
                            rs.getString("PromoName"),
                            rs.getString("Description"),
                            rs.getString("PromoType"),
                            rs.getDouble("Value"),
                            rs.getString("Status")
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    
    // ================= AUTO GENERATE PROMO ID (PR01, PR02, ...) =================
public String autoPromoId() {

    String sql = "SELECT PromoID FROM PROMOTION WHERE PromoID LIKE 'PR%' ORDER BY PromoID DESC LIMIT 1";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        int next = 1;

        if (rs.next()) {
            String lastId = rs.getString("PromoID"); // VD: PR07
            String num = lastId.substring(2);        // "07"
            next = Integer.parseInt(num) + 1;
        }

        return String.format("PR%02d", next);

    } catch (Exception e) {
        e.printStackTrace();
        return "PR01";
    }
}

}
