package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ParkingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ParkingAgent.ParkingInfo;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTaskCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParkingHistoryFragment extends Fragment {

	public static final String LAUNCH_MAPS_STRING = "geo:%s,%s?z=%s";
	public static final int PARKING_AGENT_LIMIT = 50;
	private LatLng mCurrentLocation;

	private GoogleMap mMap;
	private ScrollView mContentView;
	
	ArrayList<ParkingInfo> mInfos;
	ArrayList<ImageView> mIcons;

    protected AgentTaskCollection mTaskCollection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTaskCollection = new AgentTaskCollection();
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContentView = (ScrollView) inflater.inflate(
				R.layout.fragment_parking_agent_info, null);

        addRecordButton((LinearLayout) mContentView.findViewById(R.id.preMapItems));

        addMap((LinearLayout) mContentView
				.findViewById(R.id.configuration_items),
				(LinearLayout) mContentView.findViewById(R.id.mapItems));

		requestLocationPermission();

		return mContentView;
	}

	public void clearSpots() {
		ParkingInfo.clearAllSpots(getActivity());
		clearMap();
		
		LinearLayout mapItems = (LinearLayout) mContentView.findViewById(R.id.mapItems);
		mapItems.removeAllViews();
        addRecordButton(mapItems);
		
		LinearLayout configItems = (LinearLayout) mContentView.findViewById(R.id.configuration_items);
		showEmptyListMapState(configItems.getChildAt(0));
	}

    public void newSpot() {
        new NewSpotTask(getActivity()).execute();
    }

	private void moveToLocation(LatLng coordinates) {
		mCurrentLocation = coordinates;
		if (mMap == null) {
			return;
		}

		float zoom = mMap.getCameraPosition().zoom;
		if (zoom < 5) {
			zoom = 17.0f;
		}
		CameraUpdate update = CameraUpdateFactory.newLatLngZoom(coordinates,
				zoom);
		mMap.moveCamera(update);

	}

	private void drawCircle(LatLng coordinates, double radius) {
		if (mMap == null) {
			return;
		}

		CircleOptions circle = new CircleOptions();
		circle.center(coordinates);
		circle.radius(radius);
		mMap.addCircle(circle);
	}

	private void clearMap() {
		if (mMap == null) {
			return;
		}

		mMap.clear();
	}

	private void clearIcons() {
		for (ImageView icon : mIcons) {
			icon.setImageResource(R.drawable.ic_pin);
		}
	}

    private void shareLocation() {
        String uri = String.format(
                "http://maps.google.com/maps?saddr=&daddr=%f,%f",
                mCurrentLocation.latitude,
                mCurrentLocation.longitude);

        final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, String.format(Locale.ENGLISH, getString(R.string.share_body), uri));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_share_chooser)));
    }

	private void openMaps(boolean directions) {
		String uri;
		if(directions) {
		uri = String.format(
				Locale.ENGLISH,
				"http://maps.google.com/maps?saddr=&daddr=%f,%f(%s)",
				mCurrentLocation.latitude,
				mCurrentLocation.longitude,
				getActivity().getResources().getString(
						R.string.agent_parking_location));
		} else {
		uri = String.format(
				Locale.ENGLISH,
				"geo:%f,%f?q=%f,%f(%s)",
				mCurrentLocation.latitude,
				mCurrentLocation.longitude,
				mCurrentLocation.latitude,
				mCurrentLocation.longitude,
				getActivity().getResources().getString(
						R.string.agent_parking_location));
		}
		
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri
					.parse(uri));
			getActivity().startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			Logger.d("No activity found for uri: " + uri);
			return;
		}
	}

	@SuppressLint("NewApi")
	protected void addMap(LinearLayout mapParent, LinearLayout pinsParent) {
		ParkingAgent parkingAgent = (ParkingAgent) AgentFactory
				.getAgentFromGuid(this.getActivity(),
						ParkingAgent.HARDCODED_GUID);

		View agentStatus = View.inflate(this.getActivity(),
				R.layout.list_item_config_map, null);
		((SupportMapFragment) this.getActivity()
				.getSupportFragmentManager().findFragmentById(R.id.map))
				.getMapAsync(new OnMapReadyCallback() {
					@Override
					public void onMapReady(GoogleMap map) {
						mMap = map;

						if (mMap != null) {
							// enable map zoom
							mMap.getUiSettings().setZoomControlsEnabled(true);

							// disable map interaction
							mMap.getUiSettings().setScrollGesturesEnabled(false);
							mMap.getUiSettings().setZoomGesturesEnabled(false);
							mMap.getUiSettings().setRotateGesturesEnabled(false);
							mMap.getUiSettings().setMyLocationButtonEnabled(false);
							mMap.getUiSettings().setTiltGesturesEnabled(false);

							mMap.setOnMapClickListener(new OnMapClickListener() {

								@Override
								public void onMapClick(LatLng arg0) {
									openMaps(false);
								}
							});

							LatLng defaultPosition = new LatLng(37.78116, -122.39422);
							moveToLocation(defaultPosition);

						}
					}
				});

		LinearLayout mapItems = pinsParent;
		
		mInfos = parkingAgent.getParkingLocations(this.getActivity());
		List<ParkingInfo> infosSubList;
		if (mInfos.size() > PARKING_AGENT_LIMIT) {
			infosSubList = mInfos.subList(0, PARKING_AGENT_LIMIT - 1);
		} else {
			infosSubList = mInfos;
		}

		mIcons = new ArrayList<ImageView>(infosSubList.size());

		if (infosSubList.size() == 0) {
			showEmptyListMapState(agentStatus);
		} else {
			agentStatus.findViewById(R.id.maps_pin_button).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					openMaps(false);
				}
				
			});
			agentStatus.findViewById(R.id.maps_share_button).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//openMaps(true);
                    shareLocation();
				}
				
			});
		}

		View priorLine = null;
		final ArrayList<View> hiddenLines = new ArrayList<View>();
		
		for (ParkingInfo info : infosSubList) {
			View infoLine = View.inflate(this.getActivity(),
					R.layout.list_item_display_location, null);
			infoLine.setTag(info);
			
			if(priorLine != null) {
				
				ParkingInfo priorInfo = (ParkingInfo) priorLine.getTag();

				if( (priorInfo.getTime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES) > info.getTime() )
				{
					if(priorInfo.getTriggerType() == Constants.TRIGGER_TYPE_PARKING) {
						hiddenLines.add(priorLine);
						priorLine.setVisibility(View.GONE);
					}
				}
			}

			priorLine = infoLine;

			
			LinearLayout container = ((LinearLayout) infoLine
					.findViewById(R.id.container));
			final ImageView icon = ((ImageView) infoLine
					.findViewById(R.id.icon));
			mIcons.add(icon);
			TextView time = (TextView) infoLine.findViewById(R.id.time);

			TextView source = (TextView) infoLine.findViewById(R.id.source);
			ImageView sourceIcon = (ImageView) infoLine.findViewById(R.id.source_icon);

			container.setTag(info);
			container.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					clearMap();

					ParkingInfo infoSelected = (ParkingInfo) v.getTag();
					Logger.d("Moving to " + infoSelected.getLatitude() + ","
							+ infoSelected.getLongitude() + " @"
							+ infoSelected.getAccuracy() + "m");
					moveToLocation(new LatLng(infoSelected.getLatitude(),
							infoSelected.getLongitude()));
					drawCircle(new LatLng(infoSelected.getLatitude(),
							infoSelected.getLongitude()), infoSelected
							.getAccuracy());

					clearIcons();
					icon.setImageResource(R.drawable.ic_pin_selected);
				}

			});


            String timeText = (String) DateUtils.getRelativeDateTimeString(this.getActivity(), info.getTime(),
                    DateUtils.SECOND_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0);
			time.setText(timeText);

			if(info.getTriggerType() == Constants.TRIGGER_TYPE_MANUAL) {
				source.setText(R.string.parking_source_manual);
				sourceIcon.setImageResource(R.drawable.ic_parking_source_ad);
			} else if (info.getTriggerType() == Constants.TRIGGER_TYPE_PARKING) {
				source.setText(R.string.parking_source_ad);
				sourceIcon.setImageResource(R.drawable.ic_pin);
			} else if (info.getTriggerType() == Constants.TRIGGER_TYPE_BLUETOOTH) {
				source.setText(R.string.parking_source_bt);
				sourceIcon.setImageResource(R.drawable.ic_parking_source_bt);
			} else {
				source.setVisibility(View.GONE);
				sourceIcon.setVisibility(View.GONE);
			}

			mapItems.addView(infoLine, 0);
		}


		if(mapItems.getChildCount() > 0) {
			LinearLayout container = (LinearLayout) mapItems.getChildAt(0).findViewById(R.id.container);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
				container.callOnClick();
			} else {
				container.performClick();
			}

			final View clearButtonView = View.inflate(this.getActivity(), R.layout.parking_clear_button, null);
			clearButtonView.findViewById(R.id.parking_clear_text).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View c) {
					ParkingHistoryFragment.this.clearSpots();
				}
			});

			mapItems.addView(clearButtonView);
		}

		mapParent.addView(agentStatus);
		
		if (hiddenLines.size() > 0) {
			final View moreView = View.inflate(this.getActivity(),
					R.layout.parking_more_button, null);

			moreView.findViewById(R.id.parkingMoreText).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					for(View hiddenLine : hiddenLines) {
						hiddenLine.setVisibility(View.VISIBLE);
						moreView.setVisibility(View.GONE);
					}
				}
				
			});

			mapItems.addView(moreView);
		}
		
	}
	
	private void showEmptyListMapState(View statusView) {
		TextView captionText = ((TextView) statusView.findViewById(R.id.help));
		LinearLayout mapButtons = ((LinearLayout) statusView.findViewById(R.id.map_buttons));
		captionText.setText(R.string.parking_agent_map_guide_no_pins);
		mapButtons.setVisibility(View.GONE);
	}

    private void requestLocationPermission() {
        if (Utils.isMarshmallowOrUp()
                && !Utils.isPermissionGranted(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            Utils.requestPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Constants.PERMISSIONS_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Constants.PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Location permission granted
                }
                break;
        }
    }

    protected class NewSpotTask extends AsyncTask<Void, Void, Void> {
        protected Context mContext;

        public NewSpotTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            mTaskCollection.addTask(this);
        }

        @Override
        protected Void doInBackground(Void... nothing) {
            try {
                Agent pa = AgentFactory.getAgentFromGuid(mContext, ParkingAgent.HARDCODED_GUID);
                if ((pa == null) || (!pa.isInstalled())) { return null;}
                DbAgent.setActive(mContext, ParkingAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_MANUAL);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (this.isCancelled())
                return;

            mTaskCollection.completeTask(this);

            Toast.makeText(mContext, R.string.agent_started_parking_manual, Toast.LENGTH_LONG).show();
            ParkingHistoryFragment.this.getActivity().finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mTaskCollection.cancelTasks();
    }


    private void addRecordButton(LinearLayout mapItems) {
        final View newButtonView = View.inflate(this.getActivity(), R.layout.parking_new_loc_button, null);
        newButtonView.findViewById(R.id.parking_new_loc_text).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ParkingHistoryFragment.this.newSpot();
            }
        });
        mapItems.addView(newButtonView, 0);  // add to beginning
    }

}
