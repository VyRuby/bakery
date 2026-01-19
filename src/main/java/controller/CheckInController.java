package controller;

import DAO_Employee.CheckInDAO;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.CheckIn;

import java.net.URL;
import java.util.ResourceBundle;

public class CheckInController extends BacktoHomeController  implements Initializable {

    @FXML private TableView<CheckIn> tblCheckIn;
    @FXML private TableColumn<CheckIn, Integer> colId;
    @FXML private TableColumn<CheckIn, String> colEmpId;
    @FXML private TableColumn<CheckIn, String> colDate;
    @FXML private TableColumn<CheckIn, String> colIn;
    @FXML private TableColumn<CheckIn, String> colOut;
    @FXML private TableColumn<CheckIn, Boolean> colLate;

    // 👉 đổi ý nghĩa: nhập EMAIL
    @FXML private TextField txtEmployeeId;

    private final CheckInDAO dao = new CheckInDAO();

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

    // ===============================
    // CHECK-IN BY EMAIL
    // ===============================
    @FXML
    private void handleCheckIn() {

        String email = txtEmployeeId.getText().trim();

        if (email.isEmpty()) {
            alert("Please enter email!");
            return;
        }

        try {
            dao.checkInByEmail(email);
            loadData();
            txtEmployeeId.clear();
        } catch (RuntimeException ex) {
            alert(ex.getMessage());
        }
    }
    
    // ===============================
    // CHECK-OUT BY EMAIL
    // ===============================
    @FXML
private void handleCheckOut() {

    String email = txtEmployeeId.getText().trim();

    if (email.isEmpty()) {
        alert("Please enter email!");
        return;
    }

    try {
        dao.checkOutByEmail(email); // gọi procedure Check-Out
        loadData();                // reload bảng
        txtEmployeeId.clear();     // xóa input
        alertInfo("Check-Out successful!");
    } catch (RuntimeException ex) {
        alert(ex.getMessage());
    }
}

// Thêm Alert thông báo thành công
private void alertInfo(String msg) {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setContentText(msg);
    a.show();
}


    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setContentText(msg);
        a.show();
    }
}
