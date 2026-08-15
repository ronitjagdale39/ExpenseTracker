package com.example.expensetracker.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {

    // Insert new transaction
    @Insert
    void insert(Transaction transaction);


    // Get all transactions - newest first
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    List<Transaction> getAllTransactions();


    // Get total income
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income'")
    Double getTotalIncome();


    // Get total expense
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense'")
    Double getTotalExpense();


    // Get transaction by ID
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    Transaction getTransactionById(int id);


    // Update transaction
    @Update
    void update(Transaction transaction);


    // Delete transaction
    @Delete
    void delete(Transaction transaction);
}