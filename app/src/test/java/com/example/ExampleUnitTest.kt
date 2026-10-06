package com.example

import com.example.model.ExpenseCategories
import com.example.model.Transaction
import com.example.ui.DashboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTransactionType() {
        val exp = Transaction(type = Transaction.TYPE_EXPENSE, amount = 500.0)
        val inc = Transaction(type = Transaction.TYPE_INCOME, amount = 1000.0)

        assertTrue(exp.isExpense())
        assertTrue(inc.isIncome())
    }

    @Test
    fun testCategoryLookup() {
        val cat = ExpenseCategories.getCategoryInfo("Groceries")
        assertEquals("Groceries", cat.name)
        assertEquals("Rashan", cat.hindiName)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = DashboardViewModel.formatCurrency(45000.0)
        assertTrue(formatted.contains("45,000") || formatted.contains("45000"))
    }
}
