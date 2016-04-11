/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.flowzr.export.csv;

import android.content.Context;
import android.database.Cursor;

import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.export.Export;
import com.flowzr.model.Account;
import com.flowzr.model.Category;
import com.flowzr.model.Currency;
import com.flowzr.model.MyLocation;
import com.flowzr.model.Payee;
import com.flowzr.model.Project;
import com.flowzr.model.Transaction;
import com.flowzr.utils.CurrencyCache;
import com.flowzr.utils.Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.flowzr.datetime.DateUtils.FORMAT_DATE_ISO_8601;
import static com.flowzr.datetime.DateUtils.FORMAT_TIME_ISO_8601;

public class CsvExport extends Export {

    public static final String[] HEADER = "date,time,account,amount,currency,original amount,original currency,category,parent,payee,location,entity,note,status".split(",");

    private static final MyLocation TRANSFER_IN = new MyLocation();
    private static final MyLocation TRANSFER_OUT = new MyLocation();

    static {
        TRANSFER_IN.name = "Transfer In";
        TRANSFER_OUT.name = "Transfer Out";
    }

	private final DatabaseAdapter db;
    private final CsvExportOptions options;

    private Map<Long, Category> categoriesMap;
    private Map<Long, Account> accountsMap;
    private Map<Long, Payee> payeeMap;
    private Map<Long, Project> projectMap;
    private Map<Long, MyLocation> locationMap;

    public CsvExport(Context context, DatabaseAdapter db, CsvExportOptions options) {
        super(context, false);
		this.db = db;
		this.options = options;
	}
	
	@Override
	protected String getExtension() {
		return ".csv";
	}

	@Override
	protected void writeHeader(BufferedWriter bw) throws IOException  {
        if (options.writeUtfBom) {
            byte[] bom = new byte[3];
            bom[0] = (byte) 0xEF;
            bom[1] = (byte) 0xBB;
            bom[2] = (byte) 0xBF;
            bw.write(new String(bom,"UTF-8"));
        }
		if (options.includeHeader) {
			Csv.Writer w = new Csv.Writer(bw).delimiter(options.fieldSeparator);
            for (String h : HEADER) {
                w.value(h);
            }
			w.newLine();
		}
	}

	@Override
	protected void writeBody(BufferedWriter bw) throws IOException {
		Csv.Writer w = new Csv.Writer(bw).delimiter(options.fieldSeparator);
		try {
            MyEntityManager em = db.em();
            accountsMap = em.getAllAccountsMap();
            categoriesMap = db.getAllCategoriesMap();
            payeeMap = em.getAllPayeeByIdMap();
            projectMap = em.getAllProjectsByIdMap(true);
            locationMap = em.getAllLocationsByIdMap(false);
            Cursor c = db.getBlotter(options.filter);
            //noinspection TryFinallyCanBeTryWithResources,TryFinallyCanBeTryWithResources,TryFinallyCanBeTryWithResources,TryFinallyCanBeTryWithResources,TryFinallyCanBeTryWithResources
            try {
				while (c.moveToNext()) {
                    Transaction t = Transaction.fromBlotterCursor(c);
					writeLine(w, t);
				}					
			} finally {
				c.close();
			}
		} finally {
			w.close();
		}
	}

    private void writeLine(Csv.Writer w, Transaction t) {
        Date dt = t.dateTime > 0 ? new Date(t.dateTime) : null;
        Category category = getCategoryById(t.categoryId);
        Project project = getProjectById(t.projectId);
        Account fromAccount = getAccount(t.fromAccountId);
        if (t.isTransfer()) {
            Account toAccount = getAccount(t.toAccountId);
            writeLine(w, dt, fromAccount.title, t.fromAmount, fromAccount.currency.id, 0, 0, category, null, TRANSFER_OUT, project, t.note,t.status.name());
            writeLine(w, dt, toAccount.title, t.toAmount, toAccount.currency.id, 0, 0, category, null, TRANSFER_IN, project, t.note,t.status.name());
        } else {
            MyLocation location = getLocationById(t.locationId);
            Payee payee = getPayee(t.payeeId);
            writeLine(w, dt, fromAccount.title, t.fromAmount, fromAccount.currency.id, t.originalFromAmount, t.originalCurrencyId,
                    category, payee, location, project, t.note,t.status.name());
            if (category != null && category.isSplit() && options.exportSplits) {
                List<Transaction> splits = db.em().getSplitsForTransaction(t.id);
                for (Transaction split : splits) {
                    split.dateTime = 0;
                    writeLine(w, split);
                }
            }
        }
    }

    private void writeLine(Csv.Writer w, Date dt, String account,
                           long amount, long currencyId,
                           long originalAmount, long originalCurrencyId,
			               Category category, Payee payee, MyLocation location, Project project, String note,String status) {
        if (dt != null) {
		    w.value(FORMAT_DATE_ISO_8601.format(dt));
		    w.value(FORMAT_TIME_ISO_8601.format(dt));
        } else {
            w.value("~");
            w.value("");
        }
		w.value(account);
        String amountFormatted = options.amountFormat.format(new BigDecimal(amount).divide(Utils.HUNDRED));
        w.value(amountFormatted);
		Currency c = CurrencyCache.getCurrency(db.em(), currencyId);
		w.value(c.name);
        if (originalCurrencyId > 0) {
            w.value(options.amountFormat.format(new BigDecimal(originalAmount).divide(Utils.HUNDRED)));
            Currency originalCurrency = CurrencyCache.getCurrency(db.em(), originalCurrencyId);
            w.value(originalCurrency.name);
        } else {
            w.value("");
            w.value("");
        }
		w.value(category != null ? category.title : "");
		String sParent = buildPath(category);
		w.value(sParent);
        w.value(payee != null ? payee.title : "");
		w.value(location != null ? location.name : "");
		w.value(project != null ? project.title : "");
		w.value(note);
        w.value(status);
		w.newLine();
	}

	private String buildPath(Category category) {
		if (category == null || category.parent == null) {
			return "";
		} else {
            StringBuilder sb = new StringBuilder(category.parent.title);
			for (Category cat = category.parent.parent; cat != null; cat = cat.parent) {
                sb.insert(0,":").insert(0, cat.title);
			}
			return sb.toString();
		}
	}

	@Override
	protected void writeFooter(BufferedWriter bw) throws IOException {
	}

    private Account getAccount(long accountId) {
        return accountsMap.get(accountId);
    }

	public Category getCategoryById(long id) {
        Category category = categoriesMap.get(id);
        if (category.id == 0) return null;
        if (category.isSplit()) {
            category.title = "SPLIT";
        }
        return category;
	}

    private Payee getPayee(long payeeId) {
        return payeeMap.get(payeeId);
    }

    private Project getProjectById(long projectId) {
        return projectMap.get(projectId);
    }

    private MyLocation getLocationById(long locationId) {
        return locationMap.get(locationId);
    }

}
