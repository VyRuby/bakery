/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import model.Product;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Admin
 */
public class ProductDetailController implements Initializable {

    @FXML
    private ImageView ImageProduct;
    @FXML
    private Label NameDetail;
    @FXML
    private Label PriceDetail;
    @FXML
    private Label QuantityDetail;
    @FXML
    private Label CategoryDetail;
    @FXML
    private Spinner<Integer> SpinPick;
    @FXML
    private Label DecriptionDetail;
    @FXML
    private Button SaveButton;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    private Product product;
    private int selectedQuantity;
    
    public void setProduct (Product product){
        this.product = product;
        
        NameDetail.setText(product.getProductName());
        PriceDetail.setText(product.getPrice() + "USD");
        QuantityDetail.setText(product.getQuantity() + product.getUnit());
        CategoryDetail.setText(product.getCategoryId());
        DecriptionDetail.setText(product.getDescription());
        
        try{
            Image img = new Image(
            getClass().getResourceAsStream("/"+ product.getImage())
            );
            ImageProduct.setImage(img);
        }catch(Exception e){
            ImageProduct.setImage(null);
            
        }
        
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,product.getQuantity(),0);
        SpinPick.setValueFactory(valueFactory);
        
    }
    @FXML
    private void handleSave(){
        selectedQuantity = SpinPick.getValue();
        Stage stage = (Stage)SaveButton.getScene().getWindow();
        stage.close();
    }
    
    public int getSelectedQuantity(){
        return selectedQuantity;
    }
    
    public Product getProduct(){
        return product;
    }

public void setQuantity(int quantity){
    if(SpinPick !=null && SpinPick.getValueFactory() != null){
        SpinPick.getValueFactory().setValue(quantity);
    }
}    
}
