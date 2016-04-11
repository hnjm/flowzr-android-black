package com.flowzr.test.db;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.DatabaseUtils;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.Account;
import com.flowzr.model.Category;
import com.flowzr.model.Transaction;
import com.flowzr.test.test.DateTime;

import org.junit.After;
import org.junit.Before;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/7/11 7:22 PM
 */
public abstract class AbstractDbTest extends AndroidTestCase {

    protected DatabaseAdapter db;
    protected MyEntityManager em;
    private DatabaseHelper dbHelper;

    public static <T> Set<T> asSet(T... values) {
        return new HashSet<T>(Arrays.asList(values));
    }

    @Before
    public void setUp() throws Exception {
        Context context = new RenamingDelegatingContext(getContext(), "test-");
        dbHelper = new DatabaseHelper(context);
        db = new DatabaseAdapter(context, dbHelper);
        db.open();
        em = db.em();
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.close();
    }

    public void assertAccountTotal(Account account, long total) {
        Account a = db.em().getAccount(account.id);
        assertEquals("Account " + account.id + " total", total, a.totalAmount);
    }

    public void assertLastTransactionDate(Account account, DateTime dateTime) {
        Account a = db.em().getAccount(account.id);
        assertEquals("Account " + account.id + " last transaction date", dateTime.asLong(), a.lastTransactionDate);
    }

    public void assertFinalBalanceForAccount(Account account, long expectedBalance) {
        long balance = db.getLastRunningBalanceForAccount(account);
        assertEquals("Account " + account.id + " final balance", expectedBalance, balance);
    }

    public void assertAccountBalanceForTransaction(Transaction t, Account a, long expectedBalance) {
        long balance = db.getAccountBalanceForTransaction(a, t);
        assertEquals(expectedBalance, balance);
    }

    public void assertTransactionsCount(Account account, long expectedCount) {
        long count = DatabaseUtils.rawFetchLongValue(db,
                "select count(*) from transactions where from_account_id=?",
                new String[]{String.valueOf(account.id)});
        assertEquals("Transaction for account " + account.id, expectedCount, count);
    }

    public void assertCategory(String name, boolean isIncome, Category c) {
        assertEquals(name, c.title);
        assertEquals(isIncome, c.isIncome());
    }

}
