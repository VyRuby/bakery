package app;

import java.sql.Connection;

public class Session {
    public static String dbUser;
    public static String role;
    public static Connection conn;

    public static void clear() {
        try {
            if (conn != null) conn.close();
        } catch (Exception e) {
        }
        conn = null;
        dbUser = null;
        role = null;
    }
}
