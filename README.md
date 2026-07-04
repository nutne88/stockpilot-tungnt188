# StockPilot — Inventory & Order Management System

A console (CLI) inventory and order-management backend for a small distribution
company, built for the JSE-LA-01 long assignment. Staff can manage products
and customers, place orders with discount rules, get sales analytics, import
a product catalog from CSV, export invoices/reports to files, and run a
concurrent flash-sale simulation that proves stock is never oversold.

Built with plain **Java SE 17+** and **JDBC** — no Spring, no ORM, no GUI
framework. All persistence, transactions, and threading are hand-written.

---

## 1. Tech stack

| Concern              | Choice                                                        |
|-----------------------|----------------------------------------------------------------|
| Language / build      | Java 17, Maven                                                 |
| Database              | H2 (file mode, embedded — no separate server needed)           |
| Persistence            | Raw JDBC (`PreparedStatement`, manual transactions)             |
| Testing               | JUnit 5                                                        |
| Packaging             | `maven-shade-plugin` → single runnable fat JAR                 |

No Spring / Hibernate / any DI or ORM framework, and no GUI framework, per the
assignment's constraints.

---

## 2. Project structure

```
src/main/java/vn/edu/fpt/
├── Main.java                  # entry point + CLI menu loop (only class that touches System.in/out)
├── model/                     # Product, Customer, Order, OrderItem
├── repository/                # DAO layer — JDBC (PreparedStatement, try-with-resources) lives ONLY here
│   ├── Repository.java        # generic interface Repository<T, ID>
│   ├── ProductRepository.java
│   ├── CustomerRepository.java
│   └── OrderRepository.java   # order placement transaction (commit/rollback)
├── service/                   # business logic — no SQL here
│   ├── ProductService.java
│   ├── CustomerService.java
│   ├── OrderService.java      # cart validation, locking, discount application
│   ├── ReportService.java     # Stream-based analytics (F4)
│   └── pricing/
│       ├── PricingRule.java       # @FunctionalInterface used for lambdas
│       ├── DiscountPolicy.java    # abstract class implementing PricingRule
│       ├── NoDiscount.java
│       ├── PercentageDiscount.java
│       └── BulkDiscount.java
├── io/                         # File I/O — CSV import, invoice/report export
│   ├── CsvProductImporter.java
│   ├── InvoiceExporter.java
│   └── ReportExporter.java
├── concurrent/                 # F6 — flash-sale simulation
│   ├── StockLockManager.java   # one ReentrantLock per product id
│   └── FlashSaleSimulator.java # runUnsafe() vs runSafe()
├── exception/                  # ProductNotFoundException, InsufficientStockException,
│                                # InvalidInputException, DataAccessException
└── util/
    └── DatabaseConnection.java # connection factory + schema.sql loader

src/main/resources/
├── schema.sql                  # DDL for all 4 tables, executed on startup
└── products.csv                # sample catalog for the CSV-import feature

src/test/java/vn/edu/fpt/
└── AppTest.java                # 12 JUnit 5 tests on service-layer behaviour
```

---

## 3. Setup & how to run

### Prerequisites
- JDK 17 or newer
- Maven 3.8+

No separate database server is needed — H2 runs embedded and stores its data
as a file under `./data/` (created automatically on first run).

### Build
```bash
mvn clean package
```
This compiles the code, runs the JUnit 5 test suite (build fails if a test
fails), and packages a runnable **fat JAR** via `maven-shade-plugin`.

### Run
```bash
java -jar target/stockpilot-1.0.0.jar
```
On startup, `DatabaseConnection.initializeDatabase()` reads
`src/main/resources/schema.sql` from the classpath and executes it
(`CREATE TABLE IF NOT EXISTS ...`), so the database is ready even on a
completely fresh checkout. You'll then see the main menu:

```
1. Product Management
2. Customer Management
3. Place Order
4. Reports & Analytics
5. Flash Sale Simulation (F6 demo)
6. Exit System
```

To try the CSV import feature, use the sample catalog at
`src/main/resources/products.csv` (or `products.csv` in the project root)
from the **Product Management → Import from CSV** menu.

Invoices and sales reports exported from the app are written to an
`output/` folder created next to the JAR.

### Run the tests only
```bash
mvn test
```
Tests run against an isolated **in-memory** H2 instance
(`jdbc:h2:mem:stockpilot_apptest`), so they never touch the file-mode
database used by the real app, and each test starts from a clean, reset
table state.

---

## 4. Database schema

Four tables, defined in `src/main/resources/schema.sql`:

**`products`** — the catalog.
`id` (PK), `sku` (unique, validated against `^[A-Z]{3}-\d{4}$`), `name`,
`category`, `price`, `stock_quantity`.

**`customers`** — retail customers who place orders.
`id` (PK), `name`, `email` (unique, regex-validated), `phone` (regex-validated).

**`orders`** — one row per placed order.
`id` (PK), `customer_id` (FK → customers), `order_date`, `subtotal`,
`discount_amount`, `total_amount`.

**`order_items`** — the line items of an order.
`id` (PK), `order_id` (FK → orders, `ON DELETE CASCADE`), `product_id`
(FK → products), `quantity`, `unit_price`.

Placing an order is one JDBC transaction (`OrderRepository.save`): insert the
order header, then for each item check stock, decrement it, and insert the
item row — all on the same `Connection` with `setAutoCommit(false)`. If any
item fails the stock check, the whole transaction rolls back, so a
partially-failed order never leaves stock decremented or a half-written order
behind.

---

## 5. Race-condition write-up (F6)

**The scenario:** many customers try to buy the last few units of the same
flash-sale product at the same time.

**The bug (`FlashSaleSimulator.runUnsafe`):** decrementing stock is a
*"check-then-act"* operation done as two separate SQL statements —
`SELECT stock_quantity ...` to read the current value, then
`UPDATE products SET stock_quantity = ...` to write the new one
(`ProductRepository.decrementStockCheckThenAct`). Without any
synchronization, two threads can both execute the `SELECT` before either one
executes its `UPDATE`. Both threads read the same stock value, both believe
there's enough left, and both proceed to subtract from it — the product ends
up **oversold**, because the second thread's decrement was computed from a
value that was already stale by the time it wrote.

**The fix (`FlashSaleSimulator.runSafe`):** a `java.util.concurrent.locks.
ReentrantLock` is kept **per product id** in `StockLockManager`. Before a
thread may check-and-decrement a product's stock, it must acquire that
product's lock, and it only releases the lock after the read-then-write pair
has completed. This serializes every check-then-decrement for the same
product — no two threads can interleave their read and write — so the race
window is closed and the "safe" run never oversells, no matter how many
threads (`ExecutorService`) hit it concurrently.

The same locking strategy is reused for real order placement in
`OrderService.placeOrder`, which locks every product touched by the cart
(in a fixed order, by product id, to avoid deadlocks) before checking stock
and saving the order.

You can see both runs side by side from the CLI: **main menu → 5. Flash Sale
Simulation** lets you run the *unsafe* version first (it will visibly oversell
against a small stock count) and then the *safe* version on the same
starting stock (it correctly rejects the orders that don't fit).

---

## 6. Feature checklist

### Pass tier
- [x] `mvn clean package` builds; `java -jar target/stockpilot-1.0.0.jar` runs
- [x] OOP domain model with encapsulation (`Product`, `Customer`, `Order`,
      `OrderItem`); `DiscountPolicy` is an abstraction used polymorphically
- [x] JDBC persistence: products, customers, and orders are saved and
      reloaded from the DB
- [x] Place-order flow: stock check + decrement + order saved
- [x] Custom exceptions (`ProductNotFoundException`, `InsufficientStockException`,
      `InvalidInputException`, `DataAccessException`); CLI never crashes on them
- [x] Regex validation for SKU / email / phone
- [x] Generic `Repository<T, ID>` (`save`, `findAll`, `findById`, `update`,
      `deleteById`), implemented by `ProductRepository`, `CustomerRepository`,
      and `OrderRepository`

### Good tier
- [x] Place-order is a real transaction: commit on success, rollback on any
      failure, decrement is atomic with the order/item inserts
- [x] Stream-based reports (`ReportService`): top-N best sellers, revenue by
      category, low-stock alert — built with `flatMap` / `groupingBy` /
      `summingLong` / `reducing` / `sorted().limit()`, no SQL aggregation
- [x] CSV import (`CsvProductImporter`) with per-line validation and error
      collection; invoice export and report export to `output/` files
- [x] Lambdas + functional interface: `PricingRule` (`@FunctionalInterface`),
      implemented polymorphically by `NoDiscount` / `PercentageDiscount` /
      `BulkDiscount`, and also usable directly as ad-hoc lambdas; `Comparator`
      lambdas used to sort products (by price, name) and orders (by date)

### Excellent tier
- [x] Concurrent flash-sale simulation (`ExecutorService` + `ReentrantLock`
      per product) with correct synchronization — no oversell — plus the
      race-condition write-up above
- [x] Meaningful JUnit 5 test suite (12 tests) covering discount calculation,
      `InsufficientStockException` on insufficient stock, transactional
      rollback across multiple items, and all three Stream-based reports
- [x] Clean layered architecture: SQL only in `repository`, no business logic
      in `Main` (menu/printing only), business rules only in `service`
- [x] `BigDecimal` used for all money fields and calculations; rollback
      behaviour is verified by `testPlaceOrderIsAtomicAcrossMultipleItems`
- [x] This README (setup/run, schema, race-condition write-up, checklist)

---

## 7. Notes

- The H2 database file lives under `./data/` and exported files under
  `./output/` — both are git-ignored; delete `./data/` if you want to start
  from a completely empty database.
- `src/main/resources/products.csv` is a ready-to-use sample catalog for the
  import feature; every SKU in it matches the required `^[A-Z]{3}-\d{4}$`
  format.