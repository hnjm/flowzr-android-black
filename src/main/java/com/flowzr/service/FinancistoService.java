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
package com.flowzr.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.flowzr.R;
import com.flowzr.activity.AbstractTransactionActivity;
import com.flowzr.activity.AccountWidget;
import com.flowzr.activity.MainActivity;
import com.flowzr.activity.MyFragmentAPI;
import com.flowzr.backup.DatabaseExport;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.export.Export;
import com.flowzr.export.docs.DriveBackupTask;
import com.flowzr.export.flowzr.FlowzrSyncEngine;
import com.flowzr.export.flowzr.FlowzrSyncTask;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.TransactionInfo;
import com.flowzr.model.TransactionStatus;
import com.flowzr.recur.NotificationOptions;
import com.flowzr.utils.MyPreferences;

import java.util.Date;

import static com.flowzr.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static com.flowzr.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;

public class FinancistoService extends WakefulIntentService {

	private static final String TAG = "FinancistoService";
    public static final String ACTION_SCHEDULE_ALL = "com.flowzr.SCHEDULE_ALL";
    public static final String ACTION_SCHEDULE_ONE = "com.flowzr.SCHEDULE_ONE";
    public static final String ACTION_SCHEDULE_AUTO_BACKUP = "com.flowzr.ACTION_SCHEDULE_AUTO_BACKUP";
    public static final String ACTION_AUTO_BACKUP = "com.flowzr.ACTION_AUTO_BACKUP";
    public static final String ACTION_SCHEDULE_AUTO_SYNC = "com.flowzr.ACTION_SCHEDULE_AUTO_SYNC";
    public static final String ACTION_AUTO_SYNC = "com.flowzr.ACTION_AUTO_SYNC";
    
	private static final int RESTORED_NOTIFICATION_ID = 0;

	private DatabaseAdapter db;
    private RecurrenceScheduler scheduler;

    public FinancistoService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        db = new DatabaseAdapter(getApplicationContext());
        db.open();
        scheduler = new RecurrenceScheduler(db);
        Log.i(TAG, "Created Finacisto service ...");
    }

    @Override
    public void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    @Override
	protected void doWakefulWork(Intent intent) {
        String action = intent.getAction();
        try {
            if (ACTION_SCHEDULE_ALL.equals(action)) {
                scheduleAll();
            } else if (ACTION_SCHEDULE_ONE.equals(action)) {
                scheduleOne(intent);
            } else if (ACTION_SCHEDULE_AUTO_BACKUP.equals(action)) {
                scheduleNextAutoBackup(this);
            } else if (ACTION_AUTO_BACKUP.equals(action)) {
                doAutoBackup();
            } else if (ACTION_SCHEDULE_AUTO_SYNC.equals(action)) {
                scheduleNextAutoSync(this);
            } else if (ACTION_AUTO_SYNC.equals(action)) {
                doAutoSync();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scheduleAll() {
        int restoredTransactionsCount = scheduler.scheduleAll(this);
        if (restoredTransactionsCount > 0) {
            notifyUser(createRestoredNotification(restoredTransactionsCount), RESTORED_NOTIFICATION_ID);
        }
    }

    private void scheduleOne(Intent intent) {
        long scheduledTransactionId = intent.getLongExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, -1);
        if (scheduledTransactionId > 0) {
            TransactionInfo transaction = scheduler.scheduleOne(this, scheduledTransactionId);
            if (transaction != null) {
                notifyUser(transaction);
                AccountWidget.updateWidgets(this);
            }
        }
    }
    
    private void doAutoSync() {
    	try {
    		Log.i(TAG, "Auto-sync started at " + new Date());    		
    		if (MyPreferences.getFlowzrLastSync(getApplicationContext())>0) {
				if (isPushSyncNeed(MyPreferences.getFlowzrLastSync(getApplicationContext()))) {
	        		if (FlowzrSyncEngine.isRunning) {
		        		Log.i(TAG,"sync already in progess");
	        			return;
	        		}
					new FlowzrSyncTask(getApplicationContext()).execute();
	    		} else {
					Log.i(TAG,"no changes to push since " + new Date(MyPreferences.getFlowzrLastSync(getApplicationContext())).toString());
				}
    		} else {
        		Log.i(TAG,"skip auto sync as never sync before ...");    			
    		}
    	} finally {
    		scheduleNextAutoSync(this);
    	}
    }
    
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private boolean isPushSyncNeed(long lastSyncLocalTimestamp) {
        String sql = "select count(*) from transactions where updated_on > " + lastSyncLocalTimestamp;
        Cursor c = db.db().rawQuery(sql, null);
        try {
            c.moveToFirst();
            long total = c.getLong(0);
            return total != 0;
        } finally {
            c.close();
        }
    }
    
    private void doAutoBackup() {
        try {
            try {
                long t0 = System.currentTimeMillis();
                Log.e(TAG, "Auto-backup started at " + new Date());
                DatabaseExport export = new DatabaseExport(this, db.db(), true);
                String fileName = export.export();
                if (MyPreferences.isDropboxUploadAutoBackups(this)) {
                    Export.uploadBackupFileToDropbox(this, fileName);
                }
                if (MyPreferences.isDriveUploadAutoBackups(this)) {

                    //new DriveBackupTask(this,fileName).execute();
                }
                Log.e(TAG, "Auto-backup completed in " +(System.currentTimeMillis()-t0)+"ms");
            } catch (Exception e) {
                Log.e(TAG, "Auto-backup unsuccessful", e);
            }
        } finally {
            scheduleNextAutoBackup(this);
        }
    }

    private void notifyUser(TransactionInfo transaction) {
		Notification notification = createNotification(transaction);
		notifyUser(notification, (int)transaction.id);
	}

	private void notifyUser(Notification notification, int id) {
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(id, notification);		
	}
    //@TODO https://github.com/dsolonenko/financisto/commit/b2067a11d52ec26413e0dbc07b48b165e6138ed5
	private Notification createRestoredNotification(int count) {
		//long when = System.currentTimeMillis();
		String text = getString(R.string.scheduled_transactions_have_been_restored, count);
		Notification notification = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_alarm)
        .setContentText(text).build();
        //.set, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults = Notification.DEFAULT_ALL;
		Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra(MyFragmentAPI.REQUEST_BLOTTER, true);
		WhereFilter filter = new WhereFilter("");
		filter.eq(BlotterFilter.STATUS, TransactionStatus.RS.name());
		filter.toIntent(notificationIntent);

		//PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification  = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.scheduled_transactions_restored))
                .setContentText(getString(R.string.scheduled_transactions_restored))
                .setSmallIcon(R.drawable.ic_sync)
                .build(); // available from API level 11 and onwards


        return notification;
	}

	private Notification createNotification(TransactionInfo t) {
		//long when = System.currentTimeMillis();
        Context context = getApplicationContext();
		Notification notification = new NotificationCompat.Builder(context)
        .setSmallIcon(t.getNotificationIcon())
        .setContentText(t.getNotificationTickerText(this))
        .build();
        //.setW, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		applyNotificationOptions(notification, t.notificationOptions);

		Intent notificationIntent = new Intent(this, t.getActivity());
		notificationIntent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, t.id);
		//PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        notification = new NotificationCompat.Builder(context)
                .setContentTitle(t.getNotificationContentTitle(this))
                .setContentText(t.getNotificationContentText(this))
                .setSmallIcon(R.drawable.ic_sync)
                .build();

        return notification;
	}

	private void applyNotificationOptions(Notification notification, String notificationOptions) {
		if (notificationOptions == null) {
			notification.defaults = Notification.DEFAULT_ALL;
		} else {
			NotificationOptions options = NotificationOptions.parse(notificationOptions);
			options.apply(notification);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
