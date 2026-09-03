package com.dns.bookshop.util;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.models.Product;
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

/**
 * Prints barcode labels for products.
 * Each label is a compact card with the barcode image and product name + price,
 * suitable for sticking onto items on shelves.
 */
public class BarcodeLabelPrinter implements Printable {

    private final Product product;
    private final BarcodeService barcodeService = new BarcodeService();

    // A 3x8 grid of labels on one A4 sheet.
    private static final int COLS = 3;
    private static final int ROWS = 8;

    public BarcodeLabelPrinter(Product product) {
        this.product = product;
    }

    public boolean print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        paper.setSize(595, 842); // A4 pts
        paper.setImageableArea(0, 0, 595, 842);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
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

    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double pageW = pageFormat.getImageableWidth();
        double pageH = pageFormat.getImageableHeight();
        double margin = 8;
        double cellW = (pageW - 2 * margin) / COLS;
        double cellH = (pageH - 2 * margin) / ROWS;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                double x = margin + c * cellW;
                double y = margin + r * cellH;
                drawLabel(g2, x, y, cellW, cellH);
            }
        }
        return PAGE_EXISTS;
    }

    private void drawLabel(Graphics2D g2, double x, double y, double w, double h) {
        // Border
        g2.setColor(new Color(0xcccccc));
        g2.drawRect((int) x, (int) y, (int) w, (int) h);

        double pad = 6;
        double innerW = w - 2 * pad;

        // Product name (top)
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        String name = product.getName();
        trimToWidth(g2, name, innerW);
        float nameH = (float) (g2.getFontMetrics().getHeight() + 2);
        g2.drawString(truncateStr(name, g2, innerW), (float) (x + pad), (float) (y + pad + g2.getFontMetrics().getAscent()));

        // Barcode image (middle)
        BufferedImage img = barcodeService.renderBarcode(product.getBarcode(), 300, 90);
        int imgW = (int) (innerW - 4);
        int imgH = (int) (imgW * ((double) img.getHeight() / img.getWidth()));
        int imgX = (int) (x + pad + 2);
        int imgY = (int) (y + pad + nameH);
        g2.setColor(Color.WHITE);
        g2.fillRect(imgX, imgY, imgW, imgH);
        g2.drawImage(img, imgX, imgY, imgW, imgH, null);

        // Barcode string + price (bottom)
        g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
        double numY = imgY + imgH + pad;
        g2.drawString(product.getBarcode(), imgX, (int) numY);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String price = "LKR " + String.format("%.0f", product.getSellingPrice());
        g2.setColor(new Color(0x1f3a93));
        g2.drawString(price, imgX, (int) (numY + g2.getFontMetrics().getHeight() + 2));
    }

    private String truncateStr(String s, Graphics2D g2, double maxW) {
        if (s == null) return "";
        while (g2.getFontMetrics().getStringBounds(s, g2).getWidth() > maxW && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private void trimToWidth(Graphics2D g2, String s, double maxW) {
        // keep for layout consistency
    }
}
