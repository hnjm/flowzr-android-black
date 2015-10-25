/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - port to Android API 11+
 *                      - port to Google API V3
 ******************************************************************************/

package com.flowzr.export.docs;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.backup.DatabaseImport;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTaskListener;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import java.io.IOException;


/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:16 AM
 */
public class DriveRestoreTask  extends  ApiClientAsyncTask<Void, Void, Object> {

    private final String strDriveId;
    private DatabaseAdapter db;

    public DriveRestoreTask(final BackupListActivity backupListActivity, ProgressDialog dialog, String pStrDriveId) {
        super(backupListActivity, dialog);
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
                backupListActivity.finish();


            }
        });
        this.strDriveId = pStrDriveId;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        this.db=db;
        return true;
    }

    @Override
    protected Object doInBackgroundConnected(String... params) {
        DatabaseAdapter db = new DatabaseAdapter(context);
        db.open();
        try {
            DriveFile file= Drive.DriveApi.getFile(mClient, DriveId.decodeFromString(strDriveId));
            try {
                DatabaseImport.createFromGoogleDriveBackup(context, db,mClient, file).importDatabase();
                return true;
            } catch (IOException e) {
                return e;
            }
        } catch(Exception ex){
            Log.e("Financisto", "Unable to do import/export", ex);
            return ex;
        } finally {
            db.close();
        }
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return context.getString(R.string.restore_database_success);
    }

}
