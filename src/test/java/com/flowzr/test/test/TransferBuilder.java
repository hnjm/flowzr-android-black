package com.flowzr.test.test;

import com.flowzr.db.DatabaseAdapter;
import com.flowzr.model.Account;
import com.flowzr.model.Transaction;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/13/11 8:52 PM
 */
public class TransferBuilder {

    private final DatabaseAdapter db;
    private final Transaction t = new Transaction();

    private TransferBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public static TransferBuilder withDb(DatabaseAdapter db) {
        return new TransferBuilder(db);
    }

    public TransferBuilder fromAccount(Account a) {
        t.fromAccountId = a.id;
        return this;
    }

    public TransferBuilder fromAmount(long amount) {
        t.fromAmount = amount;
        return this;
    }

    public TransferBuilder toAccount(Account a) {
        t.toAccountId = a.id;
        return this;
    }

    public TransferBuilder toAmount(long amount) {
        t.toAmount = amount;
        return this;
    }

    public TransferBuilder dateTime(DateTime dateTime) {
        t.dateTime = dateTime.asLong();
        return this;
    }

    public TransferBuilder scheduleOnce(DateTime dateTime) {
        t.dateTime = dateTime.asLong();
        t.setAsScheduled();
        return this;
    }

    public TransferBuilder scheduleRecur(String pattern) {
        t.recurrence = pattern;
        t.setAsScheduled();
        return this;
    }

    public TransferBuilder note(String note) {
        t.note = note;
        return this;
    }

    public Transaction create() {
        t.id = db.insertOrUpdate(t, null);
        return t;
    }

}
