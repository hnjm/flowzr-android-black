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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;

import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportException;
import com.flowzr.utils.MyPreferences;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataBuffer;

public class DriveListFilesTask extends  ApiClientAsyncTask<Void, Void, Object>   {

    private final BackupListActivity context;
    private final Dialog dialog;
    private volatile int error = 0;

    public DriveListFilesTask(BackupListActivity context, ProgressDialog dialog) {
        super(context, dialog);
        this.context = context;
        this.dialog = dialog;
    }

    @Override
    protected Object doInBackgroundConnected(String... params) {

        String folderName= MyPreferences.getBackupFolder(context);
        try {
            DriveId folderId= ApiClientAsyncTask.getOrCreateDriveFolder(this.getGoogleApiClient(), folderName);
            DriveFolder folder = Drive.DriveApi.getFolder(getGoogleApiClient(), folderId);

            DriveApi.MetadataBufferResult result = folder.listChildren(getGoogleApiClient()).await();
            if (!result.getStatus().isSuccess()) {
                return new ImportExportException(R.string.gdocs_service_error);
            }
            return result.getMetadataBuffer();

        } catch (ImportExportException e) {
            return e;
        }
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        return null;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return null;
    }


    @Override
    protected void onPostExecute(Object result) {
        dialog.dismiss();
        if (result instanceof ImportExportException) {
            context.showErrorPopup(context, ((ImportExportException)result).errorResId);
            return;
        }

        // com.google.android.gms.drive.MetadataBuffer c
        // annot be cast to
        // com.google.android.gms.drive.DriveApi$MetadataBufferResult
        //DriveApi.MetadataBuffer backupFiles = (DriveApi.MetadataBuffer) result;
        context.doImportFromGoogleDrive((MetadataBuffer) result);
    }

}
