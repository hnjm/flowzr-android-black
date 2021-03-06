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
package com.flowzr.model;

import android.content.ContentValues;
import android.database.Cursor;
import com.flowzr.db.DatabaseHelper.CategoryColumns;
import com.flowzr.db.DatabaseHelper.CategoryViewColumns;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;

@Entity
@Table(name = "category")
public class Category extends CategoryEntity<Category> {
	
    public static final long NO_CATEGORY_ID = 0;
	public static final long SPLIT_CATEGORY_ID = -1;

    public static boolean isSplit(long categoryId) {
        return Category.SPLIT_CATEGORY_ID == categoryId;
    }

	@Column(name = "last_location_id")
	public long lastLocationId;

	@Column(name = "last_project_id")
	public long lastProjectId;

	@Transient
	public int level;
	
	@Transient
	public List<Attribute> attributes;

    @Transient
    public String tag;

    public Category(){}
	
	public Category(long id){
		this.id = id;
	}
	
	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(CategoryColumns._id.name(), id);
		values.put(CategoryColumns.title.name(), title);
		values.put(CategoryColumns.left.name(), left);
		values.put(CategoryColumns.right.name(), right);
        values.put(CategoryColumns.type.name(), right);
		return values;
	}
	
	@Override
	public String toString() {

		String sb=new String();

		sb.concat("[");
		sb.concat("id=").concat(String.valueOf(id));
		sb.concat(",parentId=").concat(String.valueOf(getParentId()));
		sb.concat(",title=").concat(title);
		sb.concat(",level=").concat(String.valueOf(level));
		sb.concat(",left=").concat(String.valueOf(left));
		sb.concat(",right=").concat(String.valueOf(right));
        sb.concat(",type=").concat(String.valueOf(type));
		sb.concat("]");
		return sb.toString();
	}
	
	@Override
	public String getTitle() {
		return getTitle(title, level);
	}

	public static String getTitle(String title, int level) {
		String span = getTitleSpan(level);
		return span+title;
	}
	
	public static String getTitleSpan(int level) {
		level -= 1;
		if (level <= 0) {
			return "";
		} else if (level == 1) {
			return "-- ";
		} else if (level == 2) {
			return "---- ";
		} else if (level == 3) {
			return "------ ";
		} else {
			StringBuilder sb = new StringBuilder();
			if (level > 0) {
				for (int i=1; i<level; i++) {
					sb.append("--");
				}
			}
			return sb.toString();		
		}
	}

	public static Category formCursor(Cursor c) {
		long id = c.getLong(CategoryViewColumns._id.ordinal());
		Category cat = new Category();
		cat.id = id;
		cat.title = c.getString(CategoryViewColumns.title.ordinal());
		cat.level = c.getInt(CategoryViewColumns.level.ordinal());
		cat.left = c.getInt(CategoryViewColumns.left.ordinal());
		cat.right = c.getInt(CategoryViewColumns.right.ordinal());
        cat.type = c.getInt(CategoryViewColumns.type.ordinal());
		cat.lastLocationId = c.getInt(CategoryViewColumns.last_location_id.ordinal());
		cat.lastProjectId = c.getInt(CategoryViewColumns.last_project_id.ordinal());
		return cat;
	}

    public void copyTypeFromParent() {
        if (parent != null) {
            this.type = parent.type;
        }
    }

    public boolean isSplit() {
        return id == SPLIT_CATEGORY_ID;
    }

}
