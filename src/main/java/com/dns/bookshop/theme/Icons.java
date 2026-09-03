package com.dns.bookshop.theme;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Lightweight icon generation using unicode glyphs rendered to images.
 * Keeps the app fully self-contained (no image asset files) while giving
 * navigation a clean, modern icon look that scales with any L&F.
 */
public final class Icons {

    private Icons() {}

    private static final int SIZE = 18;

    // Unicode glyph pool (rendered monochrome so we can tint them freely)
    private static final String GLYPH = "\u25A1"; // placeholder; replaced below per icon

    /** Returns a tinted glyph icon. */
    public static ImageIcon glyph(String glyph, Color color) {
        return glyph(glyph, color, SIZE);
    }

    public static ImageIcon glyph(String glyph, Color color, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(new Font("Segoe UI Symbol", Font.PLAIN, (int) (size * 0.78)));
        g.drawString(glyph, 0, (int) (size * 0.82));
        g.dispose();
        return new ImageIcon(img);
    }

    // ============================== Icon glyphs ==============================
    public static final String DASHBOARD = "\u2302";        // ⌂ house-ish
    public static final String POS       = "\u2211";        // Σ (register)
    public static final String PRODUCTS  = "\u25A6";        // ▦ (box)
    public static final String CUSTOMERS = "\u263A";        // ☺ (person)
    public static final String SALES     = "\u21C4";        // ⇄ (history)
    public static final String REPORTS   = "\u25B2";        // ▲ (chart)
    public static final String USERS     = "\u2699";        // ⚙ (gear)
    public static final String SETTINGS  = "\u2699";        // ⚙ (gear)
    public static final String LOGOUT    = "\u21E5";        // ⇥ (exit)
    public static final String KEY       = "\u2694";        // ⚔ (password)
    public static final String SEARCH    = "\u2315";        // ⌕ (search)

    public static ImageIcon dashboardIcon(Color c) { return glyph(DASHBOARD, c); }
    public static ImageIcon posIcon(Color c) { return glyph(POS, c); }
    public static ImageIcon productsIcon(Color c) { return glyph(PRODUCTS, c); }
    public static ImageIcon customersIcon(Color c) { return glyph(CUSTOMERS, c); }
    public static ImageIcon salesIcon(Color c) { return glyph(SALES, c); }
    public static ImageIcon reportsIcon(Color c) { return glyph(REPORTS, c); }
    public static ImageIcon usersIcon(Color c) { return glyph(USERS, c); }
    public static ImageIcon settingsIcon(Color c) { return glyph(SETTINGS, c); }
    public static ImageIcon logoutIcon(Color c) { return glyph(LOGOUT, c); }
}
