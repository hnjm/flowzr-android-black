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

package com.flowzr.export.flowzr;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.flowzr.R;
import com.flowzr.export.ImportExportException;
import com.flowzr.export.docs.ApiClientAsyncTask;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import com.flowzr.utils.MyPreferences;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.api.client.http.InputStreamContent;

public class PictureDriveTask extends ApiClientAsyncTask<String, String, Object> {

    private DriveId prefFolderID;
	private Uri fileUri;
	private long trDate;
	private String remote_key;
	private Context context;
	private DefaultHttpClient http_client;
	private DatabaseAdapter dba;

	public PictureDriveTask(Context context, DefaultHttpClient http_client,Uri _fileUri,long l,String _remote_key) {
        super(context,null);

        this.http_client = http_client; // (FlowzrSyncEngine will store link)
        this.context = context;
        this.fileUri = _fileUri;
        this.trDate = l;
        this.remote_key = _remote_key;
        dba = new DatabaseAdapter(context);
        dba.open();

    }

    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {

        String folder = MyPreferences.getGoogleDriveFolder(context);
        // check the backup folder registered on preferences
        if (folder == null || folder.equals("")) {
            throw new ImportExportException(R.string.gdocs_folder_not_configured);
        }

        return true;
    }



    protected boolean runUpload() throws Exception {
        DriveId targetFolderId = null;
        // File's binary content
        final java.io.File fileContent = new java.io.File(fileUri.getPath());

        prefFolderID =getOrCreateDriveFolder(getGoogleApiClient(),null, MyPreferences.getGoogleDriveFolder(context));
        //search for the target folder (depending of the date)
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(trDate));
        int month = cal.get(Calendar.MONTH) + 1;
        String targetFolder = String.valueOf(cal.get(Calendar.YEAR)) + "-" + (month < 10 ? ("0" + month) : (month));
        targetFolderId=getOrCreateDriveFolder(getGoogleApiClient(),prefFolderID,targetFolder);
        final DriveFolder pFldr=Drive.DriveApi.getFolder(getGoogleApiClient(), targetFolderId);
        if (pFldr == null) throw new Exception("no target folder " + MyPreferences.getGoogleDriveFolder(context) +"/"+ targetFolder);
        //----------------->>

        if (getFileForName(pFldr,fileContent.getName())!=null) {
            Log.e("flowzr","already got " + fileContent.getName());

            throw new Exception("file already existe " + MyPreferences.getGoogleDriveFolder(context) +"/"+ targetFolder);
            //----------------->>
        }

        DriveContentsResult r1 = Drive.DriveApi.newDriveContents(getGoogleApiClient()).await();
        if (r1 == null || !r1.getStatus().isSuccess()) throw new Exception("no drive content result");
        MetadataChangeSet meta = new MetadataChangeSet.Builder().setTitle(fileContent.getName()).setMimeType("image/jpeg").build();
        DriveFolder.DriveFileResult r2 = pFldr.createFile(getGoogleApiClient(), meta, r1.getDriveContents()).await();
        DriveFile dFil = r2 != null && r2.getStatus().isSuccess() ? r2.getDriveFile() : null;
        if (dFil == null)  throw new Exception(" unable to create file " );
        //----------------->>
        DriveContentsResult driveContentsResult = dFil.open(mClient,DriveFile.MODE_WRITE_ONLY,null).await();
        if (!driveContentsResult.getStatus().isSuccess()) {
            return false;
        }
        DriveContents driveContents = driveContentsResult.getDriveContents();
        OutputStream outputStream = driveContents.getOutputStream();
        FileInputStream fis;
        fis = new FileInputStream(fileContent.getPath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while (-1 != (n = fis.read(buf)))
            baos.write(buf, 0, n);
        byte[] photoBytes = baos.toByteArray();
        outputStream.write(photoBytes);
        com.google.android.gms.common.api.Status status =
                driveContents.commit(getGoogleApiClient(), null).await();
        Thread.sleep(10000); // wait for google to flush ...
        DriveResource.MetadataResult s = dFil.getMetadata(mClient).await();
        Log.e("flowzr", "alt url is :" + s.getMetadata().getAlternateLink());
        String file_url= s.getMetadata().getAlternateLink();
        String uploadedId= s.getMetadata().getDriveId().encodeToString();
        String sql = "update transactions set blob_key='" + file_url + "' where remote_key='" + remote_key + "'";
        dba.db().execSQL(sql);
        sql = "select from_account_id,attached_picture from " + DatabaseHelper.TRANSACTION_TABLE + " where remote_key='" + remote_key + "'";
        Cursor c = dba.db().rawQuery(sql, null);
        if (c.moveToFirst()) {
            String account_key = FlowzrSyncEngine.getRemoteKey(DatabaseHelper.ACCOUNT_TABLE, String.valueOf(c.getLong(0)));
            String file_type = "image/jpeg";
            String file_name = c.getString(1);
            if (file_url == null) {
                file_url = "";
            }

           try {
               String url = FlowzrSyncEngine.FLOWZR_API_URL + "/clear/blob/?url=" + URLEncoder.encode(file_url, "UTF-8")  + "&account=" + account_key + "&crebit=" + remote_key + "&name=" + file_name + "&blob_key=" + uploadedId + "type=" + file_type;
               HttpGet httpGet = new HttpGet(url);
                http_client.execute(httpGet);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }

    /**
     * Override this method to perform a computation on a background thread, while the client is
     * connected.
     */
    protected Object doInBackgroundConnected(String... params) {
        try {
            return runUpload();
        } catch (Exception e) {
            return e;
        }
    }

}