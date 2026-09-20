package com.wang.getapk.util;

import com.wang.baseadapter.model.ItemData;
import com.wang.getapk.model.App;

import java.util.Comparator;

/**
 * 按安装时间倒序，最新安装的排在前面
 */
public class InstallTimeComparator implements Comparator<ItemData> {
    @Override
    public int compare(ItemData data1, ItemData data2) {
        long time1 = ((App) data1.getData()).firstInstallTime;
        long time2 = ((App) data2.getData()).firstInstallTime;
        return Long.compare(time2, time1);
    }
}
