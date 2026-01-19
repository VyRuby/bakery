package controller;

import app.App;
import app.Session;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    @FXML
    private void onLogin() {

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Please enter username and password");
            return;
        }

        try {
            // ===== LOGIN = TRY DB CONNECTION =====
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bakery_db",
                username,
                password
            );

            // ===== SAVE SESSION =====
            Session.conn = conn;
            Session.dbUser = username;

            if ("manager_user".equals(username)) {
                Session.role = "MANAGER";
            } else if ("employee_user".equals(username)) {
                Session.role = "EMPLOYEE";
            } else {
                Session.role = "UNKNOWN";
            }

            try {
                App.setRoot("Home");
            } catch (IOException ex) {
                System.getLogger(LoginController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }

        } catch (SQLException e) {
            lblError.setText("Invalid MySQL username or password");
        }
    }
}
