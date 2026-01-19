/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import DAO_Customer_Order.productDao;
import model.Product;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Admin
 */
public class ProductAddEditController implements Initializable {

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblMode;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSave;
    @FXML
    private TextField txtId;
    @FXML
    private TextField txtName;
    @FXML
    private ComboBox<String> cbCategory;
    @FXML
    private TextField txtUnit;
//    @FXML
//    private TextField txtCostPrice;
    @FXML
    private TextField txtPrice;
    @FXML
    private Label lblQuantity;
    @FXML
    private TextField txtImage;
    @FXML
    private Button btnBrowse;
    @FXML
    private TextArea txtDescription;
    @FXML
    private Label lblMsg;
    @FXML
    private ImageView imgProduct;
    @FXML
    private Label lblPreviewName;
    @FXML
    private Label lblPreviewPrice;

    private final Map<String, String> idToName = Map.of(
    "C01", "Baked",
    "C02", "Cake",
    "C03", "Cookie"
);

private final Map<String, String> nameToId = Map.of(
    "Baked", "C01",
    "Cake", "C02",
    "Cookie", "C03"
);

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbCategory.getItems().setAll("Baked", "Cake", "Cookie");
    }    
    
    @FXML
private void handleSave(ActionEvent event) {
    try {
        String id = txtId.getText().trim().toUpperCase();
        String name = txtName.getText().trim();
        
        //hiển thị name, lưu ID
        String categoryName = cbCategory.getValue();
        String categoryId = nameToId.getOrDefault(categoryName, categoryName);

        String unit = txtUnit.getText().trim();
        String image = txtImage.getText().trim();
        String desc = txtDescription.getText().trim();

        // validate rỗng
        if (id.isEmpty() || name.isEmpty() || categoryId == null || categoryId.isBlank()) {
            lblMsg.setText("Please input Id, Name and choose Category.");
            return;
        }
        
        // validate format
        if (!id.matches("^PD\\d{2}$")) {
            lblMsg.setText("Product ID must be in format PD00 (e.g. PD01).");
            return;
        }
        
        // check trùng ID
        if (editingProduct == null) {
            boolean check = productDao.exists(id); 
            if (check) {
                lblMsg.setText("This Product ID already exists. Please choose another one.");
                return;
            }
        }

        float price = Float.parseFloat(txtPrice.getText().trim());
//        float costPrice = Float.parseFloat(txtCostPrice.getText());
        // KHÔNG lấy từ TextField nữa
        float costPrice = (editingProduct != null)
                ? editingProduct.getCostPrice()   // Edit → giữ nguyên
                : 0f;                             // Add → mặc định 0


        // quantity không chỉnh: giữ nguyên nếu Edit, còn Add thì = 0
        int quantity = (editingProduct != null) ? editingProduct.getQuantity() : 0;

       Product p = new Product(id, name, categoryId, quantity, unit, costPrice, price, desc, image);
        result = p;

        ((Stage) btnSave.getScene().getWindow()).close();

    } catch (NumberFormatException e) {
        lblMsg.setText("Price is invalid.");
    } catch (Exception e) {
        lblMsg.setText("Error when saving  into database.");
        e.printStackTrace();
    }
}

    @FXML
private void onCancel(ActionEvent event) {
    result = null;
    ((Stage) btnCancel.getScene().getWindow()).close();
}

    @FXML
    private void onBrowseImage(ActionEvent event) {
    }

    private Product editingProduct; // null = Add, != null = Edit
    private Product result;         // product trả về cho controller cha
  
    //SETMODE
    public void setMode(Product product) {
    this.editingProduct = product;
     lblMsg.setText("");

    if (product != null) { // EDIT
        lblMode.setText("EDIT MODE");
        txtId.setText(product.getProductId());
        txtName.setText(product.getProductName());
        cbCategory.setValue(idToName.getOrDefault(product.getCategoryId(), product.getCategoryId()));
        txtUnit.setText(product.getUnit());
        txtDescription.setText(product.getDescription());
        txtImage.setText(product.getImage());
        txtPrice.setText(String.valueOf(product.getPrice()));
        
         // quantity chỉ hiển thị
        lblQuantity.setText(String.valueOf(product.getQuantity()));

        txtId.setDisable(true); // ID không cho sửa
        
        // preview
        lblPreviewName.setText(product.getProductName());
        lblPreviewPrice.setText(String.valueOf(product.getPrice()));
        previewImage(product.getImage());
        
    } else { // ADD
         lblMode.setText("ADD MODE");
         
        txtId.clear();
        txtName.clear();
        cbCategory.getSelectionModel().clearSelection(); 
        txtUnit.clear();
        txtDescription.clear();
        txtImage.clear();
       
        txtPrice.clear();
        
         // default quantity = 0 (không cho sửa)
        lblQuantity.setText("0");

        txtId.setDisable(false);
        
        // preview clear
        lblPreviewName.setText("");
        lblPreviewPrice.setText("");
        imgProduct.setImage(null);
    
    }
}
    
    private void previewImage(String path) {
    try {
        if (path == null || path.isBlank()) {
            imgProduct.setImage(null);
            return;
        }
        Image img = new Image(getClass().getResourceAsStream("/" + path));
        imgProduct.setImage(img);
    } catch (Exception e) {
        imgProduct.setImage(null);
    }
}

    Product getResult() {
        return result;

    }


    
}
