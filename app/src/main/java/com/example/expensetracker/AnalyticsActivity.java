package com.example.expensetracker;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;
import com.google.android.material.button.MaterialButton;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private TextView tvTotalBalance;
    private TextView tvIncome;
    private TextView tvExpense;
    private TextView tvSavings;
    private TextView tvSavingsRate;

    private TextView tvIncomeChange;
    private TextView tvExpenseChange;

    private TextView tvTopCategory;
    private TextView tvTopCategoryAmount;
    private TextView tvAverageDaily;

    private TextView tvSavingsInsight;
    private TextView tvCategoryInsight;
    private TextView tvSpendingInsight;

    private TextView tvNoCategoryData;
    private TextView tvNoTrendData;

    private TextView tvTrendTitle;

    private TextView tvHeatmapTitle;
    private TextView tvHeatmapSummary;

    private LinearLayout categoryLegendContainer;
    private LinearLayout heatmapContainer;

    private PieChart pieChart;
    private LineChart lineChart;

    private Spinner spinnerPeriod;

    private MaterialButton btnExpenseTrend;
    private MaterialButton btnIncomeTrend;
    private MaterialButton btnBack;

    // =========================================================
    // DATABASE
    // =========================================================

    private TransactionDao transactionDao;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private List<Transaction> allTransactions =
            new ArrayList<>();

    // =========================================================
    // SETTINGS
    // =========================================================

    private boolean showIncomeTrend = false;

    private static final String DATE_FORMAT =
            "dd-MM-yyyy";

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    DATE_FORMAT,
                    Locale.getDefault()
            );

    // =========================================================
    // COLORS
    // =========================================================

    private static final int PURPLE =
            Color.rgb(126, 87, 194);

    private static final int GREEN =
            Color.rgb(46, 125, 50);

    private static final int HEAT_0 =
            Color.rgb(242, 239, 250);

    private static final int HEAT_1 =
            Color.rgb(222, 211, 241);

    private static final int HEAT_2 =
            Color.rgb(194, 172, 222);

    private static final int HEAT_3 =
            Color.rgb(159, 128, 199);

    private static final int HEAT_4 =
            Color.rgb(126, 87, 194);

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_analytics
        );

        initializeViews();

        transactionDao =
                AppDatabase
                        .getInstance(this)
                        .transactionDao();

        setupSpinner();
        setupPieChart();
        setupLineChart();
        setupButtons();

        loadTransactions();
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        tvTotalBalance =
                findViewById(R.id.tvTotalBalance);

        tvIncome =
                findViewById(R.id.tvIncome);

        tvExpense =
                findViewById(R.id.tvExpense);

        tvSavings =
                findViewById(R.id.tvSavings);

        tvSavingsRate =
                findViewById(R.id.tvSavingsRate);

        tvIncomeChange =
                findViewById(R.id.tvIncomeChange);

        tvExpenseChange =
                findViewById(R.id.tvExpenseChange);

        tvTopCategory =
                findViewById(R.id.tvTopCategory);

        tvTopCategoryAmount =
                findViewById(R.id.tvTopCategoryAmount);

        tvAverageDaily =
                findViewById(R.id.tvAverageDaily);

        tvSavingsInsight =
                findViewById(R.id.tvSavingsInsight);

        tvCategoryInsight =
                findViewById(R.id.tvCategoryInsight);

        tvSpendingInsight =
                findViewById(R.id.tvSpendingInsight);

        tvNoCategoryData =
                findViewById(R.id.tvNoCategoryData);

        tvNoTrendData =
                findViewById(R.id.tvNoTrendData);

        tvTrendTitle =
                findViewById(R.id.tvTrendTitle);

        categoryLegendContainer =
                findViewById(
                        R.id.categoryLegendContainer
                );

        pieChart =
                findViewById(
                        R.id.pieChart
                );

        lineChart =
                findViewById(
                        R.id.lineChart
                );

        spinnerPeriod =
                findViewById(
                        R.id.spinnerPeriod
                );

        btnExpenseTrend =
                findViewById(
                        R.id.btnExpenseTrend
                );

        btnIncomeTrend =
                findViewById(
                        R.id.btnIncomeTrend
                );

        btnBack =
                findViewById(
                        R.id.btnBack
                );

        // Heatmap
        tvHeatmapTitle =
                findViewById(
                        R.id.tvHeatmapTitle
                );

        tvHeatmapSummary =
                findViewById(
                        R.id.tvHeatmapSummary
                );

        heatmapContainer =
                findViewById(
                        R.id.heatmapContainer
                );
    }

    // =========================================================
    // SPINNER
    // =========================================================

    private void setupSpinner() {

        String[] periods = {
                "This Month",
                "Last Month",
                "Last 3 Months",
                "This Year"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        periods
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerPeriod.setAdapter(adapter);

        spinnerPeriod.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        loadAnalytics();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        btnBack.setOnClickListener(
                v -> finish()
        );

        btnExpenseTrend.setOnClickListener(
                v -> {
                    showIncomeTrend = false;
                    updateTrendButtons();
                    loadAnalytics();
                }
        );

        btnIncomeTrend.setOnClickListener(
                v -> {
                    showIncomeTrend = true;
                    updateTrendButtons();
                    loadAnalytics();
                }
        );

        updateTrendButtons();
    }

    private void updateTrendButtons() {

        if (showIncomeTrend) {

            btnIncomeTrend.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            PURPLE
                    )
            );

            btnIncomeTrend.setTextColor(
                    Color.WHITE
            );

            btnExpenseTrend.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.WHITE
                    )
            );

            btnExpenseTrend.setTextColor(
                    Color.DKGRAY
            );

        } else {

            btnExpenseTrend.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            PURPLE
                    )
            );

            btnExpenseTrend.setTextColor(
                    Color.WHITE
            );

            btnIncomeTrend.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.WHITE
                    )
            );

            btnIncomeTrend.setTextColor(
                    Color.DKGRAY
            );
        }
    }

    // =========================================================
    // DATABASE
    // =========================================================

    private void loadTransactions() {

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (user == null) {
            return;
        }

        String uid =
                user.getUid();

        executor.execute(() -> {

            List<Transaction> result =
                    transactionDao
                            .getAllTransactions(uid);

            if (result == null) {
                result =
                        new ArrayList<>();
            }

            allTransactions = result;

            runOnUiThread(
                    this::loadAnalytics
            );
        });
    }

    // =========================================================
    // ANALYTICS
    // =========================================================

    private void loadAnalytics() {

        if (allTransactions == null) {
            return;
        }

        Period current =
                getSelectedPeriod();

        Period previous =
                getPreviousPeriod(current);

        List<Transaction> currentTransactions =
                filterTransactions(
                        current.start,
                        current.end
                );

        List<Transaction> previousTransactions =
                filterTransactions(
                        previous.start,
                        previous.end
                );

        calculateOverview(
                currentTransactions,
                previousTransactions
        );

        calculateCategories(
                currentTransactions
        );

        calculateTrend(
                currentTransactions
        );

        calculateInsights(
                currentTransactions,
                previousTransactions
        );

        // NEW
        buildHeatmap();
    }

    // =========================================================
    // OVERVIEW
    // =========================================================

    private void calculateOverview(
            List<Transaction> current,
            List<Transaction> previous
    ) {

        double income = 0;
        double expense = 0;

        double previousIncome = 0;
        double previousExpense = 0;

        for (Transaction transaction : current) {

            if (isIncome(transaction)) {
                income += transaction.getAmount();
            }

            if (isExpense(transaction)) {
                expense += transaction.getAmount();
            }
        }

        for (Transaction transaction : previous) {

            if (isIncome(transaction)) {
                previousIncome += transaction.getAmount();
            }

            if (isExpense(transaction)) {
                previousExpense += transaction.getAmount();
            }
        }

        double balance =
                income - expense;

        double savingsRate =
                income > 0
                        ? (balance / income) * 100
                        : 0;

        tvTotalBalance.setText(
                formatCurrency(balance)
        );

        tvIncome.setText(
                formatCurrency(income)
        );

        tvExpense.setText(
                formatCurrency(expense)
        );

        tvSavings.setText(
                formatCurrency(balance)
        );

        tvSavingsRate.setText(
                String.format(
                        Locale.getDefault(),
                        "%.1f%% of income",
                        savingsRate
                )
        );

        tvIncomeChange.setText(
                createChangeText(
                        previousIncome,
                        income
                )
        );

        tvExpenseChange.setText(
                createChangeText(
                        previousExpense,
                        expense
                )
        );

        tvAverageDaily.setText(
                "Avg. daily expense: "
                        + formatCurrency(
                        calculateAverageDaily(
                                current
                        )
                )
        );
    }

    // =========================================================
    // CATEGORY ANALYTICS
    // =========================================================

    private void calculateCategories(
            List<Transaction> transactions
    ) {

        Map<String, Double> categoryMap =
                new HashMap<>();

        double totalExpense = 0;

        for (Transaction transaction : transactions) {

            if (!isExpense(transaction)) {
                continue;
            }

            String category =
                    transaction.getCategory();

            if (category == null ||
                    category.trim().isEmpty()) {

                category = "Other";
            }

            double amount =
                    transaction.getAmount();

            totalExpense += amount;

            categoryMap.put(
                    category,
                    categoryMap.getOrDefault(
                            category,
                            0.0
                    ) + amount
            );
        }

        if (categoryMap.isEmpty()) {

            pieChart.clear();

            pieChart.setCenterText(
                    "No data"
            );

            categoryLegendContainer.removeAllViews();

            tvNoCategoryData.setVisibility(
                    View.VISIBLE
            );

            tvTopCategory.setText(
                    "No data"
            );

            tvTopCategoryAmount.setText(
                    "Add expenses"
            );

            return;
        }

        tvNoCategoryData.setVisibility(
                View.GONE
        );

        List<Map.Entry<String, Double>> sorted =
                new ArrayList<>(
                        categoryMap.entrySet()
                );

        Collections.sort(
                sorted,
                (a, b) ->
                        Double.compare(
                                b.getValue(),
                                a.getValue()
                        )
        );

        String topCategory =
                sorted.get(0).getKey();

        double topAmount =
                sorted.get(0).getValue();

        ArrayList<PieEntry> entries =
                new ArrayList<>();

        for (Map.Entry<String, Double> entry : sorted) {

            entries.add(
                    new PieEntry(
                            entry.getValue().floatValue(),
                            entry.getKey()
                    )
            );
        }

        PieDataSet dataSet =
                new PieDataSet(
                        entries,
                        ""
                );

        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);

        ArrayList<Integer> colors =
                new ArrayList<>();

        colors.add(Color.rgb(126, 87, 194));
        colors.add(Color.rgb(66, 133, 244));
        colors.add(Color.rgb(52, 168, 83));
        colors.add(Color.rgb(251, 188, 4));
        colors.add(Color.rgb(234, 67, 53));
        colors.add(Color.rgb(156, 39, 176));
        colors.add(Color.rgb(0, 150, 136));
        colors.add(Color.rgb(255, 112, 67));

        dataSet.setColors(colors);

        pieChart.setData(
                new PieData(dataSet)
        );

        pieChart.setCenterText(
                formatCompactCurrency(
                        totalExpense
                )
                        + "\nTotal"
        );

        pieChart.animateY(600);
        pieChart.invalidate();

        tvTopCategory.setText(
                topCategory
        );

        tvTopCategoryAmount.setText(
                formatCurrency(topAmount)
                        + " spent"
        );

        buildCategoryLegend(
                sorted,
                totalExpense
        );
    }

    private void buildCategoryLegend(
            List<Map.Entry<String, Double>> categories,
            double total
    ) {

        categoryLegendContainer.removeAllViews();

        int limit =
                Math.min(
                        categories.size(),
                        8
                );

        for (int i = 0; i < limit; i++) {

            String category =
                    categories.get(i).getKey();

            double amount =
                    categories.get(i).getValue();

            double percentage =
                    total > 0
                            ? amount / total * 100
                            : 0;

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            row.setPadding(
                    0,
                    6,
                    0,
                    6
            );

            TextView dot =
                    new TextView(this);

            dot.setText("●");
            dot.setTextSize(16);
            dot.setTextColor(
                    getCategoryColor(i)
            );

            row.addView(
                    dot,
                    new LinearLayout.LayoutParams(
                            30,
                            -2
                    )
            );

            TextView name =
                    new TextView(this);

            name.setText(category);
            name.setTextSize(14);
            name.setTextColor(
                    Color.rgb(50, 50, 50)
            );

            row.addView(
                    name,
                    new LinearLayout.LayoutParams(
                            0,
                            -2,
                            1
                    )
            );

            TextView value =
                    new TextView(this);

            value.setText(
                    formatCurrency(amount)
                            + "  "
                            + String.format(
                            Locale.getDefault(),
                            "%.0f%%",
                            percentage
                    )
            );

            value.setTextSize(13);
            value.setTextColor(
                    Color.GRAY
            );

            row.addView(value);

            categoryLegendContainer.addView(
                    row
            );
        }
    }

    private int getCategoryColor(int index) {

        int[] colors = {
                Color.rgb(126, 87, 194),
                Color.rgb(66, 133, 244),
                Color.rgb(52, 168, 83),
                Color.rgb(251, 188, 4),
                Color.rgb(234, 67, 53),
                Color.rgb(156, 39, 176),
                Color.rgb(0, 150, 136),
                Color.rgb(255, 112, 67)
        };

        return colors[
                index % colors.length
                ];
    }

    // =========================================================
    // PIE CHART
    // =========================================================

    private void setupPieChart() {

        pieChart
                .getDescription()
                .setEnabled(false);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(63f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getLegend().setEnabled(false);
    }

    // =========================================================
    // LINE CHART
    // =========================================================

    private void setupLineChart() {

        lineChart
                .getDescription()
                .setEnabled(false);

        lineChart
                .getLegend()
                .setEnabled(false);

        lineChart
                .getXAxis()
                .setPosition(
                        XAxis.XAxisPosition.BOTTOM
                );

        lineChart
                .getAxisRight()
                .setEnabled(false);

        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
    }

    private void calculateTrend(
            List<Transaction> transactions
    ) {

        int selected =
                spinnerPeriod
                        .getSelectedItemPosition();

        if (selected == 0 ||
                selected == 1) {

            tvTrendTitle.setText(
                    "Daily Trend"
            );

            buildDailyTrend(
                    transactions
            );

        } else {

            tvTrendTitle.setText(
                    "Monthly Trend"
            );

            buildMonthlyTrend(
                    transactions
            );
        }
    }

    private void buildDailyTrend(
            List<Transaction> transactions
    ) {

        Period period =
                getSelectedPeriod();

        ArrayList<Entry> entries =
                new ArrayList<>();

        ArrayList<String> labels =
                new ArrayList<>();

        Calendar cursor =
                (Calendar) period.start.clone();

        int index = 0;

        while (!cursor.after(period.end)) {

            Calendar day =
                    (Calendar) cursor.clone();

            double amount = 0;

            for (Transaction transaction :
                    transactions) {

                Date date =
                        parseDate(
                                transaction.getDate()
                        );

                if (date == null) {
                    continue;
                }

                Calendar transactionDate =
                        Calendar.getInstance();

                transactionDate.setTime(date);

                if (isSameDay(
                        transactionDate,
                        day
                )) {

                    if (showIncomeTrend &&
                            isIncome(transaction)) {

                        amount +=
                                transaction.getAmount();
                    }

                    if (!showIncomeTrend &&
                            isExpense(transaction)) {

                        amount +=
                                transaction.getAmount();
                    }
                }
            }

            entries.add(
                    new Entry(
                            index,
                            (float) amount
                    )
            );

            labels.add(
                    new SimpleDateFormat(
                            "d MMM",
                            Locale.getDefault()
                    ).format(
                            day.getTime()
                    )
            );

            index++;

            cursor.add(
                    Calendar.DAY_OF_MONTH,
                    1
            );
        }

        renderLineChart(
                entries,
                labels
        );
    }

    private void buildMonthlyTrend(
            List<Transaction> transactions
    ) {

        Period period =
                getSelectedPeriod();

        ArrayList<Entry> entries =
                new ArrayList<>();

        ArrayList<String> labels =
                new ArrayList<>();

        Calendar cursor =
                (Calendar) period.start.clone();

        cursor.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        Calendar end =
                (Calendar) period.end.clone();

        end.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        int index = 0;

        while (!cursor.after(end)) {

            int month =
                    cursor.get(Calendar.MONTH);

            int year =
                    cursor.get(Calendar.YEAR);

            double amount = 0;

            for (Transaction transaction :
                    transactions) {

                Date date =
                        parseDate(
                                transaction.getDate()
                        );

                if (date == null) {
                    continue;
                }

                Calendar transactionDate =
                        Calendar.getInstance();

                transactionDate.setTime(date);

                boolean sameMonth =
                        transactionDate.get(
                                Calendar.MONTH
                        ) == month
                                &&
                                transactionDate.get(
                                        Calendar.YEAR
                                ) == year;

                if (!sameMonth) {
                    continue;
                }

                if (showIncomeTrend &&
                        isIncome(transaction)) {

                    amount +=
                            transaction.getAmount();
                }

                if (!showIncomeTrend &&
                        isExpense(transaction)) {

                    amount +=
                            transaction.getAmount();
                }
            }

            entries.add(
                    new Entry(
                            index,
                            (float) amount
                    )
            );

            labels.add(
                    new SimpleDateFormat(
                            "MMM",
                            Locale.getDefault()
                    ).format(
                            cursor.getTime()
                    )
            );

            index++;

            cursor.add(
                    Calendar.MONTH,
                    1
            );
        }

        renderLineChart(
                entries,
                labels
        );
    }

    private void renderLineChart(
            ArrayList<Entry> entries,
            ArrayList<String> labels
    ) {

        boolean hasData = false;

        for (Entry entry : entries) {

            if (entry.getY() > 0) {
                hasData = true;
                break;
            }
        }

        if (!hasData) {

            lineChart.clear();

            tvNoTrendData.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        tvNoTrendData.setVisibility(
                View.GONE
        );

        int color =
                showIncomeTrend
                        ? GREEN
                        : PURPLE;

        LineDataSet dataSet =
                new LineDataSet(
                        entries,
                        showIncomeTrend
                                ? "Income"
                                : "Expense"
                );

        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(3.5f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
        dataSet.setMode(
                LineDataSet.Mode.CUBIC_BEZIER
        );

        lineChart.setData(
                new LineData(dataSet)
        );

        lineChart
                .getXAxis()
                .setValueFormatter(
                        new IndexAxisValueFormatter(
                                labels
                        )
                );

        lineChart
                .getXAxis()
                .setGranularity(1f);

        lineChart
                .getXAxis()
                .setDrawGridLines(false);

        lineChart
                .getAxisLeft()
                .setValueFormatter(
                        new ValueFormatter() {

                            @Override
                            public String getFormattedValue(
                                    float value
                            ) {

                                return formatChartValue(
                                        value
                                );
                            }
                        }
                );

        lineChart.animateX(500);
        lineChart.invalidate();
    }

    // =========================================================
    // 🔥 EXPENSE HEATMAP
    // =========================================================

    private void buildHeatmap() {

        heatmapContainer.removeAllViews();

        int selected =
                spinnerPeriod
                        .getSelectedItemPosition();

        if (selected == 3) {

            buildYearHeatmap();

        } else if (selected == 2) {

            buildThreeMonthHeatmap();

        } else {

            buildMonthHeatmap(
                    getSelectedPeriod()
            );
        }
    }

    // =========================================================
    // MONTH HEATMAP
    // =========================================================

    private void buildMonthHeatmap(
            Period period
    ) {

        tvHeatmapTitle.setText(
                "Expense Heatmap"
        );

        tvHeatmapSummary.setText(
                "Daily spending intensity"
        );

        Map<String, Double> daily =
                getDailyExpenseMap(period);

        double max =
                getMaxValue(daily);

        Calendar first =
                (Calendar) period.start.clone();

        first.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        int firstDay =
                first.get(Calendar.DAY_OF_WEEK);

        int days =
                first.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                );

        addWeekHeader();

        LinearLayout row = null;

        int position = 0;

        // Empty cells before day 1
        int offset = firstDay - 1;

        for (int i = 0; i < offset; i++) {

            if (position % 7 == 0) {

                row =
                        createHeatmapRow();

                heatmapContainer.addView(row);
            }

            addEmptyCell(row);

            position++;
        }

        for (int dayNumber = 1;
             dayNumber <= days;
             dayNumber++) {

            if (position % 7 == 0) {

                row =
                        createHeatmapRow();

                heatmapContainer.addView(row);
            }

            Calendar day =
                    (Calendar) first.clone();

            day.set(
                    Calendar.DAY_OF_MONTH,
                    dayNumber
            );

            String key =
                    getDateKey(day);

            double amount =
                    daily.getOrDefault(
                            key,
                            0.0
                    );

            row.addView(
                    createHeatCell(
                            day,
                            amount,
                            max
                    )
            );

            position++;
        }

        while (position % 7 != 0) {

            addEmptyCell(row);

            position++;
        }

        addHeatmapLegend();
    }

    // =========================================================
    // THREE MONTH HEATMAP
    // =========================================================

    private void buildThreeMonthHeatmap() {

        tvHeatmapTitle.setText(
                "3-Month Expense Heatmap"
        );

        tvHeatmapSummary.setText(
                "Daily spending across recent months"
        );

        Calendar end =
                Calendar.getInstance();

        Calendar start =
                (Calendar) end.clone();

        start.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        start.add(
                Calendar.MONTH,
                -2
        );

        Period period =
                new Period(
                        start,
                        end
                );

        Map<String, Double> daily =
                getDailyExpenseMap(period);

        double max =
                getMaxValue(daily);

        addWeekHeader();

        LinearLayout row = null;

        int position = 0;

        Calendar cursor =
                (Calendar) start.clone();

        while (!cursor.after(end)) {

            if (position % 7 == 0) {

                row =
                        createHeatmapRow();

                heatmapContainer.addView(row);
            }

            double amount =
                    daily.getOrDefault(
                            getDateKey(cursor),
                            0.0
                    );

            row.addView(
                    createHeatCell(
                            cursor,
                            amount,
                            max
                    )
            );

            position++;

            cursor.add(
                    Calendar.DAY_OF_MONTH,
                    1
            );
        }

        addHeatmapLegend();
    }

    // =========================================================
    // YEAR HEATMAP
    // =========================================================

    private void buildYearHeatmap() {

        tvHeatmapTitle.setText(
                "Yearly Expense Heatmap"
        );

        tvHeatmapSummary.setText(
                "Monthly spending intensity"
        );

        Calendar start =
                Calendar.getInstance();

        start.set(
                Calendar.MONTH,
                Calendar.JANUARY
        );

        start.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        Calendar end =
                (Calendar) start.clone();

        end.set(
                Calendar.MONTH,
                Calendar.DECEMBER
        );

        end.set(
                Calendar.DAY_OF_MONTH,
                31
        );

        Period period =
                new Period(
                        start,
                        end
                );

        Map<String, Double> monthly =
                getMonthlyExpenseMap(period);

        double max =
                getMaxValue(monthly);

        LinearLayout row =
                createHeatmapRow();

        heatmapContainer.addView(row);

        Calendar cursor =
                (Calendar) start.clone();

        for (int i = 0; i < 12; i++) {

            double amount =
                    monthly.getOrDefault(
                            getMonthKey(cursor),
                            0.0
                    );

            row.addView(
                    createMonthHeatCell(
                            cursor,
                            amount,
                            max
                    )
            );

            cursor.add(
                    Calendar.MONTH,
                    1
            );
        }

        addHeatmapLegend();
    }

    // =========================================================
    // DAILY MAP
    // =========================================================

    private Map<String, Double>
    getDailyExpenseMap(
            Period period
    ) {

        Map<String, Double> map =
                new HashMap<>();

        for (Transaction transaction :
                allTransactions) {

            if (!isExpense(transaction)) {
                continue;
            }

            Date date =
                    parseDate(
                            transaction.getDate()
                    );

            if (date == null) {
                continue;
            }

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(date);

            if (calendar.before(period.start) ||
                    calendar.after(period.end)) {
                continue;
            }

            String key =
                    getDateKey(calendar);

            map.put(
                    key,
                    map.getOrDefault(
                            key,
                            0.0
                    )
                            +
                            transaction.getAmount()
            );
        }

        return map;
    }

    // =========================================================
    // MONTHLY MAP
    // =========================================================

    private Map<String, Double>
    getMonthlyExpenseMap(
            Period period
    ) {

        Map<String, Double> map =
                new HashMap<>();

        for (Transaction transaction :
                allTransactions) {

            if (!isExpense(transaction)) {
                continue;
            }

            Date date =
                    parseDate(
                            transaction.getDate()
                    );

            if (date == null) {
                continue;
            }

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(date);

            if (calendar.before(period.start) ||
                    calendar.after(period.end)) {
                continue;
            }

            String key =
                    getMonthKey(calendar);

            map.put(
                    key,
                    map.getOrDefault(
                            key,
                            0.0
                    )
                            +
                            transaction.getAmount()
            );
        }

        return map;
    }

    // =========================================================
    // HEATMAP CELL
    // =========================================================

    private TextView createHeatCell(
            Calendar date,
            double amount,
            double max
    ) {

        TextView cell =
                new TextView(this);

        cell.setText(
                String.valueOf(
                        date.get(
                                Calendar.DAY_OF_MONTH
                        )
                )
        );

        cell.setGravity(
                Gravity.CENTER
        );

        cell.setTextSize(11);

        cell.setTypeface(
                null,
                Typeface.BOLD
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                getHeatColor(
                        amount,
                        max
                )
        );

        background.setCornerRadius(
                10f
        );

        cell.setBackground(
                background
        );

        cell.setTextColor(
                amount > 0
                        ? Color.WHITE
                        : Color.rgb(
                        105,
                        100,
                        115
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        42,
                        42
                );

        params.setMargins(
                3,
                3,
                3,
                3
        );

        cell.setLayoutParams(params);

        Calendar clickedDate =
                (Calendar) date.clone();

        cell.setOnClickListener(
                v ->
                        showDayDialog(
                                clickedDate,
                                amount
                        )
        );

        return cell;
    }

    // =========================================================
    // MONTH CELL
    // =========================================================

    private TextView createMonthHeatCell(
            Calendar month,
            double amount,
            double max
    ) {

        TextView cell =
                new TextView(this);

        cell.setText(
                new SimpleDateFormat(
                        "MMM",
                        Locale.getDefault()
                ).format(
                        month.getTime()
                )
        );

        cell.setGravity(
                Gravity.CENTER
        );

        cell.setTextSize(12);

        cell.setTypeface(
                null,
                Typeface.BOLD
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                getHeatColor(
                        amount,
                        max
                )
        );

        background.setCornerRadius(
                12f
        );

        cell.setBackground(
                background
        );

        cell.setTextColor(
                amount > 0
                        ? Color.WHITE
                        : Color.rgb(
                        100,
                        95,
                        110
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        58,
                        58
                );

        params.setMargins(
                4,
                4,
                4,
                4
        );

        cell.setLayoutParams(params);

        Calendar clicked =
                (Calendar) month.clone();

        cell.setOnClickListener(
                v ->
                        showMonthDialog(
                                clicked,
                                amount
                        )
        );

        return cell;
    }

    // =========================================================
    // EMPTY CELL
    // =========================================================

    private void addEmptyCell(
            LinearLayout row
    ) {

        TextView empty =
                new TextView(this);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        42,
                        42
                );

        params.setMargins(
                3,
                3,
                3,
                3
        );

        empty.setLayoutParams(params);

        row.addView(empty);
    }

    // =========================================================
    // HEADER
    // =========================================================

    private void addWeekHeader() {

        LinearLayout row =
                createHeatmapRow();

        String[] days = {
                "S",
                "M",
                "T",
                "W",
                "T",
                "F",
                "S"
        };

        for (String day : days) {

            TextView text =
                    new TextView(this);

            text.setText(day);
            text.setGravity(Gravity.CENTER);
            text.setTextSize(11);
            text.setTextColor(
                    Color.rgb(
                            120,
                            115,
                            130
                    )
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            42,
                            25
                    );

            params.setMargins(
                    3,
                    0,
                    3,
                    0
            );

            text.setLayoutParams(params);

            row.addView(text);
        }

        heatmapContainer.addView(row);
    }

    private LinearLayout createHeatmapRow() {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER
        );

        row.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        return row;
    }

    // =========================================================
    // HEAT COLOR
    // =========================================================

    private int getHeatColor(
            double amount,
            double max
    ) {

        if (amount <= 0 ||
                max <= 0) {

            return HEAT_0;
        }

        double ratio =
                amount / max;

        if (ratio <= 0.25) {
            return HEAT_1;
        }

        if (ratio <= 0.50) {
            return HEAT_2;
        }

        if (ratio <= 0.75) {
            return HEAT_3;
        }

        return HEAT_4;
    }

    // =========================================================
    // LEGEND
    // =========================================================

    private void addHeatmapLegend() {

        LinearLayout legend =
                new LinearLayout(this);

        legend.setOrientation(
                LinearLayout.HORIZONTAL
        );

        legend.setGravity(
                Gravity.CENTER_VERTICAL
        );

        legend.setPadding(
                0,
                12,
                0,
                4
        );

        TextView less =
                new TextView(this);

        less.setText("Less");
        less.setTextSize(11);
        less.setTextColor(Color.GRAY);

        legend.addView(less);

        int[] colors = {
                HEAT_0,
                HEAT_1,
                HEAT_2,
                HEAT_3,
                HEAT_4
        };

        for (int color : colors) {

            View box =
                    new View(this);

            GradientDrawable drawable =
                    new GradientDrawable();

            drawable.setColor(color);
            drawable.setCornerRadius(5f);

            box.setBackground(drawable);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            22,
                            22
                    );

            params.setMargins(
                    5,
                    0,
                    0,
                    0
            );

            box.setLayoutParams(params);

            legend.addView(box);
        }

        TextView more =
                new TextView(this);

        more.setText("More");
        more.setTextSize(11);
        more.setTextColor(Color.GRAY);

        LinearLayout.LayoutParams moreParams =
                new LinearLayout.LayoutParams(
                        -2,
                        -2
                );

        moreParams.setMargins(
                6,
                0,
                0,
                0
        );

        more.setLayoutParams(
                moreParams
        );

        legend.addView(more);

        heatmapContainer.addView(
                legend
        );
    }

    // =========================================================
    // HEATMAP DIALOG
    // =========================================================

    private void showDayDialog(
            Calendar date,
            double amount
    ) {

        String title =
                new SimpleDateFormat(
                        "EEEE, d MMMM yyyy",
                        Locale.getDefault()
                ).format(
                        date.getTime()
                );

        int count =
                countExpensesOnDay(date);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                        "Expense: "
                                + formatCurrency(amount)
                                + "\n\nTransactions: "
                                + count
                )
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }

    private void showMonthDialog(
            Calendar month,
            double amount
    ) {

        String title =
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.getDefault()
                ).format(
                        month.getTime()
                );

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                        "Total expense: "
                                + formatCurrency(amount)
                )
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }

    private int countExpensesOnDay(
            Calendar target
    ) {

        int count = 0;

        for (Transaction transaction :
                allTransactions) {

            if (!isExpense(transaction)) {
                continue;
            }

            Date date =
                    parseDate(
                            transaction.getDate()
                    );

            if (date == null) {
                continue;
            }

            Calendar current =
                    Calendar.getInstance();

            current.setTime(date);

            if (isSameDay(
                    current,
                    target
            )) {
                count++;
            }
        }

        return count;
    }

    // =========================================================
    // INSIGHTS
    // =========================================================

    private void calculateInsights(
            List<Transaction> current,
            List<Transaction> previous
    ) {

        double income = 0;
        double expense = 0;
        double previousExpense = 0;

        for (Transaction transaction : current) {

            if (isIncome(transaction)) {
                income += transaction.getAmount();
            }

            if (isExpense(transaction)) {
                expense += transaction.getAmount();
            }
        }

        for (Transaction transaction : previous) {

            if (isExpense(transaction)) {
                previousExpense +=
                        transaction.getAmount();
            }
        }

        double savings =
                income - expense;

        double savingsRate =
                income > 0
                        ? savings / income * 100
                        : 0;

        if (income <= 0) {

            tvSavingsInsight.setText(
                    "Add income transactions to track your savings rate."
            );

        } else if (savingsRate >= 30) {

            tvSavingsInsight.setText(
                    String.format(
                            Locale.getDefault(),
                            "Great job! You saved %.1f%% of your income.",
                            savingsRate
                    )
            );

        } else {

            tvSavingsInsight.setText(
                    String.format(
                            Locale.getDefault(),
                            "Your current savings rate is %.1f%%.",
                            savingsRate
                    )
            );
        }

        Map<String, Double> categories =
                new HashMap<>();

        for (Transaction transaction : current) {

            if (!isExpense(transaction)) {
                continue;
            }

            String category =
                    transaction.getCategory();

            if (category == null ||
                    category.trim().isEmpty()) {

                category = "Other";
            }

            categories.put(
                    category,
                    categories.getOrDefault(
                            category,
                            0.0
                    )
                            +
                            transaction.getAmount()
            );
        }

        String topCategory = "No data";
        double topAmount = 0;

        for (Map.Entry<String, Double> entry :
                categories.entrySet()) {

            if (entry.getValue() > topAmount) {

                topAmount =
                        entry.getValue();

                topCategory =
                        entry.getKey();
            }
        }

        tvCategoryInsight.setText(
                topCategory.equals("No data")
                        ? "Add expenses to discover your top category."
                        : topCategory
                          + " is your highest spending category with "
                          + formatCurrency(topAmount)
                          + "."
        );

        double change =
                calculatePercentageChange(
                        previousExpense,
                        expense
                );

        if (change > 10) {

            tvSpendingInsight.setText(
                    String.format(
                            Locale.getDefault(),
                            "Your spending increased by %.1f%% compared with the previous period.",
                            change
                    )
            );

        } else if (change < -10) {

            tvSpendingInsight.setText(
                    String.format(
                            Locale.getDefault(),
                            "Nice! Your spending decreased by %.1f%%.",
                            Math.abs(change)
                    )
            );

        } else {

            tvSpendingInsight.setText(
                    "Your spending is relatively stable compared with the previous period."
            );
        }
    }

    // =========================================================
    // PERIOD
    // =========================================================

    private Period getSelectedPeriod() {

        Calendar start =
                Calendar.getInstance();

        Calendar end =
                Calendar.getInstance();

        int selected =
                spinnerPeriod
                        .getSelectedItemPosition();

        if (selected == 0) {

            start.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

        } else if (selected == 1) {

            start.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

            start.add(
                    Calendar.MONTH,
                    -1
            );

            end.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

            end.add(
                    Calendar.MONTH,
                    -1
            );

            end.set(
                    Calendar.DAY_OF_MONTH,
                    end.getActualMaximum(
                            Calendar.DAY_OF_MONTH
                    )
            );

        } else if (selected == 2) {

            start.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

            start.add(
                    Calendar.MONTH,
                    -2
            );

        } else {

            start.set(
                    Calendar.MONTH,
                    Calendar.JANUARY
            );

            start.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );
        }

        setStartOfDay(start);
        setEndOfDay(end);

        return new Period(
                start,
                end
        );
    }

    private Period getPreviousPeriod(
            Period current
    ) {

        Calendar start =
                (Calendar) current.start.clone();

        Calendar end =
                (Calendar) current.end.clone();

        int selected =
                spinnerPeriod
                        .getSelectedItemPosition();

        if (selected == 0 ||
                selected == 1) {

            start.add(
                    Calendar.MONTH,
                    -1
            );

            end.add(
                    Calendar.MONTH,
                    -1
            );

        } else if (selected == 2) {

            start.add(
                    Calendar.MONTH,
                    -3
            );

            end.add(
                    Calendar.MONTH,
                    -3
            );

        } else {

            start.add(
                    Calendar.YEAR,
                    -1
            );

            end.add(
                    Calendar.YEAR,
                    -1
            );
        }

        return new Period(
                start,
                end
        );
    }

    // =========================================================
    // FILTER
    // =========================================================

    private List<Transaction> filterTransactions(
            Calendar start,
            Calendar end
    ) {

        List<Transaction> result =
                new ArrayList<>();

        for (Transaction transaction :
                allTransactions) {

            Date date =
                    parseDate(
                            transaction.getDate()
                    );

            if (date == null) {
                continue;
            }

            Calendar transactionDate =
                    Calendar.getInstance();

            transactionDate.setTime(date);

            if (!transactionDate.before(start)
                    &&
                    !transactionDate.after(end)) {

                result.add(transaction);
            }
        }

        return result;
    }

    // =========================================================
    // DATE
    // =========================================================

    private Date parseDate(
            String dateString
    ) {

        if (dateString == null ||
                dateString.trim().isEmpty()) {

            return null;
        }

        try {

            return dateFormat.parse(
                    dateString
            );

        } catch (ParseException e) {

            return null;
        }
    }

    private boolean isSameDay(
            Calendar a,
            Calendar b
    ) {

        return a.get(Calendar.YEAR)
                ==
                b.get(Calendar.YEAR)
                &&
                a.get(Calendar.DAY_OF_YEAR)
                        ==
                        b.get(Calendar.DAY_OF_YEAR);
    }

    // =========================================================
    // TRANSACTION TYPE
    // =========================================================

    private boolean isIncome(
            Transaction transaction
    ) {

        return "Income".equalsIgnoreCase(
                transaction.getType()
        );
    }

    private boolean isExpense(
            Transaction transaction
    ) {

        return "Expense".equalsIgnoreCase(
                transaction.getType()
        );
    }

    // =========================================================
    // CALCULATIONS
    // =========================================================

    private double calculateAverageDaily(
            List<Transaction> transactions
    ) {

        double total = 0;

        for (Transaction transaction :
                transactions) {

            if (isExpense(transaction)) {

                total +=
                        transaction.getAmount();
            }
        }

        Period period =
                getSelectedPeriod();

        long days =
                (
                        period.end.getTimeInMillis()
                                -
                                period.start.getTimeInMillis()
                )
                        /
                        (24L * 60L * 60L * 1000L)
                        + 1;

        return days > 0
                ? total / days
                : 0;
    }

    private double calculatePercentageChange(
            double previous,
            double current
    ) {

        if (previous == 0) {

            return current == 0
                    ? 0
                    : 100;
        }

        return (
                (current - previous)
                        / previous
        ) * 100;
    }

    private String createChangeText(
            double previous,
            double current
    ) {

        double change =
                calculatePercentageChange(
                        previous,
                        current
                );

        if (change > 0) {

            return "↑ "
                    + String.format(
                    Locale.getDefault(),
                    "%.1f%% vs previous period",
                    change
            );

        } else if (change < 0) {

            return "↓ "
                    + String.format(
                    Locale.getDefault(),
                    "%.1f%% vs previous period",
                    Math.abs(change)
            );
        }

        return "→ 0.0% vs previous period";
    }

    // =========================================================
    // HEATMAP HELPERS
    // =========================================================

    private double getMaxValue(
            Map<String, Double> map
    ) {

        double max = 0;

        for (double value : map.values()) {

            if (value > max) {
                max = value;
            }
        }

        return max;
    }

    private String getDateKey(
            Calendar calendar
    ) {

        return String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    private String getMonthKey(
            Calendar calendar
    ) {

        return String.format(
                Locale.getDefault(),
                "%04d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1
        );
    }

    // =========================================================
    // FORMATTING
    // =========================================================

    private String formatCurrency(
            double amount
    ) {

        return String.format(
                Locale.getDefault(),
                "₹%,.2f",
                amount
        );
    }

    private String formatCompactCurrency(
            double amount
    ) {

        if (amount >= 100000) {

            return String.format(
                    Locale.getDefault(),
                    "₹%.1fL",
                    amount / 100000
            );
        }

        if (amount >= 1000) {

            return String.format(
                    Locale.getDefault(),
                    "₹%.1fk",
                    amount / 1000
            );
        }

        return String.format(
                Locale.getDefault(),
                "₹%.0f",
                amount
        );
    }

    private String formatChartValue(
            float value
    ) {

        if (value >= 100000) {

            return String.format(
                    Locale.getDefault(),
                    "₹%.1fL",
                    value / 100000
            );
        }

        if (value >= 1000) {

            return String.format(
                    Locale.getDefault(),
                    "₹%.1fk",
                    value / 1000
            );
        }

        return String.format(
                Locale.getDefault(),
                "₹%.0f",
                value
        );
    }

    // =========================================================
    // CALENDAR
    // =========================================================

    private void setStartOfDay(
            Calendar calendar
    ) {

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );
    }

    private void setEndOfDay(
            Calendar calendar
    ) {

        calendar.set(
                Calendar.HOUR_OF_DAY,
                23
        );

        calendar.set(
                Calendar.MINUTE,
                59
        );

        calendar.set(
                Calendar.SECOND,
                59
        );

        calendar.set(
                Calendar.MILLISECOND,
                999
        );
    }

    // =========================================================
    // PERIOD MODEL
    // =========================================================

    private static class Period {

        Calendar start;
        Calendar end;

        Period(
                Calendar start,
                Calendar end
        ) {

            this.start = start;
            this.end = end;
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executor.shutdown();
    }
}