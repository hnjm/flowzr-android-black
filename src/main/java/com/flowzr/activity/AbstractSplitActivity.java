/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - Port to AppCompat 21,  add icon title
 ******************************************************************************/

package com.flowzr.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        getActivity().setTitle(getString(R.string.accounts));
        return inflater.inflate(layoutId, container, false);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fetchData();
        projectSelector = new ProjectSelector(activity,this, em, x);
        projectSelector.fetchProjects();

        utils  = new Utils(getContext());

        split = Transaction.fromIntentAsSplit(getActivity().getIntent());
        if (split.fromAccountId > 0) {
            fromAccount = db.em().getAccount(split.fromAccountId);
        }
        if (split.originalCurrencyId > 0) {
            originalCurrency = CurrencyCache.getCurrency(em, split.originalCurrencyId);
        }

        LinearLayout layout = (LinearLayout)getView().findViewById(R.id.list);

        createUI(layout);
        createCommonUI(layout);
        updateUI();
    }

    private void createCommonUI(LinearLayout layout) {
        unsplitAmountText = x.addInfoNode(layout, R.id.add_split, R.string.unsplit_amount, "0");
        LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                justFinish();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        projectSelector.onActivityResult(requestCode, resultCode, data);
    }

    private void justFinish() {

    }

    private void saveAndFinish() {
        Intent data = new Intent();
        if (updateFromUI()) {
            split.toIntentAsSplit(data);
            Bundle bundle = data.getExtras();
            activity.onFragmentMessage(MyFragmentAPI.REQUEST_SPLIT_FINISH, bundle);
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
        return MyPreferences.isPinProtectedNewTransaction(getContext());
    }

}
