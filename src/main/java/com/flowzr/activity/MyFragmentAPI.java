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

    // GENERICS EXTRA
    String RESULT_EXTRA="RESULT_EXTRA";
    String ENTITY_ID_EXTRA = "ENTITY_ID_EXTRA";
    String ENTITY_REQUEST_EXTRA="ENTITY_REQUEST_EXTRA";

    // ROUTER
    String EDIT_ENTITY_REQUEST="EDIT_ENTITY_REQUEST";
    String REQUEST_MYENTITY_FINISH="REQUEST_MYENTITY_FINISH";
    String REQUEST_ACTIVITY="REQUEST_ACTIVITY";
    String REQUEST_BLOTTER = "REQUEST_BLOTTER";
    String ENTITY_CLASS_EXTRA="ENTITY_CLASS_EXTRA";
    //
    String EXTRA_REPORT_TYPE = "EXTRA_REPORT_TYPE";
    String CONVENTIONAL_REPORTS="CONVENTIONAL_REPORTS";


    void onFragmentMessage(String TAG, Bundle data);

}