package com.wang.getapk.repository;

import android.text.TextUtils;

import com.wang.baseadapter.model.ItemArray;
import com.wang.baseadapter.model.ItemData;
import com.wang.getapk.constant.SortType;
import com.wang.getapk.model.App;
import com.wang.getapk.model.StickyPinyin;
import com.wang.getapk.model.StickyTime;
import com.wang.getapk.util.InstallTimeComparator;
import com.wang.getapk.util.PinyinComparator;
import com.wang.getapk.util.TimeComparator;
import com.wang.getapk.view.adapter.AppAdapter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import io.reactivex.functions.Function;

/**
 * Author wang
 * Date: 2018/1/27.
 */

public class SortFunction implements Function<List<App>, ItemArray> {

    private final DateFormat mDateFormat;
    private final int mSortType;

    public SortFunction(DateFormat dateFormat, int sortType) {
        mDateFormat = dateFormat;
        mSortType = SortType.of(sortType);
    }

    @Override
    public ItemArray apply(List<App> apps) throws Exception {
        ItemArray itemArray = new ItemArray();
        List<Object> stickies = new ArrayList<>();
        for (App app : apps) {
            if (SortType.isTimeSort(mSortType)) {
                //按更新时间排序看 time，按安装时间排序看 installTime
                boolean byInstall = mSortType == SortType.INSTALL_TIME;
                String date = byInstall ? app.installTime : app.time;
                if (TextUtils.isEmpty(date)) {
                    date = app.time;
                }
                if (TextUtils.isEmpty(date)) {
                    continue;
                }
                StickyTime sticky = new StickyTime(date.substring(0, 7));
                if (!stickies.contains(sticky)) {
                    long monthEnd = monthEnd(date);
                    if (byInstall) {
                        sticky.firstInstallTime = monthEnd;
                    } else {
                        sticky.lastUpdateTime = monthEnd;
                    }
                    stickies.add(sticky);
                    itemArray.add(new ItemData(AppAdapter.TYPE_STICKY, sticky));
                }
            } else {
                StickyPinyin sticky = new StickyPinyin(app.namePinyin.isEmpty() ? null : app.namePinyin.substring(0, 1));
                if (!stickies.contains(sticky)) {
                    stickies.add(sticky);
                    itemArray.add(new ItemData(AppAdapter.TYPE_STICKY, sticky));
                }
            }
            itemArray.add(new ItemData(AppAdapter.TYPE_APP, app));
        }
        switch (mSortType) {
            case SortType.INSTALL_TIME:
                Collections.sort(itemArray, new InstallTimeComparator());
                break;
            case SortType.NAME:
                Collections.sort(itemArray, new PinyinComparator());
                break;
            case SortType.UPDATE_TIME:
            default:
                Collections.sort(itemArray, new TimeComparator());
                break;
        }
        return itemArray;
    }

    /**
     * 该月的最后一毫秒，用来让月份粘性头排在本月所有应用之前。
     * <p>
     * 必须严格大于本月任何一个应用的时间戳：原实现按「上月 0 号 + 1 个月 + 24 小时」推算，
     * 得到的是本月 30 号末尾，导致 31 号安装/更新的应用排到了上一个月的分组里。
     */
    private long monthEnd(String date) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mDateFormat.parse(date));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTimeInMillis() - 1;
    }

}
