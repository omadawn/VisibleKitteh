package com.forstersfreehold.VisibleKitteh;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class KittehActivity extends Activity {
	private static final String TAG = "VisibleKitteh::Activity";

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
}
