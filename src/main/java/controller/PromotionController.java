package controller;

import DAO_Product.PromotionDAO;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import model.Promotion;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class PromotionController extends BacktoHomeController implements Initializable {

    @FXML private Label lblUser;
    @FXML private Label lblStatus;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbFilter;

    @FXML private TableView<Promotion> tblPromotions;
    @FXML private TableColumn<Promotion, String> colId;
    @FXML private TableColumn<Promotion, String> colName;
    @FXML private TableColumn<Promotion, String> colDiscount;
    @FXML private TableColumn<Promotion, LocalDate> colStartDate;
    @FXML private TableColumn<Promotion, LocalDate> colEndDate;
    @FXML private TableColumn<Promotion, String> colType;
    @FXML private TableColumn<Promotion, Number> colValue;
    @FXML private TableColumn<Promotion, String> colStatus;


    @FXML private Label lblHint;

    private final PromotionDAO promotionDao = new PromotionDAO();

    private final ObservableList<Promotion> promoList = FXCollections.observableArrayList();
    private FilteredList<Promotion> filteredList;
    private SortedList<Promotion> sortedList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("PromotionController initialize()");


        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("promoId"));
        colName.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("promoName"));

        // Discount hiển thị dạng: "10%" hoặc "50000 (fixed)"
        colDiscount.setCellValueFactory(cd -> {
            Promotion p = cd.getValue();
            if (p == null) return new SimpleStringProperty("");
            if ("percent".equalsIgnoreCase(p.getPromoType())) {
                return new SimpleStringProperty(String.format("%.0f%%", p.getValue()));
            }
            return new SimpleStringProperty(String.format("%.0f (fixed)", p.getValue()));
        });

        colStartDate.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getStartDate()));
        colEndDate.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getEndDate()));
        colType.setCellValueFactory(new PropertyValueFactory<>("promoType"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // load data
        viewTable();

        filteredList = new FilteredList<>(promoList, x -> true);
        sortedList = new SortedList<>(filteredList);

        sortedList.comparatorProperty().bind(tblPromotions.comparatorProperty());
        tblPromotions.setItems(sortedList);

        // filter options
        cbFilter.getItems().setAll("All", "Active", "Inactive", "Upcoming", "Expired");
        cbFilter.setValue("All");
    }

    private void viewTable() {
        promoList.setAll(promotionDao.findAll());
    }

    @FXML
    private void onSearchChanged(KeyEvent event) {
        applyFilter();
    }

    @FXML
    private void onFilterChanged(ActionEvent event) {
        applyFilter();
    }

    private void applyFilter() {
        String keyword = (txtSearch.getText() == null) ? "" : txtSearch.getText().toLowerCase().trim();
        String filter = cbFilter.getValue();
        LocalDate today = LocalDate.now();

        filteredList.setPredicate(p -> {
            // SEARCH: theo PromoID hoặc PromoName
            boolean matchSearch = keyword.isEmpty()
                    || p.getPromoId().toLowerCase().contains(keyword)
                    || p.getPromoName().toLowerCase().contains(keyword);

            // FILTER:
            boolean matchFilter = true;
            if ("Active".equals(filter)) {
                matchFilter = "Active".equalsIgnoreCase(p.getStatus())
                        && !today.isBefore(p.getStartDate())
                        && !today.isAfter(p.getEndDate());
            } else if ("Inactive".equals(filter)) {
                matchFilter = "Inactive".equalsIgnoreCase(p.getStatus());
            } 

            return matchSearch && matchFilter;
        });
    }

    @FXML
    private void onRefresh(ActionEvent event) {
        try {
            viewTable();
            tblPromotions.getSelectionModel().clearSelection();
            applyFilter();
            showInfo("Refreshed", "Promotion list has been refreshed.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Refresh Failed", "Unable to refresh promotion list.");
        }
    }

    // ===== CRUD =====
    @FXML
private void onAdd(ActionEvent event) {
    Promotion p = showPromotionPopup(null);
    if (p == null) return;

    try {
        if (promotionDao.existsId(p.getPromoId())) {
            showWarning("Warning", "Promo ID already exists.");
            return;
        }

        promotionDao.insert(p);

        // reload list (an toàn, đúng dữ liệu + mapping)
        viewTable();
        applyFilter();

        showInfo("Success", "Promotion added successfully.");
    } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
        // thường do UNIQUE(ProductID): product đã thuộc promo khác
        showError("Error", "Constraint Violation",
                "Some selected products are already applied to another promotion.");
    } catch (Exception e) {
        e.printStackTrace();
        showError("Error", "Add Failed", "Unable to add promotion.");
    }
}


    @FXML
private void onEdit(ActionEvent event) {
    Promotion selected = tblPromotions.getSelectionModel().getSelectedItem();
    if (selected == null) {
        showWarning("Warning", "Please select a promotion to edit.");
        return;
    }

    Promotion updated = showPromotionPopup(selected);
    if (updated == null) return;

    try {
        promotionDao.update(updated);

        viewTable();
        applyFilter();

        showInfo("Success", "Promotion updated successfully.");
    } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
        showError("Error", "Constraint Violation",
                "Some selected products are already applied to another promotion.");
    } catch (Exception e) {
        e.printStackTrace();
        showError("Error", "Update Failed", "Unable to update promotion.");
    }
}


    @FXML
    private void onDelete(ActionEvent event) {
        Promotion selected = tblPromotions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Warning", "Please select a promotion to delete.");
            return;
        }

        boolean ok = confirm(
                "Confirm Deletion",
                "Are you sure you want to delete this promotion?",
                "Promotion ID: " + selected.getPromoId() + "\n"
                + "Promotion Name: " + selected.getPromoName()
        );

        if (!ok) return;

        try {
            promotionDao.delete(selected.getPromoId());
            promoList.remove(selected);
            showInfo("Success", "Promotion deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Delete Failed", "Unable to delete the promotion.");
        }
    }

    // ===== ALERT (reuse css AlertNoti.css) =====
    private void applyAlertCss(Alert a) {
        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/AlertNoti.css").toExternalForm()
        );
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        applyAlertCss(a);
        a.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        applyAlertCss(a);
        a.showAndWait();
    }

    private void showError(String title, String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(message);
        applyAlertCss(a);
        a.showAndWait();
    }

    private boolean confirm(String title, String header, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(message);
        applyAlertCss(a);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }
    
    private Promotion showPromotionPopup(Promotion existing) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PromoAddEdit.fxml"));
        Parent root = loader.load();

        PromoAddEditController ctrl = loader.getController();
        ctrl.setMode(existing); // null = add, != null = edit

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existing == null ? "Add Promotion" : "Edit Promotion");
        stage.setScene(new Scene(root));
        stage.showAndWait();

        return ctrl.getResult();
    } catch (Exception e) {
        e.printStackTrace();
        showError("Error", "Popup Failed", "Unable to open promotion popup.");
        return null;
    }
}

}
