package com.example.tanph.shumusic.util.UIElementHelper;

import android.graphics.Typeface;

import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.model.Constants;

public class FontFactory {
    public static Typeface getFont()
    {
        Typeface t = Typeface.MONOSPACE;
        switch (MyApplication.getPref().getInt(MyApplication.getContext().getString(R.string.pref_text_font), Constants.FONTS.MONOSPACE))
        {
            case Constants.FONTS.NORMAL:
                t = Typeface.DEFAULT;
                break;
            case Constants.FONTS.SANS_SERIF:
                t = Typeface.SANS_SERIF;
                break;
            case Constants.FONTS.SERIF:
                t = Typeface.SERIF;
                break;
        }
        return t;
    }
}