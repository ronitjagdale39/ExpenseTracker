package com.example.expensetracker.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Transaction;
import com.example.expensetracker.database.TransactionDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting periodic sync...");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No user logged in, skipping sync");
            return Result.success();
        }

        String uid = user.getUid();
        TransactionDao dao = AppDatabase.getInstance(getApplicationContext()).transactionDao();
        List<Transaction> transactions = dao.getAllTransactions(uid);

        if (transactions.isEmpty()) {
            Log.d(TAG, "No transactions to sync");
            return Result.success();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        for (Transaction t : transactions) {
            batch.set(
                    db.collection("users")
                            .document(uid)
                            .collection("transactions")
                            .document(String.valueOf(t.getId())),
                    t
            );
        }

        try {
            // We use a synchronous task wait here because WorkManager runs this on a background thread
            com.google.android.gms.tasks.Tasks.await(batch.commit());
            Log.d(TAG, "Sync successful!");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            return Result.retry();
        }
    }
}
