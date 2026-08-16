package com.example.expensetracker;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsActivity extends AppCompatActivity {

    private PieChart pieChart;
    private TextView tvTopCategory;
    private MaterialButton btnBack;

    private TransactionDao transactionDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        pieChart = findViewById(R.id.pieChart);
        tvTopCategory = findViewById(R.id.tvTopCategory);
        btnBack = findViewById(R.id.btnBack);

        transactionDao = AppDatabase.getInstance(this).transactionDao();

        setupPieChart();
        loadAnalyticsData();

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setCenterText("Expenses");
        pieChart.setCenterTextSize(18f);
    }

    private void loadAnalyticsData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        executorService.execute(() -> {
            List<Transaction> transactions = transactionDao.getAllTransactions(uid);
            
            // Map to store Category -> Total Amount
            Map<String, Double> categoryMap = new HashMap<>();
            
            for (Transaction t : transactions) {
                if ("Expense".equalsIgnoreCase(t.getType())) {
                    String category = t.getCategory();
                    double amount = t.getAmount();
                    categoryMap.put(category, categoryMap.getOrDefault(category, 0.0) + amount);
                }
            }

            if (categoryMap.isEmpty()) {
                runOnUiThread(() -> {
                    tvTopCategory.setText("No expense data available");
                    pieChart.setNoDataText("Add expenses to see analytics");
                    pieChart.invalidate();
                });
                return;
            }

            // Find top category
            String topCategory = "";
            double maxAmount = -1;
            ArrayList<PieEntry> entries = new ArrayList<>();

            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
                if (entry.getValue() > maxAmount) {
                    maxAmount = entry.getValue();
                    topCategory = entry.getKey();
                }
            }

            String finalTopCategory = topCategory;

            runOnUiThread(() -> {
                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                dataSet.setSliceSpace(3f);
                dataSet.setSelectionShift(5f);
                dataSet.setValueTextSize(12f);
                dataSet.setValueTextColor(Color.BLACK);

                PieData data = new PieData(dataSet);
                pieChart.setData(data);
                pieChart.animateY(1000);
                pieChart.invalidate();

                tvTopCategory.setText(finalTopCategory);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
