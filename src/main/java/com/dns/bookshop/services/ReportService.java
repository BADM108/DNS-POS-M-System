package com.dns.bookshop.services;

import com.dns.bookshop.db.repositories.CustomerRepository;
import com.dns.bookshop.db.repositories.ProductRepository;
import com.dns.bookshop.db.repositories.SaleRepository;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.models.Sale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregates data for the dashboard and reports.
 */
public class ReportService {

    private final SaleRepository saleRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;

    public ReportService() {
        this.saleRepo = new SaleRepository();
        this.productRepo = new ProductRepository();
        this.customerRepo = new CustomerRepository();
    }

    public long startOfToday() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        java.time.ZonedDateTime start = now.toLocalDate().atStartOfDay(now.getZone());
        return start.toInstant().toEpochMilli();
    }

    public long startOfLast7Days() {
        return startOfToday() - 6L * 86400000L;
    }

    public long startOfMonth() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        java.time.ZonedDateTime start = now.toLocalDate().withDayOfMonth(1).atStartOfDay(now.getZone());
        return start.toInstant().toEpochMilli();
    }

    /**
     * A summary of key metrics for a given period (start..end millis).
     */
    public Map<String, Object> summary(long start, long end) {
        Map<String, Object> out = new TreeMap<>();
        List<Sale> sales = saleRepo.findBetween(start, end);
        double revenue = 0;
        int count = 0;
        for (Sale s : sales) {
            if (Sale.STATUS_REFUNDED.equals(s.getStatus())) continue;
            revenue += s.getTotal();
            count++;
        }
        out.put("sales", count);
        out.put("revenue", Math.round(revenue * 100.0) / 100.0);
        out.put("products", productRepo.findAll().size());
        out.put("lowStock", productRepo.findLowStock().size());
        out.put("customers", customerRepo.findAll().size());

        long last7Start = startOfLast7Days();
        out.put("weeklyRevenue", Math.round(saleRepo.revenueBetweenExcludingRefund(last7Start, end) * 100.0) / 100.0);
        return out;
    }

    /**
     * Daily revenue as a time-ordered list of [dayStartMillis, revenue].
     */
    public List<double[]> dailyRevenueSeries(long start, long end) {
        Map<Long, double[]> data = saleRepo.dailyRevenue(start, end);
        List<double[]> series = new ArrayList<>();
        // Fill gaps with zero so chart looks continuous.
        for (long day = start; day <= end; day += 86400000L) {
            double[] v = data.getOrDefault(normalizeDay(day), new double[]{0, 0});
            series.add(new double[]{normalizeDay(day), v[1]});
        }
        return series;
    }

    private long normalizeDay(long ms) {
        return (ms / 86400000L) * 86400000L;
    }

    public List<Product> topProducts(long start, long end, int limit) {
        Map<Integer, Integer> counts = saleRepo.topProducts(start, end, limit);
        List<Product> top = new ArrayList<>();
        List<Product> all = productRepo.findAll();
        java.util.Map<Integer, Product> byId = new java.util.HashMap<>();
        for (Product p : all) byId.put(p.getId(), p);
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            Product p = byId.get(e.getKey());
            if (p != null) {
                top.add(p);
            }
        }
        return top;
    }

    public Map<Integer, Integer> topProductQuantities(long start, long end, int limit) {
        return saleRepo.topProducts(start, end, limit);
    }

    public List<Sale> recentSales(int limit) { return saleRepo.findRecent(limit); }
}
