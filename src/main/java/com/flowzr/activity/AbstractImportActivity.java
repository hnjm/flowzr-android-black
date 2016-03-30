/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.flowzr.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.flowzr.R;

import java.io.File;

public abstract class AbstractImportActivity extends AppCompatActivity {

    public static final int IMPORT_FILENAME_REQUESTCODE = 0xff;

    private final int layoutId;
    protected ImageButton bBrowse;
    protected EditText edFilename;

    public AbstractImportActivity(int layoutId) {
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
        initToolbar();
        bBrowse = (ImageButton) findViewById(R.id.btn_browse);
        bBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });
        edFilename = (EditText) findViewById(R.id.edFilename);
        internalOnCreate();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    protected void openFile() {
        String filePath = edFilename.getText().toString();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        File file = new File(filePath);
        intent.setData(Uri.fromFile(file));
        intent.setType("*/*");

        try {
            startActivityForResult(intent, IMPORT_FILENAME_REQUESTCODE);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(this, R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
        }

    }

    protected abstract void internalOnCreate();

    protected abstract void updateResultIntentFromUi(Intent data);

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_FILENAME_REQUESTCODE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    String filePath = fileUri.getPath();
                    if (filePath != null) {
                        edFilename.setText(filePath);
                        savePreferences();
                    }
                }
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        restorePreferences();
    }

    protected abstract void savePreferences();

    protected abstract void restorePreferences();



    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                onBackPressed();
            }
        }
        return super.onOptionsItemSelected(item);
    }

}
