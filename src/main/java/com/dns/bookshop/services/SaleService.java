package com.dns.bookshop.services;

import com.dns.bookshop.db.Database;
import com.dns.bookshop.db.repositories.AuditLogRepository;
import com.dns.bookshop.db.repositories.ProductRepository;
import com.dns.bookshop.db.repositories.SaleRepository;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.models.Sale;
import com.dns.bookshop.models.SaleItem;
import com.dns.bookshop.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Business logic for sales / checkout / refunds.
 */
public class SaleService {

    private final SaleRepository saleRepo;
    private final ProductRepository productRepo;
    private final AuditLogRepository audit;
    private static final AtomicInteger counter = new AtomicInteger(0);

    public SaleService() {
        this.saleRepo = new SaleRepository();
        this.productRepo = new ProductRepository();
        this.audit = new AuditLogRepository();
    }

    /**
     * Computes a unique invoice number like DNS-20260903-00042.
     */
    public String nextInvoiceNumber() {
        String prefix = Database.getInstance().getSetting("invoice.prefix", "DNS");
        java.time.LocalDate today = java.time.LocalDate.now();
        String date = String.format("%04d%02d%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        long seq = System.currentTimeMillis() % 100000;
        return prefix + "-" + date + "-" + String.format("%05d", seq);
    }

    /**
     * Completes a sale: persists sale + items and decrements stock atomically.
     */
    public Sale completeSale(List<SaleItem> items, User cashier, Integer customerId,
                             double discount, double taxRate, String paymentMethod,
                             double amountPaid) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }

        double subtotal = 0;
        for (SaleItem it : items) {
            Product p = productRepo.findById(it.getProductId());
            if (p == null) continue;
            it.setUnitPrice(p.getSellingPrice());
            it.setLineTotal(round2(it.getQuantity() * p.getSellingPrice()));
            subtotal += it.getLineTotal();
            it.setProductName(p.getName());
            it.setBarcode(p.getBarcode());
        }
        subtotal = round2(subtotal);
        double tax = round2(subtotal * taxRate / 100.0);
        double total = round2(subtotal - discount + tax);
        double change = Math.max(0, round2(amountPaid - total));

        Sale sale = new Sale();
        sale.setInvoiceNumber(nextInvoiceNumber());
        sale.setUserId(cashier.getId());
        sale.setCustomerId(customerId);
        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTax(tax);
        sale.setTotal(total);
        sale.setAmountPaid(amountPaid);
        sale.setChangeGiven(change);
        sale.setPaymentMethod(paymentMethod);
        sale.setStatus(Sale.STATUS_COMPLETE);
        sale.setCreatedAt(System.currentTimeMillis());

        int id = saleRepo.createSaleWithItems(sale, items);
        sale.setItems(items);
        sale.setId(id);

        audit.log(cashier.getId(), cashier.getUsername(),
                "SALE", "Invoice " + sale.getInvoiceNumber() + " total=" + sale.getTotal());

        return sale;
    }

    public void refund(int saleId, User user) {
        Sale sale = saleRepo.findById(saleId);
        if (sale == null) throw new IllegalArgumentException("Sale not found.");
        if (Sale.STATUS_REFUNDED.equals(sale.getStatus())) {
            throw new IllegalArgumentException("This sale has already been refunded.");
        }
        saleRepo.refundSale(saleId, user.getId());
        audit.log(user.getId(), user.getUsername(), "REFUND", "Refunded invoice " + sale.getInvoiceNumber());
    }

    public Sale findByInvoice(String invoice) { return saleRepo.findByInvoice(invoice); }
    public Sale findById(int id) { return saleRepo.findById(id); }
    public List<Sale> findRecent(int limit) { return saleRepo.findRecent(limit); }
    public List<Sale> findBetween(long start, long end) { return saleRepo.findBetween(start, end); }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
