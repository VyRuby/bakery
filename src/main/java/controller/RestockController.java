package controller;

import DAO_Customer_Order.productDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import model.Product;
import model.RestockItem;

import java.util.function.UnaryOperator;

public class RestockController {

    @FXML private ComboBox<Product> cbProduct;
    @FXML private Spinner<Integer> spQty;
    @FXML private TextField txtCostPrice;
    @FXML private Label lblMsg;
    @FXML private ListView<RestockItem> lvProducts;

    private final productDao dao = new productDao();
    private final ObservableList<RestockItem> restockList = FXCollections.observableArrayList();


    private RestockItem editingItem = null;

    // 1 popup = 1 ImportID
    private String currentImportId;

    @FXML
    public void initialize() {

        // ===== Tạo ImportID cho cả popup + tạo IMPORT header 1 lần =====
        currentImportId = dao.genImportId();
        dao.createImportHeader(currentImportId);
        lblMsg.setText("ImportID: " + currentImportId);

        cbProduct.setItems(FXCollections.observableArrayList(dao.findAll()));
        cbProduct.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product p) {
                return (p == null) ? "" : p.getProductId() + " - " + p.getProductName();
            }
            @Override
            public Product fromString(String s) { return null; }
        });

        spQty.setValueFactory(new IntegerSpinnerValueFactory(1, 100000, 1));

        lvProducts.setItems(restockList);

        // =====Chặn nhập chữ cho Cost Price =====
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

 
            if (newText.isEmpty()) return change;

            // cho phép số và tối đa 2 chữ số thập phân
            if (newText.matches("\\d+(\\.\\d{0,2})?")) return change;

            return null;
        };
        txtCostPrice.setTextFormatter(new TextFormatter<>(filter));

        lvProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            editingItem = newVal;


            Product p = cbProduct.getItems().stream()
                    .filter(x -> x.getProductId().equals(newVal.getProductId()))
                    .findFirst()
                    .orElse(null);
            cbProduct.setValue(p);

            spQty.getValueFactory().setValue(newVal.getQuantity());
            txtCostPrice.setText(String.format("%.2f", newVal.getCostPrice()));

            lblMsg.setText("Editing: " + newVal.getProductId() + " (IM: " + currentImportId + ")");
        });

        // ===== Tự fill costPrice khi chọn product =====
        cbProduct.valueProperty().addListener((obs, oldProduct, newProduct) -> {
            if (newProduct == null) return;
            if (editingItem == null) {
                txtCostPrice.setText(String.format("%.2f", newProduct.getCostPrice()));
            }
        });
    }

    @FXML
    private void onUpdate() {
        lblMsg.setText("");

        Product p = cbProduct.getValue();
        if (p == null) {
            lblMsg.setText("Please choose a product.");
            return;
        }

        int qty = spQty.getValue();
        if (qty <= 0) {
            lblMsg.setText("Quantity must be > 0");
            return;
        }

        double cost;
        try {
            String s = txtCostPrice.getText().trim();
            if (s.isEmpty()) {
                lblMsg.setText("Cost price is required.");
                return;
            }
            cost = Double.parseDouble(s);
        } catch (Exception e) {
            lblMsg.setText("Cost price must be a number.");
            return;
        }
        if (cost < 0) {
            lblMsg.setText("Cost price must be >= 0");
            return;
        }

        try {

            String importId = currentImportId;
            String productId = p.getProductId();

            dao.upsertImportDetail(importId, productId, qty, cost);

            // không cộng dồn
            dao.updateProductSetQuantity(productId, qty, cost);

            if (editingItem == null) {
                RestockItem item = new RestockItem(importId, productId, p.getProductName(), qty, cost);
                restockList.add(0, item);
                lblMsg.setText("Saved: " + productId + " (IM: " + importId + ")");
            } else {
                editingItem.setQuantity(qty);
                editingItem.setCostPrice(cost);
                lvProducts.refresh();
                lblMsg.setText("Updated: " + productId + " (IM: " + importId + ")");
            }

            clearForm();

        } catch (Exception ex) {
            ex.printStackTrace();
            lblMsg.setText("Error: " + ex.getMessage());
        }
    }

    @FXML
    private void onClose() {
        if (restockList.isEmpty() && currentImportId != null) {
            try {
                dao.deleteImportIfEmpty(currentImportId);
            } catch (Exception ignored) {}
        }
        txtCostPrice.getScene().getWindow().hide();
    }

    private void clearForm() {
        cbProduct.getSelectionModel().clearSelection();
        spQty.getValueFactory().setValue(1);
        txtCostPrice.clear();
        lvProducts.getSelectionModel().clearSelection();
        editingItem = null;
    }
}
