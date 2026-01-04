package controller;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */


import app.projectbakery.App;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 * FXML Controller class
 *
 * @author vy
 */
public class HomeController implements Initializable {

    @FXML
    private Label lblUser;
    @FXML
    private Label lblStatus;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnLogout;
    @FXML
    private GridPane gridMenu;
    @FXML
    private Button btnProduct;
    @FXML
    private Button btnEmployee;
    @FXML
    private Button btnOrder;
    @FXML
    private Button btnCustomer;
    @FXML
    private Button btnPromotion;
    @FXML
    private Button btnReport;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void onLogin(ActionEvent event) {
    }

    @FXML
    private void onLogout(ActionEvent event) {
    }

    @FXML
    private void goProduct(ActionEvent event)throws IOException {
         System.out.println("CLICK PRODUCT");
    try {
        App.setRoot("Product"); 
        System.out.println("NAVIGATED TO PRODUCT");
    } catch (Exception e) {
        e.printStackTrace(); 
    }
    }

    @FXML
    private void goPromotion(ActionEvent event) {
    }

    @FXML
    private void goOrder(ActionEvent event) {
    }

    @FXML
    private void goCustomer(ActionEvent event) {
    }
    @FXML
     private void goEmployee(ActionEvent event) {
          System.out.println("CLICK EMPLOYEE");
    try {
        App.setRoot("MainEmp"); 
        System.out.println("NAVIGATED TO EMPLOYEE");
    } catch (Exception e) {
        e.printStackTrace(); 
    }
    }

   
    @FXML
    private void goReport(ActionEvent event) {
    }

   
    
}