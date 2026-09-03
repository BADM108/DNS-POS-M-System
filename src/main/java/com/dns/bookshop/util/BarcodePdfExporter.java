package com.dns.bookshop.util;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.services.BarcodeService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Generates a printable A4 sheet of barcode labels as a PDF.
 *
 * Each label shows the product name, its barcode image, the barcode number and
 * the selling price - arranged in a grid that can be printed and cut into
 * individual stickers. The file is written into the app's exports folder so the
 * shop can print as many copies as needed, and the file stays local with the app.
 */
public class BarcodePdfExporter {

    private final BarcodeService barcodeService = new BarcodeService();

    // A4 grid: 3 columns x 8 rows of labels.
    private static final int COLS = 3;
    private static final int ROWS = 8;

    private static final java.awt.Color PDF_BAR = new java.awt.Color(0x1f3a93);

    /**
     * Builds an A4 PDF sheet containing one label per product.
     * Returns the generated file path.
     */
    public File exportSheet(List<Product> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Select at least one product to export barcodes for.");
        }
        File out = exportsFile();
        return writeGrid(products, out);
    }

    private File writeGrid(List<Product> products, File out) {
        try (FileOutputStream fos = new FileOutputStream(out)) {
            Document doc = new Document(PageSize.A4, 24, 24, 24, 24);
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            doc.open();
            writeTitle(doc, products.size());
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();

            float pageW = PageSize.A4.getWidth() - 48;
            float pageH = PageSize.A4.getHeight() - 48;
            float cellW = pageW / COLS;
            float cellH = pageH / ROWS;

            int index = 0;
            while (index < products.size()) {
                float x = 24, y = PageSize.A4.getHeight() - 24; // top-left
                for (int r = 0; r < ROWS && index < products.size(); r++) {
                    for (int c = 0; c < COLS && index < products.size(); c++, index++) {
                        Product p = products.get(index);
                        drawLabel(cb, p, x + c * cellW, y - (r + 1) * cellH, cellW, cellH);
                    }
                }
                if (index < products.size()) {
                    doc.newPage();
                }
            }
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Could not write barcode PDF: " + e.getMessage(), e);
        }
        return out;
    }

    private void writeTitle(Document doc, int count) throws Exception {
        Paragraph brand = new Paragraph("DNS BookShop - Barcode Labels",
                new Font(Font.HELVETICA, 13, Font.BOLD));
        brand.setSpacingAfter(2);
        doc.add(brand);
        Paragraph sub = new Paragraph(new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date())
                + "  |  " + count + " label(s)", new Font(Font.HELVETICA, 9));
        sub.setSpacingAfter(10);
        doc.add(sub);
    }

    private void drawLabel(com.lowagie.text.pdf.PdfContentByte cb, Product p,
                           float x, float top, float w, float h) throws Exception {
        float pad = 4;

        // Label border
        cb.setLineWidth(0.6f);
        cb.setColorStroke(new Color(0x999999));
        cb.rectangle(x + 1, top - h + 1, w - 2, h - 2);
        cb.stroke();

        float innerW = w - 2 * pad;

        // Barcode image sized to fit the middle of the cell.
        BufferedImage img = barcodeService.renderBarcode(p.getBarcode(), 300, 90);
        float imgW = Math.min(innerW - 4, innerW * 0.8f);
        float imgH = imgW * ((float) img.getHeight() / img.getWidth());
        float imgX = x + pad + 2;
        float imgY = top - pad - imgH;

        float textStartY = imgY - 2;

        com.lowagie.text.Image pdfImg = com.lowagie.text.Image.getInstance(toBmp(img));
        pdfImg.scaleToFit(imgW, imgH);
        pdfImg.setAbsolutePosition(imgX, imgY - imgH);
        cb.addImage(pdfImg);

        // Barcode number (small, under the image)
        cb.beginText();
        cb.setFontAndSize(com.lowagie.text.pdf.BaseFont.createFont(
                com.lowagie.text.pdf.BaseFont.HELVETICA, com.lowagie.text.pdf.BaseFont.WINANSI,
                com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED), 8);
        cb.setColorFill(new Color(0x333333));
        float numW = cb.getEffectiveStringWidth(p.getBarcode(), true);
        cb.showTextAligned(Element.ALIGN_CENTER, p.getBarcode(), imgX + imgW / 2, textStartY, 0);
        cb.endText();

        // Product name (top of label, clipped)
        String name = clip(p.getName(), 20);
        cb.beginText();
        cb.setFontAndSize(com.lowagie.text.pdf.BaseFont.createFont(
                com.lowagie.text.pdf.BaseFont.HELVETICA, com.lowagie.text.pdf.BaseFont.WINANSI,
                com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED), 8);
        cb.setColorFill(new Color(0x111111));
        cb.showTextAligned(Element.ALIGN_LEFT, name, x + pad, top - pad - 10, 0);
        cb.endText();

        // Price (bottom right of label)
        String price = "LKR " + String.format("%.0f", p.getSellingPrice());
        cb.beginText();
        cb.setFontAndSize(com.lowagie.text.pdf.BaseFont.createFont(
                com.lowagie.text.pdf.BaseFont.HELVETICA_BOLD, com.lowagie.text.pdf.BaseFont.WINANSI,
                com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED), 11);
        cb.setColorFill(PDF_BAR);
        cb.showTextAligned(Element.ALIGN_CENTER, price, imgX + imgW / 2, top - h + pad + 2, 0);
        cb.endText();
    }

    private String clip(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private byte[] toBmp(BufferedImage img) throws Exception {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage crop = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = crop.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(crop, "png", bos);
        return bos.toByteArray();
    }

    private File exportsFile() {
        AppConfig.exportsDir(); // ensure exists
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        return AppConfig.exportsDir().resolve("barcode-labels-" + stamp + ".pdf").toFile();
    }
}
