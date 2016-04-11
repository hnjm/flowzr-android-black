/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko  - initial API and implementation
 *     Abdsandryk Souza - implementing 2D chart reports
 *     Emmanuel Florent - port to Android API 11+
 *                      - port to Google API V3
 ******************************************************************************/
package com.flowzr.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.adapter.BackupListAdapter;
import com.flowzr.backup.Backup;
import com.flowzr.backup.BackupType;
import com.flowzr.export.BackupExportTask;
import com.flowzr.export.BackupImportTask;
import com.flowzr.export.Export;
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
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.PinProtection;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.plus.Plus;

import java.io.File;

import static com.flowzr.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;

public class BackupListActivity extends AbstractActionBarActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {

	private static final int ACTIVITY_CSV_EXPORT = 102;
    private static final int ACTIVITY_QIF_EXPORT = 103;
    private static final int ACTIVITY_CSV_IMPORT = 104;
    private static final int ACTIVITY_QIF_IMPORT = 105;
    private static final int REQUEST_CODE_RESOLUTION = 106;

    private GoogleApiClient mClient;
    private static int drive_action = 0;
    private static final int DRIVE_ACTION_RESTORE = 1;
    private static final int DRIVE_ACTION_BACKUP = 2;

    private String selectedFile;

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

    protected void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_backup);
        initToolbar();

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
                        if (isConnectionUnAvailable()) {
                            showErrorPopup(BackupListActivity.this, R.string.flowzr_sync_error_no_network);
                            return;
                        }
                        drive_action = DRIVE_ACTION_BACKUP;
                        Toast.makeText(BackupListActivity.this, R.string.gdocs_backup, Toast.LENGTH_SHORT).show();
                        mClient.connect();
                        break;
                    case 3:
                        if (isConnectionUnAvailable()) {
                            showErrorPopup(BackupListActivity.this, R.string.flowzr_sync_error_no_network);
                            return;
                        }
                        drive_action = DRIVE_ACTION_RESTORE;
                        mClient.connect();
                        break;
                    case 4:
                        if (isConnectionUnAvailable()) {
                            showErrorPopup(BackupListActivity.this, R.string.flowzr_sync_error_no_network);
                            return;
                        }
                        doBackupOnDropbox();
                        break;
                    case 5:
                        if (isConnectionUnAvailable()) {
                            showErrorPopup(BackupListActivity.this, R.string.flowzr_sync_error_no_network);
                            return;
                        }
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
        if (requestCode == REQUEST_CODE_RESOLUTION) {
            if (mClient.isConnected()) {
                String accountName = Plus.AccountApi.getAccountName(mClient);
                if (accountName != null) {
                    MyPreferences.setGoogleDriveAccount(this, accountName);
                }
            } else {
                drive_action=0;
                mClient.connect();
            }
        }
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
                onBackPressed();
            }
        }
        return super.onOptionsItemSelected(item);
    }	
	
    

    private void doBackup() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        new BackupExportTask(this, d, true).execute();
    }
    /*
    private void doBackupTo() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        final BackupExportTask t = new BackupExportTask(this, d, false);
        t.setShowResultDialog(false);
        t.setListener(new ImportExportAsyncTaskListener() {
            Override
            public void onCompleted() {
                String backupFileName = t.backupFileName;
                startBackupToChooser(backupFileName);
            }
        });
        t.execute((String[]) null);
    }
    */

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

    public void doBackupOnGoogleDrive() {
        String folder = MyPreferences.getBackupFolder(this);
        String accountName=Plus.AccountApi.getAccountName(mClient);
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.gdocs_backup) + accountName + " " + folder, true);
        new DriveBackupTask(this,d).execute();
        mClient.disconnect();
    }

    public void doRestoreFromGoogleDrive() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.google_drive_loading_files), true);
        new DriveListFilesTask(this, d).execute();
    }

    public void doImportFromGoogleDrive(final MetadataBuffer bufferResult) {
        //convert to string[] for dialog
        final String[] backupFiles=new String[bufferResult.getCount()];
        for (int i=0;i<backupFiles.length;i++) {
            backupFiles[i]=bufferResult.get(i).getTitle();
        }
        new AlertDialog.Builder(BackupListActivity.this)
                .setTitle(R.string.restore_database)
                .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selectedFile != null) {

                            ProgressDialog d = ProgressDialog.show(BackupListActivity.this, null, getString(R.string.restore_database_inprogress_dropbox), true);
                            new DriveRestoreTask(BackupListActivity.this, d, selectedFile).execute();
                            bufferResult.release();
                        }
                    }
                })
                .setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which >= 0 && which < backupFiles.length) {
                            selectedFile = bufferResult.get(which).getDriveId().encodeToString();
                        }
                    }
                })
                .show();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
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


    private String[] getBackupFilesTitles(com.google.android.gms.drive.DriveFile[] backupFiles) {
        int count = backupFiles.length;
        String[] titles = new String[count];
        for (int i = 0; i < count; i++) {
            titles[i] = backupFiles[i].toString();
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


    public void doImportFromDropbox(final String[] backupFiles) {
        if (backupFiles != null) {
            new AlertDialog.Builder(BackupListActivity.this)
                    .setTitle(R.string.restore_database)
                    .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (selectedFile != null) {
                                ProgressDialog d = ProgressDialog.show(BackupListActivity.this, null, getString(R.string.restore_database_inprogress_dropbox), true);
                                new DropboxRestoreTask(BackupListActivity.this, d, selectedFile).execute();
                            }
                        }
                    })
                    .setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which >= 0 && which < backupFiles.length) {
                                selectedFile = backupFiles[which];
                            }
                        }
                    })
                    .show();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mClient!=null) {
            mClient.disconnect();
        }
        //PinProtection.lock(this.getApplicationContext());
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        if (mClient == null) {
            if (MyPreferences.getGoogleDriveAccount(this)==null) {
                mClient = new GoogleApiClient.Builder(this)
                        .addApi(Drive.API)
                        .addApi(Plus.API)
                        .addScope(Drive.SCOPE_FILE)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            } else {

                mClient = new GoogleApiClient.Builder(this)
                        .addApi(Drive.API)
                        .addApi(Plus.API)
                        .addScope(Drive.SCOPE_FILE)
                        .setAccountName(MyPreferences.getGoogleDriveAccount(this))
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build(); //
            }

        }
        PinProtection.unlock(this);
    }

    public boolean isConnectionUnAvailable() {
        ConnectivityManager cm= (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info=cm.getActiveNetworkInfo();
        if (info==null) return true;
        NetworkInfo.State network= info.getState();
        return network != NetworkInfo.State.CONNECTED;
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            String accountName = Plus.AccountApi.getAccountName(mClient);
            if (accountName != null) {
                MyPreferences.setGoogleDriveAccount(this, accountName);
            }
            if (drive_action == DRIVE_ACTION_RESTORE) {
                doRestoreFromGoogleDrive();
            }
            if (drive_action == DRIVE_ACTION_BACKUP) {
                Toast.makeText(BackupListActivity.this, getString(R.string.gdocs_backup) + " " + accountName, Toast.LENGTH_SHORT).show();
                doBackupOnGoogleDrive();
            }
            mClient.disconnect();
        } catch (Exception e) {
            showErrorPopup(this,R.string.gdocs_credentials_not_configured);
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, R.string.gdocs_connection_failed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect
        if (!result.hasResolution()) {
            //Log.e("flowzr","no resolution");
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        /**
        try {
            //result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e("flowzr", "Exception while starting resolution activity", e);
            e.printStackTrace();
        }
         **/
        //showErrorPopup(this, R.string.gdocs_connection_failed);
        Toast.makeText(this,R.string.gdocs_connection_failed,Toast.LENGTH_LONG).show();
    }


}
