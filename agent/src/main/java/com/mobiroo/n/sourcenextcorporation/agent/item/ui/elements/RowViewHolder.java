package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class RowViewHolder {
    // TODO: Move away from a universal holder and to more custom holders in each settings class?
    public AgentUIElement mSetting;
    public TextView mName;
    public TextView mDescription;
    public ImageView mIcon;
    public CheckBox mCheckbox;
    public EditText mEditText;
    public TextView mTextView;
}
