/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko  - initial API and implementation
 *     Emmanuel Florent - port to Android API 11+
 *                      - port to Google API V3
 ******************************************************************************/
package com.flowzr.backup;

import android.content.ContentValues;
import android.content.Context;

import com.flowzr.db.Database;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseSchemaEvolution;
import com.flowzr.export.Export;
import com.flowzr.export.dropbox.Dropbox;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

import static com.flowzr.backup.Backup.RESTORE_SCRIPTS;

public class DatabaseImport extends FullDatabaseImport {

	private final DatabaseSchemaEvolution schemaEvolution;
    private final InputStream backupStream;

    public static DatabaseImport createFromFileBackup(Context context, DatabaseAdapter dbAdapter, String backupFile) throws FileNotFoundException {
        File backupPath = Export.getBackupFolder(context);
        File file = new File(backupPath, backupFile);
        FileInputStream inputStream = new FileInputStream(file);
        return new DatabaseImport(context, dbAdapter, inputStream);
    }

    public static DatabaseImport createFromGoogleDriveBackup(Context context, DatabaseAdapter dbAdapter, GoogleApiClient mGoogleApiClient, DriveFile file)
            throws IOException {
        DriveContentsResult h= file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
        InputStream inputStream = h.getDriveContents().getInputStream();
        InputStream in = new GZIPInputStream(inputStream);
        return new DatabaseImport(context, dbAdapter, in);
    }

    public static DatabaseImport createFromDropboxBackup(Context context, DatabaseAdapter dbAdapter, Dropbox dropbox, String backupFile)
            throws Exception {
        InputStream inputStream = dropbox.getFileAsStream(backupFile);
        InputStream in = new GZIPInputStream(inputStream);
        return new DatabaseImport(context, dbAdapter, in);
    }

    private DatabaseImport(Context context, DatabaseAdapter dbAdapter, InputStream backupStream) {
        super(context, dbAdapter);
        this.schemaEvolution = new DatabaseSchemaEvolution(context, Database.DATABASE_NAME, null, Database.DATABASE_VERSION);
        this.backupStream = backupStream;
	}

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    @Override
    protected void restoreDatabase() throws IOException {
        InputStream s = decompressStream(backupStream);
        InputStreamReader isr = new InputStreamReader(s, "UTF-8");
        BufferedReader br = new BufferedReader(isr, 65535);
        try {
            recoverDatabase(br);
            runRestoreAlterscripts();
        } finally {
            br.close();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private InputStream decompressStream(InputStream input) throws IOException {
        PushbackInputStream pb = new PushbackInputStream(input, 2);
        byte[] bytes = new byte[2];
        //noinspection ResultOfMethodCallIgnored
        pb.read(bytes);
        pb.unread(bytes);
        int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
        if (GZIPInputStream.GZIP_MAGIC == head)
            return new GZIPInputStream(pb);
        else
            return pb;
    }

    private void recoverDatabase(BufferedReader br) throws IOException {
        boolean insideEntity = false;
        ContentValues values = new ContentValues();
        String line;
        String tableName = null;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("$")) {
                if ("$$".equals(line)) {
                    if (tableName != null && values.size() > 0) {
                        db.insert(tableName, null, values);
                        tableName = null;
                        insideEntity = false;
                    }
                } else {
                    int i = line.indexOf(":");
                    if (i > 0) {
                        tableName = line.substring(i+1);
                        insideEntity = true;
                        values.clear();
                    }
                }
            } else {
                if (insideEntity) {
                    int i = line.indexOf(":");
                    if (i > 0) {
                        String columnName = line.substring(0, i);
                        String value = line.substring(i+1);
                        values.put(columnName, value);
                    }
                }
            }
        }
	}

	private void runRestoreAlterscripts() throws IOException {
		for (String script : RESTORE_SCRIPTS) {
			schemaEvolution.runAlterScript(db, script);
		}
	}

}
