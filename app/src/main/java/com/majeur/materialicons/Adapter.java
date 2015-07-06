package com.majeur.materialicons;

import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private String[] mAssetsFiles;
    private MainActivity mActivity;
    private ItemsClickListener mListener;

    private List<Integer> mSelectedItems = new ArrayList<>();

    private int mSelectedCardColor;

    public Adapter(MainActivity mainActivity, ItemsClickListener listener) {
        mActivity = mainActivity;
        mListener = listener;

        mSelectedCardColor = mainActivity.getResources().getColor(R.color.primary_light);
    }

    public void setAssetsFiles(String[] assetsFiles) {
        mAssetsFiles = assetsFiles;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(mActivity).inflate(R.layout.recycler_item, viewGroup, false);
        return new ViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        String fileName = mAssetsFiles[i];

        viewHolder.titleView.setText(Utils.svgFileNameToLabel(fileName));
        setCardViewSelected(viewHolder.view, mSelectedItems.contains(i));

        viewHolder.iconView.setImageDrawable(Utils.getDrawableForSvg(mActivity.getAssets(), fileName));
    }

    private void setCardViewSelected(CardView cardView, boolean selected) {
        cardView.setCardBackgroundColor(selected ? mSelectedCardColor : Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return mAssetsFiles == null ? 0 : mAssetsFiles.length;
    }

    public void toggleSelected(Integer position) {
        if (mSelectedItems.contains(position))
            mSelectedItems.remove(position);
        else
            mSelectedItems.add(position);

        notifyItemChanged(position);
    }

    public void clearSelection() {
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemsCount() {
        return mSelectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        public TextView titleView;
        public ImageView iconView;
        public CardView view;
        private ItemsClickListener mListener;

        public ViewHolder(View itemView, ItemsClickListener listener) {
            super(itemView);
            view = (CardView) itemView;
            titleView = (TextView) itemView.findViewById(R.id.name);
            iconView = (ImageView) itemView.findViewById(R.id.icon);
            iconView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            mListener = listener;

            itemView.findViewById(R.id.button).setOnClickListener(mOverflowClickListener);
            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
        }

        private View.OnClickListener mOverflowClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onOverflowClick(v, getPosition());
            }
        };

        @Override
        public void onClick(View v) {
            mListener.onItemClick(v, getPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            return mListener.onItemLongClick(v, getPosition());
        }


    }

    public interface ItemsClickListener {

        public void onOverflowClick(View v, int i);

        public void onItemClick(View v, int i);

        public boolean onItemLongClick(View v, int i);
    }

}
