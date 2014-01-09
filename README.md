Android-SlidingUpPanel
======================

Zaker style sliding up panel, using as zaker's cover.

Features
========

+ Android SDK **level 4+**.
+ Vertically **dragging & fling** content view (can only has one child view).
+ Sliding up to open panel, and not handle touch event when opened.
+ Programming to close panel.
+ Install listeners for opened, closed & scrolled events.

Snapshots
=========

[closed]: https://github.com/zzhouj/Android-SlidingUpPanel/raw/master/snapshot/closed.png  "Sliding up panel closed"
[opening]: https://github.com/zzhouj/Android-SlidingUpPanel/raw/master/snapshot/opening.png  "Sliding up panel opening"

![Closed snapshot][closed]
![Opening snapshot][opening]

Usage
=====
1. Add layout xml fragment like below:

		<com.coco.slidinguppanel.SlidingUpPanel
		    android:id="@+id/sliding_up_panel"
		    android:layout_width="match_parent"
		    android:layout_height="match_parent" >
		
		    <FrameLayout
		        android:layout_width="match_parent"
		        android:layout_height="match_parent" >
		
		        <ImageView
		            android:layout_width="match_parent"
		            android:layout_height="match_parent"
		            android:scaleType="centerCrop"
		            android:src="@drawable/cover_default" />
		
		        <ImageView
		            android:layout_width="wrap_content"
		            android:layout_height="wrap_content"
		            android:padding="20dp"
		            android:src="@drawable/cover_zaker_logo" />
		
		        <ImageView
		            android:id="@+id/cover_down"
		            android:layout_width="wrap_content"
		            android:layout_height="wrap_content"
		            android:layout_gravity="bottom"
		            android:layout_margin="10dp"
		            android:src="@drawable/selector_cover_down" />
		
		        <TextView
		            android:id="@+id/cover_hint"
		            android:layout_width="wrap_content"
		            android:layout_height="wrap_content"
		            android:layout_gravity="center_horizontal|bottom"
		            android:layout_margin="10dp"
		            android:text="@string/cover_hint"
		            android:textColor="#fff"
		            android:textSize="22sp" />
		    </FrameLayout>
		</com.coco.slidinguppanel.SlidingUpPanel>

2. Then find the **SlidingUpPanel** view install listeners to it:

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			super.onCreate(savedInstanceState);
			setContentView(R.layout.sliding_up_panel_test);
		
			mSlidingUpPanel = (SlidingUpPanel) findViewById(R.id.sliding_up_panel);
			mCoverDown = (ImageView) findViewById(R.id.cover_down);
			mCoverHint = (TextView) findViewById(R.id.cover_hint);
			mClickToClose = (Button) findViewById(R.id.click_to_close);
		
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
			mSlidingUpPanel.setOnPanelScrolledListener(new OnPanelScrollListener() {
				@Override
				public void onPanelScrolled(float offset) {
					Log.d(TAG, "onPanelScrolled offset=" + offset);
					mCoverDown.setAlpha((int) ((1f - offset) * 255));
				}
			});
		
			mCoverDown.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showToast("Cover down pressed!");
				}
			});
		
			mClickToClose.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mSlidingUpPanel.closePanel();
				}
			});
		}

License
=======

    The MIT License (MIT)
    
    Copyright (c) 2014 justin
    
    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
    the Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
    IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
      