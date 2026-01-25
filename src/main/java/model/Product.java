package model;

public class Product {

    private String productId;
    private String productName;
    private String categoryId;
    private int quantity;
    private String unit;
    private float costPrice;
    private float price;
    private String description;
    private String image;

    // ✅ NEW: status (Active / Inactive)
    private String status;

    // ===== Constructors =====

    // Constructor đầy đủ (dùng cho DAO)
    public Product(String productId, String productName, String categoryId,
                   int quantity, String unit,
                   float costPrice, float price,
                   String description, String image,
                   String status) {

        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.quantity = quantity;
        this.unit = unit;
        this.costPrice = costPrice;
        this.price = price;
        this.description = description;
        this.image = image;
        this.status = status;
    }

    // Constructor cũ (để KHÔNG vỡ code cũ)
    public Product(String productId, String productName, String categoryId,
                   int quantity, String unit,
                   float costPrice, float price,
                   String description, String image) {

        this(productId, productName, categoryId,
             quantity, unit,
             costPrice, price,
             description, image,
             "Active"); // mặc định Active
    }

    // Constructor rỗng
    public Product() {
        this.status = "Active";
    }

    // ===== Getters & Setters =====

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public float getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(float costPrice) {
        this.costPrice = costPrice;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // ===== STATUS =====
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            this.status = "Active";
        } else {
            this.status = status;
        }
    }

    // ===== Optional: toString (hiển thị đẹp trong ComboBox/ListView) =====
    @Override
    public String toString() {
        return productId + " - " + productName;
    }
}
