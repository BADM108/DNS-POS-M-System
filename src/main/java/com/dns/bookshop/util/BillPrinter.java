package com.dns.bookshop.util;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.models.Customer;
import com.dns.bookshop.models.Sale;
import com.dns.bookshop.models.SaleItem;
import com.dns.bookshop.models.User;
import com.dns.bookshop.services.BarcodeService;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders and prints a proper A4-sized bill for DNS BookShop.
 *
 * The bill is drawn as a vector document (not a raster) so it stays crisp on
 * A4 paper. It includes the shop header, customer & cashier info, an itemised
 * table, totals, payment summary and a barcode/QR footer.
 */
public class BillPrinter implements Printable {

    private final Sale sale;
    private final Customer customer;
    private final User cashier;
    private final String shopName;
    private final String addressLine;
    private final String contactLine;

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final double A4_WIDTH_MM = 210.0;
    private static final double A4_HEIGHT_MM = 297.0;
    private static final double MM_TO_POINTS = 72.0 / 25.4;

    public BillPrinter(Sale sale, Customer customer, User cashier) {
        this.sale = sale;
        this.customer = customer;
        this.cashier = cashier;
        this.shopName = AppConfig.SHOP_NAME;
        this.addressLine = "Main Road, Book City";
        this.contactLine = "Phone: 0123-456789 | Email: dnsbookshop@example.com";
    }

    /**
     * Sends the bill to the default printer. Returns true if the user printed it.
     */
    public boolean print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = preparePageFormat(job);
        job.setPrintable(this, pf);
        if (job.printDialog()) {
            try {
                job.print();
                return true;
            } catch (PrinterException e) {
                throw new RuntimeException("Printing failed: " + e.getMessage(), e);
            }
        }
        return false;
    }

    private PageFormat preparePageFormat(PrinterJob job) {
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        double w = A4_WIDTH_MM * MM_TO_POINTS;
        double h = A4_HEIGHT_MM * MM_TO_POINTS;
        paper.setSize(w, h);
        paper.setImageableArea(56.7, 56.7, w - 113.4, h - 113.4); // ~2cm margins
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double x = pageFormat.getImageableX();
        double y = pageFormat.getImageableY();
        double pageW = pageFormat.getImageableWidth();
        double pageH = pageFormat.getImageableHeight();

        Color ink = new Color(0x1a1a1a);
        Color accent = new Color(0x1f3a93);
        Color lightGray = new Color(0xf0f0f0);
        Color gray = new Color(0x666666);

        g2.setColor(ink);

        double cursor = y;

        // ---- Shop header ----
        g2.setColor(accent);
        g2.setFont(new Font("SansSerif", Font.BOLD, 26));
        g2.drawString("DNS BOOKSHOP", (float) x, (float) (cursor + 28));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(gray);
        g2.drawString(addressLine, (float) x, (float) (cursor + 46));
        g2.drawString(contactLine, (float) x, (float) (cursor + 62));
        cursor += 70;

        // separator line
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawLine((int) x, (int) cursor, (int) (x + pageW), (int) cursor);
        cursor += 12;

        // ---- Title (TAX INVOICE) centred ----
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(ink);
        String title = "TAX INVOICE";
        Rectangle2D tb = g2.getFontMetrics().getStringBounds(title, g2);
        g2.drawString(title, (float) (x + (pageW - tb.getWidth()) / 2), (float) (cursor + 16));
        cursor += 26;

        // ---- Invoice metadata (right side) ----
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(gray);
        String inv = sale.getInvoiceNumber();
        g2.drawString("Invoice No.:  " + inv, (float) (x + pageW - 200), (float) (cursor + 10));
        String dateStr = DateTimeFormatter.ofPattern("dd-MM-yyyy  HH:mm:ss")
                .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(sale.getCreatedAt()));
        g2.drawString("Date:  " + dateStr, (float) (x + pageW - 200), (float) (cursor + 26));
        cursor += 40;

        // ---- Parties: Billed To + Cashier ----
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(ink);
        g2.drawString("Customer:", (float) x, (float) cursor);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        if (customer != null) {
            g2.drawString(customer.getName(), (float) x, (float) (cursor + 16));
            if (customer.getPhone() != null && !customer.getPhone().isEmpty())
                g2.drawString("Phone: " + customer.getPhone(), (float) x, (float) (cursor + 31));
        } else {
            g2.drawString("Walk-in Customer (Cash Sale)", (float) x, (float) (cursor + 16));
        }

        // Cashier on right
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(gray);
        String cashierName = cashier != null ? cashier.getFullName() : "";
        g2.drawString("Cashier: " + cashierName, (float) (x + pageW - 200), (float) (cursor + 4));
        cursor += 52;

        // ---- Table headers ----
        double[] cols = tableColumns(x, pageW);
        g2.setColor(lightGray);
        g2.fillRect((int) x, (int) cursor, (int) pageW, 26);
        g2.setColor(accent);
        g2.fillRect((int) x, (int) cursor, 6, 26);
        g2.setColor(ink);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        drawHeader(g2, "ITEM", cols[0], cols[1], cursor, 26);
        drawHeader(g2, "QTY", cols[1], cols[2], cursor, 26);
        drawHeader(g2, "PRICE", cols[2], cols[3], cursor, 26);
        drawHeader(g2, "AMOUNT", cols[3], cols[4], cursor, 26);
        cursor += 26;

        // ---- Items ----
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        double[] colsI = tableColumns(x, pageW);
        for (int i = 0; i < sale.getItems().size(); i++) {
            SaleItem it = sale.getItems().get(i);
            if (cursor + 26 > y + pageH - 120) {
                renderFooter(g2, x, y + pageH - 120, pageW);
                return PAGE_EXISTS;
            }
            if (i % 2 == 1) {
                g2.setColor(lightGray);
                g2.fillRect((int) x, (int) cursor, (int) pageW, 26);
                g2.setColor(ink);
            }
            String name = it.getProductName();
            g2.drawString(truncate(name, g2, (colsI[1] - colsI[0]) - 12, 30), (float) (x + 8), (float) (cursor + 17));
            drawCellText(g2, String.valueOf(it.getQuantity()), colsI[1], colsI[2], cursor, 26);
            drawCellText(g2, MONEY.format(it.getUnitPrice()), colsI[2], colsI[3], cursor, 26);
            drawCellText(g2, MONEY.format(it.getLineTotal()), colsI[3], colsI[4], cursor, 26);
            cursor += 26;
        }

        cursor += 6;

        // ---- Totals ----
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        double right = x + pageW - 260;
        g2.setColor(gray);
        g2.drawString("Subtotal:                    ", (float) right, (float) (cursor + 12));
        g2.setColor(ink);
        String subRight = MONEY.format(sale.getSubtotal());
        g2.drawString(subRight, (float) (x + pageW - 120), (float) (cursor + 12));

        cursor += 22;
        g2.setColor(gray);
        g2.drawString("Discount:", (float) right, (float) (cursor + 12));
        g2.setColor(ink);
        g2.drawString(MONEY.format(sale.getDiscount()), (float) (x + pageW - 120), (float) (cursor + 12));

        if (sale.getTax() > 0) {
            cursor += 22;
            g2.setColor(gray);
            g2.drawString("Tax:        ", (float) right, (float) (cursor + 12));
            g2.setColor(ink);
            g2.drawString(MONEY.format(sale.getTax()), (float) (x + pageW - 120), (float) (cursor + 12));
        }

        cursor += 28;
        g2.setColor(accent);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.drawString("GRAND TOTAL", (float) right, (float) (cursor + 12));
        g2.drawLine((int) (x + pageW - 140), (int) cursor, (int) (x + pageW - 20), (int) cursor);
        g2.drawString(MONEY.format(sale.getTotal()), (float) (x + pageW - 120), (float) (cursor + 12));

        // Payment summary
        cursor += 48;
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(gray);
        g2.drawString("PAID:        " + MONEY.format(sale.getAmountPaid()), (float) right, (float) (cursor + 2));
        g2.drawString("CHANGE:   " + MONEY.format(sale.getChangeGiven()), (float) right, (float) (cursor + 18));
        g2.drawString("METHOD:   " + sale.getPaymentMethod(), (float) right, (float) (cursor + 34));
        cursor += 60;

        renderFooter(g2, x, y + pageH - 140, pageW);
        return PAGE_EXISTS;
    }

    private void renderFooter(Graphics2D g2, double x, double y, double pageW) {
        g2.setColor(new Color(0x1f3a93));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine((int) x, (int) y, (int) (x + pageW), (int) y);
        g2.setColor(new Color(0x666666));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String note = "Thank you for shopping at " + AppConfig.SHOP_NAME + "!";
        Rectangle2D nb = g2.getFontMetrics().getStringBounds(note, g2);
        g2.drawString(note, (float) (x + (pageW - nb.getWidth()) / 2), (float) (y + 16));
        try {
            BarcodeService bs = new BarcodeService();
            BufferedImage img = bs.renderBarcode(sale.getInvoiceNumber(), 400, 120);
            int iw = 160, ih = 48;
            int ix = (int) (x + (pageW - iw) / 2);
            g2.drawImage(img, ix, (int) y + 24, iw, ih, null);
        } catch (Exception ignored) {
            // barcode unrenderable - skip
        }
        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2.setColor(new Color(0x999999));
        String inv = sale.getInvoiceNumber();
        Rectangle2D ib = g2.getFontMetrics().getStringBounds(inv, g2);
        g2.drawString(inv, (float) (x + (pageW - ib.getWidth()) / 2), (float) (y + 80));
    }

    private double[] tableColumns(double x, double pageW) {
        double ix = x;
        double itemX = ix;
        double qtyX = ix + pageW * 0.46;
        double priceX = qtyX + pageW * 0.12;
        double amtX = priceX + pageW * 0.18;
        double rightX = ix + pageW;
        return new double[]{itemX, qtyX, priceX, amtX, rightX};
    }

    private void drawHeader(Graphics2D g2, String text, double left, double right, double y, double h) {
        g2.drawString(text, (float) (left + 8), (float) (y + h - 8));
    }

    private void drawCellText(Graphics2D g2, String text, double left, double right, double y, double h) {
        double textW = g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        double tx = right - textW - 8;
        g2.drawString(text, (float) tx, (float) (y + h - 8));
    }

    private String truncate(String s, Graphics2D g2, double maxW, int maxLen) {
        if (s == null) return "";
        if (s.length() > maxLen) s = s.substring(0, maxLen);
        while (g2.getFontMetrics().getStringBounds(s, g2).getWidth() > maxW && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
