package controller;

import DAO_Customer_Order.productDao;
import DAO_Product.PromotionDAO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.control.cell.CheckBoxListCell;

import model.Product;
import model.Promotion;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PromoAddEditController {

    @FXML private TextField txtPromoId;
    @FXML private TextField txtPromoName;
    @FXML private TextArea txtDescription;

    @FXML private ComboBox<String> cbPromoType;
    @FXML private TextField txtValue;
    @FXML private ComboBox<String> cbStatus;

    @FXML private ListView<Product> lvProducts;

    @FXML private Button btnRemoveFromProducts;
    @FXML private Label lblMsg;
    @FXML private Button btnSave;
    @FXML private Button btnClose;

    private final productDao productDAO = new productDao();
    private final PromotionDAO promotionDAO = new PromotionDAO();

    private Promotion result = null;
    private boolean editMode = false;

    // map để quản lý checkbox theo Product
    private final ObservableMap<Product, BooleanProperty> checkedMap = FXCollections.observableHashMap();
    
    // ProductID -> PromoID
    private Map<String, String> productPromoMap;

    @FXML
    public void initialize() {
        cbPromoType.setItems(FXCollections.observableArrayList("percent", "fixed"));
        cbStatus.setItems(FXCollections.observableArrayList("Active", "Inactive"));
        cbPromoType.getSelectionModel().select("percent");
        cbStatus.getSelectionModel().select("Active");

       

        // load products
        var products = FXCollections.observableArrayList(
        productDAO.findAll()
                .stream()
                .filter(p -> p.getQuantity() > 0)
                .collect(java.util.stream.Collectors.toList())
        );
        lvProducts.setItems(products);

        // init checked map
        checkedMap.clear();
        for (Product p : products) {
            checkedMap.put(p, new SimpleBooleanProperty(false));
        }
        
        productPromoMap = promotionDAO.getProductPromoMap();

        // ListView hiển thị checkbox
        lvProducts.setCellFactory(lv -> new CheckBoxListCell<>(
        item -> checkedMap.get(item)
) {
    @Override
    public void updateItem(Product item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setDisable(false);
            return;
        }

        setText(item.getProductId() + " - " + item.getProductName());

        String productId = item.getProductId();
        String promoOfProduct = productPromoMap.get(productId);

        // ADD mode: disable nếu product đã có promo
        if (!editMode) {
            if (promoOfProduct != null) {
                setDisable(true);
                checkedMap.get(item).set(false);
            } else {
                setDisable(false);
            }
            return;
        }

        // EDIT mode:
        // - cho phép nếu product thuộc promo hiện tại
        // - disable nếu thuộc promo khác
        if (promoOfProduct == null || promoOfProduct.equals(txtPromoId.getText())) {
            setDisable(false);
        } else {
            setDisable(true);
            checkedMap.get(item).set(false);
        }
    }
});
    }

    public void setMode(Promotion promo) {
        lblMsg.setText("");

        // clear ticks trước
        checkedMap.forEach((k, v) -> v.set(false));

        if (promo == null) {
            editMode = false;

            txtPromoId.setDisable(false);
            txtPromoId.clear();
            txtPromoName.clear();
            txtDescription.clear();

            txtValue.setText("0");
           
            cbPromoType.getSelectionModel().select("percent");
            cbStatus.getSelectionModel().select("Active");

            btnRemoveFromProducts.setDisable(false);
            return;
        }

        editMode = true;

        txtPromoId.setText(promo.getPromoId());
        txtPromoId.setDisable(true);

        txtPromoName.setText(promo.getPromoName());
        txtDescription.setText(promo.getDescription() == null ? "" : promo.getDescription());

      
        cbPromoType.getSelectionModel().select(promo.getPromoType());
        txtValue.setText(String.valueOf(promo.getValue()));
        cbStatus.getSelectionModel().select(promo.getStatus());

        // ✅ tick checkbox theo productIds của promo
        if (promo.getProductIds() != null) {
            for (Product pr : lvProducts.getItems()) {
                if (promo.getProductIds().contains(pr.getProductId())) {
                    BooleanProperty bp = checkedMap.get(pr);
                    if (bp != null) bp.set(true);
                }
            }
        }

        btnRemoveFromProducts.setDisable(false);
        
        lvProducts.refresh();

    }


    // lấy list productIds đang tick
    private List<String> getCheckedProductIds() {
        List<String> ids = new ArrayList<>();
        for (Product pr : lvProducts.getItems()) {
            BooleanProperty bp = checkedMap.get(pr);
            if (bp != null && bp.get()) {
                ids.add(pr.getProductId());
            }
        }
        return ids;
    }

    @FXML
    private void onRemoveFromProducts() {
        lblMsg.setText("");

        List<String> ids = getCheckedProductIds();
        if (ids.isEmpty()) {
            lblMsg.setText("Please tick product(s) to remove promotion.");
            return;
        }

        try {
            // xóa mapping theo ProductID
            promotionDAO.removePromotionFromProducts(ids);

            // bỏ tick sau khi remove
            for (Product pr : lvProducts.getItems()) {
                if (ids.contains(pr.getProductId())) {
                    BooleanProperty bp = checkedMap.get(pr);
                    if (bp != null) bp.set(false);
                }
            }

            lblMsg.setText("Removed promotion from " + ids.size() + " product(s).");
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Error remove: " + e.getMessage());
        }
    }

    @FXML
    private void onSave() {
        lblMsg.setText("");

        String promoId = txtPromoId.getText().trim();
        String promoName = txtPromoName.getText().trim();

        if (promoId.isEmpty() || promoName.isEmpty()) {
            lblMsg.setText("Promo ID & Name are required.");
            return;
        }

       

        String type = cbPromoType.getValue();
        if (type == null || (!type.equals("percent") && !type.equals("fixed"))) {
            lblMsg.setText("PromoType must be percent or fixed.");
            return;
        }

        double value;
        try {
            value = Double.parseDouble(txtValue.getText().trim());
        } catch (Exception e) {
            lblMsg.setText("Value must be numeric.");
            return;
        }
        if (value < 0) { lblMsg.setText("Value must be >= 0"); return; }
        if ("percent".equals(type) && value > 100) { lblMsg.setText("Percent must be <= 100"); return; }

        String status = cbStatus.getValue();
        if (status == null) status = "Active";

        // ✅ lấy product tick checkbox
        List<String> productIds = getCheckedProductIds();

        Promotion p = new Promotion();
        p.setPromoId(promoId);
        p.setPromoName(promoName);
        p.setDescription(txtDescription.getText());

      

        p.setPromoType(type);
        p.setValue(value);
        p.setStatus(status);
        p.setProductIds(productIds);

        result = p;
        ((Stage) btnSave.getScene().getWindow()).close();
    }

    @FXML
    private void onClose() {
        result = null;
        ((Stage) btnClose.getScene().getWindow()).close();
    }

    public Promotion getResult() {
        return result;
    }
}
