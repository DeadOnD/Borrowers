package com.thilo.android.borrowersapp;

import android.net.Uri;
import android.os.Bundle;

import java.util.Date;

import com.thilo.android.borrowersapp.database.OpenLendDbAdapter;

public class LentObject {

    public LentObject() {
        // Empty constructor
    }

    public LentObject(Bundle bundle) {
        description = bundle.getString(OpenLendDbAdapter.KEY_DESCRIPTION);
        type = bundle.getInt(OpenLendDbAdapter.KEY_TYPE);
        date = new Date(bundle.getLong(OpenLendDbAdapter.KEY_DATE));
        modificationDate = new Date(bundle.getLong(OpenLendDbAdapter.KEY_MODIFICATION_DATE));
        personName = bundle.getString(OpenLendDbAdapter.KEY_PERSON);
        personKey = bundle.getString(OpenLendDbAdapter.KEY_PERSON_KEY);
    }

    public String description;
    public int type;
    public Date date;
    public Date modificationDate;
    public String personName;
    public String personKey;
    public boolean returned;
    public Uri calendarEventURI;

}
