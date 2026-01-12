package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Promotion {

    private String promoId;
    private String promoName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String promoType;   // percent | fixed
    private double value;
    private String status;      // Active | Inactive

    //  1 promo → nhiều product
    private List<String> productIds = new ArrayList<>();

    /* ===================== CONSTRUCTORS ===================== */

    public Promotion() {
    }

    public Promotion(String promoId, String promoName, String description,
                     LocalDate startDate, LocalDate endDate,
                     String promoType, double value, String status) {
        this.promoId = promoId;
        this.promoName = promoName;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.promoType = promoType;
        this.value = value;
        this.status = status;
    }

    /* ===================== GETTERS & SETTERS ===================== */

    public String getPromoId() {
        return promoId;
    }

    public void setPromoId(String promoId) {
        this.promoId = promoId;
    }

    public String getPromoName() {
        return promoName;
    }

    public void setPromoName(String promoName) {
        this.promoName = promoName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPromoType() {
        return promoType;
    }

    public void setPromoType(String promoType) {
        this.promoType = promoType;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /* ===================== PRODUCT IDS ===================== */

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    public void addProductId(String productId) {
        if (!this.productIds.contains(productId)) {
            this.productIds.add(productId);
        }
    }

    /* ===================== HELPER ===================== */

    @Override
    public String toString() {
        return promoId + " - " + promoName;
    }
}
