package com.wang.getapk.view.adapter.delegate;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wang.baseadapter.delegate.AdapterDelegate;
import com.wang.baseadapter.model.ItemArray;
import com.wang.getapk.R;
import com.wang.getapk.constant.SortType;
import com.wang.getapk.model.App;
import com.wang.getapk.view.adapter.AppAdapter;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

import com.wang.getapk.databinding.ItemAppBinding;

/**
 * Author: wangxiaojie6
 * Date: 2018/1/25
 */

public class AppDelegate extends AdapterDelegate<AppDelegate.AppViewHolder> {

    private AppAdapter.OnAppClickListener mListener;
    private int mSortType = SortType.UPDATE_TIME;

    public AppDelegate(AppAdapter.OnAppClickListener listener) {
        mListener = listener;
    }

    public void setSortType(int sortType) {
        mSortType = SortType.of(sortType);
    }

    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(ItemArray itemArray, AppViewHolder vh, int position) {
        App app = itemArray.get(position).getData();
        Context context = vh.itemView.getContext().getApplicationContext();
        vh.mApp = app;
        vh.getIcon();
        vh.binding.nameTv.setText(app.name);
        int colorDagger = ContextCompat.getColor(context, R.color.red500);
        int colorNor = ContextCompat.getColor(context, R.color.blue500);
        if (app.isSystem) {
            vh.binding.systemTv.setTextColor(colorDagger);
            vh.binding.systemTv.setText(R.string.system);
        } else {
            vh.binding.systemTv.setTextColor(colorNor);
            vh.binding.systemTv.setText(R.string.third_party);
        }
        if (app.isDebug) {
            vh.binding.debugTv.setTextColor(colorNor);
            vh.binding.debugTv.setText(R.string.debug);
        } else {
            vh.binding.debugTv.setTextColor(colorDagger);
            vh.binding.debugTv.setText(R.string.release);
        }
        vh.binding.sizeTv.setText(Formatter.formatFileSize(context, new File(app.apkPath).length()));
        vh.binding.versionNameTv.setText(app.versionName);
        //按安装时间排序时，行尾展示安装日期，和粘性头的月份保持一致
        String date = mSortType == SortType.INSTALL_TIME ? app.installTime : app.time;
        vh.binding.timeTv.setText(TextUtils.isEmpty(date) ? app.time : date);
        bindSelectState(vh, app);
    }

    /**
     * 多选模式下显示复选框；不可卸载的应用（系统应用、SeekPack 自身）置灰且不可勾选
     */
    private void bindSelectState(AppViewHolder vh, App app) {
        boolean selectMode = mListener != null && mListener.isSelectMode();
        vh.binding.checkBox.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        if (!selectMode) {
            vh.itemView.setAlpha(1F);
            return;
        }
        boolean uninstallable = mListener.isUninstallable(app);
        vh.binding.checkBox.setEnabled(uninstallable);
        vh.binding.checkBox.setChecked(uninstallable && mListener.isSelected(app));
        vh.itemView.setAlpha(uninstallable ? 1F : 0.4F);
    }

    @Override
    protected void onViewRecycled(AppViewHolder vh) {
        vh.recycler();
    }


    static class AppViewHolder extends RecyclerView.ViewHolder {

        final ItemAppBinding binding;
        App mApp;

        Disposable mDisposable;

        public AppViewHolder(View itemView, final AppAdapter.OnAppClickListener listener) {
            super(itemView);
            binding = ItemAppBinding.bind(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener.isSelectMode()) {
                        listener.onToggleSelect(mApp);
                    } else {
                        listener.onDetail(mApp, binding.iconImg);
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listener.isSelectMode()) {
                        listener.onToggleSelect(mApp);
                    } else {
                        listener.onSave(mApp);
                    }
                    return true;
                }
            });
            binding.iconImg.setOnClickListener(v -> onIconClick(listener));
        }

        public void onIconClick(AppAdapter.OnAppClickListener listener) {
            //多选模式下图标也参与勾选，避免误启动应用
            if (listener.isSelectMode()) {
                listener.onToggleSelect(mApp);
                return;
            }
            if (mApp.launch != null) {
                itemView.getContext().startActivity(mApp.launch);
            }
        }

        public void getIcon() {
            recycler();
            final WeakReference<App> appWeak = new WeakReference<>(mApp);
            mDisposable = Flowable.just(itemView.getContext().getApplicationContext().getPackageManager())
                    .map(new Function<PackageManager, Drawable>() {
                        @Override
                        public Drawable apply(PackageManager pm) throws Exception {
                            App app = appWeak.get();
                            if (app != null){
                                return app.applicationInfo.loadIcon(pm);
                            }else {
                                throw new Exception("app is null");
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSubscriber<Drawable>() {
                        @Override
                        public void onNext(Drawable drawable) {
                            binding.iconImg.setImageDrawable(drawable);
                        }

                        @Override
                        public void onError(Throwable t) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }

        public void recycler() {
            if (mDisposable != null && !mDisposable.isDisposed()) {
                mDisposable.dispose();
            }
        }


    }
}
