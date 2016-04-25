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
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.backup.DatabaseExport;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;
import com.flowzr.export.ImportExportException;
import com.flowzr.utils.MyPreferences;
import com.google.android.gms.auth.GoogleAuthException;
//import com.google.android.gms.drive.Drive;
import com.google.api.services.drive.Drive;


import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:23 AM
 */

public class DriveBackupTask extends ImportExportAsyncTask {

    public DriveBackupTask(BackupListActivity backupListActivity, ProgressDialog dialog) {
        super(backupListActivity, dialog);
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        try {
            String folder = MyPreferences.getBackupFolder(context);
            // check the backup folder registered on preferences
            if (folder == null || folder.equals("")) {
                throw new ImportExportException(R.string.gdocs_folder_not_configured);
            }
            String googleDriveAccount = MyPreferences.getGoogleDriveAccount(context);
            Drive drive = GoogleDriveClient.create(context,googleDriveAccount);
            return export.exportOnline(drive, folder);
        } catch (ImportExportException e) {
            throw e;
        } catch (GoogleAuthException e) {
            throw new ImportExportException(R.string.gdocs_connection_failed);
        } catch (IOException e) {
            throw new ImportExportException(R.string.gdocs_service_error);
        } catch (Exception e) {
            throw new ImportExportException(R.string.gdocs_service_error, e);
        }
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }

}
