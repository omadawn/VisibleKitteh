package com.forstersfreehold.VisibleKitteh;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class KittehActivity extends Activity {
    private static final String TAG             = "VisibleKitteh::Activity";

    public static final int     VIEW_MODE_RGBA  = 0;
    public static final int     VIEW_MODE_GRAY  = 1;
    public static final int     VIEW_MODE_CANNY = 2;

    private MenuItem            mItemPreviewRGBA;
    private MenuItem            mItemPreviewGray;
    private MenuItem            mItemPreviewCanny;

    public static int           viewMode        = VIEW_MODE_RGBA;

    public KittehActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState); // savedInstanceState if I remember correctly is where your variables are saved when an activity is shut down but not neccesarily if the app is destroyed. To keep variables across app shutdowns you need to write to a preferences file (which we will do later.)
        requestWindowFeature(Window.FEATURE_NO_TITLE); // Let's open a window and hide the title bar
        setContentView(new KittehAndroidView(this));
    }

    @Override
    // TODO: Replace this. This creates a little popup options menu. I want the new page style preferences menu instead. I'll have to add the preferences saving functions as well.
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        else if (item == mItemPreviewGray)
            viewMode = VIEW_MODE_GRAY;
        else if (item == mItemPreviewCanny)
            viewMode = VIEW_MODE_CANNY;
        return true;
    }
}
