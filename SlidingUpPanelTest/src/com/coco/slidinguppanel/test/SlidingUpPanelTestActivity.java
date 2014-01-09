package com.coco.slidinguppanel.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.coco.slidinguppanel.SlidingUpPanel;
import com.coco.slidinguppanel.SlidingUpPanel.OnPanelCloseListener;
import com.coco.slidinguppanel.SlidingUpPanel.OnPanelOpenListener;

public class SlidingUpPanelTestActivity extends Activity {

	private SlidingUpPanel mSlidingUpPanel;
	private ImageView mCoverDown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sliding_up_panel_test);

		mSlidingUpPanel = (SlidingUpPanel) findViewById(R.id.sliding_up_panel);
		mSlidingUpPanel.setOnPanelOpenListener(new OnPanelOpenListener() {
			@Override
			public void onPanelOpened() {
				showToast("Sliding up panel opened!");
			}
		});
		mSlidingUpPanel.setOnPanelCloseListener(new OnPanelCloseListener() {
			@Override
			public void onPanelClosed() {
				showToast("Sliding up panel closed!");
			}
		});

		mCoverDown = (ImageView) findViewById(R.id.cover_down);
		mCoverDown.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showToast("Cover down pressed!");
			}
		});
	}

	private void showToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

}
