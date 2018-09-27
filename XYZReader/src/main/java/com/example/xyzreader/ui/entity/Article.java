package com.example.xyzreader.ui.entity;

import android.database.Cursor;

import com.example.xyzreader.data.ArticleLoader;

public class Article {

    private final long id;
    private final String title;
    private final String author;
    private final String body;
    private final String thumbUrl;
    private final String photoUrl;
    private final double aspectRatio;
    private final String publishedDate;

    public static Article extract(int position, Cursor mCursor) {
        if (!mCursor.moveToPosition(position)) {
            return null;
        }

        long id = mCursor.getLong(ArticleLoader.Query._ID);
        String publishedDate = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
        String body = mCursor.getString(ArticleLoader.Query.BODY);
        String thumbUrl = mCursor.getString(ArticleLoader.Query.THUMB_URL);
        String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
        double aspectRatio = mCursor.getShort(ArticleLoader.Query.ASPECT_RATIO);


        return new Article(id, title, author, body, thumbUrl, photoUrl, aspectRatio, publishedDate);
    }

    public Article(long id, String title, String author, String body, String thumbUrl, String photoUrl, double aspectRatio, String publishedDate) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.body = body;
        this.thumbUrl = thumbUrl;
        this.photoUrl = photoUrl;
        this.aspectRatio = aspectRatio;
        this.publishedDate = publishedDate;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public String getPublishedDate() {
        return publishedDate;
    }
}
