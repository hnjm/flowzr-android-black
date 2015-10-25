package com.flowzr.export.docs;

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

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.ImportExportAsyncTask;
import com.flowzr.export.ImportExportAsyncTaskListener;
import com.flowzr.export.ImportExportException;
import com.flowzr.utils.MyPreferences;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import android.support.v7.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;


import java.util.concurrent.CountDownLatch;

/**
 * An AsyncTask that maintains a connected client.
 */
public abstract class ApiClientAsyncTask<Params, Progress, Result>
        extends ImportExportAsyncTask {

    protected GoogleApiClient mClient;
    protected final Context context;
    protected final ProgressDialog dialog;
    private ImportExportAsyncTaskListener listener;

    public ApiClientAsyncTask(Context context, ProgressDialog dialog) {
        super(context, dialog);
        this.dialog = dialog;
        this.context = context;
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .setAccountName(MyPreferences.getGoogleDriveAccount(context))
                .addScope(Drive.SCOPE_FILE);
        mClient = builder.build();
    }

    public Metadata getFileForName(DriveFolder driveFolder, String filename) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, filename))
                .addFilter(Filters.eq(SearchableField.TRASHED, false))
                .build();
        DriveApi.MetadataBufferResult r = driveFolder.queryChildren(mClient, query).await();
        if (r.getMetadataBuffer().getCount()==0) {
            r.release();
            return null;
        } else {
            Metadata r2 = r.getMetadataBuffer().get(0);
            r.release();
            return r2;
        }
    }

    @Override
    protected final Object doInBackground(String... params) {
        DatabaseAdapter db = new DatabaseAdapter(context);
        db.open();
        final CountDownLatch latch = new CountDownLatch(1);
        mClient.registerConnectionCallbacks(new ConnectionCallbacks() {
            @Override
            public void onConnectionSuspended(int cause) {
                Log.i("flowzr", "connection suspended");
            }

            @Override
            public void onConnected(Bundle arg0) {
                latch.countDown();

            }
        });
        mClient.registerConnectionFailedListener(new OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult arg0) {
                latch.countDown();
            }
        });
        mClient.connect();
        try {
            latch.await();
        } catch (InterruptedException e) {
            return new ImportExportException(R.string.gdocs_backup_failed);
        }
        if (!mClient.isConnected()) {
            return new ImportExportException(R.string.gdocs_credentials_not_configured);
        }
        try {
            return doInBackgroundConnected(params);
        } finally {
            db.close();
            mClient.disconnect();
        }
    }

    /**
     * Override this method to perform a computation on a background thread, while the client is
     * connected.
     */
    protected abstract Result doInBackgroundConnected(String... params) ;

    /**
     * Gets the GoogleApliClient owned by this async task.
     */
    protected GoogleApiClient getGoogleApiClient() {
        return mClient;
    }

    protected abstract String getSuccessMessage(Object result);


    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        if (dialog!=null) {
            dialog.setMessage(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Object result) {
        if (dialog!=null) {
            dialog.dismiss();
        }

        if (result instanceof ImportExportException) {
            ImportExportException exception = (ImportExportException) result;
            StringBuilder sb = new StringBuilder();
            sb.append(context.getString(exception.errorResId));
            if (exception.cause != null) {
                sb.append(" : ").append(exception.cause);
            }
            //if (sb.toString().equals(context.getString(R.string.gdocs_credentials_not_configured))) {
            //    Toast.makeText(context,R.string.gdocs_credentials_not_configured,Toast.LENGTH_LONG).show();
            //    Intent intent = new Intent(context, BackupPreferencesActivity.class);
            //    intent.putExtra(BackupPreferencesActivity.DRIVE_CONFIG,BackupPreferencesActivity.DRIVE_CONFIG);
            //    context.startActivity(intent);
            //} else {
            if (dialog!=null) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.fail)
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
            return;
        }
        if (result instanceof Exception)
            return;
        String message = getSuccessMessage(result);
        if (listener != null) {
            listener.onCompleted();
        }
    }

    /**
     * search on a particualr folder or root
     */
    public static DriveId getOrCreateDriveFolder(GoogleApiClient mGoogleApiClient,DriveId baseFolderId, String targetFolder) throws ImportExportException {
        DriveFolder baseFolder=null;
        if (baseFolderId!=null) {
            baseFolder=Drive.DriveApi.getFolder(mGoogleApiClient, baseFolderId);
            DriveResource.MetadataResult r = baseFolder.getMetadata(mGoogleApiClient).await();
        }
        if (baseFolder==null) {
            baseFolder=Drive.DriveApi.getRootFolder(mGoogleApiClient);
        }

        DriveId folder = null;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Query query = new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.TITLE, targetFolder))
                    .addFilter(Filters.eq(SearchableField.TRASHED, false))
                    .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                    .build();
            // fire the query
            DriveApi.MetadataBufferResult rslt =baseFolder.queryChildren(mGoogleApiClient,query).await();
            //DriveApi.MetadataBufferResult rslt = Drive.DriveApi.query(mGoogleApiClient, query).await();
            if (rslt.getStatus().isSuccess()) {
                MetadataBuffer mdb = null;
                try {
                    mdb = rslt.getMetadataBuffer();

                    for (Metadata md : mdb) {
                        if (md == null || !md.isDataValid() || md.isTrashed()) continue;
                        //   md.getTitle(), md.getDriveId(), ....
                        if (md.getTitle().equals(targetFolder)) {
                            Log.e("flowzr","got it: " + md.getTitle());
                            folder = md.getDriveId();
                        }
                    }
                } finally {
                    if (mdb != null) mdb.close();
                }
            }
            //if not found create it
            if (folder == null) {
                Log.e("flowzr","creating " + targetFolder);
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(targetFolder).build();
                DriveFolder.DriveFolderResult rslt2 = baseFolder.createFolder(mGoogleApiClient, changeSet).await();
                folder = rslt2.getDriveFolder().getDriveId();
            }
            return folder;
        } else {
            throw new ImportExportException(R.string.gdocs_connection_failed);
        }
    }

    /**
     *  Search on all folders
     */
    public static DriveId getOrCreateDriveFolder( GoogleApiClient mGoogleApiClient, String targetFolder)  throws ImportExportException {

        DriveId folder = null;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Query query = new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.TITLE, targetFolder))
                    .addFilter(Filters.eq(SearchableField.TRASHED, false))
                    .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                    .build();
            // fire the query
            DriveApi.MetadataBufferResult rslt = Drive.DriveApi.query(mGoogleApiClient, query).await();
            if (rslt.getStatus().isSuccess()) {
                MetadataBuffer mdb = null;
                try {
                    mdb = rslt.getMetadataBuffer();

                    for (Metadata md : mdb) {
                        if (md == null || !md.isDataValid() || md.isTrashed()) continue;
                        //   md.getTitle(), md.getDriveId(), ....
                        if (md.getTitle().equals(targetFolder)) {
                            folder = md.getDriveId();
                        }
                    }
                } finally {
                    if (mdb != null) mdb.close();
                }
            }
            //if not found create it
            if (folder == null) {

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(targetFolder).build();
                DriveFolder.DriveFolderResult rslt2 = Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                        mGoogleApiClient, changeSet).await();
                folder = rslt2.getDriveFolder().getDriveId();
            }
            return folder;
        } else {
            throw new ImportExportException(R.string.gdocs_connection_failed);
        }
    }

}
