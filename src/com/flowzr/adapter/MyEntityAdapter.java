package com.flowzr.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.flowzr.model.MyEntity;

import java.util.List;

public class MyEntityAdapter<T extends MyEntity> extends ArrayAdapter<T> {

	public MyEntityAdapter(Context context, int resource,
			int textViewResourceId, List<T> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public MyEntityAdapter(Context context, int resource,
			int textViewResourceId, T[] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public MyEntityAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public MyEntityAdapter(Context context, int textViewResourceId,
			List<T> objects) {
		super(context, textViewResourceId, objects);
	}

	public MyEntityAdapter(Context context, int textViewResourceId, T[] objects) {
		super(context, textViewResourceId, objects);
	}

	public MyEntityAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).id;
	}

}
