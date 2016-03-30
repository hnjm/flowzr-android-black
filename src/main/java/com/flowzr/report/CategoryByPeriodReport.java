/**
 * 
 */
package com.flowzr.report;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.flowzr.R;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.DatabaseHelper.CategoryColumns;
import com.flowzr.db.DatabaseHelper.TransactionColumns;
import com.flowzr.db.MyEntityManager;
import com.flowzr.graph.Report2DChart;
import com.flowzr.graph.Report2DPoint;
import com.flowzr.model.Category;
import com.flowzr.model.Currency;
import com.flowzr.model.PeriodValue;
import com.flowzr.model.ReportDataByPeriod;
import com.flowzr.utils.MyPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 2D Chart Report to display monthly results by Categories.
 * @author Abdsandryk
 */
public class CategoryByPeriodReport extends Report2DChart {
	
	public CategoryByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}

	@Override
	public String getFilterName() {
		if (filterIds.size()>0) {
			long categoryId = filterIds.get(currentFilterOrder);
			Category category = em.getCategory(categoryId);
			if (category!=null) {
				return category.getTitle();
			} else {
				return context.getString(R.string.no_category);
			}
		} else {
			// no category
			return context.getString(R.string.no_category);
		}
	}

	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public void setFilterIds() {
		boolean includeSubCategories = MyPreferences.includeSubCategoriesInReport(context);
		boolean includeNoCategory = MyPreferences.includeNoFilterInReport(context);
		filterIds = new ArrayList<>();
		currentFilterOrder = 0;
		List<Category> categories = em.getAllCategoriesList(includeNoCategory);
		if (categories.size()>0) {
			//Category c;
            for (Category category : categories) {
                if (includeSubCategories) {
                    filterIds.add(category.getId());
                } else {
                    // do not include sub categories
                    if (category.level == 1) {
                        // filter root categories only
                        filterIds.add(category.getId());
                    }
                }
            }
		}
	}

	@Override
	protected void setColumnFilter() {
		columnFilter = TransactionColumns.category_id.name();
	}
	
	/**
	 * Request data and fill data objects (list of points, max, min, etc.)
	 */
	@Override
	protected void build() {
		boolean addSubs = MyPreferences.addSubCategoriesToSum(context);
		if (addSubs) {
			SQLiteDatabase db = em.db();
			Cursor cursor = null;
			try {
				long categoryId = filterIds.get(currentFilterOrder);
				Category parent = em.getCategory(categoryId);
				String where = CategoryColumns.left+" BETWEEN ? AND ?";
				String[] pars = new String[]{String.valueOf(parent.left), String.valueOf(parent.right)};
				cursor = db.query(DatabaseHelper.CATEGORY_TABLE, new String[]{CategoryColumns._id.name()}, where, pars, null, null, null);
				int[] categories = new int[cursor.getCount()+1];
				int i=0;
				while (cursor.moveToNext()) {
					categories[i] = cursor.getInt(0);
					i++;
				}
				categories[i] = filterIds.get(currentFilterOrder).intValue();
				data = new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, categories, em);
			} finally {
				if (cursor!=null) cursor.close();
			}
		} else {
			// only root category
			data = new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, filterIds.get(currentFilterOrder).intValue(), em);
		}
		
		points = new ArrayList<>();
		List<PeriodValue> pvs = data.getPeriodValues();

        for (PeriodValue pv : pvs) {
            points.add(new Report2DPoint(pv));
        }
	}

	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_category);
	}

}
