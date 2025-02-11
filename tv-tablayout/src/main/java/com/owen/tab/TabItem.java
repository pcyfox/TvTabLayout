package com.owen.tab;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;


/**
 * TabItem is a special 'view' which allows you to declare tab items for a {@link TvTabLayout}
 * within a layout. This view is not actually added to TabLayout, it is just a dummy which allows
 * setting of a tab items's text, icon and custom layout. See TabLayout for more information on how
 * to use it.
 *
 * @attr ref R.styleable#TabItem_android_icon
 * @attr ref R.styleable#TabItem_android_text
 * @attr ref R.styleable#TabItem_android_layout
 *
 * @see TvTabLayout
 */
public final class TabItem extends View {
    final CharSequence mText;
    final Drawable mIcon;
    final int mCustomLayout;

    public TabItem(Context context) {
        this(context, null);
    }

    public TabItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TVTabItem);
        mText = a.getText(R.styleable.TVTabItem_android_text);
        mIcon = a.getDrawable(R.styleable.TVTabItem_android_icon);
        mCustomLayout = a.getResourceId(R.styleable.TVTabItem_android_layout, 0);
        a.recycle();
    }
}
