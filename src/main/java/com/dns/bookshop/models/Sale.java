package com.dns.bookshop.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a completed sale (bill/transaction).
 */
public class Sale {
    public static final String STATUS_COMPLETE = "COMPLETE";
    public static final String STATUS_REFUNDED = "REFUNDED";

    private int id;
    private String invoiceNumber;
    private int userId;      // cashier who made the sale
    private Integer customerId; // nullable (walk-in)
    private double subtotal;
    private double discount;
    private double tax;
    private double total;
    private double amountPaid;
    private double changeGiven;
    private String paymentMethod; // CASH, CARD
    private long createdAt;
    private String status;

    public Sale() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public double getChangeGiven() { return changeGiven; }
    public void setChangeGiven(double changeGiven) { this.changeGiven = changeGiven; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private final List<SaleItem> items = new ArrayList<>();

    public List<SaleItem> getItems() { return items; }

    public void setItems(List<SaleItem> saleItems) {
        items.clear();
        if (saleItems != null) items.addAll(saleItems);
    }

    public void addItem(SaleItem item) { items.add(item); }
}
