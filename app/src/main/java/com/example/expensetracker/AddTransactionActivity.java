package com.example.expensetracker;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTransactionActivity extends AppCompatActivity {

    private EditText etAmount;
    private EditText etDescription;

    private Spinner spinnerCategory;

    private RadioGroup radioGroupType;
    private RadioButton radioExpense;
    private RadioButton radioIncome;

    private Button btnSaveTransaction;

    private TransactionDao transactionDao;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_transaction);


        // -----------------------------------------
        // Find Views
        // -----------------------------------------

        etAmount = findViewById(R.id.etAmount);

        etDescription = findViewById(R.id.etDescription);

        spinnerCategory = findViewById(R.id.spinnerCategory);

        radioGroupType = findViewById(R.id.radioGroupType);

        radioExpense = findViewById(R.id.radioExpense);

        radioIncome = findViewById(R.id.radioIncome);

        btnSaveTransaction =
                findViewById(R.id.btnSaveTransaction);


        // -----------------------------------------
        // Category Spinner
        // -----------------------------------------

        String[] categories = {
                "Food",
                "Travel",
                "Shopping",
                "Bills",
                "Entertainment",
                "Health",
                "Education",
                "Salary",
                "Other"
        };

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategory.setAdapter(categoryAdapter);


        // -----------------------------------------
        // Database
        // -----------------------------------------

        AppDatabase database =
                AppDatabase.getInstance(this);

        transactionDao =
                database.transactionDao();


        // -----------------------------------------
        // Save Button
        // -----------------------------------------

        btnSaveTransaction.setOnClickListener(view ->
                saveTransaction()
        );
    }


    // =============================================
    // SAVE TRANSACTION
    // =============================================

    private void saveTransaction() {

        // -----------------------------------------
        // Get Input
        // -----------------------------------------

        String amountText =
                etAmount.getText().toString().trim();

        String description =
                etDescription.getText().toString().trim();

        String category =
                spinnerCategory.getSelectedItem().toString();


        // -----------------------------------------
        // Validation
        // -----------------------------------------

        if (amountText.isEmpty()) {

            etAmount.setError("Enter amount");
            etAmount.requestFocus();
            return;
        }

        if (description.isEmpty()) {

            etDescription.setError("Enter description");
            etDescription.requestFocus();
            return;
        }


        // -----------------------------------------
        // Convert Amount
        // -----------------------------------------

        double amount;

        try {

            amount = Double.parseDouble(amountText);

        } catch (NumberFormatException e) {

            etAmount.setError("Enter a valid amount");
            etAmount.requestFocus();
            return;
        }


        if (amount <= 0) {

            etAmount.setError("Amount must be greater than 0");
            etAmount.requestFocus();
            return;
        }


        // -----------------------------------------
        // Transaction Type
        // -----------------------------------------

        String type;

        if (radioIncome.isChecked()) {

            type = "Income";

        } else {

            type = "Expense";
        }


        // -----------------------------------------
        // Current Date
        // -----------------------------------------

        String date =
                new SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                ).format(new Date());


        // -----------------------------------------
        // Create Transaction Object
        // -----------------------------------------

        Transaction transaction =
                new Transaction(
                        amount,
                        description,
                        category,
                        type,
                        date
                );


        // -----------------------------------------
        // Insert into Room Database
        // -----------------------------------------

        executorService.execute(() -> {

            transactionDao.insert(transaction);


            // Return to UI thread

            runOnUiThread(() -> {

                Toast.makeText(
                        AddTransactionActivity.this,
                        "Transaction saved successfully",
                        Toast.LENGTH_SHORT
                ).show();


                // Go back to Dashboard

                finish();
            });
        });
    }


    // =============================================
    // CLEANUP
    // =============================================

    @Override
    protected void onDestroy() {
        super.onDestroy();

        executorService.shutdown();
    }
}