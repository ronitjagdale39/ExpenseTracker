package com.example.expensetracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.database.AppDatabase;
import com.example.expensetracker.database.Transaction;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionAdapter
        extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);

        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TransactionViewHolder holder,
            int position) {

        Transaction transaction = transactions.get(position);

        // -----------------------------
        // Transaction information
        // -----------------------------

        holder.tvDescription.setText(
                transaction.getDescription()
        );

        holder.tvCategory.setText(
                transaction.getCategory()
        );

        holder.tvDate.setText(
                transaction.getDate()
        );

        // -----------------------------
        // Amount
        // -----------------------------

        String amount = String.format(
                Locale.getDefault(),
                "₹%.2f",
                transaction.getAmount()
        );

        if ("Income".equalsIgnoreCase(transaction.getType())) {

            holder.tvAmount.setText("+ " + amount);

            holder.tvAmount.setTextColor(
                    Color.rgb(22, 128, 60)
            );

        } else {

            holder.tvAmount.setText("- " + amount);

            holder.tvAmount.setTextColor(
                    Color.rgb(211, 47, 47)
            );
        }

        // -----------------------------
        // Edit button
        // -----------------------------

        holder.btnEdit.setOnClickListener(view -> {

            Context context = view.getContext();

            Intent intent = new Intent(
                    context,
                    AddTransactionActivity.class
            );

            // Send transaction ID
            intent.putExtra(
                    "transaction_id",
                    transaction.getId()
            );

            context.startActivity(intent);
        });

        // -----------------------------
        // Delete button
        // -----------------------------

        holder.btnDelete.setOnClickListener(view -> {

            AppDatabase database =
                    AppDatabase.getInstance(view.getContext());

            Executors.newSingleThreadExecutor().execute(() -> {

                database.transactionDao().delete(transaction);

                // Remove from current list
                int adapterPosition =
                        holder.getAdapterPosition();

                if (adapterPosition != RecyclerView.NO_POSITION) {

                    transactions.remove(adapterPosition);

                    holder.itemView.post(() ->
                            notifyItemRemoved(adapterPosition)
                    );
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    // ==================================================
    // ViewHolder
    // ==================================================

    static class TransactionViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvDescription;
        TextView tvCategory;
        TextView tvDate;
        TextView tvAmount;

        android.widget.Button btnEdit;
        android.widget.Button btnDelete;

        public TransactionViewHolder(
                @NonNull View itemView) {

            super(itemView);

            tvDescription =
                    itemView.findViewById(
                            R.id.tvTransactionDescription
                    );

            tvCategory =
                    itemView.findViewById(
                            R.id.tvTransactionCategory
                    );

            tvDate =
                    itemView.findViewById(
                            R.id.tvTransactionDate
                    );

            tvAmount =
                    itemView.findViewById(
                            R.id.tvTransactionAmount
                    );

            btnEdit =
                    itemView.findViewById(
                            R.id.btnEditTransaction
                    );

            btnDelete =
                    itemView.findViewById(
                            R.id.btnDeleteTransaction
                    );
        }
    }
}