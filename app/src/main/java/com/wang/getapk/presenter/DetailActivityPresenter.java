package com.wang.getapk.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.wang.getapk.model.App;
import com.wang.getapk.model.Sign;
import com.wang.getapk.repository.KWSubscriber;
import com.wang.getapk.repository.LocalRepository;
import com.wang.getapk.util.SignUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

/**
 * Author: wangxiaojie6
 * Date: 2019/1/2
 */
public class DetailActivityPresenter {

    private IView mView;
    private LocalRepository mRepository;

    public DetailActivityPresenter(IView view) {
        mView = view;
        mRepository = LocalRepository.getInstance();
    }

    public Disposable getSignature(Context context, String packageName, boolean file) {
        return mRepository.getSignature(context.getApplicationContext(), packageName, file, new KWSubscriber<Sign>() {
            @Override
            public void success(Sign sign) {
                if (mView != null) {
                    mView.getSignatureSuccess(sign);
                }
            }

            @Override
            public void error(int code, String error) {
                if (mView != null) {
                    mView.getSignatureError(error);
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

    /**
     * Activity 销毁时解绑，避免异步回调回到已销毁的界面
     */
    public void detach() {
        mView = null;
    }

    public interface IView {

        void getSignatureSuccess(Sign sign);

        void getSignatureError(String error);

        void inProgress(float progress);

        void saveSuccess(String path);

        void saveError(String message);

    }
}
