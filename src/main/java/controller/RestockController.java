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

    // Nếu null -> tạo mới dòng trong list, nếu != null -> đang sửa dòng đó
    private RestockItem editingItem = null;

    // 1 popup = 1 ImportID
    private String currentImportId;

    @FXML
    public void initialize() {

        // ===== 1) Tạo ImportID cho cả popup + tạo IMPORT header 1 lần =====
        currentImportId = dao.genImportId();
        dao.createImportHeader(currentImportId);
        lblMsg.setText("ImportID: " + currentImportId);

        // ===== 2) Load products vào ComboBox =====
        cbProduct.setItems(FXCollections.observableArrayList(dao.findAll()));
        cbProduct.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product p) {
                return (p == null) ? "" : p.getProductId() + " - " + p.getProductName();
            }
            @Override
            public Product fromString(String s) { return null; }
        });

        // ===== 3) Spinner qty =====
        spQty.setValueFactory(new IntegerSpinnerValueFactory(1, 100000, 1));

        // ===== 4) ListView =====
        lvProducts.setItems(restockList);

        // ===== 5) Chặn nhập chữ cho Cost Price =====
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            // cho phép rỗng (user xoá)
            if (newText.isEmpty()) return change;

            // cho phép số và tối đa 2 chữ số thập phân
            if (newText.matches("\\d+(\\.\\d{0,2})?")) return change;

            return null;
        };
        txtCostPrice.setTextFormatter(new TextFormatter<>(filter));

        // ===== 6) Click dòng trong list -> đổ dữ liệu lên form để sửa =====
        lvProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            editingItem = newVal;

            // set combobox theo productId
            Product p = cbProduct.getItems().stream()
                    .filter(x -> x.getProductId().equals(newVal.getProductId()))
                    .findFirst()
                    .orElse(null);
            cbProduct.setValue(p);

            spQty.getValueFactory().setValue(newVal.getQuantity());
            txtCostPrice.setText(String.format("%.2f", newVal.getCostPrice()));

            lblMsg.setText("Editing: " + newVal.getProductId() + " (IM: " + currentImportId + ")");
        });

        // ===== 7) Tự fill costPrice khi chọn product (chỉ khi không edit) =====
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
            // ===== luôn dùng ImportID của popup =====
            String importId = currentImportId;
            String productId = p.getProductId();

            // 1) upsert IMPORT_DETAIL (cùng importId)
            dao.upsertImportDetail(importId, productId, qty, cost);

            // 2) update PRODUCT (SET quantity, không cộng)
            dao.updateProductSetQuantity(productId, qty, cost);

            // 3) update listview
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
        // Nếu không có sản phẩm nào trong popup → xoá IMPORT rỗng
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
