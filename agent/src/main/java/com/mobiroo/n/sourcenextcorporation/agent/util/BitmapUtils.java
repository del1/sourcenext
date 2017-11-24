package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.io.InputStream;

/**
 * Created by omarseyal on 4/9/14.
 */
public class BitmapUtils {

    public static Bitmap getContactImage(Context context, TelephonyUtils.SimpleContactWrapper contactWrapper, int height, int width, boolean blackAndWhite) {
        Bitmap contactImage = getContactImage(context, contactWrapper, height, width);

        if(blackAndWhite)
            return toGrayscale(contactImage);
        else
            return contactImage;
    }

    protected static Bitmap toGrayscale(Bitmap bmpOriginal){
        if(bmpOriginal == null)
            return null;

        Bitmap bmpGrayscale = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.RGB_565);

        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);

        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);

        c.drawBitmap(bmpOriginal, 0, 0, paint);

        return bmpGrayscale;
    }

    protected static Bitmap getContactImage(Context context, TelephonyUtils.SimpleContactWrapper contactWrapper, int height, int width) {
        try {
            if (contactWrapper.photoUri == null) {
                return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_status_person), height, width, false);
            }
            Uri ptUri = Uri.parse(contactWrapper.photoUri);
            InputStream is = context.getContentResolver().openInputStream(ptUri);
            if (is != null) {
                Bitmap unscaledBitmap = BitmapFactory.decodeStream(is);
                return Bitmap.createScaledBitmap(unscaledBitmap, width, height, false);
            } else {
                Logger.d("Null contact thumbnail input stream.");
            }
        } catch (Exception e) {
            Logger.d("Could not set contact thumbnail: " + e.toString());
        }

        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_status_person), height, width, false);
    }
}
