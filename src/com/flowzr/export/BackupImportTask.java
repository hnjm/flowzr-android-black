/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.export;

import android.app.ProgressDialog;
import android.content.Context;
import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.activity.MainActivity;
import com.flowzr.backup.DatabaseImport;
import com.flowzr.db.DatabaseAdapter;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:21 AM
 */
public class BackupImportTask extends ImportExportAsyncTask {

    public BackupImportTask(final BackupListActivity backupListActivity, ProgressDialog dialog) {
        super(backupListActivity, dialog);
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
            	backupListActivity.finish();  
            }
        });
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        DatabaseImport.createFromFileBackup(context, db, params[0]).importDatabase();
        return true;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return context.getString(R.string.restore_database_success);
    }

}
