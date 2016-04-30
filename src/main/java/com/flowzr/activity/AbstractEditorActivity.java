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
 ******************************************************************************/
package com.flowzr.activity;

import android.support.v4.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.utils.PinProtection;
import com.flowzr.utils.Utils;
import com.flowzr.view.NodeInflater;

import java.util.List;
import java.util.MissingFormatArgumentException;

public abstract class AbstractEditorActivity extends ListFragment implements ActivityLayoutListener {

	protected DatabaseAdapter db;
	protected MyEntityManager em;
    protected int contentId;
	protected ActivityLayout x;
    protected Utils u;
    protected MainActivity activity;

    protected abstract int getLayoutId();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ok, menu);
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        NodeInflater nodeInflater = new NodeInflater(inflater);
        x = new ActivityLayout(nodeInflater, this);
        final Bundle args = getArguments();
        contentId = args != null ? args.getInt("EXTRA_LAYOUT", this.getLayoutId()) : this.getLayoutId();
        return inflater.inflate(getLayoutId(), container, false);
    }


    public boolean finishAndClose(int result) {
        Bundle bundle = new  Bundle();
        bundle.putInt(MyFragmentAPI.RESULT_EXTRA,result);
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
        return true;
    }

    public boolean finishAndClose(Bundle bundle) {
        bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,getArguments().getInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA));
        bundle.putInt(MyFragmentAPI.RESULT_EXTRA, AppCompatActivity.RESULT_OK);
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
        return true;
    }

    public void onAttach(Context a) {
        super.onAttach(a);
        setHasOptionsMenu(true);
        activity=(MainActivity)a;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = new DatabaseAdapter(getContext());
		db.open();
		em = db.em();
	}

    protected boolean shouldLock() {
        return true;
    }

	@Override
	public void onClick(View v) {
		int id = v.getId();
		onClick(v, id);
	}

	protected abstract void onClick(View v, int id);


	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
	}

    protected boolean checkSelected(Object value, @SuppressWarnings("SameParameterValue") int messageResId) {
        if (value == null) {
            Toast.makeText(getContext(), messageResId, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    protected boolean checkSelectedId(long value, @SuppressWarnings("SameParameterValue") int messageResId) {
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


    @Override
    public void onPause() {
        super.onPause();
        if (shouldLock()) {
            PinProtection.lock(getContext());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (shouldLock()) {
            PinProtection.unlock(getContext());
        }
    }

}
