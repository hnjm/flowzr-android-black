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
import android.os.AsyncTask;


import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;

import com.flowzr.export.Export;
import com.flowzr.export.ImportExportException;
import com.flowzr.utils.MyPreferences;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DriveListFilesTask extends AsyncTask<Void, Void, File[]> {

    private final BackupListActivity context;
    private final Dialog dialog;

    private volatile int error = 0;

    public DriveListFilesTask(BackupListActivity backupListActivity, Dialog dialog) {
        this.context = backupListActivity;
        this.dialog = dialog;
    }

    @Override
    protected File[] doInBackground(Void... contexts) {
        try {
            String googleDriveAccount = MyPreferences.getGoogleDriveAccount(context);
            Drive drive = GoogleDriveClient.create(context,googleDriveAccount);

            String targetFolder = MyPreferences.getBackupFolder(context);

            if (targetFolder == null || targetFolder.equals("")) {
                error = R.string.gdocs_folder_not_configured;
                return null;
            }

            String folderId = GoogleDriveClient.getOrCreateDriveFolder(drive, targetFolder);

            List<File> backupFiles = new ArrayList<File>();
            FileList files = drive.files().list().setQ("mimeType='" + Export.BACKUP_MIME_TYPE + "' and '" + folderId + "' in parents").execute();
            for (com.google.api.services.drive.model.File f : files.getItems()) {
                if ((f.getExplicitlyTrashed() == null || !f.getExplicitlyTrashed()) && f.getDownloadUrl() != null && f.getDownloadUrl().length() > 0) {
                    if (f.getFileExtension().equals("backup")) {
                        backupFiles.add(f);
                    }
                }
            }
            return backupFiles.toArray(new File[backupFiles.size()]);

        } catch (ImportExportException e) {
            error = e.errorResId;
            return null;
        } catch (GoogleAuthException e) {
            error = R.string.gdocs_connection_failed;
            return null;
        } catch (IOException e) {
            error = R.string.gdocs_service_error;
            return null;
        } catch (Exception e) {
            error = R.string.gdocs_service_error;
            return null;
        }
    }

    @Override
    protected void onPostExecute(File[] backupFiles) {
        dialog.dismiss();
        if (error != 0) {
            context.showErrorPopup(context, error);
            return;
        }
        context.doImportFromGoogleDrive(backupFiles);
    }


}
