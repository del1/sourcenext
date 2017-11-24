package com.mobiroo.n.sourcenextcorporation.agent.activity.choosers;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTaskCollection;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationChooserActivity extends FragmentActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_location_chooser);

        ActionBar actionBar = getActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.location_chooser);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new LocationChooserFragment())
                    .commit();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class LocationChooserFragment extends android.support.v4.app.Fragment {
        protected AgentTaskCollection mTaskCollection;

        protected Geocoder mGeocoder;

        protected LatLng mCurrentLocation;
        protected String mCurrentLocationString;

        protected STATE mCurrentState;

        protected LinearLayout mMapContainer;
        protected LinearLayout mAddressEditContainer;
        protected ListView mAddressList;
        protected TextView mPositiveButton;
        protected TextView mNegativeButton;
        protected TextView mInstruction;
        protected EditText mAddressEdit;
        protected View mNegativeButtonSpacer;

        protected enum STATE {
            TYPING_ADDRESS,
            SELECTING_ADDRESS,
            CONFIRMING_ADDRESS,
        }


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mCurrentLocation = null;
            mGeocoder = new Geocoder(getActivity(), Locale.getDefault());

            mCurrentState = STATE.TYPING_ADDRESS;

            mTaskCollection = new AgentTaskCollection();
        }



        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_location_chooser, container, false);

            mPositiveButton = (TextView) rootView.findViewById(R.id.chooser_continue);
            mPositiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    continueButtonClicked();
                }
            });

            mNegativeButton = (TextView) rootView.findViewById(R.id.chooser_clear);
            mNegativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cancelButtonClicked();
                }
            });

            mNegativeButtonSpacer = rootView.findViewById(R.id.spacer_chooser_button);

            mAddressEdit = (EditText) rootView.findViewById(R.id.address_edit);

            mAddressList = (ListView) rootView.findViewById(R.id.address_results_list);
            mAddressList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    AddressWrapper wrapper = (AddressWrapper) mAddressList.getAdapter().getItem(i);
                    mCurrentLocationString = wrapper.toString();
                    mCurrentState = STATE.CONFIRMING_ADDRESS;
                    showLocationForConfirmation(wrapper.latLng);
                }
            });

            mMapContainer = (LinearLayout) rootView.findViewById(R.id.address_map);

            mInstruction = (TextView) rootView.findViewById(R.id.address_instruction);

            mAddressEditContainer = (LinearLayout) rootView.findViewById(R.id.address_edit_container);

            setTitle();
            setVisibility();
            setButtons();

            return rootView;
        }

        protected void setTitle() {
            switch(mCurrentState) {
                case TYPING_ADDRESS:
                    mInstruction.setText(R.string.location_chooser_title_input);
                    break;
                case SELECTING_ADDRESS:
                    mInstruction.setText(R.string.location_chooser_title_select);
                    break;
                case CONFIRMING_ADDRESS:
                    mInstruction.setText(R.string.location_chooser_title_confirm);
                    break;
            }
        }

        protected void setVisibility() {
            mInstruction.setVisibility(View.VISIBLE);
            mAddressEditContainer.setVisibility(View.GONE);
            mMapContainer.setVisibility(View.GONE);
            mPositiveButton.setVisibility(View.GONE);
            mNegativeButtonSpacer.setVisibility(View.GONE);
            mNegativeButtonSpacer.setVisibility(View.GONE);
            mNegativeButton.setVisibility(View.GONE);
            mAddressList.setVisibility(View.GONE);


            switch(mCurrentState) {
                case TYPING_ADDRESS:
                    mAddressEditContainer.setVisibility(View.VISIBLE);
                    mPositiveButton.setVisibility(View.VISIBLE);
                    mNegativeButton.setVisibility(View.VISIBLE);
                    mNegativeButtonSpacer.setVisibility(View.VISIBLE);
                    break;
                case SELECTING_ADDRESS:
                    mAddressList.setVisibility(View.VISIBLE);
                    break;
                case CONFIRMING_ADDRESS:
                    mMapContainer.setVisibility(View.VISIBLE);
                    mPositiveButton.setVisibility(View.VISIBLE);
                    break;
            }
        }

        protected void setButtons() {
            switch(mCurrentState) {
                case TYPING_ADDRESS:
                    mPositiveButton.setText(R.string.location_chooser_button_lookup);
                    mNegativeButton.setText(R.string.location_chooser_clear_location);
                    break;
                case SELECTING_ADDRESS:
                    break;
                case CONFIRMING_ADDRESS:
                    mPositiveButton.setText(R.string.location_chooser_button_confirm);
                    break;
            }
        }

        protected void cancelButtonClicked() {
            switch(mCurrentState) {
                case TYPING_ADDRESS:
                    Intent intent = getActivity().getIntent();
                    intent.putExtra("clear", true);
                    getActivity().setResult(RESULT_OK, intent);
                    getActivity().finish();
                    return;
            }

            Assert.fail();
        }

        protected void continueButtonClicked() {
            switch(mCurrentState) {
                case TYPING_ADDRESS:
                    new GeocoderTask(mAddressEdit.getText().toString()).execute();
                    return;
                case CONFIRMING_ADDRESS:
                    Intent intent = getActivity().getIntent();
                    intent.putExtra("location", mCurrentLocationString + AgentPreferences.STRING_SPLIT + mCurrentLocation.latitude + AgentPreferences.STRING_SPLIT + mCurrentLocation.longitude);
                    getActivity().setResult(RESULT_OK, intent);
                    getActivity().finish();
                    return;
            }
            Assert.fail();
        }

        protected void showLocationForConfirmation(LatLng latLng) {
            mCurrentLocation = latLng;

            if(mMap == null) {
                Logger.i("adding map");
                addMap(mMapContainer);
            } else {
                Logger.i("not adding // only moving map");
                moveToLocation(mCurrentLocation);
            }

            setTitle();
            setVisibility();
            setButtons();
        }

        protected class AddressWrapper {
            public LatLng latLng;
            public String description;

            @Override
            public String toString() {
                return description;
            }
        }

        protected class GeocoderTask extends AsyncTask<Void, Void, Void> {

            protected String mLocationString;
            protected List<Address> mGeoCodeResults;
            protected ProgressDialog mDialog;

            public GeocoderTask(String locationString) {
                mLocationString = locationString;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mTaskCollection.addTask(this);

                mDialog = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.location_lookup), true);
                mDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.agent_animation_agent));
            }

            @Override
            protected Void doInBackground(Void... nothing) {
                try {
                    mGeoCodeResults = mGeocoder.getFromLocationName(mLocationString, 10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if(isCancelled())
                    return;

                mTaskCollection.completeTask(this);

                if (mGeoCodeResults == null) {
                    Logger.d("mGeoCodeResults is null");
                    mGeoCodeResults = new ArrayList<Address>();
                }


                Logger.i("Looked up: " + mLocationString + " // found (" + mGeoCodeResults.size() + ") results.");


                if(mGeoCodeResults.size() == 0) {
                    mDialog.dismiss();
                    Toast.makeText(getActivity(), "No matching locations found.", Toast.LENGTH_LONG);
                    return;
                }

                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mAddressEdit.getWindowToken(), 0);

                if(mGeoCodeResults.size() == 1) {
                    mCurrentState = STATE.CONFIRMING_ADDRESS;

                    Address address = mGeoCodeResults.get(0);
                    mCurrentLocationString = String.format( "%s, %s, %s", address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "", address.getLocality(), address.getCountryName());;

                    showLocationForConfirmation(new LatLng(mGeoCodeResults.get(0).getLatitude(), mGeoCodeResults.get(0).getLongitude()));

                    mDialog.dismiss();
                    return;
                }


                List<AddressWrapper> addressWrappers = new ArrayList<AddressWrapper>();
                for(Address address : mGeoCodeResults) {
                    AddressWrapper newWrapper = new AddressWrapper();
                    newWrapper.latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    newWrapper.description = String.format( "%s, %s, %s", address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "", address.getLocality(), address.getCountryName());
                    addressWrappers.add(newWrapper);
                }

                mCurrentState = STATE.SELECTING_ADDRESS;

                mAddressList.setAdapter(new ArrayAdapter<AddressWrapper>(getActivity(), R.layout.list_item_generic, addressWrappers));

                setTitle();
                setVisibility();
                setButtons();

                mDialog.dismiss();
            }
        }


        private GoogleMap mMap;

        @SuppressLint("NewApi")
        protected void addMap(final LinearLayout mapParent) {

            final View agentMap = View.inflate(this.getActivity(),
                    R.layout.list_item_display_map, null);
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


                                moveToLocation(mCurrentLocation);

                            }

                            mapParent.addView(agentMap);
                        }
                    });
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

        private void moveToLocation(LatLng coordinates) {
            mCurrentLocation = coordinates;
            if (mMap == null) {
                return;
            }

            clearMap();

            float zoom = mMap.getCameraPosition().zoom;
            if (zoom < 5) {
                zoom = 17.0f;
            }
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(coordinates,
                    zoom);
            drawCircle(new LatLng(coordinates.latitude,
                    coordinates.longitude), 10);

            mMap.moveCamera(update);

        }
    }
}
