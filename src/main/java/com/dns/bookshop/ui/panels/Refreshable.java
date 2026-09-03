package com.dns.bookshop.ui.panels;

/**
 * Panels implement this to reload their data whenever they become visible,
 * so data is always up to date.
 */
public interface Refreshable {
    void refresh();
}
