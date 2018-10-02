package com.example.xyzreader.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.SubtitleCollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
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
    private boolean viewCreated;
    private SubtitleCollapsingToolbarLayout titleToolbar;
    private String labelFormat;

    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private ImageView mPhotoView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private ImageView mPhotoBackgroundView;
    private int cursorIndex;

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        labelFormat = getText(R.string.article_date_author_format_html).toString();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView =  mRootView.findViewById(R.id.photo);
        mPhotoBackgroundView =  mRootView.findViewById(R.id.photo_background);
//        titleToolbar = mRootView.findViewById(R.id.article_toolbar_title);
        titleToolbar = mRootView.findViewById(R.id.collapsing_toolbar);

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

        if (isVisibleToUser && isResumed() && (mCursor == null || mCursor.isClosed())) {
            loadArticle();
        }
    }

    private void loadArticle() {
        getLoaderManager().initLoader(0, null, this);
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
        RecyclerView bodyBlocks = mRootView.findViewById(R.id.article_body_blocks);
        bodyBlocks.setLayoutManager(new LinearLayoutManager(getContext()));
        bodyBlocks.addItemDecoration(new SimplePaddingDecoration(getResources().getDimensionPixelSize(R.dimen.detail_body_paragraph_spacing)));
        bodyBlocks.setAdapter(textListAdapter);

        if (article == null) {
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            titleToolbar.setTitle("N/A");
            titleToolbar.setSubtitle("N/A");

            textListAdapter.setDatasource("");
        } else {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleToolbar.setTitle(article.getTitle());

            titleView.setText(article.getTitle());

            String author = article.getAuthor();
            titleToolbar.setSubtitle(TextUtils.isEmpty(author)
                                        ? ""
                                        : String.format(getString(R.string.article_author_format), author));


            Date publishedDate = parsePublishedDate();
            String formattedDate = publishedDate.before(START_OF_EPOCH.getTime())
                                                            ? outputFormat.format(publishedDate)
                                                            : DateUtils.getRelativeTimeSpanString(publishedDate.getTime(),
                                                                    System.currentTimeMillis(),
                                                                    DateUtils.HOUR_IN_MILLIS,
                                                                    DateUtils.FORMAT_ABBREV_ALL).toString();

            String subtitle = String.format(labelFormat, formattedDate, author);
            bylineView.setText(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                                        ? Html.fromHtml(subtitle, Html.FROM_HTML_MODE_LEGACY)
                                        : Html.fromHtml(subtitle));

            loadTextBody(article.getBody());

            Glide.with(getContext())
                .setDefaultRequestOptions(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .centerCrop())
                .load(article.getPhotoUrl())
                .into(mPhotoBackgroundView);

            Glide.with(getContext())
                    .setDefaultRequestOptions(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .centerInside())
                    .load(article.getPhotoUrl())
                    .into(mPhotoView);
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

    class TextListAdapter extends RecyclerView.Adapter<TextListAdapter.TextBlockViewHolder> {
        private final LayoutInflater layoutInflater;
        private String[] textBlocks;

        public TextListAdapter(LayoutInflater inflater) {
            layoutInflater = inflater;
            textBlocks = new String[0];
        }

        @NonNull
        @Override
        public TextListAdapter.TextBlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = layoutInflater.inflate(R.layout.article_text_body_paragraph, parent, false);


            return new TextBlockViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TextListAdapter.TextBlockViewHolder holder, int position) {
            holder.setText(textBlocks[position]);
        }

        @Override
        public int getItemCount() {
            return textBlocks.length;
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
}
