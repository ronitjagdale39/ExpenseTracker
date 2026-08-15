package com.example.expensetracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
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
import com.google.android.material.button.MaterialButton;
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

    // =========================================
    // VIEWS
    // =========================================

    private TextInputEditText etAmount;
    private TextInputEditText etDescription;

    private Spinner spinnerCategory;

    private RadioGroup radioGroupType;
    private RadioButton radioExpense;
    private RadioButton radioIncome;

    private MaterialButton btnSaveTransaction;
    private MaterialButton btnScanReceipt;


    // =========================================
    // DATABASE
    // =========================================

    private TransactionDao transactionDao;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();


    // =========================================
    // EDIT MODE
    // =========================================

    private int transactionId = -1;

    private Transaction existingTransaction;


    // =========================================
    // CAMERA
    // =========================================

    private Uri photoUri;


    // =========================================
    // CAMERA RESULT
    // =========================================

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {

                        if (success && photoUri != null) {

                            processCapturedImage();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Photo capture cancelled",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });


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


    // =========================================
    // ON CREATE
    // =========================================

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
        // CATEGORY SPINNER
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
        // CHECK EDIT MODE
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

        btnScanReceipt.setOnClickListener(
                view -> checkCameraPermission()
        );


        // =========================================
        // SAVE / UPDATE
        // =========================================

        btnSaveTransaction.setOnClickListener(
                view -> {

                    if (transactionId != -1) {

                        updateTransaction();

                    } else {

                        saveTransaction();
                    }
                }
        );
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
    // OPEN FULL-RESOLUTION CAMERA
    // =====================================================

    private void openCamera() {

        try {

            File photoFile =
                    createImageFile();

            photoUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
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
    // CREATE IMAGE FILE
    // =====================================================

    private File createImageFile()
            throws IOException {

        String timeStamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                ).format(new Date());


        String imageFileName =
                "RECEIPT_"
                        + timeStamp
                        + "_";


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
    // PROCESS CAPTURED IMAGE
    // =====================================================

    private void processCapturedImage() {

        if (photoUri == null) {
            return;
        }


        Toast.makeText(
                this,
                "Reading receipt...",
                Toast.LENGTH_SHORT
        ).show();


        executorService.execute(() -> {

            try {

                Bitmap bitmap =
                        BitmapFactory.decodeStream(
                                getContentResolver()
                                        .openInputStream(photoUri)
                        );


                if (bitmap == null) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "Could not read image",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }


                // Improve image for OCR
                Bitmap processedBitmap =
                        enhanceForOCR(bitmap);


                runOCR(processedBitmap);


            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Could not process image",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }
        });
    }


    // =====================================================
    // IMAGE ENHANCEMENT
    // =====================================================

    private Bitmap enhanceForOCR(Bitmap original) {

        Bitmap result =
                Bitmap.createBitmap(
                        original.getWidth(),
                        original.getHeight(),
                        Bitmap.Config.ARGB_8888
                );


        Canvas canvas =
                new Canvas(result);


        Paint paint =
                new Paint();


        ColorMatrix colorMatrix =
                new ColorMatrix();


        // Convert to grayscale
        colorMatrix.setSaturation(0);


        paint.setColorFilter(
                new ColorMatrixColorFilter(
                        colorMatrix
                )
        );


        canvas.drawBitmap(
                original,
                0,
                0,
                paint
        );


        return result;
    }


    // =====================================================
    // OCR
    // =====================================================

    private void runOCR(Bitmap bitmap) {

        InputImage image =
                InputImage.fromBitmap(
                        bitmap,
                        0
                );


        TextRecognizer recognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                );


        recognizer.process(image)

                .addOnSuccessListener(
                        result -> {

                            processOCRText(result);

                            recognizer.close();
                        }
                )

                .addOnFailureListener(
                        error -> {

                            Toast.makeText(
                                    this,
                                    "Could not read receipt",
                                    Toast.LENGTH_SHORT
                            ).show();

                            recognizer.close();
                        }
                );
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
                            + detectedAmount,
                    Toast.LENGTH_SHORT
            );

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


        // =========================================
        // CATEGORY
        // =========================================

        String category =
                detectCategory(text);


        selectCategory(category);


        // =========================================
        // TYPE
        // =========================================

        String type =
                detectTransactionType(text);


        if ("Income".equals(type)) {

            radioIncome.setChecked(true);

        } else {

            radioExpense.setChecked(true);
        }
    }


    // =====================================================
    // EXTRACT AMOUNT
    // =====================================================
    private Double extractAmount(String text) {

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String cleanText = text
                .replace("₹", " Rs ")
                .replace("रु", " Rs ")
                .replace("Rs.", " Rs ")
                .replace("rs.", " Rs ")
                .replace("RS", " Rs ")
                .replace("—", "-")
                .replace("–", "-");

        // =========================================
        // 1. AMOUNT LABEL
        // Examples:
        // Amount: ₹8000
        // Amount ₹8000
        // Amount: 8000
        // =========================================

        Pattern amountLabelPattern = Pattern.compile(
                "(?i)(amount|total\\s*amount|grand\\s*total|total)"
                        + "\\s*[:=\\-]?\\s*"
                        + "(?:rs\\s*)?"
                        + "₹?\\s*"
                        + "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
        );

        Matcher amountLabelMatcher =
                amountLabelPattern.matcher(cleanText);

        while (amountLabelMatcher.find()) {

            try {

                String number =
                        amountLabelMatcher.group(2)
                                .replace(",", "");

                double value =
                        Double.parseDouble(number);

                if (value > 0) {
                    return value;
                }

            } catch (Exception ignored) {
            }
        }


        // =========================================
        // 2. ₹ / Rs / INR FOLLOWED BY NUMBER
        // =========================================

        Pattern currencyPattern = Pattern.compile(
                "(?i)(?:₹|rs\\.?|inr)\\s*"
                        + "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
        );

        Matcher currencyMatcher =
                currencyPattern.matcher(cleanText);

        Double largestAmount = null;

        while (currencyMatcher.find()) {

            try {

                String number =
                        currencyMatcher.group(1)
                                .replace(",", "");

                double value =
                        Double.parseDouble(number);

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
        // 3. INTEGER / DECIMAL NUMBERS
        // IMPORTANT FOR HANDWRITTEN RECEIPTS
        //
        // OCR might return:
        // Amount
        // 8000
        //
        // instead of:
        // Amount ₹8000
        // =========================================

        Pattern numberPattern = Pattern.compile(
                "\\b([0-9]{2,}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)\\b"
        );

        Matcher numberMatcher =
                numberPattern.matcher(cleanText);

        while (numberMatcher.find()) {

            try {

                String number =
                        numberMatcher.group(1)
                                .replace(",", "");

                double value =
                        Double.parseDouble(number);

                // Ignore very small numbers such as dates
                if (value >= 10 &&
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
    // DESCRIPTION
    // =====================================================

    private String extractDescription(String text) {

        if (text == null ||
                text.trim().isEmpty()) {

            return null;
        }


        String[] lines =
                text.split("\\r?\\n");


        // =========================================
        // DESCRIPTION LABEL
        // =========================================

        for (String line : lines) {

            String clean =
                    line.trim();


            if (clean.toLowerCase(
                    Locale.getDefault()
            ).startsWith("description")) {


                String description =
                        clean.replaceFirst(
                                "(?i)description"
                                        + "\\s*[:=\\-]?\\s*",
                                ""
                        ).trim();


                if (!description.isEmpty()) {

                    return description;
                }
            }
        }


        // =========================================
        // NORMAL TEXT FALLBACK
        // =========================================

        for (String line : lines) {

            String clean =
                    line.trim();


            String lower =
                    clean.toLowerCase(
                            Locale.getDefault()
                    );


            if (clean.length() >= 3
                    && clean.length() <= 50
                    && !lower.contains("amount")
                    && !lower.contains("category")
                    && !lower.contains("type")
                    && !lower.contains("total")
                    && !clean.matches(
                    ".*\\d{3,}.*"
            )) {

                return clean;
            }
        }


        return null;
    }


    // =====================================================
    // DETECT CATEGORY
    // =====================================================

    private String detectCategory(String text) {

        String lower =
                text.toLowerCase(
                        Locale.getDefault()
                );


        if (lower.contains("food")
                || lower.contains("restaurant")
                || lower.contains("lunch")
                || lower.contains("dinner")
                || lower.contains("breakfast")) {

            return "Food";
        }


        if (lower.contains("travel")
                || lower.contains("taxi")
                || lower.contains("uber")
                || lower.contains("bus")
                || lower.contains("train")
                || lower.contains("flight")) {

            return "Travel";
        }


        if (lower.contains("shopping")
                || lower.contains("shirt")
                || lower.contains("clothes")
                || lower.contains("amazon")) {

            return "Shopping";
        }


        if (lower.contains("bill")
                || lower.contains("electricity")
                || lower.contains("electric")
                || lower.contains("recharge")
                || lower.contains("internet")) {

            return "Bills";
        }


        if (lower.contains("movie")
                || lower.contains("game")
                || lower.contains("entertainment")) {

            return "Entertainment";
        }


        if (lower.contains("doctor")
                || lower.contains("medicine")
                || lower.contains("hospital")
                || lower.contains("health")) {

            return "Health";
        }


        if (lower.contains("college")
                || lower.contains("course")
                || lower.contains("education")
                || lower.contains("book")) {

            return "Education";
        }


        if (lower.contains("salary")
                || lower.contains("income")
                || lower.contains("credited")) {

            return "Salary";
        }


        return "Other";
    }


    // =====================================================
    // SELECT CATEGORY
    // =====================================================

    private void selectCategory(String category) {

        if (category == null) {
            return;
        }


        ArrayAdapter<?> adapter =
                (ArrayAdapter<?>)
                        spinnerCategory.getAdapter();


        for (int i = 0;
             i < adapter.getCount();
             i++) {


            if (category.equalsIgnoreCase(
                    adapter.getItem(i).toString()
            )) {

                spinnerCategory.setSelection(i);

                return;
            }
        }
    }


    // =====================================================
    // DETECT TRANSACTION TYPE
    // =====================================================

    private String detectTransactionType(
            String text
    ) {

        String lower =
                text.toLowerCase(
                        Locale.getDefault()
                );


        if (lower.contains("income")
                || lower.contains("salary")
                || lower.contains("credited")
                || lower.contains("received")) {

            return "Income";
        }


        return "Expense";
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
                                existingTransaction
                                        .getAmount()
                        )
                );


                etDescription.setText(
                        existingTransaction
                                .getDescription()
                );


                if ("Income".equalsIgnoreCase(
                        existingTransaction.getType()
                )) {

                    radioIncome.setChecked(true);

                } else {

                    radioExpense.setChecked(true);
                }


                selectCategory(
                        existingTransaction
                                .getCategory()
                );
            });
        });
    }


    // =====================================================
    // SAVE TRANSACTION
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
    // UPDATE TRANSACTION
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


        // Preserve ID
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