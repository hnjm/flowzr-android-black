package com.flowzr.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.model.Category;
import com.flowzr.model.Currency;
import com.flowzr.model.TransactionAttribute;
import com.flowzr.widget.AmountInput;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public class SplitTransactionActivity extends AbstractSplitActivity implements CategorySelector.CategorySelectorListener {

    private TextView amountTitle;
    private AmountInput amountInput;

    private CategorySelector categorySelector;

    public SplitTransactionActivity() {
        super(R.layout.split_fixed);
    }

    @Override
    public String getMyTag() {
        return MyFragmentAPI.REQUEST_SPLITTRANSACTION_FINISH;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.split_fixed;
    }


    @Override
    protected void createUI(LinearLayout layout) {
        categorySelector.createNode(layout, false);

        amountInput = new AmountInput(getContext());
        amountInput.setOwner(activity);
        amountInput.setOnAmountChangedListener(new AmountInput.OnAmountChangedListener() {
            @Override
            public void onAmountChanged(long oldAmount, long newAmount) {
                setUnsplitAmount(split.unsplitAmount - newAmount);
            }
        });
        View v = x.addEditNode(layout, R.string.amount, amountInput);
        amountTitle = (TextView) v.findViewById(R.id.label);
        categorySelector.createAttributesLayout(layout);

    }

    @Override
    protected void fetchData() {
        categorySelector = new CategorySelector(activity,this, db, x);
        categorySelector.setListener(this);
        categorySelector.doNotShowSplitCategory();
        categorySelector.fetchCategories(false);
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        categorySelector.selectCategory(split.categoryId);
        setAmount(split.fromAmount);
    }

    @Override
    protected boolean updateFromUI() {
        super.updateFromUI();
        split.fromAmount = amountInput.getAmount();
        split.categoryAttributes = getAttributes();
        return true;
    }

    private Map<Long, String> getAttributes() {
        List<TransactionAttribute> attributeList = categorySelector.getAttributes();
        Map<Long, String> attributes = new HashMap<>();
        for (TransactionAttribute ta : attributeList) {
            attributes.put(ta.attributeId, ta.value);
        }
        return attributes;
    }

    @Override
    public void onCategorySelected(Category category, boolean selectLast) {
        if (category.isIncome()) {
            amountInput.setIncome();
        } else {
            amountInput.setExpense();
        }
        split.categoryId = category.id;
        categorySelector.addAttributes(split);
    }

    private void setAmount(long amount) {
        amountInput.setAmount(amount);
        Currency c = getCurrency();
        amountInput.setCurrency(c);
        amountTitle.setText(getString(R.string.amount)+" ("+c.name+")");
    }

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        categorySelector.onClick(id);
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        categorySelector.onSelectedId(id, selectedId);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            amountInput.processActivityResult(requestCode, data);
        }
    }

}
