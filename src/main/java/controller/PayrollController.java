package controller;

import DAO_Employee.PayrollDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import model.Payroll;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class PayrollController implements Initializable {

    @FXML
    private TableView<Payroll> tblPayroll;

    @FXML
    private TableColumn<Payroll, String> colEmpId;
    @FXML
    private TableColumn<Payroll, String> colName;
    @FXML
    private TableColumn<Payroll, Integer> colMonth;

    @FXML
    private TableColumn<Payroll, BigDecimal> colBaseSalary;
    @FXML
    private TableColumn<Payroll, BigDecimal> colBonus;
    @FXML
    private TableColumn<Payroll, BigDecimal> colPenalty;
    @FXML
    private TableColumn<Payroll, BigDecimal> colTotalSalary;

    @FXML
    private ComboBox<String> cbMonth;
   
    @FXML
    private Button btnCalculate;

    private final PayrollDAO payrollDAO = new PayrollDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        initMonth();
        loadData(6); // mặc định tháng 6
    }

    // =========================
    // INIT TABLE
    // =========================
    private void initTable() {

        colEmpId.setCellValueFactory(d
                -> new ReadOnlyStringWrapper(d.getValue().getEmployeeID()));

        colName.setCellValueFactory(d
                -> new ReadOnlyStringWrapper(d.getValue().getFullName()));

        colMonth.setCellValueFactory(d
                -> new ReadOnlyObjectWrapper<>(d.getValue().getMonth()));

        colBaseSalary.setCellValueFactory(d
                -> new ReadOnlyObjectWrapper<>(d.getValue().getBaseSalary()));

        colBonus.setCellValueFactory(d
                -> new ReadOnlyObjectWrapper<>(d.getValue().getBonus()));

        colPenalty.setCellValueFactory(d
                -> new ReadOnlyObjectWrapper<>(d.getValue().getPenalty()));

        colTotalSalary.setCellValueFactory(d
                -> new ReadOnlyObjectWrapper<>(d.getValue().getTotalSalary()));

        // format tiền VN
        formatMoney(colBaseSalary);
        formatMoney(colBonus);
        formatMoney(colPenalty);
        formatMoney(colTotalSalary);
    }

    // =========================
    // INIT MONTH COMBOBOX
    // =========================
    private void initMonth() {
        cbMonth.getItems().addAll(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"
        );
        cbMonth.setValue("6");

        cbMonth.setOnAction(e
                -> loadData(Integer.parseInt(cbMonth.getValue()))
        );
    }

    // =========================
    // LOAD DATA
    // =========================
    private void loadData(int month) {
        ObservableList<Payroll> list
                = FXCollections.observableArrayList(
                        payrollDAO.getPayrollByMonth(month)
                );
        tblPayroll.setItems(list);
    }

    // =========================
    // FORMAT MONEY
    // =========================
    private void formatMoney(TableColumn<Payroll, BigDecimal> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f", value));
                }
            }
        });
    }

    // =========================
    // Tạo hàm xử lý nút Calculate
    // =========================
    @FXML private void handleCalculate() {

        int month = Integer.parseInt(cbMonth.getValue());
        int year = 2025; // hoặc lấy theo năm hiện tại

        // 1. Tính lại lương trong DB
        payrollDAO.calculatePayroll(month, year);

        // 2. Load lại bảng
        loadData(month);

        // 3. Thông báo
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Salary calculated successfully!");
        alert.showAndWait();
    }

   

}
