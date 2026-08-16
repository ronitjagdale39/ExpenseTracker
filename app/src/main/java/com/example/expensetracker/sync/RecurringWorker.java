package com.example.expensetracker.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.RecurringTransaction;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecurringWorker extends Worker {

    private static final String TAG = "RecurringWorker";

    public RecurringWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Checking for recurring transactions...");

        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        String dateStr = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        TransactionDao dao = AppDatabase.getInstance(getApplicationContext()).transactionDao();
        List<RecurringTransaction> recurringList = dao.getRecurringForDay(currentDay);

        for (RecurringTransaction rt : recurringList) {
            Transaction t = new Transaction(
                    rt.getAmount(),
                    rt.getDescription(),
                    rt.getCategory(),
                    rt.getType(),
                    dateStr,
                    rt.getUserId()
            );
            dao.insert(t);
            Log.d(TAG, "Added recurring transaction: " + rt.getDescription());
        }

        return Result.success();
    }
}
