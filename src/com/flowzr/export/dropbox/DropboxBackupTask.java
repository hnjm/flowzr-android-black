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

import com.flowzr.activity.BackupListActivity;
import com.flowzr.activity.MainActivity;
import com.flowzr.backup.DatabaseExport;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;

public class DropboxBackupTask extends ImportExportAsyncTask {

    public DropboxBackupTask(BackupListActivity backupListActivity, ProgressDialog dialog) {
        super(backupListActivity, dialog);
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        String backupFileName = export.export();
        doForceUploadToDropbox(context, backupFileName);
        return backupFileName;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }

}
