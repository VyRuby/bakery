package controller;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainEmpController {

    @FXML private TabPane tabPane;
    @FXML private Tab tabProduct;

    @FXML
    public void initialize() {
        tabPane.getSelectionModel().select(tabProduct);
    }
}
