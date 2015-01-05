/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - port to Android API 11+
 */

package com.flowzr.export.qif;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.activity.MainActivity;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;
import com.flowzr.export.ImportExportAsyncTaskListener;


public class QifImportTask extends ImportExportAsyncTask {

    private final QifImportOptions options;
    private final Handler handler;

    public QifImportTask(final BackupListActivity backupListActivity, Handler handler, ProgressDialog dialog, QifImportOptions options) {
        super(backupListActivity, dialog);
        this.options = options;
        this.handler = handler;
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
                backupListActivity.finish();
            }
        });
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        try {
            QifImport qifImport = new QifImport(context, db, options);
            qifImport.importDatabase();
            return null;
        } catch (Exception e) {
            Log.e("Flowzr", "Qif import error", e);
            handler.sendEmptyMessage(R.string.qif_import_error);
            return e;
        }
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return context.getString(R.string.qif_import_success);
    }

}
