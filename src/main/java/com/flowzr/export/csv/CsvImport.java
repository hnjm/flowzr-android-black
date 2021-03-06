package com.flowzr.export.csv;

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.export.CategoryCache;
import com.flowzr.export.CategoryInfo;
import com.flowzr.export.ProgressListener;
import com.flowzr.model.Account;
import com.flowzr.model.Category;
import com.flowzr.model.Currency;
import com.flowzr.model.MyEntity;
import com.flowzr.model.Payee;
import com.flowzr.model.Project;
import com.flowzr.model.Transaction;
import com.flowzr.model.TransactionAttribute;
import com.flowzr.utils.Utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CsvImport {

    private final DatabaseAdapter db;
    private final CsvImportOptions options;
    private final Account account;
    private char decimalSeparator;
    private char groupSeparator;
    private ProgressListener progressListener;

    public CsvImport(DatabaseAdapter db, CsvImportOptions options) {
        this.db = db;
        this.options = options;
        this.account = db.em().getAccount(options.selectedAccountId);
        this.decimalSeparator = options.currency.decimalSeparator.charAt(1);
        this.groupSeparator = options.currency.groupSeparator.charAt(1);
    }

    public Object doImport() throws Exception {
        long t0 = System.currentTimeMillis();
        List<CsvTransaction> transactions = parseTransactions();
        long t1 = System.currentTimeMillis();
        Log.i("Flowzr", "Parsing transactions ="+(t1-t0)+"ms");
        Map<String, Category> categories = collectAndInsertCategories(transactions);
        long t2 = System.currentTimeMillis();
        Log.i("Flowzr", "Collecting categories ="+(t2-t1)+"ms");
        Map<String, Project> projects = collectAndInsertProjects(transactions);
        long t3 = System.currentTimeMillis();
        Log.i("Flowzr", "Collecting projects ="+(t3-t2)+"ms");
        Map<String, Payee> payees = collectAndInsertPayees(transactions);
        long t4 = System.currentTimeMillis();
        Log.i("Flowzr", "Collecting payees ="+(t4-t3)+"ms");
        Map<String, Currency> currencies = collectAndInsertCurrencies(transactions);
        long t5 = System.currentTimeMillis();
        Log.i("Flowzr", "Collecting currencies ="+(t5-t4)+"ms");
        importTransactions(transactions, currencies, categories, projects, payees);
        long t6 = System.currentTimeMillis();
        Log.i("Flowzr", "Inserting transactions ="+(t6-t5)+"ms");
        Log.i("Flowzr", "Overall csv import ="+((t6-t0)/1000)+"s");
        return options.filename + " imported!";
    }

    public Map<String, Project> collectAndInsertProjects(List<CsvTransaction> transactions) {
        MyEntityManager em = db.em();
        Map<String, Project> map = em.getAllProjectsByTitleMap(false);
        for (CsvTransaction transaction : transactions) {
            String project = transaction.project;
            if (isNewProject(map, project)) {
                Project p = new Project();
                p.title = project;
                p.isActive = true;
                em.saveOrUpdate(p);
                map.put(project, p);
            }
        }
        return map;
    }

    private boolean isNewProject(Map<String, Project> map, String project) {
        return Utils.isNotEmpty(project) && !"No entity".equals(project) && !map.containsKey(project);
    }

    public Map<String, Payee> collectAndInsertPayees(List<CsvTransaction> transactions) {
        MyEntityManager em = db.em();
        Map<String, Payee> map = em.getAllPayeeByTitleMap();
        for (CsvTransaction transaction : transactions) {
            String payee = transaction.payee;
            if (isNewEntity(map, payee)) {
                Payee p = new Payee();
                p.title = payee;
                em.saveOrUpdate(p);
                map.put(payee, p);
            }
        }
        return map;
    }

    private boolean isNewEntity(Map<String, ? extends MyEntity> map, String name) {
        return Utils.isNotEmpty(name) && !map.containsKey(name);
    }

    public Map<String, Category> collectAndInsertCategories(List<CsvTransaction> transactions) {
        Set<CategoryInfo> categories = collectCategories(transactions);
        CategoryCache cache = new CategoryCache();
        cache.loadExistingCategories(db);
        cache.insertCategories(db, categories);
        return cache.categoryNameToCategory;
    }

    private Map<String, Currency> collectAndInsertCurrencies(List<CsvTransaction> transactions) {
        MyEntityManager em = db.em();
        Map<String, Currency> map = em.getAllCurrenciesByTtitleMap();
        for (CsvTransaction transaction : transactions) {
            String currency = transaction.originalCurrency;
            if (isNewEntity(map, currency)) {
                Currency c = new Currency();
                c.name = currency;
                c.symbol = currency;
                c.title = currency;
                c.decimalSeparator = Currency.EMPTY.decimalSeparator;
                c.groupSeparator = Currency.EMPTY.groupSeparator;
                c.isDefault = false;
                em.saveOrUpdate(c);
                map.put(currency, c);
            }
        }
        return map;
    }

    private void importTransactions(List<CsvTransaction> transactions,
                                    Map<String, Currency> currencies,
                                    Map<String, Category> categories,
                                    Map<String, Project> projects,
                                    Map<String, Payee> payees) {
        SQLiteDatabase database = db.db();
        database.beginTransaction();
        try {
            List<TransactionAttribute> emptyAttributes = Collections.emptyList();
            int count = 0;
            int totalCount = transactions.size();
            for (CsvTransaction transaction : transactions) {
                Transaction t = transaction.createTransaction(currencies, categories, projects, payees);
                db.insertOrUpdateInTransaction(t, emptyAttributes);
                if (++count % 100 == 0) {
                    Log.i("Flowzr", "Inserted "+count+" out of "+totalCount);
                    if (progressListener != null) {
                        progressListener.onProgress((int)(100f*count/totalCount));
                    }
                }
            }
            Log.i("Flowzr", "Total transactions inserted: "+count);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private List<CsvTransaction> parseTransactions() throws Exception {
        String csvFilename = options.filename;
        boolean parseLine = false;
        List<String> header = null;
        if (!options.useHeaderFromFile) {
            parseLine = true;
            header = Arrays.asList(CsvExport.HEADER);
        }
        try {
            long deltaTime = 0;
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            Csv.Reader reader = new Csv.Reader(new FileReader(csvFilename))
                    .delimiter(options.fieldSeparator).ignoreComments(true);
            List<CsvTransaction> transactions = new LinkedList<>();
            List<String> line;
            while ((line = reader.readLine()) != null) {
                if (parseLine) {
                    CsvTransaction transaction = new CsvTransaction();
                    transaction.fromAccountId = this.account.id;
                    int countOfColumns = line.size();
                    for (int i = 0; i < countOfColumns; i++) {
                        String transactionField = myTrim(header.get(i));
                        if (!transactionField.equals("")) {
                            try {
                                String fieldValue = line.get(i);
                                if (!fieldValue.equals("")) {
                                    switch (transactionField) {
                                        case "date":
                                            transaction.date = options.dateFormat.parse(fieldValue).getTime();
                                            break;
                                        case "time":
                                            transaction.time = format.parse(fieldValue).getTime();
                                            break;
                                        case "amount":
                                            Double fromAmountDouble = parseAmount(fieldValue);
                                            transaction.fromAmount = fromAmountDouble.longValue();
                                            break;
                                        case "original amount":
                                            Double originalAmountDouble = parseAmount(fieldValue);
                                            transaction.originalAmount = originalAmountDouble.longValue();
                                            break;
                                        case "original currency":
                                            transaction.originalCurrency = fieldValue;
                                            break;
                                        case "payee":
                                            transaction.payee = fieldValue;
                                            break;
                                        case "category":
                                            transaction.category = fieldValue;
                                            break;
                                        case "parent":
                                            transaction.categoryParent = fieldValue;
                                            break;
                                        case "note":
                                            transaction.note = fieldValue;
                                            break;
                                        case "project":
                                            transaction.project = fieldValue;
                                            break;
                                        case "currency":
                                            if (!account.currency.name.equals(fieldValue)) {
                                                throw new Exception("Wrong currency " + fieldValue);
                                            }
                                            transaction.currency = fieldValue;
                                            break;
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                throw new Exception("IllegalArgumentException");
                            } catch (ParseException e) {
                                throw new Exception("ParseException");
                            }
                        }
                    }
                    transaction.time += deltaTime++;
                    transactions.add(transaction);
                } else {
                    // first line of csv-file is table headline
                    parseLine = true;
                    header = line;
                }
            }
            return transactions;
        } catch (FileNotFoundException e) {
            throw new Exception("Import file not found");
        }
    }

    private Double parseAmount(String fieldValue) {
        fieldValue = fieldValue.trim();
        if (fieldValue.length() > 0) {
            fieldValue = fieldValue.replace(groupSeparator + "", "");
            fieldValue = fieldValue.replace(decimalSeparator, '.');
            double fromAmount = Double.parseDouble(fieldValue);
            return fromAmount * 100.0;
        } else {
            return 0.0;
        }
    }

    public Set<CategoryInfo> collectCategories(List<CsvTransaction> transactions) {
        Set<CategoryInfo> categories = new HashSet<>();
        for (CsvTransaction transaction : transactions) {
            String category = transaction.category;
            if (Utils.isNotEmpty(transaction.categoryParent)) {
                category = transaction.categoryParent+CategoryInfo.SEPARATOR+category;
            }
            if (Utils.isNotEmpty(category)) {
                categories.add(new CategoryInfo(category, false));
                transaction.category = category;
                transaction.categoryParent = null;
            }
        }
        return categories;
    }

    //Workaround function which is needed for reimport of CsvExport files
    public String myTrim(String s) {
        if (Character.isLetter(s.charAt(0))) {
            return s;
        } else {
            return s.substring(1);
        }

    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }
}
