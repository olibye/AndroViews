package org.jared.commons.ui;

/**
     * Copyright 2010 Eric Taix (eric.taix@gmail.com) Licensed under the Apache License, Version 2.0 (the "License"); you
     * may not use this file except in compliance with the License. You may obtain a copy of the License at
     * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
     * either express or implied. See the License for the specific language governing permissions and limitations under the
     * License.
     */

    import android.content.Context;
import android.graphics.*;
    import android.os.Parcel;
    import android.os.Parcelable;
    import android.util.AttributeSet;
    import android.util.Log;
    import android.view.MotionEvent;
    import android.view.VelocityTracker;
    import android.view.View;
    import android.view.ViewConfiguration;
    import android.view.ViewGroup;
    import android.view.ViewParent;
    import android.view.animation.Interpolator;
import android.widget.Scroller;

    /**
     * The workspace is a wide area with a infinite number of screens. Each screen contains a view. A workspace is meant to
     * be used with a fixed width only.<br/>
     * <br/>
     * This code has been done by using com.android.launcher.Workspace.java
     */
    public class WorkspaceView extends ViewGroup {

        private static final int INVALID_POINTER = -1;

        private int mActivePointerId = INVALID_POINTER;
        private static final int INVALID_SCREEN = -1;

        // The velocity at which a fling gesture will cause us to snap to the next screen
        private static final int SNAP_VELOCITY = 500;

        // the default screen index
        private int defaultScreen;
        // The current screen index
        private int currentScreen;
        // The next screen index
        private int nextScreen = INVALID_SCREEN;
        // Wallpaper properties
        private Bitmap wallpaper;
        private Paint paint;
        private int wallpaperWidth;
        private int wallpaperHeight;
        private float wallpaperOffset;
        private boolean wallpaperLoaded;
        private boolean firstWallpaperLayout = true;
        private static final int TAB_INDICATOR_HEIGHT_PCT = 2;
        private RectF selectedTab;


        // The scroller which scroll each view
        private Scroller scroller;
        // A tracker which to calculate the velocity of a mouvement
        private VelocityTracker mVelocityTracker;

        // Tha last known values of X and Y
        private float lastMotionX;
        private float lastMotionY;

        private final static int TOUCH_STATE_REST = 0;
        private final static int TOUCH_STATE_SCROLLING = 1;

        // The current touch state
        private int touchState = TOUCH_STATE_REST;
        // The minimal distance of a touch slop
        private int touchSlop;

        // An internal flag to reset long press when user is scrolling
        private boolean allowLongPress;
        // A flag to know if touch event have to be ignored. Used also in internal
        private boolean locked;

        private WorkspaceOvershootInterpolator mScrollInterpolator;

        private int mMaximumVelocity;

        private Paint selectedTabPaint;
        private Canvas canvas;

        private RectF bar;

        private Paint tabIndicatorBackgroundPaint;

        private static class WorkspaceOvershootInterpolator implements Interpolator {
            private static final float DEFAULT_TENSION = 1.3f;
            private float mTension;

            public WorkspaceOvershootInterpolator() {
                mTension = DEFAULT_TENSION;
            }

            public void setDistance(int distance) {
                mTension = distance > 0 ? DEFAULT_TENSION / distance : DEFAULT_TENSION;
            }

            public void disableSettle() {
                mTension = 0.f;
            }

            public float getInterpolation(float t) {
                // _o(t) = t * t * ((tension + 1) * t + tension)
                // o(t) = _o(t - 1) + 1
                t -= 1.0f;
                return t * t * ((mTension + 1) * t + mTension) + 1.0f;
            }
        }

        /**
         * Used to inflate the Workspace from XML.
         * 
         * @param context The application's context.
         * @param attrs The attribtues set containing the Workspace's customization values.
         */
        public WorkspaceView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        /**
         * Used to inflate the Workspace from XML.
         * 
         * @param context The application's context.
         * @param attrs The attribtues set containing the Workspace's customization values.
         * @param defStyle Unused.
         */
        public WorkspaceView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            defaultScreen = 0;
            initWorkspace();
        }

        /**
         * Initializes various states for this workspace.
         */
        private void initWorkspace() {
            mScrollInterpolator = new WorkspaceOvershootInterpolator();
            scroller = new Scroller(getContext(),mScrollInterpolator);
            currentScreen = defaultScreen;

            paint = new Paint();
            paint.setDither(false);

            // Does this do anything for me?
            final ViewConfiguration configuration = ViewConfiguration.get(getContext());
            touchSlop = configuration.getScaledTouchSlop();
            mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

            selectedTabPaint = new Paint();
            selectedTabPaint.setColor(Color.RED);
            selectedTabPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            tabIndicatorBackgroundPaint = new Paint();
            tabIndicatorBackgroundPaint.setColor(Color.GRAY);
            tabIndicatorBackgroundPaint.setStyle(Paint.Style.FILL);
        }

        /**
         * Set a new distance that a touch can wander before we think the user is scrolling in pixels slop<br/>
         * 
         * @param touchSlopP
         */
        public void setTouchSlop(int touchSlopP) {
            touchSlop = touchSlopP;
        }

        /**
         * Set the background's wallpaper.
         */
        public void loadWallpaper(Bitmap bitmap) {
            wallpaper = bitmap;
            wallpaperLoaded = true;
            requestLayout();
            invalidate();
        }

        boolean isDefaultScreenShowing() {
            return currentScreen == defaultScreen;
        }

        /**
         * Returns the index of the currently displayed screen.
         * 
         * @return The index of the currently displayed screen.
         */
        int getCurrentScreen() {
            return currentScreen;
        }

        /**
         * Sets the current screen.
         * 
         * @param currentScreen
         */
        public void setCurrentScreen(int theCurrentScreen) {

            if (!scroller.isFinished()) scroller.abortAnimation();
            currentScreen = Math.max(0, Math.min(theCurrentScreen, getChildCount()));
            scrollTo(currentScreen * getWidth(), 0);
            Log.d("workspace", "setCurrentScreen: width is " + getWidth());
            invalidate();
        }

        /**
         * Shows the default screen (defined by the firstScreen attribute in XML.)
         */
        void showDefaultScreen() {
            setCurrentScreen(defaultScreen);
        }

        /**
         * Registers the specified listener on each screen contained in this workspace.
         * 
         * @param l The listener used to respond to long clicks.
         */
        @Override
        public void setOnLongClickListener(OnLongClickListener l) {
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).setOnLongClickListener(l);
            }
        }


        @Override
        public void computeScroll() {
            if (scroller.computeScrollOffset()) {
                scrollTo(scroller.getCurrX(), scroller.getCurrY());
                postInvalidate();
            } else if (nextScreen != INVALID_SCREEN) {
                currentScreen = Math.max(0, Math.min(nextScreen, getChildCount() - 1));
                nextScreen = INVALID_SCREEN;
            }
        }

        /**
         * ViewGroup.dispatchDraw() supports many features we don't need: clip to padding, layout animation, animation
         * listener, disappearing children, etc. The following implementation attempts to fast-track the drawing dispatch by
         * drawing only what we know needs to be drawn.
         */
        @Override
        protected void dispatchDraw(Canvas canvas) {
            // First draw the wallpaper if needed

            if (wallpaper != null) {
                float x = getScrollX() * wallpaperOffset;
                if (x + wallpaperWidth < getRight() - getLeft()) {
                    x = getRight() - getLeft() - wallpaperWidth;
                }
                canvas.drawBitmap(wallpaper, x, (getBottom() - getTop() - wallpaperHeight) / 2, paint);
            }

            // Determine if we need to draw every child or only the current screen
            boolean fastDraw = touchState != TOUCH_STATE_SCROLLING && nextScreen == INVALID_SCREEN;
            // If we are not scrolling or flinging, draw only the current screen
            if (fastDraw) {
                View v = getChildAt(currentScreen);
                drawChild(canvas, v, getDrawingTime());
            }
            else {
                final long drawingTime = getDrawingTime();
                // If we are flinging, draw only the current screen and the target screen
                if (nextScreen >= 0 && nextScreen < getChildCount() && Math.abs(currentScreen - nextScreen) == 1) {
                    drawChild(canvas, getChildAt(currentScreen), drawingTime);
                    drawChild(canvas, getChildAt(nextScreen), drawingTime);
                }
                else {
                    // If we are scrolling, draw all of our children
                    final int count = getChildCount();
                    for (int i = 0; i < count; i++) {
                        drawChild(canvas, getChildAt(i), drawingTime);
                    }
                }
            }
            updateTabIndicator();
            canvas.drawBitmap(bitmap, getScrollX(), getMeasuredHeight()*(100-TAB_INDICATOR_HEIGHT_PCT)/100, paint);

        }


        /**
         * Measure the workspace AND also children
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            final int width = MeasureSpec.getSize(widthMeasureSpec);
            final int height = MeasureSpec.getSize(heightMeasureSpec);
    //      Log.d("workspace","Height is " + height);
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            if (widthMode != MeasureSpec.EXACTLY) {
                throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
            }

            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (heightMode != MeasureSpec.EXACTLY) {
                throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
            }

            // The children are given the same width and height as the workspace
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                int adjustedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height*(100-TAB_INDICATOR_HEIGHT_PCT)/100, heightMode);
                getChildAt(i).measure(widthMeasureSpec,adjustedHeightMeasureSpec);

            }

            // Compute wallpaper
            if (wallpaperLoaded) {
                wallpaperLoaded = false;
                wallpaper = centerToFit(wallpaper, width, height, getContext());
                wallpaperWidth = wallpaper.getWidth();
                wallpaperHeight = wallpaper.getHeight();
            }
            wallpaperOffset = wallpaperWidth > width ? (count * width - wallpaperWidth) / ((count - 1) * (float) width) : 1.0f;
            if (firstWallpaperLayout) {
                scrollTo(currentScreen * width, 0);
                firstWallpaperLayout = false;
            }

    //      Log.d("workspace","Top is "+getTop()+", bottom is "+getBottom()+", left is "+getLeft()+", right is "+getRight());

            updateTabIndicator();
            invalidate();
        }

        Bitmap bitmap;

        private OnLoadListener load;


    private int lastEvHashCode;

        private void updateTabIndicator(){
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();

            //For drawing in its own bitmap:
            bar = new RectF(0, 0, width, (TAB_INDICATOR_HEIGHT_PCT*height/100));

            int startPos = getScrollX()/(getChildCount());
            selectedTab = new RectF(startPos, 0, startPos+width/getChildCount(), (TAB_INDICATOR_HEIGHT_PCT*height/100));

            bitmap = Bitmap.createBitmap(width, (TAB_INDICATOR_HEIGHT_PCT*height/100), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            canvas.drawRoundRect(bar,0,0, tabIndicatorBackgroundPaint);
            canvas.drawRoundRect(selectedTab, 5,5, selectedTabPaint);
        }

        /**
         * Overrided method to layout child
         */
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int childLeft = 0;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() != View.GONE) {
                    final int childWidth = child.getMeasuredWidth();
                    child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                    childLeft += childWidth;
                }
            }
            if (load!=null) {
            	load.onLoad();
            }
        }

        @Override
        public boolean dispatchUnhandledMove(View focused, int direction) {
            if (direction == View.FOCUS_LEFT) {
                if (getCurrentScreen() > 0) {
                    scrollToScreen(getCurrentScreen() - 1);
                    return true;
                }
            }
            else if (direction == View.FOCUS_RIGHT) {
                if (getCurrentScreen() < getChildCount() - 1) {
                    scrollToScreen(getCurrentScreen() + 1);
                    return true;
                }
            }
            return super.dispatchUnhandledMove(focused, direction);
        }

        /**
         * This method JUST determines whether we want to intercept the motion. If we return true, onTouchEvent will be called
         * and we do the actual scrolling there.
         */
        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            Log.d("workspace","Intercepted a touch event");
            if (locked) {
                return true;
            }

            /*
             * Shortcut the most recurring case: the user is in the dragging state and he is moving his finger. We want to
             * intercept this motion.
             */
            final int action = ev.getAction();
            if ((action == MotionEvent.ACTION_MOVE) && (touchState != TOUCH_STATE_REST)) {
                return true;
            }

            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(ev);

    //      switch (action & MotionEvent.ACTION_MASK) {
            switch (action) {
            case MotionEvent.ACTION_MOVE:

    //          Log.d("workspace","Intercepted a move event");
                /*
                 * Locally do absolute value. mLastMotionX is set to the y value of the down event.
                 */
                handleInterceptMove(ev);
                break;

            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                final float x1 = ev.getX();
                final float y1 = ev.getY();
                lastMotionX = x1;
                lastMotionY = y1;
                allowLongPress = true;
                mActivePointerId = ev.getPointerId(0);

                /*
                 * If being flinged and user touches the screen, initiate drag; otherwise don't. mScroller.isFinished should be
                 * false when being flinged.
                 */
                touchState = scroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER;
                allowLongPress = false;

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                touchState = TOUCH_STATE_REST;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            }

            /*
             * The only time we want to intercept motion events is if we are in the drag mode.
             */
            return touchState != TOUCH_STATE_REST;
        }

        private void handleInterceptMove(MotionEvent ev) {
            final int pointerIndex = ev.findPointerIndex(mActivePointerId);
            final float x = ev.getX(pointerIndex);
            final float y = ev.getY(pointerIndex);
            final int xDiff = (int) Math.abs(x - lastMotionX);
            final int yDiff = (int) Math.abs(y - lastMotionY);
            boolean xMoved = xDiff > touchSlop;
            boolean yMoved = yDiff > touchSlop;

            if (xMoved || yMoved) {
                //Log.d("workspace","Detected move.  Checking to scroll.");
                if (xMoved && !yMoved) {
                    //Log.d("workspace","Detected X move.  Scrolling.");
                    // Scroll if the user moved far enough along the X axis
                    touchState = TOUCH_STATE_SCROLLING;
                    lastMotionX = x;
                }
                // Either way, cancel any pending longpress
                if (allowLongPress) {
                    allowLongPress = false;
                    // Try canceling the long press. It could also have been scheduled
                    // by a distant descendant, so use the mAllowLongPress flag to block
                    // everything
                    final View currentView = getChildAt(currentScreen);
                    currentView.cancelLongPress();
                }
            }
        }

        private void onSecondaryPointerUp(MotionEvent ev) {
            final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >>
                    MotionEvent.ACTION_POINTER_ID_SHIFT;
            final int pointerId = ev.getPointerId(pointerIndex);
            if (pointerId == mActivePointerId) {
                // This was our active pointer going up. Choose a new
                // active pointer and adjust accordingly.
                // TODO: Make this decision more intelligent.
                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                lastMotionX = ev.getX(newPointerIndex);
                lastMotionY = ev.getY(newPointerIndex);
                mActivePointerId = ev.getPointerId(newPointerIndex);
                if (mVelocityTracker != null) {
                    mVelocityTracker.clear();
                }
            }
        }

        /**
         * Track the touch event
         */
        @Override
        public boolean onTouchEvent(MotionEvent ev) {
    //      Log.d("workspace","caught a touch event");
            if (locked) {
                return true;
            }
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(ev);

            final int action = ev.getAction();
            final float x = ev.getX();

            switch (action) {
            case MotionEvent.ACTION_DOWN:

                //We can still get here even if we returned false from the intercept function.
                //That's the only way we can get a TOUCH_STATE_REST (0) here.
                //That means that our child hasn't handled the event, so we need to 
    //          Log.d("workspace","caught a down touch event and touchstate =" + touchState);

                if(touchState != TOUCH_STATE_REST){
                    /*
                     * If being flinged and user touches, stop the fling. isFinished will be false if being flinged.
                     */
                    if (!scroller.isFinished()) {
                        scroller.abortAnimation();
                    }

                    // Remember where the motion event started
                    lastMotionX = x;
                    mActivePointerId = ev.getPointerId(0);
                } 
                break;
            case MotionEvent.ACTION_MOVE:

                if (touchState == TOUCH_STATE_SCROLLING) {
                    handleScrollMove(ev);
                } else {
    //              Log.d("workspace","caught a move touch event but not scrolling");
                    //NOTE:  We will never hit this case in Android 2.2.  This is to fix a 2.1 bug.
                    //We need to do the work of interceptTouchEvent here because we don't intercept the move
                    //on children who don't scroll.

                    Log.d("workspace","handling move from onTouch");

                    if(onInterceptTouchEvent(ev) && touchState == TOUCH_STATE_SCROLLING){
                        handleScrollMove(ev);
                    }

                }

                break;
            case MotionEvent.ACTION_UP:
    //          Log.d("workspace","caught an up touch event");
                if (touchState == TOUCH_STATE_SCROLLING) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity();

                    if (velocityX > SNAP_VELOCITY && currentScreen > 0) {
                        // Fling hard enough to move left
                        scrollToScreen(currentScreen - 1);
                    }
                    else if (velocityX < -SNAP_VELOCITY && currentScreen < getChildCount() - 1) {
                        // Fling hard enough to move right
                        scrollToScreen(currentScreen + 1);
                    }
                    else {
                        snapToDestination();
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                touchState = TOUCH_STATE_REST;
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.d("workspace","caught a cancel touch event");
                touchState = TOUCH_STATE_REST;
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d("workspace","caught a pointer up touch event");
                onSecondaryPointerUp(ev);
                break;
            }

            return true;
        }

        private void handleScrollMove(MotionEvent ev){
            // Scroll to follow the motion event
            final int pointerIndex = ev.findPointerIndex(mActivePointerId);
            final float x1 = ev.getX(pointerIndex);
            final int deltaX = (int) (lastMotionX - x1);
            lastMotionX = x1;

            if (deltaX < 0) {
                if (getScrollX() > 0) {
                    //Scrollby invalidates automatically
                    scrollBy(Math.max(-getScrollX(), deltaX), 0);
                }
            }
            else if (deltaX > 0) {
                final int availableToScroll = getChildAt(getChildCount() - 1).getRight() - getScrollX() - getWidth();
                if (availableToScroll > 0) {
                    //Scrollby invalidates automatically
                    scrollBy(Math.min(availableToScroll, deltaX), 0);
                }
            } else {
                awakenScrollBars();
            }
        }

        /**
         * Scroll to the appropriated screen depending of the current position
         */
        private void snapToDestination() {
            final int screenWidth = getWidth();
            final int whichScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
            Log.d("workspace", "snapToDestination");
            scrollToScreen(whichScreen);
        }

        /**
         * Scroll to a specific screen
         * 
         * @param whichScreen
         */
        public void scrollToScreen(int whichScreen) {
            scrollToScreen(whichScreen, false);
        }

        private void scrollToScreen(int whichScreen, boolean immediate){
            Log.d("workspace", "snapToScreen=" + whichScreen);

            boolean changingScreens = whichScreen != currentScreen;

            nextScreen = whichScreen;

            View focusedChild = getFocusedChild();
            if (focusedChild != null && changingScreens && focusedChild == getChildAt(currentScreen)) {
                focusedChild.clearFocus();
            }

            final int newX = whichScreen * getWidth();
            final int delta = newX - getScrollX();
            Log.d("workspace", "newX=" + newX + " scrollX=" + getScrollX() + " delta=" + delta);
            scroller.startScroll(getScrollX(), 0, delta, 0, immediate ? 0 : Math.abs(delta) * 2);
            invalidate();
        }

        public void scrollToScreenImmediate(int whichScreen){
            scrollToScreen(whichScreen, true);
        }

        /**
         * Return the parceable instance to be saved
         */
        @Override
        protected Parcelable onSaveInstanceState() {
            final SavedState state = new SavedState(super.onSaveInstanceState());
            state.currentScreen = currentScreen;
            return state;
        }

        /**
         * Restore the previous saved current screen
         */
        @Override
        protected void onRestoreInstanceState(Parcelable state) {
            SavedState savedState = (SavedState) state;
            super.onRestoreInstanceState(savedState.getSuperState());
            if (savedState.currentScreen != -1) {
                currentScreen = savedState.currentScreen;
            }
        }

        /**
         * Scroll to the left right screen
         */
        public void scrollLeft() {
            if (nextScreen == INVALID_SCREEN && currentScreen > 0 && scroller.isFinished()) {
                scrollToScreen(currentScreen - 1);
            }
        }

        /**
         * Scroll to the next right screen
         */
        public void scrollRight() {
            if (nextScreen == INVALID_SCREEN && currentScreen < getChildCount() - 1 && scroller.isFinished()) {
                scrollToScreen(currentScreen + 1);
            }
        }

        /**
         * Return the screen's index where a view has been added to.
         * 
         * @param v
         * @return
         */
        public int getScreenForView(View v) {
            int result = -1;
            if (v != null) {
                ViewParent vp = v.getParent();
                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    if (vp == getChildAt(i)) {
                        return i;
                    }
                }
            }
            return result;
        }

        /**
         * Return a view instance according to the tag parameter or null if the view could not be found
         * 
         * @param tag
         * @return
         */
        public View getViewForTag(Object tag) {
            int screenCount = getChildCount();
            for (int screen = 0; screen < screenCount; screen++) {
                View child = getChildAt(screen);
                if (child.getTag() == tag) {
                    return child;
                }
            }
            return null;
        }

        /**
         * Unlocks the SlidingDrawer so that touch events are processed.
         * 
         * @see #lock()
         */
        public void unlock() {
            locked = false;
        }

        /**
         * Locks the SlidingDrawer so that touch events are ignores.
         * 
         * @see #unlock()
         */
        public void lock() {
            locked = true;
        }

        /**
         * @return True is long presses are still allowed for the current touch
         */
        public boolean allowLongPress() {
            return allowLongPress;
        }

        /**
         * Move to the default screen
         */
        public void moveToDefaultScreen() {
            scrollToScreen(defaultScreen);
            getChildAt(defaultScreen).requestFocus();
        }

        // ========================= INNER CLASSES ==============================

        /**
         * A SavedState which save and load the current screen
         */
        public static class SavedState extends BaseSavedState {
            int currentScreen = -1;

            /**
             * Internal constructor
             * 
             * @param superState
             */
            SavedState(Parcelable superState) {
                super(superState);
            }

            /**
             * Private constructor
             * 
             * @param in
             */
            private SavedState(Parcel in) {
                super(in);
                currentScreen = in.readInt();
            }

            /**
             * Save the current screen
             */
            @Override
            public void writeToParcel(Parcel out, int flags) {
                super.writeToParcel(out, flags);
                out.writeInt(currentScreen);
            }

            /**
             * Return a Parcelable creator
             */
            public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
                public SavedState createFromParcel(Parcel in) {
                    return new SavedState(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        }

        //Added for "flipper" compatibility
        public int getDisplayedChild(){
            return getCurrentScreen();
        }

        public void setDisplayedChild(int i){
            //    setCurrentScreen(i);
            scrollToScreen(i);
            getChildAt(i).requestFocus();
        }

        public void setOnLoadListener(OnLoadListener load){
            this.load = load;
        }

        public void flipLeft(){
            scrollLeft();
        }

        public void flipRight(){
            scrollRight();
        }

        // ======================== UTILITIES METHODS ==========================

        /**
         * Return a centered Bitmap
         * 
         * @param bitmap
         * @param width
         * @param height
         * @param context
         * @return
         */
        static Bitmap centerToFit(Bitmap bitmap, int width, int height, Context context) {
            final int bitmapWidth = bitmap.getWidth();
            final int bitmapHeight = bitmap.getHeight();

            if (bitmapWidth < width || bitmapHeight < height) {
                // Normally should get the window_background color of the context
                int color = Integer.valueOf("FF191919", 16);
                Bitmap centered = Bitmap.createBitmap(bitmapWidth < width ? width : bitmapWidth, bitmapHeight < height ? height
                        : bitmapHeight, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(centered);
                canvas.drawColor(color);
                canvas.drawBitmap(bitmap, (width - bitmapWidth) / 2.0f, (height - bitmapHeight) / 2.0f, null);
                bitmap = centered;
            }
            return bitmap;
        }

    }