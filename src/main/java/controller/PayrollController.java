package controller;

import DAO_Employee.PayrollDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import model.Payroll;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class PayrollController extends BacktoHomeController implements Initializable {

    @FXML private TableView<Payroll> tblPayroll;

    @FXML private TableColumn<Payroll, String> colEmpId;
    @FXML private TableColumn<Payroll, String> colName;
    @FXML private TableColumn<Payroll, Integer> colMonth;
    @FXML private TableColumn<Payroll, Integer> colYear;
    @FXML private TableColumn<Payroll, Integer> colWorkDays;
    @FXML private TableColumn<Payroll, BigDecimal> colBonus;
    @FXML private TableColumn<Payroll, BigDecimal> colPenalty;
    @FXML private TableColumn<Payroll, BigDecimal> colTotalSalary;

    @FXML private ComboBox<String> cbMonth;
    @FXML private ComboBox<String> cbYear;
    @FXML private TextField txtSearchEmployee;
    @FXML private ComboBox<String> cbStatus;

    private FilteredList<Payroll> filteredData;
    private final PayrollDAO dao = new PayrollDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        initFilter();
        initExtraFilter();
        reload();
    }

    private void initFilter() {
        cbMonth.getItems().addAll(
                "1","2","3","4","5","6","7","8","9","10","11","12"
        );
        cbYear.getItems().addAll("2025","2026");

        cbMonth.setValue("6");
        cbYear.setValue("2025");

        cbMonth.setOnAction(e -> reload());
        cbYear.setOnAction(e -> reload());
    }

    private void initExtraFilter() {
        cbStatus.getItems().addAll("All", "Normal", "Bonus", "Penalty");
        cbStatus.setValue("All");

        txtSearchEmployee.textProperty().addListener((obs,o,n) -> applyFilter());
        cbStatus.setOnAction(e -> applyFilter());
    }

    private void reload() {
        int month = Integer.parseInt(cbMonth.getValue());
        int year = Integer.parseInt(cbYear.getValue());

        dao.calculatePayroll(month, year);

        filteredData = new FilteredList<>(
                FXCollections.observableArrayList(
                        dao.getPayroll(month, year)
                )
        );

        tblPayroll.setItems(filteredData);
        applyFilter();
    }

    private void applyFilter() {
        String keyword = txtSearchEmployee.getText() == null ? "" :
                txtSearchEmployee.getText().toLowerCase();

        String status = cbStatus.getValue();

        filteredData.setPredicate(p -> {

            boolean matchEmp =
                    p.getEmployeeID().toLowerCase().contains(keyword) ||
                    p.getFullName().toLowerCase().contains(keyword);

            boolean matchStatus = switch (status) {
                case "Bonus" -> p.getBonus().compareTo(BigDecimal.ZERO) > 0;
                case "Penalty" -> p.getPenalty().compareTo(BigDecimal.ZERO) > 0;
                case "Normal" -> p.getBonus().compareTo(BigDecimal.ZERO) == 0
                              && p.getPenalty().compareTo(BigDecimal.ZERO) == 0;
                default -> true;
            };

            return matchEmp && matchStatus;
        });
    }

    private void initTable() {

        colEmpId.setCellValueFactory(d ->
                new ReadOnlyStringWrapper(d.getValue().getEmployeeID()));

        colName.setCellValueFactory(d ->
                new ReadOnlyStringWrapper(d.getValue().getFullName()));

        colMonth.setCellValueFactory(d ->
                new ReadOnlyObjectWrapper<>(d.getValue().getMonth()));

        colYear.setCellValueFactory(d ->
                new ReadOnlyObjectWrapper<>(d.getValue().getYear()));

        colWorkDays.setCellValueFactory(d ->
                new ReadOnlyObjectWrapper<>(d.getValue().getWorkDays()));

        colBonus.setCellValueFactory(d ->
                new ReadOnlyObjectWrapper<>(d.getValue().getBonus()));

        colPenalty.setCellValueFactory(d ->
                new ReadOnlyObjectWrapper<>(d.getValue().getPenalty()));

        colTotalSalary.setCellValueFactory(d ->
                new ReadOnlyObjectWrapper<>(d.getValue().getTotalSalary()));

        formatMoney(colBonus);
        formatMoney(colPenalty);
        formatMoney(colTotalSalary);
    }

    private void formatMoney(TableColumn<Payroll, BigDecimal> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : String.format("%,d", val.intValue()));
            }
        });
    }
}
