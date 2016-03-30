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
package com.flowzr.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.flowzr.service.FinancistoService;

public class PackageReplaceReceiver extends BroadcastReceiver {

	private static final String PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
        String dataString = intent.getDataString();
		if (PACKAGE_REPLACED.equals(action) && context !=null) {
            Log.d("PackageReplaceReceiver", "Received " + dataString);
            if ("package:com.flowzr".equals(dataString)) {
                Log.d("PackageReplaceReceiver", "Re-scheduling all transactions");
                requestScheduleAll(context);
                Log.d("PackageReplaceReceiver", "Re-scheduling auto Backup");
                requestScheduleAutoBackup(context);
                Log.d("PackageReplaceReceiver", "Re-scheduling next sync");
                requestScheduleAutoSync(context);
                Log.d("PackageReplaceReceiver", "Re-scheduling done");
            }
		}
	}

    protected void requestScheduleAll(Context context) {
        try {
            Intent serviceIntent = MainActivity.createExplicitFromImplicitIntent(context,new Intent(FinancistoService.ACTION_SCHEDULE_ALL));
            WakefulIntentService.sendWakefulWork(context, serviceIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void requestScheduleAutoBackup(Context context) {
        try {
            Intent serviceIntent = MainActivity.createExplicitFromImplicitIntent(context, new Intent(FinancistoService.ACTION_SCHEDULE_AUTO_BACKUP));
            WakefulIntentService.sendWakefulWork(context, serviceIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void requestScheduleAutoSync(Context context) {
        try {
            Intent serviceIntent =MainActivity.createExplicitFromImplicitIntent(context, new Intent(FinancistoService.ACTION_SCHEDULE_AUTO_SYNC));
            WakefulIntentService.sendWakefulWork(context, serviceIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }  
}
