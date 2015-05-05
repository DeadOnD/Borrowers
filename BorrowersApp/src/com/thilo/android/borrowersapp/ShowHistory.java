package com.thilo.android.borrowersapp;

import com.thilo.anroid.borrowersapp.R;

import android.database.Cursor;

public class ShowHistory extends AbstractListFragment {

	@Override
	protected int getIntentTitle() {
        return R.string.history_title;
	}

    @Override
    boolean optionsMenuAvailable() {
        return false;
    }

	@Override
	protected int getEditAction() {
		return AddObject.ACTION_EDIT_RETURNED;
	}

    @Override
    protected boolean redirectToDefaultListAfterEdit() {
        return true;
    }

	@Override
	protected Cursor getDisplayedObjects() {
		return mDbHelper.fetchReturnedObjects();
	}

    @Override
    protected boolean isMarkAsReturnedAvailable() {
        return false;
    }
}
