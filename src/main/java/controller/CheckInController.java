package controller;

import DAO_Employee.CheckInDAO;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.CheckIn;

import java.net.URL;
import java.util.ResourceBundle;

public class CheckInController implements Initializable {

    @FXML private TableView<CheckIn> tblCheckIn;
    @FXML private TableColumn<CheckIn, Integer> colId;
    @FXML private TableColumn<CheckIn, String> colEmpId;
    @FXML private TableColumn<CheckIn, String> colDate;
    @FXML private TableColumn<CheckIn, String> colIn;
    @FXML private TableColumn<CheckIn, String> colOut;
    @FXML private TableColumn<CheckIn, Boolean> colLate;

    @FXML private TextField txtEmployeeId;

    private CheckInDAO dao = new CheckInDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("checkInID"));
        colEmpId.setCellValueFactory(new PropertyValueFactory<>("employeeID"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("workDate"));
        colIn.setCellValueFactory(new PropertyValueFactory<>("checkInTime"));
        colOut.setCellValueFactory(new PropertyValueFactory<>("checkOutTime"));
        colLate.setCellValueFactory(new PropertyValueFactory<>("late"));

        loadData();
    }

    private void loadData() {
        ObservableList<CheckIn> list =
                FXCollections.observableArrayList(dao.getTodayCheckIn());
        tblCheckIn.setItems(list);
    }

 @FXML
private void handleCheckIn() {

    String empId = txtEmployeeId.getText().trim();

    if (empId.isEmpty()) {
        alert("Please enter Employee ID!");
        return;
    }

    if (dao.isCheckedInToday(empId)) {
        alert("Employee already checked in today!");
        return;
    }

    dao.checkIn(empId);
    loadData();
    txtEmployeeId.clear();
}


    @FXML
    private void handleCheckOut() {
        CheckIn c = tblCheckIn.getSelectionModel().getSelectedItem();
        if (c == null) {
            alert("Select record to checkout!");
            return;
        }
        dao.checkOut(c.getCheckInID());
        loadData();
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setContentText(msg);
        a.show();
    }
}
