package controller;

import DAO_Report.ReportDAO;
import app.ConnectDB;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Report;

import java.net.URL;
import java.sql.Connection;
import java.util.List;
import java.util.ResourceBundle;

public class ReportController implements Initializable {

    // ===== CHART TOP =====
    @FXML private BarChart<String, Number> chartTopLeft;
    @FXML private LineChart<String, Number> chartTopRight;

    // ===== CHART BOTTOM =====
    @FXML private BarChart<String, Number> chartBottom;

    // ===== IMAGE =====
    @FXML private ImageView imgTop;
    @FXML private ImageView imgBottom;

    private ReportDAO reportDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        try {
            // ✅ DÙNG CONNECTDB CỦA M
            Connection conn = ConnectDB.getConnection();
            reportDAO = new ReportDAO(conn);

            loadImages();
            loadTopRevenueChart();
            loadGrowthChart();
            loadBottomRevenueProfitChart();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= IMAGE =================
    private void loadImages() {
        imgTop.setImage(
            new Image(getClass().getResource("/image/report1.png").toExternalForm())
        );
        imgBottom.setImage(
            new Image(getClass().getResource("/image/report2.png").toExternalForm())
        );
    }

    // ================= TOP LEFT =================
    // Tổng doanh thu theo tháng (SQL thật)
    private void loadTopRevenueChart() throws Exception {

        chartTopLeft.getData().clear();
        chartTopLeft.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        List<Report> list = reportDAO.revenueByMonth();
        for (Report r : list) {
            series.getData().add(
                new XYChart.Data<>(r.getPeriod(), r.getTotalRevenue())
            );
        }

        chartTopLeft.getData().add(series);

        // màu xanh giống UI mẫu
        series.getNode().setStyle("-fx-bar-fill:#A7C957;");
    }

    // ================= TOP RIGHT =================
    // Định hướng phát triển (forecast)
    private void loadGrowthChart() {

        chartTopRight.getData().clear();
        chartTopRight.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        series.getData().add(new XYChart.Data<>("2024", 100));
        series.getData().add(new XYChart.Data<>("2025", 140));
        series.getData().add(new XYChart.Data<>("2026", 190));
        series.getData().add(new XYChart.Data<>("2027", 260));

        chartTopRight.getData().add(series);
    }

    // ================= BOTTOM =================
    // Doanh thu & lợi nhuận (SQL thật)
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

        // màu giống ảnh summary
        revenueSeries.getNode().setStyle("-fx-bar-fill:#C7E77F;");
        profitSeries.getNode().setStyle("-fx-bar-fill:#1F3D2B;");
    }
}
