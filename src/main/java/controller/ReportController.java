package controller;

import DAO_Report.ReportDAO;
import app.ConnectDB;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Report;

import java.net.URL;
import java.sql.Connection;
import java.util.List;
import java.util.ResourceBundle;

public class ReportController extends BacktoHomeController implements Initializable {

    // ===== TOP LEFT (DONUT) =====
    @FXML private ProgressIndicator revenueProgress;
    @FXML private Label lbRevenue;

    // ===== TOP RIGHT (LINE) =====
    @FXML private LineChart<String, Number> chartGrowth;

    // ===== BOTTOM =====
    @FXML private BarChart<String, Number> chartBottom;

    // ===== IMAGE =====
    @FXML private ImageView imgTop;
    @FXML private ImageView imgBottom;

    private ReportDAO reportDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        try {
            Connection conn = ConnectDB.getConnection();
            reportDAO = new ReportDAO(conn);

//            loadImages();
            loadTotalRevenue();
            loadGrowthChart();
            loadBottomRevenueProfitChart();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    // ================= IMAGE =================
//    private void loadImages() {
//        imgTop.setImage(
//                new Image(getClass().getResource("/images/farm1.jpg").toExternalForm())
//        );
//        imgBottom.setImage(
//                new Image(getClass().getResource("/images/farm2.jpg").toExternalForm())
//        );
//    }

    // ================= TOTAL REVENUE (DONUT) =================
    private void loadTotalRevenue() throws Exception {

        double total = 0;
        List<Report> list = reportDAO.revenueByMonth();
        for (Report r : list) {
            total += r.getTotalRevenue();
        }

        lbRevenue.setText("$" + String.format("%,.0f", total));

        // giả lập progress (vì JavaFX ProgressIndicator không phải donut chart thật)
        revenueProgress.setProgress(0.75);
    }

    // ================= GROWTH =================
    private void loadGrowthChart() {

        chartGrowth.getData().clear();
        chartGrowth.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        series.getData().add(new XYChart.Data<>("2024", 100));
        series.getData().add(new XYChart.Data<>("2025", 140));
        series.getData().add(new XYChart.Data<>("2026", 190));
        series.getData().add(new XYChart.Data<>("2027", 260));

        chartGrowth.getData().add(series);
    }

    // ================= BOTTOM =================
    private void loadBottomRevenueProfitChart() throws Exception {

        chartBottom.getData().clear();
        chartBottom.setLegendVisible(true);

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue");

        XYChart.Series<String, Number> profitSeries = new XYChart.Series<>();
        profitSeries.setName("Profit");

        List<Report> list = reportDAO.profitByMonth();
        for (Report r : list) {
            revenueSeries.getData().add(
                    new XYChart.Data<>(r.getPeriod(), r.getTotalRevenue())
            );
            profitSeries.getData().add(
                    new XYChart.Data<>(r.getPeriod(), r.getProfit())
            );
        }

        chartBottom.getData().addAll(revenueSeries, profitSeries);
    }
}
