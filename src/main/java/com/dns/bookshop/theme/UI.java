package com.dns.bookshop.theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * Factory helpers that produce a consistent, modern look across the whole app.
 * All buttons are rounded, cards are clean white surfaces with subtle borders,
 * and tables use a softer header + zebra striping for readability.
 */
public final class UI {

    public static final int CORNER = 8;

    private UI() {}

    // ============================== Labels ==============================

    public static JLabel title(String text) {
        return styled(text, UIStyle.PAGE_TITLE, UIStyle.TEXT);
    }

    public static JLabel section(String text) {
        return styled(text, UIStyle.SECTION, UIStyle.TEXT);
    }

    public static JLabel label(String text) {
        return styled(text, UIStyle.NORMAL, UIStyle.TEXT);
    }

    public static JLabel label(String text, Color color) {
        return styled(text, UIStyle.NORMAL, color);
    }

    public static JLabel muted(String text) {
        return styled(text, UIStyle.SMALL, UIStyle.TEXT_MUTED);
    }

    private static JLabel styled(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(c);
        return l;
    }

    // ============================== Buttons ==============================

    /** Primary (filled) button - rounded. */
    public static JButton primary(String text) {
        return button(text, UIStyle.PRIMARY, UIStyle.PRIMARY_DARK, Color.WHITE);
    }

    /** Success (filled green) button. */
    public static JButton success(String text) {
        return button(text, UIStyle.SUCCESS, UIStyle.SUCCESS_DARK, Color.WHITE);
    }

    /** Danger (filled red) button. */
    public static JButton danger(String text) {
        return button(text, UIStyle.DANGER, UIStyle.DANGER_DARK, Color.WHITE);
    }

    /** Ghost / secondary button with a border. */
    public static JButton ghost(String text) {
        JButton b = new RoundedButton(text, CORNER);
        b.setForeground(UIStyle.TEXT);
        b.setBackground(UIStyle.SURFACE);
        b.setFont(UIStyle.NORMAL_BOLD);
        b.setBorder(new CompoundBorder(new RoundedBorder(new Color(0xD1D5DB), CORNER),
                new EmptyBorder(8, 16, 8, 16)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(true);
        b.setRolloverEnabled(true);
        return b;
    }

    /** Subtle link-like text button (transparent background, colored text). */
    public static JButton textButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(new Color(0, 0, 0, 0));
        b.setForeground(color);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setFont(UIStyle.NORMAL_BOLD);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton button(String text, Color bg, Color rollover, Color fg) {
        JButton b = new RoundedButton(text, CORNER);
        b.setForeground(fg);
        b.setBackground(bg);
        b.setFont(UIStyle.NORMAL_BOLD);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(true);
        b.setRolloverEnabled(true);
        return b;
    }

    private static final class RoundedButton extends JButton {
        private final int corner;
        RoundedButton(String text, int corner) {
            super(text);
            this.corner = corner;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, corner, corner));
            g2.dispose();
            super.paintComponent(g);
        }
        @Override
        protected void paintBorder(Graphics g) {
            // Let the filled round rectangle act as the shape; suppress default border.
        }
    }

    // ============================== Panels / Cards ==============================

    /** A clean white card with a soft border and rounded corners. */
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, CORNER, CORNER));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setBackground(UIStyle.SURFACE);
        p.setBorder(new CompoundBorder(new RoundedBorder(new Color(0xE5E7EB), CORNER),
                new EmptyBorder(16, 18, 16, 18)));
        p.setOpaque(false);
        return p;
    }

    /** A tinted "metric" card used by the dashboard/reports. */
    public static JPanel metricCard(Color accent) {
        return metricCard(null, accent);
    }

    /**
     * A small rounded accent chip holding a white icon, used to lead metric
     * cards on the dashboard. The chip is sized to the icon for a clean look.
     */
    public static JComponent metricIconChip(String glyph, Color accent, float alignment) {
        int size = 40;
        JLabel chip = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = accent;
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 24));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setIcon(Icons.glyph(glyph, accent, 20));
        chip.setOpaque(false);
        chip.setPreferredSize(new Dimension(size, size));
        chip.setMaximumSize(new Dimension(size, size));
        chip.setMinimumSize(new Dimension(size, size));
        chip.setAlignmentX(alignment);
        return chip;
    }

    public static JPanel metricCard(String label, Color accent) {
        JPanel p = card();
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.Y_AXIS));
        if (label != null) {
            JLabel l = new JLabel(label);
            l.setFont(UIStyle.SMALL_BOLD);
            l.setForeground(accent);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(l);
            p.add(javax.swing.Box.createVerticalStrut(4));
        }
        return p;
    }

    // ============================== Inputs ==============================

    /** A standard text field with a subtle rounded border and focus-friendly padding. */
    public static JTextField textField() {
        return textField(12);
    }

    public static JTextField textField(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(UIStyle.NORMAL);
        f.setForeground(UIStyle.TEXT);
        f.setBackground(UIStyle.SURFACE);
        f.setCaretColor(UIStyle.PRIMARY);
        f.setBorder(new CompoundBorder(new RoundedBorder(new Color(0xD1D5DB), 6),
                new EmptyBorder(7, 10, 7, 10)));
        return f;
    }

    public static JTextField textField(String initialText) {
        JTextField f = textField(12);
        f.setText(initialText);
        return f;
    }

    // ============================== Tables ==============================

    /** Builds a scroll pane around a styled, read-only table. */
    public static JScrollPane table(JTable table) {
        table.setRowHeight(34);
        table.setFont(UIStyle.NORMAL);
        table.setForeground(UIStyle.TEXT);
        table.setBackground(UIStyle.SURFACE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setSelectionBackground(UIStyle.PRIMARY_LIGHT);
        table.setSelectionForeground(UIStyle.PRIMARY_DARK);
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(UIStyle.SMALL_BOLD);
        header.setForeground(UIStyle.TEXT_MUTED);
        header.setBackground(UIStyle.TABLE_HEADER_BG);
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyle.BORDER));

        // Zebra striping + right-align numeric columns handled by caller-specific renderers.

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER));
        sp.getViewport().setBackground(UIStyle.SURFACE);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JScrollBar vsb = sp.getVerticalScrollBar();
        vsb.setUnitIncrement(16);
        vsb.setBackground(UIStyle.SURFACE);
        vsb.setForeground(UIStyle.BORDER);
        return sp;
    }

    /** Applies a centered renderer + zebra striping helper. */
    public static void styleTableWithZebra(JTable table) {
        table.setDefaultRenderer(Object.class, new ZebraRenderer());
    }

    public static void alignColumn(JTable table, int col, int alignment) {
        TableColumnModel m = table.getColumnModel();
        if (col < m.getColumnCount()) {
            DefaultTableCellRenderer r = new DefaultTableCellRenderer();
            r.setHorizontalAlignment(alignment);
            r.setOpaque(false);
            m.getColumn(col).setCellRenderer(r);
        }
    }

    private static final class ZebraRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setFont(UIStyle.NORMAL);
            if (!selected) {
                c.setBackground(row % 2 == 0 ? UIStyle.SURFACE : UIStyle.SURFACE_MUTED);
                c.setForeground(UIStyle.TEXT);
            } else {
                c.setBackground(UIStyle.PRIMARY_LIGHT);
                c.setForeground(UIStyle.PRIMARY_DARK);
            }
            return c;
        }
    }

    // ============================== Rounded border helper ==============================

    /** Draws a rounded rectangle border for cards and inputs. */
    public static final class RoundedBorder implements Border {
        private final Color color;
        private final int radius;
        public RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) {
            int pad = (int) (radius * 0.5) + 1;
            return new java.awt.Insets(pad, pad, pad, pad);
        }
        @Override
        public boolean isBorderOpaque() { return false; }
    }

    // ============================== Global L&F polish ==============================

    /** Applies a cohesive look-and-feel across all Swing components. */
    public static void applyGlobalTheme() {
        UIManager.put("nimbusBase", UIStyle.PRIMARY);
        UIManager.put("Control", UIStyle.SURFACE);
        UIManager.put("Control.highlight", UIStyle.SURFACE);
        UIManager.put("nimbusFocus", UIStyle.PRIMARY_LIGHT);
        UIManager.put("OptionPane.background", UIStyle.SURFACE);
        UIManager.put("Panel.background", UIStyle.BG);
        UIManager.put("Table.background", UIStyle.SURFACE);
        UIManager.put("TableHeader.background", UIStyle.TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground", UIStyle.TEXT_MUTED);
        UIManager.put("TableHeader.font", UIStyle.SMALL_BOLD);
        UIManager.put("ScrollBar.thumb", UIStyle.BORDER);
        UIManager.put("ScrollBar.track", UIStyle.SURFACE);
    }
}
