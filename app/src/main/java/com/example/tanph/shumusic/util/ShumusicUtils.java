package com.example.tanph.shumusic.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.example.tanph.shumusic.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ShumusicUtils {
    public static final String makeShortTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs %= 3600;
        mins = secs / 60;
        secs %= 60;

        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }

    public static Drawable createBlurredImageFromBitmap(Bitmap bitmap, Context context, int inSampleSize)
    {
        android.support.v8.renderscript.RenderScript renderScript = android.support.v8.renderscript.RenderScript.create(context);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
        byte[] imageInByte = stream.toByteArray();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageInByte);
        Bitmap blurTemplate = BitmapFactory.decodeStream(inputStream,null,options);

        final android.support.v8.renderscript.Allocation input = android.support.v8.renderscript.Allocation.createFromBitmap(renderScript,blurTemplate);
        final android.support.v8.renderscript.Allocation output = android.support.v8.renderscript.Allocation.createTyped(renderScript,input.getType());
        final android.support.v8.renderscript.ScriptIntrinsicBlur intrinsicBlur = android.support.v8.renderscript.ScriptIntrinsicBlur.create(renderScript, android.support.v8.renderscript.Element.U8_4(renderScript));

        intrinsicBlur.setRadius(4f);
        intrinsicBlur.setInput(input);
        intrinsicBlur.forEach(output);
        output.copyTo(blurTemplate);

        return new BitmapDrawable(context.getResources(),blurTemplate);
    }
}


