package com.dns.bookshop.util;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.JTextField;
import java.util.function.Consumer;

/**
 * Reads input from a barcode scanner.
 *
 * Most USB barcode scanners behave like a keyboard: they type the code very
 * quickly and finish with an Enter. This helper listens to a focused text
 * field and, when it detects a fast, continuous keystroke stream ending in
 * Enter, treats the whole input as a single barcode scan.
 *
 * This avoids the scanner firing on every key and lets you react once per scan.
 */
public final class ScannerInput {

    private ScannerInput() {}

    /**
     * Attach a scan listener to a text field. A "scan" is: characters that
     * arrive quickly (<= 25ms between them) followed by Enter.
     */
    public static void attach(JTextField field, Consumer<String> onScan) {
        StringBuilder buffer = new StringBuilder();
        long lastKeyTime = 0;
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == '\n' || c == '\r') {
                    String code = buffer.toString().trim();
                    if (!code.isEmpty()) {
                        onScan.accept(code);
                    }
                    buffer.setLength(0);
                    field.setText("");
                    e.consume();
                    field.requestFocusInWindow();
                    return;
                }
                if (e.isActionKey() || c == KeyEvent.CHAR_UNDEFINED) {
                    return;
                }
                buffer.append(c);
            }
        });
    }
}
