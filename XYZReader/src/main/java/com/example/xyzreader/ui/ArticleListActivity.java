package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean mRefreshError;
    private boolean mRefreshNoInternet;
    private RecyclerView mRecyclerView;

    private final SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refresh();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        setupViews();

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = findViewById(R.id.recycler_view);

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(onRefreshListener);
    }

    private void refresh() {
        Log.d("REFRESH", "Triggered");
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                mRefreshError = intent.getBooleanExtra(UpdaterService.EXTRA_IS_ERROR, false);
                mRefreshNoInternet = intent.getBooleanExtra(UpdaterService.EXTRA_NO_INTERNET, false);

                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);

        if (mRefreshNoInternet) {
            Snackbar.make(mSwipeRefreshLayout, R.string.list_refresh_no_internet, Snackbar.LENGTH_LONG).show();
        } else if (mRefreshError) {
            Snackbar.make(mSwipeRefreshLayout, R.string.list_refresh_error, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private final Cursor mCursor;

        private final String labelFormat = getString(R.string.article_date_author_format);

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
        // Use default locale format
        private final SimpleDateFormat outputFormat = new SimpleDateFormat();
        // Most time functions can only handle 1902 - 2037
        private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            mCursor.moveToPosition(position);

            String title = mCursor.getString(ArticleLoader.Query.TITLE);
            holder.titleView.setText(title);

            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
            String parsedLabel = parseLabel(date, author);
            holder.subtitleView.setText(parsedLabel);

            String thumbnailUrl = mCursor.getString(ArticleLoader.Query.THUMB_URL);
            Glide.with(ArticleListActivity.this)
                    .setDefaultRequestOptions(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .centerCrop())
                    .load(thumbnailUrl)
                    .into(holder.thumbnailView);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

        private Date parsePublishedDate(String date) {
            try {
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        private String parseLabel(String date, String author) {
            Date publishedDate = parsePublishedDate(date);

            String formattedDate = publishedDate.before(START_OF_EPOCH.getTime())
                    ? outputFormat.format(publishedDate)
                    : DateUtils.getRelativeTimeSpanString(publishedDate.getTime(),
                                                        System.currentTimeMillis(),
                                                        DateUtils.HOUR_IN_MILLIS,
                                                        DateUtils.FORMAT_ABBREV_ALL).toString();

            return String.format(labelFormat, formattedDate, author);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnailView;
            TextView titleView;
            TextView subtitleView;

            ViewHolder(View view) {
                super(view);
                thumbnailView = view.findViewById(R.id.thumbnail);
                titleView = view.findViewById(R.id.article_title);
                subtitleView = view.findViewById(R.id.article_subtitle);
            }
        }
    }
}
