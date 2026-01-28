package model;

public class Report {

    // period: day / week / month / quarter / year
    private String period;

    // số đơn
    private int totalOrders;

    // doanh thu
    private double totalRevenue;

    // chi phí nhập
    private double totalImportCost;

    // lợi nhuận = revenue - importCost
    private double profit;

    public Report() {}

    // ===== GET SET =====
    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
        calcProfit();
    }

    public double getTotalImportCost() {
        return totalImportCost;
    }

    public void setTotalImportCost(double totalImportCost) {
        this.totalImportCost = totalImportCost;
        calcProfit();
    }

    public double getProfit() {
        return profit;
    }

    private void calcProfit() {
        this.profit = this.totalRevenue - this.totalImportCost;
    }
}
