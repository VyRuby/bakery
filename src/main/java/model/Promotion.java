package model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Promotion {

    private String promoId;
    private String promoName;
    private String description;
   

    // enum('percent','fixed')
    private String promoType;
    private double value;

    // enum('Active','Inactive')
    private String status;

    // mapping PROMOTION_PRODUCT
    private List<String> productIds = new ArrayList<>();

    public Promotion() {}

    public Promotion(String promoId, String promoName, String description,
                     String promoType, double value, String status) {
        this.promoId = promoId;
        this.promoName = promoName;
        this.description = description;
     
        this.promoType = promoType;
        this.value = value;
        this.status = status;
    }

    public String getPromoId() { return promoId; }
    public void setPromoId(String promoId) { this.promoId = promoId; }

    public String getPromoName() { return promoName; }
    public void setPromoName(String promoName) { this.promoName = promoName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

  

    public String getPromoType() { return promoType; }
    public void setPromoType(String promoType) { this.promoType = promoType; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getProductIds() { return productIds; }
    public void setProductIds(List<String> productIds) {
        this.productIds = (productIds == null) ? new ArrayList<>() : productIds;
    }

    @Override
    public String toString() {
        return promoId + " - " + promoName;
    }
}
