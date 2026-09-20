# 📘 Expense Tracker - Feature Implementation Directory

This directory details **every feature in the Expense Tracker application**, specifying the **Main Package Name**, **File Paths & Classes**, **Libraries Used**, and **In-Depth Technical Explanations with Code Snippets**.

---

## 🗂️ Package Architecture Overview

```text
com.example.expensetracker                   <-- Main UI & Activity Package
├── com.example.expensetracker.database      <-- Local Room ORM Persistence Layer
├── com.example.expensetracker.sync          <-- Jetpack WorkManager Background Tasks
└── com.example.expensetracker.utils         <-- OCR RegEx Parsing Helpers
```

---

## 📋 Table of Features

1. [Biometric App Lock & Authentication](#1-biometric-app-lock--authentication)
2. [User Sign-Up & Password Policy Enforcement](#2-user-sign-up--password-policy-enforcement)
3. [User Login & Session Persistence](#3-user-login--session-persistence)
4. [Dashboard Totals, Balance & Monthly Budget Health Bar](#4-dashboard-totals-balance--monthly-budget-health-bar)
5. [Live Search & Category Filter Dialog](#5-live-search--category-filter-dialog)
6. [Transaction RecyclerView Feed & Adapter Actions](#6-transaction-recyclerview-feed--adapter-actions)
7. [Single Transaction Creation, Modification & Deletion](#7-single-transaction-creation-modification--deletion)
8. [Camera Receipt Scanning & Image Enhancement (OCR)](#8-camera-receipt-scanning--image-enhancement-ocr)
9. [Smart RegEx Text Parsing Engine](#9-smart-regex-text-parsing-engine)
10. [Bulk Gallery Receipt OCR Batch Processing](#10-bulk-gallery-receipt-ocr-batch-processing)
11. [Financial Analytics & MPAndroidChart Visualizations](#11-financial-analytics--mpandroidchart-visualizations)
12. [Financial Advice Insights & Activity Heatmap Grid](#12-financial-advice-insights--activity-heatmap-grid)
13. [Monthly Budget Target Setup](#13-monthly-budget-target-setup)
14. [A4 PDF Financial Report Generation & Sharing](#14-a4-pdf-financial-report-generation--sharing)
15. [Background Weekly Cloud Sync (Firestore)](#15-background-weekly-cloud-sync-firestore)
16. [Background Daily Recurring Transaction Worker](#16-background-daily-recurring-transaction-worker)
17. [Room ORM Database Architecture & App Security](#17-room-orm-database-architecture--app-security)

---

## 1. Biometric App Lock & Authentication

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/SplashActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/SplashActivity.java) $\rightarrow$ `SplashActivity`
  * [`app/src/main/res/layout/activity_splash.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/activity_splash.xml)
* **Libraries / APIs Used:** `androidx.biometric:biometric:1.4.0-alpha07` (`BiometricPrompt`), `SharedPreferences`
* **Implementation Details:**
  * Displays a 2-second launch screen, then checks if a Firebase user session is active (`mAuth.getCurrentUser() != null`).
  * If authenticated, checks `SharedPreferences` key `"biometric_enabled"`. If `true`, instantiates `BiometricPrompt` and displays system fingerprint/face prompt before navigating to `MainActivity`.

```java
// SplashActivity.java
boolean biometricEnabled = getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("biometric_enabled", false);
if (biometricEnabled) {
    BiometricPrompt biometricPrompt = new BiometricPrompt(SplashActivity.this, executor, 
        new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                navigateToMain();
            }
        });

    BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Expense Tracker")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build();

    biometricPrompt.authenticate(promptInfo);
}
```

---

## 2. User Sign-Up & Password Policy Enforcement

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/RegisterActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/RegisterActivity.java) $\rightarrow$ `RegisterActivity`
  * [`app/src/main/res/layout/activity_register.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/activity_register.xml)
* **Libraries / APIs Used:** `com.google.firebase:firebase-auth` (`FirebaseAuth`, `FirebaseUser`, `UserProfileChangeRequest`), `Patterns.EMAIL_ADDRESS`
* **Implementation Details:**
  * Validates inputs against strict RegEx policy: length $\ge 6$, uppercase (`A-Z`), lowercase (`a-z`), digit (`0-9`), and special character (`!@#$%^&*()_+-=[]{};':"|,.<>/?`).
  * Calls `mAuth.createUserWithEmailAndPassword()` and attaches display name using `UserProfileChangeRequest`.

```java
// RegisterActivity.java
if (!password.matches(".*[A-Z].*")) {
    etPassword.setError("Password must contain at least one uppercase letter (A-Z)");
    return;
}
if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
    etPassword.setError("Password must contain at least one special character (!@#$%^&*)");
    return;
}

mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
    if (task.isSuccessful()) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());
        }
        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
        finish();
    }
});
```

---

## 3. User Login & Session Persistence

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/LoginActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/LoginActivity.java) $\rightarrow$ `LoginActivity`
  * [`app/src/main/res/layout/activity_login.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/activity_login.xml)
* **Libraries / APIs Used:** `com.google.firebase:firebase-auth` (`signInWithEmailAndPassword`)
* **Implementation Details:**
  * Validates email format and non-empty password fields.
  * Authenticates credentials with Firebase Auth server, toggling a loading `ProgressBar` and handling authentication failures cleanly.

```java
// LoginActivity.java
mAuth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener(task -> {
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        if (task.isSuccessful()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication Failed";
            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }
    });
```

---

## 4. Dashboard Totals, Balance & Monthly Budget Health Bar

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/MainActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/MainActivity.java) $\rightarrow$ `MainActivity`
  * [`app/src/main/res/layout/activity_main.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/activity_main.xml)
* **Libraries / APIs Used:** `com.google.android.material.progressindicator.LinearProgressIndicator`, Room ORM (`TransactionDao`)
* **Implementation Details:**
  * Reads income and expense totals for the current user ID (`uid`) asynchronously via an `ExecutorService`.
  * Computes Net Balance (`income - expense`) and Savings Rate percentage (`(balance / income) * 100`).
  * Calculates current month spending against active `Budget` limit and updates `LinearProgressIndicator` (`budgetProgress`).

```java
// MainActivity.java -> loadDashboard()
Double income = transactionDao.getTotalIncome(uid);
Double expense = transactionDao.getTotalExpense(uid);
double balance = (income != null ? income : 0.0) - (expense != null ? expense : 0.0);

if (finalTotalBudget > 0) {
    int progress = (int) ((finalMonthExpense / finalTotalBudget) * 100);
    budgetProgress.setProgress(Math.min(progress, 100));
    tvBudgetText.setText(String.format(Locale.getDefault(), "₹%.2f spent of ₹%.2f", finalMonthExpense, finalTotalBudget));
}
```

---

## 5. Live Search & Category Filter Dialog

* **Main Package:** `com.example.expensetracker` & `com.example.expensetracker.database`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/MainActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/MainActivity.java) $\rightarrow$ `MainActivity`
  * [`app/src/main/java/com/example/expensetracker/database/TransactionDao.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/TransactionDao.java) $\rightarrow$ `TransactionDao`
* **Libraries / APIs Used:** `androidx.appcompat.widget.SearchView`, `MaterialAlertDialogBuilder`
* **Implementation Details:**
  * `SearchView` positioned directly above "Recent Transactions" section triggers real-time Room SQL `LIKE` queries (`searchTransactions`).
  * `btnFilter` opens a dialog with categories (`Food`, `Travel`, `Shopping`, `Bills`, etc.) triggering `filterByCategory`.

```java
// MainActivity.java
searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
    @Override
    public boolean onQueryTextChange(String newText) {
        currentSearchQuery = newText;
        loadDashboard();
        return true;
    }
});

// TransactionDao.java
@Query("SELECT * FROM transactions WHERE userId = :uid AND category = :category ORDER BY id DESC")
List<Transaction> filterByCategory(String uid, String category);

@Query("SELECT * FROM transactions WHERE userId = :uid AND description LIKE :query ORDER BY id DESC")
List<Transaction> searchTransactions(String uid, String query);
```

---

## 6. Transaction RecyclerView Feed & Adapter Actions

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/TransactionAdapter.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/TransactionAdapter.java) $\rightarrow$ `TransactionAdapter`
  * [`app/src/main/res/layout/item_transaction.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/item_transaction.xml)
* **Libraries / APIs Used:** `androidx.recyclerview.widget.RecyclerView`
* **Implementation Details:**
  * Binds transaction date, description, category, and formatted amount (`+ ₹` in green for Income, `- ₹` in red for Expense).
  * Edit button passes `transaction_id` intent extra to `AddTransactionActivity`. Delete button triggers `OnDeleteClickListener`.

```java
// TransactionAdapter.java
if ("Income".equalsIgnoreCase(transaction.getType())) {
    holder.tvAmount.setText("+ " + amount);
    holder.tvAmount.setTextColor(Color.rgb(22, 128, 60));
} else {
    holder.tvAmount.setText("- " + amount);
    holder.tvAmount.setTextColor(Color.rgb(211, 47, 47));
}
```

---

## 7. Single Transaction Creation, Modification & Deletion

* **Main Package:** `com.example.expensetracker` & `com.example.expensetracker.database`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/AddTransactionActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/AddTransactionActivity.java) $\rightarrow$ `AddTransactionActivity`
  * [`app/src/main/res/layout/activity_add_transaction.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/activity_add_transaction.xml)
* **Libraries / APIs Used:** Room ORM (`TransactionDao`), `RadioGroup`, `Spinner`
* **Implementation Details:**
  * Detects Edit Mode if `getIntent().getIntExtra("transaction_id", -1)` $\ne -1$, loading existing data and updating button text to "Update Transaction".
  * Validates amount $> 0$ and non-empty description. Inserts or updates transaction in Room DB, and optionally inserts into `recurring_transactions` if `cbRecurring` is checked.

```java
// AddTransactionActivity.java -> saveTransaction()
Transaction transaction = new Transaction(amount, description, category, type, date, uid);
executorService.execute(() -> {
    transactionDao.insert(transaction);
    if (cbRecurring != null && cbRecurring.isChecked()) {
        Calendar cal = Calendar.getInstance();
        RecurringTransaction rt = new RecurringTransaction(amount, description, category, type, cal.get(Calendar.DAY_OF_MONTH), uid);
        transactionDao.insertRecurring(rt);
    }
    finish();
});
```

---

## 8. Camera Receipt Scanning & Image Enhancement (OCR)

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/AddTransactionActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/AddTransactionActivity.java) $\rightarrow$ `AddTransactionActivity`
* **Libraries / APIs Used:** `com.google.mlkit:text-recognition:16.0.1`, `FileProvider`, `ActivityResultContracts.TakePicture()`
* **Implementation Details:**
  * Captures full-resolution JPEG via `FileProvider`.
  * Preprocesses bitmap through `enhanceForOCR()`, converting image to high-contrast ARGB_8888 grayscale using a `ColorMatrix` saturation filter.
  * Passes bitmap to ML Kit `TextRecognition.getClient()`.

```java
// AddTransactionActivity.java -> enhanceForOCR()
private Bitmap enhanceForOCR(Bitmap original) {
    Bitmap result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(result);
    ColorMatrix colorMatrix = new ColorMatrix();
    colorMatrix.setSaturation(0); // Convert to grayscale for clear OCR contrast
    Paint paint = new Paint();
    paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
    canvas.drawBitmap(original, 0, 0, paint);
    return result;
}
```

---

## 9. Smart RegEx Text Parsing Engine

* **Main Package:** `com.example.expensetracker.utils` & `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/utils/OCRUtils.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/utils/OCRUtils.java) $\rightarrow$ `OCRUtils`
  * [`app/src/main/java/com/example/expensetracker/AddTransactionActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/AddTransactionActivity.java)
* **Libraries / APIs Used:** `java.util.regex.Pattern`, `java.util.regex.Matcher`
* **Implementation Details:**
  * Normalizes currency symbols (`₹`, `रु`, `Rs.`, `rs.`, `RS` $\rightarrow$ `" Rs "`).
  * 3-Stage RegEx Parser:
    1. Matches total labels (`Amount`, `Total Amount`, `Grand Total`) and numbers.
    2. Matches currency tags (`₹`, `Rs`, `INR`) followed by values.
    3. Fallback matching for numbers $\ge 10$ (excluding dates).
  * Auto-selects category spinner (`Food`, `Travel`, `Bills`, etc.) and transaction type (`Income`/`Expense`).

```java
// OCRUtils.java -> extractAmount()
Pattern amountLabelPattern = Pattern.compile(
    "(?i)(amount|total\\s*amount|grand\\s*total|total)\\s*[:=\\-]?\\s*(?:rs\\s*)?₹?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
);
Matcher matcher = amountLabelPattern.matcher(cleanText);
if (matcher.find()) {
    return Double.parseDouble(matcher.group(2).replace(",", ""));
}
```

---

## 10. Bulk Gallery Receipt OCR Batch Processing

* **Main Package:** `com.example.expensetracker` & `com.example.expensetracker.utils`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/MainActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/MainActivity.java) $\rightarrow$ `MainActivity`
* **Libraries / APIs Used:** `ActivityResultContracts.GetMultipleContents()`, ML Kit `TextRecognition`, `Tasks.await()`
* **Implementation Details:**
  * `btnBulkScan` triggers multiple photo picker. Asynchronously processes each image via ML Kit on `ExecutorService`, extracts amounts/descriptions using `OCRUtils`, and batch-inserts records into Room database.

```java
// MainActivity.java -> processBulkImages()
private final ActivityResultLauncher<String> bulkScanLauncher =
        registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), this::processBulkImages);

executorService.execute(() -> {
    for (Uri uri : uris) {
        InputImage image = InputImage.fromFilePath(this, uri);
        Text result = Tasks.await(recognizer.process(image));
        Double amount = OCRUtils.extractAmount(result.getText());
        String description = OCRUtils.extractDescription(result.getText());
        if (amount != null && amount > 0) {
            transactionDao.insert(new Transaction(amount, description, "Other", "Expense", date, uid));
        }
    }
});
```

---

## 11. Financial Analytics & MPAndroidChart Visualizations

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/AnalyticsActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/AnalyticsActivity.java) $\rightarrow$ `AnalyticsActivity`
  * [`app/src/main/res/layout/activity_analytics.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/activity_analytics.xml)
* **Libraries / APIs Used:** `com.github.PhilJay:MPAndroidChart:v3.1.0` (`PieChart`, `LineChart`, `PieDataSet`, `LineDataSet`)
* **Implementation Details:**
  * Filter period selector (`This Month`, `Last Month`, `Last 3 Months`, `This Year`).
  * `PieChart` renders interactive category expense percentage distribution.
  * `LineChart` renders expense/income trend lines over time with toggle buttons (`btnExpenseTrend` / `btnIncomeTrend`).

```java
// AnalyticsActivity.java -> setupPieChart()
PieDataSet dataSet = new PieDataSet(entries, "");
dataSet.setColors(colors);
PieData data = new PieData(dataSet);
pieChart.setData(data);
pieChart.setUsePercentValues(true);
pieChart.animateY(1000);
```

---

## 12. Financial Advice Insights & Activity Heatmap Grid

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/AnalyticsActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/AnalyticsActivity.java) $\rightarrow$ `AnalyticsActivity`
* **Implementation Details:**
  * Evaluates spending metrics to display top spending category, average daily spend, income vs. expense percentage changes, and actionable financial advice.
  * Dynamically populates a 28-cell `LinearLayout` heatmap grid with color-coded intensity background drawables.

```java
// AnalyticsActivity.java -> renderHeatmap()
for (int i = 0; i < 28; i++) {
    View dayCell = new View(this);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cellSize, cellSize);
    double amount = dayAmountMap.getOrDefault(dayKey, 0.0);
    dayCell.setBackground(getHeatmapDrawable(amount, maxAmount));
    heatmapContainer.addView(dayCell);
}
```

---

## 13. Monthly Budget Target Setup

* **Main Package:** `com.example.expensetracker` & `com.example.expensetracker.database`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/SettingsActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/SettingsActivity.java) $\rightarrow$ `SettingsActivity`
  * [`app/src/main/java/com/example/expensetracker/database/Budget.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/Budget.java) $\rightarrow$ `Budget`
* **Libraries / APIs Used:** `MaterialAlertDialogBuilder`, Room ORM (`transactionDao.insertBudget()`)
* **Implementation Details:**
  * Dialog prompts user for target spending limit. Stores `Budget` record associated with current `MM-yyyy` string key and user ID.

```java
// SettingsActivity.java -> saveBudget()
String monthYear = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());
Budget budget = new Budget(monthYear, amount, user.getUid());
executorService.execute(() -> {
    transactionDao.insertBudget(budget);
    runOnUiThread(() -> Toast.makeText(this, "Budget saved", Toast.LENGTH_SHORT).show());
});
```

---

## 14. A4 PDF Financial Report Generation & Sharing

* **Main Package:** `com.example.expensetracker`
* **File Paths & Classes:**
  * [`app/src/main/java/com/example/expensetracker/SettingsActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/SettingsActivity.java) $\rightarrow$ `SettingsActivity`
  * [`app/src/main/res/xml/file_paths.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/xml/file_paths.xml)
* **Libraries / APIs Used:** `android.graphics.pdf.PdfDocument`, `Canvas`, `FileProvider`, `Intent.ACTION_SEND`
* **Implementation Details:**
  * Creates an A4 PDF document (595 x 842 points).
  * Draws header title, metadata, transaction table (Date, Description, Category, color-coded Amounts), and Net Balance totals on `Canvas`.
  * Saves to cache directory and triggers system share sheet via `FileProvider`.

```java
// SettingsActivity.java -> generateAndSharePdf()
PdfDocument document = new PdfDocument();
PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 Size
PdfDocument.Page page = document.startPage(pageInfo);
Canvas canvas = page.getCanvas();

canvas.drawText("Expense Tracker Report", 40, 50, paint);
// Draw transaction rows, calculate net balance, finish page, save to cache and share via Intent.ACTION_SEND
```

---

## 15. Background Weekly Cloud Sync (Firestore)

* **Main Package:** `com.example.expensetracker.sync` & `com.example.expensetracker`
* **File Paths & Classes:**
  * Worker: [`app/src/main/java/com/example/expensetracker/sync/SyncWorker.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/sync/SyncWorker.java) $\rightarrow$ `SyncWorker`
  * Trigger: [`app/src/main/java/com/example/expensetracker/MainActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/MainActivity.java) (`scheduleWeeklySync()`)
* **Libraries / APIs Used:** `androidx.work:work-runtime:2.11.2` (`WorkManager`, `PeriodicWorkRequest`), `com.google.firebase:firebase-firestore` (`WriteBatch`)
* **Implementation Details:**
  * Scheduled every 7 days when device has network connectivity.
  * Reads local Room transactions for active user UID and batch-commits documents to Cloud Firestore under path `users/{uid}/transactions/{id}`.

```java
// SyncWorker.java -> doWork()
FirebaseFirestore db = FirebaseFirestore.getInstance();
WriteBatch batch = db.batch();
for (Transaction t : transactions) {
    batch.set(db.collection("users").document(uid).collection("transactions").document(String.valueOf(t.getId())), t);
}
com.google.android.gms.tasks.Tasks.await(batch.commit());
```

---

## 16. Background Daily Recurring Transaction Worker

* **Main Package:** `com.example.expensetracker.sync` & `com.example.expensetracker.database`
* **File Paths & Classes:**
  * Worker: [`app/src/main/java/com/example/expensetracker/sync/RecurringWorker.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/sync/RecurringWorker.java) $\rightarrow$ `RecurringWorker`
  * Trigger: [`app/src/main/java/com/example/expensetracker/MainActivity.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/MainActivity.java) (`scheduleDailyRecurringCheck()`)
* **Libraries / APIs Used:** `androidx.work:work-runtime:2.11.2` (`WorkManager`)
* **Implementation Details:**
  * Scheduled daily via WorkManager. Checks `recurring_transactions` table for entries matching today's day of the month, auto-inserting new records into the `transactions` table.

```java
// RecurringWorker.java -> doWork()
int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
List<RecurringTransaction> recurringList = dao.getRecurringForDay(currentDay);
for (RecurringTransaction rt : recurringList) {
    dao.insert(new Transaction(rt.getAmount(), rt.getDescription(), rt.getCategory(), rt.getType(), dateStr, rt.getUserId()));
}
```

---

## 17. Room ORM Database Architecture & App Security

* **Main Package:** `com.example.expensetracker.database`
* **File Paths & Classes:**
  * Configuration: [`app/src/main/java/com/example/expensetracker/database/AppDatabase.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/AppDatabase.java)
  * Data Access Object: [`app/src/main/java/com/example/expensetracker/database/TransactionDao.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/TransactionDao.java)
  * Entities:
    * [`app/src/main/java/com/example/expensetracker/database/Transaction.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/Transaction.java) $\rightarrow$ `transactions`
    * [`app/src/main/java/com/example/expensetracker/database/Budget.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/Budget.java) $\rightarrow$ `budgets`
    * [`app/src/main/java/com/example/expensetracker/database/RecurringTransaction.java`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/RecurringTransaction.java) $\rightarrow$ `recurring_transactions`
  * Manifest & Permissions: [`app/src/main/AndroidManifest.xml`](file:///C:/Users/Rushikesh%20Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/AndroidManifest.xml)
* **App Security Configuration:**
  * Permissions: `android.permission.INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA`.
  * Security Setup: Sensitive config files (`google-services.json`, `local.properties`) are excluded in `.gitignore`. Google Cloud API keys are restricted with SHA-1 fingerprint certificate matching.
