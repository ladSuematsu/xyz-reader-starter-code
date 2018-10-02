package com.example.xyzreader.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.volley.toolbox.NetworkImageView;
import com.example.xyzreader.R;

public class DynamicHeightImageView extends AppCompatImageView {
    private float mAspectRatio = 1.0F;

    public DynamicHeightImageView(Context context) {
        super(context);
    }

    public DynamicHeightImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributeSet(context, attrs, 0);
    }

    public DynamicHeightImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributeSet(context, attrs, defStyle);
    }

    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
        requestLayout();
    }

    private void getAttributeSet(Context context, AttributeSet attrs, int defStyleRes) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,
                                                        R.styleable.DynamicHeightImage,
                                                        0,
                                                        0);

        try {
            mAspectRatio = typedArray.getFloat(R.styleable.DynamicHeightImage_proportion, 1.0F);
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int aspectRatioHeight = (int) (MeasureSpec.getSize(widthMeasureSpec) * mAspectRatio);
        int aspectRatioHeightSpec = MeasureSpec.makeMeasureSpec(aspectRatioHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, aspectRatioHeightSpec);
    }
}
