package com.dns.bookshop.theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A simple panel that paints a solid rounded rectangle background.
 * Useful for sidebar blocks, accent strips, badges and decorative surfaces.
 */
public class RoundedPanel extends JPanel {

    private final int corner;
    private Color fill;

    public RoundedPanel(Color fill, int corner) {
        this.fill = fill;
        this.corner = corner;
        setOpaque(false);
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    @Override
    public Color getBackground() {
        return fill;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, corner, corner));
        g2.dispose();
        super.paintComponent(g);
    }
}
