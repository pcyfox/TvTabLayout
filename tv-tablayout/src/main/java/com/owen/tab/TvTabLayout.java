package com.owen.tab;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.Pools;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.TextViewCompat;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import static android.support.v4.view.ViewPager.SCROLL_STATE_DRAGGING;
import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;
import static android.support.v4.view.ViewPager.SCROLL_STATE_SETTLING;

/**
 * TabLayout provides a horizontal layout to display tabs.
 *
 * <p>Population of the tabs to display is
 * done through {@link Tab} instances. You create tabs via {@link #newTab()}. From there you can
 * change the tab's label or icon via {@link Tab#setText(int)} and {@link Tab#setIcon(int)}
 * respectively. To display the tab, you need to add it to the layout via one of the
 * {@link #addTab(Tab)} methods. For example:
 * <pre>
 * TabLayout tabLayout = ...;
 * tabLayout.addTab(tabLayout.newTab().setText("Tab 1"));
 * tabLayout.addTab(tabLayout.newTab().setText("Tab 2"));
 * tabLayout.addTab(tabLayout.newTab().setText("Tab 3"));
 * </pre>
 * You should set a listener via {@link #setOnTabSelectedListener(OnTabSelectedListener)} to be
 * notified when any tab's selection state has been changed.
 *
 * <p>You can also add items to TabLayout in your layout through the use of {@link TabItem}.
 * An example usage is like so:</p>
 *
 * <pre>
 * &lt;widget.TabLayout
 *         android:layout_height=&quot;wrap_content&quot;
 *         android:layout_width=&quot;match_parent&quot;&gt;
 *
 *     &lt;widget.TabItem
 *             android:text=&quot;@string/tab_text&quot;/&gt;
 *
 *     &lt;widget.TabItem
 *             android:icon=&quot;@drawable/ic_android&quot;/&gt;
 *
 * &lt;/widget.TabLayout&gt;
 * </pre>
 *
 * <h3>ViewPager integration</h3>
 * <p>
 * If you're using a {@link ViewPager} together
 * with this layout, you can call {@link #setupWithViewPager(ViewPager)} to link the two together.
 * This layout will be automatically populated from the {@link PagerAdapter}'s page titles.</p>
 *
 * <p>
 * This view also supports being used as part of a ViewPager's decor, and can be added
 * directly to the ViewPager in a layout resource file like so:</p>
 *
 * <pre>
 * &lt;android.support.v4.view.ViewPager
 *     android:layout_width=&quot;match_parent&quot;
 *     android:layout_height=&quot;match_parent&quot;&gt;
 *
 *     &lt;widget.TabLayout
 *         android:layout_width=&quot;match_parent&quot;
 *         android:layout_height=&quot;wrap_content&quot;
 *         android:layout_gravity=&quot;top&quot; /&gt;
 *
 * &lt;/android.support.v4.view.ViewPager&gt;
 * </pre>
 *
 * @see <a href="http://www.google.com/design/spec/components/tabs.html">Tabs</a>
 *
 * @attr ref R.styleable#TabLayout_tabPadding
 * @attr ref R.styleable#TabLayout_tabPaddingStart
 * @attr ref R.styleable#TabLayout_tabPaddingTop
 * @attr ref R.styleable#TabLayout_tabPaddingEnd
 * @attr ref R.styleable#TabLayout_tabPaddingBottom
 * @attr ref R.styleable#TabLayout_tabContentStart
 * @attr ref R.styleable#TabLayout_tabBackground
 * @attr ref R.styleable#TabLayout_tabMinWidth
 * @attr ref R.styleable#TabLayout_tabMaxWidth
 * @attr ref R.styleable#TabLayout_tabTextAppearance
 */
public class TvTabLayout extends HorizontalScrollView {

    private static final int DEFAULT_HEIGHT_WITH_TEXT_ICON = 72; // dps
    private static final int DEFAULT_GAP_TEXT_ICON = 8; // dps
    private static final int INVALID_WIDTH = -1;
    private static final int DEFAULT_HEIGHT = 48; // dps
    private static final int TAB_MIN_WIDTH_MARGIN = 56; //dps
    private static final int FIXED_WRAP_GUTTER_MIN = 16; //dps
    private static final int MOTION_NON_ADJACENT_OFFSET = 24;

    private static final int ANIMATION_DURATION = 300;
    
    private boolean mTabTextSelectedCentered;
    private float mTabTextSelectedScaleValue;

    private static final Pools.Pool<Tab> sTabPool = new Pools.SynchronizedPool<>(16);

    /**
     * Scrollable tabs display a subset of tabs at any given moment, and can contain longer tab
     * labels and a larger number of tabs. They are best used for browsing contexts in touch
     * interfaces when users don’t need to directly compare the tab labels.
     *
     * @see #setTabScrollMode(int)
     * @see #getTabScrollMode()
     */
    public static final int MODE_SCROLLABLE = 0;

    /**
     * Fixed tabs display all tabs concurrently and are best used with content that benefits from
     * quick pivots between tabs. The maximum number of tabs is limited by the view’s width.
     * Fixed tabs have equal width, based on the widest tab label.
     *
     * @see #setTabScrollMode(int)
     * @see #getTabScrollMode()
     */
    public static final int MODE_FIXED = 1;

    /**
     * @hide
     */
    @IntDef(value = {MODE_SCROLLABLE, MODE_FIXED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {}

    /**
     * Gravity used to fill the {@link TvTabLayout} as much as possible. This option only takes effect
     * when used with {@link #MODE_FIXED}.
     *
     * @see #setTabGravity(int)
     * @see #getTabGravity()
     */
    public static final int GRAVITY_FILL = 3;

    public static final int GRAVITY_TOP = 0;

    public static final int GRAVITY_CENTER = 1;

    public static final int GRAVITY_BOTTOM = 2;

    /**
     * @hide
     */
    @IntDef(flag = true, value = {GRAVITY_TOP, GRAVITY_CENTER, GRAVITY_BOTTOM, GRAVITY_FILL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TabGravity {}

    /**
     * Callback interface invoked when a tab's selection state changes.
     */
    public interface OnTabSelectedListener {

        /**
         * Called when a tab enters the selected state.
         *
         * @param tab The tab that was selected
         */
        public void onTabSelected(Tab tab);

        /**
         * Called when a tab exits the selected state.
         *
         * @param tab The tab that was unselected
         */
        public void onTabUnselected(Tab tab);

        /**
         * Called when a tab that is already selected is chosen again by the user. Some applications
         * may use this action to return to the top level of a category.
         *
         * @param tab The tab that was reselected.
         */
        public void onTabReselected(Tab tab);
    }

    private final ArrayList<Tab> mTabs = new ArrayList<>();
    private Tab mSelectedTab;

    protected final SlidingTabStrip mTabStrip;

    private int mTabPaddingStart;
    private int mTabPaddingTop;
    private int mTabPaddingEnd;
    private int mTabPaddingBottom;

    private ColorStateList mTabTextColors;
    private float mTabTextSize;
    private float mTabTextMultiLineSize;

    private final int mTabBackgroundResId;

    private int mTabMaxWidth = Integer.MAX_VALUE / 2;
    private final int mRequestedTabMinWidth;
    private final int mRequestedTabMaxWidth;
    private final int mScrollableTabMinWidth;

    private int mContentInsetStart;
    private int mContentInsetBottom;

    private int mScrollMode;
    private int mIndicatorGravity;
    private int mTabGravity;

    private OnTabSelectedListener mSelectedListener;
    private final ArrayList<OnTabSelectedListener> mSelectedListeners = new ArrayList<>();
    private OnTabSelectedListener mCurrentVpSelectedListener;

    private ValueAnimatorCompat mScrollAnimator;

    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    private DataSetObserver mPagerAdapterObserver;
    private TabLayoutOnPageChangeListener mPageChangeListener;
    private AdapterChangeListener mAdapterChangeListener;
    private boolean mSetupViewPagerImplicitly;

    // Pool we use as a simple RecyclerBin
    private final Pools.Pool<TabView> mTabViewPool = new Pools.SimplePool<>(12);

    public TvTabLayout(Context context) {
        this(context, null);
    }

    public TvTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TvTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);

        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setWillNotDraw(true);
        setClickable(false);
        setFocusable(true);
        setFocusableInTouchMode(true);

        // Add the TabStrip
        mTabStrip = new SlidingTabStrip(context);
        super.addView(mTabStrip, 0, new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TvTabLayout, defStyleAttr, 0);

        Drawable mStateListDrawable = a.getDrawable(R.styleable.TvTabLayout_tvTabIndicatorResId);
        if(null != mStateListDrawable) {
            mStateListDrawable.setCallback(this); //设置回调，当改变状态时，会回掉该View进行invalidate()刷新操作.
            mTabStrip.setIndicatorDrawable(mStateListDrawable);
        }

        mIndicatorGravity = a.getInt(R.styleable.TvTabLayout_tvTabIndicatorGravity, GRAVITY_BOTTOM);
        mTabStrip.setCircleDotRadius(a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabCircleDotRadius, 0));
        mTabStrip.setCircleDotColor(a.getColor(R.styleable.TvTabLayout_tvTabCircleDotColor, 0));
        mTabStrip.setIndicatorWidth(a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabIndicatorWidth, 0));
        mTabStrip.setIndicatorHeight(
                a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabIndicatorHeight, 0));
        mTabStrip.setIndicatorBackgroundColor(a.getColor(R.styleable.TvTabLayout_tvTabIndicatorBackgroundColor, 0));
        mTabStrip.setIndicatorBackgroundHeight(a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabIndicatorBackgroundHeight, 0));

        mTabPaddingStart = mTabPaddingTop = mTabPaddingEnd = mTabPaddingBottom = a
                .getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabPadding, 0);
        mTabPaddingStart = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabPaddingStart,
                mTabPaddingStart);
        mTabPaddingTop = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabPaddingTop,
                mTabPaddingTop);
        mTabPaddingEnd = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabPaddingEnd,
                mTabPaddingEnd);
        mTabPaddingBottom = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabPaddingBottom,
                mTabPaddingBottom);

        mTabTextSize = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabTexSize,
                getResources().getDimensionPixelOffset(R.dimen.tvTabLayout_tab_text_size));
        mTabTextColors = a.getColorStateList(R.styleable.TvTabLayout_tvTabTextColor);

        mRequestedTabMinWidth = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabMinWidth,
                INVALID_WIDTH);
        mRequestedTabMaxWidth = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabMaxWidth,
                INVALID_WIDTH);
        mTabBackgroundResId = a.getResourceId(R.styleable.TvTabLayout_tvTabBackground, 0);
        mContentInsetStart = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabContentStart, 0);
        mContentInsetBottom = a.getDimensionPixelOffset(R.styleable.TvTabLayout_tvTabContentBottom, 0);
        mScrollMode = a.getInt(R.styleable.TvTabLayout_tvTabScrollMode, MODE_FIXED);
        mTabGravity = a.getInt(R.styleable.TvTabLayout_tvTabGravity, GRAVITY_CENTER);
    
        mTabTextSelectedCentered = a.getBoolean(R.styleable.TvTabLayout_tvTabTextSelectedCentered, false);
        mTabTextSelectedScaleValue = a.getFloat(R.styleable.TvTabLayout_tvTabTextSelectedScale, 0);
        
        a.recycle();

        // TODO add attr for these
        final Resources res = getResources();
        mTabTextMultiLineSize = res.getDimensionPixelOffset(R.dimen.tvTabLayout_tab_text_size_2line);
        mScrollableTabMinWidth = res.getDimensionPixelOffset(R.dimen.tvTabLayout_tab_scrollable_min_width);

        // Now apply the tab mode and gravity
        applyModeAndGravity();
    }
    
    public void setTabTextSelectedCentered(boolean tabTextSelectedCentered) {
        mTabTextSelectedCentered = tabTextSelectedCentered;
    }
    
    public void setTabTextSelectedScaleValue(float tabTextSelectedScaleValue) {
        mTabTextSelectedScaleValue = tabTextSelectedScaleValue;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if(null != mSelectedTab) {
            final View selectedView = mSelectedTab.getView();
            if(null != selectedView) {
                selectedView.setActivated(!gainFocus);
                selectedView.setSelected(gainFocus);
            }
        }
        mTabStrip.invalidate();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                    return selectTab(getSelectedTabPosition() - 1);
                }
                else if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return selectTab(getSelectedTabPosition() + 1);
                }
                break;
        }
        
        return super.dispatchKeyEvent(event);
    }

    /**
     * Set the scroll position of the tabs. This is useful for when the tabs are being displayed as
     * part of a scrolling container such as {@link ViewPager}.
     * <p>
     * Calling this method does not update the selected tab, it is only used for drawing purposes.
     *
     * @param position current scroll position
     * @param positionOffset Value from [0, 1) indicating the offset from {@code position}.
     * @param updateSelectedText Whether to update the text's selected state.
     */
    public void setScrollPosition(int position, float positionOffset, boolean updateSelectedText) {
        setScrollPosition(position, positionOffset, updateSelectedText, true);
    }

    private void setScrollPosition(int position, float positionOffset, boolean updateSelectedText,
                                   boolean updateIndicatorPosition) {
        final int roundedPosition = Math.round(position + positionOffset);
        if (roundedPosition < 0 || roundedPosition >= mTabStrip.getChildCount()) {
            return;
        }
        // Set the indicator position, if enabled
        if (updateIndicatorPosition) {
            mTabStrip.setIndicatorPositionFromTabPosition(position, positionOffset);
        }

        // Now update the scroll position, canceling any running animation
        if (mScrollAnimator != null && mScrollAnimator.isRunning()) {
            mScrollAnimator.cancel();
        }

        scrollTo(calculateScrollXForTab(position, positionOffset), 0);

        // Update the 'selected state' view as we scroll, if enabled
        if (updateSelectedText) {
            setSelectedTabView(roundedPosition);
        }
    }

    private float getScrollPosition() {
        return mTabStrip.getIndicatorPosition();
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab Tab to add
     */
    public void addTab(@NonNull Tab tab) {
        addTab(tab, mTabs.isEmpty());
    }

    /**
     * Add a tab to this layout. The tab will be inserted at <code>position</code>.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     */
    public void addTab(@NonNull Tab tab, int position) {
        addTab(tab, position, mTabs.isEmpty());
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     *
     * @param tab Tab to add
     * @param setSelected True if the added tab should become the selected tab.
     */
    public void addTab(@NonNull Tab tab, boolean setSelected) {
        addTab(tab, mTabs.size(), setSelected);
    }

    /**
     * Add a tab to this layout. The tab will be inserted at <code>position</code>.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     * @param setSelected True if the added tab should become the selected tab.
     */
    public void addTab(@NonNull final Tab tab, int position, boolean setSelected) {
        if (tab.mParent != this) {
            throw new IllegalArgumentException("Tab belongs to a different TabLayout.");
        }
        configureTab(tab, position);
        addTabView(tab);

        if (setSelected) {
            tab.getView().post(new Runnable() {
                @Override
                public void run() {
                    tab.select();
                }
            });
        }
    }

    private void addTabFromItemView(@NonNull TabItem item) {
        final Tab tab = newTab();
        if (item.mText != null) {
            tab.setText(item.mText);
        }
        if (item.mIcon != null) {
            tab.setIcon(item.mIcon);
        }
        if (item.mCustomLayout != 0) {
            tab.setCustomView(item.mCustomLayout);
        }
        if (!TextUtils.isEmpty(item.getContentDescription())) {
            tab.setContentDescription(item.getContentDescription());
        }
        addTab(tab);
    }

    /**
     * @deprecated Use {@link #addOnTabSelectedListener(OnTabSelectedListener)} and
     * {@link #removeOnTabSelectedListener(OnTabSelectedListener)}.
     */
    @Deprecated
    public void setOnTabSelectedListener(@Nullable OnTabSelectedListener listener) {
        // The logic in this method emulates what we had before support for multiple
        // registered listeners.
        if (mSelectedListener != null) {
            removeOnTabSelectedListener(mSelectedListener);
        }
        // Update the deprecated field so that we can remove the passed listener the next
        // time we're called
        mSelectedListener = listener;
        if (listener != null) {
            addOnTabSelectedListener(listener);
        }
    }

    /**
     * Add a {@link TvTabLayout.OnTabSelectedListener} that will be invoked when tab selection
     * changes.
     *
     * <p>Components that add a listener should take care to remove it when finished via
     * {@link #removeOnTabSelectedListener(OnTabSelectedListener)}.</p>
     *
     * @param listener listener to add
     */
    public void addOnTabSelectedListener(@NonNull OnTabSelectedListener listener) {
        if (!mSelectedListeners.contains(listener)) {
            mSelectedListeners.add(listener);
        }
    }

    /**
     * Remove the given {@link TvTabLayout.OnTabSelectedListener} that was previously added via
     * {@link #addOnTabSelectedListener(OnTabSelectedListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnTabSelectedListener(@NonNull OnTabSelectedListener listener) {
        mSelectedListeners.remove(listener);
    }

    /**
     * Create and return a new {@link Tab}. You need to manually add this using
     * {@link #addTab(Tab)} or a related method.
     *
     * @return A new Tab
     * @see #addTab(Tab)
     */
    @NonNull
    public Tab newTab() {
        Tab tab = sTabPool.acquire();
        if (tab == null) {
            tab = new Tab();
        }
        tab.mParent = this;
        tab.mView = createTabView(tab);
        return tab;
    }

    /**
     * Returns the number of tabs currently registered with the action bar.
     *
     * @return Tab count
     */
    public int getTabCount() {
        return mTabs.size();
    }

    /**
     * Returns the tab at the specified index.
     */
    @Nullable
    public Tab getTabAt(int index) {
        if(index >= 0 && index < mTabs.size()) {
            return mTabs.get(index);
        }
        return null;
    }

    /**
     * Returns the position of the current selected tab.
     *
     * @return selected tab position, or {@code -1} if there isn't a selected tab.
     */
    public int getSelectedTabPosition() {
        return mSelectedTab != null ? mSelectedTab.getPosition() : -1;
    }

    /**
     * Remove a tab from the layout. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param tab The tab to remove
     */
    public void removeTab(Tab tab) {
        if (tab.mParent != this) {
            throw new IllegalArgumentException("Tab does not belong to this TabLayout.");
        }

        removeTabAt(tab.getPosition());
    }

    /**
     * Remove a tab from the layout. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param position Position of the tab to remove
     */
    public void removeTabAt(int position) {
        final int selectedTabPosition = mSelectedTab != null ? mSelectedTab.getPosition() : 0;
        removeTabViewAt(position);

        final Tab removedTab = mTabs.remove(position);
        if (removedTab != null) {
            removedTab.reset();
            sTabPool.release(removedTab);
        }

        final int newTabCount = mTabs.size();
        for (int i = position; i < newTabCount; i++) {
            mTabs.get(i).setPosition(i);
        }

        if (selectedTabPosition == position) {
            selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0, position - 1)));
        }
    }

    /**
     * Remove all tabs from the action bar and deselect the current tab.
     */
    public void removeAllTabs() {
        // Remove all the views
        for (int i = mTabStrip.getChildCount() - 1; i >= 0; i--) {
            removeTabViewAt(i);
        }

        for (final Iterator<Tab> i = mTabs.iterator(); i.hasNext();) {
            final Tab tab = i.next();
            i.remove();
            tab.reset();
            sTabPool.release(tab);
        }

        mSelectedTab = null;
    }

    /**
     * Set the behavior mode for the Tabs in this layout. The valid input options are:
     * <ul>
     * <li>{@link #MODE_FIXED}: Fixed tabs display all tabs concurrently and are best used
     * with content that benefits from quick pivots between tabs.</li>
     * <li>{@link #MODE_SCROLLABLE}: Scrollable tabs display a subset of tabs at any given moment,
     * and can contain longer tab labels and a larger number of tabs. They are best used for
     * browsing contexts in touch interfaces when users don’t need to directly compare the tab
     * labels. This mode is commonly used with a {@link ViewPager}.</li>
     * </ul>
     *
     * @param mode one of {@link #MODE_FIXED} or {@link #MODE_SCROLLABLE}.
     *
     * @attr ref R.styleable#TabLayout_tabMode
     */
    public void setTabScrollMode(@Mode int mode) {
        if (mode != mScrollMode) {
            mScrollMode = mode;
            applyModeAndGravity();
        }
    }

    /**
     * Returns the current mode used by this {@link TvTabLayout}.
     *
     * @see #setTabScrollMode(int)
     */
    @Mode
    public int getTabScrollMode() {
        return mScrollMode;
    }

    /**
     * Set the gravity to use when laying out the tabs.
     *
     * @param gravity one of {@link #GRAVITY_TOP} or {@link #GRAVITY_CENTER} or {@link #GRAVITY_BOTTOM} or {@link #GRAVITY_FILL}.
     *
     * @attr ref R.styleable#TabLayout_tabGravity
     */
    public void setTabGravity(@TabGravity int gravity) {
        if (mTabGravity != gravity) {
            mTabGravity = gravity;
            applyModeAndGravity();
        }
    }

    /**
     * The current gravity used for laying out tabs.
     *
     * @return one of {@link #GRAVITY_TOP} or {@link #GRAVITY_CENTER} or {@link #GRAVITY_BOTTOM} or {@link #GRAVITY_FILL}.
     */
    @TabGravity
    public int getTabGravity() {
        return mTabGravity;
    }

    /**
     * Set the gravity to use when laying out the indicator.
     *
     * @param gravity one of {@link #GRAVITY_TOP} or {@link #GRAVITY_CENTER} or {@link #GRAVITY_BOTTOM}.
     *
     * @attr ref R.styleable#TabLayout_tabIndicatorGravity
     */
    public void setTabIndicatorGravity(@TabGravity int gravity) {
        if (mIndicatorGravity != gravity) {
            mIndicatorGravity = gravity;
            if(null != mTabStrip) {
                mTabStrip.invalidate();
            }
        }
    }

    /**
     * The current gravity used for laying out indicator.
     *
     * @return one of {@link #GRAVITY_TOP} or {@link #GRAVITY_CENTER} or {@link #GRAVITY_BOTTOM}.
     */
    @TvTabLayout.TabGravity
    public int getTabIndicatorGravity() {
        return mIndicatorGravity;
    }

    public void setTabTextColor(int color) {
        mTabTextColors = ColorStateList.valueOf(color);
        updateAllTabs();
    }

    /**
     * Sets the text colors for the different states (normal, selected) used for the tabs.
     *
     * @see #getTabTextColors()
     */
    public void setTabTextColor(@Nullable ColorStateList textColor) {
        if (mTabTextColors != textColor) {
            mTabTextColors = textColor;
            updateAllTabs();
        }
    }

    /**
     * Gets the text colors for the different states (normal, selected) used for the tabs.
     */
    @Nullable
    public ColorStateList getTabTextColors() {
        return mTabTextColors;
    }

    /**
     * Sets the text colors for the different states (normal, selected) used for the tabs.
     *
     * @attr ref R.styleable#TabLayout_tabTextColor
     * @attr ref R.styleable#TabLayout_tabActivatedTextColor
     * @attr ref R.styleable#TabLayout_tabSelectedTextColor
     */
    public void setTabTextColors(int normalColor, int activatedColor, int selectedColor) {
        setTabTextColor(createColorStateList(normalColor, activatedColor, selectedColor));
    }


    /**
     * Sets the tab indicator's height for the currently selected tab.
     *
     * @param height height to use for the indicator in pixels
     *
     * @attr ref R.styleable#TabLayout_tabIndicatorHeight
     */
    public void setTabIndicatorHeight(int height) {
        mTabStrip.setIndicatorHeight(height);
    }

    public int getTabIndicatorHeight() {
        return mTabStrip.mIndicatorHeight;
    }

    public void setTabIndicatorWidth(int width) {
        mTabStrip.setIndicatorWidth(width);
    }

    public int getTabIndicatorWidth() {
        return mTabStrip.mIndicatorWidth;
    }

    public void setIndicatorBackgroundColor(int colcor) {
        mTabStrip.setIndicatorBackgroundColor(colcor);
    }

    public void setIndicatorBackgroundHeight(int height) {
        mTabStrip.setIndicatorBackgroundHeight(height);
    }

    /**
     * The one-stop shop for setting up this {@link TvTabLayout} with a {@link ViewPager}.
     *
     * <p>This is the same as calling {@link #setupWithViewPager(ViewPager, boolean)} with
     * auto-refresh enabled.</p>
     *
     * @param viewPager the ViewPager to link to, or {@code null} to clear any previous link
     */
    public void setupWithViewPager(@Nullable ViewPager viewPager) {
        setupWithViewPager(viewPager, true);
    }

    /**
     * The one-stop shop for setting up this {@link TvTabLayout} with a {@link ViewPager}.
     *
     * <p>This method will link the given ViewPager and this TabLayout together so that
     * changes in one are automatically reflected in the other. This includes scroll state changes
     * and clicks. The tabs displayed in this layout will be populated
     * from the ViewPager adapter's page titles.</p>
     *
     * <p>If {@code autoRefresh} is {@code true}, any changes in the {@link PagerAdapter} will
     * trigger this layout to re-populate itself from the adapter's titles.</p>
     *
     * <p>If the given ViewPager is non-null, it needs to already have a
     * {@link PagerAdapter} set.</p>
     *
     * @param viewPager   the ViewPager to link to, or {@code null} to clear any previous link
     * @param autoRefresh whether this layout should refresh its contents if the given ViewPager's
     *                    content changes
     */
    public void setupWithViewPager(@Nullable final ViewPager viewPager, boolean autoRefresh) {
        setupWithViewPager(viewPager, autoRefresh, true);
    }

    private void setupWithViewPager(@Nullable final ViewPager viewPager, boolean autoRefresh,
                                    boolean implicitSetup) {
        if (mViewPager != null) {
            // If we've already been setup with a ViewPager, remove us from it
            if (mPageChangeListener != null) {
                mViewPager.removeOnPageChangeListener(mPageChangeListener);
            }
            if (mAdapterChangeListener != null) {
                mViewPager.removeOnAdapterChangeListener(mAdapterChangeListener);
            }
        }

        if (mCurrentVpSelectedListener != null) {
            // If we already have a tab selected listener for the ViewPager, remove it
            removeOnTabSelectedListener(mCurrentVpSelectedListener);
            mCurrentVpSelectedListener = null;
        }

        if (viewPager != null) {
            mViewPager = viewPager;

            // Add our custom OnPageChangeListener to the ViewPager
            if (mPageChangeListener == null) {
                mPageChangeListener = new TabLayoutOnPageChangeListener(this);
            }
            mPageChangeListener.reset();
            viewPager.addOnPageChangeListener(mPageChangeListener);

            // Now we'll add a tab selected listener to set ViewPager's current item
            mCurrentVpSelectedListener = new ViewPagerOnTabSelectedListener(viewPager);
            addOnTabSelectedListener(mCurrentVpSelectedListener);

            final PagerAdapter adapter = viewPager.getAdapter();
            if (adapter != null) {
                // Now we'll populate ourselves from the pager adapter, adding an observer if
                // autoRefresh is enabled
                setPagerAdapter(adapter, autoRefresh);
            }

            // Add a listener so that we're notified of any adapter changes
            if (mAdapterChangeListener == null) {
                mAdapterChangeListener = new AdapterChangeListener();
            }
            mAdapterChangeListener.setAutoRefresh(autoRefresh);
            viewPager.addOnAdapterChangeListener(mAdapterChangeListener);

            // Now update the scroll position to match the ViewPager's current item
            setScrollPosition(viewPager.getCurrentItem(), 0f, true);
        } else {
            // We've been given a null ViewPager so we need to clear out the internal state,
            // listeners and observers
            mViewPager = null;
            setPagerAdapter(null, false);
        }

        mSetupViewPagerImplicitly = implicitSetup;
    }

    /**
     * @deprecated Use {@link #setupWithViewPager(ViewPager)} to link a TabLayout with a ViewPager
     *             together. When that method is used, the TabLayout will be automatically updated
     *             when the {@link PagerAdapter} is changed.
     */
    @Deprecated
    public void setTabsFromPagerAdapter(@Nullable final PagerAdapter adapter) {
        setPagerAdapter(adapter, false);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        // Only delay the pressed state if the tabs can scroll
        return getTabScrollRange() > 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mViewPager == null) {
            // If we don't have a ViewPager already, check if our parent is a ViewPager to
            // setup with it automatically
            final ViewParent vp = getParent();
            if (vp instanceof ViewPager) {
                // If we have a ViewPager parent and we've been added as part of its decor, let's
                // assume that we should automatically setup to display any titles
                setupWithViewPager((ViewPager) vp, true, true);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mSetupViewPagerImplicitly) {
            // If we've been setup with a ViewPager implicitly, let's clear out any listeners, etc
            setupWithViewPager(null);
            mSetupViewPagerImplicitly = false;
        }
    }

    private int getTabScrollRange() {
        return Math.max(0, mTabStrip.getWidth() - getWidth() - getPaddingLeft()
                - getPaddingRight());
    }

    private void setPagerAdapter(@Nullable final PagerAdapter adapter, final boolean addObserver) {
        if (mPagerAdapter != null && mPagerAdapterObserver != null) {
            // If we already have a PagerAdapter, unregister our observer
            mPagerAdapter.unregisterDataSetObserver(mPagerAdapterObserver);
        }

        mPagerAdapter = adapter;

        if (addObserver && adapter != null) {
            // Register our observer on the new adapter
            if (mPagerAdapterObserver == null) {
                mPagerAdapterObserver = new PagerAdapterObserver();
            }
            adapter.registerDataSetObserver(mPagerAdapterObserver);
        }

        // Finally make sure we reflect the new adapter
        populateFromPagerAdapter();
    }

    private void populateFromPagerAdapter() {
        removeAllTabs();

        if (mPagerAdapter != null) {
            final int adapterCount = mPagerAdapter.getCount();
            for (int i = 0; i < adapterCount; i++) {
                addTab(newTab().setText(mPagerAdapter.getPageTitle(i)), false);
            }

            // Make sure we reflect the currently set ViewPager item
            if (mViewPager != null && adapterCount > 0) {
                final int curItem = mViewPager.getCurrentItem();
                if (curItem != getSelectedTabPosition() && curItem < getTabCount()) {
                    final Tab tab = getTabAt(curItem);
                    if(null != tab && null != tab.getView()) {
                        tab.getView().post(new Runnable() {
                            @Override
                            public void run() {
                                selectTab(tab);
                            }
                        });
                    } else {
                        selectTab(tab);
                    }
                }
            }
        }
    }

    private void updateAllTabs() {
        for (int i = 0, z = mTabs.size(); i < z; i++) {
            mTabs.get(i).updateView();
        }
    }

    private TabView createTabView(@NonNull final Tab tab) {
        TabView tabView = mTabViewPool != null ? mTabViewPool.acquire() : null;
        if (tabView == null) {
            tabView = new TabView(getContext());
        }
        tabView.setTab(tab);
        tabView.setFocusable(true);
        tabView.setMinimumWidth(getTabMinWidth());
        return tabView;
    }

    private void configureTab(Tab tab, int position) {
        tab.setPosition(position);
        mTabs.add(position, tab);

        final int count = mTabs.size();
        for (int i = position + 1; i < count; i++) {
            mTabs.get(i).setPosition(i);
        }
    }

    private void addTabView(Tab tab) {
        final TabView tabView = tab.mView;
        mTabStrip.addView(tabView, tab.getPosition(), createLayoutParamsForTabs());
    }

    @Override
    public void addView(View child) {
        addViewInternal(child);
    }

    @Override
    public void addView(View child, int index) {
        addViewInternal(child);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        addViewInternal(child);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        addViewInternal(child);
    }

    private void addViewInternal(final View child) {
        if (child instanceof TabItem) {
            addTabFromItemView((TabItem) child);
        } else {
            throw new IllegalArgumentException("Only TabItem instances can be added to TabLayout");
        }
    }

    public int getFontHeight(float fontSize) {
        Paint paint = new Paint();
        paint.setTextSize(fontSize);
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) Math.ceil(fm.descent - fm.ascent);
    }

    private LinearLayout.LayoutParams createLayoutParamsForTabs() {
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        updateTabViewLayoutParams(lp);
        return lp;
    }

    private void updateTabViewLayoutParams(LinearLayout.LayoutParams lp) {
        if (mScrollMode == MODE_FIXED && mTabGravity == GRAVITY_FILL) {
            lp.height = LayoutParams.MATCH_PARENT;
            lp.width = 0;
            lp.weight = 1;
        } else {
            lp.height = LayoutParams.WRAP_CONTENT;
            lp.width = LayoutParams.WRAP_CONTENT;
            lp.weight = 0;
        }
        switch (mTabGravity) {
            case GRAVITY_TOP:
                lp.gravity = Gravity.TOP| Gravity.CENTER_HORIZONTAL;
                break;
            case GRAVITY_CENTER:
                lp.gravity = Gravity.CENTER;
                break;
            case GRAVITY_BOTTOM:
                lp.gravity = Gravity.BOTTOM| Gravity.CENTER_HORIZONTAL;
                break;
        }
    }

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If we have a MeasureSpec which allows us to decide our height, try and use the default
        // height
        final int idealHeight = getDefaultHeight() + getPaddingTop() + getPaddingBottom() + mContentInsetBottom;
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.AT_MOST:
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.min(idealHeight, MeasureSpec.getSize(heightMeasureSpec)),
                        MeasureSpec.EXACTLY);
                break;
            case MeasureSpec.UNSPECIFIED:
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(idealHeight, MeasureSpec.EXACTLY);
                break;
        }

        final int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            // If we don't have an unspecified width spec, use the given size to calculate
            // the max tab width
            mTabMaxWidth = mRequestedTabMaxWidth > 0
                    ? mRequestedTabMaxWidth
                    : specWidth - dpToPx(TAB_MIN_WIDTH_MARGIN);
        }

        // Now super measure itself using the (possibly) modified height spec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getChildCount() == 1) {
            // If we're in fixed mode then we need to make the tab strip is the same width as us
            // so we don't scroll
            final View child = getChildAt(0);
            boolean remeasure = false;

            switch (mScrollMode) {
                case MODE_SCROLLABLE:
                    // We only need to resize the child if it's smaller than us. This is similar
                    // to fillViewport
                    remeasure = child.getMeasuredWidth() < getMeasuredWidth();
                    break;
                case MODE_FIXED:
                    // Resize the child so that it doesn't scroll
                    remeasure = child.getMeasuredWidth() != getMeasuredWidth();
                    break;
            }

            if (remeasure) {
                // Re-measure the child with a widthSpec set to be exactly our measure width
                int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop()
                        + getPaddingBottom(), child.getLayoutParams().height);
                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        getMeasuredWidth(), MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    private void removeTabViewAt(int position) {
        final TabView view = (TabView) mTabStrip.getChildAt(position);
        mTabStrip.removeViewAt(position);
        if (view != null) {
            view.reset();
            mTabViewPool.release(view);
        }
        requestLayout();
    }

    private void animateToTab(int newPosition) {
        if (newPosition == Tab.INVALID_POSITION) {
            return;
        }

        if (getWindowToken() == null || !ViewCompat.isLaidOut(this)
                || mTabStrip.childrenNeedLayout()) {
            // If we don't have a window token, or we haven't been laid out yet just draw the new
            // position now
            setScrollPosition(newPosition, 0f, true);
            return;
        }

        if(null == mViewPager) { //修复indicator跳动的问题
            final int startScrollX = getScrollX();
            final int targetScrollX = calculateScrollXForTab(newPosition, 0);
            if (startScrollX != targetScrollX) {
                if (mScrollAnimator == null) {
                    mScrollAnimator = ViewUtils.createAnimator();
                    mScrollAnimator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
                    mScrollAnimator.setDuration(ANIMATION_DURATION);
                    mScrollAnimator.setUpdateListener(new ValueAnimatorCompat.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimatorCompat animator) {
                            scrollTo(animator.getAnimatedIntValue(), 0);
                        }
                    });
                }
    
                mScrollAnimator.setIntValues(startScrollX, targetScrollX);
                mScrollAnimator.start();
            }
            // Now animate the indicator
            mTabStrip.animateIndicatorToPosition(newPosition, ANIMATION_DURATION);
        }
    }

    private void setSelectedTabView(int position) {
        final int tabCount = mTabStrip.getChildCount();
        if (position < tabCount) {
            for (int i = 0; i < tabCount; i++) {
                final View child = mTabStrip.getChildAt(i);
                child.setSelected(i == position && hasFocus());
                child.setActivated(i == position && !hasFocus());
            }
        }
    }

   public boolean selectTab(int position) {
        return selectTab(getTabAt(position));
    }

    boolean selectTab(Tab tab) {
        return selectTab(tab, true);
    }

    boolean selectTab(final Tab tab, boolean updateIndicator) {
        if(null == tab)
            return false;
        final Tab currentTab = mSelectedTab;
        if (currentTab == tab) {
            dispatchTabReselected(tab);
            animateToTab(tab.getPosition());
        } else {
            final int newPosition = tab.getPosition();
            if (updateIndicator) {
                if ((currentTab == null || currentTab.getPosition() == Tab.INVALID_POSITION)
                        && newPosition != Tab.INVALID_POSITION) {
                    // If we don't currently have a tab, just draw the indicator
                    setScrollPosition(newPosition, 0f, true);
                } else {
                    animateToTab(newPosition);
                }
            }
            if (newPosition != Tab.INVALID_POSITION) {
                setSelectedTabView(newPosition);
            }
            if(null != currentTab) {
                dispatchTabUnselected(currentTab);
            }
            mSelectedTab = tab;
            dispatchTabSelected(tab);
        }
        return true;
    }

    // add by zhousuqiang
    public Tab getSelectedTab() {
        return mSelectedTab;
    }

    private void dispatchTabSelected(@NonNull final Tab tab) {
        onTabSelected(tab);
        for (int i = mSelectedListeners.size() - 1; i >= 0; i--) {
            mSelectedListeners.get(i).onTabSelected(tab);
        }
    }

    private void dispatchTabUnselected(@NonNull final Tab tab) {
        onTabUnselected(tab);
        for (int i = mSelectedListeners.size() - 1; i >= 0; i--) {
            mSelectedListeners.get(i).onTabUnselected(tab);
        }
    }

    private void dispatchTabReselected(@NonNull final Tab tab) {
        onTabReselected(tab);
        for (int i = mSelectedListeners.size() - 1; i >= 0; i--) {
            mSelectedListeners.get(i).onTabReselected(tab);
        }
    }

    protected void onTabSelected(@NonNull final Tab tab) {
        final ViewPropertyAnimator animator = tab.getView().animate();
        if(mTabTextSelectedCentered) {
            animator.scaleX(mTabTextSelectedScaleValue).scaleY(mTabTextSelectedScaleValue)
                    .translationY((getHeight() - tab.getView().getHeight()) / 2)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration(700)
                    .start();
        } else if(mTabTextSelectedScaleValue > 0) {
            animator.scaleX(mTabTextSelectedScaleValue).scaleY(mTabTextSelectedScaleValue)
                    .setDuration(500)
                    .start();
        }
    }

    protected void onTabUnselected(@NonNull final Tab tab) {
        final ViewPropertyAnimator animator = tab.getView().animate();
        if(mTabTextSelectedCentered) {
            animator.scaleX(1f).scaleY(1f)
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator())
                    .setDuration(500)
                    .start();
        } else if(mTabTextSelectedScaleValue > 0) {
            animator.scaleX(1f).scaleY(1f)
                    .setDuration(500)
                    .start();
        }
    }

    protected void onTabReselected(@NonNull final Tab tab) {
    }

    private int calculateScrollXForTab(int position, float positionOffset) {
        if (mScrollMode == MODE_SCROLLABLE) {
            final View selectedChild = mTabStrip.getChildAt(position);
            final View nextChild = position + 1 < mTabStrip.getChildCount()
                    ? mTabStrip.getChildAt(position + 1)
                    : null;
            final int selectedWidth = selectedChild != null ? selectedChild.getWidth() : 0;
            final int nextWidth = nextChild != null ? nextChild.getWidth() : 0;
            
            // base scroll amount: places center of tab in center of parent
            int scrollBase = selectedChild.getLeft() + (selectedWidth / 2) - (getWidth() / 2);
            // offset amount: fraction of the distance between centers of tabs
            int scrollOffset = (int) ((selectedWidth + nextWidth) * 0.5f * positionOffset);
            
            return (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR)
                    ? scrollBase + scrollOffset
                    : scrollBase - scrollOffset;
        }
        return 0;
    }

    private void applyModeAndGravity() {
        int paddingStart = 0;
        if (mScrollMode == MODE_SCROLLABLE) {
            // If we're scrollable, or fixed at start, inset using padding
            paddingStart = Math.max(0, mContentInsetStart - mTabPaddingStart);
        }
        ViewCompat.setPaddingRelative(mTabStrip, paddingStart, 0, 0, mContentInsetBottom);

        switch (mScrollMode) {
            case MODE_FIXED:
                mTabStrip.setGravity(Gravity.CENTER_HORIZONTAL);
                break;
            case MODE_SCROLLABLE:
                mTabStrip.setGravity(GravityCompat.START);
                break;
        }

        updateTabViews(true);
    }

    private void updateTabViews(final boolean requestLayout) {
        for (int i = 0; i < mTabStrip.getChildCount(); i++) {
            View child = mTabStrip.getChildAt(i);
            child.setMinimumWidth(getTabMinWidth());
            updateTabViewLayoutParams((LinearLayout.LayoutParams) child.getLayoutParams());
            if (requestLayout) {
                child.requestLayout();
            }
        }
    }

    /**
     * A tab in this layout. Instances can be created via {@link #newTab()}.
     */
    public static final class Tab {

        /**
         * An invalid position for a tab.
         *
         * @see #getPosition()
         */
        public static final int INVALID_POSITION = -1;

        private Object mTag;
        private Drawable mIcon;
        private CharSequence mText;
        private CharSequence mContentDesc;
        private int mPosition = INVALID_POSITION;
        private View mCustomView;

        private TvTabLayout mParent;
        private TabView mView;

        private Tab() {
            // Private constructor
        }

        /**
         * @return This Tab's tag object.
         */
        @Nullable
        public Object getTag() {
            return mTag;
        }

        /**
         * Give this Tab an arbitrary object to hold for later use.
         *
         * @param tag Object to store
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setTag(@Nullable Object tag) {
            mTag = tag;
            return this;
        }

        public TabView getTabView() {
            return mView;
        }

        public View getView() {
            return null != mView ? mView : mCustomView;
        }

        /**
         * Returns the custom view used for this tab.
         *
         * @see #setCustomView(View)
         * @see #setCustomView(int)
         */
        @Nullable
        public View getCustomView() {
            return mCustomView;
        }

        /**
         * Set a custom view to be used for this tab.
         * <p>
         * If the provided view contains a {@link TextView} with an ID of
         * {@link android.R.id#text1} then that will be updated with the value given
         * to {@link #setText(CharSequence)}. Similarly, if this layout contains an
         * {@link ImageView} with ID {@link android.R.id#icon} then it will be updated with
         * the value given to {@link #setIcon(Drawable)}.
         * </p>
         *
         * @param view Custom view to be used as a tab.
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setCustomView(@Nullable View view) {
            mCustomView = view;
            updateView();
            return this;
        }

        /**
         * Set a custom view to be used for this tab.
         * <p>
         * If the inflated layout contains a {@link TextView} with an ID of
         * {@link android.R.id#text1} then that will be updated with the value given
         * to {@link #setText(CharSequence)}. Similarly, if this layout contains an
         * {@link ImageView} with ID {@link android.R.id#icon} then it will be updated with
         * the value given to {@link #setIcon(Drawable)}.
         * </p>
         *
         * @param resId A layout resource to inflate and use as a custom tab view
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setCustomView(@LayoutRes int resId) {
            final LayoutInflater inflater = LayoutInflater.from(mView.getContext());
            return setCustomView(inflater.inflate(resId, mView, false));
        }

        /**
         * Return the icon associated with this tab.
         *
         * @return The tab's icon
         */
        @Nullable
        public Drawable getIcon() {
            return mIcon;
        }

        /**
         * Return the current position of this tab in the action bar.
         *
         * @return Current position, or {@link #INVALID_POSITION} if this tab is not currently in
         * the action bar.
         */
        public int getPosition() {
            return mPosition;
        }

        void setPosition(int position) {
            mPosition = position;
        }

        /**
         * Return the text of this tab.
         *
         * @return The tab's text
         */
        @Nullable
        public CharSequence getText() {
            return mText;
        }

        /**
         * Set the icon displayed on this tab.
         *
         * @param icon The drawable to use as an icon
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setIcon(@Nullable Drawable icon) {
            mIcon = icon;
            updateView();
            return this;
        }

        /**
         * Set the icon displayed on this tab.
         *
         * @param resId A resource ID referring to the icon that should be displayed
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setIcon(@DrawableRes int resId) {
            if (mParent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            return setIcon(mParent.getResources().getDrawable(resId));
        }

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not room to display
         * the entire string.
         *
         * @param text The text to display
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setText(@Nullable CharSequence text) {
            mText = text;
            updateView();
            return this;
        }

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not room to display
         * the entire string.
         *
         * @param resId A resource ID referring to the text that should be displayed
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setText(@StringRes int resId) {
            if (mParent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            return setText(mParent.getResources().getText(resId));
        }

        /**
         * Select this tab. Only valid if the tab has been added to the action bar.
         */
        public void select() {
            if (mParent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            mParent.selectTab(this);
        }

        /**
         * Returns true if this tab is currently selected.
         */
        public boolean isSelected() {
            if (mParent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            return mParent.getSelectedTabPosition() == mPosition;
        }

        /**
         * Set a description of this tab's content for use in accessibility support. If no content
         * description is provided the title will be used.
         *
         * @param resId A resource ID referring to the description text
         * @return The current instance for call chaining
         * @see #setContentDescription(CharSequence)
         * @see #getContentDescription()
         */
        @NonNull
        public Tab setContentDescription(@StringRes int resId) {
            if (mParent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            return setContentDescription(mParent.getResources().getText(resId));
        }

        /**
         * Set a description of this tab's content for use in accessibility support. If no content
         * description is provided the title will be used.
         *
         * @param contentDesc Description of this tab's content
         * @return The current instance for call chaining
         * @see #setContentDescription(int)
         * @see #getContentDescription()
         */
        @NonNull
        public Tab setContentDescription(@Nullable CharSequence contentDesc) {
            mContentDesc = contentDesc;
            updateView();
            return this;
        }

        /**
         * Gets a brief description of this tab's content for use in accessibility support.
         *
         * @return Description of this tab's content
         * @see #setContentDescription(CharSequence)
         * @see #setContentDescription(int)
         */
        @Nullable
        public CharSequence getContentDescription() {
            return mContentDesc;
        }

        private void updateView() {
            if (mView != null) {
                mView.update();
            }
        }

        private void reset() {
            mParent = null;
            mView = null;
            mTag = null;
            mIcon = null;
            mText = null;
            mContentDesc = null;
            mPosition = INVALID_POSITION;
            mCustomView = null;
        }
    }

    public class TabView extends LinearLayout implements OnLongClickListener {
        private Tab mTab;
        private TextView mTextView;
        private ImageView mIconView;

        private View mCustomView;
        private TextView mCustomTextView;
        private ImageView mCustomIconView;

        private int mDefaultMaxLines = 2;

        public TabView(Context context) {
            super(context);
            if (mTabBackgroundResId != 0) {
                setBackgroundDrawable(getResources().getDrawable(mTabBackgroundResId));
            }
            ViewCompat.setPaddingRelative(this, mTabPaddingStart, mTabPaddingTop,
                    mTabPaddingEnd, mTabPaddingBottom);
            setGravity(Gravity.CENTER);
            setOrientation(VERTICAL);
            setClickable(true);
        }

        @Override
        public boolean performClick() {
            final boolean value = super.performClick();

            if (mTab != null) {
                mTab.select();
                return true;
            } else {
                return value;
            }
        }

        @Override
        public void setSelected(final boolean selected) {
            final boolean changed = isSelected() != selected;

            super.setSelected(selected);

            if (changed && selected && Build.VERSION.SDK_INT < 16) {
                // Pre-JB we need to manually send the TYPE_VIEW_SELECTED event
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            }

            // Always dispatch this to the child views, regardless of whether the value has
            // changed
            if (mTextView != null) {
                mTextView.setSelected(selected);
            }
            if (mIconView != null) {
                mIconView.setSelected(selected);
            }
            if (mCustomView != null) {
                mCustomView.setSelected(selected);
            }
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            // This view masquerades as an action bar tab.
//            event.setClassName("android.support.v7.app.ActionBar");
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            // This view masquerades as an action bar tab.
//            info.setClassName("android.support.v7.app.ActionBar");
        }

        @Override
        public void onMeasure(final int origWidthMeasureSpec, final int origHeightMeasureSpec) {
            final int specWidthSize = MeasureSpec.getSize(origWidthMeasureSpec);
            final int specWidthMode = MeasureSpec.getMode(origWidthMeasureSpec);
            final int maxWidth = getTabMaxWidth();

            final int widthMeasureSpec;
            final int heightMeasureSpec = origHeightMeasureSpec;

            if (maxWidth > 0 && (specWidthMode == MeasureSpec.UNSPECIFIED
                    || specWidthSize > maxWidth)) {
                // If we have a max width and a given spec which is either unspecified or
                // larger than the max width, update the width spec using the same mode
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(mTabMaxWidth, MeasureSpec.AT_MOST);
            } else {
                // Else, use the original width spec
                widthMeasureSpec = origWidthMeasureSpec;
            }

            // Now lets measure
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // We need to switch the text size based on whether the text is spanning 2 lines or not
            if (mTextView != null) {
                final Resources res = getResources();
                float textSize = mTabTextSize;
                int maxLines = mDefaultMaxLines;

                if (mIconView != null && mIconView.getVisibility() == VISIBLE) {
                    // If the icon view is being displayed, we limit the text to 1 line
                    maxLines = 1;
                } else if (mTextView != null && mTextView.getLineCount() > 1) {
                    // Otherwise when we have text which wraps we reduce the text size
                    textSize = mTabTextMultiLineSize;
                }

                final float curTextSize = mTextView.getTextSize();
                final int curLineCount = mTextView.getLineCount();
                final int curMaxLines = TextViewCompat.getMaxLines(mTextView);

                if (textSize != curTextSize || (curMaxLines >= 0 && maxLines != curMaxLines)) {
                    // We've got a new text size and/or max lines...
                    boolean updateTextView = true;

                    if (mScrollMode == MODE_FIXED && textSize > curTextSize && curLineCount == 1) {
                        // If we're in fixed mode, going up in text size and currently have 1 line
                        // then it's very easy to get into an infinite recursion.
                        // To combat that we check to see if the change in text size
                        // will cause a line count change. If so, abort the size change and stick
                        // to the smaller size.
                        final Layout layout = mTextView.getLayout();
                        if (layout == null || approximateLineWidth(layout, 0, textSize)
                                > getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) {
                            updateTextView = false;
                        }
                    }

                    if (updateTextView) {
                        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                        mTextView.setMaxLines(maxLines);
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    }
                }
            }
        }

        private void setTab(@Nullable final Tab tab) {
            if (tab != mTab) {
                mTab = tab;
                update();
            }
        }

        private void reset() {
            setTab(null);
            setSelected(false);
        }

        final void update() {
            final Tab tab = mTab;
            final View custom = tab != null ? tab.getCustomView() : null;
            if (custom != null) {
                final ViewParent customParent = custom.getParent();
                if (customParent != this) {
                    if (customParent != null) {
                        ((ViewGroup) customParent).removeView(custom);
                    }
                    addView(custom);
                }
                mCustomView = custom;
                if (mTextView != null) {
                    mTextView.setVisibility(GONE);
                }
                if (mIconView != null) {
                    mIconView.setVisibility(GONE);
                    mIconView.setImageDrawable(null);
                }

                mCustomTextView = (TextView) custom.findViewById(android.R.id.text1);
                if (mCustomTextView != null) {
                    mDefaultMaxLines = TextViewCompat.getMaxLines(mCustomTextView);
                }
                mCustomIconView = (ImageView) custom.findViewById(android.R.id.icon);
            } else {
                // We do not have a custom view. Remove one if it already exists
                if (mCustomView != null) {
                    removeView(mCustomView);
                    mCustomView = null;
                }
                mCustomTextView = null;
                mCustomIconView = null;
            }

            if (mCustomView == null) {
                // If there isn't a custom view, we'll us our own in-built layouts
                if (mIconView == null) {
                    ImageView iconView = (ImageView) LayoutInflater.from(getContext())
                            .inflate(R.layout.tab_layout_tab_icon, this, false);
                    addView(iconView, 0);
                    mIconView = iconView;
                }
                if (mTextView == null) {
                    TextView textView = (TextView) LayoutInflater.from(getContext())
                            .inflate(R.layout.tab_layout_tab_text, this, false);
                    addView(textView);
                    mTextView = textView;
                    mDefaultMaxLines = TextViewCompat.getMaxLines(mTextView);
                }
                if (mTabTextColors != null) {
                    mTextView.setTextColor(mTabTextColors);
                }
                updateTextAndIcon(mTextView, mIconView);
            } else {
                // Else, we'll see if there is a TextView or ImageView present and update them
                if (mCustomTextView != null || mCustomIconView != null) {
                    updateTextAndIcon(mCustomTextView, mCustomIconView);
                }
            }

            // Finally update our selected state
            setSelected(tab != null && tab.isSelected());
        }

        private void updateTextAndIcon(@Nullable final TextView textView,
                                       @Nullable final ImageView iconView) {
            final Drawable icon = mTab != null ? mTab.getIcon() : null;
            final CharSequence text = mTab != null ? mTab.getText() : null;
            final CharSequence contentDesc = mTab != null ? mTab.getContentDescription() : null;

            if (iconView != null) {
                if (icon != null) {
                    iconView.setImageDrawable(icon);
                    iconView.setVisibility(VISIBLE);
                    setVisibility(VISIBLE);
                } else {
                    iconView.setVisibility(GONE);
                    iconView.setImageDrawable(null);
                }
                iconView.setContentDescription(contentDesc);
            }

            final boolean hasText = !TextUtils.isEmpty(text);
            if (textView != null) {
                if (hasText) {
                    textView.setText(text);
                    textView.setVisibility(VISIBLE);
                    setVisibility(VISIBLE);
                } else {
                    textView.setVisibility(GONE);
                    textView.setText(null);
                }
                textView.setContentDescription(contentDesc);
            }

            if (iconView != null) {
                MarginLayoutParams lp = ((MarginLayoutParams) iconView.getLayoutParams());
                int bottomMargin = 0;
                if (hasText && iconView.getVisibility() == VISIBLE) {
                    // If we're showing both text and icon, add some margin bottom to the icon
                    bottomMargin = dpToPx(DEFAULT_GAP_TEXT_ICON);
                }
                if (bottomMargin != lp.bottomMargin) {
                    lp.bottomMargin = bottomMargin;
                    iconView.requestLayout();
                }
            }

            if (!hasText && !TextUtils.isEmpty(contentDesc)) {
                setOnLongClickListener(this);
            } else {
                setOnLongClickListener(null);
                setLongClickable(false);
            }
        }

        @Override
        public boolean onLongClick(final View v) {
            final int[] screenPos = new int[2];
            final Rect displayFrame = new Rect();
            getLocationOnScreen(screenPos);
            getWindowVisibleDisplayFrame(displayFrame);

            final Context context = getContext();
            final int width = getWidth();
            final int height = getHeight();
            final int midy = screenPos[1] + height / 2;
            int referenceX = screenPos[0] + width / 2;
            if (ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
                referenceX = screenWidth - referenceX; // mirror
            }

            Toast cheatSheet = Toast.makeText(context, mTab.getContentDescription(),
                    Toast.LENGTH_SHORT);
            if (midy < displayFrame.height()) {
                // Show below the tab view
                cheatSheet.setGravity(Gravity.TOP | GravityCompat.END, referenceX,
                        screenPos[1] + height - displayFrame.top);
            } else {
                // Show along the bottom center
                cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
            }
            cheatSheet.show();
            return true;
        }

        public Tab getTab() {
            return mTab;
        }

        /**
         * Approximates a given lines width with the new provided text size.
         */
        private float approximateLineWidth(Layout layout, int line, float textSize) {
            return layout.getLineWidth(line) * (textSize / layout.getPaint().getTextSize());
        }
    }

    protected class SlidingTabStrip extends LinearLayout {
        private int mIndicatorHeight;
        private int mIndicatorWidth;
        private Drawable mIndicatorDrawable;

        private int mSelectedPosition = -1;
        private float mSelectionOffset;

        private int mIndicatorLeft = -1;
        private int mIndicatorRight = -1;
        private int mIndicatorTop = -1;
        private int mIndicatorBottom = -1;

        private Paint mIndicatorBackgroundPaint;
        private Paint mCircleDotPaint;
        private int mIndicatorBackgroundHeight;
        private int mCircleDotRadius;

        private int mIndicatorBackgroundLeft;
        private int mIndicatorBackgroundRight;
        private int mIndicatorBackgroundTop;
        private int mIndicatorBackgroundBottom;

        private ValueAnimatorCompat mIndicatorAnimator;

        SlidingTabStrip(Context context) {
            super(context);
            setWillNotDraw(false);
            mIndicatorBackgroundPaint = new Paint();
            mIndicatorBackgroundPaint.setAntiAlias(true);
            mCircleDotPaint = new Paint();
            mCircleDotPaint.setAntiAlias(true);
        }

        void setIndicatorBackgroundColor(int color) {
            if (mIndicatorBackgroundPaint.getColor() != color) {
                mIndicatorBackgroundPaint.setColor(color);
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        public void setIndicatorBackgroundHeight(int height) {
            if(mIndicatorBackgroundHeight != height) {
                mIndicatorBackgroundHeight = height;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        void setIndicatorDrawable(Drawable indicatorDrawable) {
            if(mIndicatorDrawable != indicatorDrawable) {
                mIndicatorDrawable = indicatorDrawable;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        void setIndicatorHeight(int height) {
            if (mIndicatorHeight != height) {
                mIndicatorHeight = height;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        void setIndicatorWidth(int width) {
            if(mIndicatorWidth != width) {
                mIndicatorWidth = width;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        void setCircleDotRadius(int radius) {
            if (mCircleDotRadius != radius) {
                mCircleDotRadius = radius;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        void setCircleDotColor(int color) {
            if (mCircleDotPaint.getColor() != color) {
                mCircleDotPaint.setColor(color);
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        boolean childrenNeedLayout() {
            for (int i = 0, z = getChildCount(); i < z; i++) {
                final View child = getChildAt(i);
                if (child.getWidth() <= 0) {
                    return true;
                }
            }
            return false;
        }

        void setIndicatorPositionFromTabPosition(int position, float positionOffset) {
            if (mIndicatorAnimator != null && mIndicatorAnimator.isRunning()) {
                mIndicatorAnimator.cancel();
            }

            mSelectedPosition = position;
            mSelectionOffset = positionOffset;
            updateIndicatorPosition();
        }

        float getIndicatorPosition() {
            return mSelectedPosition + mSelectionOffset;
        }

        @Override
        protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
                // HorizontalScrollView will first measure use with UNSPECIFIED, and then with
                // EXACTLY. Ignore the first call since anything we do will be overwritten anyway
                return;
            }

            if (mScrollMode == MODE_FIXED && mTabGravity != GRAVITY_FILL) {
                final int count = getChildCount();

                // First we'll find the widest tab
                int largestTabWidth = 0;
                for (int i = 0, z = count; i < z; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() == VISIBLE) {
                        largestTabWidth = Math.max(largestTabWidth, child.getMeasuredWidth());
                    }
                }

                if (largestTabWidth <= 0) {
                    // If we don't have a largest child yet, skip until the next measure pass
                    return;
                }

                final int gutter = dpToPx(FIXED_WRAP_GUTTER_MIN);
                boolean remeasure = false;

                if (largestTabWidth * count <= getMeasuredWidth() - gutter * 2) {
                    // If the tabs fit within our width minus gutters, we will set all tabs to have
                    // the same width
                    for (int i = 0; i < count; i++) {
                        final LayoutParams lp =
                                (LayoutParams) getChildAt(i).getLayoutParams();
                        if (lp.width != largestTabWidth || lp.weight != 0) {
                            lp.width = largestTabWidth;
                            lp.weight = 0;
                            remeasure = true;
                        }
                    }
                } else {
                    // If the tabs will wrap to be larger than the width minus gutters, we need
                    // to switch to GRAVITY_FILL
                    mTabGravity = GRAVITY_FILL;
                    updateTabViews(false);
                    remeasure = true;
                }

                if (remeasure) {
                    // Now re-measure after our changes
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (mIndicatorAnimator != null && mIndicatorAnimator.isRunning()) {
                // If we're currently running an animation, lets cancel it and start a
                // new animation with the remaining duration
                mIndicatorAnimator.cancel();
                final long duration = mIndicatorAnimator.getDuration();
                final float oldFraction = 1f - mIndicatorAnimator.getAnimatedFraction();
                animateIndicatorToPosition(mSelectedPosition,
                        Math.round(oldFraction * duration));
            } else {
                // If we've been layed out, update the indicator position
                updateIndicatorPosition();
            }
        }

        private void updateIndicatorPosition() {
            final View selectedTitle = getChildAt(mSelectedPosition);
            int left, right, indicatorOffset;

            if (selectedTitle != null && selectedTitle.getWidth() > 0) {
                left = selectedTitle.getLeft();
                right = selectedTitle.getRight();
                indicatorOffset = mIndicatorWidth > 0 ? (selectedTitle.getWidth() - mIndicatorWidth) / 2 : 0;

                if (mSelectionOffset > 0f && mSelectedPosition < getChildCount() - 1) {
                    // Draw the selection partway between the tabs
                    View nextTitle = getChildAt(mSelectedPosition + 1);
                    left = (int) (mSelectionOffset * nextTitle.getLeft() +
                            (1.0f - mSelectionOffset) * left);
                    right = (int) (mSelectionOffset * nextTitle.getRight() +
                            (1.0f - mSelectionOffset) * right);
                }
                left += indicatorOffset;
                right -= indicatorOffset;

            } else {
                left = right = -1;
            }
            updateIndicatorTopAndBottom();
            updateIndicatorBackgroundBounds();
            setIndicatorPosition(left, right);
        }

        private void updateIndicatorTopAndBottom() {
            switch (mIndicatorGravity) {
                case GRAVITY_BOTTOM:
                    mIndicatorTop = getHeight() - mIndicatorHeight;
                    mIndicatorBottom = getHeight();
                    break;
                case GRAVITY_CENTER:
                    mIndicatorTop = (getHeight() - mIndicatorHeight) / 2;
                    mIndicatorBottom = (getHeight() + mIndicatorHeight) / 2;
                    break;
                case GRAVITY_TOP:
                    mIndicatorTop = 0;
                    mIndicatorBottom = mIndicatorHeight;
                    break;
            }
        }

        private void updateIndicatorBackgroundBounds() {
            if(mIndicatorBackgroundHeight > 0) {
                mIndicatorBackgroundLeft = 0;
                mIndicatorBackgroundRight = getWidth();
                mIndicatorBackgroundTop = mIndicatorTop + (mIndicatorHeight - mIndicatorBackgroundHeight) / 2;
                mIndicatorBackgroundBottom = mIndicatorBackgroundTop + mIndicatorBackgroundHeight;
                if(mCircleDotRadius > 0) {
                    final View leftView = getChildAt(0);
                    final View rightView = getChildAt(getChildCount() - 1);
                    mIndicatorBackgroundLeft = null == leftView ? mRequestedTabMinWidth / 2 : leftView.getWidth() / 2;
                    mIndicatorBackgroundRight = null == rightView ? getChildCount() * mRequestedTabMinWidth - mRequestedTabMinWidth / 2
                            :  rightView.getRight() - rightView.getWidth() / 2;
                }
            }
        }

        private void setIndicatorPosition(int left, int right) {
            if (left != mIndicatorLeft || right != mIndicatorRight) {
                // If the indicator's left/right has changed, invalidate
                mIndicatorLeft = left;
                mIndicatorRight = right;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        void animateIndicatorToPosition(final int position, int duration) {
            if (mIndicatorAnimator != null && mIndicatorAnimator.isRunning()) {
                mIndicatorAnimator.cancel();
            }
            final boolean isRtl = ViewCompat.getLayoutDirection(this)
                    == ViewCompat.LAYOUT_DIRECTION_RTL;

            final View targetView = getChildAt(position);
            if (targetView == null) {
                // If we don't have a view, just update the position now and return
                updateIndicatorPosition();
                return;
            }

            final int indicatorOffset = mIndicatorWidth > 0 ? (targetView.getWidth() - mIndicatorWidth) / 2 : 0;
            final int targetLeft = targetView.getLeft() + indicatorOffset;
            final int targetRight = targetView.getRight() - indicatorOffset;
            final int startLeft;
            final int startRight;

            if (Math.abs(position - mSelectedPosition) <= 1) {
                // If the views are adjacent, we'll animate from edge-to-edge
                startLeft = mIndicatorLeft;
                startRight = mIndicatorRight;
            } else {
                // Else, we'll just grow from the nearest edge
                final int offset = dpToPx(MOTION_NON_ADJACENT_OFFSET);
                if (position < mSelectedPosition) {
                    // We're going end-to-start
                    if (isRtl) {
                        startLeft = startRight = targetLeft - offset;
                    } else {
                        startLeft = startRight = targetRight + offset;
                    }
                } else {
                    // We're going start-to-end
                    if (isRtl) {
                        startLeft = startRight = targetRight + offset;
                    } else {
                        startLeft = startRight = targetLeft - offset;
                    }
                }
            }

            if (startLeft != targetLeft || startRight != targetRight) {
                ValueAnimatorCompat animator = mIndicatorAnimator = ViewUtils.createAnimator();
                animator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
                animator.setDuration(duration);
                animator.setFloatValues(0, 1);
                animator.setUpdateListener(new ValueAnimatorCompat.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimatorCompat animator) {
                        final float fraction = animator.getAnimatedFraction();
                        setIndicatorPosition(
                                AnimationUtils.lerp(startLeft, targetLeft, fraction),
                                AnimationUtils.lerp(startRight, targetRight, fraction));
                    }
                });
                animator.setListener(new ValueAnimatorCompat.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(ValueAnimatorCompat animator) {
                        mSelectedPosition = position;
                        mSelectionOffset = 0f;
                    }
                });
//                animator.setStartDelay(100); 修复tab过多进行scroll时Indicator抖动问题
                animator.start();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();

            final int childCount = getChildCount();

            // 画指示背影
            if(mIndicatorBackgroundHeight > 0) {
                canvas.drawRect(mIndicatorBackgroundLeft, mIndicatorBackgroundTop, mIndicatorBackgroundRight, mIndicatorBackgroundBottom, mIndicatorBackgroundPaint);
            }

            //画白色小圆点
            if(mCircleDotRadius > 0) {
                for (int i = 0; i < childCount; i++) {
                    View childView = getChildAt(i);
                    if(null != childView) {
                        canvas.drawCircle(childView.getWidth() / 2 + i * childView.getWidth(),
                                getHeight() - (mIndicatorHeight - mIndicatorTop)/ 2,
                                mCircleDotRadius, mCircleDotPaint);
                    }
                }
            }

            // draw indicator below the current selection
            if (null != mIndicatorDrawable && mIndicatorLeft >= 0 && mIndicatorRight > mIndicatorLeft) {
                mIndicatorDrawable.setBounds(mIndicatorLeft, mIndicatorTop, mIndicatorRight, mIndicatorBottom);
                mIndicatorDrawable.draw(canvas);
            }

            canvas.restore();
            super.draw(canvas);
        }
    }

    @Override
    protected void drawableStateChanged() {
        Drawable drawable = mTabStrip.mIndicatorDrawable;
        if (drawable != null && drawable.isStateful()) {
            drawable.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    //验证图片是否相等 , 在invalidateDrawable()会调用此方法，我们需要重写该方法。
    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mTabStrip.mIndicatorDrawable || super.verifyDrawable(who);
    }

    private static ColorStateList createColorStateList(int defaultColor, int activatedColor, int selectedColor) {
        final int states[][] = {{android.R.attr.state_activated}, {android.R.attr.state_selected}, {0}};
        final int colors[] = {activatedColor, selectedColor, defaultColor};
        return new ColorStateList(states, colors);
    }

    private int getDefaultHeight() {
        boolean hasIconAndText = false;
        for (int i = 0, count = mTabs.size(); i < count; i++) {
            Tab tab = mTabs.get(i);
            if (tab != null && tab.getIcon() != null && !TextUtils.isEmpty(tab.getText())) {
                hasIconAndText = true;
                break;
            }
        }

        int defaultHeight = hasIconAndText ? DEFAULT_HEIGHT_WITH_TEXT_ICON : DEFAULT_HEIGHT;
        return Math.max(mTabStrip.mIndicatorHeight, dpToPx(defaultHeight));
    }

    private int getTabMinWidth() {
        if (mRequestedTabMinWidth != INVALID_WIDTH) {
            // If we have been given a min width, use it
            return mRequestedTabMinWidth;
        }
        // Else, we'll use the default value
        return mScrollMode == MODE_SCROLLABLE ? mScrollableTabMinWidth : 0;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        // We don't care about the layout params of any views added to us, since we don't actually
        // add them. The only view we add is the SlidingTabStrip, which is done manually.
        // We return the default layout params so that we don't blow up if we're given a TabItem
        // without android:layout_* values.
        return generateDefaultLayoutParams();
    }

    private int getTabMaxWidth() {
        return mTabMaxWidth;
    }

    /**
     * A {@link ViewPager.OnPageChangeListener} class which contains the
     * necessary calls back to the provided {@link TvTabLayout} so that the tab position is
     * kept in sync.
     *
     * <p>This class stores the provided TabLayout weakly, meaning that you can use
     * {@link ViewPager#addOnPageChangeListener(ViewPager.OnPageChangeListener)
     * addOnPageChangeListener(OnPageChangeListener)} without removing the listener and
     * not cause a leak.
     */
    public static class TabLayoutOnPageChangeListener implements ViewPager.OnPageChangeListener {
        private final WeakReference<TvTabLayout> mTabLayoutRef;
        private int mPreviousScrollState;
        private int mScrollState;

        public TabLayoutOnPageChangeListener(TvTabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<>(tabLayout);
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            mPreviousScrollState = mScrollState;
            mScrollState = state;
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset,
                                   final int positionOffsetPixels) {
            final TvTabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null) {
                // Only update the text selection if we're not settling, or we are settling after
                // being dragged
                final boolean updateText = mScrollState != SCROLL_STATE_SETTLING ||
                        mPreviousScrollState == SCROLL_STATE_DRAGGING;
                // Update the indicator if we're not settling after being idle. This is caused
                // from a setCurrentItem() call and will be handled by an animation from
                // onPageSelected() instead.
//                final boolean updateIndicator = !(mScrollState == SCROLL_STATE_SETTLING
//                        && mPreviousScrollState == SCROLL_STATE_IDLE);
                tabLayout.setScrollPosition(position, positionOffset, updateText, true);
            }
        }

        @Override
        public void onPageSelected(final int position) {
            final TvTabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null && tabLayout.getSelectedTabPosition() != position
                    && position < tabLayout.getTabCount()) {
                // Select the tab, only updating the indicator if we're not being dragged/settled
                // (since onPageScrolled will handle that).
                final boolean updateIndicator = mScrollState == SCROLL_STATE_IDLE
                        || (mScrollState == SCROLL_STATE_SETTLING
                        && mPreviousScrollState == SCROLL_STATE_IDLE);
                tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator);
            }
        }

        private void reset() {
            mPreviousScrollState = mScrollState = SCROLL_STATE_IDLE;
        }
    }

    /**
     * A {@link TvTabLayout.OnTabSelectedListener} class which contains the necessary calls back
     * to the provided {@link ViewPager} so that the tab position is kept in sync.
     */
    public static class ViewPagerOnTabSelectedListener implements TvTabLayout.OnTabSelectedListener {
        private final ViewPager mViewPager;
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 122) {
                    mViewPager.setCurrentItem(msg.arg1);
                }
            }
        };

        public ViewPagerOnTabSelectedListener(ViewPager viewPager) {
            mViewPager = viewPager;
        }

        @Override
        public void onTabSelected(TvTabLayout.Tab tab) {
            //延迟setCurrentItem，修复连续按键时indicator跳动的问题
            mHandler.removeMessages(122);
            Message msg = mHandler.obtainMessage(122, tab.mPosition, tab.mPosition);
            mHandler.sendMessageDelayed(msg, 100);
        }

        @Override
        public void onTabUnselected(TvTabLayout.Tab tab) {
            // No-op
        }

        @Override
        public void onTabReselected(TvTabLayout.Tab tab) {
            // No-op
        }
    }

    private class PagerAdapterObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            populateFromPagerAdapter();
        }

        @Override
        public void onInvalidated() {
            populateFromPagerAdapter();
        }
    }

    private class AdapterChangeListener implements ViewPager.OnAdapterChangeListener {
        private boolean mAutoRefresh;

        @Override
        public void onAdapterChanged(@NonNull ViewPager viewPager,
                                     @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter) {
            if (mViewPager == viewPager) {
                setPagerAdapter(newAdapter, mAutoRefresh);
            }
        }

        void setAutoRefresh(boolean autoRefresh) {
            mAutoRefresh = autoRefresh;
        }
    }
}

