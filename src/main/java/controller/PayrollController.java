package controller;

import model.PayrollDAO;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import model.Payroll;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import model.PayrollDAO;

public class PayrollController implements Initializable {

    @FXML private TableView<Payroll> tblPayroll;
    @FXML private TableColumn<Payroll, String> colEmpId;
    @FXML private TableColumn<Payroll, String> colName;
    @FXML private TableColumn<Payroll, Integer> colMonth;
    @FXML private TableColumn<Payroll, Double> colBaseSalary;
    @FXML private TableColumn<Payroll, Double> colBonus;
    @FXML private TableColumn<Payroll, Double> colPenalty;
    @FXML private TableColumn<Payroll, Double> colTotalSalary;

    @FXML private ComboBox<String> cbMonth;
    @FXML private Button btnRefresh;

    private PayrollDAO payrollDAO = new PayrollDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        initMonth();
        loadData(6); // mặc định tháng 6
    }

    private void initTable() {
        colEmpId.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmployeeID()));
        colName.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getFullName()));
        colMonth.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMonth()));
        colBaseSalary.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getBaseSalary()));
        colBonus.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getBonus()));
        colPenalty.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getPenalty()));
        colTotalSalary.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getTotalSalary()));
    }

    private void initMonth() {
        cbMonth.getItems().addAll("1","2","3","4","5","6","7","8","9","10","11","12");
        cbMonth.setValue("6");

        cbMonth.setOnAction(e ->
                loadData(Integer.parseInt(cbMonth.getValue()))
        );
    }

    private void loadData(int month) {
        ObservableList<Payroll> list =
                FXCollections.observableArrayList(payrollDAO.getPayrollByMonth(month));
        tblPayroll.setItems(list);
    }
}
