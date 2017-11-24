package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.R;

/**
 * Created by Pritam Kadam on 27/06/2017.
 */
public class AlertDialogUtility {

    /**
     * function to show alert dialog
     * @param activity
     * @param title
     * @param message
     * @param positiveButton
     * @param negativeButton
     */
    public static void showDialog(Activity activity, String title, String message, String positiveButton, String negativeButton, final AlertDialogClickListner alertDialogClickListner) {
        // custom dialog
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_message);

        if(!ValidatorUtility.isBlank(title)) {
            TextView textViewTitle = (TextView) dialog.findViewById(R.id.textViewTitle);
            textViewTitle.setText(title);
            textViewTitle.setVisibility(View.VISIBLE);
        }

        if(!ValidatorUtility.isBlank(message)) {
            // set the custom dialog components - text, image and button
            TextView textViewMessage = (TextView) dialog.findViewById(R.id.textViewMessage);
            textViewMessage.setText(message);
            textViewMessage.setVisibility(View.VISIBLE);
        }

        if(!ValidatorUtility.isBlank(negativeButton)) {
            TextView textViewCancel = (TextView) dialog.findViewById(R.id.textViewCancel);
            // if button is clicked, close the custom dialog
            textViewCancel.setText(negativeButton);
            textViewCancel.setVisibility(View.VISIBLE);

            if(alertDialogClickListner!=null) {
                textViewCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        alertDialogClickListner.onCancelClick();
                    }
                });
            }
        }

        if(!ValidatorUtility.isBlank(positiveButton)) {
            TextView textViewOk = (TextView) dialog.findViewById(R.id.textViewOk);
            // if button is clicked, close the custom dialog
            textViewOk.setText(positiveButton);
            textViewOk.setVisibility(View.VISIBLE);

            if(alertDialogClickListner!=null) {
                textViewOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        alertDialogClickListner.onOkClick();
                    }
                });
            }
        }

        dialog.show();

        //Grab the window of the dialog, and change the width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        //This makes the dialog take up the full width
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }

    public abstract static class AlertDialogClickListner{
        public abstract void onOkClick();
        public abstract void onCancelClick();
    }

    /**
     * function to show alert dialog for notification settings
     * @param activity
     */
    public static void showNotificationConfirmationDialog(Activity activity, final AlertDialogNotificationClickListner alertDialogNotificationClickListner) {
        // custom dialog
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_notification_confirmation);

        ((Button) dialog.findViewById(R.id.buttonYes)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                alertDialogNotificationClickListner.onYesClick();
            }
        });

        ((Button) dialog.findViewById(R.id.buttonLater)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                alertDialogNotificationClickListner.onLaterClick();
            }
        });

        ((Button) dialog.findViewById(R.id.buttonDoNotDisplay)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                alertDialogNotificationClickListner.onDoNotDisplayClick();
            }
        });

        dialog.show();

        //Grab the window of the dialog, and change the width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        //This makes the dialog take up the full width
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }

    public interface AlertDialogNotificationClickListner{
        void onYesClick();
        void onLaterClick();
        void onDoNotDisplayClick();
    }

    /**
     * function to show alert dialog for rating application
     * @param activity
     */
    public static void showRateAppDialog(Activity activity, final AlertDialogRateAppClickListner alertDialogRateAppClickListner) {
        // custom dialog
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rate_app);

        ((Button) dialog.findViewById(R.id.buttonSupport)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                alertDialogRateAppClickListner.onSupport();
            }
        });

        ((Button) dialog.findViewById(R.id.buttonWillNotSupport)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                alertDialogRateAppClickListner.onWillNotSupport();
            }
        });

        dialog.show();

        //Grab the window of the dialog, and change the width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        //This makes the dialog take up the full width
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }

    public interface AlertDialogRateAppClickListner{
        void onSupport();
        void onWillNotSupport();
    }
}
