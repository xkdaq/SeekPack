package com.wang.getapk.view.adapter;

import android.view.View;

import com.wang.baseadapter.BaseRecyclerViewAdapter;
import com.wang.baseadapter.model.ItemArray;
import com.wang.getapk.model.App;
import com.wang.getapk.view.adapter.delegate.AppDelegate;
import com.wang.getapk.view.adapter.delegate.StickyDelegate;

/**
 * Author: wangxiaojie6
 * Date: 2018/1/24
 */

public class AppAdapter extends BaseRecyclerViewAdapter {

    public static final int TYPE_STICKY = 1;
    public static final int TYPE_APP = 2;

    private final AppDelegate mAppDelegate;

    public AppAdapter(ItemArray itemArray, OnAppClickListener listener, int sortType) {
        super(itemArray);
        mAppDelegate = new AppDelegate(listener);
        mAppDelegate.setSortType(sortType);
        delegatesManager.addDelegate(TYPE_STICKY, new StickyDelegate());
        delegatesManager.addDelegate(TYPE_APP, mAppDelegate);
    }

    /**
     * 列表行右下角的日期跟随当前排序：按安装时间排序时显示安装日期，否则显示更新日期
     */
    public void setSortType(int sortType) {
        mAppDelegate.setSortType(sortType);
    }

    public interface OnAppClickListener {

        void onDetail(App app, View iconImg);

        void onSave(App app);

        /**
         * 是否处于批量卸载的多选模式
         */
        boolean isSelectMode();

        /**
         * 该应用是否可被卸载（系统应用与自身包不可卸载）
         */
        boolean isUninstallable(App app);

        boolean isSelected(App app);

        /**
         * 多选模式下点击条目：切换勾选状态
         */
        void onToggleSelect(App app);

    }
}
