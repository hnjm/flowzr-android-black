/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.flowzr.export;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

import com.flowzr.R;
import com.flowzr.export.docs.ApiClientAsyncTask;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.flowzr.export.dropbox.Dropbox;
import com.flowzr.utils.MyPreferences;
//import com.google.api.client.http.InputStreamContent;

import java.io.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public abstract class Export {
	
	public static final File DEFAULT_EXPORT_PATH =  new File(Environment.getExternalStorageDirectory(), "financisto");
    public static final String BACKUP_MIME_TYPE = "application/x-gzip";

    private final Context context;
    private final boolean useGzip;

    private GoogleApiClient mGoogleApiClient;


    protected Export(Context context, boolean useGzip) {
        this.context = context;
        this.useGzip = useGzip;
    }

    public String export() throws Exception {
		File path = getBackupFolder(context);
        String fileName = generateFilename();
        File file = new File(path, fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            if (useGzip) {
                export(new GZIPOutputStream(outputStream));
            } else {
                export(outputStream);
            }
        } finally {
            outputStream.flush();
            outputStream.close();
        }
        return fileName;
	}

    protected void export(OutputStream outputStream) throws Exception {
        generateBackup(outputStream);
    }
	
	/**
	 * Backup database to google docs
	 * 
	 * @param drive Google docs connection
	 * @param targetFolder Google docs folder name
	 * */


	public String exportOnline(final String targetFolder,final GoogleApiClient pGoogleApiClient) throws Exception {
        this.mGoogleApiClient=pGoogleApiClient;
        if (!mGoogleApiClient.isConnected()) {
            Log.e("flowzr",context.getResources().getString(R.string.gdocs_connection_failed));
            return context.getResources().getString(R.string.gdocs_connection_failed);
        } else {
            Log.e("flowzr","connected");
        }

        DriveId folderId = ApiClientAsyncTask.getOrCreateDriveFolder(mGoogleApiClient, targetFolder);
        Log.i("flowzr", "Drive folder id is " + folderId.encodeToString());
        final DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, folderId);

        // generation backup file
        final String fileName = generateFilename();
        Log.i("flowzr", "creating new drive content for " + fileName);
        DriveContentsResult result = Drive.DriveApi.newDriveContents(mGoogleApiClient).await();
        if (!result.getStatus().isSuccess()) {
            Log.i("flowzr", "Failed to create new contents.");
            return "failed to create new content";
        }

        Log.i("flowzr", "Sending file");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream out = new BufferedOutputStream(new GZIPOutputStream(outputStream));
        generateBackup(out);

        //InputStreamContent mediaContent = new InputStreamContent(BACKUP_MIME_TYPE, new  BufferedInputStream(backup));
        // Get an output stream for the contents.
        OutputStream outputStream2 = result.getDriveContents().getOutputStream();
        // transforming streams
        //InputStream backup = new ByteArrayInputStream(outputStream.toByteArray());
        outputStream2.write(outputStream.toByteArray());
        outputStream2.close();

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(fileName)
                .setMimeType(BACKUP_MIME_TYPE)
                .build();

        DriveFolder.DriveFileResult rslt = folder.createFile(mGoogleApiClient,
                changeSet, result.getDriveContents()).await();

        return fileName;
    }


	private String generateFilename() {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");
		return df.format(new Date())+getExtension();
	}
	
	private void generateBackup(OutputStream outputStream) throws Exception {
		OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw, 65536);
		try {
			writeHeader(bw);
			writeBody(bw);
			writeFooter(bw);
		} finally {
			bw.close();
		}	
	}

	protected abstract void writeHeader(BufferedWriter bw) throws IOException, NameNotFoundException;

	protected abstract void writeBody(BufferedWriter bw) throws IOException;

	protected abstract void writeFooter(BufferedWriter bw) throws IOException;

	protected abstract String getExtension();
	
	public static File getBackupFolder(Context context) {
        String path = MyPreferences.getDatabaseBackupFolder(context);
        File file = new File(path);
        file.mkdirs();
        if (file.isDirectory() && file.canWrite()) {
            return file;
        }
        file = Export.DEFAULT_EXPORT_PATH;
        file.mkdirs();
        return file;
	}

    public static File getBackupFile(Context context, String backupFileName) {
        File path = getBackupFolder(context);
        return new File(path, backupFileName);
    }

    public static void uploadBackupFileToDropbox(Context context, String backupFileName) throws Exception {
        File file = getBackupFile(context, backupFileName);
        Dropbox dropbox = new Dropbox(context);
        dropbox.uploadFile(file);
    }


}
