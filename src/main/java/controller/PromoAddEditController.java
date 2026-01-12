package controller;

import DAO_Product.PromotionDAO;
import DAO_Customer_Order.productDao;
import app.ConnectDB;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import model.Product;
import model.Promotion;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PromoAddEditController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblMode;

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtValue;
    @FXML private DatePicker dpStart;
    @FXML private DatePicker dpEnd;
    @FXML private ComboBox<String> cbStatus;

    @FXML private TextArea txtDescription;
    @FXML private ListView<Product> lvProducts;
    @FXML private Label lblMsg;

    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Button btnRemoveFromProducts;

    private final productDao productDao = new productDao();
    private final PromotionDAO promotionDao = new PromotionDAO();

    private Promotion editing; // null = add
    private Promotion result;

    // checkbox state
    private final Map<String, BooleanProperty> checkedMap = new HashMap<>();
    private Set<String> currentPromoProductIds = new HashSet<>();
    private Set<String> productIdsUsedByOtherPromos = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbType.getItems().setAll("percent", "fixed");
        cbStatus.getItems().setAll("Active", "Inactive");
        cbType.setValue("percent");
        cbStatus.setValue("Inactive"); //  Add mặc định Inactive
    }

    public void setMode(Promotion promo) {
        this.editing = promo;
        this.result = null;
        lblMsg.setText("");

        if (promo == null) {
            lblTitle.setText("Add Promotion");
            lblMode.setText("ADD MODE");

            txtId.clear();
            txtId.setDisable(false);

            txtName.clear();
            txtDescription.clear();

            cbType.setValue("percent");
            txtValue.clear();

            dpStart.setValue(LocalDate.now());
            dpEnd.setValue(LocalDate.now().plusDays(7));

            cbStatus.setValue("Inactive");

        } else {
            lblTitle.setText("Edit Promotion");
            lblMode.setText("EDIT MODE");

            txtId.setText(promo.getPromoId());
            txtId.setDisable(true);

            txtName.setText(promo.getPromoName());
            txtDescription.setText(promo.getDescription());

            cbType.setValue(promo.getPromoType());
            txtValue.setText(String.valueOf(promo.getValue()));

            dpStart.setValue(promo.getStartDate());
            dpEnd.setValue(promo.getEndDate());

            cbStatus.setValue(promo.getStatus());
        }

        // chỉ Edit mới có nút remove
        btnRemoveFromProducts.setVisible(editing != null);
        btnRemoveFromProducts.setManaged(editing != null);

        loadProductsWithCheckbox();
    }

    public Promotion getResult() {
        return result;
    }

    // all product qty>0, tick sản phẩm thuộc promo hiện tại, disable sản phẩm thuộc promo khác
    private void loadProductsWithCheckbox() {
        try {
            List<Product> all = productDao.findAll().stream()
                    .filter(p -> p.getQuantity() > 0)
                    .collect(Collectors.toList());

            currentPromoProductIds = (editing != null && editing.getProductIds() != null)
                    ? new HashSet<>(editing.getProductIds())
                    : new HashSet<>();

            productIdsUsedByOtherPromos = getProductIdsAssignedToOtherPromos(editing == null ? null : editing.getPromoId());

            lvProducts.getItems().setAll(all);

            checkedMap.clear();
            for (Product p : all) {
                boolean checked = currentPromoProductIds.contains(p.getProductId());
                checkedMap.put(p.getProductId(), new SimpleBooleanProperty(checked));
            }

            lvProducts.setCellFactory(list -> new CheckBoxListCell<>(item -> checkedMap.get(item.getProductId())) {
                @Override
                public void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setDisable(false);
                        return;
                    }

                    setText(item.getProductId() + " - " + item.getProductName() + " (Qty: " + item.getQuantity() + ")");

                    boolean usedByOther = productIdsUsedByOtherPromos.contains(item.getProductId());
                    boolean isCurrent = currentPromoProductIds.contains(item.getProductId());

                    // disable nếu thuộc promo khác và không phải promo hiện tại
                    setDisable(usedByOther && !isCurrent);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            lvProducts.getItems().clear();
            lblMsg.setText("Unable to load products.");
        }
    }

    private Set<String> getProductIdsAssignedToOtherPromos(String currentPromoId) throws Exception {
        Set<String> ids = new HashSet<>();

        String sql = (currentPromoId == null)
                ? "SELECT ProductID FROM PROMOTION_PRODUCT"
                : "SELECT ProductID FROM PROMOTION_PRODUCT WHERE PromoID <> ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (currentPromoId != null) ps.setString(1, currentPromoId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("ProductID"));
            }
        }
        return ids;
    }

    @FXML
    private void onSave(ActionEvent event) {
        lblMsg.setText("");

        try {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String desc = txtDescription.getText().trim();
            String type = cbType.getValue();
            String status = cbStatus.getValue();

            if (id.isEmpty()) { lblMsg.setText("Please enter Promo ID."); return; }
            if (name.isEmpty()) { lblMsg.setText("Please enter Promo Name."); return; }
            if (type == null || type.isBlank()) { lblMsg.setText("Please select Promo Type."); return; }
            if (status == null || status.isBlank()) { lblMsg.setText("Please select Status."); return; }

            LocalDate start = dpStart.getValue();
            LocalDate end = dpEnd.getValue();
            if (start == null || end == null) { lblMsg.setText("Please select Start/End date."); return; }
            if (end.isBefore(start)) { lblMsg.setText("End Date must be after Start Date."); return; }

            double value;
            try {
                value = Double.parseDouble(txtValue.getText().trim());
            } catch (NumberFormatException e) {
                lblMsg.setText("Value must be a valid number.");
                return;
            }
            if (value < 0) { lblMsg.setText("Value must be >= 0."); return; }

            // ✅ không bắt buộc chọn product
            List<String> productIds = new ArrayList<>();
            for (Product pr : lvProducts.getItems()) {
                BooleanProperty bp = checkedMap.get(pr.getProductId());
                if (bp != null && bp.get()) {
                    productIds.add(pr.getProductId());
                }
            }

            Promotion p = new Promotion(id, name, desc, start, end, type, value, status);
            p.setProductIds(productIds);

            result = p;
            ((Stage) btnSave.getScene().getWindow()).close();

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Unexpected error.");
        }
    }

    @FXML
    private void onRemoveFromProducts(ActionEvent event) {
        if (editing == null) return;

        try {
            // remove = trước đây thuộc promo này nhưng user bỏ tick
            List<String> remove = new ArrayList<>();

            for (Product pr : lvProducts.getItems()) {
                boolean wasCurrent = currentPromoProductIds.contains(pr.getProductId());
                BooleanProperty bp = checkedMap.get(pr.getProductId());
                boolean checked = bp != null && bp.get();

                if (wasCurrent && !checked) {
                    remove.add(pr.getProductId());
                }
            }

            if (remove.isEmpty()) {
                lblMsg.setText("No product removed.");
                return;
            }

            promotionDao.removeProductsFromPromo(editing.getPromoId(), remove);

            // cập nhật lại state local
            currentPromoProductIds.removeAll(remove);
            for (String pid : remove) {
                BooleanProperty bp = checkedMap.get(pid);
                if (bp != null) bp.set(false);
            }

            lblMsg.setText("Removed from " + remove.size() + " product(s).");

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Remove failed.");
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        result = null;
        ((Stage) btnCancel.getScene().getWindow()).close();
    }
}
