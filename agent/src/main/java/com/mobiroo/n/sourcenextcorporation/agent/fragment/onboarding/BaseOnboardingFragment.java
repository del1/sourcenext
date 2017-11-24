package com.mobiroo.n.sourcenextcorporation.agent.fragment.onboarding;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.item.onboarding.OnboardingCallbackInterface;
import com.mobiroo.n.sourcenextcorporation.agent.R;

/**
 * Created by krohnjw on 4/23/2014.
 */
public class BaseOnboardingFragment extends Fragment implements View.OnClickListener {

    public static final String KEY_NEXT_FRAGMENT = "onboarding_next_fragment";

    protected String NEXT_FRAGMENT_TAG = null;
    protected OnboardingCallbackInterface mCallback;

    public String getFragmentTag() {
        return getClass().getSimpleName();
    }

    public void setNextFragment(String tag) {
        NEXT_FRAGMENT_TAG = tag;
    }

    public String getNextFragment() { return NEXT_FRAGMENT_TAG; }
    public void setCallback(OnboardingCallbackInterface callback) {
        mCallback = callback;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_base_layout,  container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupButton(view, R.id.button_left);
        setupButton(view, R.id.button_right);

        view.findViewById(R.id.button_right).setOnClickListener(this);
        view.findViewById(R.id.button_left).setOnClickListener(this);
        view.findViewById(R.id.help_ok).setOnClickListener(this);

    }


    public boolean onBackPressed() {
        if (getView().findViewById(R.id.help_container).getVisibility() == View.VISIBLE) {
            getView().findViewById(R.id.help_container).setVisibility(View.GONE);
            return false;
        }
        return true;
    }

    protected void setupButton(View view, int id) {
        switch (id) {
            case R.id.button_left:
                setupButton(id, view, getLeftButtonText(), getLeftButtonBackground(), getLeftButtonTextColor(), isLeftButtonClickable());
                break;
            case R.id.button_right:
                setupButton(id, view, getRightButtonText(), getRightButtonBackground(), getRightButtonTextColor(), isRightButtonClickable());
                break;
        }

    }


    protected void setupButton(int id, View v, String text, int background, int color, boolean clickable) {
        TextView b = (TextView) v.findViewById(id);

        if (color > 0) {
            b.setTextColor(getResources().getColor(color));
        } else {
            b.setTextColor(color);
        }

        b.setClickable(clickable);

        if (text != null) {
            b.setText(text);
        }

        if (background != -1) {
            b.setBackgroundResource(background);
        }




    }

    protected String getLeftButtonText() {
        return getString(R.string.skip);
    }

    protected String getRightButtonText() {
        return getString(R.string.yes_im_in);
    }

    protected int getRightButtonBackground() {
        return R.drawable.onboarding_button_solid_clickable;
    }

    protected int getLeftButtonBackground() {
        return R.drawable.onboarding_transparent_clickable;
    }

    protected boolean isLeftButtonClickable() {
        return true;
    }

    protected boolean isRightButtonClickable() {
        return true;
    }

    protected int getRightButtonTextColor() {
        return Color.WHITE;
    }

    protected int getLeftButtonTextColor() {
        return R.color.onboarding_sub_text;
    }

    protected void leftClick() {
        if (mCallback != null) {
            mCallback.goToNext(OnboardingCallbackInterface.Action.SKIP_ONBOARDING, null);
        }
    }

    protected void rightClick() {
        if (mCallback != null) {
            mCallback.goToNext(OnboardingCallbackInterface.Action.NEXT_FRAGMENT, getNextFragment());
        }
    }

    protected void showHelp() { }

    protected void showHelp(String text) {
        ((TextView) getView().findViewById(R.id.help_text)).setText(text);
        getView().findViewById(R.id.help_image).setVisibility(View.GONE);
        getView().findViewById(R.id.help_container).setVisibility(View.VISIBLE);
    }

    protected void showHelp(String text, int image) {
        ((TextView) getView().findViewById(R.id.help_text)).setText(text);
        getView().findViewById(R.id.help_image).setVisibility(View.VISIBLE);
        ((ImageView) getView().findViewById(R.id.help_image)).setImageResource(image);
        getView().findViewById(R.id.help_container).setVisibility(View.VISIBLE);
    }

    protected void dismissHelp() {
        (getView().findViewById(R.id.help_container)).setVisibility(View.GONE);
    }

    protected String buildIntroString(int id) {
        return getString(id) + " " + "<font color=\"#009ee9\">" + getString(R.string.onboarding_learn_more) + "</font>";
    }

    protected void showFinished() {
        (getView().findViewById(R.id.finished_container)).setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_left:
                leftClick();
                break;
            case R.id.button_right:
                rightClick();
                break;
            case R.id.intro:
                showHelp();
                break;
            case R.id.help_ok:
                dismissHelp();
                break;
        }
    }
}
