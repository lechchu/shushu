package com.lcchu.shushu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private final ArrayList<NovelInfo> bookDatas;


    SearchAdapter(ArrayList<NovelInfo> data) {

        bookDatas = data;
    }

    // 建立ViewHolder
    class ViewHolder extends RecyclerView.ViewHolder {
        // 宣告元件
        private final TextView title_text, author_text, desc_text;
        private final ImageView cover_image;

        private ConstraintLayout resultDetailLayout;


        ViewHolder(View itemView) {
            super(itemView);
            final Button read_btn, addfav_btn;
            title_text = itemView.findViewById(R.id.booknameTV);
            author_text = itemView.findViewById(R.id.bookauthorTV);
            desc_text = itemView.findViewById(R.id.bookdescTV);
            cover_image = itemView.findViewById(R.id.bookcoverIV);

            resultDetailLayout = itemView.findViewById(R.id.expandlayout);
            read_btn = itemView.findViewById(R.id.startreadBtn);
            addfav_btn = itemView.findViewById(R.id.addfavoriteBtn);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NovelInfo ni = bookDatas.get(getAdapterPosition());
                    ni.setExpanded(!ni.isExpanded());
                    notifyItemChanged(getAdapterPosition());
                }
            });


            read_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.putExtra("bookName",bookDatas.get(getAdapterPosition()).getName());
                    intent.setClass(v.getContext(), ReadActivity.class);
                    v.getContext().startActivity(intent);
                    System.out.println("starting readactivty");
                    ((Activity)v.getContext()).overridePendingTransition(R.anim.in,0);


                }
            });

            addfav_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //ArrayList<NovelInfo> favBooks = new ArrayList<NovelInfo>();

                    try {
                        File favFile = v.getContext().getFileStreamPath("favBooksInfo.json");
                        if(favFile.exists()){
                            FileInputStream fis = new FileInputStream(favFile);

                        //FileInputStream fis = v.getContext().openFileInput("favBooksInfo.json");

                        byte[] readBytes = new byte[fis.available()];
                        fis.read(readBytes);
                        fis.close();

                        String json = new String(readBytes);

                        JSONArray favBooksJson = new JSONObject(json).getJSONArray("books");
                        JSONObject tempJO= new JSONObject();
                        NovelInfo tnv = bookDatas.get(getAdapterPosition());

                        tempJO.put("name", tnv.getName());
                        tempJO.put("title", tnv.getTitle());
                        tempJO.put("cover_url", tnv.getCoverURL());
                        favBooksJson.put(tempJO);
                        JSONObject doneJO = new JSONObject();
                        doneJO.put("books",favBooksJson);

                        FileOutputStream fos = v.getContext().openFileOutput("favBooksInfo.json", Context.MODE_PRIVATE);
                        fos.write(doneJO.toString().getBytes());
                        fos.close();
                        }else{
                            JSONArray favBooksJson = new JSONArray();
                            JSONObject tempJO= new JSONObject();
                            NovelInfo tnv = bookDatas.get(getAdapterPosition());

                            tempJO.put("name", tnv.getName());
                            tempJO.put("title", tnv.getTitle());
                            tempJO.put("cover_url", tnv.getCoverURL());
                            favBooksJson.put(tempJO);
                            JSONObject doneJO = new JSONObject();
                            doneJO.put("books",favBooksJson);

                            FileOutputStream fos = v.getContext().openFileOutput("favBooksInfo.json", Context.MODE_PRIVATE);
                            fos.write(doneJO.toString().getBytes());
                            fos.close();
                        }

                        Toast.makeText(v.getContext(),"加入成功",Toast.LENGTH_LONG).show();
                        addfav_btn.setEnabled(false);

                    }catch (Exception e){e.printStackTrace();
                        Toast.makeText(v.getContext(),"加入失敗",Toast.LENGTH_LONG).show();
                    }
                }
            });

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.searchresult_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        try {

            NovelInfo bookData = bookDatas.get(position);

            Glide.with(holder.itemView).load(bookData.getCoverURL()).into(holder.cover_image);
            holder.title_text.setText(bookData.getTitle());
            holder.author_text.setText(bookData.getAuthor());
            holder.desc_text.setText(bookData.getDesc());
            holder.resultDetailLayout.setVisibility(bookData.isExpanded()?View.VISIBLE:View.GONE);

        }
        catch (Exception e){
            e.printStackTrace();
        }

    }


    @Override
    public int getItemCount() {
        return bookDatas.size();
    }


}