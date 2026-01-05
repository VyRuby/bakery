/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import DAO_Customer_Order.CustomerDao;
import model.Customer;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

/**
 * FXML Controller class
 *
 * @author Admin
 */
public class CustomerDataController extends BacktoHomeController implements Initializable {

    @FXML
    private ListView<Customer> ListCustomer;
    @FXML
    private TextField findtextcustomer;
    @FXML
    private Button selectbutton;
    @FXML
    private TextField cName;
    @FXML
    private TextField cPhone;
    @FXML
    private TextField cEmail;
    @FXML
    private TextField cAddress;
    @FXML
    private ToggleGroup cGender;
    @FXML
    private DatePicker cDOB;
    @FXML
    private RadioButton cGenMale;
    @FXML
    private RadioButton cGenFemale;
    @FXML
    private Button cAddNew;
    @FXML
    private Button cDelete;
    @FXML
    private Button cSave;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    allCustomers.setAll(customerDao.findAll() != null ? customerDao.findAll() : FXCollections.observableArrayList());
    ListCustomer.setItems(allCustomers);
    
    ListCustomer.getSelectionModel().selectedItemProperty().addListener((obs,oldC,newC) -> {
        if(newC != null){
            loadCustomerDetail(newC.getId());
        }
    });
    
    
    ListCustomer.setCellFactory(lv -> new javafx.scene.control.ListCell<Customer>(){
        @Override 
        protected void updateItem(Customer c, boolean empty){
        super.updateItem(c,empty);
        if(empty|| c==null){
            setText(null);
            
        }else{
            setText(c.getName()+ " - " + c.getPhone() );
        }
    }
    });
    
    findtextcustomer.textProperty().addListener((observable,oldVable, newVable)-> {
        if(newVable.isEmpty()){
            ListCustomer.setItems(allCustomers);
        }else{
            List<Customer> result = customerDao.findByKeyword(newVable);
            ListCustomer.setItems(FXCollections.observableArrayList(result));
        }
    });
    
    cAddNew.setOnAction(e -> clearCustomerForm());
    }    
    
    private CustomerDao customerDao = new CustomerDao();
    private ObservableList<Customer>allCustomers = FXCollections.observableArrayList();
    
    private void onFindCustomer(){
        String keyword = findtextcustomer.getText().trim();
        
        if(keyword.isEmpty()){
            ListCustomer.setItems(allCustomers);
            return ;}else{
            ListCustomer.setItems(
            FXCollections.observableArrayList(
            customerDao.findByKeyword(keyword)
            )
            );
        }
    }
    
    
    
    private void loadCustomerDetail(int customerId){
        Customer fullCustomer = customerDao.findByID(customerId);
        if(fullCustomer !=null){
            showCustomerToForm(fullCustomer);
        }
    }
    
    private void showCustomerToForm (Customer c){
        cName.setText(c.getName());
        cPhone.setText(c.getPhone());
        cEmail.setText(c.getEmail());
        cAddress.setText(c.getAddress());
        
        if(c.getGender() == Customer.Gender.Male){
            cGenMale.setSelected(true);
            
        }else if (c.getGender() == Customer.Gender.Female){
            cGenFemale.setSelected(true);
            
        }
        
        cDOB.setValue(c.getDob());
        
    }
    
    private void clearCustomerForm(){
        cName.clear();
        cPhone.clear();
        cEmail.clear();
        cAddress.clear();
        cDOB.setValue(null);
        cGender.selectToggle(null);
        
        ListCustomer.getSelectionModel().clearSelection();
    }
    
    
    
}
