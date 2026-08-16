package com.example.expensetracker;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import com.google.android.material.button.MaterialButton;
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

    // ============================================================
    // UI
    // ============================================================

    private TextView tvTrendTitle;

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

    private LinearLayout categoryLegendContainer;

    private PieChart pieChart;
    private LineChart lineChart;

    private Spinner spinnerPeriod;

    private MaterialButton btnExpenseTrend;
    private MaterialButton btnIncomeTrend;
    private MaterialButton btnBack;

    // ============================================================
    // DATABASE
    // ============================================================

    private TransactionDao transactionDao;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    // ============================================================
    // DATA
    // ============================================================

    private List<Transaction> allTransactions =
            new ArrayList<>();

    private boolean showIncomeTrend = false;

    private static final String DATE_FORMAT = "dd-MM-yyyy";

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    DATE_FORMAT,
                    Locale.getDefault()
            );

    // ============================================================
    // COLORS
    // ============================================================

    private static final int PURPLE =
            Color.rgb(126, 87, 194);

    private static final int GREEN =
            Color.rgb(46, 125, 50);

    private static final int RED =
            Color.rgb(211, 72, 72);

    // ============================================================
    // ON CREATE
    // ============================================================

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

        setupPeriodSpinner();

        setupPieChart();

        setupLineChart();

        btnBack.setOnClickListener(
                v -> finish()
        );

        btnExpenseTrend.setOnClickListener(v -> {

            showIncomeTrend = false;

            updateTrendButtonState();

            loadAnalytics();
        });

        btnIncomeTrend.setOnClickListener(v -> {

            showIncomeTrend = true;

            updateTrendButtonState();

            loadAnalytics();
        });

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

        updateTrendButtonState();

        loadTransactions();
    }

    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private void initializeViews() {

        tvTrendTitle =
                findViewById(R.id.tvTrendTitle);

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

        categoryLegendContainer =
                findViewById(
                        R.id.categoryLegendContainer
                );

        pieChart =
                findViewById(R.id.pieChart);

        lineChart =
                findViewById(R.id.lineChart);

        spinnerPeriod =
                findViewById(R.id.spinnerPeriod);

        btnExpenseTrend =
                findViewById(R.id.btnExpenseTrend);

        btnIncomeTrend =
                findViewById(R.id.btnIncomeTrend);

        btnBack =
                findViewById(R.id.btnBack);
    }

    // ============================================================
    // PERIOD SPINNER
    // ============================================================

    private void setupPeriodSpinner() {

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

        spinnerPeriod.setAdapter(
                adapter
        );
    }

    // ============================================================
    // LOAD TRANSACTIONS
    // ============================================================

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

        executorService.execute(() -> {

            List<Transaction> transactions =
                    transactionDao.getAllTransactions(
                            uid
                    );

            if (transactions == null) {

                transactions =
                        new ArrayList<>();
            }

            allTransactions =
                    transactions;

            runOnUiThread(
                    this::loadAnalytics
            );
        });
    }

    // ============================================================
    // MAIN ANALYTICS
    // ============================================================

    private void loadAnalytics() {

        if (allTransactions == null) {

            return;
        }

        Period currentPeriod =
                getSelectedPeriod();

        Period previousPeriod =
                getPreviousPeriod(
                        currentPeriod
                );

        List<Transaction> currentTransactions =
                filterTransactions(
                        allTransactions,
                        currentPeriod.start,
                        currentPeriod.end
                );

        List<Transaction> previousTransactions =
                filterTransactions(
                        allTransactions,
                        previousPeriod.start,
                        previousPeriod.end
                );

        calculateOverview(
                currentTransactions,
                previousTransactions
        );

        calculateCategoryAnalytics(
                currentTransactions
        );

        calculateTrend(
                currentTransactions
        );

        calculateInsights(
                currentTransactions,
                previousTransactions
        );
    }

    // ============================================================
    // OVERVIEW
    // ============================================================

    private void calculateOverview(
            List<Transaction> current,
            List<Transaction> previous
    ) {

        double income = 0;

        double expense = 0;

        for (Transaction transaction : current) {

            if (isIncome(transaction)) {

                income +=
                        transaction.getAmount();
            }

            if (isExpense(transaction)) {

                expense +=
                        transaction.getAmount();
            }
        }

        double previousIncome = 0;

        double previousExpense = 0;

        for (Transaction transaction : previous) {

            if (isIncome(transaction)) {

                previousIncome +=
                        transaction.getAmount();
            }

            if (isExpense(transaction)) {

                previousExpense +=
                        transaction.getAmount();
            }
        }

        double balance =
                income - expense;

        double savingsRate =
                income > 0
                        ? balance / income * 100
                        : 0;

        double incomeChange =
                calculatePercentageChange(
                        previousIncome,
                        income
                );

        double expenseChange =
                calculatePercentageChange(
                        previousExpense,
                        expense
                );

        double averageDaily =
                calculateAverageDailyExpense(
                        current
                );

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
                        incomeChange
                )
        );

        tvExpenseChange.setText(
                createChangeText(
                        expenseChange
                )
        );

        tvAverageDaily.setText(
                "Avg. daily expense: "
                        + formatCurrency(
                        averageDaily
                )
        );
    }

    // ============================================================
    // CATEGORY ANALYTICS
    // ============================================================

    private void calculateCategoryAnalytics(
            List<Transaction> transactions
    ) {

        Map<String, Double> categoryMap =
                new HashMap<>();

        double totalExpense = 0;

        for (Transaction transaction :
                transactions) {

            if (!isExpense(transaction)) {

                continue;
            }

            String category =
                    transaction.getCategory();

            if (category == null ||
                    category.trim().isEmpty()) {

                category =
                        "Other";
            }

            double amount =
                    transaction.getAmount();

            totalExpense +=
                    amount;

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

            tvNoCategoryData.setVisibility(
                    View.VISIBLE
            );

            categoryLegendContainer
                    .removeAllViews();

            tvTopCategory.setText(
                    "No data"
            );

            tvTopCategoryAmount.setText(
                    "Add some expenses"
            );

            return;
        }

        tvNoCategoryData.setVisibility(
                View.GONE
        );

        List<Map.Entry<String, Double>>
                sortedCategories =
                new ArrayList<>(
                        categoryMap.entrySet()
                );

        Collections.sort(
                sortedCategories,
                (a, b) ->
                        Double.compare(
                                b.getValue(),
                                a.getValue()
                        )
        );

        String topCategory =
                sortedCategories
                        .get(0)
                        .getKey();

        double topAmount =
                sortedCategories
                        .get(0)
                        .getValue();

        ArrayList<PieEntry> entries =
                new ArrayList<>();

        for (Map.Entry<String, Double> entry :
                sortedCategories) {

            entries.add(
                    new PieEntry(
                            entry.getValue()
                                    .floatValue(),
                            entry.getKey()
                    )
            );
        }

        setupPieData(
                entries,
                totalExpense
        );

        tvTopCategory.setText(
                topCategory
        );

        tvTopCategoryAmount.setText(
                formatCurrency(topAmount)
                        + " spent"
        );

        buildCategoryLegend(
                sortedCategories,
                totalExpense
        );
    }

    // ============================================================
    // PIE CHART SETUP
    // ============================================================

    private void setupPieChart() {

        pieChart
                .getDescription()
                .setEnabled(false);

        pieChart.setUsePercentValues(
                false
        );

        pieChart.setDrawHoleEnabled(
                true
        );

        pieChart.setHoleRadius(
                58f
        );

        pieChart.setTransparentCircleRadius(
                63f
        );

        pieChart.setDrawEntryLabels(
                false
        );

        pieChart.getLegend()
                .setEnabled(false);

        pieChart.setRotationEnabled(
                true
        );

        pieChart.setHighlightPerTapEnabled(
                true
        );
    }

    private void setupPieData(
            ArrayList<PieEntry> entries,
            double totalExpense
    ) {

        PieDataSet dataSet =
                new PieDataSet(
                        entries,
                        ""
                );

        dataSet.setSliceSpace(
                3f
        );

        dataSet.setSelectionShift(
                5f
        );

        dataSet.setValueTextSize(
                11f
        );

        dataSet.setValueTextColor(
                Color.WHITE
        );

        ArrayList<Integer> colors =
                new ArrayList<>();

        colors.add(
                Color.rgb(126, 87, 194)
        );

        colors.add(
                Color.rgb(66, 133, 244)
        );

        colors.add(
                Color.rgb(52, 168, 83)
        );

        colors.add(
                Color.rgb(251, 188, 4)
        );

        colors.add(
                Color.rgb(234, 67, 53)
        );

        colors.add(
                Color.rgb(156, 39, 176)
        );

        colors.add(
                Color.rgb(0, 150, 136)
        );

        colors.add(
                Color.rgb(255, 112, 67)
        );

        dataSet.setColors(
                colors
        );

        PieData pieData =
                new PieData(dataSet);

        pieChart.setData(
                pieData
        );

        pieChart.setCenterText(
                formatCompactCurrency(
                        totalExpense
                )
                        + "\nTotal"
        );

        pieChart.animateY(
                600
        );

        pieChart.invalidate();
    }

    // ============================================================
    // LINE CHART SETUP
    // ============================================================

    private void setupLineChart() {

        lineChart
                .getDescription()
                .setEnabled(false);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
    }

    // ============================================================
    // CATEGORY LEGEND
    // ============================================================

    private void buildCategoryLegend(
            List<Map.Entry<String, Double>>
                    categories,
            double totalExpense
    ) {

        categoryLegendContainer
                .removeAllViews();

        int limit =
                Math.min(
                        categories.size(),
                        8
                );

        for (int i = 0; i < limit; i++) {

            String category =
                    categories
                            .get(i)
                            .getKey();

            double amount =
                    categories
                            .get(i)
                            .getValue();

            double percentage =
                    totalExpense > 0
                            ? amount /
                              totalExpense * 100
                            : 0;

            LinearLayout row =
                    new LinearLayout(
                            this
                    );

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            row.setPadding(
                    0,
                    7,
                    0,
                    7
            );

            TextView dot =
                    new TextView(this);

            dot.setText(
                    "●"
            );

            dot.setTextSize(
                    17
            );

            dot.setTextColor(
                    getCategoryColor(i)
            );

            LinearLayout.LayoutParams
                    dotParams =
                    new LinearLayout.LayoutParams(
                            30,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            row.addView(
                    dot,
                    dotParams
            );

            TextView name =
                    new TextView(this);

            name.setText(
                    category
            );

            name.setTextSize(
                    14
            );

            name.setTextColor(
                    Color.rgb(
                            40,
                            40,
                            40
                    )
            );

            LinearLayout.LayoutParams
                    nameParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    );

            row.addView(
                    name,
                    nameParams
            );

            TextView value =
                    new TextView(this);

            value.setText(
                    formatCurrency(
                            amount
                    )
                            + "  "
                            + String.format(
                            Locale.getDefault(),
                            "%.0f%%",
                            percentage
                    )
            );

            value.setTextSize(
                    13
            );

            value.setTextColor(
                    Color.rgb(
                            100,
                            100,
                            100
                    )
            );

            row.addView(
                    value
            );

            categoryLegendContainer
                    .addView(row);
        }
    }

    private int getCategoryColor(
            int index
    ) {

        int[] colors = {

                Color.rgb(
                        126,
                        87,
                        194
                ),

                Color.rgb(
                        66,
                        133,
                        244
                ),

                Color.rgb(
                        52,
                        168,
                        83
                ),

                Color.rgb(
                        251,
                        188,
                        4
                ),

                Color.rgb(
                        234,
                        67,
                        53
                ),

                Color.rgb(
                        156,
                        39,
                        176
                ),

                Color.rgb(
                        0,
                        150,
                        136
                ),

                Color.rgb(
                        255,
                        112,
                        67
                )
        };

        return colors[
                index % colors.length
                ];
    }

    // ============================================================
    // TREND
    // ============================================================

    private void calculateTrend(
            List<Transaction> transactions
    ) {

        int selectedPeriod =
                spinnerPeriod
                        .getSelectedItemPosition();

        if (selectedPeriod == 0 ||
                selectedPeriod == 1) {

            tvTrendTitle.setText(
                    "Daily Trend"
            );

            calculateDailyTrend(
                    transactions
            );

        } else {

            tvTrendTitle.setText(
                    "Monthly Trend"
            );

            calculateMonthlyTrend(
                    transactions
            );
        }
    }

    // ============================================================
    // DAILY TREND
    // ============================================================

    private void calculateDailyTrend(
            List<Transaction> transactions
    ) {

        Period period =
                getSelectedPeriod();

        ArrayList<Entry> entries =
                new ArrayList<>();

        ArrayList<String> labels =
                new ArrayList<>();

        Calendar cursor =
                (Calendar)
                        period.start.clone();

        int index = 0;

        while (!cursor.after(
                period.end
        )) {

            Calendar currentDay =
                    (Calendar)
                            cursor.clone();

            double total = 0;

            for (Transaction transaction :
                    transactions) {

                Date transactionDate =
                        parseDate(
                                transaction.getDate()
                        );

                if (transactionDate == null) {

                    continue;
                }

                Calendar transactionCalendar =
                        Calendar.getInstance();

                transactionCalendar.setTime(
                        transactionDate
                );

                if (isSameDay(
                        transactionCalendar,
                        currentDay
                )) {

                    if (showIncomeTrend &&
                            isIncome(
                                    transaction
                            )) {

                        total +=
                                transaction.getAmount();
                    }

                    if (!showIncomeTrend &&
                            isExpense(
                                    transaction
                            )) {

                        total +=
                                transaction.getAmount();
                    }
                }
            }

            entries.add(
                    new Entry(
                            index,
                            (float) total
                    )
            );

            labels.add(
                    new SimpleDateFormat(
                            "d MMM",
                            Locale.getDefault()
                    ).format(
                            currentDay.getTime()
                    )
            );

            index++;

            cursor.add(
                    Calendar.DAY_OF_MONTH,
                    1
            );
        }

        renderTrendChart(
                entries,
                labels
        );
    }

    // ============================================================
    // MONTHLY TREND
    // ============================================================

    private void calculateMonthlyTrend(
            List<Transaction> transactions
    ) {

        Period period =
                getSelectedPeriod();

        ArrayList<Entry> entries =
                new ArrayList<>();

        ArrayList<String> labels =
                new ArrayList<>();

        Calendar cursor =
                (Calendar)
                        period.start.clone();

        cursor.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        Calendar end =
                (Calendar)
                        period.end.clone();

        end.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        int index = 0;

        SimpleDateFormat monthFormat =
                new SimpleDateFormat(
                        "MMM",
                        Locale.getDefault()
                );

        while (!cursor.after(end)) {

            int month =
                    cursor.get(
                            Calendar.MONTH
                    );

            int year =
                    cursor.get(
                            Calendar.YEAR
                    );

            double total = 0;

            for (Transaction transaction :
                    transactions) {

                Date transactionDate =
                        parseDate(
                                transaction.getDate()
                        );

                if (transactionDate == null) {

                    continue;
                }

                Calendar transactionCalendar =
                        Calendar.getInstance();

                transactionCalendar.setTime(
                        transactionDate
                );

                boolean sameMonth =
                        transactionCalendar.get(
                                Calendar.MONTH
                        ) == month
                                &&
                                transactionCalendar.get(
                                        Calendar.YEAR
                                ) == year;

                if (!sameMonth) {

                    continue;
                }

                if (showIncomeTrend &&
                        isIncome(
                                transaction
                        )) {

                    total +=
                            transaction.getAmount();
                }

                if (!showIncomeTrend &&
                        isExpense(
                                transaction
                        )) {

                    total +=
                            transaction.getAmount();
                }
            }

            entries.add(
                    new Entry(
                            index,
                            (float) total
                    )
            );

            labels.add(
                    monthFormat.format(
                            cursor.getTime()
                    )
            );

            index++;

            cursor.add(
                    Calendar.MONTH,
                    1
            );
        }

        renderTrendChart(
                entries,
                labels
        );
    }

    // ============================================================
    // RENDER TREND CHART
    // ============================================================

    private void renderTrendChart(
            ArrayList<Entry> entries,
            ArrayList<String> labels
    ) {

        if (entries.isEmpty()) {

            lineChart.clear();

            tvNoTrendData.setVisibility(
                    View.VISIBLE
            );

            return;
        }

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

        int lineColor =
                showIncomeTrend
                        ? GREEN
                        : PURPLE;

        String label =
                showIncomeTrend
                        ? "Income"
                        : "Expense";

        LineDataSet dataSet =
                new LineDataSet(
                        entries,
                        label
                );

        dataSet.setColor(
                lineColor
        );

        dataSet.setCircleColor(
                lineColor
        );

        dataSet.setLineWidth(
                2.5f
        );

        dataSet.setCircleRadius(
                3.5f
        );

        dataSet.setDrawValues(
                false
        );

        dataSet.setDrawFilled(
                true
        );

        dataSet.setFillAlpha(
                35
        );

        dataSet.setMode(
                LineDataSet.Mode.CUBIC_BEZIER
        );

        LineData lineData =
                new LineData(
                        dataSet
                );

        lineChart.setData(
                lineData
        );

        XAxis xAxis =
                lineChart.getXAxis();

        xAxis.setValueFormatter(
                new IndexAxisValueFormatter(
                        labels
                )
        );

        int labelCount;

        if (labels.size() <= 12) {

            labelCount =
                    labels.size();

        } else {

            labelCount = 7;
        }

        xAxis.setLabelCount(
                labelCount,
                true
        );

        xAxis.setGranularity(
                1f
        );

        xAxis.setDrawGridLines(
                false
        );

        lineChart
                .getAxisLeft()
                .setValueFormatter(
                        new ValueFormatter() {

                            @Override
                            public String
                            getFormattedValue(
                                    float value
                            ) {

                                return formatChartValue(
                                        value
                                );
                            }
                        }
                );

        lineChart
                .getAxisLeft()
                .setDrawGridLines(
                        true
                );

        lineChart
                .getAxisRight()
                .setEnabled(
                        false
                );

        lineChart.animateX(
                600
        );

        lineChart.invalidate();
    }

    // ============================================================
    // INSIGHTS
    // ============================================================

    private void calculateInsights(
            List<Transaction> current,
            List<Transaction> previous
    ) {

        double currentIncome = 0;

        double currentExpense = 0;

        double previousExpense = 0;

        for (Transaction transaction :
                current) {

            if (isIncome(transaction)) {

                currentIncome +=
                        transaction.getAmount();
            }

            if (isExpense(transaction)) {

                currentExpense +=
                        transaction.getAmount();
            }
        }

        for (Transaction transaction :
                previous) {

            if (isExpense(transaction)) {

                previousExpense +=
                        transaction.getAmount();
            }
        }

        Map<String, Double> categoryMap =
                new HashMap<>();

        for (Transaction transaction :
                current) {

            if (!isExpense(transaction)) {

                continue;
            }

            String category =
                    transaction.getCategory();

            if (category == null ||
                    category.trim().isEmpty()) {

                category = "Other";
            }

            categoryMap.put(
                    category,
                    categoryMap.getOrDefault(
                            category,
                            0.0
                    )
                            + transaction.getAmount()
            );
        }

        String topCategory =
                "No data";

        double topAmount = 0;

        for (Map.Entry<String, Double> entry :
                categoryMap.entrySet()) {

            if (entry.getValue() >
                    topAmount) {

                topAmount =
                        entry.getValue();

                topCategory =
                        entry.getKey();
            }
        }

        double savings =
                currentIncome -
                        currentExpense;

        double savingsRate =
                currentIncome > 0
                        ? savings /
                          currentIncome *
                          100
                        : 0;

        double expenseChange =
                calculatePercentageChange(
                        previousExpense,
                        currentExpense
                );

        String savingsInsight;

        if (currentIncome <= 0) {

            savingsInsight =
                    "Add income transactions "
                            + "to track your savings rate.";

        } else if (savingsRate >= 30) {

            savingsInsight =
                    "Great job! You saved "
                            + String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            savingsRate
                    )
                            + " of your income.";

        } else if (savingsRate >= 10) {

            savingsInsight =
                    "You saved "
                            + String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            savingsRate
                    )
                            + " of your income.";

        } else {

            savingsInsight =
                    "Your savings rate is low. "
                            + "Consider reducing "
                            + "unnecessary expenses.";
        }

        String categoryInsight;

        if (!topCategory.equals(
                "No data"
        )) {

            categoryInsight =
                    topCategory
                            + " is your highest "
                            + "spending category with "
                            + formatCurrency(
                            topAmount
                    )
                            + ".";

        } else {

            categoryInsight =
                    "Add expenses to discover "
                            + "your top spending category.";
        }

        String spendingInsight;

        if (previousExpense <= 0 &&
                currentExpense > 0) {

            spendingInsight =
                    "This is your first tracked "
                            + "expense period.";

        } else if (expenseChange > 10) {

            spendingInsight =
                    "Your spending increased by "
                            + String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            expenseChange
                    )
                            + " compared with the "
                            + "previous period.";

        } else if (expenseChange < -10) {

            spendingInsight =
                    "Nice! Your spending decreased by "
                            + String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            Math.abs(
                                    expenseChange
                            )
                    )
                            + ".";

        } else {

            spendingInsight =
                    "Your spending is relatively "
                            + "stable compared with "
                            + "the previous period.";
        }

        tvSavingsInsight.setText(
                savingsInsight
        );

        tvCategoryInsight.setText(
                categoryInsight
        );

        tvSpendingInsight.setText(
                spendingInsight
        );
    }

    // ============================================================
    // PERIOD
    // ============================================================

    private Period getSelectedPeriod() {

        Calendar start =
                Calendar.getInstance();

        Calendar end =
                Calendar.getInstance();

        int selected =
                spinnerPeriod
                        .getSelectedItemPosition();

        if (selected == 0) {

            // THIS MONTH

            start.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

        } else if (selected == 1) {

            // LAST MONTH

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

            // LAST 3 MONTHS

            start.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

            start.add(
                    Calendar.MONTH,
                    -2
            );

        } else {

            // THIS YEAR

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

    // ============================================================
    // PREVIOUS PERIOD
    // ============================================================

    private Period getPreviousPeriod(
            Period current
    ) {

        Calendar start =
                (Calendar)
                        current.start.clone();

        Calendar end =
                (Calendar)
                        current.end.clone();

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

    // ============================================================
    // FILTER TRANSACTIONS
    // ============================================================

    private List<Transaction> filterTransactions(
            List<Transaction> transactions,
            Calendar start,
            Calendar end
    ) {

        List<Transaction> result =
                new ArrayList<>();

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

            transactionDate.setTime(
                    date
            );

            if (!transactionDate.before(
                    start
            )
                    &&
                    !transactionDate.after(
                            end
                    )) {

                result.add(
                        transaction
                );
            }
        }

        return result;
    }

    // ============================================================
    // DATE
    // ============================================================

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
            Calendar first,
            Calendar second
    ) {

        return first.get(
                Calendar.YEAR
        ) ==
                second.get(
                        Calendar.YEAR
                )

                &&

                first.get(
                        Calendar.DAY_OF_YEAR
                ) ==
                        second.get(
                                Calendar.DAY_OF_YEAR
                        );
    }

    // ============================================================
    // TRANSACTION TYPE
    // ============================================================

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

    // ============================================================
    // PERCENTAGE
    // ============================================================

    private double calculatePercentageChange(
            double previous,
            double current
    ) {

        if (previous == 0) {

            if (current == 0) {

                return 0;
            }

            return 100;
        }

        return (
                (current - previous)
                        / previous
        ) * 100;
    }

    // ============================================================
    // AVERAGE DAILY EXPENSE
    // ============================================================

    private double calculateAverageDailyExpense(
            List<Transaction> transactions
    ) {

        double totalExpense = 0;

        for (Transaction transaction :
                transactions) {

            if (isExpense(transaction)) {

                totalExpense +=
                        transaction.getAmount();
            }
        }

        Period period =
                getSelectedPeriod();

        long difference =
                period.end.getTimeInMillis()
                        -
                        period.start.getTimeInMillis();

        long days =
                difference /
                        (
                                24L *
                                        60L *
                                        60L *
                                        1000L
                        )
                        + 1;

        if (days <= 0) {

            return 0;
        }

        return totalExpense / days;
    }

    // ============================================================
    // CHANGE TEXT
    // ============================================================

    private String createChangeText(
            double percentage
    ) {

        if (percentage > 0) {

            return "↑ "
                    + String.format(
                    Locale.getDefault(),
                    "%.1f%% vs previous period",
                    percentage
            );

        } else if (percentage < 0) {

            return "↓ "
                    + String.format(
                    Locale.getDefault(),
                    "%.1f%% vs previous period",
                    Math.abs(
                            percentage
                    )
            );
        }

        return "→ 0.0% vs previous period";
    }

    // ============================================================
    // CURRENCY
    // ============================================================

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
                    value / 100000f
            );
        }

        if (value >= 1000) {

            return String.format(
                    Locale.getDefault(),
                    "₹%.1fk",
                    value / 1000f
            );
        }

        return String.format(
                Locale.getDefault(),
                "₹%.0f",
                value
        );
    }

    // ============================================================
    // TREND BUTTONS
    // ============================================================

    private void updateTrendButtonState() {

        if (showIncomeTrend) {

            btnIncomeTrend.setBackgroundTintList(
                    ColorStateList.valueOf(
                            PURPLE
                    )
            );

            btnIncomeTrend.setTextColor(
                    Color.WHITE
            );

            btnExpenseTrend.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.WHITE
                    )
            );

            btnExpenseTrend.setTextColor(
                    Color.rgb(
                            80,
                            80,
                            80
                    )
            );

        } else {

            btnExpenseTrend.setBackgroundTintList(
                    ColorStateList.valueOf(
                            PURPLE
                    )
            );

            btnExpenseTrend.setTextColor(
                    Color.WHITE
            );

            btnIncomeTrend.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.WHITE
                    )
            );

            btnIncomeTrend.setTextColor(
                    Color.rgb(
                            80,
                            80,
                            80
                    )
            );
        }
    }

    // ============================================================
    // CALENDAR HELPERS
    // ============================================================

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

    // ============================================================
    // PERIOD MODEL
    // ============================================================

    private static class Period {

        Calendar start;

        Calendar end;

        Period(
                Calendar start,
                Calendar end
        ) {

            this.start =
                    start;

            this.end =
                    end;
        }
    }

    // ============================================================
    // DESTROY
    // ============================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executorService.shutdown();
    }
}