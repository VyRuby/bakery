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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

/**
 * FXML Controller class
 *
 * @author vy
 */
public class PromotionController extends BacktoHomeController implements Initializable {

    @FXML
    private Label lblUser;
    @FXML
    private Label lblStatus;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<?> cbFilter;
    @FXML
    private Button btnRefresh;
   
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnExport;
    @FXML
    private TableView<?> tblPromotions;
    @FXML
    private TableColumn<?, ?> colId;
    @FXML
    private TableColumn<?, ?> colCode;
    @FXML
    private TableColumn<?, ?> colName;
    @FXML
    private TableColumn<?, ?> colDiscount;
    @FXML
    private TableColumn<?, ?> colStartDate;
    @FXML
    private TableColumn<?, ?> colEndDate;
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
    private void onSearchChanged(KeyEvent event) {
    }

    @FXML
    private void onFilterChanged(ActionEvent event) {
    }

    @FXML
    private void onRefresh(ActionEvent event) {
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
    private void onExport(ActionEvent event) {
    }
    
}
