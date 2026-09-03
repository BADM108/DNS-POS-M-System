package com.dns.bookshop.theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

/**
 * Factory helpers for a consistent look across the app.
 */
public final class UI {

    private UI() {}

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIStyle.TITLE);
        l.setForeground(UIStyle.TEXT);
        return l;
    }

    public static JLabel section(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIStyle.SECTION);
        l.setForeground(UIStyle.PRIMARY);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIStyle.NORMAL);
        l.setForeground(UIStyle.TEXT);
        return l;
    }

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(UIStyle.PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    public static JButton success(String text) {
        JButton b = new JButton(text);
        b.setBackground(UIStyle.SUCCESS);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    public static JButton danger(String text) {
        JButton b = new JButton(text);
        b.setBackground(UIStyle.DANGER);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    public static JButton ghost(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.WHITE);
        b.setForeground(UIStyle.TEXT);
        b.setFocusPainted(false);
        b.setFont(UIStyle.NORMAL);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER));
        return b;
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        return p;
    }

    public static JScrollPane table(JTable table) {
        table.setRowHeight(28);
        table.setFont(UIStyle.NORMAL);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(UIStyle.TABLE_HEADER);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(UIStyle.ACCENT);
        table.setSelectionForeground(Color.WHITE);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER));
        return sp;
    }
}
