package com.wang.getapk.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wang.getapk.R;
import com.wang.getapk.model.FileItem;
import com.wang.getapk.view.listener.OnRecyclerClickListener;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wang.getapk.databinding.ItemFileBinding;

/**
 * Author: wangxiaojie6
 * Date: 2018/1/11
 */

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    @NonNull
    private List<FileItem> mFileItems;
    @NonNull
    private OnRecyclerClickListener mListener;

    public FileAdapter(@NonNull List<FileItem> fileItems, @NonNull OnRecyclerClickListener listener) {
        mFileItems = fileItems;
        mListener = listener;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FileViewHolder vh, int position) {
        FileItem item = mFileItems.get(position);
        if (item.isDirectory) {
            vh.binding.nameTv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder_blue_24dp, 0, 0, 0);
        } else {
            vh.binding.nameTv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_android_green_24dp, 0, 0, 0);
        }
        vh.binding.nameTv.setText(item.name);
    }

    @Override
    public int getItemCount() {
        return mFileItems.size();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {

        final ItemFileBinding binding;

        public FileViewHolder(View itemView) {
            super(itemView);
            binding = ItemFileBinding.bind(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onClick(0, getAdapterPosition(), null);
                }
            });
        }
    }
}
