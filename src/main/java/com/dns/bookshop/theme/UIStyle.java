package com.dns.bookshop.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Central visual theme and reusable styling constants.
 */
public final class UIStyle {

    public static final Color PRIMARY = new Color(0x1f3a93);
    public static final Color PRIMARY_DARK = new Color(0x14264f);
    public static final Color ACCENT = new Color(0x2e86de);
    public static final Color SUCCESS = new Color(0x27ae60);
    public static final Color DANGER = new Color(0xe74c3c);
    public static final Color WARNING = new Color(0xe67e22);
    public static final Color BG = new Color(0xf5f6fa);
    public static final Color TEXT = new Color(0x2d3436);
    public static final Color MUTED = new Color(0x7f8c8d);
    public static final Color BORDER = new Color(0xdfe4ea);
    public static final Color HEADER_BG = new Color(0x2c3e50);
    public static final Color TABLE_HEADER = new Color(0x2c3e50);

    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 20);
    public static final Font SECTION = new Font("SansSerif", Font.BOLD, 16);
    public static final Font NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font MONO = new Font("Monospaced", Font.PLAIN, 13);
    public static final Font MONO_BIG = new Font("Monospaced", Font.BOLD, 22);

    private UIStyle() {}
}
