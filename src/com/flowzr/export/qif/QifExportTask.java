package com.flowzr.export.qif;

import android.app.ProgressDialog;
import android.content.Context;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;

public class QifExportTask extends ImportExportAsyncTask {

	private final QifExportOptions options;

	public QifExportTask(Context context, ProgressDialog dialog, QifExportOptions options) {
		super(context, dialog);
        this.options = options;
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
        QifExport qifExport = new QifExport(context, db, options);
        String backupFileName = qifExport.export();
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
