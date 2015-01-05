/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - implementing 2D chart reports
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;	

import static com.flowzr.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static com.flowzr.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;

import java.io.File;

import com.flowzr.R;
import com.flowzr.adapter.BackupListAdapter;
import com.flowzr.backup.Backup;
import com.flowzr.backup.BackupType;
import com.flowzr.export.BackupExportTask;
import com.flowzr.export.BackupImportTask;
import com.flowzr.export.Export;
import com.flowzr.export.ImportExportAsyncTaskListener;
import com.flowzr.export.csv.CsvExportOptions;
import com.flowzr.export.csv.CsvExportTask;
import com.flowzr.export.csv.CsvImportOptions;
import com.flowzr.export.csv.CsvImportTask;
import com.flowzr.export.docs.DriveBackupTask;
import com.flowzr.export.docs.DriveListFilesTask;
import com.flowzr.export.docs.DriveRestoreTask;
import com.flowzr.export.dropbox.DropboxBackupTask;
import com.flowzr.export.dropbox.DropboxListFilesTask;
import com.flowzr.export.dropbox.DropboxRestoreTask;
import com.flowzr.export.qif.QifExportOptions;
import com.flowzr.export.qif.QifExportTask;
import com.flowzr.export.qif.QifImportOptions;
import com.flowzr.export.qif.QifImportTask;
import com.flowzr.utils.PinProtection;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class BackupListActivity extends ActionBarActivity {

	private static final int ACTIVITY_CSV_EXPORT = 2;
    private static final int ACTIVITY_QIF_EXPORT = 3;
    private static final int ACTIVITY_CSV_IMPORT = 4;
    private static final int ACTIVITY_QIF_IMPORT = 5;
	
	public final BackupType[] backups = new BackupType[]{
			BackupType.BACKUP_FILE,
			BackupType.RESTORE_FILE,
			BackupType.BACKUP_GDOC,
			BackupType.RESTORE_GDOC,
			BackupType.BACKUP_DROPBOX,
			BackupType.REESTORE_DROPBOX,
			BackupType.QIF_EXPORT,
			BackupType.QIF_IMPORT,
			BackupType.CSV_EXPORT,
			BackupType.CSV_IMPORT
	};


	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.prefs, menu);
        return true;
    }
	

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);     
		setContentView(R.layout.main_entities);  

    	//@see: http://stackoverflow.com/questions/16539251/get-rid-of-blue-line, 
        //only way found to remove on various devices 2.3x, 3.0, ...
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#121212")));   

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	
		ListView listview= (ListView)findViewById(R.id.listview);
		listview.setAdapter(new BackupListAdapter(this, backups));  
		listview.setOnItemClickListener(new OnItemClickListener() {
			  @Override
			  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					  switch (position) {
					  	case 0:
					  		doBackup();			  		
					  		break;
					  	case 1:
					  		doImport();			  		
					  		break;
					  	case 2:
					  		doBackupOnGoogleDrive();			  		
					  		break;
					  	case 3:
					  		doRestoreFromGoogleDrive();			  		
					  		break;
					  	case 4:
					  		doBackupOnDropbox();			  		
					  		break;
					  	case 5:
					  		doRestoreFromDropbox();			  		
					  		break;
					  	case 6:
					  		doQifExport();			  		
					  		break;
					  	case 7:
					  		doQifImport();		  		
					  		break;
					  	case 8:
					  		doCsvExport();						  				  		
					  		break;
					  	case 9:
					  		doCsvImport();						  				  		
					  		break;
					  }
			  }
			}); 
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_CSV_EXPORT) {
            if (resultCode == RESULT_OK) {
                CsvExportOptions options = CsvExportOptions.fromIntent(data);
                doCsvExport(options);
            }
        } else if (requestCode == ACTIVITY_QIF_EXPORT) {
            if (resultCode == RESULT_OK) {
                QifExportOptions options = QifExportOptions.fromIntent(data);
                doQifExport(options);
            }
        } else if (requestCode == ACTIVITY_CSV_IMPORT) {
            if (resultCode == RESULT_OK) {
                CsvImportOptions options = CsvImportOptions.fromIntent(data);
                doCsvImport(options);
            }
        } else if (requestCode == ACTIVITY_QIF_IMPORT) {
            if (resultCode == RESULT_OK) {
                QifImportOptions options = QifImportOptions.fromIntent(data);
                doQifImport(options);
            }
        } else if (requestCode == MainActivity.CHANGE_PREFERENCES) {
            scheduleNextAutoBackup(this);
        }        
    }
	

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        	case R.id.action_settings: 
        		startActivity(new Intent(getApplicationContext(),BackupPreferencesActivity.class));
        		return true;
            case android.R.id.home:
            {
                TaskStackBuilder tsb = TaskStackBuilder.create(this);
                final int intentCount = tsb.getIntentCount();
                if (intentCount > 0)
                {
                    Intent upIntent = tsb.getIntents()[intentCount - 1];
                    if (NavUtils.shouldUpRecreateTask(this, upIntent))
                    {
                        // This activity is not part of the application's task, so create a new task with a synthesized back stack.
                        tsb.startActivities();
                        finish();
                    }
                    else
                    {
                        // This activity is part of the application's task, so simply navigate up to the hierarchical parent activity.
                        NavUtils.navigateUpTo(this, upIntent);
                    }
                }
                else
                {
                    onBackPressed();
                }
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }	
	
    

    private void doBackup() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        new BackupExportTask(this, d, true).execute();
    }

    private void doBackupTo() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        final BackupExportTask t = new BackupExportTask(this, d, false);
        t.setShowResultDialog(false);
        t.setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
                String backupFileName = t.backupFileName;
                startBackupToChooser(backupFileName);
            }
        });
        t.execute((String[]) null);
    }

    private void startBackupToChooser(String backupFileName) {
        File file = Export.getBackupFile(this, backupFileName);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getString(R.string.backup_database_to_title)));
    }

    private void doCsvExport(CsvExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_export_inprogress), true);
        new CsvExportTask(this, progressDialog, options).execute();
    }

    private void doCsvImport(CsvImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_import_inprogress), true);
        new CsvImportTask(this, handler, progressDialog, options).execute();
    }

    private void doQifExport(QifExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.qif_export_inprogress), true);
        new QifExportTask(this, progressDialog, options).execute();
    }

    private void doQifImport(QifImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.qif_import_inprogress), true);
        new QifImportTask(this, handler, progressDialog, options).execute();
    }

    private void doCsvExport() {
        Intent intent = new Intent(this, CsvExportActivity.class);
        startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
    }

    private void doCsvImport() {
        Intent intent = new Intent(this, CsvImportActivity.class);
        startActivityForResult(intent, ACTIVITY_CSV_IMPORT);
    }

    private void doQifExport() {
        Intent intent = new Intent(this, QifExportActivity.class);
        startActivityForResult(intent, ACTIVITY_QIF_EXPORT);
    }

    private void doQifImport() {
        Intent intent = new Intent(this, QifImportActivity.class);
        startActivityForResult(intent, ACTIVITY_QIF_IMPORT);
    }

    private String selectedBackupFile;
    private com.google.api.services.drive.model.File selectedDriveFile;

    private void doImport() {
        final String[] backupFiles = Backup.listBackups(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_database)
                .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selectedBackupFile != null) {
                            ProgressDialog d = ProgressDialog.show(BackupListActivity.this, null, getString(R.string.restore_database_inprogress), true);
                            new BackupImportTask(BackupListActivity.this, d).execute(selectedBackupFile);
                        }
                    }
                })
                .setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (backupFiles != null && which >= 0 && which < backupFiles.length) {
                            selectedBackupFile = backupFiles[which];
                        }
                    }
                })
                .show();
    }

    private void doBackupOnGoogleDrive() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_gdocs_inprogress), true);
        new DriveBackupTask(this, d).execute();
    }

    private void doRestoreFromGoogleDrive() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.google_drive_loading_files), true);
        new DriveListFilesTask(this, d).execute();
    }

    public void doImportFromGoogleDrive(final com.google.api.services.drive.model.File[] backupFiles) {
        if (backupFiles != null) {
            String[] backupFilesNames = getBackupFilesTitles(backupFiles);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.restore_database)
                    .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (selectedDriveFile != null) {
                                ProgressDialog d = ProgressDialog.show(BackupListActivity.this, null, getString(R.string.restore_database_inprogress_gdocs), true);
                                new DriveRestoreTask(BackupListActivity.this, d, selectedDriveFile).execute();
                            }
                        }
                    })
                    .setSingleChoiceItems(backupFilesNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which >= 0 && which < backupFiles.length) {
                                selectedDriveFile = backupFiles[which];
                            }
                        }
                    })
                    .show();
        }
    }

    /**
     * Treat asynchronous requests to popup error messages
     */
    private Handler handler = new Handler() {
        /**
         * Schedule the popup of the given error message
         * @param msg The message to display
         **/
        @Override
        public void handleMessage(Message msg) {
            showErrorPopup(BackupListActivity.this, msg.what);
        }
    };

    public void showErrorPopup(Context context, int message) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setTitle(R.string.error)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(true)
                .create().show();
    }


    private String[] getBackupFilesTitles(com.google.api.services.drive.model.File[] backupFiles) {
        int count = backupFiles.length;
        String[] titles = new String[count];
        for (int i = 0; i < count; i++) {
            titles[i] = backupFiles[i].getTitle();
        }
        return titles;
    }

    private void doBackupOnDropbox() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_dropbox_inprogress), true);
        new DropboxBackupTask(this, d).execute();
    }

    private void doRestoreFromDropbox() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.dropbox_loading_files), true);
        new DropboxListFilesTask(this, d).execute();
    }

    private String selectedDropboxFile;

    public void doImportFromDropbox(final String[] backupFiles) {
        if (backupFiles != null) {
            new AlertDialog.Builder(BackupListActivity.this)
                    .setTitle(R.string.restore_database)
                    .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (selectedDropboxFile != null) {
                                ProgressDialog d = ProgressDialog.show(BackupListActivity.this, null, getString(R.string.restore_database_inprogress_dropbox), true);
                                new DropboxRestoreTask(BackupListActivity.this, d, selectedDropboxFile).execute();
                            }
                        }
                    })
                    .setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which >= 0 && which < backupFiles.length) {
                                selectedDropboxFile = backupFiles[which];
                            }
                        }
                    })
                    .show();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this.getApplicationContext());
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        PinProtection.unlock(this);
    }
    
    
}
