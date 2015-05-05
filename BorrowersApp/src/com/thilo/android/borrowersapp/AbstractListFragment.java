package com.thilo.android.borrowersapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.thilo.android.borrowersapp.database.DatabaseHelper;
import com.thilo.android.borrowersapp.database.OpenLendDbAdapter;
import com.thilo.anroid.borrowersapp.R;

public abstract  class AbstractListFragment extends ListFragment {

    private static final int SUBMENU_EDIT = SubMenu.FIRST;
    private static final int SUBMENU_MARK_AS_RETURNED = SubMenu.FIRST + 1;
    private static final int SUBMENU_LEND_AGAIN = SubMenu.FIRST + 2;
    private static final int SUBMENU_DELETE = SubMenu.FIRST + 3;

    public static final int ACTION_ADD = 1;
    public static final int ACTION_EDIT = 2;

    public static final int RESULT_DELETE = 2;
    public static final int RESULT_RETURNED = 3;

    public static final String LOG_TAG = "WhoHasMyStuff";
    public static final String FIRST_START = "FirstStart";

    protected OpenLendDbAdapter mDbHelper;
    private Cursor mLentObjectCursor;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(getIntentTitle());

        mDbHelper = OpenLendDbAdapter.getInstance(getActivity());
        mDbHelper.open();

        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        boolean firstStart = preferences.getBoolean(FIRST_START, true);

        // Database may not be empty after upgrade
        boolean emptyDatabase = mDbHelper.fetchAllObjects().getCount() == 0;

        if (firstStart && emptyDatabase) {
            if (DatabaseHelper.existsBackupFile()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.restore_on_first_start);

                builder.setPositiveButton(R.string.restore_on_first_start_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DatabaseHelper.importDatabaseFromXML(mDbHelper);
                        fillData();
                    }
                });

                builder.setNegativeButton(R.string.restore_on_first_start_no, null);
                builder.create().show();
            }
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(FIRST_START, false);
        editor.commit();

        fillData();

        ListView listView = getListView();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                launchEditActivity(position, id);
            }
        });

        registerForContextMenu(listView);
        setHasOptionsMenu(optionsMenuAvailable());
    }

    abstract boolean optionsMenuAvailable();

    protected abstract int getIntentTitle();

    private void launchEditActivity(int position, long id) {
        Cursor c = mLentObjectCursor;
        c.moveToPosition(position);
        Bundle extras = new Bundle();
        extras.putInt(AddObject.ACTION_TYPE, getEditAction());
        extras.putLong(OpenLendDbAdapter.KEY_ROWID, id);
        extras.putString(OpenLendDbAdapter.KEY_DESCRIPTION, c.getString(
                c.getColumnIndexOrThrow(OpenLendDbAdapter.KEY_DESCRIPTION)));
        extras.putInt(OpenLendDbAdapter.KEY_TYPE, c.getInt(
                c.getColumnIndexOrThrow(OpenLendDbAdapter.KEY_TYPE)));
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = df.parse(c.getString(c.getColumnIndexOrThrow(OpenLendDbAdapter.KEY_DATE)));
            extras.putLong(OpenLendDbAdapter.KEY_DATE, date.getTime());
            Date modificationDate = df.parse(c.getString(c.getColumnIndexOrThrow(OpenLendDbAdapter.KEY_MODIFICATION_DATE)));
            extras.putLong(OpenLendDbAdapter.KEY_MODIFICATION_DATE, modificationDate.getTime());
        } catch (ParseException e) {
            throw new IllegalStateException("Illegal date in database!");
        }

        extras.putString(OpenLendDbAdapter.KEY_PERSON, c.getString(
                c.getColumnIndexOrThrow(OpenLendDbAdapter.KEY_PERSON)));
        extras.putString(OpenLendDbAdapter.KEY_PERSON_KEY, c.getString(
                c.getColumnIndexOrThrow(OpenLendDbAdapter.KEY_PERSON_KEY)));

        Intent intent = new Intent(getActivity(), AddObject.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.putExtras(extras);
        startActivityForResult(intent, ACTION_EDIT);
    }

    protected abstract int getEditAction();

    protected abstract boolean redirectToDefaultListAfterEdit();

    protected void fillData() {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Calendar now = new GregorianCalendar();

        SimpleCursorAdapter lentObjects = getLentObjects();

        lentObjects.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == 3) {
                    Calendar lentDate = new GregorianCalendar();
                    try {
                        long time = df.parse(cursor.getString(columnIndex)).getTime();
                        lentDate.setTimeInMillis(time);
                    } catch (ParseException e) {
                        throw new IllegalStateException("Unable to parse date " + cursor.getString(columnIndex));
                    }

                    TextView dateView = (TextView) view.findViewById(R.id.date);
                    dateView.setText(getTimeDifference(lentDate, now));

                    return true;
                }

                return false;
            }

        });

        setListAdapter(lentObjects);
    }

    private String getTimeDifference(Calendar lentDate, Calendar now) {
        if (now.before(lentDate)) {
            return "0 days";
        }

        // Check if one or more years have passed

        int differenceInYears = now.get(Calendar.YEAR) - lentDate.get(Calendar.YEAR);
        Calendar lentTimeInSameYear = new GregorianCalendar();
        lentTimeInSameYear.setTimeInMillis(lentDate.getTimeInMillis());
        lentTimeInSameYear.set(Calendar.YEAR, now.get(Calendar.YEAR));
        if (now.before(lentTimeInSameYear)) {
            differenceInYears--;
        }

        if (differenceInYears > 1) {
            return differenceInYears + " " + getString(R.string.years);
        }
        else if (differenceInYears > 0) {
            return differenceInYears + " " + getString(R.string.year);
        }

        // Check if one or more months have passed

        int monthsOfLentDate = lentDate.get(Calendar.YEAR) * 12 + lentDate.get(Calendar.MONTH);
        int monthsNow = now.get(Calendar.YEAR) * 12 + now.get(Calendar.MONTH);
        int differenceInMonths = monthsNow - monthsOfLentDate;
        Calendar lentTimeInSameMonth = new GregorianCalendar();
        lentTimeInSameMonth.setTimeInMillis(lentDate.getTimeInMillis());
        lentTimeInSameMonth.set(Calendar.YEAR, now.get(Calendar.YEAR));
        lentTimeInSameMonth.set(Calendar.MONTH, now.get(Calendar.MONTH));
        if (now.before(lentTimeInSameMonth)) {
            differenceInMonths--;
        }

        if (differenceInMonths > 1) {
            return differenceInMonths + " " + getString(R.string.months);
        }
        else if (differenceInMonths > 0) {
            return differenceInMonths + " " + getString(R.string.month);
        }

        // Check if one or more weeks have passed

        long difference = now.getTimeInMillis() - lentDate.getTimeInMillis();
        int differenceInDays = (int) (difference / DateUtils.DAY_IN_MILLIS);
        int differenceInWeeks = differenceInDays / 7;

        if (differenceInWeeks > 1) {
            return differenceInWeeks + " " + getString(R.string.weeks);
        }
        else if (differenceInWeeks > 0) {
            return differenceInWeeks + " " + getString(R.string.week);
        }

        // Check if one or more days have passed

        if (differenceInDays > 1) {
            return differenceInDays + " " + getString(R.string.days);
        }
        else if (differenceInDays == 1) {
            return getString(R.string.yesterday);
        }
        else if (differenceInDays == 0) {
            if (now.get(Calendar.DAY_OF_MONTH) == lentDate.get(Calendar.DAY_OF_MONTH)) {
                return getString(R.string.today);
            }
            else {
                return getString(R.string.yesterday);
            }
        }
        else {
            return getString(R.string.unknown);
        }
    }

    private SimpleCursorAdapter getLentObjects() {
        mLentObjectCursor = getDisplayedObjects();
        getActivity().startManagingCursor(mLentObjectCursor);

        String[] from = new String[]{
                OpenLendDbAdapter.KEY_DESCRIPTION,
                OpenLendDbAdapter.KEY_PERSON,
                OpenLendDbAdapter.KEY_DATE,
                OpenLendDbAdapter.KEY_MODIFICATION_DATE
        };

        int[] to = new int[]{
                R.id.toptext,
                R.id.bottomtext,
                R.id.date
        };

        return new SimpleCursorAdapter(getActivity(), R.layout.row, mLentObjectCursor, from, to);
    }

    protected abstract Cursor getDisplayedObjects();

    protected abstract boolean isMarkAsReturnedAvailable();

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(Menu.NONE, SUBMENU_EDIT, Menu.NONE, R.string.submenu_edit);

        if (isMarkAsReturnedAvailable()) {
            menu.add(Menu.NONE, SUBMENU_MARK_AS_RETURNED, Menu.NONE, R.string.submenu_mark_as_returned);
        }
        else {
            menu.add(Menu.NONE, SUBMENU_LEND_AGAIN, Menu.NONE, R.string.mark_as_lent_button);
        }

        menu.add(Menu.NONE, SUBMENU_DELETE, Menu.NONE, R.string.submenu_delete);
    }

    @Override
    public boolean onContextItemSelected (MenuItem item) {

        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(LOG_TAG, "Bad MenuInfo", e);
            return false;
        }
        int id = (int) getListAdapter().getItemId(info.position);

        if (item.getItemId() == SUBMENU_EDIT || item.getItemId() == SUBMENU_LEND_AGAIN) {
            launchEditActivity(info.position, id);
        } else if (item.getItemId() == SUBMENU_MARK_AS_RETURNED) {
            mDbHelper.markLentObjectAsReturned(id);
            fillData();
        } else if (item.getItemId() == SUBMENU_DELETE) {
            mDbHelper.deleteLentObject(id);
            fillData();
        }

        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Bundle bundle = data.getExtras();
            LentObject lentObject = new LentObject(bundle);

            if (bundle.getString(AddObject.CALENDAR_ID) != null) {
                ContentValues event = new ContentValues();

                event.put("title", getString(R.string.expected_return) + " " + lentObject.description);
                event.put("description", "Expecting the return of " + lentObject.description +
                        " from " + lentObject.personName);

                long startTime = bundle.getLong(AddObject.RETURN_DATE);
                long endTime = startTime + 1;

                event.put("dtstart", startTime);
                event.put("dtend", endTime);
                event.put("allDay", 1);

                event.put("calendar_id", bundle.getString(AddObject.CALENDAR_ID));

                if (Build.VERSION.SDK_INT >= 14) {
                    TimeZone timeZone = TimeZone.getDefault();
                    event.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
                }

                Uri eventsLocation;

                if (Build.VERSION.SDK_INT >= 8) {
                    eventsLocation = Uri.parse("content://com.android.calendar/events");
                }
                else {
                    eventsLocation = Uri.parse("content://calendar/events");
                }

                lentObject.calendarEventURI = getActivity().getContentResolver().insert(eventsLocation, event);
            }

            if (requestCode == ACTION_ADD) {
                lentObject.returned = false;
                lentObject.modificationDate = new Date();
                mDbHelper.createLentObject(lentObject);
            } else if (requestCode == ACTION_EDIT) {
                Long rowId = bundle.getLong(OpenLendDbAdapter.KEY_ROWID);
                mDbHelper.updateLentObject(rowId, lentObject);
                mDbHelper.markReturnedObjectAsLentAgain(rowId);
                if (redirectToDefaultListAfterEdit()) {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                }
            }

            fillData();
        }
        else if (resultCode == RESULT_DELETE) {
            Bundle bundle = data.getExtras();
            Long rowId = bundle.getLong(OpenLendDbAdapter.KEY_ROWID);
            mDbHelper.deleteLentObject(rowId);
            fillData();
        }
        else if (resultCode == RESULT_RETURNED) {
            Bundle bundle = data.getExtras();
            Long rowId = bundle.getLong(OpenLendDbAdapter.KEY_ROWID);
            mDbHelper.markLentObjectAsReturned(rowId);
            fillData();
        }
    }

}
