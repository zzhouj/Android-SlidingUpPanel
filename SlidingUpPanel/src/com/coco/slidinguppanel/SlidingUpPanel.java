package com.coco.slidinguppanel;

/*
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
 */

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.BounceInterpolator;
import android.widget.Scroller;

/**
 * Zaker style sliding up panel, using as zaker's cover.
 */
public class SlidingUpPanel extends ViewGroup {
	private static final String TAG = "SlidingUpPanel";
	private static final boolean DEBUG = false;
	private static final boolean USE_CACHE = false;

	private static void DEBUG_LOG(String msg) {
		if (DEBUG) {
			Log.v(TAG, msg);
		}
	}

	private static final int MAX_SETTLE_DURATION = 900; // ms
	private static final int MIN_DISTANCE_FOR_FLING = 25; // dips
	private static final int MIN_FLING_VELOCITY = 400; // dips

	private static final int INVALID_POINTER = -1;

	// states
	public static final int STATE_CLOSED = 0;
	public static final int STATE_OPENED = 1;
	public static final int STATE_DRAGGING = 2;
	public static final int STATE_FLING = 3;

	// fling
	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mMinimumVelocity;
	private int mMaximumVelocity;
	private int mFlingDistance;

	// dragging
	private int mTouchSlop;
	private boolean mIsBeingDragged;
	private boolean mIsUnableToDrag;
	private float mLastMotionX;
	private float mLastMotionY;
	private float mInitialMotionX;
	private float mInitialMotionY;
	private int mActivePointerId = INVALID_POINTER;

	// state & listener
	private int mState = STATE_CLOSED;
	private boolean mIsOpen = false;
	private OnPanelCloseListener mOnPanelCloseListener;
	private OnPanelOpenListener mOnPanelOpenListener;
	private OnPanelScrollListener mOnPanelScrollListener;
	private float mLastPanelScrolledOffset;

	// drawing cache
	private boolean mScrollingCacheEnabled;

	private final Runnable mEndScrollRunnable = new Runnable() {
		public void run() {
			setState(mIsOpen ? STATE_OPENED : STATE_CLOSED);
		}
	};

	/**
	 * Callback interface for responding to the open state of the sliding up panel.
	 */
	public interface OnPanelOpenListener {
		public void onPanelOpened();
	}

	/**
	 * Callback interface for responding to the close state of the sliding up panel.
	 */
	public interface OnPanelCloseListener {
		public void onPanelClosed();
	}

	/**
	 * Callback interface for responding to scrolling of the panel.
	 */
	public interface OnPanelScrollListener {
		/**
		 * This method will be invoked when the panel is scrolled.
		 * 
		 * @param offset
		 *            Value from [0, 1] indicating the offset of the scrolling panel.
		 */
		public void onPanelScrolled(float offset);
	}

	public SlidingUpPanel(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initSlidingUpPanel();
	}

	public SlidingUpPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
		initSlidingUpPanel();
	}

	public SlidingUpPanel(Context context) {
		super(context);
		initSlidingUpPanel();
	}

	private void initSlidingUpPanel() {
		setWillNotDraw(false);
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		setFocusable(true);

		final Context context = getContext();
		final ViewConfiguration configuration = ViewConfiguration.get(context);
		final float density = context.getResources().getDisplayMetrics().density;

		mScroller = new Scroller(context, new BounceInterpolator());
		mMinimumVelocity = (int) (MIN_FLING_VELOCITY * density);
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
		mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
	}

	@Override
	protected void onDetachedFromWindow() {
		removeCallbacks(mEndScrollRunnable);
		super.onDetachedFromWindow();
	}

	protected int getState() {
		return mState;
	}

	protected void setState(int newState) {
		if (mState == newState) {
			return;
		}

		DEBUG_LOG("setState " + mState + " ==> " + newState);
		mState = newState;

		final boolean isDraggingOrFling = mState == STATE_DRAGGING || mState == STATE_FLING;
		enableLayers(isDraggingOrFling);
		setScrollingCacheEnabled(isDraggingOrFling);

		if (mState == STATE_CLOSED) {
			if (mOnPanelCloseListener != null) {
				mOnPanelCloseListener.onPanelClosed();
			}
		} else if (mState == STATE_OPENED) {
			if (mOnPanelOpenListener != null) {
				mOnPanelOpenListener.onPanelOpened();
			}
		}
	}

	public boolean isOpen() {
		return mIsOpen;
	}

	public void closePanel() {
		if (isOpen()) {
			startFling(false, 0);
		}
	}

	public void openPanel() {
		if (!isOpen()) {
			startFling(true, 0);
		}
	}

	public void setOnPanelCloseListener(OnPanelCloseListener onPanelCloseListener) {
		mOnPanelCloseListener = onPanelCloseListener;
	}

	public void setOnPanelOpenListener(OnPanelOpenListener onPanelOpenListener) {
		mOnPanelOpenListener = onPanelOpenListener;
	}

	public void setOnPanelScrolledListener(OnPanelScrollListener onPanelScrollListener) {
		mOnPanelScrollListener = onPanelScrollListener;
	}

	protected void onPanelScrolled(float scrollY) {
		if (mOnPanelScrollListener != null) {
			final int height = getHeight();
			float offset = 0f;
			if (height > 0) {
				offset = Math.max(0f, Math.min(1f, Math.abs(scrollY / height)));
			}
			if (Math.abs(mLastPanelScrolledOffset - offset) > 0.009f) {
				mLastPanelScrolledOffset = offset;
				mOnPanelScrollListener.onPanelScrolled(offset);
			}
		}
	}

	// layout
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (getChildCount() > 1) {
			throw new IllegalStateException("SlidingUpPanel can only contain on child view");
		} else {
			int maxWidth = 0;
			int maxHeight = 0;
			if (getChildCount() == 1) {
				final View child = getChildAt(0);
				if (child.getVisibility() != GONE) {
					measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
					final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
					maxWidth = child.getMeasuredWidth() + getPaddingLeft() + getPaddingRight()
							+ lp.leftMargin + lp.rightMargin;
					maxHeight = child.getMeasuredHeight() + getPaddingTop() + getPaddingBottom()
							+ lp.topMargin + lp.bottomMargin;
				}
			}
			// Check against our minimum height and width
			maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
			maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());

			// Report our final dimensions.
			setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
					resolveSize(maxHeight, heightMeasureSpec));
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (getChildCount() > 1) {
			throw new IllegalStateException("SlidingUpPanel can only contain on child view");
		} else if (getChildCount() == 1) {
			final View child = getChildAt(0);
			if (child.getVisibility() != GONE) {
				final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
				final int width = child.getMeasuredWidth();
				final int height = child.getMeasuredHeight();
				final int left = getPaddingLeft() + lp.leftMargin;
				final int top = getPaddingTop() + lp.topMargin;
				child.layout(left, top, left + width, top + height);
				if (mIsOpen) {
					scrollTo(0, height);
					mIsOpen = false;
					openPanel();
				}
			}
		}
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new MarginLayoutParams(getContext(), attrs);
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new MarginLayoutParams(p);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof MarginLayoutParams;
	}

	// dragging
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// This method JUST determines whether we want to intercept the motion.
		// If we return true, onMotionEvent will be called and we do the actual
		// scrolling there.
		final int action = MotionEventCompat.getActionMasked(ev);

		// Always take care of the touch gesture being complete.
		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			// Release the drag.
			DEBUG_LOG("Intercept done!");
			endDrag();
			return false;
		}

		// Nothing more to do here if we have decided whether or not we are dragging.
		if (action != MotionEvent.ACTION_DOWN) {
			if (mIsBeingDragged) {
				DEBUG_LOG("Intercept returning true!");
				return true;
			}
			if (mIsUnableToDrag) {
				DEBUG_LOG("Intercept returning false!");
				return false;
			}
		}

		// Check whether the user has moved far enough from his original down touch.
		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			onTouchDown(ev, true);
			DEBUG_LOG("***Down at " + mLastMotionX + "," + mLastMotionY
					+ " mIsBeingDragged=" + mIsBeingDragged
					+ " mIsUnableToDrag=" + mIsUnableToDrag);
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			final int activePointerId = mActivePointerId;
			if (activePointerId == INVALID_POINTER) {
				// If we don't have a valid id, the touch down wasn't on content.
				break;
			}
			final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
			final float x = MotionEventCompat.getX(ev, pointerIndex);
			final float y = MotionEventCompat.getY(ev, pointerIndex);
			final float xDiff = Math.abs(x - mInitialMotionX);
			final float yDiff = Math.abs(y - mInitialMotionY);
			DEBUG_LOG("***Moved to " + x + "," + y + " diff=" + xDiff + "," + yDiff);
			onTouchMove(x, y, xDiff, yDiff, true);
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			onTouchPointerUp(ev);
			break;
		}

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		// The only time we want to intercept motion events is if we are in the drag mode.
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (getState() == STATE_OPENED) {
			// disable touch handle when in opened state.
			return false;
		}

		final int action = MotionEventCompat.getActionMasked(ev);

		if (action == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
			// Don't handle edge touches immediately -- they may actually belong to one of our descendants.
			return false;
		}

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			onTouchDown(ev, false);
			DEBUG_LOG("Down at " + mLastMotionX + "," + mLastMotionY
					+ " mIsBeingDragged=" + mIsBeingDragged
					+ " mIsUnableToDrag=" + mIsUnableToDrag);
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			final int activePointerId = mActivePointerId;
			if (activePointerId == INVALID_POINTER || mIsUnableToDrag) {
				// If we don't have a valid id, the touch down wasn't on content.
				break;
			}
			final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
			final float x = MotionEventCompat.getX(ev, pointerIndex);
			final float y = MotionEventCompat.getY(ev, pointerIndex);
			final float xDiff = Math.abs(x - mInitialMotionX);
			final float yDiff = Math.abs(y - mInitialMotionY);
			DEBUG_LOG("Moved to " + x + "," + y + " diff=" + xDiff + "," + yDiff);
			onTouchMove(x, y, xDiff, yDiff, false);
			break;
		}
		case MotionEvent.ACTION_UP: {
			if (mIsBeingDragged) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				final int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(
						velocityTracker, mActivePointerId);
				final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
				final float y = MotionEventCompat.getY(ev, pointerIndex);
				final int totalDelta = (int) (y - mInitialMotionY);
				boolean toOpen = determineToOpen(initialVelocity, totalDelta);
				startFling(toOpen, initialVelocity);
				endDrag();
			}
			DEBUG_LOG("Touch up!!!");
			break;
		}
		case MotionEvent.ACTION_CANCEL: {
			if (mIsBeingDragged) {
				startFling(isOpen(), 0);
				endDrag();
			}
			DEBUG_LOG("Touch cancel!!!");
			break;
		}
		case MotionEventCompat.ACTION_POINTER_DOWN: {
			final int pointerIndex = MotionEventCompat.getActionIndex(ev);
			final float x = MotionEventCompat.getX(ev, pointerIndex);
			final float y = MotionEventCompat.getY(ev, pointerIndex);
			mLastMotionX = x;
			mLastMotionY = y;
			mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			onTouchPointerUp(ev);
			break;
		}

		return true;
	}

	private void onTouchDown(MotionEvent ev, boolean intercept) {
		// Remember location of down touch.
		// ACTION_DOWN always refers to pointer index 0.
		mLastMotionX = mInitialMotionX = ev.getX();
		mLastMotionY = mInitialMotionY = ev.getY();
		mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
		if (intercept) {
			mIsUnableToDrag = false;
			mScroller.computeScrollOffset();
			if (getState() == STATE_FLING) {
				// Let the user 'catch' the pager as it animates.
				mScroller.abortAnimation();
				mIsBeingDragged = true;
				requestParentDisallowInterceptTouchEvent(true);
				setState(STATE_DRAGGING);
			} else {
				completeScroll(false);
				mIsBeingDragged = false;
			}
		} else {
			mScroller.abortAnimation();
		}
	}

	private void onTouchMove(float x, float y, float xDiff, float yDiff, boolean intercept) {
		if (!mIsBeingDragged) {
			if (yDiff > mTouchSlop && yDiff * 0.5f > xDiff) {
				DEBUG_LOG((intercept ? "***" : "") + "Starting drag!!!");
				mIsBeingDragged = true;
				requestParentDisallowInterceptTouchEvent(true);
				setState(STATE_DRAGGING);
				mLastMotionX = x > mInitialMotionX ? mInitialMotionX + mTouchSlop :
						mInitialMotionX - mTouchSlop;
				mLastMotionY = y > mInitialMotionY ? mInitialMotionY + mTouchSlop :
						mInitialMotionY - mTouchSlop;
			} else if (xDiff > mTouchSlop) {
				// The finger has moved enough in the horizontally
				// direction to be counted as a drag... abort
				// any attempt to drag vertical, to work correctly
				// with children that have scrolling containers.
				DEBUG_LOG((intercept ? "***" : "") + "Unable to drag!!!");
				mIsUnableToDrag = true;
			}
		}
		// Not else! Note that mIsBeingDragged can be set above.
		if (mIsBeingDragged) {
			// Scroll to follow the motion event
			if (performDrag(x, y)) {
				ViewCompat.postInvalidateOnAnimation(this);
			}
		}
	}

	private void onTouchPointerUp(MotionEvent ev) {
		final int pointerIndex = MotionEventCompat.getActionIndex(ev);
		final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionX = MotionEventCompat.getX(ev, newPointerIndex);
			mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
			mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}

	private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
		final ViewParent parent = getParent();
		if (parent != null) {
			parent.requestDisallowInterceptTouchEvent(disallowIntercept);
		}
	}

	private boolean performDrag(float x, float y) {
		boolean needsInvalidate = false;

		// final float deltaX = mLastMotionX - x;
		final float deltaY = mLastMotionY - y;
		mLastMotionX = x;
		mLastMotionY = y;

		float oldScrollY = getScrollY();
		float scrollY = oldScrollY + deltaY;
		final int height = getHeight();

		float topBound = height;
		float bottomBound = 0;

		if (scrollY > topBound) {
			scrollY = topBound;
		} else if (scrollY < bottomBound) {
			scrollY = bottomBound;
		}
		// Don't lose the rounded component
		mLastMotionY += scrollY - (int) scrollY;
		scrollTo(getScrollX(), (int) scrollY);
		onPanelScrolled(scrollY);

		return needsInvalidate;
	}

	private void endDrag() {
		mIsBeingDragged = false;
		mIsUnableToDrag = false;
		mActivePointerId = INVALID_POINTER;
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	private void enableLayers(boolean enable) {
		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final int layerType = enable ?
					ViewCompat.LAYER_TYPE_HARDWARE : ViewCompat.LAYER_TYPE_NONE;
			ViewCompat.setLayerType(getChildAt(i), layerType, null);
		}
	}

	private void setScrollingCacheEnabled(boolean enabled) {
		if (mScrollingCacheEnabled != enabled) {
			mScrollingCacheEnabled = enabled;
			if (USE_CACHE) {
				final int size = getChildCount();
				for (int i = 0; i < size; ++i) {
					final View child = getChildAt(i);
					if (child.getVisibility() != GONE) {
						child.setDrawingCacheEnabled(enabled);
					}
				}
			}
		}
	}

	// fling
	private boolean determineToOpen(int velocity, int deltaX) {
		final int height = getHeight();
		final int scrollY = getScrollY();
		boolean toOpen;
		if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
			toOpen = velocity < 0 ? true : false;
		} else {
			int deltaHeight;
			if (isOpen()) {
				deltaHeight = (int) (height * 0.7);
			} else {
				deltaHeight = (int) (height * 0.3);
			}
			toOpen = scrollY > deltaHeight ? true : false;
		}
		return toOpen;
	}

	private void startFling(boolean isOpen, int velocity) {
		mIsOpen = isOpen;
		if (isOpen) {
			final int height = getHeight();
			smoothScrollTo(0, height, velocity);
		} else {
			smoothScrollTo(0, 0, velocity);
		}
	}

	private void smoothScrollTo(int x, int y, int velocity) {
		if (getChildCount() == 0) {
			// Nothing to do.
			return;
		}
		final int sx = getScrollX();
		final int sy = getScrollY();
		final int dx = x - sx;
		final int dy = y - sy;
		final int height = getHeight();

		if ((dx == 0 && dy == 0) || height == 0) {
			completeScroll(false);
			setState(mIsOpen ? STATE_OPENED : STATE_CLOSED);
			return;
		}

		setState(STATE_FLING);

		final int halfHeight = height / 2;
		final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dy) / height);
		final float distance = halfHeight + halfHeight *
				distanceInfluenceForSnapDuration(distanceRatio);

		int duration = 0;
		velocity = Math.abs(velocity);
		velocity = Math.max(velocity, mMinimumVelocity);
		duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
		duration = Math.min(duration, MAX_SETTLE_DURATION);

		DEBUG_LOG("smoothScrollTo x" + x + ", y=" + y +
				", velocity=" + velocity +
				", distance=" + distance +
				", duration=" + duration);

		mScroller.startScroll(sx, sy, dx, dy, duration);
		ViewCompat.postInvalidateOnAnimation(this);
	}

	@Override
	public void computeScroll() {
		if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
			int oldX = getScrollX();
			int oldY = getScrollY();
			int x = mScroller.getCurrX();
			int y = mScroller.getCurrY();

			if (oldX != x || oldY != y) {
				scrollTo(x, y);
				onPanelScrolled(y);
			}

			// Keep on drawing until the animation has finished.
			ViewCompat.postInvalidateOnAnimation(this);
			return;
		}

		// Done with scroll, clean up state.
		completeScroll(true);
	}

	// We want the duration of the page snap animation to be influenced by the distance that
	// the screen has to travel, however, we don't want this duration to be effected in a
	// purely linear fashion. Instead, we use this method to moderate the effect that the distance
	// of travel has on the overall snap duration.
	private float distanceInfluenceForSnapDuration(float f) {
		f -= 0.5f; // center the values about 0.
		f *= 0.3f * Math.PI / 2.0f;
		return (float) Math.sin(f);
	}

	private void completeScroll(boolean postEvents) {
		if (getState() == STATE_FLING) {
			// Done with scroll, no longer want to cache view drawing.
			mScroller.abortAnimation();
			int oldX = getScrollX();
			int oldY = getScrollY();
			int x = mScroller.getCurrX();
			int y = mScroller.getCurrY();
			if (oldX != x || oldY != y) {
				scrollTo(x, y);
			}
			if (postEvents) {
				ViewCompat.postOnAnimation(this, mEndScrollRunnable);
			} else {
				mEndScrollRunnable.run();
			}
		}
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		mIsOpen = savedState.isOpen;
		requestLayout();
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.isOpen = mIsOpen;
		return savedState;
	}

	static class SavedState extends BaseSavedState {
		boolean isOpen;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			isOpen = in.readInt() == 1;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(isOpen ? 1 : 0);
		}

		@SuppressWarnings("UnusedDeclaration")
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

}
