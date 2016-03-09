/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - Port to AppCompat 21,  add icon title
 ******************************************************************************/
package com.flowzr.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.utils.Utils;
import com.flowzr.view.NodeInflater;
import com.flowzr.view.NodeInflater.Builder;
import com.flowzr.view.NodeInflater.CheckBoxBuilder;
import com.flowzr.view.NodeInflater.EditBuilder;
import com.flowzr.view.NodeInflater.ListBuilder;
import com.flowzr.view.NodeInflater.PictureBuilder;

import java.util.List;

public class ActivityLayout {

	public final NodeInflater inflater;
	private final ActivityLayoutListener listener;

	public ActivityLayout(NodeInflater inflater, ActivityLayoutListener listener) {
		this.inflater = inflater;
		this.listener = listener;
	}

	public View addTitleNodeNoDivider(LinearLayout layout, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
		return b.withLabel(labelId).withNoDivider().create();
	}

    public View addTitleNodeNoDivider(LinearLayout layout, String label) {
        Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
        return b.withLabel(label).withNoDivider().create();
    }

	public void addInfoNodeSingle(LinearLayout layout, int id, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_single);
		b.withId(id, listener).withLabel(labelId).create();
	}

    public TextView addInfoNodeSingle(LinearLayout layout, int id, String label) {
        Builder b = inflater.new Builder(layout, R.layout.select_entry_single);
        View v = b.withId(id, listener).withLabel(label).create();
        TextView labelView = (TextView)v.findViewById(R.id.label);
        labelView.setTag(v);
        return labelView;
    }

	public TextView addInfoNode(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
        TextView data = (TextView)v.findViewById(R.id.data);
        data.setTag(v);
		return data;
	}
	
	public TextView addInfoNode(LinearLayout layout, int id, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}
	/**
	 *
	 public View addTitleNode(LinearLayout layout, int labelId) {
	 Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
	 return b.withLabel(labelId).create();
	 }

	 public View addTitleNode(LinearLayout layout, String label) {
	 Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
	 return b.withLabel(label).create();
	 }

	 public void addListNodeSingle(LinearLayout layout, int id, int labelId) {
	 Builder b = inflater.new Builder(layout, R.layout.select_entry_simple_list);
	 b.withId(id, listener).withLabel(labelId).create();
	 }

	 public TextView addInfoNode(LinearLayout layout, int id, String label, String defaultValue) {
        Builder b = inflater.new Builder(layout, R.layout.select_entry_simple);
        View v = b.withId(id, listener).withLabel(label).withData(defaultValue).create();
        TextView data = (TextView)v.findViewById(R.id.data);
        data.setTag(v);
        return data;
    }
	 public View addListNodeIcon(LinearLayout layout, int id, int labelId, String defaultValue) {
	 Builder b = inflater.new Builder(layout, R.layout.select_entry_icon);
	 return b.withId(id, listener).withLabel(labelId).withData(defaultValue).create();
	 }

	 public View addListNode(LinearLayout layout, int id) {
	 Builder b = inflater.new Builder(layout, R.layout.select_entry);
	 return b.withId(id, listener).create();
	 }

	 public TextView addListNode(LinearLayout layout, int id, String label, String defaultValue) {
	 Builder b = inflater.new Builder(layout, R.layout.select_entry);
	 View v = b.withId(id, listener).withLabel(label).withData(defaultValue).create();
	 return (TextView)v.findViewById(R.id.data);
	 }

	 public View addCheckboxNode(LinearLayout layout, int id) {
	 Builder b = inflater.new Builder(layout, R.layout.select_entry_checkbox);
	 return b.withId(id, listener).create();
	 }

	 public TextView addListNodePlusWithoutDivider(LinearLayout layout, int id, int plusId, int labelId, int defaultValueResId) {
	 ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus);
	 View v = b.withButtonId(plusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValueResId).withNoDivider().create();
	 return (TextView)v.findViewById(R.id.data);
	 }

	 public TextView addListNodePlusWithoutLabel(LinearLayout layout, int id, int plusId, int defaultValueResId) {
	 ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus_no_label);
	 View v = b.withButtonId(plusId, listener).withId(id, listener).withData(defaultValueResId).create();
	 return (TextView)v.findViewById(R.id.data);
	 }

	 public TextView addListNodePlus(LinearLayout layout, int id, int plusId, int labelId, int defaultValueResId) {
	 ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus);
	 View v = b.withButtonId(plusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
	 TextView textView = (TextView) v.findViewById(R.id.data);
	 textView.setTag(v);
	 return textView;
	 }

	*/
	public View addListNodeIcon(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_icon);
		return b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();		
	}


	
	public TextView addListNode(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
        TextView data = (TextView)v.findViewById(R.id.data);
		data.setHint(labelId);
        data.setTag(v);
		return data;
	}

	public TextView addListNode(LinearLayout layout, int id, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_plus_no_label);
		View v = b.withId(id, listener).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNode2(LinearLayout layout, int id,int drawableId, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_icontitle);
		View v = b.withId(id, listener).withData(defaultValue).withIcon(drawableId).create();
		return (TextView)v.findViewById(R.id.data);
	}


	
	public CheckBox addCheckboxNode(LinearLayout layout, int id, int labelId, int drawableId, int dataId, boolean checked) {
		CheckBoxBuilder b = inflater.new CheckBoxBuilder(layout);
		View v = b.withCheckbox(checked).withIcon(drawableId).withLabel(labelId).withId(id, listener).withData(dataId).create();
		return (CheckBox)v.findViewById(R.id.checkbox);
	}

	public void addInfoNodePlus(LinearLayout layout, int id, int plusId, int labelId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_simple_plus);
		b.withButtonId(plusId, listener).withLabel(labelId).withId(id, listener).create();
	}



    public TextView addListNodeCategory(LinearLayout layout) {
        ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_category);
        View v = b.withId(R.id.category, listener).withData(R.string.select_category).create();
        return (TextView)v.findViewById(R.id.data);
    }

    public View addNodeUnsplit(LinearLayout layout) {
        ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_unsplit);
        View v = b.withButtonId(R.id.add_split, listener).withId(R.id.unsplit_action, listener).withLabel(R.string.unsplit_amount).withData("0").create();
        ImageView transferImageView = (ImageView)v.findViewById(R.id.transfer);
        transferImageView.setId(R.id.add_split_transfer);
        transferImageView.setImageResource(R.drawable.ic_import_export_white_24dp);
        transferImageView.setOnClickListener(listener);
        return v;
    }

    public View addSplitNodeMinus(LinearLayout layout, int id, int minusId, int labelId, String defaultValue) {
        ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_minus);
        return b.withButtonId(minusId, listener).withoutMoreButton().withId(id, listener).withLabel(labelId).withData(defaultValue).create();
    }

	public TextView addFilterNodeMinus(LinearLayout layout, int id, int minusId, int labelId, int defaultValueResId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_minus);
		View v = b.withButtonId(minusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
        hideMinusButton(v, minusId);
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addFilterNodeMinus(LinearLayout layout, int id, int minusId, int labelId, String defaultValue) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_minus);
		View v = b.withButtonId(minusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValue).create();
        hideMinusButton(v, minusId);
		return (TextView)v.findViewById(R.id.data);
	}

    private void hideMinusButton(View v, int minusId) {
        ImageView plusImageView = (ImageView)v.findViewById(minusId);
        plusImageView.setVisibility(View.GONE);
    }

    public ImageView addPictureNodeMinus(Context context, LinearLayout layout, int id,int drawableId, int minusId, int labelId, int defaultLabelResId) {
		PictureBuilder b = inflater.new PictureBuilder(layout);
		View v = b.withPicture(context, null).withButtonId(minusId, listener).withId(id, listener)
				  .withLabel(labelId).withData(defaultLabelResId).create();
		return (ImageView)v.findViewById(R.id.picture);
	}


	public View addEditNode(LinearLayout layout, int labelId, View view) {
		EditBuilder b = inflater.new EditBuilder(layout, view);
		return b.withLabel(labelId).create();
	}

	public View addEditNode2(LinearLayout layout, int drawableId, View view) {
		EditBuilder b = inflater.new EditBuilder(layout, view);
		return b.withIcon(drawableId).create();
	}

	public View addEditNode2(LinearLayout layout, int drawableId, int labelId, View view) {
		EditBuilder b = inflater.new EditBuilder(layout, view);
		return b.withIcon(drawableId).withLabel(labelId).create();
	}


	private void selectSingleChoice(Context context, int titleId, ListAdapter adapter, int checkedItem,
			DialogInterface.OnClickListener onClickListener) {
		new AlertDialog.Builder(context)
		.setSingleChoiceItems(adapter, checkedItem, onClickListener)
		.setTitle(titleId)
		.show();
	}

	private void selectSingleChoiceWithAdd(Context context, int titleId, ListAdapter adapter, int checkedItem,
									DialogInterface.OnClickListener onClickListener,int addOption) {
		new AlertDialog.Builder(context)
				.setSingleChoiceItems(adapter, checkedItem, onClickListener)
				.setTitle(titleId)
				.setNeutralButton(addOption, onClickListener)
				.show();
	}

	public void selectMultiChoice(Context context, final int id, int titleId, final List<? extends MultiChoiceItem> items) {
		int count = items.size();
		String[] titles = new String[count];
		boolean[] checked = new boolean[count];
		for (int i=0; i<count; i++) {
			titles[i] = items.get(i).getTitle();
			checked[i] = items.get(i).isChecked();
		}
		new AlertDialog.Builder(context)
		.setMultiChoiceItems(titles, checked, new DialogInterface.OnMultiChoiceClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				items.get(which).setChecked(isChecked);
			}
		})
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.onSelected(id, items);
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		})
		.setTitle(titleId)
		.show();
	}
	
	public void selectPositionWithAddOption(Context context, final int id, int titleId,
			final ListAdapter adapter, int selectedPosition,int addOption) {
		selectSingleChoiceWithAdd(context, titleId, adapter, selectedPosition,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						listener.onSelectedPos(id, which);
					}
				}, addOption);
	}

	public void selectPosition(Context context, final int id, int titleId,
							   final ListAdapter adapter, int selectedPosition) {
		selectSingleChoice(context, titleId, adapter, selectedPosition,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						listener.onSelectedPos(id, which);
					}
				});
	}

	public void selectItemId(Context context, final int id, int titleId, 
			final ListAdapter adapter, int selectedPosition) {		
		selectSingleChoice(context, titleId, adapter, selectedPosition, 
				new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						long selectedId = adapter.getItemId(which);
						listener.onSelectedId(id, selectedId);						
					}
		});
	}

	public void select(Context context, final int id, int titleId, 
			final Cursor cursor, final ListAdapter adapter, 
			final String idColumn, long valueId) {		
		int pos = Utils.moveCursor(cursor, idColumn, valueId);
		selectSingleChoice(context, titleId, adapter, pos,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						cursor.moveToPosition(which);
						long selectedId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn));
						listener.onSelectedId(id, selectedId);
					}
				});
	}

	public void selectWithAddOption(Context context, final int id, int titleId,
					   final Cursor cursor, final ListAdapter adapter,
					   final String idColumn, long valueId,int addOption, final int requestCode) {
		int pos=0;
		try {
			pos = Utils.moveCursor(cursor, idColumn, valueId);
		} catch (Exception e) {
			//pass unkwown location
		}
		selectSingleChoiceWithAdd(context, titleId, adapter, pos,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						if (which == DialogInterface.BUTTON_NEUTRAL) {
							listener.onSelectedId(id, requestCode);
						} else {
							cursor.moveToPosition(which);
							long selectedId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn));
							listener.onSelectedId(id, selectedId);
						}
					}
				}, addOption);
	}

    public View addRateNode(LinearLayout layout) {
        return inflater.new Builder(layout, R.layout.select_entry_rate)
                .withLabel(R.string.rate)
                .withData(R.string.no_rate)
                .create();
    }

}
