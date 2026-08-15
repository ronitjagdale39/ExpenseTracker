package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView tvBalance;
    private TextView tvIncome;
    private TextView tvExpense;

    private Button btnAddTransaction;
    private Button btnAnalytics;
    private Button btnSettings;

    private RecyclerView recyclerTransactions;

    private TransactionDao transactionDao;

    private TransactionAdapter transactionAdapter;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();


    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        // =================================================
        // FIND VIEWS
        // =================================================

        tvBalance = findViewById(R.id.tvBalance);

        tvIncome = findViewById(R.id.tvIncome);

        tvExpense = findViewById(R.id.tvExpense);

        btnAddTransaction =
                findViewById(R.id.btnAddTransaction);

        btnAnalytics =
                findViewById(R.id.btnAnalytics);

        btnSettings =
                findViewById(R.id.btnSettings);

        recyclerTransactions =
                findViewById(R.id.recyclerTransactions);


        // =================================================
        // RECYCLER VIEW
        // =================================================

        recyclerTransactions.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerTransactions.setNestedScrollingEnabled(
                false
        );


        // =================================================
        // DATABASE
        // =================================================

        AppDatabase database =
                AppDatabase.getInstance(this);

        transactionDao =
                database.transactionDao();


        // =================================================
        // ADD TRANSACTION
        // =================================================

        btnAddTransaction.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            AddTransactionActivity.class
                    );

            startActivity(intent);
        });


        // =================================================
        // ANALYTICS
        // =================================================

        btnAnalytics.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            AnalyticsActivity.class
                    );

            startActivity(intent);
        });


        // =================================================
        // SETTINGS
        // =================================================

        btnSettings.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            SettingsActivity.class
                    );

            startActivity(intent);
        });


        // =================================================
        // LOAD DASHBOARD
        // =================================================

        loadDashboard();
    }


    // =====================================================
    // ON RESUME
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (transactionDao != null) {

            loadDashboard();
        }
    }


    // =====================================================
    // LOAD DASHBOARD
    // =====================================================

    private void loadDashboard() {

        executorService.execute(() -> {

            // =================================================
            // INCOME
            // =================================================

            Double income =
                    transactionDao.getTotalIncome();


            // =================================================
            // EXPENSE
            // =================================================

            Double expense =
                    transactionDao.getTotalExpense();


            // =================================================
            // NULL SAFETY
            // =================================================

            if (income == null) {

                income = 0.0;
            }

            if (expense == null) {

                expense = 0.0;
            }


            // =================================================
            // BALANCE
            // =================================================

            double balance =
                    income - expense;


            // =================================================
            // TRANSACTIONS
            // =================================================

            List<Transaction> transactions =
                    transactionDao.getAllTransactions();


            // =================================================
            // FINAL VALUES
            // =================================================

            double finalIncome = income;

            double finalExpense = expense;

            double finalBalance = balance;


            // =================================================
            // UPDATE UI
            // =================================================

            runOnUiThread(() -> {

                tvIncome.setText(
                        String.format(
                                Locale.getDefault(),
                                "₹%.2f",
                                finalIncome
                        )
                );


                tvExpense.setText(
                        String.format(
                                Locale.getDefault(),
                                "₹%.2f",
                                finalExpense
                        )
                );


                tvBalance.setText(
                        String.format(
                                Locale.getDefault(),
                                "₹%.2f",
                                finalBalance
                        )
                );


                // =================================================
                // TRANSACTION ADAPTER
                // =================================================

                transactionAdapter =
                        new TransactionAdapter(
                                transactions,
                                this::deleteTransaction
                        );


                recyclerTransactions.setAdapter(
                        transactionAdapter
                );
            });
        });
    }


    // =====================================================
    // DELETE TRANSACTION
    // =====================================================

    private void deleteTransaction(
            Transaction transaction) {

        executorService.execute(() -> {

            // Delete from Room

            transactionDao.delete(
                    transaction
            );


            // Refresh dashboard

            runOnUiThread(() -> {

                Toast.makeText(
                        MainActivity.this,
                        "Transaction deleted",
                        Toast.LENGTH_SHORT
                ).show();


                loadDashboard();
            });
        });
    }


    // =====================================================
    // CLEANUP
    // =====================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executorService.shutdown();
    }
}