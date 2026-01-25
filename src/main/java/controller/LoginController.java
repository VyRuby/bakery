package controller;

import app.App;
import app.Session;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblError;

    @FXML
    private void onLogin()  {

       String username = txtUsername.getText().trim();
    String password = txtPassword.getText().trim();

    lblError.setText("");

    if (username.isEmpty() || password.isEmpty()) {
        lblError.setText("Please enter username and password");
        return;
    }

    try {
        // ===== CONNECT MYSQL =====
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bakery_db",
                username,
                password
        );

        Session.conn = conn;
        Session.dbUser = username;

        // ===== CHECK EMPLOYEE =====
        String sql = """
            SELECT Position
            FROM EMPLOYEE
            WHERE Email = ? AND Status = 'Active'
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                lblError.setText("Account not found or inactive");
                Session.clear();
                return;
            }

            String position = rs.getString("Position");

            Session.role = position.equalsIgnoreCase("Manager")
                    ? "MANAGER"
                    : "EMPLOYEE";
        }

        // ===== LOGIN SUCCESS =====
        App.setRoot("Home");

    } catch (SQLException e) {
        // ❌ SAI USERNAME / PASSWORD MYSQL
        lblError.setText("Invalid database username or password");
    } catch (IOException e) {
        lblError.setText("Cannot load Home screen");
    }
}
}
