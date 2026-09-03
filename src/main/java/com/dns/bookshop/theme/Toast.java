package com.dns.bookshop.theme;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.RoundRectangle2D;

/**
 * Lightweight, non-blocking toast notification shown at the bottom of a window.
 * A clean, unobtrusive alternative to JOptionPane for confirmation / info messages.
 */
public final class Toast {

    private Toast() {}

    public static void show(Window owner, String message, Color background) {
        show(owner, message, background, 2600);
    }

    public static void show(String message, Color background) {
        show(null, message, background, 2600);
    }

    public static void showInfo(Window owner, String message) {
        show(owner, message, UIStyle.PRIMARY);
    }

    public static void showSuccess(Window owner, String message) {
        show(owner, message, UIStyle.SUCCESS);
    }

    public static void showError(Window owner, String message) {
        show(owner, message, UIStyle.DANGER);
    }

    public static void showWarning(Window owner, String message) {
        show(owner, message, UIStyle.WARNING);
    }

    public static void show(Window owner, String message, Color background, int durationMs) {
        SwingUtilities.invokeLater(() -> {
            Window anchor = owner;
            if (anchor == null) {
                for (Window w : Window.getWindows()) {
                    if (w instanceof Frame && w.isVisible()) { anchor = w; break; }
                }
            }
            if (anchor == null) return;

            JDialog toast = new JDialog(anchor, JDialog.ModalityType.MODELESS);
            toast.setUndecorated(true);
            toast.setAlwaysOnTop(true);

            JLabel label = new JLabel(message);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setForeground(Color.WHITE);
            label.setOpaque(false);
            toast.setContentPane(new ToastSurface(label, background));
            toast.pack();

            int margin = 28;
            int x = anchor.getX() + (anchor.getWidth() - toast.getWidth()) / 2;
            int y = anchor.getY() + anchor.getHeight() - toast.getHeight() - margin;
            toast.setLocation(x, y);
            toast.setVisible(true);

            Timer timer = new Timer(durationMs, e -> {
                toast.dispose();
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    private static final class ToastSurface extends JComponent {
        private final JLabel label;
        private final Color fill;
        ToastSurface(JLabel label, Color fill) {
            this.label = label;
            this.fill = fill;
            setOpaque(false);
            setLayout(new java.awt.BorderLayout());
            setBorder(new javax.swing.border.EmptyBorder(12, 22, 12, 22));
            add(label, java.awt.BorderLayout.CENTER);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
