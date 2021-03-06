/*
 * MIT License
 *
 * Copyright (c) 2016. Dmytro Karataiev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.adkdevelopment.rssreader.ui.presenters;

import android.content.Context;
import android.util.Log;

import com.adkdevelopment.rssreader.App;
import com.adkdevelopment.rssreader.data.local.NewsObject;
import com.adkdevelopment.rssreader.data.remote.Rss;
import com.adkdevelopment.rssreader.ui.base.BaseMvpPresenter;
import com.adkdevelopment.rssreader.ui.contracts.ListContract;
import com.adkdevelopment.rssreader.utils.Utilities;

import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Presenter for the ListFragment.
 * Created by Dmytro Karataiev on 8/10/16.
 */
public class ListPresenter
        extends BaseMvpPresenter<ListContract.View>
        implements ListContract.Presenter {

    private static final String TAG = ListPresenter.class.getSimpleName();

    private final CompositeSubscription mSubscription;
    private final Context mContext;
    private Rss mRss = null;

    public ListPresenter(Context context) {
        mSubscription = new CompositeSubscription();
        mContext = context;
    }

    @Override
    public void requestData() {
        checkViewAttached();
        getMvpView().showProgress(true);

        mSubscription.add(App.getDataManager().findAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<List<NewsObject>>() {
                    @Override
                    public void onCompleted() {
                        getMvpView().showProgress(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "FindAll Error Getting: " + e);
                        if (e.toString().contains("collection == null")) {
                            getMvpView().showEmpty();
                        } else {
                            getMvpView().showError();
                        }
                        getMvpView().showProgress(false);
                    }

                    @Override
                    public void onNext(List<NewsObject> results) {
                        if (results != null && results.size() > 0) {
                            getMvpView().showData(results);
                        } else {
                            getMvpView().showEmpty();
                        }
                    }
                }));

    }

    @Override
    public void requestData(String query) {
        checkViewAttached();
        getMvpView().showProgress(true);

        if (query.length() == 0) {
            requestData();
        } else {
            mSubscription.add(App.getDataManager().search(query)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Subscriber<List<NewsObject>>() {
                        @Override
                        public void onCompleted() {
                            getMvpView().showProgress(false);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "Search Error Getting: " + e);
                            getMvpView().showError();
                            getMvpView().showProgress(false);
                        }

                        @Override
                        public void onNext(List<NewsObject> results) {
                            if (results != null && results.size() > 0) {
                                getMvpView().showData(results);
                            } else {
                                getMvpView().showEmpty();
                            }
                        }
                    }));
        }
    }

    @Override
    public void fetchData() {
        checkViewAttached();
        getMvpView().showProgress(true);

        if (Utilities.isOnline(mContext)) {
            mSubscription.add(App.getApiManager().getNewsService().getNews()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Subscriber<Rss>() {
                        @Override
                        public void onCompleted() {
                            addToDb();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "Fetch Error Getting: " + e);
                        }

                        @Override
                        public void onNext(Rss rss) {
                            mRss = rss;
                        }
                    }));
        } else {
            getMvpView().showProgress(false);
        }

    }

    /**
     * Method to add news to the db in async.
     */
    private void addToDb() {
        if (mRss != null && mRss.getChannel().getItem().size() > 0) {
            mSubscription.add(App.getDataManager().addBulk(mRss.getChannel().getItem())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Subscriber<Boolean>() {
                        @Override
                        public void onCompleted() {
                            requestData();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "GetNews Error Getting: " + e);
                            getMvpView().showError();
                        }

                        @Override
                        public void onNext(Boolean added) {
                        }
                    }));
        }
    }

    @Override
    public void detachView() {
        super.detachView();
        if (!mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
    }
}
