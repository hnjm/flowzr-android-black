/*
 * Copyright (c) 2014 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - port to Android API 11+
 */

package com.flowzr.export.dropbox;


import android.app.ProgressDialog;
import android.content.Context;

import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.backup.DatabaseImport;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;
import com.flowzr.export.ImportExportAsyncTaskListener;


public class DropboxRestoreTask extends ImportExportAsyncTask {

    private final String backupFile;

    public DropboxRestoreTask(final BackupListActivity backupListActivity, ProgressDialog dialog, String backupFile) {
        super(backupListActivity, dialog);
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
                //mainActivity.refreshCurrentTab();
            }
        });
        this.backupFile = backupFile;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        Dropbox dropbox = new Dropbox(context);
        DatabaseImport.createFromDropboxBackup(context, db, dropbox, backupFile).importDatabase();
        return true;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return context.getString(R.string.restore_database_success);
    }

}
