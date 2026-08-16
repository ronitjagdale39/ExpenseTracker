package com.example.expensetracker;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Budget;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail;
    private MaterialButton btnExportPdf, btnLogout, btnSetBudget;
    private com.google.android.material.switchmaterial.SwitchMaterial switchBiometric;

    private TransactionDao transactionDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnLogout = findViewById(R.id.btnLogout);
        btnSetBudget = findViewById(R.id.btnSetBudget);
        switchBiometric = findViewById(R.id.switchBiometric);

        transactionDao = AppDatabase.getInstance(this).transactionDao();

        loadUserInfo();
        loadSettings();

        btnExportPdf.setOnClickListener(v -> showExportDialog());
        btnSetBudget.setOnClickListener(v -> showBudgetDialog());
        btnLogout.setOnClickListener(v -> logout());

        switchBiometric.setOnCheckedChangeListener((v, isChecked) -> {
            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("biometric_enabled", isChecked).apply();
        });
    }

    private void loadSettings() {
        boolean isEnabled = getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("biometric_enabled", false);
        switchBiometric.setChecked(isEnabled);
    }

    private void showBudgetDialog() {
        TextInputEditText etBudget = new TextInputEditText(this);
        etBudget.setHint("Enter amount (e.g. 10000)");
        etBudget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Monthly Budget")
                .setMessage("Set your total monthly spending limit")
                .setView(etBudget)
                .setPositiveButton("Save", (dialog, which) -> {
                    String amountStr = etBudget.getText().toString();
                    if (!amountStr.isEmpty()) {
                        saveBudget(Double.parseDouble(amountStr));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveBudget(double amount) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String currentMonthYear = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());

        executorService.execute(() -> {
            List<Budget> existing = transactionDao.getBudgetsForMonth(uid, currentMonthYear);
            if (!existing.isEmpty()) {
                Budget b = existing.get(0);
                b.setLimitAmount(amount);
                transactionDao.updateBudget(b);
            } else {
                Budget b = new Budget("All", amount, currentMonthYear, uid);
                transactionDao.insertBudget(b);
            }
            runOnUiThread(() -> Toast.makeText(this, "Budget saved", Toast.LENGTH_SHORT).show());
        });
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            tvUserEmail.setText(user.getEmail());
        }
    }

    private void showExportDialog() {
        String[] options = {"All Time Report", "Monthly Report"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Export Type")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportTransactions(null); // null means all time
                    } else {
                        showMonthPicker();
                    }
                })
                .show();
    }

    private void showMonthPicker() {
        // Simple month picker: Current month and previous 5 months
        String[] months = new String[6];
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        
        for (int i = 0; i < 6; i++) {
            months[i] = sdf.format(cal.getTime());
            cal.add(Calendar.MONTH, -1);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Month")
                .setItems(months, (dialog, which) -> {
                    exportTransactions(months[which]);
                })
                .show();
    }

    private void exportTransactions(String filter) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            List<Transaction> transactions = transactionDao.getAllTransactions(uid);
            List<Transaction> filteredList = new ArrayList<>();

            if (filter == null) {
                filteredList = transactions;
            } else {
                for (Transaction t : transactions) {
                    if (t.getDate().contains(filter)) {
                        filteredList.add(t);
                    }
                }
            }

            if (filteredList.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "No transactions found to export", Toast.LENGTH_SHORT).show());
                return;
            }

            generateAndSharePdf(filteredList, filter == null ? "All Time" : filter);
        });
    }

    private void generateAndSharePdf(List<Transaction> transactions, String title) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 Size
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Header
        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        canvas.drawText("Expense Tracker Report", 40, 50, paint);

        paint.setTextSize(14f);
        paint.setFakeBoldText(false);
        canvas.drawText("Type: " + title, 40, 80, paint);
        canvas.drawText("Date: " + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()), 40, 100, paint);

        // Table Header
        paint.setFakeBoldText(true);
        int y = 150;
        canvas.drawText("Date", 40, y, paint);
        canvas.drawText("Description", 140, y, paint);
        canvas.drawText("Category", 340, y, paint);
        canvas.drawText("Amount", 480, y, paint);
        
        canvas.drawLine(40, y + 5, 550, y + 5, paint);
        y += 30;

        // Rows
        paint.setFakeBoldText(false);
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : transactions) {
            if (y > 800) break; // Simple page break protection for demo

            canvas.drawText(t.getDate(), 40, y, paint);
            
            String desc = t.getDescription();
            if (desc.length() > 20) desc = desc.substring(0, 17) + "...";
            canvas.drawText(desc, 140, y, paint);
            
            canvas.drawText(t.getCategory(), 340, y, paint);

            String amountStr = String.format("₹%.2f", t.getAmount());
            if ("Income".equalsIgnoreCase(t.getType())) {
                totalIncome += t.getAmount();
                paint.setColor(Color.GREEN);
            } else {
                totalExpense += t.getAmount();
                paint.setColor(Color.RED);
            }
            canvas.drawText(amountStr, 480, y, paint);
            paint.setColor(Color.BLACK);

            y += 25;
        }

        // Summary
        y += 20;
        canvas.drawLine(40, y, 550, y, paint);
        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("Total Income: ₹" + totalIncome, 40, y, paint);
        y += 25;
        canvas.drawText("Total Expense: ₹" + totalExpense, 40, y, paint);
        y += 25;
        canvas.drawText("Net Balance: ₹" + (totalIncome - totalExpense), 40, y, paint);

        document.finishPage(page);

        // Save and Share
        try {
            File cachePath = new File(getCacheDir(), "exports");
            cachePath.mkdirs();
            File pdfFile = new File(cachePath, "Expense_Report_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            sharePdfFile(pdfFile);

        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show());
        }
    }

    private void sharePdfFile(File file) {
        Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(intent, "Share Report"));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
