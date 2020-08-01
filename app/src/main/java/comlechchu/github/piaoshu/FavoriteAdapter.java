package comlechchu.github.piaoshu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import androidx.cardview.widget.CardView;
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
        private CardView novelView;
        private ConstraintLayout resultDetailLayout;
        private Button readBtn;

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