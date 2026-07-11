package com.wang.getapk.view.dialog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.wang.getapk.databinding.DialogProgressBinding;

/**
 * Author: wangxiaojie6
 * Date: 2018/1/26
 */

public class ProgressDialog extends BaseDialog<ProgressDialog.Builder, DialogProgressBinding> {

    private ProgressDialog(Builder builder) {
        super(builder);
    }

    @Override
    protected DialogProgressBinding getViewBinding() {
        return DialogProgressBinding.inflate(getLayoutInflater());
    }

    @Nullable
    @Override
    protected AppCompatTextView getTitleView() {
        return binding.title.titleTv;
    }

    @Nullable
    @Override
    protected AppCompatButton getNeutralButton() {
        return null;
    }

    @Nullable
    @Override
    protected AppCompatButton getNegativeButton() {
        return null;
    }

    @Nullable
    @Override
    protected AppCompatButton getPositiveButton() {
        return null;
    }

    @Override
    protected void afterView(Context context, Builder builder) {

    }

    public static class Builder extends BaseBuilder<Builder> {

        public Builder(@NonNull Context context) {
            super(context);
        }

        @UiThread
        public ProgressDialog build() {
            return new ProgressDialog(this);
        }

        @UiThread
        public ProgressDialog show() {
            ProgressDialog dialog = build();
            dialog.show();
            return dialog;
        }
    }
}
