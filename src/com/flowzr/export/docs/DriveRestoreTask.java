/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.export.docs;

import android.app.ProgressDialog;
import android.content.Context;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.activity.MainActivity;
import com.flowzr.backup.DatabaseImport;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;
import com.flowzr.export.ImportExportAsyncTaskListener;
import com.flowzr.export.ImportExportException;
import com.flowzr.utils.MyPreferences;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:16 AM
 */
public class DriveRestoreTask extends ImportExportAsyncTask {

    private final com.google.api.services.drive.model.File entry;

    public DriveRestoreTask(final BackupListActivity backupListActivity, ProgressDialog dialog, File entry) {
        super(backupListActivity, dialog);
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
            	backupListActivity.finish();
            }
        });
        this.entry = entry;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        try {
            String googleDriveAccount = MyPreferences.getGoogleDriveAccount(context);
            Drive drive = GoogleDriveClient.create(context,googleDriveAccount);
            DatabaseImport.createFromGoogleDriveBackup(context, db, drive, entry).importDatabase();
        } catch (ImportExportException e) {
            throw e;
        } catch (GoogleAuthException e) {
            throw new ImportExportException(R.string.gdocs_connection_failed);
        } catch (IOException e) {
            throw new ImportExportException(R.string.gdocs_io_error);
        } catch (Exception e) {
            throw new ImportExportException(R.string.gdocs_service_error, e);
        }
        return true;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return context.getString(R.string.restore_database_success);
    }

}
