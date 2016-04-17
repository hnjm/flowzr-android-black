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

package com.flowzr.widget;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.activity.AbstractEditorActivity;
import com.flowzr.activity.AbstractTransactionActivity;
import com.flowzr.activity.ActivityLayout;
import com.flowzr.activity.MainActivity;
import com.flowzr.model.Currency;

import static com.flowzr.activity.AbstractEditorActivity.setVisibility;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 6/24/11 6:45 PM
 */
public class RateLayoutView implements RateNodeOwner {

    private final AbstractEditorActivity activity;
    private final ActivityLayout x;
    private final LinearLayout layout;

    private AmountInput amountInputFrom;
    private AmountInput amountInputTo;

    private RateNode rateNode;
    private View amountInputFromNode;
    private View amountInputToNode;
    private int amountFromTitleId;
    private int amountToTitleId;

    private AmountInput.OnAmountChangedListener amountFromChangeListener;
    private AmountInput.OnAmountChangedListener amountToChangeListener;

    private Currency currencyFrom;
    private Currency currencyTo;

    public RateLayoutView(AbstractEditorActivity activity, ActivityLayout x, LinearLayout layout) {
        this.activity = activity;
        this.x = x;
        this.layout = layout;
    }

    public void setAmountFromChangeListener(AmountInput.OnAmountChangedListener amountFromChangeListener) {
        this.amountFromChangeListener = amountFromChangeListener;
    }

    public void setAmountToChangeListener(AmountInput.OnAmountChangedListener amountToChangeListener) {
        this.amountToChangeListener = amountToChangeListener;
    }

    private void createUI(AbstractEditorActivity context, int fromAmountTitleId, int toAmountTitleId) {
        //amount from
        amountInputFrom = new AmountInput(context.getContext());
        amountInputFrom.setOwner(context);
        amountInputFrom.setExpense();
        amountFromTitleId = fromAmountTitleId;
        amountInputFromNode = x.addEditNode(layout, fromAmountTitleId, amountInputFrom);
        //amount to & rate
        amountInputTo = new AmountInput(context.getContext());
        amountInputTo.setOwner(context);
        amountInputTo.setIncome();
        amountToTitleId = toAmountTitleId;
        amountInputToNode = x.addEditNode(layout, toAmountTitleId, amountInputTo);
        amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
        amountInputFrom.setOnAmountChangedListener(onAmountFromChangedListener);
        setVisibility(amountInputToNode, View.GONE);
        rateNode = new RateNode(this, x, layout);
        setVisibility(rateNode.rateInfoNode, View.GONE);

    }

    public void createTransferUI(AbstractEditorActivity context) {
        createUI(context, R.string.amount_from, R.string.amount_to);
        amountInputFrom.disableIncomeExpenseButton();
        amountInputTo.disableIncomeExpenseButton();
    }

    public void createTransactionUI(AbstractEditorActivity context) {
        createUI(context,R.string.amount, R.string.amount);
        amountInputTo.disableIncomeExpenseButton();
    }

    public void setIncome() {
        amountInputFrom.setIncome();
        amountInputTo.setIncome();
    }

    public void setExpense() {
        amountInputFrom.setExpense();
        amountInputTo.setExpense();
    }

    public void selectCurrencyFrom(Currency currency) {
        currencyFrom = currency;
        amountInputFrom.setCurrency(currencyFrom);
        updateTitle(amountInputFromNode, amountFromTitleId, currencyFrom);
        checkNeedRate();
    }

    public void selectCurrencyTo(Currency currency) {
        currencyTo = currency;
        amountInputTo.setCurrency(currencyTo);
        updateTitle(amountInputToNode, amountToTitleId, currencyTo);
        checkNeedRate();
    }

    private void updateTitle(View node, int titleId, Currency currency) {
        TextView title = (TextView) node.findViewById(R.id.label);
        if (currency != null && currency.id > 0) {
            title.setText(currency.name);
        } else {
            title.setText(activity.getString(titleId));
        }
    }

    private void checkNeedRate() {
        if (isDifferentCurrencies()) {
            setVisibility(rateNode.rateInfoNode, View.VISIBLE);
            setVisibility(amountInputToNode, View.VISIBLE);
            calculateRate();
        } else {
            setVisibility(rateNode.rateInfoNode, View.GONE);
            setVisibility(amountInputToNode, View.GONE);
        }
    }

    private void calculateRate() {
        long amountFrom = amountInputFrom.getAmount();
        long amountTo = amountInputTo.getAmount();
        float r = 1.0f*amountTo/amountFrom;
        if (!Float.isNaN(r)) {
            rateNode.setRate(r);
        }
        rateNode.updateRateInfo();
    }

    public long getFromAmount() {
        return amountInputFrom.getAmount();
    }

    public long getToAmount() {
        if (isDifferentCurrencies()) {
            return amountInputTo.getAmount();
        } else {
            return -amountInputFrom.getAmount();
        }
    }

    private boolean isDifferentCurrencies() {
        return currencyFrom != null && currencyTo != null && currencyFrom.id != currencyTo.id;
    }

    public void onActivityResult(int requestCode, Intent data) {
        if (amountInputFrom.processActivityResult(requestCode, data)) {
            calculateRate();
            return;
        }
        if (amountInputTo.processActivityResult(requestCode, data)) {
            calculateRate();
            return;
        }
        if (requestCode == RateNode.EDIT_RATE) {
            String amount = data.getStringExtra(AmountInput.EXTRA_AMOUNT);
            if (amount != null) {
                rateNode.setRate(Float.parseFloat(amount));
                updateToAmountFromRate();
            }
        }
    }

    private final AmountInput.OnAmountChangedListener onAmountFromChangedListener = new AmountInput.OnAmountChangedListener(){
        @Override
        public void onAmountChanged(long oldAmount, long newAmount) {
            double r = rateNode.getRate();
            if (r > 0) {
                long amountFrom = amountInputFrom.getAmount();
                long amountTo = Math.round(r*amountFrom);
                amountInputTo.setOnAmountChangedListener(null);
                amountInputTo.setAmount(amountTo);
                amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
            } else {
                long amountFrom = amountInputFrom.getAmount();
                long amountTo = amountInputTo.getAmount();
                if (amountFrom > 0) {
                    rateNode.setRate(1.0f * amountTo / amountFrom);
                }
            }
            if (amountInputFrom.isIncomeExpenseEnabled()) {
                if (amountInputFrom.isExpense()) {
                    amountInputTo.setExpense();
                } else {
                    amountInputTo.setIncome();
                }
            }
            rateNode.updateRateInfo();
            if (amountFromChangeListener != null) {
                amountFromChangeListener.onAmountChanged(oldAmount, newAmount);
            }
        }
    };

    private final AmountInput.OnAmountChangedListener onAmountToChangedListener = new AmountInput.OnAmountChangedListener(){
        @Override
        public void onAmountChanged(long oldAmount, long newAmount) {
            long amountFrom = amountInputFrom.getAmount();
            long amountTo = amountInputTo.getAmount();
            if (amountFrom > 0) {
                rateNode.setRate(1.0f * amountTo / amountFrom);
            }
            rateNode.updateRateInfo();
            if (amountToChangeListener != null) {
                amountToChangeListener.onAmountChanged(oldAmount, newAmount);
            }
        }
    };

    public void setFromAmount(long fromAmount) {
        amountInputFrom.setAmount(fromAmount);
        calculateRate();
    }

    public void setToAmount(long toAmount) {
        amountInputTo.setAmount(toAmount);
        calculateRate();
    }

    private void updateToAmountFromRate() {
        double r = rateNode.getRate();
        long amountFrom = amountInputFrom.getAmount();
        long amountTo = (long)Math.floor(r*amountFrom);
        amountInputTo.setOnAmountChangedListener(null);
        amountInputTo.setAmount(amountTo);
        rateNode.updateRateInfo();
        amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
    }

    public void openFromAmountCalculator(String title) {
        amountInputFrom.openCalculator(title);
    }


    @Override
    public void onBeforeRateDownload() {
        amountInputFrom.setEnabled(false);
        amountInputTo.setEnabled(false);
        rateNode.disableAll();
    }

    @Override
    public Currency getCurrencyFrom() {
        return currencyFrom;
    }

    @Override
    public Currency getCurrencyTo() {
        return currencyTo;
    }

    public long getCurrencyToId() {
        return currencyTo != null ? currencyTo.id : 0;
    }

    @Override
    public void onAfterRateDownload() {
        amountInputFrom.setEnabled(true);
        amountInputTo.setEnabled(true);
        rateNode.enableAll();
    }

    @Override
    public void onSuccessfulRateDownload() {
        updateToAmountFromRate();
    }

    @Override
    public void onRateChanged() {
        updateToAmountFromRate();
    }

    @Override
    public Activity getActivity() {
        return activity.getActivity();
    }


    public void selectSameCurrency(Currency currency) {
        selectCurrencyFrom(currency);
        selectCurrencyTo(currency);
    }

    public void hideFromAmount() {
        this.amountInputFromNode.setVisibility(View.GONE);
    }
    public void invertSign() {
        //amountInputFromNode.findViewById(R.id.toggle).callOnClick();
        long amount = amountInputFrom.getAmount();
        amountInputFrom.onAmountChangedListener.onAmountChanged(-amount, amount);
        // amountFromChangeListener.onAmountChanged(amountInputFrom.getAmount(), amountInputFrom.getAmount() - amountInputFrom.getAmount()*2);
        //this.amountInputFromNode.setVisibility(View.GONE);
    }

}
