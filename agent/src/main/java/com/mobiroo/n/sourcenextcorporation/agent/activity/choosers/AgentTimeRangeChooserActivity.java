package com.mobiroo.n.sourcenextcorporation.agent.activity.choosers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.activity.AgentTimeRangeEditor;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentTimeRange;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.util.Collections;
import java.util.List;

public class AgentTimeRangeChooserActivity extends FragmentActivity {

	public static final String TIME_RANGES = "TIME_RANGES";
	public static final int EDIT_TIME_RANGE = 1001;

    protected boolean mDirty;
	String mSerializedTimes;
	List<AgentTimeRange> mAgentTimeRanges;
	ListView mList;
	ArrayAdapter<AgentTimeRange> mListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

		setContentView(R.layout.activity_time_ranges_chooser);
        setTitle(R.string.edit_time_ranges);
        mDirty = false;

		if (savedInstanceState != null) {
			mSerializedTimes = savedInstanceState.getString("times");
            mDirty = savedInstanceState.getBoolean("dirty", false);
		} else {
			Bundle extras = getIntent().getExtras();
			if(extras != null) {
				mSerializedTimes = extras.getString(TIME_RANGES, "");
			} else {
				mSerializedTimes = "";
			}
		}

		mAgentTimeRanges = AgentTimeRange.getTimeRanges(mSerializedTimes);
		mList = (ListView) findViewById(R.id.listview);
        mList.setDivider(null);
		mListAdapter = getAdapter();
		mList.setAdapter(mListAdapter);

		setNoDataVisibility();

		findViewById(R.id.continue_button).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent returnData = new Intent();
						returnData.putExtra(TIME_RANGES,
								AgentTimeRange.serializeList(mAgentTimeRanges));
						setResult(RESULT_OK, returnData);

						AgentTimeRangeChooserActivity.this.finish();
					}

				});
	}

    protected void cancel() {
        if(mDirty) {
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.agent_uninstall_confirm)
                    .setMessage(R.string.confirm_cancel_message)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    setResult(RESULT_CANCELED, null);
                                    finish();
                                }
                            })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            setResult(RESULT_CANCELED, null);
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_time_range_chooser, m);

        return true;
    }

    @Override
    public void onBackPressed() {
        cancel();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case R.id.edit_all:
                intent = new Intent(AgentTimeRangeChooserActivity.this,
                        AgentTimeRangeEditor.class);
                intent.putExtra(AgentTimeRangeEditor.MODE, AgentTimeRangeEditor.MODE_ALL);
                AgentTimeRangeChooserActivity.this.startActivityForResult(
                        intent, EDIT_TIME_RANGE);
                return true;
            case R.id.add:
                intent = new Intent(AgentTimeRangeChooserActivity.this,
                        AgentTimeRangeEditor.class);
                AgentTimeRangeChooserActivity.this.startActivityForResult(
                        intent, EDIT_TIME_RANGE);
                return true;
            case android.R.id.home:
                cancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mSerializedTimes = AgentTimeRange.serializeList(mAgentTimeRanges);
			savedInstanceState.putString("times", mSerializedTimes);
            savedInstanceState.putBoolean("dirty", mDirty);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_TIME_RANGE) {
			if (resultCode == RESULT_OK) {
				int id = data.getIntExtra(AgentTimeRangeEditor.TIME_ID, -1);
				String range = data
						.getStringExtra(AgentTimeRangeEditor.TIME_RANGE);
				int mode = data.getIntExtra(AgentTimeRangeEditor.MODE, AgentTimeRangeEditor.MODE_SINGLE);

				if(mode == AgentTimeRangeEditor.MODE_SINGLE) {
					updateTimeRange(id, range);
				} else {
					updateAllTimeRanges(range);
				}
			}
		}
	}


	protected void updateAllTimeRanges(String range) {
        mDirty = true;

		AgentTimeRange input_range = new AgentTimeRange();
		input_range.deserialize(range);

		for(AgentTimeRange atr : mAgentTimeRanges) {
			atr.setStartTimeOfDay(input_range.getStartTime());
			atr.setEndTimeOfDay(input_range.getEndTime());

            if(atr.getTimeLength() > 24*60) { // if it's greater than a day
                atr.setStartDay(atr.getEndDay()); // make start day the same as the end day
            }

            if(atr.getTimeLength() > 24*60) { // if it's greater than a day (still)
                atr.setStartDay((atr.getEndDay()-1)%7); // make start day the day before end day
            }

		}
		dataChanged();
	}
	
	protected void updateTimeRange(int id, String range) {
        mDirty = true;

        if (id == -1) {
			AgentTimeRange atr = new AgentTimeRange();
			atr.deserialize(range);
			mAgentTimeRanges.add(atr);
		} else {
			if(mAgentTimeRanges.size() > id) {
				mAgentTimeRanges.get(id).deserialize(range);
			}
		}
		dataChanged();
	}

	protected void setNoDataVisibility() {

		if (mAgentTimeRanges.size() == 0) {
			findViewById(R.id.no_times).setVisibility(View.VISIBLE);
            findViewById(R.id.listview).setVisibility(View.GONE);
		} else {
			findViewById(R.id.no_times).setVisibility(View.GONE);
            findViewById(R.id.listview).setVisibility(View.VISIBLE);
		}
	}

	protected void dataChanged() {
		setNoDataVisibility();
		
		Collections.sort(mAgentTimeRanges);
		mListAdapter.notifyDataSetChanged();
	}

	protected ArrayAdapter<AgentTimeRange> getAdapter() {
		return new ArrayAdapter<AgentTimeRange>(
				AgentTimeRangeChooserActivity.this,
				R.layout.list_view_item_thin, mAgentTimeRanges) {
			@Override
			public View getView(final int position, View convertView,
					ViewGroup parent) {
				if (null == convertView) {
					convertView = View.inflate(
							AgentTimeRangeChooserActivity.this,
							R.layout.time_range_list_view_item, null);
				}

				final AgentTimeRange atr = getItem(position);

				String[] rangeStrings = atr
						.getDisplayStringRange(AgentTimeRangeChooserActivity.this);

				((TextView) convertView.findViewById(R.id.range_start_value))
						.setText(rangeStrings[0]);
				((TextView) convertView.findViewById(R.id.range_end_value))
						.setText(rangeStrings[1]);

				convertView.findViewById(R.id.edit_range).setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent intent = new Intent(
										AgentTimeRangeChooserActivity.this,
										AgentTimeRangeEditor.class);
								intent.putExtra(
										AgentTimeRangeEditor.TIME_RANGE,
										atr.serialize());
								intent.putExtra(AgentTimeRangeEditor.TIME_ID,
										position);
								AgentTimeRangeChooserActivity.this
										.startActivityForResult(intent,
												EDIT_TIME_RANGE);
							}
						});

				convertView.findViewById(R.id.delete_icon).setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {
                                mDirty = true;
                                mAgentTimeRanges.remove(position);
								AgentTimeRangeChooserActivity.this
										.dataChanged();
							}
						});
				return convertView;
			}
		};
	}

}
