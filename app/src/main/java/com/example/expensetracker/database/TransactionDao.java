package com.example.expensetracker.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {

    // ==========================================
    // ADD TRANSACTION
    // ==========================================

    @Insert
    void insert(Transaction transaction);


    // ==========================================
    // GET ALL TRANSACTIONS
    // ==========================================

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    List<Transaction> getAllTransactions();


    // ==========================================
    // GET SINGLE TRANSACTION
    // Used for EDIT
    // ==========================================

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    Transaction getTransactionById(int id);


    // ==========================================
    // TOTAL INCOME
    // ==========================================

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income'")
    Double getTotalIncome();


    // ==========================================
    // TOTAL EXPENSE
    // ==========================================

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense'")
    Double getTotalExpense();


    // ==========================================
    // EDIT / UPDATE TRANSACTION
    // ==========================================

    @Update
    void update(Transaction transaction);


    // ==========================================
    // DELETE TRANSACTION
    // ==========================================

    @Delete
    void delete(Transaction transaction);
}