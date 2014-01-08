package com.coco.slidinguppanel.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class SlidingUpPanelTestActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sliding_up_panel_test);
	}

}
