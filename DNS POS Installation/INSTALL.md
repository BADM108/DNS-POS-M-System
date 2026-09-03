# DNS BookShop POS — Installation Guide

This folder contains **everything needed to install and run the POS system** on a
shop PC. You do **not** need the source code, Maven, or an internet connection after
this is set up.

## What's in this folder

| File | Purpose |
|------|---------|
| `DNS-BookShop-POS.jar` | The application itself (runnable single file). |
| `run.bat` | Launcher for **Windows**. Double-click to run. |
| `run.sh` | Launcher for **Linux / macOS**. Run with `./run.sh`. |
| `INSTALL.md` | This guide. |

---

## Step 1 — Install Java 17 or newer

The **only** software you need is Java (free).

**Windows**
1. Go to https://adoptium.net and download the **Temurin 17 (LTS)** or **21** installer.
2. Run it and accept the defaults (it adds Java to your PATH automatically).
3. Verify: open **Command Prompt** and type `java -version` — you should see a
   version number like `openjdk version "17..."`.

**Linux (Debian/Ubuntu)**
```bash
sudo apt update
sudo apt install openjdk-17-jre
java -version
```

**macOS**
```bash
brew install openjdk@17
java -version
```

> No database server, no Docker, no other dependency, and **no internet** is
> required to run the app once Java is installed.

---

## Step 2 — Put this folder on the shop PC

Copy this entire **`DNS POS Installation`** folder somewhere easy on the shop PC,
for example:
- Windows: `C:\DNS BookShop\`
- Linux: `/home/yourname/DNSBookShop/`

Do **not** separate the `.jar` from the `run.bat`/`run.sh` — they must stay in the
same folder.

---

## Step 3 — Run it

- **Windows:** double-click **`run.bat`**.
- **Linux/macOS:** open a terminal in the folder and run `./run.sh`.

*(Tip: right-click `run.bat` → Send to → Desktop shortcut to make it easy to open.)*

---

## Step 4 — First login & important setup

1. Log in with the default admin account:
   - **Username:** `admin`
   - **Password:** `admin`
2. **Immediately change the admin password** — bottom-left of the main window →
   *Change password*.
3. Plug in your **USB barcode scanner** (they work like a keyboard — no driver needed).
4. Check that your normal **A4 printer** is set as the default printer.

---

## Step 5 — Connect the barcode scanner

Your scanner (e.g. **RL-LSH2500**) works like a USB keyboard — no special driver
needed. Just plug it in and it types the barcode into whichever field is focused.

- **Point of Sale:** click the scan box and scan an item — it appears on the bill
  instantly.
- **Products & Barcodes:** scan a barcode to *instantly open* that product so you
  can view or update it. Scanning an unknown barcode starts registering a brand
  new product with that code. No typing needed.
- **Product with no barcode:** tick *“Auto-generate…”* to create one from the
  product info (books get an ISBN/EAN barcode, stationery gets a Code-128 DNS
  barcode).
- **Export labels as PDF:** in **Products & Barcodes**, use the
  **Export Labels (PDF)** button. It writes an **A4 sheet of barcode labels** (grid
  layout, ready to cut) into the `data/exports` folder and opens it automatically,
  so you can print as many copies as you like or keep the PDF for later.
- **Print one label:** select a product and use **Print Label** to send it
  straight to your printer.

---

## Where is all the data kept? (all inside this folder, no server)

Everything is stored **inside a `data` sub-folder that sits right next to the app**.
This makes the whole system **portable**: copy this entire folder to any shop PC
(or to a backup pen drive) and all your products, sales, settings and labels come
with it.

| Item | Location |
|------|----------|
| Database (SQLite) | `DNS POS Installation/data/dns_bookshop.db` |
| Backups | `DNS POS Installation/data/exports/backup-*.db` |
| Sales CSV exports | `DNS POS Installation/data/exports/sales-*.csv` |
| Barcode label PDFs | `DNS POS Installation/data/exports/barcode-labels-*.pdf` |

**Back up the `data` folder** regularly (or use the app's **Reports → Backup
Database** button) to a pen drive so you never lose records. Because everything
is in one folder, backing up is as simple as copying it.

---

## Troubleshooting

- **"Java not found" when running `run.bat`** → Java is not on PATH. Re-run the Java
  installer, or in `run.bat` replace `java` with the full path to `java.exe`.
- **Bill won't print** → make a normal A4 printer the default printer in Windows
  Settings → Printers.
- **Scanner not adding items** → click on the scan box first, and make sure the
  scanner is plugged in and set to "keyboard emulation" (default on most models).

---

Need the developer/source version or to build from scratch? See the **README.md**
at the repository root.
