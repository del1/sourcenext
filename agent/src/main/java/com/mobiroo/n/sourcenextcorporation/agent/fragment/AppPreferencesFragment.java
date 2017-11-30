package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.billing.IabClient;
import com.mobiroo.n.sourcenextcorporation.agent.compat.AppIssueActivity;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPrefDumper;
import com.mobiroo.n.sourcenextcorporation.agent.util.AlertDialogUtility;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.DebugFileUtils;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;

public class AppPreferencesFragment extends Fragment {

    protected SharedPreferences mSharedPrefs;
    protected View mView;
    protected View mSettingsView;

    protected RadioButton mNotificationRadioOne;
    // protected RadioButton mNotificationRadioAll;
    protected RadioButton mNotificationRadioNone;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mSettingsView = inflater.inflate(R.layout.fragment_settings_main, null);

        mNotificationRadioOne = (RadioButton) mSettingsView
                .findViewById(R.id.notifications_one);
        mNotificationRadioNone = (RadioButton) mSettingsView
                .findViewById(R.id.notifications_none);

        attachOnClick(R.id.notifications_one, mSettingsView);
        attachOnClick(R.id.notifications_none, mSettingsView);

        //rohan's code...
        //this are the changes made for the japanese version of code
        /*
        attachOnClick(R.id.prefAnalytics, mSettingsView);
        attachOnClick(R.id.prefDebugViewContainer, mSettingsView);
        attachOnClick(R.id.prefDebugClearContainer, mSettingsView);
        attachOnClick(R.id.prefZipDebugFile, mSettingsView);
        */

        attachOnClick(R.id.prefPreserveSettingsOnAgentUninstall, mSettingsView);
        attachOnClick(R.id.prefSoundOnAgentStart, mSettingsView);
        attachOnClick(R.id.prefPushNotification, mSettingsView);
        attachOnClick(R.id.showWelcomeButton, mSettingsView);
        attachOnClick(R.id.showIssuesButton, mSettingsView);
        attachOnClick(R.id.showAboutButton, mSettingsView);
        attachOnClick(R.id.showPrivacyPolicyButton, mSettingsView);

        if (!IabClient.checkLocalUnlock(getActivity())) {
            mSettingsView.findViewById(R.id.prefPreserveSettingsOnAgentUninstall).setVisibility(View.GONE);
        }
        return mSettingsView;
    }

    protected void attachOnClick(int id, View v) {
        v.findViewById(id).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPreferencesFragment.this.onClick(v);
            }
        });
    }
    protected void setCheckboxChecked(int id, boolean checked) {
        ((CheckBox) mView.findViewById(id)).setChecked(checked);
    }

    protected void setChecked(int id, boolean checked) {
        ((Switch) mView.findViewById(id)).setChecked(checked);
    }

    protected boolean getCheckboxChecked(int id) {
        return ((CheckBox) mView.findViewById(id)).isChecked();
    }

    protected boolean getChecked(int id) {
        return ((Switch) mView.findViewById(id)).isChecked();
    }

    protected SharedPreferences getPrefs(Context context) {
        if (mSharedPrefs == null) {
            mSharedPrefs = context.getSharedPreferences(Constants.PREFS_NAME, 0);
        }
        return mSharedPrefs;
    }

    protected SharedPreferences.Editor getEditor(Context context) {
        mSharedPrefs = getPrefs(context);
        return mSharedPrefs.edit();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mView = view;
        loadSettings();
    }



    public void onClick(final View v) {

        Intent intent;
        switch (v.getId()) {

            case R.id.notifications_one:
                if (((RadioButton) v).isChecked()) {
                    getEditor(getActivity()).putInt(Constants.PREF_NOTIFICATIONS,
                            Constants.PREF_VAL_NOTIFICATIONS_ONE).commit();
                }
                break;

            // case R.id.notifications_all:
            // if (((RadioButton) v).isChecked()) {
            // getEditor(getActivity()).putInt(Constants.PREF_NOTIFICATIONS,
            // Constants.PREF_VAL_NOTIFICATIONS_ALL).commit(); }
            // break;

            case R.id.notifications_none:
                if (((RadioButton) v).isChecked()) {
                    getEditor(getActivity()).putInt(Constants.PREF_NOTIFICATIONS,
                            Constants.PREF_VAL_NOTIFICATIONS_NONE).commit();
                }
                break;

            case R.id.prefSoundOnAgentStart:
                setChecked(v.getId(), getChecked(v.getId()));
                PrefsHelper.setPrefBool(getActivity(), Constants.PREF_SOUND_ON_AGENT_START, getChecked(v.getId()));
                break;

            case R.id.prefPushNotification:
                if(getChecked(v.getId())) {
                    AlertDialogUtility.showDialog(getActivity(),
                            getString(R.string.notification_confirmation_title),
                            getString(R.string.notification_confirmation_message),
                            getString(R.string.yes_recommended),
                            getString(R.string.label_no),
                            new AlertDialogUtility.AlertDialogClickListner() {
                                @Override
                                public void onOkClick() {

                                    setChecked(v.getId(), true);
                                    PrefsHelper.setPrefBool(getActivity(), Constants.PREF_PUSH_NOTIFICATION, true);
                                }

                                @Override
                                public void onCancelClick() {
                                    setChecked(v.getId(), false);
                                    PrefsHelper.setPrefBool(getActivity(), Constants.PREF_PUSH_NOTIFICATION, false);
                                }
                            });
                } else {
                    setChecked(v.getId(), false);
                    PrefsHelper.setPrefBool(getActivity(), Constants.PREF_PUSH_NOTIFICATION, false);
                }
                break;

            //rohan's code...
            //this is commented for the japanese version of code....
            /*
            case R.id.prefAnalytics:
                setChecked(v.getId(), getChecked(v.getId()));
                if (!getChecked(v.getId())) {
                    Usage.logAnalyticsRemoved(getActivity());
                }
                getEditor(getActivity()).putBoolean(Constants.PREF_USE_ANALYTICS,
                        getChecked(v.getId())).commit();
                break;
            case R.id.prefZipDebugFile:
                setChecked(v.getId(), getChecked(v.getId()));
                PrefsHelper.setPrefBool(getActivity(), Constants.PREF_ZIP_DEBUG_FILE, getChecked(v.getId()));
                break;

                */

            case R.id.prefPreserveSettingsOnAgentUninstall:
                setChecked(v.getId(), getChecked(v.getId()));
                PrefsHelper.setPrefBool(getActivity(), Constants.PREF_PRESERVE_SETTINGS_AGENT_UNINSTALL, getChecked(v.getId()));
                AgentPrefDumper.dumpAgentPrefs(getActivity());
                break;

            //rohan's code...
            //this code is commented for the japanese version of code...
            /*
            case R.id.prefDebugViewContainer:
                DebugFileUtils.viewDebugLog(getActivity());
                break;
            case R.id.prefDebugClearContainer:

                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.agent_confirm_sure)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                DebugFileUtils.clearDebugLog(getActivity());
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                break;
                */


            case R.id.showWelcomeButton:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(Constants.PREF_WELCOME_ACTIVITY_SEEN);
                editor.commit();

                intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            //this block is for displaying the issues about the application
            case R.id.showIssuesButton:
                intent = new Intent(getActivity(), AppIssueActivity.class);
                startActivity(intent);
                break;


            case R.id.showAboutButton:
                showAboutDialog();
                break;

            case R.id.showPrivacyPolicyButton:
                AlertDialogUtility.showDialog(getActivity(), getString(R.string.external_link_dialog_title), getString(R.string.external_link_dialog_message), getString(R.string.label_yes), getString(R.string.label_no), new AlertDialogUtility.AlertDialogClickListner() {
                    @Override
                    public void onOkClick() {
                        showPrivacyPolicy();
                    }

                    @Override
                    public void onCancelClick() {

                    }
                });
                break;
        }
        BackupManager.dataChanged(getActivity().getPackageName());
    }

    private void loadSettings() {

        //rohan's code...
        //the code is commented for the japanese version of code....
        /*
        setChecked(R.id.prefAnalytics, getPrefs(getActivity()).getBoolean(Constants.PREF_USE_ANALYTICS, true));
        */

        setChecked(R.id.prefSoundOnAgentStart, PrefsHelper.getPrefBool(getActivity(), Constants.PREF_SOUND_ON_AGENT_START, false));
        setChecked(R.id.prefPushNotification, PrefsHelper.getPrefBool(getActivity(), Constants.PREF_PUSH_NOTIFICATION, false));

        //rohan's code...
        //the code is commented for the japanese version of code...
        /*
        setChecked(R.id.prefZipDebugFile, PrefsHelper.getPrefBool(getActivity(), Constants.PREF_ZIP_DEBUG_FILE, true));
        */

        setChecked(R.id.prefPreserveSettingsOnAgentUninstall,
                PrefsHelper.getPrefBool(getActivity(), Constants.PREF_PRESERVE_SETTINGS_AGENT_UNINSTALL, false));
        int n = getPrefs(getActivity()).getInt(Constants.PREF_NOTIFICATIONS,
                Constants.PREF_VAL_NOTIFICATIONS_ONE);
        switch (n) {

            case Constants.PREF_VAL_NOTIFICATIONS_ONE:
                mNotificationRadioOne.setChecked(true);
                break;

            // case Constants.PREF_VAL_NOTIFICATIONS_ALL:
            // mNotificationRadioAll.setChecked(true);
            // break;

            case Constants.PREF_VAL_NOTIFICATIONS_NONE:
                mNotificationRadioNone.setChecked(true);
                break;
        }
    }

    private void showAboutDialog() {
        View view = getActivity().getLayoutInflater().inflate(
                R.layout.about_dialog, null, false);

        TextView heading = (TextView) view.findViewById(R.id.aboutTextHeading);

        //rohan's code...
        //this code is for the japanes version of this application...
        //heading.setText(String.format("Agent\nVersion: %s", getPackageVersion()));
        //heading.setText(String.format("スマ執事\nVersion: %s", getPackageVersion()));

        heading.setText(String.format(getString(R.string.aboutTitle_agent)+"\nVersion: %s", getPackageVersion()));


        TextView aboutTextView = (TextView) view.findViewById(R.id.aboutText);
        aboutTextView.setText(Constants.ABOUT_TEXT);
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Linkify.addLinks(aboutTextView, Linkify.WEB_URLS);

        new AlertDialog.Builder(getActivity()).setView(view).show();
    }

    private String getPackageVersion() {
        try {
            PackageInfo packageInfo = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0);
            return String.format("%s (%s)", packageInfo.versionName,
                    packageInfo.versionCode);
        } catch (Exception e) {
            return null;
        }
    }

    private void showPrivacyPolicy() {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(Constants.PRIVACY_POLICY_URL));

        if(intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

}
