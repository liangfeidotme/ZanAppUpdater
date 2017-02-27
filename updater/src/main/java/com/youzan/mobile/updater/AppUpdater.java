package com.youzan.mobile.updater;

import android.content.Context;
import android.content.Intent;

public class AppUpdater {
    private Builder builder;

    public static class Builder {
        private Context context;

        private String url;
        private String title;
        private String content;
        private boolean force;

        private String app;
        private String description;

        public Builder(final Context context) {
            this.context = context;
        }

        public Builder url(final String url) {
            this.url = url;
            return this;
        }

        public Builder app(final String app) {
            this.app = app;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder title(final String title) {
            this.title = title;
            return this;
        }

        public Builder content(final String content) {
            this.content = content;
            return this;
        }

        public Builder force(final boolean force) {
            this.force = force;
            return this;
        }

        public AppUpdater build() {
            return new AppUpdater(this);
        }
    }

    private AppUpdater(final Builder builder) {
        this.builder = builder;
    }

    public void update() {
        Intent intent = new Intent(builder.context, DownloadActivity.class);
        intent.putExtra(DownloadActivity.EXTRA_STRING_APP_NAME,  builder.app);
        intent.putExtra(DownloadActivity.EXTRA_STRING_URL, builder.url);
        intent.putExtra(DownloadActivity.EXTRA_STRING_TITLE, builder.title);
        intent.putExtra(DownloadActivity.EXTRA_STRING_CONTENT, builder.content);
        intent.putExtra(DownloadActivity.EXTRA_STRING_DESCRIPTION,  builder.description);
        intent.putExtra(DownloadActivity.EXTRA_BOOLEAN_FORCE, builder.force);
        builder.context.startActivity(intent);
    }
}
