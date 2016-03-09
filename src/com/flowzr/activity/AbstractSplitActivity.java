package com.flowzr.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.model.Account;
import com.flowzr.model.Currency;
import com.flowzr.model.Transaction;
import com.flowzr.utils.CurrencyCache;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.Utils;

import static com.flowzr.utils.Utils.text;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public abstract class AbstractSplitActivity extends AbstractEditorActivity {

    protected EditText noteText;
    protected TextView unsplitAmountText;

    protected Account fromAccount;
    protected Currency originalCurrency;
    protected Utils utils;
    protected Transaction split;

    private ProjectSelector projectSelector;

    private final int layoutId;

    protected AbstractSplitActivity(int layoutId) {
        this.layoutId = layoutId;
    }

    protected void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutId);

        fetchData();
        projectSelector = new ProjectSelector(this, em, x);
        projectSelector.fetchProjects();

        utils  = new Utils(this);
        split = Transaction.fromIntentAsSplit(getIntent());
        if (split.fromAccountId > 0) {
            fromAccount = db.em().getAccount(split.fromAccountId);
        }
        if (split.originalCurrencyId > 0) {
            originalCurrency = CurrencyCache.getCurrency(em, split.originalCurrencyId);
        }

        LinearLayout layout = (LinearLayout)findViewById(R.id.list);

        createUI(layout);
        createCommonUI(layout);
        updateUI();
    }

    private void createCommonUI(LinearLayout layout) {
        unsplitAmountText = x.addInfoNode(layout, R.id.add_split, R.string.unsplit_amount, "0");
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        noteText = (EditText) layoutInflater.inflate(R.layout.edit_text, null);

        x.addEditNode(layout, R.string.note, noteText);

        projectSelector.createNode(layout);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
                saveAndFinish();      	        		
        		return true;
	    	case R.id.action_cancel:
				setResult(RESULT_CANCELED);
				finish();
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
    
    protected abstract void fetchData();

    protected abstract void createUI(LinearLayout layout);

    @Override
    protected void onClick(View v, int id) {
        projectSelector.onClick(id);
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        projectSelector.onSelectedPos(id, selectedPos);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        projectSelector.onActivityResult(requestCode, resultCode, data);
    }

    private void saveAndFinish() {
        Intent data = new Intent();
        if (updateFromUI()) {
            split.toIntentAsSplit(data);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    }

    protected boolean updateFromUI() {
        split.note = text(noteText);
        split.projectId = projectSelector.getSelectedProjectId();
        return true;
    }

    protected void updateUI() {
        projectSelector.selectProject(split.projectId);
        setNote(split.note);
    }

    private void setNote(String note) {
        noteText.setText(note);
    }

    protected void setUnsplitAmount(long amount) {
        Currency currency = getCurrency();
        utils.setAmountText(unsplitAmountText, currency, amount, false);
    }

    protected Currency getCurrency() {
        return originalCurrency != null ? originalCurrency : (fromAccount != null ? fromAccount.currency : Currency.defaultCurrency());
    }

    @Override
    protected boolean shouldLock() {
        return MyPreferences.isPinProtectedNewTransaction(this);
    }

}
