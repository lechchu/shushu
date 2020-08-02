package com.lcchu.shushu;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    private ArrayList<NovelInfo> bookDatas;
    private NovelInfo bookData;
    private String bFName;
    private int selectedItem;
    Bitmap coverBmp;

    FavoriteAdapter(ArrayList<NovelInfo> data) {

        bookDatas = data;
    }

    // 建立ViewHolder
    class ViewHolder extends RecyclerView.ViewHolder {
        // 宣告元件
        private TextView titleText, authorText, descText;
        private ImageView coverImange;

        private ConstraintLayout resultDetailLayout;


        ViewHolder(View itemView) {
            super(itemView);

            titleText = (TextView) itemView.findViewById(R.id.fav_titleTV);
            //authorText = (TextView) itemView.findViewById(R.id.bookauthorTV);
            //descText = (TextView) itemView.findViewById(R.id.bookdescTV);
            coverImange = (ImageView) itemView.findViewById(R.id.fav_bookcoverIV);
            //novelView = (CardView)itemView.findViewById(R.id.novelCV);



            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.putExtra("bookName",bookDatas.get(getAdapterPosition()).getName());
                    intent.setClass(v.getContext(), ReadActivity.class);
                    v.getContext().startActivity(intent);
                    ((Activity)v.getContext()).overridePendingTransition(R.anim.in,R.anim.out);

                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(v.getContext());
                    dialog.setTitle("提示");
                    dialog.setMessage("確定要刪除小說嗎?");
                    dialog.setNegativeButton("NO",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                        }

                    });
                    dialog.setPositiveButton("YES",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            // TODO Auto-generated method stub
                            String json;

                            ArrayList<NovelInfo> favBooks = new ArrayList<NovelInfo>();
                            File favFile = v.getContext().getFileStreamPath("favBooksInfo.json");
                            if(favFile.exists()) {
                                try {

                                    FileInputStream fis = new FileInputStream(favFile);
                                    byte[] readBytes = new byte[fis.available()];
                                    fis.read(readBytes);
                                    fis.close();
                                    json = new String(readBytes);

                                    NovelInfo favBookDatas;
                                    JSONArray favBooksJson = new JSONObject(json).getJSONArray("books");
                                    favBooksJson.remove(getAdapterPosition());

                                    JSONObject doneJO = new JSONObject();
                                    doneJO.put("books",favBooksJson);

                                    FileOutputStream fos = v.getContext().openFileOutput("favBooksInfo.json", Context.MODE_PRIVATE);
                                    fos.write(doneJO.toString().getBytes());
                                    fos.close();


                                    bookDatas.remove(getAdapterPosition());
                                    notifyDataSetChanged();
                                    Toast.makeText(v.getContext(),"刪除成功",Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                        /*
                        FAdapter = new FavoriteAdapter(favBooks);
                        uiUpdateHandler.sendEmptyMessage(2);

                         */
                            }

                        }

                    });

                    dialog.show();


                    return true;
                }
            });

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.favorite_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        bookData = bookDatas.get(position);
        Glide.with(holder.itemView).load(bookData.getCoverURL()).into(holder.coverImange);
        holder.titleText.setText(bookData.getTitle());

        //holder.resultDetailLayout.setVisibility(bookData.isExpanded()?View.VISIBLE:View.GONE);




    }


    @Override
    public int getItemCount() {
        return bookDatas.size();
    }




}