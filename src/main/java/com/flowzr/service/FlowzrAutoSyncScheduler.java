/*
 * Copyright (c) 2013 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.flowzr.utils.MyPreferences;

import java.util.Date;
  
public class FlowzrAutoSyncScheduler {

    private final long now;

    public static void scheduleNextAutoSync(Context context) {
        if (MyPreferences.isAutoSync(context)) {
            new FlowzrAutoSyncScheduler(System.currentTimeMillis() + (10*60*1000)).scheduleSync(context);
        } else {
            Log.e("flowzr","auto sync disabled in prefs");
        }
    }
    
    public FlowzrAutoSyncScheduler(long now) {
        this.now = now;
    }

    public void scheduleSync(Context context) {
        AlarmManager service = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context);
        service.set(AlarmManager.RTC_WAKEUP, now, pendingIntent);
        Log.i("Flowzr", "Next flowzr-sync scheduled at "+ new Date(now).toString());
    }

    private PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent("com.flowzr.SCHEDULED_SYNC");
        return PendingIntent.getBroadcast(context, -100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public long getScheduledTime() {
        return now;
    }
}
