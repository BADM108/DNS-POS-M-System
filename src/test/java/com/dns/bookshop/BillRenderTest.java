package com.dns.bookshop;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.db.Database;
import com.dns.bookshop.models.Customer;
import com.dns.bookshop.models.Sale;
import com.dns.bookshop.models.SaleItem;
import com.dns.bookshop.models.User;
import com.dns.bookshop.util.BillPrinter;
import com.dns.bookshop.util.BarcodeLabelPrinter;
import com.dns.bookshop.models.Product;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies the A4 bill and barcode label render without errors.
 */
public class BillRenderTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path tmp = Files.createTempDirectory("dns-bill-test");
        AppConfig.setDataDir(tmp);
        Database.getInstance().init();

        User cashier = new User("cashier", "x", "Cashier Name", "WORKER");
        cashier.setId(1);

        Customer customer = new Customer("John Doe", "0712345678", "john@example.com", "Colombo");

        Sale sale = new Sale();
        sale.setId(1);
        sale.setInvoiceNumber("DNS-20260903-00001");
        sale.setUserId(1);
        sale.setCustomerId(1);
        sale.setSubtotal(5250.00);
        sale.setDiscount(250.00);
        sale.setTax(0.00);
        sale.setTotal(5000.00);
        sale.setAmountPaid(5000.00);
        sale.setChangeGiven(0.00);
        sale.setPaymentMethod("CASH");
        sale.setStatus(Sale.STATUS_COMPLETE);
        sale.setCreatedAt(System.currentTimeMillis());

        SaleItem i1 = new SaleItem();
        i1.setProductName("Intermediate Maths (O/L)");
        i1.setBarcode("9789553000111");
        i1.setQuantity(2);
        i1.setUnitPrice(2000);
        i1.setLineTotal(4000);
        SaleItem i2 = new SaleItem();
        i2.setProductName("A4 Ruled Notebooks");
        i2.setBarcode("DNS000123456");
        i2.setQuantity(5);
        i2.setUnitPrice(250);
        i2.setLineTotal(1250);
        sale.setItems(List.of(i1, i2));

        // Render bill to image.
        BillPrinter bp = new BillPrinter(sale, customer, cashier);
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        paper.setSize(595, 842);
        paper.setImageableArea(56.7, 56.7, 595 - 113.4, 842 - 113.4);
        pf.setPaper(paper);
        BufferedImage img = new BufferedImage(595, 842, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        int code = bp.print(g, pf, 0);
        g.dispose();
        boolean billOk = code == java.awt.print.Printable.PAGE_EXISTS;
        System.out.println((billOk ? "PASS" : "FAIL") + " - A4 bill rendered (return=" + code + ")");

        // Walk-in customer (null) must also render.
        BillPrinter bp2 = new BillPrinter(sale, null, cashier);
        BufferedImage img2 = new BufferedImage(595, 842, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img2.createGraphics();
        int code2 = bp2.print(g2, pf, 0);
        g2.dispose();
        System.out.println((code2 == java.awt.print.Printable.PAGE_EXISTS ? "PASS" : "FAIL")
                + " - A4 bill (walk-in) rendered");

        // Barcode label render.
        Product p = new Product();
        p.setName("Intermediate Maths (O/L)");
        p.setBarcode("9789553000111");
        p.setSellingPrice(2000);
        BarcodeLabelPrinter blp = new BarcodeLabelPrinter(p);
        int codel = blp.print(g2, pf, 0);
        System.out.println((codel == java.awt.print.Printable.PAGE_EXISTS ? "PASS" : "FAIL")
                + " - barcode label rendered");

        if (!billOk) System.exit(1);
    }
}
