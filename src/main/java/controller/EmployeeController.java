package controller;

import model.Employee;
import DAO_Employee.EmployeeDAO;
import app.Session;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EmployeeController extends BacktoHomeController implements Initializable {
    

    // ===== TABLE =====
    @FXML private TableView<Employee> tblEmployee;
    @FXML private TableColumn<Employee, String> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, Date> colDob;
    @FXML private TableColumn<Employee, String> colGender;
    @FXML private TableColumn<Employee, String> colPhone;
    @FXML private TableColumn<Employee, String> colEmail;
    @FXML private TableColumn<Employee, String> colPosition;
    @FXML private TableColumn<Employee, String> colStatus;
    @FXML private TableColumn<Employee, Integer> colSalary;
    @FXML private TextField txtSearch;

    @FXML private ComboBox<String> cbFilterStatus;

    private final EmployeeDAO dao = new EmployeeDAO();
    private ObservableList<Employee> empList;

    // ================= INITIALIZE =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
          // ===== CHECK PERMISSION =====
    if (Session.role == null || !Session.role.equalsIgnoreCase("Manager")) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Access Denied");
        alert.setHeaderText(null);
        alert.setContentText("You do not have permission to access Employee Management.");
        alert.showAndWait();

        Platform.runLater(() -> {
            Stage stage = (Stage) tblEmployee.getScene().getWindow();
            stage.close();
        });

        return;
    }

        cbFilterStatus.getItems().addAll("ALL", "Active", "Inactive");
        cbFilterStatus.setValue("ALL");

        colId.setCellValueFactory(new PropertyValueFactory<>("employeeID"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDob.setCellValueFactory(new PropertyValueFactory<>("dob"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("baseDailySalary"));

        styleInactive(colId);
        styleInactive(colName);
        styleInactive(colDob);
        styleInactive(colGender);
        styleInactive(colPhone);
        styleInactive(colEmail);
        styleInactive(colPosition);
        styleInactive(colStatus);
        styleInactive(colSalary);

        loadData();

        txtSearch.textProperty().addListener((o, a, b) -> autoSearch());
        cbFilterStatus.valueProperty().addListener((o, a, b) -> autoSearch());
    }

    private <T> void styleInactive(TableColumn<Employee, T> col) {
        col.setCellFactory(column -> new TableCell<>() {
            private final Text text = new Text();

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                text.setText(item.toString());
                Employee emp = getTableView().getItems().get(getIndex());

                if ("Inactive".equalsIgnoreCase(emp.getStatus())) {
                    text.setFill(javafx.scene.paint.Color.web("#B91C1C"));
                    text.setStrikethrough(true);
                    text.setOpacity(0.8);
                } else {
                    text.setFill(javafx.scene.paint.Color.BLACK);
                    text.setStrikethrough(false);
                    text.setOpacity(1);
                }

                setGraphic(text);
            }
        });
    }

    private void loadData() {
        try {
            empList = FXCollections.observableArrayList(dao.getEmployees("ALL"));
            tblEmployee.setItems(empList);
        } catch (Exception e) {
            showAlert("Cannot load employee data!");
        }
    }

    private void autoSearch() {
        try {
            String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
            String status = cbFilterStatus.getValue();

            List<Employee> list = keyword.isEmpty()
                    ? dao.getEmployees("ALL")
                    : dao.search(keyword);

            if (!"ALL".equalsIgnoreCase(status)) {
                list.removeIf(e -> !status.equalsIgnoreCase(e.getStatus()));
            }

            tblEmployee.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) {
            showAlert("Search failed!");
        }
    }

    @FXML private void handleAdd() {
        openEmployeeForm(null, false);
    }

    @FXML private void handleUpdate() {
        Employee e = tblEmployee.getSelectionModel().getSelectedItem();
        if (e == null) {
            showAlert("Please select an employee!");
            return;
        }
        openEmployeeForm(e, true);
    }

    @FXML private void handleDeactivate() {
        Employee e = tblEmployee.getSelectionModel().getSelectedItem();
        if (e == null) {
            showAlert("Select employee first!");
            return;
        }
        if ("Inactive".equalsIgnoreCase(e.getStatus())) {
            showAlert("Employee already inactive!");
            return;
        }

        try {
            dao.deactivate(e.getEmployeeID());
            loadData();
            showSuccess("Employee deactivated!");
        } catch (Exception ex) {
            showError("Deactivate failed!");
        }
    }

    // ================= POPUP FORM =================
    private void openEmployeeForm(Employee emp, boolean isUpdate) {

        Stage stage = new Stage();
        stage.setTitle(isUpdate ? "Update Employee" : "Add Employee");
        stage.initModality(Modality.APPLICATION_MODAL);

        final int INPUT_WIDTH = 260;

        String inputStyle =
                "-fx-background-color:#FFFDF8;" +
                "-fx-border-color:#C19A6B;" +
                "-fx-border-radius:10;" +
                "-fx-background-radius:10;" +
                "-fx-padding:6 10;";

        TextField txtId = new TextField();
        TextField txtName = new TextField();
        TextField txtPhone = new TextField();
        TextField txtEmail = new TextField();
        TextField txtAddress = new TextField();
        TextField txtSalary = new TextField();

        DatePicker dpDob = new DatePicker();
        DatePicker dpHire = new DatePicker();

        ComboBox<String> cbGender = new ComboBox<>(FXCollections.observableArrayList("male", "female"));
        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList("Active", "Inactive"));
        ComboBox<String> cbPosition = new ComboBox<>(FXCollections.observableArrayList("Manager", "Staff"));

        Control[] inputs = {
                txtId, txtName, txtPhone, txtEmail, txtAddress, txtSalary,
                dpDob, dpHire, cbGender, cbStatus, cbPosition
        };

        for (Control c : inputs) {
            c.setStyle(inputStyle);
            c.setPrefWidth(INPUT_WIDTH);
        }

        txtPhone.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d*") ? c : null));
        txtSalary.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d*") ? c : null));

        Label errId = new Label();
        Label errName = new Label();
        Label errEmail = new Label();
        Label errSalary = new Label();
        Label errPhone = new Label();
        Label errDob = new Label();

        Label[] errors = {errId, errName, errEmail, errSalary, errPhone, errDob};
        for (Label e : errors) {
            e.setVisible(false);
            e.setManaged(false);
            e.setPrefWidth(140);
        }

        if (isUpdate && emp != null) {
            txtId.setText(emp.getEmployeeID());
            txtId.setDisable(true);
            txtName.setText(emp.getFullName());
            dpDob.setValue(emp.getDob().toLocalDate());
            cbGender.setValue(emp.getGender());
            txtPhone.setText(emp.getPhone());
            txtEmail.setText(emp.getEmail());
            txtAddress.setText(emp.getAddress());
            dpHire.setValue(emp.getHireDate().toLocalDate());
            cbPosition.setValue(emp.getPosition());
            cbStatus.setValue(emp.getStatus());
            txtSalary.setText(String.valueOf(emp.getBaseDailySalary()));
        }

        Button btnSave = new Button(isUpdate ? "Update" : "Add");
        btnSave.setDisable(true);

        Runnable validate = () -> {
            boolean ok = true;

            if (!isUpdate) {
                if (txtId.getText().isBlank()) {
                    markError(txtId, errId, "Required");
                    ok = false;
                } else if (dao.exists(txtId.getText().trim())) {
                    markError(txtId, errId, "ID exists");
                    ok = false;
                } else clearError(txtId, errId);
            }

            if (txtName.getText().isBlank()) {
                markError(txtName, errName, "Required");
                ok = false;
            } else clearError(txtName, errName);

            if (dpDob.getValue() == null ||
                    Period.between(dpDob.getValue(), LocalDate.now()).getYears() < 18) {
                markError(dpDob, errDob, "Age >= 18");
                ok = false;
            } else clearError(dpDob, errDob);

            if (!txtPhone.getText().matches("\\d{9,11}")) {
                markError(txtPhone, errPhone, "9–11 digits");
                ok = false;
            } else clearError(txtPhone, errPhone);

            if (!txtEmail.getText().matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
                markError(txtEmail, errEmail, "Invalid");
                ok = false;
            } else clearError(txtEmail, errEmail);

            try {
                int s = Integer.parseInt(txtSalary.getText());
                if (s <= 0) throw new Exception();
                clearError(txtSalary, errSalary);
            } catch (Exception e) {
                markError(txtSalary, errSalary, "Invalid");
                ok = false;
            }

            btnSave.setDisable(!ok);
        };

        txtId.textProperty().addListener((o,a,b)->validate.run());
        txtName.textProperty().addListener((o,a,b)->validate.run());
        txtPhone.textProperty().addListener((o,a,b)->validate.run());
        txtEmail.textProperty().addListener((o,a,b)->validate.run());
        txtSalary.textProperty().addListener((o,a,b)->validate.run());
        dpDob.valueProperty().addListener((o,a,b)->validate.run());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(20));

        grid.getColumnConstraints().addAll(
                new ColumnConstraints(120),
                new ColumnConstraints(260),
                new ColumnConstraints(140)
        );

        int r = 0;
        grid.addRow(r++, new Label("Employee ID"), txtId, errId);
        grid.addRow(r++, new Label("Full Name"), txtName, errName);
        grid.addRow(r++, new Label("DOB"), dpDob, errDob);
        grid.addRow(r++, new Label("Gender"), cbGender);
        grid.addRow(r++, new Label("Phone"), txtPhone, errPhone);
        grid.addRow(r++, new Label("Email"), txtEmail, errEmail);
        grid.addRow(r++, new Label("Address"), txtAddress);
        grid.addRow(r++, new Label("Hire Date"), dpHire);
        grid.addRow(r++, new Label("Position"), cbPosition);
        grid.addRow(r++, new Label("Status"), cbStatus);
        grid.addRow(r++, new Label("Daily Salary"), txtSalary, errSalary);

        btnSave.setOnAction(e -> {
    try {
        Employee ne = new Employee(
            txtId.getText(), txtName.getText(),
            Date.valueOf(dpDob.getValue()),
            cbGender.getValue(),
            txtPhone.getText(),
            txtEmail.getText(),
            txtAddress.getText(),
            Date.valueOf(dpHire.getValue()),
            cbPosition.getValue(),
            cbStatus.getValue(),
            Integer.parseInt(txtSalary.getText())
        );

        if (isUpdate) {
            dao.update(ne);
        } else {
            dao.insert(ne);

            // 🔥🔥🔥 CHỖ QUAN TRỌNG 🔥🔥🔥
            CheckInController ctrl = ControllerRegistry.getCheckInController();
            if (ctrl != null) {
                ctrl.loadActiveEmployees();
            }
        }

        loadData();
        stage.close();

    } catch (Exception ex) {
        showError("Save failed!");
    }
});


        Button btnClose = new Button("Close");
        btnClose.setOnAction(e -> stage.close());

        HBox hbox = new HBox(10, btnSave, btnClose);
        hbox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(18, grid, hbox);
        root.setPadding(new Insets(18));

        stage.setScene(new Scene(root, 580, 560));
        stage.setResizable(false);
        stage.showAndWait();
    }

    private void markError(Control f, Label l, String m) {
        f.setStyle("-fx-border-color:red;");
        l.setText(m);
        l.setVisible(true);
        l.setManaged(true);
    }

    private void clearError(Control f, Label l) {
        f.setStyle("-fx-border-color:#C19A6B;");
        l.setVisible(false);
        l.setManaged(false);
    }

    private void showAlert(String m) {
        new Alert(Alert.AlertType.WARNING, m).showAndWait();
    }

    private void showSuccess(String m) {
        new Alert(Alert.AlertType.INFORMATION, m).showAndWait();
    }

    private void showError(String m) {
        new Alert(Alert.AlertType.ERROR, m).showAndWait();
    }
}
