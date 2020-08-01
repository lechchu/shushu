package comlechchu.github.piaoshu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

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

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private ArrayList<NovelInfo> bookDatas;
    private NovelInfo bookData;
    private String bFName;
    private int selectedItem;
    Bitmap coverBmp;

    SearchAdapter(ArrayList<NovelInfo> data) {

        bookDatas = data;
    }

    // 建立ViewHolder
    class ViewHolder extends RecyclerView.ViewHolder {
        // 宣告元件
        private TextView titleText, authorText, descText;
        private ImageView coverImange;
        private CardView novelView;
        private ConstraintLayout resultDetailLayout;
        private Button readBtn, addFavBtn;

        ViewHolder(View itemView) {
            super(itemView);

            titleText = (TextView) itemView.findViewById(R.id.booknameTV);
            authorText = (TextView) itemView.findViewById(R.id.bookauthorTV);
            descText = (TextView) itemView.findViewById(R.id.bookdescTV);
            coverImange = (ImageView) itemView.findViewById(R.id.bookcoverIV);
            novelView = (CardView)itemView.findViewById(R.id.novelCV);
            resultDetailLayout = (ConstraintLayout)itemView.findViewById(R.id.expandlayout);
            readBtn = (Button)itemView.findViewById(R.id.startreadBtn);
            addFavBtn = (Button)itemView.findViewById(R.id.addfavoriteBtn);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NovelInfo ni = bookDatas.get(getAdapterPosition());
                    ni.setExpanded(!ni.isExpanded());
                    notifyItemChanged(getAdapterPosition());
                }
            });


            readBtn.setOnClickListener(new View.OnClickListener() {
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

            addFavBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<NovelInfo> favBooks = new ArrayList<NovelInfo>();

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

                    }catch (Exception e){e.printStackTrace();
                        Toast.makeText(v.getContext(),"加入失敗",Toast.LENGTH_LONG).show();
                    }
                }
            });

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.searchresult_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        bookData = bookDatas.get(position);
        Glide.with(holder.itemView).load(bookData.getCoverURL()).into(holder.coverImange);
        holder.titleText.setText(bookData.getTitle());
        holder.authorText.setText(bookData.getAuthor());
        holder.descText.setText(bookData.getDesc());
        holder.resultDetailLayout.setVisibility(bookData.isExpanded()?View.VISIBLE:View.GONE);




    }


    @Override
    public int getItemCount() {
        return bookDatas.size();
    }


}