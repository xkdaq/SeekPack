package com.wang.getapk.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.wang.baseadapter.StickyHeaderDecoration;
import com.wang.baseadapter.listener.OnHeaderClickListener;
import com.wang.baseadapter.listener.StickyHeaderTouchListener;
import com.wang.baseadapter.model.ItemArray;
import com.wang.baseadapter.model.ItemData;
import com.wang.baseadapter.widget.WaveSideBarView;
import com.wang.getapk.R;
import com.wang.getapk.constant.Key;
import com.wang.getapk.constant.SortType;
import com.wang.getapk.model.App;
import com.wang.getapk.presenter.MainActivityPresenter;
import com.wang.getapk.presenter.UninstallPresenter;
import com.wang.getapk.util.CommonPreference;
import com.wang.getapk.util.FileUtil;
import com.wang.getapk.databinding.ActivityMainBinding;
import com.wang.getapk.view.adapter.AppAdapter;
import com.wang.getapk.view.dialog.BaseDialog;
import com.wang.getapk.view.dialog.NumberProgressDialog;
import com.wang.getapk.view.dialog.ProgressDialog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity
        implements AppAdapter.OnAppClickListener,
        Toolbar.OnMenuItemClickListener,

        SwipeRefreshLayout.OnRefreshListener,
        WaveSideBarView.OnTouchLetterChangeListener,
        OnHeaderClickListener,
        MainActivityPresenter.IView,
        UninstallPresenter.IView {

    private static final String KEY_SELECT_MODE = "select_mode";
    private static final String KEY_SELECTED = "selected_packages";

    RecyclerView mRecyclerView;
    Toolbar mToolbar;
    SwipeRefreshLayout mRefreshView;
    WaveSideBarView mSideBarView;

    private ActivityMainBinding mBinding;
    private MainActivityPresenter mPresenter;
    private UninstallPresenter mUninstallPresenter;
    private CompositeDisposable mDisposables;
    private Disposable mDisposable;
    private NumberProgressDialog mNumDialog;
    private ProgressDialog mDialog;

    private int mSortType = SortType.UPDATE_TIME;

    /**
     * 批量卸载的多选模式，选中项按包名记录，列表刷新后依然稳定
     */
    private boolean mIsSelectMode = false;
    private final LinkedHashSet<String> mSelectedPackages = new LinkedHashSet<>();

    private OnBackPressedCallback mBackCallback;
    private CharSequence mDefaultTitle;

    private ActivityResultLauncher<Intent> mUninstallLauncher;
    private ActivityResultLauncher<String> mCreateDocLauncher;
    /**
     * 等待 SAF 选好保存位置的应用
     */
    private App mPendingSaveApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mRecyclerView = mBinding.recyclerView;
        mToolbar = mBinding.toolbar;
        mRefreshView = mBinding.refreshView;
        mSideBarView = mBinding.sideBarView;
        mSortType = SortType.of(CommonPreference.getInt(this, Key.KEY_SORT_TYPE, SortType.UPDATE_TIME));
        mPresenter = new MainActivityPresenter(this);
        mUninstallPresenter = new UninstallPresenter(this);
        mDisposables = new CompositeDisposable();

        registerLaunchers();
        restoreState(savedInstanceState);

        mDefaultTitle = mToolbar.getTitle();
        mToolbar.inflateMenu(R.menu.menu_main);
        updateSortIcon();
        mToolbar.setOnMenuItemClickListener(this);
        mRefreshView.setOnRefreshListener(this);
        mRefreshView.setColorSchemeResources(R.color.blue300, R.color.red300, R.color.green300);
        mRefreshView.setRefreshing(true);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        StickyHeaderDecoration decoration = new StickyHeaderDecoration(AppAdapter.TYPE_STICKY);
        mRecyclerView.addItemDecoration(decoration);
        mRecyclerView.addOnItemTouchListener(new StickyHeaderTouchListener(this, decoration, this));
        mRecyclerView.setVerticalScrollBarEnabled(SortType.isTimeSort(mSortType));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    if (mSortType == SortType.NAME) {
                        mSideBarView.hide();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

            }
        });

        mSideBarView.setOnTouchLetterChangeListener(this);

        mBinding.selectAllBtn.setOnClickListener(v -> onSelectAll());
        mBinding.uninstallBtn.setOnClickListener(v -> onUninstallClick());

        //多选模式下返回键先退出多选
        mBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                exitSelectMode();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, mBackCallback);

        updateSelectModeUI();
        onRefresh();
    }

    private void registerLaunchers() {
        //系统卸载页返回：交给队列统计结果并推进下一个
        mUninstallLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> mUninstallPresenter.onUninstallReturned(this));

        //导出 apk：由系统文档选择器给出可写入的 uri，绕开分区存储限制
        mCreateDocLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/vnd.android.package-archive"),
                uri -> {
                    App app = mPendingSaveApp;
                    mPendingSaveApp = null;
                    if (uri == null || app == null) {
                        return;
                    }
                    startSave(app, uri);
                });
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mIsSelectMode = savedInstanceState.getBoolean(KEY_SELECT_MODE, false);
        List<String> selected = savedInstanceState.getStringArrayList(KEY_SELECTED);
        if (selected != null) {
            mSelectedPackages.addAll(selected);
        }
        //系统卸载页是独立 Activity，宿主可能被回收重建，这里恢复未跑完的队列
        mUninstallPresenter.restoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SELECT_MODE, mIsSelectMode);
        outState.putStringArrayList(KEY_SELECTED, new ArrayList<>(mSelectedPackages));
        mUninstallPresenter.saveState(outState);
    }

    // ------------------------------------------------------------ 批量卸载

    private void enterSelectMode() {
        if (mIsSelectMode) {
            return;
        }
        mIsSelectMode = true;
        mSelectedPackages.clear();
        updateSelectModeUI();
        notifyAdapter();
    }

    private void exitSelectMode() {
        if (!mIsSelectMode) {
            return;
        }
        mIsSelectMode = false;
        mSelectedPackages.clear();
        updateSelectModeUI();
        notifyAdapter();
    }

    private void updateSelectModeUI() {
        Menu menu = mToolbar.getMenu();
        MenuItem uninstall = menu.findItem(R.id.uninstall);
        MenuItem done = menu.findItem(R.id.done);
        if (uninstall != null) {
            uninstall.setVisible(!mIsSelectMode);
        }
        if (done != null) {
            done.setVisible(mIsSelectMode);
        }

        int count = mSelectedPackages.size();
        mToolbar.setTitle(mIsSelectMode ? getString(R.string.selected_count, count) : mDefaultTitle);
        if (mBackCallback != null) {
            mBackCallback.setEnabled(mIsSelectMode);
        }
        mBinding.selectBar.setVisibility(mIsSelectMode ? View.VISIBLE : View.GONE);
        mBinding.uninstallBtn.setText(getString(R.string.uninstall_count, count));
        mBinding.uninstallBtn.setEnabled(count > 0);
        mBinding.selectAllBtn.setText(isAllSelected() ? R.string.deselect_all : R.string.select_all);
        //多选时禁用下拉刷新，避免列表在勾选过程中被重建
        mRefreshView.setEnabled(!mIsSelectMode);
    }

    private void notifyAdapter() {
        RecyclerView.Adapter<?> adapter = mRecyclerView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 当前列表中所有可卸载的应用
     */
    private List<App> uninstallableApps() {
        List<App> apps = new ArrayList<>();
        RecyclerView.Adapter<?> adapter = mRecyclerView.getAdapter();
        if (!(adapter instanceof AppAdapter)) {
            return apps;
        }
        ItemArray itemArray = ((AppAdapter) adapter).getItems();
        for (int i = 0; i < itemArray.size(); i++) {
            ItemData data = itemArray.get(i);
            if (data.getDataType() != AppAdapter.TYPE_APP) {
                continue;
            }
            App app = data.getData();
            if (isUninstallable(app)) {
                apps.add(app);
            }
        }
        return apps;
    }

    private boolean isAllSelected() {
        List<App> apps = uninstallableApps();
        return !apps.isEmpty() && mSelectedPackages.size() >= apps.size();
    }

    private void onSelectAll() {
        List<App> apps = uninstallableApps();
        if (apps.isEmpty()) {
            return;
        }
        if (isAllSelected()) {
            mSelectedPackages.clear();
        } else {
            for (App app : apps) {
                mSelectedPackages.add(app.packageName);
            }
        }
        updateSelectModeUI();
        notifyAdapter();
    }

    private void onUninstallClick() {
        final int count = mSelectedPackages.size();
        if (count == 0) {
            Toast.makeText(this, R.string.no_app_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.batch_uninstall)
                .setMessage(getString(R.string.confirm_uninstall, count))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) ->
                        mUninstallPresenter.start(new ArrayList<>(mSelectedPackages)))
                .show();
    }

    @Override
    public void requestUninstall(String packageName, int index, int total) {
        mToolbar.setTitle(getString(R.string.uninstalling_progress, index, total));
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + packageName));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        try {
            mUninstallLauncher.launch(intent);
        } catch (Exception e) {
            //拉不起系统卸载页（极少见）时按未卸载处理，继续下一个
            mUninstallPresenter.onUninstallReturned(this);
        }
    }

    @Override
    public void uninstallFinished(int success, int failed) {
        exitSelectMode();
        new AlertDialog.Builder(this)
                .setTitle(R.string.uninstall_result_title)
                .setMessage(getString(R.string.uninstall_result, success, failed))
                .setPositiveButton(R.string.confirm, null)
                .show();
        if (success > 0) {
            //缓存里还留着已卸载的包，必须清掉重查
            mRefreshView.setRefreshing(true);
            onRefresh();
        }
    }

    // ------------------------------------------------------ 列表选中状态回调

    @Override
    public boolean isSelectMode() {
        return mIsSelectMode;
    }

    @Override
    public boolean isUninstallable(App app) {
        return app != null && !app.isSystem && !app.isFormFile
                && !getPackageName().equals(app.packageName);
    }

    @Override
    public boolean isSelected(App app) {
        return app != null && mSelectedPackages.contains(app.packageName);
    }

    @Override
    public void onToggleSelect(App app) {
        if (app == null) {
            return;
        }
        if (!isUninstallable(app)) {
            int tip = getPackageName().equals(app.packageName)
                    ? R.string.self_cannot_uninstall : R.string.system_app_cannot_uninstall;
            Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mSelectedPackages.remove(app.packageName)) {
            mSelectedPackages.add(app.packageName);
        }
        updateSelectModeUI();
        notifyAdapter();
    }

    // ------------------------------------------------------------ 导出 apk

    /**
     * 让系统文档选择器决定保存位置，无需任何存储权限
     */
    private void showSavePicker(App app) {
        if (app == null) {
            return;
        }
        mPendingSaveApp = app;
        String fileName = FileUtil.safeFileName(app.name + "_" + app.versionName) + ".apk";
        try {
            mCreateDocLauncher.launch(fileName);
        } catch (Exception e) {
            mPendingSaveApp = null;
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void startSave(App app, Uri uri) {
        dismissDialog();
        mNumDialog = new NumberProgressDialog.Builder(MainActivity.this)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .title(R.string.copying)
                .negative(R.string.cancel)
                .onNegative(new BaseDialog.OnButtonClickListener() {
                    @Override
                    public void onClick(@NonNull BaseDialog dialog, int which) {
                        if (mDisposable != null && !mDisposable.isDisposed()) {
                            mDisposable.dispose();
                        }
                    }
                }).show();
        mDisposable = mPresenter.saveApk(this, app, uri);
        mDisposables.add(mDisposable);
    }

    // ------------------------------------------------------------------ 列表

    @Override
    public void onRefresh() {
        mPresenter.clearApps();
        MenuItem sort = mToolbar.getMenu().findItem(R.id.sort);
        if (sort != null) {
            sort.setEnabled(false);
        }
        mDisposables.add(mPresenter.getAndSort(this, mSortType));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.uninstall) {
            enterSelectMode();
        } else if (itemId == R.id.done) {
            exitSelectMode();
        } else if (itemId == R.id.sort) {
            showSortDialog();
        }

        return true;
    }

    /**
     * 三种排序方式单选，切换后立即重排；多选模式下也可切换，选中项按包名保留
     */
    private void showSortDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sort_title)
                .setSingleChoiceItems(R.array.sort_types, mSortType, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == mSortType) {
                        return;
                    }
                    mSortType = SortType.of(which);
                    updateSortIcon();
                    if (!mIsSelectMode) {
                        mRefreshView.setRefreshing(true);
                    }
                    MenuItem sortItem = mToolbar.getMenu().findItem(R.id.sort);
                    if (sortItem != null) {
                        sortItem.setEnabled(false);
                    }
                    mDisposables.add(mPresenter.getAndSort(this, mSortType));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateSortIcon() {
        MenuItem sortItem = mToolbar.getMenu().findItem(R.id.sort);
        if (sortItem == null) {
            return;
        }
        switch (mSortType) {
            case SortType.NAME:
                sortItem.setIcon(R.drawable.ic_a_white_24dp);
                break;
            case SortType.INSTALL_TIME:
                sortItem.setIcon(R.drawable.ic_install_white_24dp);
                break;
            case SortType.UPDATE_TIME:
            default:
                sortItem.setIcon(R.drawable.ic_timer_white_24dp);
                break;
        }
    }

    @Override
    public void onLetterChange(String letter) {
        if (mRecyclerView.getAdapter() == null) {
            return;
        }
        ItemArray itemArray = ((AppAdapter) mRecyclerView.getAdapter()).getItems();
        int size = itemArray.size();
        for (int i = 0; i < size; i++) {
            ItemData data = itemArray.get(i);
            if (data.getDataType() == AppAdapter.TYPE_STICKY) {
                App app = data.getData();
                if (app.namePinyin.startsWith(letter)) {
                    LinearLayoutManager mLayoutManager =
                            (LinearLayoutManager) mRecyclerView.getLayoutManager();
                    mLayoutManager.scrollToPositionWithOffset(i, 0);
                    return;
                }
            }
        }
    }

    @Override
    public void onHeader(int viewType, int position) {

    }

    @Override
    public void onDetail(App app, View iconImg) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("app", app);
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                Pair.create(iconImg, "logo_img")
        ).toBundle());
    }

    @Override
    public void onSave(App app) {
        showSavePicker(app);
    }

    private void dismissDialog() {
        if (mNumDialog != null) {
            mNumDialog.dismiss();
            mNumDialog = null;
        }
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void getAppsSuccess(ItemArray apps, int sortType) {
        mRefreshView.setRefreshing(false);
        MenuItem sort = mToolbar.getMenu().findItem(R.id.sort);
        if (sort != null) {
            sort.setEnabled(true);
        }
        CommonPreference.putInt(this, Key.KEY_SORT_TYPE, sortType);
        mRecyclerView.setVerticalScrollBarEnabled(SortType.isTimeSort(sortType));
        if (sortType == SortType.NAME) {
            mSideBarView.setVisibility(View.VISIBLE);
            mSideBarView.showAfterHide();
        } else {
            mSideBarView.setVisibility(View.GONE);
        }
        if (mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(new AppAdapter(apps, this, sortType));
        } else {
            AppAdapter adapter = (AppAdapter) mRecyclerView.getAdapter();
            adapter.setSortType(sortType);
            adapter.setItems(apps);
        }
        //列表变了，去掉已经不存在（被卸载）的选中项
        if (mIsSelectMode) {
            mSelectedPackages.retainAll(currentPackages());
            updateSelectModeUI();
        }
    }

    private LinkedHashSet<String> currentPackages() {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        for (App app : uninstallableApps()) {
            packages.add(app.packageName);
        }
        return packages;
    }

    @Override
    public void getAppsError(String message) {
        mRefreshView.setRefreshing(false);
        MenuItem sort = mToolbar.getMenu().findItem(R.id.sort);
        if (sort != null) {
            sort.setEnabled(true);
        }
        Toast.makeText(this, "error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void getAppSuccess(App app) {
        dismissDialog();
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("app", app);
        startActivity(intent);
    }

    @Override
    public void getAppError(String error) {
        dismissDialog();
        Toast.makeText(this, "error: " + error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void inProgress(float progress) {
        if (mNumDialog != null) {
            mNumDialog.setProgress(Math.round(progress * 100));
        }
    }

    @Override
    public void saveSuccess(String path) {
        dismissDialog();
        Toast.makeText(this, "success: " + path, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void saveError(String message) {
        dismissDialog();
        Toast.makeText(this, "error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        mDisposables.clear();
        dismissDialog();
        mPresenter.detach();
        mUninstallPresenter.detach();
        mBinding = null;
        super.onDestroy();
    }


}
