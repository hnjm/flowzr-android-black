/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - adding 2D chart reports
 ******************************************************************************/
package com.flowzr.backup;

import com.flowzr.R;
import android.content.Context;
import com.flowzr.model.Currency;

public enum BackupType {

	RESTORE_FILE(R.string.restore_database, R.string.restore, R.drawable.ic_action_sd_storage){

	},
	RESTORE_GDOC(R.string.restore_database_gdocs, R.string.restore, R.drawable.ic_action_drive){

	},
	RESTORE_DROPBOX(R.string.restore_database_online_dropbox, R.string.restore, R.drawable.ic_action_dropbox){
		
	},
	BACKUP_FILE(R.string.backup_database, R.string.backup, R.drawable.ic_action_sd_storage){

	},
	BACKUP_GDOC(R.string.backup_database_gdocs, R.string.backup, R.drawable.ic_action_drive){
		
	},
	BACKUP_DROPBOX(R.string.backup_database_online_dropbox, R.string.backup, R.drawable.ic_action_dropbox){
		
	},
	REESTORE_DROPBOX(R.string.restore_database_online_dropbox, R.string.backup, R.drawable.ic_action_dropbox){
		
	},

	

    CSV_EXPORT(R.string.csv_export,  R.string.import_export,R.drawable.ic_action_csv) {

    },
    CSV_IMPORT(R.string.csv_import,  R.string.import_export,R.drawable.ic_action_csv) {

    },
    QIF_EXPORT(R.string.qif_export, R.string.import_export, R.drawable.ic_action_qif) {

    },
    QIF_IMPORT(R.string.qif_import,  R.string.import_export,R.drawable.ic_action_qif) {

    };	
	
	public final int titleId;
	public final int summaryId;
	public final int iconId;
	
	BackupType(int titleId, int summaryId, int iconId) {
		this.titleId = titleId;
		this.summaryId = summaryId;
		this.iconId = iconId;
	}
}
