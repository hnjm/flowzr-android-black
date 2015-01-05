/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.flowzr.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.flowzr.R;
import com.flowzr.utils.PinProtection;

import java.io.File;

public abstract class AbstractSyncActivity extends Activity {

    public static final int IMPORT_FILENAME_REQUESTCODE = 0xff;

    private final int layoutId;


    public AbstractSyncActivity(int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutId);
        internalOnCreate();
    }


    protected abstract void internalOnCreate();

    protected abstract void updateResultIntentFromUi(Intent data);

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_FILENAME_REQUESTCODE) {

        }

    }



    protected abstract void savePreferences();

    protected abstract void restorePreferences();

}
