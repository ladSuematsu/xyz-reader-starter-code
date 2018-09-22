package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.entity.Article;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private FragmentManager fragmentManager;
    private boolean viewCreated;
    private Toolbar titleToolbar;

    public interface Callback {
        Article requestArticle(int itemIndex);
    }

    private static final String TAG = "ArticleDetailFragment";
    private static final String ARG_ITEM_INDEX = "arg_item_index";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    @Deprecated private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private NestedScrollView mScrollView;
    private ColorDrawable mStatusBarColorDrawable;

    private View mPhotoContainerView;
    private ImageView mPhotoView;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private ImageView mPhotoBackgroundView;
    private int cursorIndex;

    private int itemIndex;
    private Callback callback;
    TextListAdapter textListAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    public static ArticleDetailFragment newInstance2(int itemIndex) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_ITEM_INDEX, itemIndex);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        callback  = (Callback) (getParentFragment() == null
                    ?  context
                    : getParentFragment());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        if (getArguments().containsKey(ARG_ITEM_INDEX)) {
            itemIndex = getArguments().getInt(ARG_ITEM_INDEX);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);

        fragmentManager = getFragmentManager();
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
//        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView =  mRootView.findViewById(R.id.photo);
        mPhotoBackgroundView =  mRootView.findViewById(R.id.photo_background);
        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);
        titleToolbar = mRootView.findViewById(R.id.article_toolbar_title);

        mStatusBarColorDrawable = new ColorDrawable(0);

        textListAdapter = new TextListAdapter(inflater);

        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewCreated = true;
        loadArticle();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && callback != null && !isResumed()) {
            loadArticle();
        }
    }

    private void loadArticle() {
        getLoaderManager().initLoader(0, null, this);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = article.getPublishedDate();
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (!viewCreated|| mRootView == null) {
            return;
        }

        TextView titleView =  mRootView.findViewById(R.id.article_title);
        TextView bylineView =  mRootView.findViewById(R.id.article_byline);
//        TextView bodyView =  mRootView.findViewById(R.id.article_body);
        RecyclerView bodyBlocks = mRootView.findViewById(R.id.article_body_blocks);
        bodyBlocks.setLayoutManager(new LinearLayoutManager(getContext()));
        bodyBlocks.setAdapter(textListAdapter);

//        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (article != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleToolbar.setTitle(article.getTitle());
            titleView.setText(article.getTitle());
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                Spanned subtitle = Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + article.getAuthor()
                                + "</font>");


                bylineView.setText(subtitle);
            } else {
                String subtitle = outputFormat.format(publishedDate) + " by "
                        + article.getAuthor();

                // If date is before 1902, just show the string
                bylineView.setText(subtitle);
            }

            loadTextBody(article.getBody());
            
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(article.getPhotoUrl(), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                mPhotoBackgroundView.setImageBitmap(imageContainer.getBitmap());

                                mRootView.findViewById(R.id.meta_bar)
                                        .setBackgroundColor(mMutedColor);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );

            titleToolbar.setTitle("N/A");
        }
    }

    private void loadTextBody(String body) {
        textListAdapter.setDatasource(body);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        } else if (mCursor != null && mCursor.moveToFirst()){
            cursorIndex = 0;
            article = Article.extract(cursorIndex, mCursor);
        }

        bindViews();
    }

    Article article;

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    class TextListAdapter extends RecyclerView.Adapter<TextListAdapter.TextBlockViewHolderBase> {
        private static final int VIEWHOLDER_TYPE_HEADER = 1;
        private static final int VIEWHOLDER_TYPE_PARAGRAPH = 2;

        private final LayoutInflater layoutInflater;
        private String[] textBlocks;

        public TextListAdapter(LayoutInflater inflater) {
            layoutInflater = inflater;
            textBlocks = new String[0];
        }

        @NonNull
        @Override
        public TextListAdapter.TextBlockViewHolderBase onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextListAdapter.TextBlockViewHolderBase viewholder;

            View view;
            switch (viewType) {
                case VIEWHOLDER_TYPE_HEADER:
                    view = layoutInflater.inflate(R.layout.article_text_body_header, parent, false);
                    viewholder = new TextBlockViewHolder(view);
                    break;

                case VIEWHOLDER_TYPE_PARAGRAPH:
                default:
                    view = layoutInflater.inflate(R.layout.article_text_body_paragraph, parent, false);
                    viewholder = new TextBlockViewHolder(view);
                    break;
            }

            return viewholder;
        }

        @Override
        public void onBindViewHolder(@NonNull TextListAdapter.TextBlockViewHolderBase holder, int position) {
            holder.setText(textBlocks[position]);
        }

        @Override
        public int getItemCount() {
            return textBlocks.length + 1;
        }

        void setDatasource(String text) {
            this.textBlocks = TextUtils.isEmpty(text)
                                        ? new String[0]
                                        : TextUtils.split(text, "(\r\n\r\n|\n\n)");

            notifyDataSetChanged();
        }

        abstract class TextBlockViewHolderBase extends RecyclerView.ViewHolder {
            TextBlockViewHolderBase(View itemView) {
                super(itemView);
            }

            abstract void setText(String text);
        }

        class TextBlockViewHolder extends TextBlockViewHolderBase {

            private final TextView textBlock;

            TextBlockViewHolder(View itemView) {
                super(itemView);
                this.textBlock = (TextView) itemView;
            }

            @Override
            void setText(String text) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    textBlock.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    textBlock.setText(Html.fromHtml(text));
                }
            }
        }
    }

    public static class TextBlockView extends View {
        public final Point screenSize = new Point();

        private StaticLayout textBlockLayout;
        private TextPaint textBlockPaint;
        private final int verticalPadding = (int) dpToPx(8);
        private final int horizontalPadding = (int) dpToPx(8);

        public TextBlockView(Context context) {
            super(context);
            init();
        }

        public TextBlockView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public TextBlockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public TextBlockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            init();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int textHeight = textBlockLayout.getHeight();
            int textWidth = textBlockLayout.getWidth();

            int viewHeight = 2 * verticalPadding + textHeight;
            int viewWidth= 2 * horizontalPadding + textWidth;

            setMeasuredDimension(viewWidth, viewHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            textBlockLayout.draw(canvas);
            canvas.restore();
        }

        private void init() {
            textBlockPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textBlockPaint.setColor(getContext().getResources().getColor(R.color.ltgray));
            textBlockPaint.setTextSize(spToPx(18));

            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getSize(screenSize);
        }

        public void setTextBlock(CharSequence textBlock) {
            textBlockLayout = new StaticLayout(textBlock, textBlockPaint, screenSize.x, Layout.Alignment.ALIGN_NORMAL, 1, 1, true);

            requestLayout();
            invalidate();
        }

        private float spToPx(int i) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, i, getResources().getDisplayMetrics());
        }

        private float dpToPx(int i) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, i, getResources().getDisplayMetrics());
        }
    }
}
