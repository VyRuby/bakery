package controller;

import DAO_Employee.CheckInDAO;
import DAO_Employee.EmployeeDAO;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import model.CheckIn;
import model.Employee;

import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class CheckInController extends BacktoHomeController implements Initializable {

    @FXML private TableView<CheckIn> tblCheckIn;

    @FXML private TableColumn<CheckIn, Integer> colId;
    @FXML private TableColumn<CheckIn, String> colEmpId;
    @FXML private TableColumn<CheckIn, String> colFullName;
    @FXML private TableColumn<CheckIn, Date> colDate;
    @FXML private TableColumn<CheckIn, Time> colIn;
    @FXML private TableColumn<CheckIn, Time> colOut;
    @FXML private TableColumn<CheckIn, Boolean> colLate;

    // 🔁 THAY TEXTFIELD → COMBOBOX
    @FXML private ComboBox<Employee> cbEmployee;

    @FXML private DatePicker dpDate;

    private final EmployeeDAO empDao = new EmployeeDAO();
    private final CheckInDAO dao = new CheckInDAO();

    // ================= INITIALIZE =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

          // ✅ REGISTER CONTROLLER
    ControllerRegistry.setCheckInController(this);
        colId.setCellValueFactory(new PropertyValueFactory<>("checkInID"));
        colEmpId.setCellValueFactory(new PropertyValueFactory<>("employeeID"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("workDate"));
        colIn.setCellValueFactory(new PropertyValueFactory<>("checkInTime"));
        colOut.setCellValueFactory(new PropertyValueFactory<>("checkOutTime"));
        colLate.setCellValueFactory(new PropertyValueFactory<>("late"));
        
           // 🔁 LOAD ACTIVE EMPLOYEES
    loadActiveEmployees();

       

        // ✅ HIỂN THỊ: email - fullname
        cbEmployee.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employee e) {
                return e == null ? "" : e.getEmail() + " - " + e.getFullName();
            }

            @Override
            public Employee fromString(String s) {
                return null; // không cho nhập tay
            }
        });

        loadCurrentMonth();
    }

    // ================= LOAD CURRENT MONTH =================
    private void loadCurrentMonth() {

        LocalDate now = LocalDate.now();

        tblCheckIn.setItems(
                FXCollections.observableArrayList(
                        dao.getCheckInByMonth(
                                now.getMonthValue(),
                                now.getYear()
                        )
                )
        );
    }

    // ================= FILTER BY DATE =================
    @FXML
    private void filterByDate() {

        if (dpDate.getValue() == null) {
            loadCurrentMonth();
            return;
        }

        tblCheckIn.setItems(
                FXCollections.observableArrayList(
                        dao.getCheckInByDate(
                                Date.valueOf(dpDate.getValue())
                        )
                )
        );
    }

    // ================= CHECK-IN =================
    @FXML
    private void handleCheckIn() {

        try {
            Employee emp = cbEmployee.getValue();
            if (emp == null) {
                alert("Please select employee!");
                return;
            }

            dao.checkInByEmail(emp.getEmail());
            refreshAfterAction();
            alertInfo("Check-In successful!");

        } catch (RuntimeException ex) {
            alert(ex.getMessage());
        } catch (Exception e) {
            alert("Unexpected error: " + e.getMessage());
        }
    }

    // ================= CHECK-OUT =================
    @FXML
    private void handleCheckOut() {

        try {
            Employee emp = cbEmployee.getValue();
            if (emp == null) {
                alert("Please select employee!");
                return;
            }

            dao.checkOutByEmail(emp.getEmail());
            refreshAfterAction();
            alertInfo("Check-Out successful!");

        } catch (RuntimeException ex) {
            alert(ex.getMessage());
        } catch (Exception e) {
            alert("Unexpected error: " + e.getMessage());
        }
    }

    private void refreshAfterAction() {
        cbEmployee.getSelectionModel().clearSelection();
        if (dpDate.getValue() != null) {
            filterByDate();
        } else {
            loadCurrentMonth();
        }
    }

    // ================= ALERT =================
    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
     // ================= UPDATE EMAIL OF NEW PARTNER =================
    public void loadActiveEmployees() {
    cbEmployee.setItems(
        FXCollections.observableArrayList(
            empDao.getEmployees("Active")
        )
    );
}

}
