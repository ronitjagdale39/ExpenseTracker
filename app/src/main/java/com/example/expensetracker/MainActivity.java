package com.example.expensetracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Budget;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;
import com.example.expensetracker.sync.RecurringWorker;
import com.example.expensetracker.sync.SyncWorker;
import com.example.expensetracker.utils.OCRUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView tvBalance, tvIncome, tvExpense, tvBudgetText;
    private Button btnAddTransaction, btnAnalytics, btnSettings;
    private ImageButton btnFilter, btnBulkScan;
    private SearchView searchView;
    private RecyclerView recyclerTransactions;
    private LinearProgressIndicator budgetProgress;

    private TransactionDao transactionDao;
    private TransactionAdapter transactionAdapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private String currentSearchQuery = "";
    private String currentCategoryFilter = null;

    private final ActivityResultLauncher<String> bulkScanLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), this::processBulkImages);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBalance = findViewById(R.id.tvBalance);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvBudgetText = findViewById(R.id.tvBudgetText);
        budgetProgress = findViewById(R.id.budgetProgress);
        
        btnAddTransaction = findViewById(R.id.btnAddTransaction);
        btnAnalytics = findViewById(R.id.btnAnalytics);
        btnSettings = findViewById(R.id.btnSettings);
        btnFilter = findViewById(R.id.btnFilter);
        btnBulkScan = findViewById(R.id.btnBulkScan);
        searchView = findViewById(R.id.searchView);
        recyclerTransactions = findViewById(R.id.recyclerTransactions);

        recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        recyclerTransactions.setNestedScrollingEnabled(false);

        transactionDao = AppDatabase.getInstance(this).transactionDao();

        btnAddTransaction.setOnClickListener(view -> startActivity(new Intent(this, AddTransactionActivity.class)));
        btnAnalytics.setOnClickListener(view -> startActivity(new Intent(this, AnalyticsActivity.class)));
        btnSettings.setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));
        btnBulkScan.setOnClickListener(v -> bulkScanLauncher.launch("image/*"));

        setupSearchAndFilter();
        loadDashboard();
        scheduleWeeklySync();
        scheduleDailyRecurringCheck();
    }

    private void setupSearchAndFilter() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                loadDashboard();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                loadDashboard();
                return true;
            }
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        String[] categories = {"All", "Food", "Travel", "Shopping", "Bills", "Entertainment", "Health", "Education", "Salary", "Other"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Filter by Category")
                .setItems(categories, (dialog, which) -> {
                    if (which == 0) {
                        currentCategoryFilter = null;
                    } else {
                        currentCategoryFilter = categories[which];
                    }
                    loadDashboard();
                })
                .show();
    }

    private void processBulkImages(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;

        Toast.makeText(this, "Scanning " + uris.size() + " images...", Toast.LENGTH_SHORT).show();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        executorService.execute(() -> {
            for (Uri uri : uris) {
                try {
                    InputImage image = InputImage.fromFilePath(this, uri);
                    Text result = Tasks.await(recognizer.process(image));
                    String text = result.getText();
                    
                    Double amount = OCRUtils.extractAmount(text);
                    String description = OCRUtils.extractDescription(text);
                    
                    if (amount != null && amount > 0) {
                        Transaction t = new Transaction(amount, description, "Other", "Expense", date, uid);
                        transactionDao.insert(t);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Bulk scan complete", Toast.LENGTH_SHORT).show();
                loadDashboard();
            });
        });
    }

    private void scheduleWeeklySync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 7, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "WeeklySync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    private void scheduleDailyRecurringCheck() {
        PeriodicWorkRequest recurringRequest =
                new PeriodicWorkRequest.Builder(RecurringWorker.class, 1, TimeUnit.DAYS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyRecurring",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringRequest
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (transactionDao != null) loadDashboard();
    }

    private void loadDashboard() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        executorService.execute(() -> {
            Double income = transactionDao.getTotalIncome(uid);
            Double expense = transactionDao.getTotalExpense(uid);
            
            if (income == null) income = 0.0;
            if (expense == null) expense = 0.0;
            
            double balance = income - expense;

            List<Transaction> transactions;
            if (currentCategoryFilter != null) {
                transactions = transactionDao.filterByCategory(uid, currentCategoryFilter);
            } else if (!currentSearchQuery.isEmpty()) {
                transactions = transactionDao.searchTransactions(uid, "%" + currentSearchQuery + "%");
            } else {
                transactions = transactionDao.getAllTransactions(uid);
            }

            // Budget Calculation
            String currentMonthYear = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());
            List<Budget> budgets = transactionDao.getBudgetsForMonth(uid, currentMonthYear);
            double totalBudget = 0;
            for (Budget b : budgets) totalBudget += b.getLimitAmount();

            double totalMonthExpense = 0;
            List<Transaction> allTransactions = transactionDao.getAllTransactions(uid);
            for (Transaction t : allTransactions) {
                if ("Expense".equalsIgnoreCase(t.getType()) && t.getDate().contains(currentMonthYear)) {
                    totalMonthExpense += t.getAmount();
                }
            }

            double finalIncome = income;
            double finalExpense = expense;
            double finalBalance = balance;
            double finalTotalBudget = totalBudget;
            double finalMonthExpense = totalMonthExpense;

            runOnUiThread(() -> {
                tvIncome.setText(String.format(Locale.getDefault(), "₹%.2f", finalIncome));
                tvExpense.setText(String.format(Locale.getDefault(), "₹%.2f", finalExpense));
                tvBalance.setText(String.format(Locale.getDefault(), "₹%.2f", finalBalance));

                if (finalTotalBudget > 0) {
                    int progress = (int) ((finalMonthExpense / finalTotalBudget) * 100);
                    budgetProgress.setProgress(Math.min(progress, 100));
                    tvBudgetText.setText(String.format(Locale.getDefault(), "₹%.2f spent of ₹%.2f", finalMonthExpense, finalTotalBudget));
                } else {
                    budgetProgress.setProgress(0);
                    tvBudgetText.setText("Set a budget in Settings");
                }

                transactionAdapter = new TransactionAdapter(transactions, this::deleteTransaction);
                recyclerTransactions.setAdapter(transactionAdapter);
            });
        });
    }

    private void deleteTransaction(Transaction transaction) {
        executorService.execute(() -> {
            transactionDao.delete(transaction);
            runOnUiThread(() -> {
                Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                loadDashboard();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
