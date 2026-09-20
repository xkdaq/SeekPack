package com.wang.getapk.constant;

/**
 * 列表排序方式
 */
public class SortType {

    /**
     * 按最后更新时间倒序（最近更新的在前）
     */
    public static final int UPDATE_TIME = 0;

    /**
     * 按应用名拼音升序
     */
    public static final int NAME = 1;

    /**
     * 按安装时间倒序（最新安装的在前）
     */
    public static final int INSTALL_TIME = 2;

    public static int of(int value) {
        return (value == NAME || value == INSTALL_TIME) ? value : UPDATE_TIME;
    }

    /**
     * 是否按时间分组（按月粘性头）
     */
    public static boolean isTimeSort(int sortType) {
        return sortType == UPDATE_TIME || sortType == INSTALL_TIME;
    }
}
