package com.flowzr.export.csv;

import android.app.ProgressDialog;
import android.content.Context;

import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;

public class CsvExportTask extends ImportExportAsyncTask {

    private final CsvExportOptions options;

	public CsvExportTask(Context context, ProgressDialog dialog, CsvExportOptions options) {
		super(context, dialog);
		this.options = options;
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
		CsvExport export = new CsvExport(context, db, options);
        String backupFileName = export.export();
        if (options.uploadToDropbox) {
            doUploadToDropbox(context, backupFileName);
        }
        return backupFileName;
	}

	@Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}
