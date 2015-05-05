package com.thilo.android.borrowersapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.*;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.thilo.android.borrowersapp.database.OpenLendDbAdapter;
import com.thilo.anroid.borrowersapp.R;

public class AddObject extends FragmentActivity {

    private Long mRowId;

    private Button mPickDate;
    private Button mPickReturnDate;
    private Button mAddButton;
    private Button mCancelButton;
    private Button mDeleteButton;
    private Button mReturnedButton;
    private AutoCompleteTextView mDescriptionText;
    private AutoCompleteTextView mPersonName;
    private Spinner mTypeSpinner;
    private Spinner mCalendarSpinner;
    private TextView mModificationDate;

	private String originalName;
	private String originalPersonKey;
	private String selectedPersonKey;

    private int mYear;
    private int mMonth;
    private int mDay;
    
    private int mReturnYear;
    private int mReturnMonth;
    private int mReturnDay;

    private Date selectedDate;

    private boolean addCalendarEntry;

    static final String CALENDAR_ID = "calendar_id";
    static final String ACTION_TYPE = "action_type";
    static final String RETURN_DATE = "return_date";
    static final int ACTION_ADD = 0;
    static final int ACTION_EDIT_LENT = 1;
	static final int ACTION_EDIT_RETURNED = 2;
    static final int ACTION_SELECT_PERSON = 3;

    private final String LAST_USED_CALENDAR = "LastUsedCalendar";

	private OpenLendDbAdapter mDbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_object);
        setTitle(R.string.add_title);

        mDescriptionText = (AutoCompleteTextView) findViewById(R.id.add_description);
        mTypeSpinner = (Spinner) findViewById(R.id.type_spinner);
        mPersonName = (AutoCompleteTextView) findViewById(R.id.personName);
        mAddButton = (Button) findViewById(R.id.add_button);
        mCancelButton = (Button) findViewById(R.id.cancel_button);
		mDeleteButton = (Button) findViewById(R.id.delete_button);
		mReturnedButton = (Button) findViewById(R.id.returned_button);
        mPickDate = (Button) findViewById(R.id.pickDate);
        mPickReturnDate = (Button) findViewById(R.id.returnDate);
        mCalendarSpinner = (Spinner) findViewById(R.id.calendar_select);
        mModificationDate = (TextView) findViewById(R.id.modification_date_text);

        CheckBox mAddCalendarEntryCheckbox = (CheckBox) findViewById(R.id.add_calendar_checkbox);
        ImageButton selectPerson = (ImageButton) findViewById(R.id.choosePerson);

        mDbHelper = OpenLendDbAdapter.getInstance(this);
        mDbHelper.open();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeSpinner.setAdapter(adapter);

        Bundle bundle = getIntent().getExtras();

		if (bundle.getInt(ACTION_TYPE) == ACTION_ADD) {
			mReturnedButton.setVisibility(View.GONE);
		}

        if (bundle.containsKey(OpenLendDbAdapter.KEY_ROWID)) {
            initalizeValuesFromBundle(bundle);
            mAddCalendarEntryCheckbox.setVisibility(View.GONE);
        } else {
            selectedDate = new Date();
			mDeleteButton.setVisibility(View.GONE);
            mModificationDate.setVisibility(View.GONE);
        }

        initializeDatePicker(selectedDate);
        Date returnDate = new Date(selectedDate.getTime() + 14 * DateUtils.DAY_IN_MILLIS);
        initializeReturnDatePicker(returnDate);

        selectPerson.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, ACTION_SELECT_PERSON);
            }
        });

        mAddCalendarEntryCheckbox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addCalendarEntry = ((CheckBox) v).isChecked();
                if (addCalendarEntry) {
                    mCalendarSpinner.setVisibility(View.VISIBLE);
                    mPickReturnDate.setVisibility(View.VISIBLE);
                    findViewById(R.id.select_calendar_separator).setVisibility(View.VISIBLE);
                    findViewById(R.id.select_calendar_text).setVisibility(View.VISIBLE);
                    findViewById(R.id.return_date_separator).setVisibility(View.VISIBLE);
                    findViewById(R.id.return_date_text).setVisibility(View.VISIBLE);
                } else {
                    mCalendarSpinner.setVisibility(View.GONE);
                    mPickReturnDate.setVisibility(View.GONE);
                    findViewById(R.id.select_calendar_separator).setVisibility(View.GONE);
                    findViewById(R.id.select_calendar_text).setVisibility(View.GONE);
                    findViewById(R.id.return_date_separator).setVisibility(View.GONE);
                    findViewById(R.id.return_date_text).setVisibility(View.GONE);
                }
            }
        });

        initializeCalendarSpinner();

        mDescriptionText.setAdapter(descriptionsFromOtherItemsAdapter());
        mPersonName.setAdapter(contactNameAdapter());

        mAddButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Bundle bundle = new Bundle();

                if (mRowId != null) {
                    bundle.putLong(OpenLendDbAdapter.KEY_ROWID, mRowId);
                }

                bundle.putString(OpenLendDbAdapter.KEY_DESCRIPTION, mDescriptionText.getText().toString());
                bundle.putInt(OpenLendDbAdapter.KEY_TYPE, mTypeSpinner.getSelectedItemPosition());

                Calendar c = Calendar.getInstance();
                c.set(mYear, mMonth, mDay);
                bundle.putLong(OpenLendDbAdapter.KEY_DATE, c.getTime().getTime());

                bundle.putString(OpenLendDbAdapter.KEY_PERSON, mPersonName.getText().toString());

				if (mPersonName.getText().toString().equals(originalName) && selectedPersonKey == null) {
					bundle.putString(OpenLendDbAdapter.KEY_PERSON_KEY, originalPersonKey);
				}
				else {
					bundle.putString(OpenLendDbAdapter.KEY_PERSON_KEY, selectedPersonKey);
				}

                if (addCalendarEntry) {
                    Cursor selectedItem = (Cursor) mCalendarSpinner.getSelectedItem();
                    if (selectedItem == null) {
                        showNoSelectedCalendarError();
                        return;
                    }
                    String selectedCalendarId = selectedItem.getString(selectedItem.getColumnIndex("_id"));

                    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(LAST_USED_CALENDAR, selectedCalendarId);
                    editor.commit();

                    bundle.putString(CALENDAR_ID, selectedCalendarId);
                    c.set(mReturnYear, mReturnMonth, mReturnDay);
                    bundle.putLong(RETURN_DATE, c.getTime().getTime());
                }

                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
            }
        });

		mCancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent mIntent = new Intent();
                mIntent.putExtra(OpenLendDbAdapter.KEY_ROWID, mRowId);
                setResult(ListLentObjects.RESULT_DELETE, mIntent);
                finish();
            }
        });

		mReturnedButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent mIntent = new Intent();
				mIntent.putExtra(OpenLendDbAdapter.KEY_ROWID, mRowId);
				setResult(ListLentObjects.RESULT_RETURNED, mIntent);
				finish();
			}
		});
    }

    private void showNoSelectedCalendarError() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.setTitle(getString(R.string.no_calendar_selected_title));
        dialog.setMessage(getString(R.string.no_calendar_selected_message));
        dialog.setPositiveButton("OK", null);
        dialog.show();
    }

    private void initalizeValuesFromBundle(Bundle bundle) {
        int actionType = bundle.getInt(ACTION_TYPE);
        if (actionType == ACTION_EDIT_LENT || actionType == ACTION_EDIT_RETURNED) {
            setTitle(R.string.edit_title);
            mCancelButton.setVisibility(View.GONE);
        }

        if (actionType == ACTION_EDIT_LENT) {
            mDeleteButton.setVisibility(View.GONE);
            mAddButton.setText(R.string.edit_button);
        }
        else if (actionType == ACTION_EDIT_RETURNED) {
            mReturnedButton.setVisibility(View.GONE);
            mAddButton.setText(R.string.mark_as_lent_button);
        }

        mRowId = bundle.getLong(OpenLendDbAdapter.KEY_ROWID);

        mDescriptionText.setText(bundle.getString(OpenLendDbAdapter.KEY_DESCRIPTION));
        mTypeSpinner.setSelection(bundle.getInt(OpenLendDbAdapter.KEY_TYPE));
        mPersonName.setText(bundle.getString(OpenLendDbAdapter.KEY_PERSON));
        originalName = bundle.getString(OpenLendDbAdapter.KEY_PERSON);
        originalPersonKey = bundle.getString(OpenLendDbAdapter.KEY_PERSON_KEY);
        selectedDate = new Date(bundle.getLong(OpenLendDbAdapter.KEY_DATE));
        final DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        Date modificationDate = new Date(bundle.getLong(OpenLendDbAdapter.KEY_MODIFICATION_DATE));
        mModificationDate.setText(getString(R.string.last_modified) + ": " + df.format(modificationDate));
    }

    private ArrayAdapter<String> descriptionsFromOtherItemsAdapter() {
        List<String> descriptions = new ArrayList<String>();

        Cursor returnedItems = mDbHelper.fetchAllObjects();
        int columnIndex = returnedItems.getColumnIndex(OpenLendDbAdapter.KEY_DESCRIPTION);
        while (returnedItems.moveToNext()) {
            final String description = returnedItems.getString(columnIndex).trim();
            if (!descriptions.contains(description)) {
                descriptions.add(description);
            }
        }
        returnedItems.close();

        Collections.sort(descriptions);

        return new ArrayAdapter<String>(getApplicationContext(), R.layout.autocomplete_select, R.id.tv_autocomplete, descriptions);
    }

    private ArrayAdapter<String> contactNameAdapter() {
        List<String> names = new ArrayList<String>();

        // Add names from address book
        Cursor contactsCursor =
                getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (contactsCursor != null) {
            int columnIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            while (contactsCursor.moveToNext()) {
                final String name = contactsCursor.getString(columnIndex);
                if (name != null && !names.contains(name.trim())) {
                    names.add(name.trim());
                }
            }
            contactsCursor.close();
        }

        // Add names from history and current items
        Cursor allItems = mDbHelper.fetchAllObjects();
        int columnIndex = allItems.getColumnIndex(OpenLendDbAdapter.KEY_PERSON);
        while (allItems.moveToNext()) {
            final String name = allItems.getString(columnIndex).trim();
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        allItems.close();

        Collections.sort(names);

        return new ArrayAdapter<String>(getApplicationContext(), R.layout.autocomplete_select, R.id.tv_autocomplete, names);
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
	}

    private void initializeDatePicker(final Date date) {
        mPickDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FragmentManager fm = getSupportFragmentManager();
                Bundle bundle = new Bundle();
                bundle.putInt("year", mYear);
                bundle.putInt("month", mMonth);
                bundle.putInt("day", mDay);
                DatePickerFragment pickDateDialog = new DatePickerFragment();
                pickDateDialog.setArguments(bundle);
                pickDateDialog.show(fm, "fragment_pick_date");
            }
        });

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        updateDisplay();
    }

    private void initializeReturnDatePicker(final Date date) {
        mPickReturnDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                c.set(mReturnYear, mReturnMonth, mReturnDay);

                FragmentManager fm = getSupportFragmentManager();
                Bundle bundle = new Bundle();
                bundle.putInt("year", mReturnYear);
                bundle.putInt("month", mReturnMonth);
                bundle.putInt("day", mReturnDay);
                DatePickerFragment pickDateDialog = new DatePickerFragment();
                pickDateDialog.setArguments(bundle);
                pickDateDialog.show(fm, "fragment_pick_return_date");
            }
        });

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        mReturnYear = c.get(Calendar.YEAR);
        mReturnMonth = c.get(Calendar.MONTH);
        mReturnDay = c.get(Calendar.DAY_OF_MONTH);

        updateDisplay();
    }
    
    private void initializeCalendarSpinner() {

        Cursor calendars;
        String nameColumn;

        if (Build.VERSION.SDK_INT >= 14) {
            calendars = getCalendarsForICS();
            nameColumn = CalendarContract.Calendars.CALENDAR_DISPLAY_NAME;
        }
        else {
            calendars = getCalendarsBeforeICS();
            nameColumn = "name";
        }

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String lastUsedCalendarId = preferences.getString(LAST_USED_CALENDAR, null);

        Integer initialSpinnerPosition = null;

        if (lastUsedCalendarId != null) {
            calendars.moveToFirst();
            int currentPosition = 0;
            do {
                if (lastUsedCalendarId.equals(calendars.getString(calendars.getColumnIndex("_id")))) {
                    initialSpinnerPosition = currentPosition;
                }
                ++currentPosition;
            } while (calendars.moveToNext());
        }

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item,
                calendars, new String[] {nameColumn},new int[]{android.R.id.text1});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCalendarSpinner.setAdapter(adapter);

        if (initialSpinnerPosition != null) {
            mCalendarSpinner.setSelection(initialSpinnerPosition);
        }
    }

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    public static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
    };

    private Cursor getCalendarsForICS() {
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        return cr.query(uri, EVENT_PROJECTION, null, null, null);
    }

    private Cursor getCalendarsBeforeICS() {
        Uri calendarsLocation;

        if (Build.VERSION.SDK_INT >= 8) {
            calendarsLocation = Uri.parse("content://com.android.calendar/calendars");
        }
        else {
            calendarsLocation = Uri.parse("content://calendar/calendars");
        }

        String[] columns = new String[] { "_id", "name" };
        return managedQuery(calendarsLocation, columns, "selected=1 AND name is not null", null, null);
    }

    private void updateDisplay() {
        final DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        Calendar c = Calendar.getInstance();
        
        c.set(mYear, mMonth, mDay);
        mPickDate.setText(df.format(c.getTime()));
        
        c.set(mReturnYear, mReturnMonth, mReturnDay);
        mPickReturnDate.setText(df.format(c.getTime()));
    }

    @Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
			case (ACTION_SELECT_PERSON):
				if (resultCode == Activity.RESULT_OK) {
					Uri contactData = data.getData();
					Cursor c = managedQuery(contactData, null, null, null, null);
					if (c.moveToFirst()) {
						String name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
						mPersonName.setText(name);
						selectedPersonKey = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
					}
				}
				break;
		}
	}

    void updateDate(int year, int monthOfYear, int dayOfMonth) {
        mYear = year;
        mMonth = monthOfYear;
        mDay = dayOfMonth;

        updateDisplay();
    }

    void updateReturnDate(int year, int monthOfYear, int dayOfMonth) {
        mReturnYear = year;
        mReturnMonth = monthOfYear;
        mReturnDay = dayOfMonth;

        updateDisplay();
    }
}
