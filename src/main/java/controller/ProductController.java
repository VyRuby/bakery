/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
/**
 * FXML Controller class
 *
 * @author vy
 */
public class ProductController implements Initializable {


    @FXML
    private ToggleGroup categoryGroup;
    @FXML
    private Label lblUser;
    @FXML
    private Label lblStatus;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnLogout;
    @FXML
    private RadioButton rbCat1;
    @FXML
    private RadioButton rbCat2;
    @FXML
    private RadioButton rbCat3;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<?> cbFilter;
    @FXML
    private Button btnBack;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnRefresh;
    @FXML
    private TableView<?> tblProducts;
    @FXML
    private TableColumn<?, ?> colId;
    @FXML
    private TableColumn<?, ?> colName;
    @FXML
    private TableColumn<?, ?> colCategory;
    @FXML
    private TableColumn<?, ?> colPrice;
    @FXML
    private TableColumn<?, ?> colQuantity;
    @FXML
    private TableColumn<?, ?> colUnit;
    @FXML
    private Label lblHint;
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
    private void onCategoryChanged(ActionEvent event) {
    }

    @FXML
    private void onSearchChanged(KeyEvent event) {
    }

    @FXML
    private void onFilterChanged(ActionEvent event) {
    }

    @FXML
    private void onBack(ActionEvent event) {
    }

    @FXML
    private void onAdd(ActionEvent event) {
    }

    @FXML
    private void onEdit(ActionEvent event) {
    }

    @FXML
    private void onDelete(ActionEvent event) {
    }

    @FXML
    private void onRefresh(ActionEvent event) {
    }

}
