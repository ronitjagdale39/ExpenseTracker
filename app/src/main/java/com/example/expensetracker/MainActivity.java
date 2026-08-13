package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

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

    private RecyclerView recyclerTransactions;

    private TransactionDao transactionDao;

    private TransactionAdapter transactionAdapter;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // -----------------------------
        // Find Views
        // -----------------------------

        tvBalance = findViewById(R.id.tvBalance);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);

        btnAddTransaction = findViewById(R.id.btnAddTransaction);

        recyclerTransactions =
                findViewById(R.id.recyclerTransactions);


        // -----------------------------
        // RecyclerView Setup
        // -----------------------------

        recyclerTransactions.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerTransactions.setNestedScrollingEnabled(false);


        // -----------------------------
        // Database Setup
        // -----------------------------

        AppDatabase database =
                AppDatabase.getInstance(this);

        transactionDao =
                database.transactionDao();


        // -----------------------------
        // Add Transaction Button
        // -----------------------------

        btnAddTransaction.setOnClickListener(view -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    AddTransactionActivity.class
            );

            startActivity(intent);
        });


        // -----------------------------
        // Load Dashboard
        // -----------------------------

        loadDashboard();
    }


    // --------------------------------------------------
    // Refresh Dashboard when Activity comes back
    // --------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        if (transactionDao != null) {
            loadDashboard();
        }
    }


    // --------------------------------------------------
    // Load Income, Expense, Balance & Transactions
    // --------------------------------------------------

    private void loadDashboard() {

        executorService.execute(() -> {

            // Get total income
            Double income =
                    transactionDao.getTotalIncome();

            // Get total expense
            Double expense =
                    transactionDao.getTotalExpense();

            // Prevent null values
            if (income == null) {
                income = 0.0;
            }

            if (expense == null) {
                expense = 0.0;
            }

            // Calculate balance
            double balance =
                    income - expense;

            // Get all transactions
            List<Transaction> transactions =
                    transactionDao.getAllTransactions();


            // Final values for UI thread
            double finalIncome = income;
            double finalExpense = expense;
            double finalBalance = balance;


            // Update UI
            runOnUiThread(() -> {

                // -----------------------------
                // Income
                // -----------------------------

                tvIncome.setText(
                        String.format(
                                Locale.getDefault(),
                                "₹%.2f",
                                finalIncome
                        )
                );


                // -----------------------------
                // Expense
                // -----------------------------

                tvExpense.setText(
                        String.format(
                                Locale.getDefault(),
                                "₹%.2f",
                                finalExpense
                        )
                );


                // -----------------------------
                // Balance
                // -----------------------------

                tvBalance.setText(
                        String.format(
                                Locale.getDefault(),
                                "₹%.2f",
                                finalBalance
                        )
                );


                // -----------------------------
                // Recent Transactions
                // -----------------------------

                transactionAdapter =
                        new TransactionAdapter(
                                transactions
                        );

                recyclerTransactions.setAdapter(
                        transactionAdapter
                );
            });
        });
    }


    // --------------------------------------------------
    // Cleanup
    // --------------------------------------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();

        executorService.shutdown();
    }
}