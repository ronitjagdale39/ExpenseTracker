package com.example.expensetracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddTransactionActivity extends AppCompatActivity {

    private TextInputEditText etAmount;
    private TextInputEditText etDescription;

    private Spinner spinnerCategory;

    private RadioGroup radioGroupType;
    private RadioButton radioExpense;
    private RadioButton radioIncome;

    private Button btnSaveTransaction;
    private Button btnScanReceipt;

    private TransactionDao transactionDao;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    // =========================================
    // EDIT MODE
    // =========================================

    private int transactionId = -1;
    private Transaction existingTransaction;

    // =========================================
    // FULL RESOLUTION CAMERA
    // =========================================

    private Uri photoUri;

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {

                        if (success && photoUri != null) {
                            runOCRFromUri(photoUri);
                        } else {
                            Toast.makeText(
                                    this,
                                    "Photo capture cancelled",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    // =========================================
    // CAMERA PERMISSION
    // =========================================

    private final ActivityResultLauncher<String>
            cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {

                        if (granted) {
                            openCamera();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Camera permission required",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_add_transaction
        );

        // =========================================
        // FIND VIEWS
        // =========================================

        etAmount =
                findViewById(R.id.etAmount);

        etDescription =
                findViewById(R.id.etDescription);

        spinnerCategory =
                findViewById(R.id.spinnerCategory);

        radioGroupType =
                findViewById(R.id.radioGroupType);

        radioExpense =
                findViewById(R.id.radioExpense);

        radioIncome =
                findViewById(R.id.radioIncome);

        btnSaveTransaction =
                findViewById(R.id.btnSaveTransaction);

        btnScanReceipt =
                findViewById(R.id.btnScanReceipt);

        // =========================================
        // DATABASE
        // =========================================

        AppDatabase database =
                AppDatabase.getInstance(this);

        transactionDao =
                database.transactionDao();

        // =========================================
        // CATEGORY
        // =========================================

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

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategory.setAdapter(adapter);

        // =========================================
        // EDIT MODE
        // =========================================

        transactionId =
                getIntent().getIntExtra(
                        "transaction_id",
                        -1
                );

        if (transactionId != -1) {

            btnSaveTransaction.setText(
                    "Update Transaction"
            );

            loadTransaction();

        } else {

            radioExpense.setChecked(true);
        }

        // =========================================
        // SCAN RECEIPT
        // =========================================

        btnScanReceipt.setOnClickListener(view -> {
            checkCameraPermission();
        });

        // =========================================
        // SAVE / UPDATE
        // =========================================

        btnSaveTransaction.setOnClickListener(view -> {

            if (transactionId != -1) {
                updateTransaction();
            } else {
                saveTransaction();
            }
        });
    }

    // =====================================================
    // CAMERA PERMISSION
    // =====================================================

    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            openCamera();

        } else {

            cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }

    // =====================================================
    // CREATE FULL RESOLUTION IMAGE
    // =====================================================

    private File createImageFile() throws IOException {

        String timeStamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                ).format(new Date());

        String imageFileName =
                "RECEIPT_" + timeStamp;

        File storageDir =
                getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES
                );

        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    // =====================================================
    // OPEN FULL RESOLUTION CAMERA
    // =====================================================

    private void openCamera() {

        try {

            File photoFile =
                    createImageFile();

            photoUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".fileprovider",
                            photoFile
                    );

            cameraLauncher.launch(photoUri);

        } catch (IOException e) {

            Toast.makeText(
                    this,
                    "Could not create image file",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =====================================================
    // OCR FROM FULL RESOLUTION IMAGE
    // =====================================================

    private void runOCRFromUri(Uri uri) {

        Toast.makeText(
                this,
                "Reading receipt...",
                Toast.LENGTH_SHORT
        ).show();

        try {

            InputImage image =
                    InputImage.fromFilePath(
                            this,
                            uri
                    );

            TextRecognizer recognizer =
                    TextRecognition.getClient(
                            TextRecognizerOptions.DEFAULT_OPTIONS
                    );

            recognizer.process(image)
                    .addOnSuccessListener(result -> {

                        processOCRText(result);

                        recognizer.close();

                    })
                    .addOnFailureListener(error -> {

                        recognizer.close();

                        Toast.makeText(
                                this,
                                "Could not read receipt",
                                Toast.LENGTH_SHORT
                        ).show();
                    });

        } catch (IOException e) {

            Toast.makeText(
                    this,
                    "Could not load receipt image",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =====================================================
    // PROCESS OCR TEXT
    // =====================================================

    private void processOCRText(Text result) {

        String text =
                result.getText();

        if (text == null ||
                text.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "No text detected",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // DEBUG
        Toast.makeText(
                this,
                "OCR:\n" + text,
                Toast.LENGTH_LONG
        ).show();

        // =========================================
        // AMOUNT
        // =========================================

        Double detectedAmount =
                extractAmount(text);

        if (detectedAmount != null) {

            etAmount.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.2f",
                            detectedAmount
                    )
            );

            Toast.makeText(
                    this,
                    "Amount detected: ₹"
                            + String.format(
                            Locale.getDefault(),
                            "%.2f",
                            detectedAmount
                    ),
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "Amount not detected. Enter manually.",
                    Toast.LENGTH_LONG
            ).show();
        }

        // =========================================
        // DESCRIPTION
        // =========================================

        String description =
                extractDescription(text);

        if (description != null &&
                !description.isEmpty()) {

            etDescription.setText(
                    description
            );
        }
    }

    // =====================================================
    // EXTRACT AMOUNT
    // =====================================================

    private Double extractAmount(String text) {

        if (text == null ||
                text.trim().isEmpty()) {

            return null;
        }

        String cleanText =
                text.replace(",", "")
                        .replace("₹", " Rs ")
                        .replace("—", "-")
                        .replace("–", "-");

        // =========================================
        // 1. TOTAL / GRAND TOTAL
        // =========================================

        Pattern totalPattern =
                Pattern.compile(
                        "(?i)(grand\\s*total|"
                                + "total\\s*amount|"
                                + "net\\s*amount|"
                                + "amount\\s*payable|"
                                + "total|"
                                + "amount)"
                                + "\\s*[:=\\-]?\\s*"
                                + "(?:rs\\.?|inr)?\\s*"
                                + "([0-9]+(?:\\.[0-9]{1,2})?)"
                );

        Matcher totalMatcher =
                totalPattern.matcher(cleanText);

        Double totalAmount = null;

        while (totalMatcher.find()) {

            try {

                double value =
                        Double.parseDouble(
                                totalMatcher.group(2)
                        );

                if (value > 0) {
                    totalAmount = value;
                }

            } catch (Exception ignored) {
            }
        }

        if (totalAmount != null) {
            return totalAmount;
        }

        // =========================================
        // 2. CURRENCY
        // =========================================

        Pattern currencyPattern =
                Pattern.compile(
                        "(?i)(?:rs\\.?|inr)\\s*"
                                + "([0-9]+(?:\\.[0-9]{1,2})?)"
                );

        Matcher currencyMatcher =
                currencyPattern.matcher(cleanText);

        Double largestAmount = null;

        while (currencyMatcher.find()) {

            try {

                double value =
                        Double.parseDouble(
                                currencyMatcher.group(1)
                        );

                if (value > 0 &&
                        (largestAmount == null ||
                                value > largestAmount)) {

                    largestAmount = value;
                }

            } catch (Exception ignored) {
            }
        }

        if (largestAmount != null) {
            return largestAmount;
        }

        // =========================================
        // 3. DECIMAL NUMBERS
        // =========================================

        Pattern decimalPattern =
                Pattern.compile(
                        "\\b([0-9]+\\.[0-9]{1,2})\\b"
                );

        Matcher decimalMatcher =
                decimalPattern.matcher(cleanText);

        while (decimalMatcher.find()) {

            try {

                double value =
                        Double.parseDouble(
                                decimalMatcher.group(1)
                        );

                if (value > 0 &&
                        (largestAmount == null ||
                                value > largestAmount)) {

                    largestAmount = value;
                }

            } catch (Exception ignored) {
            }
        }

        if (largestAmount != null) {
            return largestAmount;
        }

        // =========================================
        // 4. INTEGER FALLBACK
        // =========================================

        Pattern numberPattern =
                Pattern.compile(
                        "\\b([0-9]{2,7})\\b"
                );

        Matcher numberMatcher =
                numberPattern.matcher(cleanText);

        while (numberMatcher.find()) {

            try {

                double value =
                        Double.parseDouble(
                                numberMatcher.group(1)
                        );

                // Ignore years
                if (value >= 1900 &&
                        value <= 2100) {
                    continue;
                }

                if (value > 0 &&
                        (largestAmount == null ||
                                value > largestAmount)) {

                    largestAmount = value;
                }

            } catch (Exception ignored) {
            }
        }

        return largestAmount;
    }

    // =====================================================
    // EXTRACT DESCRIPTION
    // =====================================================

    private String extractDescription(String text) {

        String[] lines =
                text.split("\\r?\\n");

        for (String line : lines) {

            String clean =
                    line.trim();

            if (clean.length() >= 3 &&
                    clean.length() <= 40 &&
                    !clean.matches(".*\\d{3,}.*") &&
                    !clean.matches("(?i).*total.*") &&
                    !clean.matches("(?i).*amount.*") &&
                    !clean.matches("(?i).*subtotal.*") &&
                    !clean.matches("(?i).*tax.*")) {

                return clean;
            }
        }

        return null;
    }

    // =====================================================
    // LOAD TRANSACTION
    // =====================================================

    private void loadTransaction() {

        executorService.execute(() -> {

            existingTransaction =
                    transactionDao.getTransactionById(
                            transactionId
                    );

            if (existingTransaction == null) {

                runOnUiThread(() -> {

                    Toast.makeText(
                            this,
                            "Transaction not found",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                });

                return;
            }

            runOnUiThread(() -> {

                etAmount.setText(
                        String.valueOf(
                                existingTransaction.getAmount()
                        )
                );

                etDescription.setText(
                        existingTransaction.getDescription()
                );

                if ("Income".equalsIgnoreCase(
                        existingTransaction.getType()
                )) {

                    radioIncome.setChecked(true);

                } else {

                    radioExpense.setChecked(true);
                }

                String category =
                        existingTransaction.getCategory();

                ArrayAdapter<String> adapter =
                        (ArrayAdapter<String>)
                                spinnerCategory.getAdapter();

                int position =
                        adapter.getPosition(category);

                if (position >= 0) {

                    spinnerCategory.setSelection(
                            position
                    );
                }
            });
        });
    }

    // =====================================================
    // SAVE
    // =====================================================

    private void saveTransaction() {

        String amountText =
                etAmount.getText()
                        .toString()
                        .trim();

        String description =
                etDescription.getText()
                        .toString()
                        .trim();

        if (amountText.isEmpty()) {

            etAmount.setError(
                    "Enter amount"
            );

            etAmount.requestFocus();

            return;
        }

        if (description.isEmpty()) {

            etDescription.setError(
                    "Enter description"
            );

            etDescription.requestFocus();

            return;
        }

        double amount;

        try {

            amount =
                    Double.parseDouble(
                            amountText
                    );

        } catch (NumberFormatException e) {

            etAmount.setError(
                    "Enter valid amount"
            );

            return;
        }

        if (amount <= 0) {

            etAmount.setError(
                    "Amount must be greater than 0"
            );

            return;
        }

        String category =
                spinnerCategory
                        .getSelectedItem()
                        .toString();

        String type =
                radioIncome.isChecked()
                        ? "Income"
                        : "Expense";

        String date =
                new SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                ).format(new Date());

        Transaction transaction =
                new Transaction(
                        amount,
                        description,
                        category,
                        type,
                        date
                );

        executorService.execute(() -> {

            transactionDao.insert(
                    transaction
            );

            runOnUiThread(() -> {

                Toast.makeText(
                        this,
                        "Transaction saved",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            });
        });
    }

    // =====================================================
    // UPDATE
    // =====================================================

    private void updateTransaction() {

        if (existingTransaction == null) {

            Toast.makeText(
                    this,
                    "Transaction not loaded",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String amountText =
                etAmount.getText()
                        .toString()
                        .trim();

        String description =
                etDescription.getText()
                        .toString()
                        .trim();

        if (amountText.isEmpty()) {

            etAmount.setError(
                    "Enter amount"
            );

            return;
        }

        if (description.isEmpty()) {

            etDescription.setError(
                    "Enter description"
            );

            return;
        }

        double amount;

        try {

            amount =
                    Double.parseDouble(
                            amountText
                    );

        } catch (NumberFormatException e) {

            etAmount.setError(
                    "Enter valid amount"
            );

            return;
        }

        if (amount <= 0) {

            etAmount.setError(
                    "Amount must be greater than 0"
            );

            return;
        }

        String category =
                spinnerCategory
                        .getSelectedItem()
                        .toString();

        String type =
                radioIncome.isChecked()
                        ? "Income"
                        : "Expense";

        Transaction updatedTransaction =
                new Transaction(
                        amount,
                        description,
                        category,
                        type,
                        existingTransaction.getDate()
                );

        updatedTransaction.setId(
                existingTransaction.getId()
        );

        executorService.execute(() -> {

            transactionDao.update(
                    updatedTransaction
            );

            runOnUiThread(() -> {

                Toast.makeText(
                        this,
                        "Transaction updated",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
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