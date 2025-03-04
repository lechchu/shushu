package com.lcchu.shushu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class ChapterListAdapter extends RecyclerView.Adapter<ChapterListAdapter.ViewHolder> {

    private ArrayList<ArrayList<String>> mData;

    private int selectedItemIndex;

    private OnRecyclerViewClickListener listener;

    ChapterListAdapter(ArrayList<ArrayList<String>> data, int index) {

        mData = data;
        selectedItemIndex = index;
    }

    // 建立ViewHolder
    static class ViewHolder extends RecyclerView.ViewHolder{

        // 宣告元件
        private TextView txtItem;

        ViewHolder(View itemView) {
            super(itemView);
            txtItem = itemView.findViewById(R.id.txtItem);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chpaterlist_item_view, parent, false);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClickListener(v);
            }
        });

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        //TODO 速度太慢
        holder.txtItem.setText(mData.get(position).get(0));
        if(position==selectedItemIndex)
            holder.txtItem.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.txtItem.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextDefault));
    }

    public void updateIndex(int index){
        selectedItemIndex = index;
        notifyDataSetChanged();
    }

    public void setItemClickListener(OnRecyclerViewClickListener itemClickListener) {
        listener = itemClickListener;
    }

    public interface OnRecyclerViewClickListener {
        void onItemClickListener(View view);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }
}