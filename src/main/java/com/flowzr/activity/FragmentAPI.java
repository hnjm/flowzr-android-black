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

public interface FragmentAPI {

    String REQUEST_REPORTS="REQUEST_REPORTS";
    String CONVENTIONAL_REPORTS="CONVENTIONAL_REPORTS";
    String EXTRA_REPORT_TYPE = "EXTRA_REPORT_TYPE";
    String REQUEST_BLOTTER = "REQUEST_BLOTTER";

    void onFragmentMessage(String TAG, Bundle data);

    void onFragmentMessage(int requestCode, int resultCode, Intent data);

}
