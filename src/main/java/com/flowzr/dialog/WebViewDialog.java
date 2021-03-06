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
package com.flowzr.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.support.v7.app.AlertDialog;
import android.webkit.WebView;

import com.flowzr.R;
import com.flowzr.utils.Utils;

public class WebViewDialog {

	public static Boolean checkVersionAndShowWhatsNewIfNeeded(Activity activity) {
		try {
			PackageInfo info = Utils.getPackageInfo(activity);
			SharedPreferences preferences = activity.getPreferences(0); 
			int newVersionCode = info.versionCode;
			int oldVersionCode = preferences.getInt("versionCode", -1);
			if (newVersionCode > oldVersionCode) {
				preferences.edit().putInt("versionCode", newVersionCode).apply();
				showWhatsNew(activity);
				return true;
			}
			return false;
		} catch(Exception ex) { 
			return false;
		}
	}
	
	public static void showWhatsNew(Context context) {
		showHTMDialog(context, "whatsnew.htm", R.string.whats_new);
	}

	private static void showHTMDialog(Context context, String fileName, int dialogTitleResId) {
		WebView webView = new WebView(context);
		webView.loadUrl("file:///android_asset/"+fileName);
		new AlertDialog.Builder(context)
			.setView(webView)
			.setTitle(dialogTitleResId)
			.setPositiveButton(R.string.ok, null)
			.show();		
	}

}
