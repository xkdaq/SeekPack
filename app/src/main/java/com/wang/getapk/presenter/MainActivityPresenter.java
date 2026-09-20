package com.wang.getapk.presenter;

import android.content.Context;
import android.net.Uri;

import com.wang.baseadapter.model.ItemArray;
import com.wang.getapk.model.App;
import com.wang.getapk.repository.KWSubscriber;
import com.wang.getapk.repository.LocalRepository;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

/**
 * Author: wangxiaojie6
 * Date: 2018/1/24
 */

public class MainActivityPresenter {

    private IView mView;
    private LocalRepository mRepository;


    public MainActivityPresenter(IView view) {
        mView = view;
        mRepository = LocalRepository.getInstance();
    }


    public Disposable getAndSort(Context context, final int sortType) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return mRepository.getAndSort(context.getApplicationContext(), sortType, dateFormat, new KWSubscriber<ItemArray>() {
            @Override
            public void success(ItemArray apps) {
                if (mView != null) {
                    mView.getAppsSuccess(apps, sortType);
                }
            }

            @Override
            public void error(int code, String error) {
                if (mView != null) {
                    mView.getAppsError(error);
                }
            }
        });
    }

    public Disposable getApp(Context context, String path) {
        return mRepository.getApp(context.getApplicationContext(), path, new KWSubscriber<App>() {
            @Override
            public void success(App app) {
                if (mView != null) {
                    mView.getAppSuccess(app);
                }
            }

            @Override
            public void error(int code, String error) {
                if (mView != null) {
                    mView.getAppError(error);
                }
            }
        });
    }


    public Disposable saveApk(Context context, App app, final Uri dest) {
        return mRepository.saveApk(context, app, dest, new KWSubscriber<String>() {
            @Override
            public void success(String path) {
                if (mView != null) {
                    mView.saveSuccess(path);
                }
            }

            @Override
            public void error(int code, String error) {
                if (mView != null) {
                    mView.saveError(error);
                }
            }

            @Override
            public void inProgress(float progress) {
                if (mView != null) {
                    mView.inProgress(progress);
                }
            }
        });
    }

    public void clearApps() {
        mRepository.setApps(null);
    }

    /**
     * Activity 销毁时解绑，避免异步回调回到已销毁的界面
     */
    public void detach() {
        mView = null;
    }


    public interface IView {

        void getAppsSuccess(ItemArray apps, int sortType);

        void getAppsError(String message);

        void getAppSuccess(App app);

        void getAppError(String error);

        void inProgress(float progress);

        void saveSuccess(String path);

        void saveError(String message);

    }
}
