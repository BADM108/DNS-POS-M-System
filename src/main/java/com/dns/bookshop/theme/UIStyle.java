package com.dns.bookshop.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Central visual theme for the DNS BookShop POS application.
 *
 * Design language:
 *  - Deep indigo/navy sidebar for a calm, professional "enterprise" feel.
 *  - Light grey content background for readability and low eye strain.
 *  - Purple accent for interactive/Primary actions, with green/amber/red
 *    used consistently for success/warning/danger.
 *  - Generous whitespace, softer borders and rounded corners throughout.
 */
public final class UIStyle {

    // ============================== Brand / Primary ==============================
    /** Deep indigo used as the primary interactive color. */
    public static final Color PRIMARY = new Color(0x6D28D9);
    public static final Color PRIMARY_DARK = new Color(0x5B21B6);
    public static final Color PRIMARY_LIGHT = new Color(0xEDE9FE);

    // ============================== Semantic ==============================
    public static final Color SUCCESS = new Color(0x10B981);
    public static final Color SUCCESS_DARK = new Color(0x0E9F6E);
    public static final Color DANGER = new Color(0xEF4444);
    public static final Color DANGER_DARK = new Color(0xDC2626);
    public static final Color WARNING = new Color(0xF59E0B);
    public static final Color INFO = new Color(0x3B82F6);

    // ============================== Neutral surfaces ==============================
    public static final Color BG = new Color(0xF4F5F7);          // app content background
    public static final Color SURFACE = new Color(0xFFFFFF);      // cards / panels
    public static final Color SURFACE_MUTED = new Color(0xFAFBFC); // subtle row fills
    public static final Color TEXT = new Color(0x1F2937);         // primary text
    public static final Color TEXT_MUTED = new Color(0x6B7280);   // secondary text
    public static final Color TEXT_FAINT = new Color(0x9CA3AF);   // hints / placeholders
    public static final Color BORDER = new Color(0xE5E7EB);       // input/table borders
    public static final Color DIVIDER = new Color(0xF0F1F3);

    // ============================== Sidebar / Header ==============================
    public static final Color SIDEBAR_BG = new Color(0x111827);          // near-black slate
    public static final Color SIDEBAR_BG_HOVER = new Color(0x1F2937);
    public static final Color SIDEBAR_ACTIVE = new Color(0x6D28D9);
    public static final Color SIDEBAR_TEXT = new Color(0xD1D5DB);
    public static final Color SIDEBAR_TEXT_MUTED = new Color(0x9CA3AF);
    public static final Color HEADER_BG = new Color(0xFFFFFF);
    public static final Color TABLE_HEADER_BG = new Color(0xF9FAFB);

    // ============================== Typography ==============================
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font PAGE_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font NORMAL_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 13);
    public static final Font MONO_BIG = new Font("Consolas", Font.BOLD, 22);
    public static final Font NAV = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font NAV_ACTIVE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font DISPLAY = new Font("Segoe UI", Font.BOLD, 26);

    private UIStyle() {}
}
