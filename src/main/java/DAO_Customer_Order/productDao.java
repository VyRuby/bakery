/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO_Customer_Order;

import model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import app.ConnectDB;
import java.time.LocalDate;
import model.RestockItem;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 *
 * @author Admin
 */
public class productDao {
    
    //================= FIND ALL ACTIVE + INACTVE =================
   public List<Product> findAll() {

    List<Product> list = new ArrayList<>();
    String sql = "SELECT * FROM product";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            list.add(new Product(
                    rs.getString("ProductID"),
                    rs.getString("ProductName"),
                    rs.getString("CategoryID"),
                    rs.getInt("Quantity"),
                    rs.getString("Unit"),
                    rs.getFloat("CostPrice"),
                    rs.getFloat("Price"),
                    rs.getString("Description"),
                    rs.getString("Image"),
                    rs.getString("Status")

            ));
        }

        if (list.isEmpty()) {
            System.out.println("No Data");
        }

    } catch (SQLException e) {
        System.out.println("Error !" + e);
    }

    return list;
}
   // find all active
   public List<Product> findAllActive() {
    List<Product> list = new ArrayList<>();
    String sql = "SELECT * FROM PRODUCT WHERE Status = 'Active'";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            list.add(new Product(
                rs.getString("ProductID"),
                rs.getString("ProductName"),
                rs.getString("CategoryID"),
                rs.getInt("Quantity"),
                rs.getString("Unit"),
                rs.getFloat("CostPrice"),
                rs.getFloat("Price"),
                rs.getString("Description"),
                rs.getString("Image")
            ));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return list;
}


    
    // ================= FIND BY ID =================
    public Product findById(String productId) {

        String sql = "SELECT * FROM product WHERE ProductID = ?";
        Product p = null;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                p = new Product(
                        rs.getString("ProductID"),
                        rs.getString("ProductName"),
                        rs.getString("CategoryID"),
                        rs.getInt("Quantity"),
                        rs.getString("Unit"),
                        rs.getFloat("CostPrice"),
                        rs.getFloat("Price"),
                        rs.getString("Description"),
                        rs.getString("Image")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return p;
    }
    
      // ================= CHECK ID EXISTS =================
    public static boolean exists(String productId) {

        String sql = "SELECT 1 FROM product WHERE ProductID = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // có bản ghi → trùng

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    // ================= INSERT =================
    public void insert(Product p) {
          String sql = "INSERT INTO product "
               + "(ProductID, ProductName, CategoryID, Quantity, Unit, Price, Description, Image) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getProductId());
            ps.setString(2, p.getProductName());
            ps.setString(3, p.getCategoryId());
            ps.setInt(4, p.getQuantity());
            ps.setString(5, p.getUnit());
            ps.setFloat(6, p.getCostPrice());
            ps.setFloat(6, p.getPrice());
            ps.setString(7, p.getDescription());
            ps.setString(8, p.getImage());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Insert product failed");
        }
    }
    // ================= UPDATE =================
    public void update(Product p) {

        String sql = "UPDATE product SET "
           + "ProductName=?, CategoryID=?, Quantity=?, Unit=?, Price=?, Description=?, Image=? "
           + "WHERE ProductID=?";


        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getProductName());
            ps.setString(2, p.getCategoryId());
            ps.setInt(3, p.getQuantity());
            ps.setString(4, p.getUnit());
            ps.setFloat(5, p.getPrice());
            ps.setString(6, p.getDescription());
            ps.setString(7, p.getImage());
            ps.setString(8, p.getProductId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Update product failed");
        }
    }

    // ================ DEACTIVATE PRODUCT (DELETE FROM VIEW)====================
    public void delete(String productId) {

    String sql = "UPDATE product SET Status = 'Inactive' WHERE ProductID = ?";

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, productId);
        ps.executeUpdate();

    } catch (SQLException e) {
        e.printStackTrace();
        throw new RuntimeException("Deactivate product failed");
    }
}


    // Tạo ImportID 
    public String genImportId() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "IM" + ts;
    }

    // =======================
    // Create IMPORT header (chỉ gọi 1 lần khi mở popup)
    // =======================
    public void createImportHeader(String importId) {
        String sql = "INSERT IGNORE INTO IMPORT(ImportID, Note) VALUES(?, ?)";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, importId);
            ps.setString(2, "Restock from UI");
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Create IMPORT failed: " + e.getMessage());
        }
    }

    // =======================
    // Insert hoặc Update IMPORT_DETAIL (cùng ImportID)
    // =======================
    public void upsertImportDetail(
            String importId,
            String productId,
            int qty,
            double costPrice
    ) {
        String sql =
        "INSERT INTO IMPORT_DETAIL(ImportID, ProductID, Quantity, CostPrice) " +
        "VALUES(?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE " +
        "Quantity = VALUES(Quantity), " +
        "CostPrice = VALUES(CostPrice)";


        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, importId);
            ps.setString(2, productId);
            ps.setInt(3, qty);
            ps.setDouble(4, costPrice);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Upsert IMPORT_DETAIL failed: " + e.getMessage());
        }
    }

    // =======================
    // Update PRODUCT (SET quantity, không cộng)
    // =======================
    public void updateProductSetQuantity(
            String productId,
            int qty,
            double costPrice
    ) {
        String sql = "UPDATE PRODUCT SET Quantity = ?, CostPrice = ? WHERE ProductID = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, qty);
            ps.setDouble(2, costPrice);
            ps.setString(3, productId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Update PRODUCT failed: " + e.getMessage());
        }
    }

    // =======================
    // Lấy ProductName (phục vụ ListView)
    // =======================
    public String getProductName(String productId) {
        String sql = "SELECT ProductName FROM PRODUCT WHERE ProductID = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ProductName");
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return "";
    }

        public void resetQuantityIfNewDay() {
            String lockSql = "SELECT ConfigValue FROM SYSTEM_CONFIG WHERE ConfigKey = 'LAST_RESET_DATE' FOR UPDATE";
            String updateProductSql = "UPDATE PRODUCT SET Quantity = 0";
            String updateConfigSql = "UPDATE SYSTEM_CONFIG SET ConfigValue = ? WHERE ConfigKey = 'LAST_RESET_DATE'";

            try (Connection con = ConnectDB.getConnection()) {
                con.setAutoCommit(false);

                LocalDate today = LocalDate.now();
                String lastReset;

                try (PreparedStatement psLock = con.prepareStatement(lockSql);
                     ResultSet rs = psLock.executeQuery()) {

                    if (!rs.next()) {
                        // nếu thiếu key (trường hợp DB chưa insert)
                        throw new RuntimeException("SYSTEM_CONFIG missing key LAST_RESET_DATE");
                    }
                    lastReset = rs.getString(1);
                }

                LocalDate last = LocalDate.parse(lastReset); // format yyyy-MM-dd

                if (!last.equals(today)) {
                    try (PreparedStatement ps1 = con.prepareStatement(updateProductSql);
                         PreparedStatement ps2 = con.prepareStatement(updateConfigSql)) {

                        ps1.executeUpdate();

                        ps2.setString(1, today.toString());
                        ps2.executeUpdate();
                    }
                }

                con.commit();
                con.setAutoCommit(true);

            } catch (Exception e) {
                e.printStackTrace();
         
                throw new RuntimeException("Daily reset failed: " + e.getMessage());
            }
        }
        
        //DELETE mã import rỗng
                public void deleteImportIfEmpty(String importId) {
            String sql =
                "DELETE FROM IMPORT " +
                "WHERE ImportID = ? " +
                "AND NOT EXISTS ( " +
                "    SELECT 1 FROM IMPORT_DETAIL WHERE ImportID = ? " +
                ")";

            try (Connection con = ConnectDB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, importId);
                ps.setString(2, importId);
                ps.executeUpdate();

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Delete empty IMPORT failed: " + e.getMessage());
            }
        }
//Xuat danh sach sp quantity>0
                public List<Product> findAllWithStock() {
    String sql = "SELECT * FROM PRODUCT WHERE Quantity > 0";
    List<Product> list = new ArrayList<>();

    try (Connection con = ConnectDB.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            Product p = new Product();
            p.setProductId(rs.getString("ProductID"));
            p.setProductName(rs.getString("ProductName"));
            p.setQuantity(rs.getInt("Quantity"));
            // set các field khác nếu có
            list.add(p);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return list;
}

                public void reduceQuantity(String productId,int soldQty){
                    String sql= "UPDATE product SET Quantity = Quantity - ? Where ProductID= ?";
                    
                    try(Connection con= ConnectDB.getConnection();
                            PreparedStatement ps= con.prepareStatement(sql)){
                        ps.setInt(1,soldQty);
                        ps.setString(2,productId);
                        ps.executeUpdate();
                    }catch(SQLException e){
                        e.printStackTrace();
                    }
                    
                }
                        
                
                
                

       }
