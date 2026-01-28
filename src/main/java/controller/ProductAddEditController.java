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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javafx.stage.FileChooser;


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
        lblMsg.setText("");

        String id = txtId.getText() == null ? "" : txtId.getText().trim().toUpperCase();
        String name = txtName.getText() == null ? "" : txtName.getText().trim();

        String categoryName = cbCategory.getValue();
        String categoryId = nameToId.getOrDefault(categoryName, categoryName);

        String unit = txtUnit.getText() == null ? "" : txtUnit.getText().trim();
        String image = txtImage.getText() == null ? "" : txtImage.getText().trim();
        String desc = txtDescription.getText() == null ? "" : txtDescription.getText().trim();

        // ===== validate rỗng =====
        if (id.isEmpty() || name.isEmpty() || categoryId == null || categoryId.isBlank()) {
            lblMsg.setText("Please input Id, Name and choose Category.");
            return;
        }

        // ===== validate format ID =====
        if (!id.matches("^PD\\d{2}$")) {
            lblMsg.setText("Product ID must be in format PD00 (e.g. PD01).");
            return;
        }

        // ===== CHECK TRÙNG TÊN (ADD/EDIT) =====
        productDao dao = new productDao();
        if (editingProduct == null) { // ADD
            if (dao.existsName(name)) {
                lblMsg.setText("Product name already exists. Please choose another name.");
                return;
            }
        } else { // EDIT
            if (dao.existsNameEdit(name, editingProduct.getProductId())) {
                lblMsg.setText("Product name already exists. Please choose another name.");
                return;
            }
        }

        // ===== VALIDATE PRICE: phải là số & > 0 =====
        String priceText = txtPrice.getText() == null ? "" : txtPrice.getText().trim();
        if (priceText.isEmpty()) {
            lblMsg.setText("Price is required.");
            return;
        }

        float price;
        try {
            price = Float.parseFloat(priceText);
        } catch (NumberFormatException e) {
            lblMsg.setText("Price must be a valid number.");
            return;
        }

        if (price <= 0) {
            lblMsg.setText("Price must be greater than 0.");
            return;
        }

      
        float costPrice = (editingProduct != null) ? editingProduct.getCostPrice() : 0f;
        int quantity = (editingProduct != null) ? editingProduct.getQuantity() : 0;

        Product p = new Product(id, name, categoryId, quantity, unit, costPrice, price, desc, image);
        result = p;

        ((Stage) btnSave.getScene().getWindow()).close();

    } catch (Exception e) {
        lblMsg.setText("Error when saving into database.");
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
    try {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose product image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        Stage stage = (Stage) btnBrowse.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        String fileName = file.getName();
        String relativePath = "image/" + fileName;

        boolean copied = false;

        Path mavenResources = Path.of("src", "main", "resources", "image");
        if (Files.exists(mavenResources.getParent())) {
            Files.createDirectories(mavenResources);
            Path dest = mavenResources.resolve(fileName);
            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            copied = true;
        } else {
            Path normalResources = Path.of("src", "image");
            if (Files.exists(normalResources.getParent())) {
                Files.createDirectories(normalResources);
                Path dest = normalResources.resolve(fileName);
                Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                copied = true;
            }
        }

        txtImage.setText(relativePath);
        previewImage(relativePath);

        lblPreviewName.setText(txtName.getText() == null ? "" : txtName.getText().trim());
        lblPreviewPrice.setText(txtPrice.getText() == null ? "" : txtPrice.getText().trim());

        lblMsg.setText(copied ? "Selected image: " + relativePath
                              : "Selected image. (Please copy into resources/image if preview fails)");

    } catch (IOException ex) {
        ex.printStackTrace();
        lblMsg.setText("Cannot copy image file.");
    } catch (Exception ex) {
        ex.printStackTrace();
        lblMsg.setText("Browse image failed.");
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

    private Product editingProduct; 
    private Product result;         
  
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
        // ID không cho sửa
        txtId.setDisable(true); 
        
        // preview
        lblPreviewName.setText(product.getProductName());
        lblPreviewPrice.setText(String.valueOf(product.getPrice()));
        previewImage(product.getImage());
        
    } else { // ADD
         lblMode.setText("ADD MODE");
         
        txtId.setText(new productDao().makeProductId());

        txtName.clear();
        cbCategory.getSelectionModel().clearSelection(); 
        txtUnit.clear();
        txtDescription.clear();
        txtImage.clear();
       
        txtPrice.clear();
        
         // default quantity = 0 (không cho sửa)
        lblQuantity.setText("0");

        txtId.setDisable(true);
        
        // preview clear
        lblPreviewName.setText("");
        lblPreviewPrice.setText("");
        imgProduct.setImage(null);
    
    }
}
    
   


    
}
