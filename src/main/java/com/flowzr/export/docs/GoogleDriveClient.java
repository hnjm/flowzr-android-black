package com.flowzr.export.docs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.DriveScopes;

import com.google.api.services.drive.model.FileList;
import com.flowzr.R;
import com.flowzr.export.ImportExportException;
import com.flowzr.utils.MyPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GoogleDriveClient {

    public static Drive create(Context context, String googleDriveAccount) throws IOException, GoogleAuthException, ImportExportException {
        if (googleDriveAccount == null) {
            throw new ImportExportException(R.string.google_drive_account_select_error);
        }
        try {
            List<String> scope = new ArrayList<String>();
            scope.add(DriveScopes.DRIVE_FILE);
            if (MyPreferences.isGoogleDriveFullReadonly(context)) {
                scope.add(DriveScopes.DRIVE_READONLY);
            }
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, scope);
            credential.setSelectedAccountName(googleDriveAccount);
            credential.getToken();
            return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
        } catch (UserRecoverableAuthException e) {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            Intent authorizationIntent = e.getIntent();
            authorizationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(
                    Intent.FLAG_FROM_BACKGROUND);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    authorizationIntent, 0);
            Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setTicker(context.getString(R.string.google_drive_permission_requested))
                    .setContentTitle(context.getString(R.string.google_drive_permission_requested))
                    .setContentText(context.getString(R.string.google_drive_permission_requested_for_account) +" " + googleDriveAccount)
                    .setContentIntent(pendingIntent).setAutoCancel(true).build();
            notificationManager.notify(0, notification);
            throw new ImportExportException(R.string.google_drive_permission_required);
        }
    }

    public static String getOrCreateDriveFolder(Drive drive, String targetFolder) throws IOException {
        String folderId = null;
        FileList folders = drive.files().list().setQ("mimeType='application/vnd.google-apps.folder'").execute();
        for (com.google.api.services.drive.model.File f : folders.getItems()) {
            if (f.getTitle().equals(targetFolder)) {
                folderId = f.getId();
            }
        }
        //if not found create it
        if (folderId == null) {
            com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
            body.setTitle(targetFolder);
            body.setMimeType("application/vnd.google-apps.folder");
            com.google.api.services.drive.model.File file = drive.files().insert(body).execute();
            folderId = file.getId();
        }
        return folderId;
    }

}