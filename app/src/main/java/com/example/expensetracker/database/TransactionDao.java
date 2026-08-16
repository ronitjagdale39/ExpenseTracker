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


    // Get all transactions - newest first for specific user
    @Query("SELECT * FROM transactions WHERE userId = :uid ORDER BY id DESC")
    List<Transaction> getAllTransactions(String uid);


    // Get total income for specific user
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income' AND userId = :uid")
    Double getTotalIncome(String uid);


    // Get total expense for specific user
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense' AND userId = :uid")
    Double getTotalExpense(String uid);


    // Get transaction by ID
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    Transaction getTransactionById(int id);


    // Update transaction
    @Update
    void update(Transaction transaction);


    // Delete transaction
    @Delete
    void delete(Transaction transaction);

    // Search transactions
    @Query("SELECT * FROM transactions WHERE userId = :uid AND description LIKE :query ORDER BY id DESC")
    List<Transaction> searchTransactions(String uid, String query);

    // Filter by category
    @Query("SELECT * FROM transactions WHERE userId = :uid AND category = :category ORDER BY id DESC")
    List<Transaction> filterByCategory(String uid, String category);

    // Get total expense for a specific month and category
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :uid AND type = 'Expense' AND category = :category AND date LIKE :monthYearFilter")
    Double getCategoryExpenseForMonth(String uid, String category, String monthYearFilter);

    // --- Budget Queries ---
    @Insert
    void insertBudget(Budget budget);

    @Query("SELECT * FROM budgets WHERE userId = :uid AND monthYear = :monthYear")
    List<Budget> getBudgetsForMonth(String uid, String monthYear);

    @Update
    void updateBudget(Budget budget);

    // --- Recurring Transaction Queries ---
    @Insert
    void insertRecurring(RecurringTransaction recurring);

    @Query("SELECT * FROM recurring_transactions WHERE userId = :uid")
    List<RecurringTransaction> getAllRecurring(String uid);

    @Query("SELECT * FROM recurring_transactions WHERE dayOfMonth = :day")
    List<RecurringTransaction> getRecurringForDay(int day);
}
