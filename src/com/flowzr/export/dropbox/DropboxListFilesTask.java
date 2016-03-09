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

import android.app.Dialog;
import android.os.AsyncTask;

import com.flowzr.R;
import com.flowzr.activity.BackupListActivity;
import com.flowzr.export.ImportExportException;

import java.util.List;

public class DropboxListFilesTask extends AsyncTask<Void, Void, String[]> {

    private final BackupListActivity context;
    private final Dialog dialog;

    private volatile int error = 0;

    public DropboxListFilesTask(BackupListActivity backupListActivity, Dialog dialog) {
        this.context = backupListActivity;
        this.dialog = dialog;
    }

    @Override
    protected String[] doInBackground(Void... contexts) {
        try {
            Dropbox dropbox = new Dropbox(context);
            List<String> files = dropbox.listFiles();
            return files.toArray(new String[files.size()]);
        } catch (ImportExportException e) {
            error = e.errorResId;
            return null;
        } catch (Exception e) {
            error = R.string.dropbox_error;
            return null;
        }
    }

    @Override
    protected void onPostExecute(String[] backupFiles) {
        dialog.dismiss();
        if (error != 0) {
            context.showErrorPopup(context, error);
            return;
        }
        context.doImportFromDropbox(backupFiles);
    }


}
