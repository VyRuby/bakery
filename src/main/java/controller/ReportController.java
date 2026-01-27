package controller;

import DAO_Report.ReportDAO;
import DAO_Report.ReportDAO.TopProductStat;
import app.Session;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Platform;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import model.Report;

public class ReportController extends BacktoHomeController implements Initializable {

    @FXML private ProgressIndicator revenueProgress;
    @FXML private Label lbRevenue;

    @FXML private LineChart<String, Number> chartGrowth;
    @FXML private BarChart<String, Number> chartBottom;

    // pie chart controls
    @FXML private ComboBox<String> cbTopMode;     // Month | Quarter
    @FXML private ComboBox<String> cbTopPeriod;   // 2026-01 | 2026-Q1
    @FXML private PieChart pieTopProducts;

    private final ReportDAO dao = new ReportDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadRevenueThisMonth();
        loadWeeklyGrowth();
        loadMonthlySummary();

        initTopProductsPie();
          // ===== CHECK PERMISSION =====
    if (Session.role == null || !Session.role.equalsIgnoreCase("Manager")) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Access Denied");
        alert.setHeaderText(null);
        alert.setContentText("You do not have permission to access Reports.");
        alert.showAndWait();

        Platform.runLater(() -> {
            Stage stage = (Stage) revenueProgress.getScene().getWindow();
            stage.close();
        });

        return; // ❗ bắt buộc
    }
    }

    // ====== TOP: revenue this month ======
    private void loadRevenueThisMonth() {
        try {
            List<Report> list = dao.revenueByMonth();
            if (list.isEmpty()) {
                lbRevenue.setText("0");
                revenueProgress.setProgress(0);
                return;
            }

            String thisMonth = LocalDate.now().toString().substring(0, 7); // yyyy-MM
            double revenue = 0;

            for (Report r : list) {
                if (thisMonth.equals(r.getPeriod())) {
                    revenue = r.getTotalRevenue();
                    break;
                }
            }

            lbRevenue.setText(formatVND(revenue));

            // ProgressIndicator: nếu bạn không có target -> cho full khi có doanh thu
            revenueProgress.setProgress(revenue > 0 ? 1.0 : 0.0);

        } catch (Exception e) {
            e.printStackTrace();
            lbRevenue.setText("ERR");
            revenueProgress.setProgress(0);
        }
    }

    // ====== LineChart: weekly revenue ======
    private void loadWeeklyGrowth() {
    try {
        chartGrowth.getData().clear();

        List<Report> list = dao.revenueByWeek();

        // ✅ chỉ lấy 12 tuần gần nhất
        int n = 12;
        if (list.size() > n) {
            list = list.subList(list.size() - n, list.size());
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Revenue");

        for (Report r : list) {
            s.getData().add(new XYChart.Data<>(r.getPeriod(), r.getTotalRevenue()));
        }
        chartGrowth.getData().add(s);

        // ✅ giảm rối: xoay label + bỏ symbol
        CategoryAxis x = (CategoryAxis) chartGrowth.getXAxis();
        x.setTickLabelRotation(45);

        chartGrowth.setCreateSymbols(false);

    } catch (Exception e) {
        e.printStackTrace();
    }
}


    // ====== BarChart: monthly revenue ======
    private void loadMonthlySummary() {
        try {
            chartBottom.getData().clear();

            List<Report> list = dao.revenueByMonth();
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName("Revenue");

            for (Report r : list) {
                s.getData().add(new XYChart.Data<>(r.getPeriod(), r.getTotalRevenue()));
            }
            chartBottom.getData().add(s);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====== PIE: top products month/quarter ======
    private void initTopProductsPie() {
        cbTopMode.setItems(FXCollections.observableArrayList("Month", "Quarter"));
        cbTopMode.getSelectionModel().select("Month");

        cbTopMode.valueProperty().addListener((obs, o, n) -> reloadTopPeriods());
        cbTopPeriod.valueProperty().addListener((obs, o, n) -> reloadPie());

        reloadTopPeriods(); // load period list + pie
    }

    private void reloadTopPeriods() {
        try {
            String mode = cbTopMode.getValue();
            List<String> periods;

            if ("Quarter".equalsIgnoreCase(mode)) {
                periods = dao.listQuarters();
            } else {
                periods = dao.listMonths();
            }

            cbTopPeriod.setItems(FXCollections.observableArrayList(periods));

            if (!periods.isEmpty()) {
                cbTopPeriod.getSelectionModel().select(0); // chọn period mới nhất
            } else {
                pieTopProducts.setData(FXCollections.observableArrayList());
            }

        } catch (Exception e) {
            e.printStackTrace();
            pieTopProducts.setData(FXCollections.observableArrayList());
        }
    }

   private void reloadPie() {
    try {
        String mode = cbTopMode.getValue();
        String period = cbTopPeriod.getValue();
        if (period == null || period.isBlank()) {
            pieTopProducts.setData(FXCollections.observableArrayList());
            return;
        }

        List<TopProductStat> tops =
                "Quarter".equalsIgnoreCase(mode)
                        ? dao.topProductsByQuarter(period, 5)
                        : dao.topProductsByMonth(period, 5);

        var data = FXCollections.<PieChart.Data>observableArrayList();
        for (TopProductStat t : tops) {
            String label = t.getProductName() + " (" + t.getTotalQty() + ")";
            PieChart.Data d = new PieChart.Data(label, t.getTotalQty());
            data.add(d);
        }

        pieTopProducts.setData(data);

        // ✅ luôn hiện legend + label (label có thể bị ẩn khi nhỏ, tooltip sẽ chắc chắn có)
        pieTopProducts.setLegendVisible(true);
        pieTopProducts.setLabelsVisible(true);

        // ✅ tooltip cho từng lát
        for (PieChart.Data d : pieTopProducts.getData()) {
            javafx.scene.control.Tooltip.install(d.getNode(), new javafx.scene.control.Tooltip(d.getName()));
        }

    } catch (Exception e) {
        e.printStackTrace();
        pieTopProducts.setData(FXCollections.observableArrayList());
    }
}


    private String formatVND(double v) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(v) + " VND";
    }

   
}
