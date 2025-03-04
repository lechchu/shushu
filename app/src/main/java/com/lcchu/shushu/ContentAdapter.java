package com.lcchu.shushu;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder>{

    private ArrayList<Pair<Integer, String>> novelList;
    private int fontSize = 24;

    public ContentAdapter(ArrayList<Pair<Integer, String>> contents) {
        this.novelList = contents;
    }
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        notifyItemRangeChanged(0, getItemCount());
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.novel_content_textview);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.novelcontent_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(novelList.get(position).second);
        holder.textView.setTextSize(fontSize);
    }

    @Override
    public int getItemCount() {
        return novelList.size();
    }
    public int getChapterIndex(int position){
        Pair<Integer, String> novelData = novelList.get(position);
        return novelData.first;

    }
    public void insertPreviousChapter(Integer newChapterID, String newChapterContent) {
        Log.d("TAG", "insertPrevChapter new size: "+novelList.size());
        novelList.add(0, new Pair<>(newChapterID, newChapterContent));
        notifyItemInserted(0);
    }

    public void insertNextChapter(Integer newChapterID, String newChapterContent) {
        Log.d("TAG", "insertNextChapter new size: "+novelList.size());
        novelList.add(new Pair<>(newChapterID, newChapterContent));
        if(!novelList.isEmpty())
            notifyItemInserted(novelList.size() - 1);
        else
            notifyItemInserted(0);
    }

    public void clearChapterList(){
        int size = novelList.size();

        if(!novelList.isEmpty()) {
            novelList.clear();
            Log.d("ContentAdapter", "clearChapter, new size: " + novelList.size());
            notifyItemRangeRemoved(0, size);
        }else
            Log.d("ContentAdapter", "novel item is not empty");

    }
}