package com.wang.getapk.view.adapter.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wang.baseadapter.delegate.AdapterDelegate;
import com.wang.baseadapter.model.ItemArray;
import com.wang.getapk.R;
import com.wang.getapk.model.App;
import com.wang.getapk.model.StickyTime;

import androidx.recyclerview.widget.RecyclerView;

import com.wang.getapk.databinding.ItemStickyBinding;

/**
 * Author: wangxiaojie6
 * Date: 2018/1/25
 */

public class StickyDelegate extends AdapterDelegate<StickyDelegate.StickyViewHolder> {

    @Override
    public StickyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticky, parent, false);
        return new StickyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemArray itemArray, StickyViewHolder vh, int position) {
        App sticky = itemArray.get(position).getData();
        if (sticky instanceof StickyTime) {
            vh.binding.nameTv.setText(sticky.time);
        }else {
            vh.binding.nameTv.setText(sticky.namePinyin);
        }
    }

    static class StickyViewHolder extends RecyclerView.ViewHolder {

        final ItemStickyBinding binding;

        public StickyViewHolder(View itemView) {
            super(itemView);
            binding = ItemStickyBinding.bind(itemView);
        }
    }
}
