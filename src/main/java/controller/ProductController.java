/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import DAO_Customer_Order.productDao;
import java.net.URL;
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
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Product;

public class ProductController extends BacktoHomeController implements Initializable {

    @FXML private Label lblUser;
    @FXML private Label lblStatus;
    @FXML private Button btnLogin;
    @FXML private Button btnLogout;

    @FXML private CheckBox cbCat1;
    @FXML private CheckBox cbCat2;
    @FXML private CheckBox cbCat3;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbFilter;

    @FXML private Button btnRestock;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;

    // PRODUCT TABLE
    @FXML private TableView<Product> tblProducts;
    @FXML private TableColumn<Product, String> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colCostPrice;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colQuantity;
    @FXML private TableColumn<Product, String> colUnit;

    @FXML private Label lblHint;

    private final productDao productDao = new productDao();

    private final ObservableList<Product> prodList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredList;
    private SortedList<Product> sortedList;

    // map CategoryID -> CategoryName (đúng theo DB của bạn)
    private final Map<String, String> categoryMap = Map.of(
            "C01", "Baked",
            "C02", "Cake",
            "C03", "Cookie"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // daily reset
        productDao.resetQuantityIfNewDay();

        // table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));

        colCategory.setCellValueFactory(cellData -> {
            String catId = cellData.getValue().getCategoryId();
            String catName = categoryMap.getOrDefault(catId, catId);
            return new SimpleStringProperty(catName);
        });

        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colCostPrice.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        // load data
        viewTable();

        filteredList = new FilteredList<>(prodList, p -> true);
        sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tblProducts.comparatorProperty());
        tblProducts.setItems(sortedList);

        // FILTER
        cbFilter.getItems().setAll(
                "All",
                "Active",
                "Inactive",
                "In Stock",
                "Out of Stock",
                "Active - In Stock",
                "Active - Out of Stock"
        );
        cbFilter.setValue("All");

        // Inactive mờ đi
        tblProducts.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                String st = safeStatus(item);
                if ("Inactive".equalsIgnoreCase(st)) {
                    setStyle("-fx-opacity: 0.55;");
                } else {
                    setStyle("");
                }
            }
        });

        // disable Edit/Delete khi chọn Inactive
        tblProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                btnEdit.setDisable(true);
                btnDelete.setDisable(true);
                return;
            }
            boolean inactive = "Inactive".equalsIgnoreCase(safeStatus(newVal));
            btnEdit.setDisable(inactive);
            btnDelete.setDisable(inactive);
        });

    
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
    }

    private void viewTable() {
        // find all Active + Inactive
        prodList.setAll(productDao.findAll());

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

    // ===== popup product add/edit =====
    private Product showProductPopup(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductAddEdit.fxml"));
            Parent root = loader.load();

            ProductAddEditController ctrl = loader.getController();
            ctrl.setMode(product); // null=Add, != null=Edit

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(product == null ? "Add Product" : "Edit Product");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            return ctrl.getResult();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Popup Failed", "Cannot open ProductAddEdit popup.");
            return null;
        }
    }

    @FXML
    private void onAdd(ActionEvent event) {
        Product p = showProductPopup(null);
        if (p == null) return;

        try {
            // Add mới -> mặc định Active (nếu model có status)
            // Nếu Product constructor chưa có status thì bỏ qua cũng ok.
            try {
                p.setStatus("Active");
            } catch (Exception ignore) {}

            productDao.insert(p);
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

        if ("Inactive".equalsIgnoreCase(safeStatus(selected))) {
            showWarning("Warning", "Inactive product cannot be edited.");
            return;
        }

        Product updated = showProductPopup(selected);
        if (updated == null) return;

        // đảm bảo quantity giữ nguyên + status giữ nguyên
        updated.setQuantity(selected.getQuantity());
        try { updated.setStatus(safeStatus(selected)); } catch (Exception ignore) {}

        try {
            productDao.update(updated);
            viewTable();
            showInfo("Success", "Product updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Update Failed", "Unable to update the product.");
        }
    }

    @FXML
    private void onDelete(ActionEvent event) {
        Product selected = tblProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Warning", "Please select a product to delete.");
            return;
        }

        if ("Inactive".equalsIgnoreCase(safeStatus(selected))) {
            showWarning("Warning", "This product is already inactive.");
            return;
        }

        boolean ok = confirm(
                "Confirm",
                "Set product to Inactive?",
                "Product ID: " + selected.getProductId() + "\n"
                        + "Product Name: " + selected.getProductName() + "\n\n"
                        + "This will keep import/promo/order history."
        );

        if (!ok) return;

        try {
            // ✅ soft delete
            productDao.delete(selected.getProductId()); // bạn đã đổi delete() thành UPDATE Status='Inactive'
            viewTable();
            showInfo("Success", "Product set to Inactive.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Delete Failed", "Unable to deactivate the product.");
        }
    }

    @FXML
    private void onRefresh(ActionEvent event) {
        try {
            viewTable();
            tblProducts.getSelectionModel().clearSelection();
            showInfo("Refreshed", "Product list has been refreshed.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Refresh Failed", "Unable to refresh product list.");
        }
    }

    // ===== Restock popup =====
    @FXML
    private void onRestock(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Restock.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Restock");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // sau restock -> reload bảng product
            viewTable();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Restock Failed", "Cannot open restock popup.");
        }
    }

    // ===== FILTER =====
private void applyFilter() {

    final String keyword = (txtSearch.getText() == null)
            ? ""
            : txtSearch.getText().toLowerCase().trim();

    final boolean cat1 = cbCat1.isSelected();
    final boolean cat2 = cbCat2.isSelected();
    final boolean cat3 = cbCat3.isSelected();

    final String filter = (cbFilter.getValue() == null) ? "All" : cbFilter.getValue();

    filteredList.setPredicate(product -> {
        if (product == null) return false;

        String catId = product.getCategoryId();
        String catName = categoryMap.getOrDefault(catId, catId);

        // SEARCH
        boolean matchSearch = keyword.isEmpty()
                || (product.getProductName() != null && product.getProductName().toLowerCase().contains(keyword))
                || (product.getProductId() != null && product.getProductId().toLowerCase().contains(keyword));

        // CATEGORY
        boolean matchCategory =
                (!cat1 && !cat2 && !cat3)
                || (cat1 && "Baked".equalsIgnoreCase(catName))
                || (cat2 && "Cake".equalsIgnoreCase(catName))
                || (cat3 && "Cookie".equalsIgnoreCase(catName));

        // STATUS + STOCK filter
        String st = safeStatus(product);
        boolean isActive = "Active".equalsIgnoreCase(st);
        boolean isInactive = "Inactive".equalsIgnoreCase(st);

        boolean matchFilter;

        switch (filter) {
            case "Active":
                matchFilter = isActive;
                break;
            case "Inactive":
                matchFilter = isInactive;
                break;
            case "In Stock":
                matchFilter = product.getQuantity() > 0;
                break;
            case "Out of Stock":
                matchFilter = product.getQuantity() == 0;
                break;
            case "Active - In Stock":
                matchFilter = isActive && product.getQuantity() > 0;
                break;
            case "Active - Out of Stock":
                matchFilter = isActive && product.getQuantity() == 0;
                break;
            default:
                matchFilter = true; // All
        }

        return matchSearch && matchCategory && matchFilter;
    });
}

// nếu status null -> coi như Active để không crash
private String safeStatus(Product p) {
    try {
        String s = p.getStatus();
        return (s == null || s.isBlank()) ? "Active" : s;
    } catch (Exception e) {
        return "Active";
    }
}


    // ===== Alerts =====
    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/AlertNoti.css").toExternalForm());
        a.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/AlertNoti.css").toExternalForm());
        a.showAndWait();
    }

    private void showError(String title, String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(message);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/AlertNoti.css").toExternalForm());
        a.showAndWait();
    }

    private boolean confirm(String title, String header, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(message);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/AlertNoti.css").toExternalForm());
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }
}
