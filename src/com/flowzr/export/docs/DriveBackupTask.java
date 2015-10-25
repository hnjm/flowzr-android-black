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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.backup.DatabaseExport;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;
import com.flowzr.export.ImportExportException;
import com.flowzr.utils.MyPreferences;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.Metadata;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:23 AM
 */


public class DriveBackupTask  extends  ApiClientAsyncTask<Void, Void, Object>  {

    public DriveBackupTask(Context context,ProgressDialog dialog) {
        super(context,dialog);
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        return null;
    }

    @Override
    protected Object doInBackgroundConnected(String... params) {
        DatabaseAdapter db=new DatabaseAdapter(context);
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        String folder = MyPreferences.getBackupFolder(context);
        try {
            return export.exportOnline(folder,getGoogleApiClient());
        } catch (Exception e) {
            e.printStackTrace();
            return new ImportExportException(R.string.gdocs_backup_failed);
        }
    }

    @Override
    protected String getSuccessMessage(Object result) {
        Log.i("flowzr", "done");
        Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show();
        return String.valueOf(result);

    }

}
