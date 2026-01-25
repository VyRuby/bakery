package controller;

import DAO_Employee.CheckInDAO;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.CheckIn;

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

    @FXML private TextField txtEmployeeId;
    @FXML private DatePicker dpDate;

    private final CheckInDAO dao = new CheckInDAO();

    // ================= INITIALIZE =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        colId.setCellValueFactory(new PropertyValueFactory<>("checkInID"));
        colEmpId.setCellValueFactory(new PropertyValueFactory<>("employeeID"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("workDate"));
        colIn.setCellValueFactory(new PropertyValueFactory<>("checkInTime"));
        colOut.setCellValueFactory(new PropertyValueFactory<>("checkOutTime"));
        colLate.setCellValueFactory(new PropertyValueFactory<>("late"));

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
            String email = txtEmployeeId.getText().trim();
            if (email.isEmpty()) {
                alert("Please enter email!");
                return;
            }

            dao.checkInByEmail(email);
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
            String email = txtEmployeeId.getText().trim();
            if (email.isEmpty()) {
                alert("Please enter email!");
                return;
            }

            dao.checkOutByEmail(email);
            refreshAfterAction();
            alertInfo("Check-Out successful!");

        } catch (RuntimeException ex) {
            alert(ex.getMessage());
        } catch (Exception e) {
            alert("Unexpected error: " + e.getMessage());
        }
    }

    private void refreshAfterAction() {
        txtEmployeeId.clear();
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
}
