package com.flowzr.export;

import android.app.ProgressDialog;
import android.content.Context;

import com.flowzr.backup.DatabaseExport;
import com.flowzr.db.DatabaseAdapter;

public class BackupExportTask extends ImportExportAsyncTask {

    public final boolean uploadToDropbox;

    public volatile String backupFileName;
	
	public BackupExportTask(Context context, ProgressDialog dialog, boolean uploadToDropbox) {
		super(context, dialog);
        this.uploadToDropbox = uploadToDropbox;
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
		DatabaseExport export = new DatabaseExport(context, db.db(), true);
        backupFileName = export.export();
        if (uploadToDropbox) {
            doUploadToDropbox(context, backupFileName);
        }
        return backupFileName;
	}

    @Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}