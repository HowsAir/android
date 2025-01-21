package com.example.mborper.breathbetter.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mborper.breathbetter.R;

import java.util.List;

// Adapter class for ViewPager2
class PopupPagerAdapter extends RecyclerView.Adapter<PopupPagerAdapter.ViewHolder> {

    private final List<PageContent> pageContents;

    // Builder
    public PopupPagerAdapter(List<PageContent> pageContents) {
        this.pageContents = pageContents;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the page layout (page_item.xml) for each page of the ViewPager2
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PageContent content = pageContents.get(position);
        holder.bind(content); // Bind data (texts and icons) to the page views
        holder.itemView.requestLayout(); // Force the size adjustment
    }


    @Override
    public int getItemCount() {
        return pageContents.size();
    }

    // ViewHolder for each page
    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView text1, text2, text3, text4;
        private final ImageView icon1, icon2, icon3, icon4;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.page_title);
            text1 = itemView.findViewById(R.id.text1);
            text2 = itemView.findViewById(R.id.text2);
            text3 = itemView.findViewById(R.id.text3);
            text4 = itemView.findViewById(R.id.text4);
            icon1 = itemView.findViewById(R.id.icon1);
            icon2 = itemView.findViewById(R.id.icon2);
            icon3 = itemView.findViewById(R.id.icon3);
            icon4 = itemView.findViewById(R.id.icon4);
        }

        public void bind(PageContent content) {
            title.setText(content.getTitle());
            text1.setText(content.getTexts().get(0));
            text2.setText(content.getTexts().get(1));
            text3.setText(content.getTexts().get(2));
            text4.setText(content.getTexts().get(3));
            icon1.setImageResource(content.getIcons().get(0));
            icon2.setImageResource(content.getIcons().get(1));
            icon3.setImageResource(content.getIcons().get(2));
            icon4.setImageResource(content.getIcons().get(3));
        }
    }
}
