package com.wang.getapk.view.dialog;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.wang.getapk.R;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.viewbinding.ViewBinding;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Author: wangxiaojie6
 * Date: 2017/12/28
 */

public abstract class BaseDialog<Builder extends BaseBuilder, B extends ViewBinding> extends AppCompatDialog {

    public static final int POSITIVE = 1;
    public static final int NEUTRAL = 2;
    public static final int NEGATIVE = 3;

    @IntDef({POSITIVE, NEUTRAL, NEGATIVE})
    public @interface DialogAction {
    }

    protected B binding;

    private CompositeDisposable mCompositeDisposable;
    Builder mBuilder;

    BaseDialog(Builder builder) {
        super(builder.context);
        mBuilder = builder;
        mCompositeDisposable = new CompositeDisposable();
        setCancelable(builder.cancelable);
        setCanceledOnTouchOutside(builder.canceledOnTouchOutside);
        setOnShowListener(builder.showListener);
        setOnDismissListener(builder.dismissListener);
        setOnCancelListener(builder.cancelListener);
        setOnKeyListener(builder.keyListener);
        getWindow().setSoftInputMode(mBuilder.softInputMode);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = getViewBinding();
        setContentView(binding.getRoot());
    }

    @Override
    public void show() {
        super.show();
        initCommonView();
        initViewListener();
        afterView(mBuilder.context, mBuilder);
    }

    private void initCommonView() {
        AppCompatTextView titleTV = getTitleView();
        if (titleTV != null) {
            if (mBuilder.titleColorSet) {
                titleTV.setTextColor(mBuilder.titleColor);
            }
            titleTV.setText(mBuilder.title);
        }
        setButton(getPositiveButton(), mBuilder.positive);
        setButton(getNegativeButton(), mBuilder.negative);
        setButton(getNeutralButton(), mBuilder.neutral);

        AppCompatButton neutral = getNeutralButton();
        if (neutral != null) {
            neutral.setOnClickListener(v -> onNeutral());
        }
        AppCompatButton negative = getNegativeButton();
        if (negative != null) {
            negative.setOnClickListener(v -> onNegative());
        }
        AppCompatButton positive = getPositiveButton();
        if (positive != null) {
            positive.setOnClickListener(v -> onPositive());
        }
    }

    private void setButton(Button button, CharSequence name) {
        if (button != null) {
            if (TextUtils.isEmpty(name)) {
                button.setVisibility(View.GONE);
            } else {
                button.setVisibility(View.VISIBLE);
                button.setText(name);
            }
        }
    }

    protected abstract B getViewBinding();

    @Nullable
    protected abstract AppCompatTextView getTitleView();

    @Nullable
    protected abstract AppCompatButton getNeutralButton();

    @Nullable
    protected abstract AppCompatButton getNegativeButton();

    @Nullable
    protected abstract AppCompatButton getPositiveButton();

    protected void initViewListener() {

    }

    protected abstract void afterView(Context context, Builder builder);

    public void onNeutral() {
        if (mBuilder.autoDismiss) {
            dismiss();
        }
        if (mBuilder.onNeutralListener != null) {
            mBuilder.onNeutralListener.onClick(this, NEUTRAL);
        }
    }

    public void onNegative() {
        if (mBuilder.autoDismiss) {
            dismiss();
        }
        if (mBuilder.onNegativeListener != null) {
            mBuilder.onNegativeListener.onClick(this, NEGATIVE);
        }
    }

    public void onPositive() {
        if (mBuilder.autoDismiss) {
            dismiss();
        }
        if (mBuilder.onPositiveListener != null) {
            mBuilder.onPositiveListener.onClick(this, POSITIVE);
        }
    }

    public Builder getBuilder() {
        return mBuilder;
    }

    /**
     * 插入到观察者集合
     *
     * @param disposable
     */
    public void addDisposable(Disposable disposable) {
        if (disposable != null) {
            if (mCompositeDisposable != null) {
                mCompositeDisposable.add(disposable);
            } else {
                disposable.dispose();
            }
        }
    }

    public void removeDisposable(Disposable disposable) {
        if (disposable != null) {
            if (mCompositeDisposable != null) {
                mCompositeDisposable.remove(disposable);
            } else if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        }
    }

    /**
     * 获取观察者集合
     *
     * @return
     */
    public CompositeDisposable getCompositeDisposable() {
        return mCompositeDisposable;
    }


    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();
            mCompositeDisposable = null;
        }
    }

    public interface OnButtonClickListener {

        void onClick(@NonNull BaseDialog dialog, @DialogAction int which);

    }
}
