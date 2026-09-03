package com.dns.bookshop.services;

import com.dns.bookshop.config.AppConfig;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates barcodes for products.
 *
 * - If the item is a book, we generate an ISBN-13 (EAN-13) barcode.
 * - For other items, we generate a Code128 barcode to allow arbitrary
 *   alphanumeric content and compact, scannable output.
 *
 * Any generated value is guaranteed unique in stock (checked against the DB
 * by the caller when persisting).
 */
public class BarcodeService {

    public static final boolean IS_BOOK_PREFIX = true;

    /**
     * Generates a unique numeric EAN-13-style barcode (used for ISBN books).
     * Uses a valid GS1 prefix for books (978/979) plus a random unique body
     * and a computed EAN-13 check digit, which ensures real scanners read it.
     */
    public String generateBookISBN() {
        Random rnd = new Random();
        String base;
        while (true) {
            int prefix = rnd.nextBoolean() ? 978 : 979;
            StringBuilder sb = new StringBuilder(13);
            sb.append(prefix);
            for (int i = 0; i < 9; i++) {
                sb.append(rnd.nextInt(10));
            }
            base = sb.toString(); // 12 digits now
            String ean = base + eanChecksum(base);
            // Reject if it accidentally collides with an existing product barcode.
            if (com.dns.bookshop.db.Database.getInstance().getProductByBarcode(ean) == null) {
                return ean;
            }
        }
    }

    /**
     * Generates a unique alphanumeric barcode for non-book (stationery) items.
     * Prefix DNS + random digits => scannable via Code128.
     */
    public String generateItemBarcode() {
        Random rnd = new Random();
        while (true) {
            String code = "DNS" + String.format("%09d", Math.abs(rnd.nextLong() % 1_000_000_000L));
            if (com.dns.bookshop.db.Database.getInstance().getProductByBarcode(code) == null) {
                return code;
            }
        }
    }

    /**
     * Renders a barcode to a BufferedImage. Detects numeric EAN-13 vs Code128.
     */
    public BufferedImage renderBarcode(String content, int width, int height) {
        BitMatrix matrix;
        boolean isEan = content != null && content.matches("\\d{13}");
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 0);
        try {
            if (isEan) {
                try {
                    matrix = new EAN13Writer().encode(content, BarcodeFormat.EAN_13, width, height);
                } catch (IllegalArgumentException eanError) {
                    // A 13-digit code that fails the EAN checksum (e.g. an
                    // arbitrary supplier number) is still scannable as Code128.
                    matrix = new Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height, hints);
                }
            } else {
                matrix = new Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height, hints);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not encode barcode: " + content, e);
        }
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (matrix.get(x, y)) {
                    img.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        g.dispose();
        return img;
    }

    /**
     * Saves a rendered barcode PNG to the barcodes directory and returns the File.
     */
    public File saveBarcodePng(String content, String label) {
        BufferedImage img = renderBarcode(content, 500, 180);
        File file = AppConfig.barcodesDir().resolve(label + ".png").toFile();
        try {
            ImageIO.write(img, "png", file);
        } catch (IOException e) {
            throw new RuntimeException("Could not save barcode image", e);
        }
        return file;
    }

    /**
     * Renders a QR code (e.g. for item linking) - useful for customer-facing labels.
     */
    public BufferedImage renderQr(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    img.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            return img;
        } catch (WriterException e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }

    private String eanChecksum(String twelveDigits) {
        int sum = 0;
        for (int i = 0; i < twelveDigits.length(); i++) {
            int d = twelveDigits.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        int check = (10 - (sum % 10)) % 10;
        return String.valueOf(check);
    }
}
