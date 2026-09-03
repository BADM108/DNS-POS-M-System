# DNS BookShop POS (Point of Sale) System

A complete, industrial **desktop Point of Sale application** for **DNS BookShop**
(books & stationery). Built in **Java (Swing)**, stores all data **locally on the shop
PC** using SQLite — **no internet, no server, no hosting** needed.

![Status](https://img.shields.io/badge/build-passing-brightgreen)

---

## Features

### 🛒 Point of Sale (Scanner)
- **Barcode scanner ready** — just point a USB barcode reader at the scan box; it
  detects scans and adds items to the bill automatically (no key presses needed).
- Items are added to a live cart; the **subtotal and total update instantly** as
  you scan.
- Pick the customer (or walk-in), apply a discount, choose CASH/CARD payment.
- Prints a **professional A4-size bill** on any normal A4 printer.
- "Reprint Bill" for any past sale.

### 🏷️ Products & Barcodes
- **Register an item by scanning its existing barcode** (e.g. a book that already
  has an ISBN) — toggle *"Scan existing barcode"*, scan, fill the details, save.
- **Generate a barcode for any product** that doesn't have one:
  - Books → generates a valid **ISBN (EAN-13)** barcode.
  - Stationery → generates a **Code-128** barcode (DNS + digits).
- **Print barcode labels** (A4 sheet with 24 labels) to stick on items/shelves.
- Stock tracking with **low-stock warnings**.
- The same book type shares the same barcode — register it once, sell it many times.

### 👥 Users & Permissions
- **Admin** has full access.
- Admin can **add/remove workers** and **grant or remove any single permission**:
  make sales, add/edit/delete products, generate barcodes, view customers,
  add customers (admin-only by default), refunds, reports, stock, backups, etc.
- Admins get the **"Add customer"** and **"Workers"** screens; workers don't
  (unless permission is granted).

### 👤 Customers
- Manage a customer list (add, edit, delete, search). Adding customers is
  **admin-only by default** but can be granted to workers.

### 📊 Sales, Reports & Backup
- Full **sales history** with item breakdown, filters by day/week/month/all.
- **Refunds** (stock is returned automatically to inventory).
- Dashboard with revenue, sales count, low-stock and recent sales.
- **Reports**: top products, revenue summaries.
- **Backup & export**: one-click database backup (`.db` file) and export sales to
  CSV — all saved inside the app's data folder on the same PC.

---

## How to set it up on the shop PC

### 1. Install Java
The only requirement is **Java 17 or newer** (free).

- **Windows:** Download from https://adoptium.net (Eclipse Temurin 17/21 LTS) and
  install. Ensure `java -version` works in a Command Prompt.
- **Linux:** `sudo apt install openjdk-17-jre` (Debian/Ubuntu) or your distro's
  package.
- **macOS:** `brew install openjdk@17`.

> No other software, database server, or internet connection is required.

### 2. Get the application (easiest)
> **Use the ready-made install package.** Everything you need is already in the
> **`DNS POS Installation/`** folder in this repository:
> - `DNS-BookShop-POS.jar`  (the application)
> - `run.bat` (Windows launcher) and `run.sh` (Linux/Mac launcher)
> - `INSTALL.md` (step-by-step shop-PC guide)

Copy that whole **`DNS POS Installation`** folder to the shop PC (e.g.
`C:\DNS BookShop\` or `~/DNSBookShop/`) and follow its `INSTALL.md`.

**Alternatively – build from source:**
```bash
mvn clean package
# Produces: target/DNS-BookShop-POS.jar
```
Then `cp target/DNS-BookShop-POS.jar "DNS POS Installation/"` to refresh the install
package with your latest build.

### 3. Run it
- **Windows:** double-click `run.bat` in the install folder (or
  `java -jar DNS-BookShop-POS.jar`).
- **Linux/Mac:** `./run.sh` (or `java -jar DNS-BookShop-POS.jar`).
- Optional: create a desktop shortcut / add to startup so it opens on boot.

### 4. First login
- **Username:** `admin`
- **Password:** `admin`
- **IMPORTANT:** Change the admin password right away
  (bottom-left of the main window → *Change password*).

### 5. Connect the barcode scanner
- Most USB barcode scanners are plug-and-play and act like a keyboard.
- Plug in, then click on the **scan box** in the POS screen and scan an item.
- In **Products & Barcodes**, to register by scanning, tick *"Scan existing
  barcode"* and scan.

### 6. Set up printing (A4)
- Make sure a normal A4 printer is installed and set as default.
- When you complete a sale, choose **Print A4 bill** — the system opens the
  Windows/Linux print dialog. Choose your A4 printer and print.
- Barcode labels print as an A4 sheet with 24 stick-on labels.

---

## Where is the data stored? (all local)

| Item | Location (created automatically on first run) |
|------|----------------|
| Database (SQLite) | `<Home>/DNSBookShop/data/dns_bookshop.db` |
| Backups | `<Home>/DNSBookShop/data/exports/backup-*.db` |
| Sales CSV exports | `<Home>/DNSBookShop/data/exports/sales-*.csv` |
| Barcode images | `<Home>/DNSBookShop/data/barcodes/` |

Everything is on the PC. **Back up the `dns_bookshop.db` file** (or use the
app's *Backup Database* button) to a pen drive / cloud to prevent loss.

---

## How to build / run for development

```bash
mvn clean package          # build
mvn -DskipTests package    # build (skip tests)
java -jar target/DNS-BookShop-POS.jar   # run
```

### Tests
```bash
mvn test-compile
# then run the headless functional test:
java -cp "target/classes:target/test-classes:$(find ~/.m2 -name 'sqlite-jdbc*.jar'|head -1)" com.dns.bookshop.FunctionalTest
```

The build includes:
- `FunctionalTest` — end-to-end data/service checks (auth, permissions, products,
  barcodes, sales, refunds, stock).
- `UiSmokeTest` — builds every screen to catch errors.
- `BillRenderTest` — renders the A4 bill and barcode label.

---

## Project structure

```
src/main/java/com/dns/bookshop/
├── Main.java                 # entry point
├── config/AppConfig.java     # paths & shop constants
├── db/
│   ├── Database.java         # SQLite schema + seed
│   └── repositories/         # DAOs (users, products, sales, customers, audit)
├── models/                   # User, Product, Customer, Sale, SaleItem, Permissions
├── services/                 # Auth, Product, Sale, Barcode, Report, Backup
├── theme/                    # UIStyle + UI helpers
└── ui/
    ├── LoginFrame.java
    ├── MainFrame.java        # nav + shell
    └── panels/               # Dashboard, POS, Products, Customers, Users, Sales, Reports, Settings
```

---

## Tech stack
- **Java 17+** / Swing (UI)
- **SQLite** via `sqlite-jdbc` (embedded, local, no server)
- **ZXing** (barcode generation: EAN-13/ISBN, Code-128)
- **Maven** (build) → single runnable fat jar

## Branches
- `main` — stable release
- Feature branches created as needed during development.

---

*Made for DNS BookShop — books & stationery.*
