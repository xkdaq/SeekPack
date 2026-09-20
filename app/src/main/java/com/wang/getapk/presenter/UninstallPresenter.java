package com.wang.getapk.presenter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 批量卸载的队列状态机。
 * <p>
 * 普通应用无法静默卸载他人应用，只能逐个弹系统卸载确认框，因此这里维护一个串行队列：
 * 每次把队首交给 {@link IView#requestUninstall} 去拉起系统卸载页，系统页返回后调用
 * {@link #onUninstallReturned(Context)} 统计结果并推进到下一个。
 * <p>
 * 只保存包名而不是 App 对象，便于在 Activity 重建时通过 Bundle 续跑。
 */
public class UninstallPresenter {

    private static final String KEY_PENDING = "uninstall_pending";
    private static final String KEY_CURRENT = "uninstall_current";
    private static final String KEY_TOTAL = "uninstall_total";
    private static final String KEY_SUCCESS = "uninstall_success";
    private static final String KEY_FAILED = "uninstall_failed";

    private IView mView;

    private final ArrayList<String> mPending = new ArrayList<>();
    private String mCurrent;
    private int mTotal;
    private int mSuccess;
    private int mFailed;

    public UninstallPresenter(IView view) {
        mView = view;
    }

    public void detach() {
        mView = null;
    }

    /**
     * 是否有未完成的卸载队列（含正在系统卸载页里的那一个）
     */
    public boolean isRunning() {
        return mCurrent != null || !mPending.isEmpty();
    }

    public void start(Collection<String> packageNames) {
        mPending.clear();
        mPending.addAll(packageNames);
        mTotal = mPending.size();
        mSuccess = 0;
        mFailed = 0;
        mCurrent = null;
        next();
    }

    /**
     * 系统卸载页返回后调用。不信任 resultCode（各 ROM 返回不一致），
     * 直接查包是否还在来判定是否真的卸载成功。
     */
    public void onUninstallReturned(Context context) {
        if (mCurrent == null) {
            return;
        }
        if (isInstalled(context, mCurrent)) {
            mFailed++;
        } else {
            mSuccess++;
        }
        mCurrent = null;
        next();
    }

    private void next() {
        if (mPending.isEmpty()) {
            mCurrent = null;
            if (mView != null) {
                mView.uninstallFinished(mSuccess, mFailed);
            }
            return;
        }
        mCurrent = mPending.remove(0);
        if (mView != null) {
            mView.requestUninstall(mCurrent, mSuccess + mFailed + 1, mTotal);
        }
    }

    private static boolean isInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void saveState(Bundle outState) {
        if (!isRunning()) {
            return;
        }
        outState.putStringArrayList(KEY_PENDING, new ArrayList<>(mPending));
        outState.putString(KEY_CURRENT, mCurrent);
        outState.putInt(KEY_TOTAL, mTotal);
        outState.putInt(KEY_SUCCESS, mSuccess);
        outState.putInt(KEY_FAILED, mFailed);
    }

    public void restoreState(Bundle state) {
        if (state == null || !state.containsKey(KEY_TOTAL)) {
            return;
        }
        List<String> pending = state.getStringArrayList(KEY_PENDING);
        mPending.clear();
        if (pending != null) {
            mPending.addAll(pending);
        }
        mCurrent = state.getString(KEY_CURRENT);
        mTotal = state.getInt(KEY_TOTAL);
        mSuccess = state.getInt(KEY_SUCCESS);
        mFailed = state.getInt(KEY_FAILED);
    }

    public interface IView {

        /**
         * 请求拉起系统卸载页
         *
         * @param packageName 待卸载包名
         * @param index       当前是第几个（从 1 开始）
         * @param total       总数
         */
        void requestUninstall(String packageName, int index, int total);

        void uninstallFinished(int success, int failed);
    }
}
