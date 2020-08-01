package comlechchu.github.piaoshu;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private ArrayList<ArrayList<String>> mData;
    private int selectedItem;

    MyAdapter(ArrayList<ArrayList<String>> data) {

        mData = data;
    }

    // 建立ViewHolder
    class ViewHolder extends RecyclerView.ViewHolder{
        // 宣告元件
        private TextView txtItem;

        ViewHolder(View itemView) {
            super(itemView);
            txtItem = (TextView) itemView.findViewById(R.id.txtItem);

            txtItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    txtItem.setTextColor(Color.parseColor("#FF0000"));
                }
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chpaterlist_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        //TODO 速度太慢
        // 設置txtItem要顯示的內容
        holder.txtItem.setText(mData.get(position).get(0));

        if (selectedItem == position) {
            //holder.txtItem.setTextColor(Color.parseColor("#ff0000"));
        }


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int previousItem = selectedItem;
                selectedItem = position;

                notifyItemChanged(previousItem);
                notifyItemChanged(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }
}