package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AgentPermission {
	protected int mIconId;
	protected int mReasonId;
	
	public AgentPermission(int iconId, int reasonId) {
		mIconId = iconId;
		mReasonId = reasonId;
	}
	
	public int getIconId() {
		return mIconId;
	}
	
	public int getReasonId() {
		return mReasonId;
	}
	
	public View getView(Context context) {
        View permissionView = View.inflate(context, R.layout.list_item_config_permission, null);
        
        Typeface font = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");  
        
        ImageView iconView = ((ImageView) permissionView.findViewById(R.id.icon));
        iconView.setImageResource(getIconId());
        TextView reasonText = ((TextView) permissionView.findViewById(R.id.reason));
        reasonText.setText(getReasonId());
        reasonText.setTypeface(font);          
        
		return permissionView;
	}
	
	
}
