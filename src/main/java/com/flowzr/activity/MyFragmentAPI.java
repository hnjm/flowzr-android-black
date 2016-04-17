/*******************************************************************************
 * Copyright (c) 2016 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Emmanuel Florent - initial commit
 *
 ******************************************************************************/

package com.flowzr.activity;

import android.content.Intent;
import android.os.Bundle;

public interface MyFragmentAPI {



    String EDIT_ENTITY_REQUEST="EDIT_ENTITY_REQUEST";
    String REQUEST_MYENTITY_FINISH="REQUEST_MYENTITY_FINISH";
    String REQUEST_ACTIVITY="REQUEST_ACTIVITY";
    String REQUEST_BLOTTER = "REQUEST_BLOTTER";

    //String RESPONSE_CODE = "RESPONSE_CODE";
    // GENERICS EXTRA
    String RESULT_EXTRA="RESULT_EXTRA";
    String ENTITY_ID_EXTRA = "ENTITY_ID_EXTRA";

    // REPORTS
    String REQUEST_REPORTS="REQUEST_REPORTS";
    String EXTRA_REPORT_TYPE = "EXTRA_REPORT_TYPE";
    String CONVENTIONAL_REPORTS="CONVENTIONAL_REPORTS";

    // ROUTER
    String ENTITY_CLASS_EXTRA="ENTITY_CLASS_EXTRA";
    String ENTITY_REQUEST_EXTRA="ENTITY_REQUEST_EXTRA";
    // HELPERS / SELECTOR / TOOLS

    // From Widget
    String REQUEST_NEW_TRANSACTION_FROM_TEMPLATE="REQUEST_NEW_TRANSACTION_FROM_TEMPLATE";
    // From Service
    String REQUEST_MASS_OP = "REQUEST_MASSOP";

    // ACCOUNT EDIT
    String REQUEST_ACCOUNT_FINISH = "REQUEST_ACCOUNT_FINISH";

    // BUDGET EDIT
    String REQUEST_BUDGET_FINISH="REQUEST_BUDGET_FINISH";

    // ATTRIBUTE ATTRIBUTE EDIT
    //String REQUEST_ATTRIBUTE_FINISH="REQUEST_ATTRIBUTE_FINISH";

    // RECURRENCE
    String REQUEST_RECURRENCE_FINISH="REQUEST_RECURRENCE_FINISH";
    String REQUEST_PURGEACCOUNT_FINISH="REQUEST_PURGEACCOUNT_FINISH";

    String REQUEST_DATEFILTER_FINISH ="REQUEST_DATEFILTER_FINISH";
    String REQUEST_ABSTRACT_TRANSACTION_FINISH = "REQUEST_ABSTRACT_TRANSACTION_FINISH";




    String REQUEST_SPLITTRANSACTION_FINISH = "REQUEST_SPLITTRANSACTION_FINISH ";
    String REQUEST_SPLITTRANSFER_FINISH="REQUEST_SPLITTRANSFERFINISH";
    String REQUEST_TRANSFER_FINISH="REQUEST_TRANSFER_FINISH";

    // FILTERS
    String REQUEST_WHEREFILTER_FINISH = "REQUEST_WHERE_FINISH";
    String REQUEST_REPORTFILTER_FINISH = "REQUEST_REPORTFILTER_FINISH";
    String REQUEST_BLOTTERFILTER_FINISH ="REQUEST_BLOTTERFILTER_FINISH";

    // SPLIT EDIT FROM TRANSACTIONS
    String REQUEST_SPLIT_FINISH="REQUEST_SPLIT_FINISH";

    //public final static String REQUEST_MASS_OP = "REQUEST_MASSOP";
    //public final static String REQUEST_BLOTTER_TOTALS="REQUEST_BLOTTER_TOTALS";
    //public final static String REQUEST_EXCHANGE_RATES="REQUEST_EXCHANGE_RATES";
    //public final static String REQUEST_BUDGET_BLOTTER="REQUEST_BUDGET_BLOTTER";
    //public final static String REQUEST_REPORTS="REQUEST_REPORTS";
    //public final static String REQUEST_PLANNER="REQUEST_PLANNER";
    //public final static String REQUEST_CATEGORY_SELECTOR="REQUEST_CATEGORY_SELECTOR";
    //public final static String REQUEST_SCHEDULED="REQUEST_SCHEDULED";
    //public final static String REQUEST_TEMPLATES="REQUEST_TEMPLATES";
    //public final static String REQUEST_BUDGET_TOTALS="REQUEST_BUDGET_TOTALS";
    //public final static String REQUEST_ACCOUNT_TOTALS="REQUEST_ACCOUNT_TOTALS";


    //public static final int TAB_BLOTTER = 1;
    //public static final String REQUEST_SPLIT_BLOTTER = "REQUEST_SPLIT_BLOTTER";
    //public final static String REQUEST_BLOTTER_TOTALS = "REQUEST_BLOTTER_TOTALS";

    //
    //public final static String REQUEST_TEMPLATES = "REQUEST_TEMPLATES";
    //public final static String REQUEST_EXCHANGE_RATES = "REQUEST_EXCHANGE_RATES";
    //public final static String REQUEST_BUDGET_BLOTTER = "REQUEST_BUDGET_BLOTTER";

    //public final static String REQUEST_PLANNER = "REQUEST_PLANNER";
    //public final static String REQUEST_CATEGORY_SELECTOR = "REQUEST_CATEGORY_SELECTOR";
    //public final static String REQUEST_SCHEDULED = "REQUEST_SCHEDULED";
    //public final static String REQUEST_NEW_TRANSACTION_FROM_TEMPLATE = "REQUEST_NEW_TRANSACTION_FROM_TEMPLATE";
    //public final static String REQUEST_BUDGET_TOTALS = "REQUEST_BUDGET_TOTALS";
    //public final static String REQUEST_ACCOUNT_TOTALS = "REQUEST_ACCOUNT_TOTALS";


    void onFragmentMessage(String TAG, Bundle data);


}