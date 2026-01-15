/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import model.Customer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author Admin
 */
public class CustomerController implements Initializable {

    @FXML
    private VBox customerFill;
    @FXML
    private TextField cusName;
    @FXML
    private TextField cusPhone;
    @FXML
    private TextField cusAddress;
    @FXML
    private TextField cusEmail;
    @FXML
    private RadioButton genderMale;
    @FXML
    private ToggleGroup genderGroup;
    @FXML
    private RadioButton genderFemale;
    @FXML
    private DatePicker cusDOB;
    @FXML
    private Button cusSave;
    @FXML
    private Button cusBack;
    @FXML
    private Label cusID;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cusSave.setOnAction(e -> saveCustomer());
       cusBack.setOnAction(e -> closeWindow()); 
        
    }    
 
//    public void setcustomer(Customer c){
//        if(c == null) return;
//        cusID.setText(String.valueOf(c.getId()));
//        cusName.setText(c.getName());
//        cusPhone.setText(c.getPhone());
//        cusEmail.setText(c.getEmail());
//        cusAddress.setText(c.getAddress());
//        cusDOB.setValue(c.getDob());
//        
//        if(c.getGender()== Customer.Gender.Male ){
//            genderMale.setSelected(true);
//        }else if(c.getGender() == Customer.Gender.Female){
//            genderFemale.setSelected(true);
//        }
//    }
//      
    private Customer saveCustomer;
    
    private void saveCustomer(){
        if(cusName.getText().isEmpty() || cusPhone.getText().isEmpty()){
            return ;
        }
        saveCustomer = new Customer(
        cusName.getText(),
        cusPhone.getText(),
          genderMale.isSelected() ? Customer.Gender.Male : Customer.Gender.Female,      
        cusDOB.getValue(),
                cusEmail.getText(),
                
        cusAddress.getText(),
                0
        );
        closeWindow();
    }
    
    public Customer getSaveCustomer(){
        return saveCustomer;
    }
    
    private void closeWindow(){
        cusBack.getScene().getWindow().hide();
    }
    
    
    
    
    
    
}
