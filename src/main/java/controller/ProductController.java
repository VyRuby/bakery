/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import DAO_Customer_Order.productDao;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Product;
/**
 * FXML Controller class
 *
 * @author vy
 */
public class ProductController extends BacktoHomeController implements Initializable {

   
    
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
    private CheckBox cbCat1;
    @FXML
    private CheckBox cbCat2;
    @FXML
    private CheckBox cbCat3;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbFilter;
  
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnRefresh;
    
//   PRODUCT TABLE
    @FXML private TableView<Product> tblProducts;
    @FXML private TableColumn<Product, String> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colQuantity;
    @FXML private TableColumn<Product, String> colUnit;

    @FXML
    private Label lblHint;
    
    private productDao productDao = new productDao();
    
    private ObservableList<Product> prodList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredList;
    private SortedList<Product> sortedList;
    
    private final Map<String, String> categoryMap = Map.of(
    "C01", "Baked",
    "C02", "Cake",
    "C03", "Cookie"
);
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        //Show cat name từ catID
        colCategory.setCellValueFactory(cellData -> {
            String catId = cellData.getValue().getCategoryId();
            String catName = categoryMap.getOrDefault(catId, catId);
            return new SimpleStringProperty(catName);
        });
        
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        viewTable();
        
        filteredList = new FilteredList<>(prodList, p -> true);
        sortedList = new SortedList<>(filteredList);

        sortedList.comparatorProperty().bind(tblProducts.comparatorProperty());
        tblProducts.setItems(sortedList);

        // ví dụ dữ liệu filter
        cbFilter.getItems().setAll("All", "In Stock", "Out of Stock");
        cbFilter.setValue("All");
        
    }    
    
    private void viewTable() {
        prodList.setAll(productDao.findAll());
    }
    
    
    @FXML
    private void onLogin(ActionEvent event) {
    }

    @FXML
    private void onLogout(ActionEvent event) {
    }

    @FXML
    private void onCategoryChanged(ActionEvent event) {
        applyFilter();
    }

    @FXML
    private void onSearchChanged(KeyEvent event) {
        applyFilter();
    }

    @FXML
    private void onFilterChanged(ActionEvent event) {
        applyFilter();
    }
    
    //============METHOD POPUP CHUNG=========
    private Product showProductPopup(Product product) {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/ProductAddEdit.fxml")
        );
        Parent root = loader.load();

        ProductAddEditController ctrl = loader.getController();
        ctrl.setMode(product); // null = Add, != null = Edit

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(product == null ? "Add Product" : "Edit Product");
        stage.setScene(new Scene(root));
        stage.showAndWait();

        return ctrl.getResult(); // nhận Product từ popup

    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

    @FXML
    private void onAdd(ActionEvent event) {
     Product p = showProductPopup(null);
    if (p == null) return;

    try {
        productDao.insert(p);
//        prodList.add(p);
        viewTable(); 
        showInfo("Success", "Product added successfully.");
    } catch (Exception e) {
        e.printStackTrace();
        showError("Error", "Add Failed", "Unable to add the product.");
    }
    }

  @FXML
private void onEdit(ActionEvent event) {
    Product selected = tblProducts.getSelectionModel().getSelectedItem();
   if (selected == null) {
        showWarning("Warning", "Please select a product to edit.");
        return;
    }

    Product updated = showProductPopup(selected);
    if (updated != null) {

        // đảm bảo quantity không đổi
        updated.setQuantity(selected.getQuantity());

         try {
        productDao.update(updated);

        int index = prodList.indexOf(selected);
        prodList.set(index, updated);
        viewTable();
        showInfo("Success", "Product updated successfully.");
    } catch (Exception e) {
        e.printStackTrace();
        showError("Error", "Update Failed", "Unable to update the product.");
    }
    }
}


    @FXML
 private void onDelete(ActionEvent event) {
     Product selected = tblProducts.getSelectionModel().getSelectedItem();
     if (selected == null) {
         showWarning("Warning", "Please select a product to delete.");
         return;
     }

     boolean check = confirm(
         "Confirm Deletion",
         "Are you sure you want to delete this product?",
         "Product ID: " + selected.getProductId() + "\n" +
         "Product Name: " + selected.getProductName()
     );

     if (!check) return;

     try {
         productDao.delete(selected.getProductId());
         prodList.remove(selected);
         showInfo("Success", "Product deleted successfully.");
     } catch (Exception e) {
         e.printStackTrace();
         showError("Error", "Delete Failed", "Unable to delete the product.");
     }
 }

    @FXML
    private void onRefresh(ActionEvent event) {
     
    try {
        prodList.setAll(productDao.findAll());
        tblProducts.getSelectionModel().clearSelection();
        applyFilter();
        showInfo("Refreshed", "Product list has been refreshed.");
    } catch (Exception e) {
        e.printStackTrace();
        showError("Error", "Refresh Failed", "Unable to refresh product list.");
    }

    }

//============POP-UP ALERT/ NOTI=====================
    private void showInfo(String title, String message) {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setTitle(title);
    a.setHeaderText(null);
    a.setContentText(message);
    a.getDialogPane().getStylesheets().add(
        getClass().getResource("/css/AlertNoti.css").toExternalForm()
    );
    a.showAndWait();
}

private void showWarning(String title, String message) {
    Alert a = new Alert(Alert.AlertType.WARNING);
    a.setTitle(title);
    a.setHeaderText(null);
    a.setContentText(message);
    a.getDialogPane().getStylesheets().add(
        getClass().getResource("/css/AlertNoti.css").toExternalForm()
    );
    a.showAndWait();
}

private void showError(String title, String header, String message) {
    Alert a = new Alert(Alert.AlertType.ERROR);
    a.setTitle(title);
    a.setHeaderText(header);
    a.setContentText(message);
    a.getDialogPane().getStylesheets().add(
        getClass().getResource("/css/AlertNoti.css").toExternalForm()
    );
    a.showAndWait();
}

private boolean confirm(String title, String header, String message) {
    Alert a = new Alert(Alert.AlertType.CONFIRMATION);
    a.setTitle(title);
    a.setHeaderText(header);
    a.setContentText(message);
    a.getDialogPane().getStylesheets().add(
        getClass().getResource("/css/AlertNoti.css").toExternalForm()
    );
    Optional<ButtonType> res = a.showAndWait();
    return res.isPresent() && res.get() == ButtonType.OK;
}

//METHOD FILTER CHUNG
private void applyFilter() {

    String keyword = txtSearch.getText() == null ? ""
            : txtSearch.getText().toLowerCase().trim();

    boolean cat1 = cbCat1.isSelected();
    boolean cat2 = cbCat2.isSelected();
    boolean cat3 = cbCat3.isSelected();
    String filter = cbFilter.getValue();
    


    filteredList.setPredicate(product -> {
        String catId = product.getCategoryId();
        String catName = categoryMap.getOrDefault(catId, catId); 
        // ===== SEARCH =====
        boolean matchSearch = keyword.isEmpty()
                || product.getProductName().toLowerCase().contains(keyword)
                || product.getProductId().toLowerCase().contains(keyword);

        // ===== CATEGORY =====
        boolean matchCategory =
        (!cat1 && !cat2 && !cat3) // không tick cái nào → lấy tất cả
        || (cat1 && "Bread".equalsIgnoreCase(catName))
        || (cat2 && "Cake".equalsIgnoreCase(catName))
        || (cat3 && "Cookie".equalsIgnoreCase(catName));


        // ===== FILTER =====
        boolean matchFilter = true;
        if ("In Stock".equals(filter)) {
            matchFilter = product.getQuantity() > 0;
        } else if ("Out of Stock".equals(filter)) {
            matchFilter = product.getQuantity() == 0;
        }

        return matchSearch && matchCategory && matchFilter;
    });
}

    }    
