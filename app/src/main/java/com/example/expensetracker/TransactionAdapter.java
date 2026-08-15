package com.example.expensetracker;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.database.Transaction;

import java.util.List;
import java.util.Locale;

public class TransactionAdapter
        extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<Transaction> transactions;

    private final OnDeleteClickListener onDeleteClickListener;


    // ==========================================
    // DELETE CALLBACK
    // ==========================================

    public interface OnDeleteClickListener {
        void onDeleteClick(Transaction transaction);
    }


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public TransactionAdapter(
            List<Transaction> transactions,
            OnDeleteClickListener onDeleteClickListener) {

        this.transactions = transactions;
        this.onDeleteClickListener = onDeleteClickListener;
    }


    // ==========================================
    // CREATE VIEW HOLDER
    // ==========================================

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_transaction,
                        parent,
                        false
                );

        return new TransactionViewHolder(view);
    }


    // ==========================================
    // BIND DATA
    // ==========================================

    @Override
    public void onBindViewHolder(
            @NonNull TransactionViewHolder holder,
            int position) {

        Transaction transaction =
                transactions.get(position);


        // Description

        holder.tvDescription.setText(
                transaction.getDescription()
        );


        // Category

        holder.tvCategory.setText(
                transaction.getCategory()
        );


        // Date

        holder.tvDate.setText(
                transaction.getDate()
        );


        // Amount

        String amount = String.format(
                Locale.getDefault(),
                "₹%.2f",
                transaction.getAmount()
        );


        if ("Income".equalsIgnoreCase(
                transaction.getType())) {

            holder.tvAmount.setText(
                    "+ " + amount
            );

            holder.tvAmount.setTextColor(
                    Color.rgb(22, 128, 60)
            );

        } else {

            holder.tvAmount.setText(
                    "- " + amount
            );

            holder.tvAmount.setTextColor(
                    Color.rgb(211, 47, 47)
            );
        }


        // ==========================================
        // EDIT BUTTON
        // ==========================================

        holder.btnEdit.setOnClickListener(view -> {

            Intent intent = new Intent(
                    view.getContext(),
                    AddTransactionActivity.class
            );


            // Send transaction ID

            intent.putExtra(
                    "transaction_id",
                    transaction.getId()
            );


            view.getContext().startActivity(intent);
        });


        // ==========================================
        // DELETE BUTTON
        // ==========================================

        holder.btnDelete.setOnClickListener(view -> {

            if (onDeleteClickListener != null) {

                onDeleteClickListener.onDeleteClick(
                        transaction
                );
            }
        });
    }


    // ==========================================
    // ITEM COUNT
    // ==========================================

    @Override
    public int getItemCount() {
        return transactions.size();
    }


    // ==========================================
    // VIEW HOLDER
    // ==========================================

    static class TransactionViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvDescription;
        TextView tvCategory;
        TextView tvDate;
        TextView tvAmount;

        Button btnEdit;
        Button btnDelete;


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