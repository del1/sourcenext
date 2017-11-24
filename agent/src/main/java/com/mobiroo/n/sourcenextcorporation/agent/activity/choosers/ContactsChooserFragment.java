package com.mobiroo.n.sourcenextcorporation.agent.activity.choosers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@SuppressLint("ValidFragment")
public class ContactsChooserFragment extends ListFragment {

    protected LayoutInflater mInflater;
    protected ContactAdapter mAdapter;
    protected TextView mCountView;

    protected ArrayList<Contact> mContactsList; 	// list of all contacts
    protected HashMap<String, String> mMap; 		// hash of contacts selected

    protected HashMap<String, String> mLookup;    // hash of contacts and names

    protected EditText mSearchEditText;

    protected LinearLayout mSearchEditTextContainer;

    protected boolean mAllowStrangers;
	protected boolean mSelectAll;
    protected boolean mIsDirty;

    public static ContactsChooserFragment newInstance(String serializedList) {
        ContactsChooserFragment myFragment = new ContactsChooserFragment();

        Bundle args = new Bundle();
        args.putString("serializedList", serializedList);
        myFragment.setArguments(args);

        return myFragment;
    }

	public ContactsChooserFragment() {
        mMap = new HashMap<String, String>();
	}

	protected void deSerializeContactsString(String serializedList) {
		if (serializedList == null)
			return;

		mSelectAll = serializedList.contains(AgentPreferences.SMS_AUTORESPOND_CONTACT_EVERYONE);
		serializedList.replace(AgentPreferences.SMS_AUTORESPOND_CONTACT_EVERYONE, "");
		mAllowStrangers = serializedList.contains(AgentPreferences.SMS_AUTORESPOND_CONTACT_STRANGERS);
		serializedList.replace(AgentPreferences.SMS_AUTORESPOND_CONTACT_STRANGERS, "");

		String[] ids = serializedList.split(AgentPreferences.STRING_SPLIT);
		
		for (String id : ids) {
			if (!(id.equals("")||id.equals(AgentPreferences.SMS_AUTORESPOND_CONTACT_STRANGERS)||
					id.equals(AgentPreferences.SMS_AUTORESPOND_CONTACT_EVERYONE))) {
				mMap.put(id, id);
			}
		}
	}

    public boolean getAllowStrangers() {
        return mAllowStrangers;
    }

    public void setAllowStrangers(boolean strangers) {
        mIsDirty = true;
        mAllowStrangers = strangers;
    }


	/*
	 * "everyone" "strangers" (anyone not in contact list)
	 */

    public boolean isDirty() {
        return mIsDirty;
    }

    public String serializeContactsList() {
		StringBuilder sb = new StringBuilder();


		// if strangers are allowed -- add the strangers contact
		if (mAllowStrangers) {
			sb.append(AgentPreferences.SMS_AUTORESPOND_CONTACT_STRANGERS);
			sb.append(AgentPreferences.STRING_SPLIT);
		}
		

		// add contacts (or everyone if all contacts are selected)
		if(mMap.keySet().size() == mContactsList.size())
		{
			sb.append(AgentPreferences.SMS_AUTORESPOND_CONTACT_EVERYONE);
		}
		else
		{
			for (String id : mMap.keySet()) {
				sb.append(id);
				sb.append(AgentPreferences.STRING_SPLIT);
			}
		}
		
		Logger.d("output string: [" + sb.toString() + "]");

		return sb.toString();
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contact_chooser, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.contacts_select_all:
                selectAll();
                return true;
            case R.id.contacts_select_none:
                selectNone();
                return true;
            case R.id.action_search_toggle:
                mSearchEditTextContainer.setVisibility((mSearchEditTextContainer.getVisibility() == View.GONE) ? (View.VISIBLE) : (View.GONE));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setContactsListChecks() {
		   int total = getListView().getCount();
		    for (int i = 0; i < total; i++) {
		      Contact c = (Contact)getListView().getItemAtPosition(i);
		      if (mMap.get(c.getId()) != null) {
		        //getListView().setItemChecked(i, true);
		        //Log.i("cc_debug", "(setContactsListChecks) setting true: // c: " + c.getId() + "/" + c.getName());
		      } else {
		        //getListView().setItemChecked(i, false);
		       }
		     }
		   }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle arguments = this.getArguments();
        if(arguments != null) {
            String serializedList = arguments.getString("serializedList");
            if(serializedList != null) {
                deSerializeContactsString(serializedList);
            }
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		mInflater = inflater;
		mContactsList = new ArrayList<Contact>();
		mLookup = new HashMap<String, String>();
        mIsDirty = false;

		View chooserView = mInflater.inflate(
				R.layout.fragment_contacts_chooser, null);

		// Check for permission before accessing contacts
		if (Utils.isPermissionGranted(getActivity(), Manifest.permission.READ_CONTACTS)) {
			// setup list
			ContentResolver cr = getActivity().getApplicationContext().getContentResolver();
			Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if ((cur.getCount() > 0) && (cur.moveToFirst()))
				do {
					if (cur.getInt(cur
							.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) == 1) {
						Contact c = new Contact(cur);
						mContactsList.add(c);
						mLookup.put(c.getId(), c.getName());

						if (mSelectAll) {
							mMap.put(c.getId(), c.getId());
						}
					}
				} while (cur.moveToNext());
		} else {
			Utils.postNotification(getActivity(), new String[] {
                    Manifest.permission.READ_CONTACTS
            });
		}

		Collections.sort(mContactsList);

		mCountView = (TextView) chooserView
				.findViewById(R.id.contacts_selected_text);

		chooserView.findViewById(R.id.contacts_select_container).setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {

				View view = getActivity().getLayoutInflater().inflate(R.layout.contacts_dialog, null,
						false);

				String text;
				if(mMap.size() > 0) {
					StringBuilder sbNames = new StringBuilder();
					for(String id : mMap.values()) {
						sbNames.append(mLookup.get(id));
						sbNames.append(", ");
					}
					
					text = String.format(
							getResources().getString(R.string.cc_contacts_names),
							Integer.toString(mMap.size()),
							sbNames.subSequence(0, sbNames.length() - 2));
					} else {			
						text = String.format(
							getResources().getString(R.string.cc_contacts),
							Integer.toString(mMap.size()));
					}

				
				((TextView) view.findViewById(R.id.contacts_text)).setText(text);
				
				new AlertDialog.Builder(getActivity()).setView(view).setPositiveButton(R.string.app_issue_ok, null).show();
				
			}
			
		} );

		// setup search
        mSearchEditTextContainer = (LinearLayout) chooserView.findViewById(R.id.contacts_inputSearch_container);
        mSearchEditTextContainer.setVisibility(View.GONE);

		mSearchEditText = (EditText) chooserView
				.findViewById(R.id.contacts_inputSearch);
		mSearchEditText.addTextChangedListener(mTextWatcher);

		return chooserView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			String serializedList = savedInstanceState.getString("contacts");
			mMap = new HashMap<String, String>();
			deSerializeContactsString(serializedList);
            mIsDirty = savedInstanceState.getBoolean("dirty", false);
		}
		setContactsListChecks();
		setListCount();

        View headerView = View.inflate(getActivity(), R.layout.layout_simple_switch, null);
        Switch otherNumbersCheckBox = (Switch) headerView.findViewById(R.id.switch_label);

        otherNumbersCheckBox.setChecked(getAllowStrangers());

        otherNumbersCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        setAllowStrangers(true);
                    } else {
                        setAllowStrangers(false);

                    }
            }
        });

        getListView().addHeaderView(headerView, null, false);

        setAdapter();

        setListAdapter(mAdapter);

    }

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("contacts", serializeContactsList());
        savedInstanceState.putBoolean("dirty", mIsDirty);
	}

	protected class Contact implements Comparable<Object> {
		private String _name;
		private String _id;

		public Contact(Cursor cur) {
			_id = cur.getString(cur
					.getColumnIndex(ContactsContract.Contacts._ID));
			_name = cur.getString(cur
					.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		}
		
		public String getName() {
			return (_name == null ? "Unknown" : _name);
		}
		public String getId() {
			return _id;
		}

		@Override
		public int compareTo(Object another) {
			return getName().compareTo(((Contact) another).getName());
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		String sId = Long.toString(id);
        mIsDirty = true;

		if (mMap.containsKey(sId)) {
			mMap.remove(sId);
            v.setBackgroundResource(R.drawable.item_default_unchecked_clickable);
		} else {
			mMap.put(sId, sId);
            v.setBackgroundResource(R.drawable.item_default_checked_clickable);
		}

		mSelectAll = (mMap.keySet().size() == mContactsList.size());
		
		setListCount();
	}

    public void selectNone() {
        mIsDirty = true;
        int total = getListView().getCount();
        for (int i = 0; i < total; i++) {
            getListView().setItemChecked(i, false);
        }
        mMap.clear();
        mSelectAll = false;
        mSearchEditText.setText("");
        setListCount();
    }

    public void selectAll() {
        mIsDirty = true;
        int total = getListView().getCount();
        for (int i = 0; i < total; i++) {
            getListView().setItemChecked(i, true);
        }
        mMap.clear();
        for (Contact c : mContactsList) {
            mMap.put(c.getId(), c.getId());
        }
        mSelectAll = true;
        mSearchEditText.setText("");
        setListCount();
    }

	protected TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
			mAdapter.getFilter().filter(cs);
			mAdapter.notifyDataSetInvalidated();

		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
		}

		@Override
		public void afterTextChanged(Editable arg0) {
		}
	};

	protected void setListCount() {
		if(mSelectAll) {
			mCountView.setText(String.format(
					getResources().getString(R.string.cc_contacts),
					Integer.toString(mContactsList.size())));
		} else {
			if(mMap.size() > 0) {
			StringBuilder sbNames = new StringBuilder();
			for(String id : mMap.values()) {
				sbNames.append(mLookup.get(id));
				sbNames.append(", ");
			}
			
			mCountView.setText(String.format(
					getResources().getString(R.string.cc_contacts_names),
					Integer.toString(mMap.size()),
					sbNames.subSequence(0, sbNames.length() - 2)));
			} else {			
			mCountView.setText(String.format(
					getResources().getString(R.string.cc_contacts),
					Integer.toString(mMap.size())));
			}
		}
	}

	protected void setAdapter() {
		mAdapter = new ContactAdapter(getActivity(), mContactsList);
	}

	private class ContactAdapter extends BaseAdapter implements Filterable {
		private LayoutInflater mInflater;

		ArrayList<Contact> mBaseContactsList;
		ArrayList<Contact> mFilteredContactsList;

		public ContactAdapter(Context context, ArrayList<Contact> contactsList) {
			mInflater = LayoutInflater.from(context);
			mBaseContactsList = contactsList;
			mFilteredContactsList = contactsList;
		}

		public int getCount() {
			return mFilteredContactsList.size();
		}

		public Contact getItem(int position) {
			return mFilteredContactsList.get(position);
		}

		public long getItemId(int position) {
			return Long.decode(mFilteredContactsList.get(position).getId());
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.list_view_item_checkable,
						parent, false);
			}

			Contact c = getItem(position);

			String name = c.getName();
			((TextView) convertView.findViewById(R.id.text)).setText(name);

			if ((ContactsChooserFragment.this.mMap.get(c.getId()) != null) || mSelectAll) {
				((ListView) parent).setItemChecked(position, true);
                convertView.setBackgroundResource(R.drawable.item_default_checked_clickable);
			} else {
				((ListView) parent).setItemChecked(position, false);
                convertView.setBackgroundResource(R.drawable.item_default_unchecked_clickable);
			}

			return convertView;
		}

		@Override
		public Filter getFilter() {
			return new Filter() {
				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					mFilteredContactsList = (ArrayList<Contact>) results.values;

					ContactAdapter.this.notifyDataSetChanged();
				}

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					ArrayList<Contact> filteredContactsList = new ArrayList<Contact>();

					FilterResults results = new FilterResults();
					String lwr = constraint.toString().toLowerCase();

					for (Contact c : mBaseContactsList) {
						if (c.getName().toLowerCase().contains(lwr))
							filteredContactsList.add(c);
					}
					results.values = filteredContactsList;

					return results;
				}

			};
		}
	}

}
