package DAO_Customer_Order;

import app.Session;
import java.sql.*;

/**
 *
 * @author Admin
 */
public class OrderDao {

    /**
     * Insert Order
     * Quyền INSERT được MySQL kiểm soát (GRANT)
     * MANAGER / EMPLOYEE đều dùng chung
     */
    public int insertOrder(int customerId, float total, String paymentMethod)
            throws SQLException {

       String sql = "INSERT INTO orders (CustomerID, OrderDate, Total, PaymentMethod)\n"
           + "VALUES (?, NOW(), ?, ?)";


        try (
            PreparedStatement ps = Session.conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setInt(1, customerId);
            ps.setFloat(2, total);
            ps.setString(3, paymentMethod);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // OrderID vừa tạo
            }

        } catch (SQLException e) {
            // Nếu employee vượt quyền → MySQL tự chặn
            e.printStackTrace();
            throw e;
        }

        return -1;
    }
}
