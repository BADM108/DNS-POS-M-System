package com.dns.bookshop.models;

/**
 * Represents a product/item in the shop. Can be a book or stationery.
 * Books share the same barcode for the same title/edition.
 */
public class Product {
    private int id;
    private String name;
    private String category;      // BOOK or STATIONERY
    private String barcode;       // EAN-13 / ISBN / Code128
    private String supplier;
    private double purchasePrice; // cost price
    private double sellingPrice;  // sale price
    private int stockQuantity;
    private int lowStockThreshold;
    private boolean barcodeGenerated; // true if we generated this barcode
    private long createdAt;

    public Product() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public boolean isBarcodeGenerated() { return barcodeGenerated; }
    public void setBarcodeGenerated(boolean barcodeGenerated) { this.barcodeGenerated = barcodeGenerated; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isLowStock() {
        return stockQuantity <= lowStockThreshold;
    }

    @Override
    public String toString() {
        return name;
    }
}
