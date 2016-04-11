/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.BundleCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.Currency;
import com.flowzr.model.Total;
import com.flowzr.rates.ExchangeRate;
import com.flowzr.rates.ExchangeRateProvider;
import com.flowzr.utils.Utils;
import com.flowzr.view.NodeInflater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/15/12 16:40 PM
 */
public abstract class AbstractTotalsDetailsFragment extends Fragment implements ActivityLayoutListener {

    private LinearLayout layout;
    private View calculatingNode;
    private Utils u;
    protected boolean shouldShowHomeCurrencyTotal = true;

    private final int titleNodeResId;
	protected DatabaseAdapter db;
	protected MyEntityManager em;
	private ActivityLayout x;
    private MainActivity activity;

    protected AbstractTotalsDetailsFragment(int titleNodeResId) {
        this.titleNodeResId = titleNodeResId;
    }

    public void onAttach(Context a) {
        super.onAttach(a);
        setHasOptionsMenu(true);
        activity=(MainActivity)a;
    }
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

		LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		NodeInflater nodeInflater = new NodeInflater(layoutInflater);

		x = new ActivityLayout(nodeInflater, this);
		db = new DatabaseAdapter(this.getActivity());
		db.open();
		em = db.em();
        

        u = new Utils(this.getActivity());
        layout = (LinearLayout)getView().findViewById(R.id.list);
        calculatingNode = x.addTitleNodeNoDivider(layout, R.string.calculating);

        internalOnCreate();
        calculateTotals();
    }



    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.totals_details, container, false);
	}
    
    protected void internalOnCreate() {}

    private void calculateTotals() {
        CalculateAccountsTotalsTask task = new CalculateAccountsTotalsTask();
        task.execute();
    }

    private class CalculateAccountsTotalsTask extends AsyncTask<Void, Void, TotalsInfo> {

        @Override
        protected TotalsInfo doInBackground(Void... voids) {
            prepareInBackground();
            Total[] totals = getTotals();
            Total totalInHomeCurrency = getTotalInHomeCurrency();
            Currency homeCurrency = totalInHomeCurrency.currency;
            ExchangeRateProvider rates = db.getLatestRates();
            List<TotalInfo> result = new ArrayList<>();
            for (Total total : totals) {
            	if (total.currency!=null) {
            		ExchangeRate rate = rates.getRate(total.currency, homeCurrency);
                	TotalInfo info = new TotalInfo(total, rate);
                	result.add(info);
            	}
            }
            Collections.sort(result, new Comparator<TotalInfo>() {
                @Override
                public int compare(TotalInfo thisTotalInfo, TotalInfo thatTotalInfo) {
                    String thisName = thisTotalInfo.total.currency.name;
                    String thatName = thatTotalInfo.total.currency.name;
                    return thisName.compareTo(thatName);
                }
            });
            return new TotalsInfo(result, totalInHomeCurrency);
        }

        @Override
        protected void onPostExecute(TotalsInfo totals) {
            calculatingNode.setVisibility(View.GONE);
            try {
	            for (TotalInfo total : totals.totals) {
	                String title = getString(titleNodeResId, total.total.currency.name);
	                addAmountNode(total.total, title);
	            }
	            if (shouldShowHomeCurrencyTotal) {
	                addAmountNode(totals.totalInHomeCurrency, getString(R.string.home_currency_total));
	            }
            } catch (Exception e) {
            	//fragment not attached
            }
        }

        private void addAmountNode(Total total, String title) {
            x.addTitleNodeNoDivider(layout, title);
            if (total.isError()) {
                addAmountAndErrorNode(total);
            } else {
                addSingleAmountNode(total);
            }
        }

        private void addAmountAndErrorNode(Total total) {
            TextView data = x.addInfoNode(layout, -1, R.string.not_available, "");
            Drawable dr = getResources().getDrawable(R.drawable.ic_error);
            dr.setBounds(0, 0, dr != null ? dr.getIntrinsicWidth() : 0, dr.getIntrinsicHeight());
            if (total.currency == Currency.EMPTY) {
                data.setText(R.string.currency_make_default_warning);
            } else {
                data.setText(total.getError(AbstractTotalsDetailsFragment.this.getActivity()));
            }
            data.setError("Error!", dr);
            data.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
                    Bundle bundle = new Bundle();
                    bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,ExchangeRateActivity.class.getCanonicalName());
                    activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
				}
			});
        }

        private void addSingleAmountNode(Total total) {
            TextView label = x.addInfoNodeSingle(layout, -1, "");
            u.setAmountText(label, total);
        }

    }

    protected abstract Total getTotalInHomeCurrency();

    protected abstract Total[] getTotals();

    protected void prepareInBackground() { }

    private static class TotalInfo {

        public final Total total;
        public final ExchangeRate rate;

        public TotalInfo(Total total, ExchangeRate rate) {
            this.total = total;
            this.rate = rate;
        }
    }
    
    private static class TotalsInfo {
        
        public final List<TotalInfo> totals;
        public final Total totalInHomeCurrency;

        public TotalsInfo(List<TotalInfo> totals, Total totalInHomeCurrency) {
            this.totals = totals;
            this.totalInHomeCurrency = totalInHomeCurrency;
        }

    }
    

}
