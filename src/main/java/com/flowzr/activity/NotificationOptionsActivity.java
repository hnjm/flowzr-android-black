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

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.recur.NotificationOptions;
import com.flowzr.utils.EnumUtils;
import com.flowzr.view.NodeInflater;

import java.io.File;
import java.util.List;

import static com.flowzr.utils.ThumbnailUtil.PICTURES_DIR;

public class NotificationOptionsActivity extends AbstractEditorActivity implements ActivityLayoutListener {

	public static final String NOTIFICATION_OPTIONS = "options";
	private static final int PICKUP_RINGTONE = 1;

	private static final NotificationOptions.LedColor[] colors = NotificationOptions.LedColor.values();
	private static final NotificationOptions.VibrationPattern[] patterns = NotificationOptions.VibrationPattern.values();

	private LinearLayout layout;

	private TextView soundText;
	private TextView ledText;
	private TextView vibraText;
	private NotificationOptions options = NotificationOptions.createDefault();

	@Override
	public String getMyTag() {
		return null;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.recurrence;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		NodeInflater nodeInflater = new NodeInflater(inflater);
		x = new ActivityLayout(nodeInflater, this);
		return inflater.inflate(getLayoutId(), container, false);
	}

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        db = new DatabaseAdapter(activity);
        db.open();
        em = db.em();

		layout = (LinearLayout)getView().findViewById(R.id.layout);

		Bundle bundle = getArguments();
		if (bundle != null) {
			String options = bundle.getString(NOTIFICATION_OPTIONS);
			if (options != null) {
				try {
					this.options = NotificationOptions.parse(options);
				} catch (Exception e) {
					this.options = NotificationOptions.createDefault();
				}
			}
		}
        createNodes();
		updateOptions();
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_done:
				Bundle bundle = new Bundle();
				bundle.putString(NOTIFICATION_OPTIONS, options.stateToString());
				finishAndClose(bundle);
				return true;
			case R.id.action_cancel:
				finishAndClose(AppCompatActivity.RESULT_CANCELED);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void createNodes() {
		layout.removeAllViews();
		soundText = x.addListNode(layout, R.id.notification_sound, R.string.notification_sound, options.getSoundName(activity));
		vibraText = x.addListNode(layout, R.id.notification_vibra, R.string.notification_vibra, options.vibration.titleId);
		ledText = x.addListNode(layout, R.id.notification_led, R.string.notification_led, options.ledColor.titleId);
		x.addInfoNodeSingle(layout, R.id.result1, R.string.notification_options_default);
		x.addInfoNodeSingle(layout, R.id.result2, R.string.notification_options_off);
	}


	protected void onClick(View v, int id) {
		switch (id) {
			case R.id.notification_sound: {
				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				if (options.sound != null) {
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(options.sound));
				}
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                activity.onFragmentMessage(MyFragmentAPI.REQUEST_ACTIVITY,PICKUP_RINGTONE,intent,this);
			} break;
			case R.id.notification_vibra: {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(getContext(), patterns);
				x.selectPosition(getContext(), R.id.notification_vibra, R.string.notification_vibra, adapter, options.vibration.ordinal());
			} break;
			case R.id.notification_led:  {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(getContext(), colors);
				x.selectPosition(getContext(), R.id.notification_led, R.string.notification_led, adapter, options.ledColor.ordinal());
			} break;
			case R.id.result1: {
				options = NotificationOptions.createDefault();
				updateOptions();
			} break;
			case R.id.result2: {
				options = NotificationOptions.createOff();
				updateOptions();
			} break;
		}
	}

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICKUP_RINGTONE && resultCode == AppCompatActivity.RESULT_OK) {
			Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			options.sound = ringtoneUri != null ? ringtoneUri.toString() : null;
			updateOptions();
		}
	}

    @Override
    public void onSelectedId(int id, long selectedId) {
    }

    @Override
    public void onSelected(int id, List<? extends MultiChoiceItem> items) {

    }


	@Override
	public void onSelectedPos(int id, int selectedPos) {
		switch (id) {
			case R.id.notification_sound:
				updateOptions();
				break;
			case R.id.notification_vibra:
				options.vibration = patterns[selectedPos];
				updateOptions();
				break;
			case R.id.notification_led:
				options.ledColor = colors[selectedPos];
				updateOptions();
				break;
		}
	}

	private void updateOptions() {
		soundText.setText(options.getSoundName(getContext()));
		vibraText.setText(options.vibration.titleId);
		ledText.setText(options.ledColor.titleId);
	}

	protected DatabaseAdapter db;
	protected MyEntityManager em;

	protected ActivityLayout x;



	protected boolean shouldLock() {
		return true;
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		onClick(v, id);
	}








    protected boolean checkSelected(Object value, int messageResId) {
		if (value == null) {
			Toast.makeText(getContext(), messageResId, Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	protected boolean checkSelectedId(long value, int messageResId) {
		if (value <= 0) {
			Toast.makeText(getContext(), messageResId, Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	public static void setVisibility(View v, int visibility) {
		v.setVisibility(visibility);
		Object o = v.getTag();
		if (o instanceof View) {
			((View)o).setVisibility(visibility);
		}
	}

	@Override
    public void onDestroy() {
		db.close();
		super.onDestroy();
	}


}
