package com.kikkos.myMovieNews;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kikkos on 9/14/2016.
 */
public class DetailsRecyclerAdapter extends RecyclerView.Adapter<DetailsRecyclerAdapter.ViewHolder>{

    // variable for storing all URI's, both trailer and review URI's.
    // then i use this into the recyclerview bellow to set the intents for each item.
    private ArrayList<Uri> mUrisList = new ArrayList<Uri>();
    private ArrayList<Uri> trailers = new ArrayList<Uri>();
    private ArrayList<Uri> reviews = new ArrayList<Uri>();

    // with this variable i hold the position of the last trailer Uri within the mUrisList ArrayList<Uri> variable above.
    private int lastTrailerPosition;
    public static final int TYPE_TRAILER = 100;
    public static final int TYPE_REVIEW = 200;

    private Context mContext;
    private int mBackground;
    private final TypedValue mTypedValue = new TypedValue();

    public static class ViewHolder extends RecyclerView.ViewHolder{

        public final View mView;
        public final TextView mTextView;
        public final ImageView mImageView;

        public ViewHolder(View view){
            super(view);
            mView = view;
            mTextView = (TextView) view.findViewById(R.id.details_list_item_textview);
            mImageView = (ImageView) view.findViewById(R.id.details_list_item_imageview);
        }
    }

    public DetailsRecyclerAdapter(Context context){
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mContext = context;
    }

    public void swapItems(ArrayList<Uri> trailersList, ArrayList<Uri> reviewsList){
        // adding the Uris from the fragment to the adapter.
        if (trailersList != null && trailers.size() == 0){
            trailers = trailersList;
        }
        if (reviewsList != null && reviews.size() == 0){
            reviews = reviewsList;
        }
        createList();
    }

    private void createList(){
        // this will compine as said earlier the trailers and reviews uri lists within the mUrisList
        // variable and notify the adapter that data has changed in order to display them on screen.
        // this though will only happen once both trailer and review data have been passed into the adapter.
        // by doing so i secure that in the combined list i store in the first positions the trailer uris and after that the review uris.
        if (trailers.size() > 0 && reviews.size() > 0 && mUrisList.size() == 0){
            for (int i = 0; i < trailers.size(); i++){
                mUrisList.add(trailers.get(i));
            }
            for (int i = 0; i < reviews.size(); i++){
                mUrisList.add(reviews.get(i));
            }
            // here is were i set the last trailer position in the combined list of uris.
            lastTrailerPosition = trailers.size() - 1;
            notifyDataSetChanged();
        }
    }

    public void clearList(){
        mUrisList.clear();
    }

    private int getItemType(int position){
        // with this helper function i identify what type is the current uri in the combined list
        // is it a trailer or a review uri.
        if (mUrisList.size() > 0){
            if (position <= lastTrailerPosition){
                return TYPE_TRAILER;
            }else {
                return TYPE_REVIEW;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        if (mUrisList.size() > 0){
            return mUrisList.size();
        }
        return 0;
    }

    @Override
    public DetailsRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_details_item, parent, false);
        view.setBackgroundResource(mBackground);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DetailsRecyclerAdapter.ViewHolder holder, final int position) {
        // here i check each uri in the list for its type as mentioned above and act accordingly.
            switch (getItemType(position)){
                case TYPE_TRAILER:{
                    int i = position + 1;
                    holder.mTextView.setText(mContext.getString(R.string.trailers_text) + i);
                    holder.mImageView.setImageResource(R.drawable.trailers_icon);
                    break;
                }
                case TYPE_REVIEW:{
                    int i = position - lastTrailerPosition;
                    holder.mTextView.setText(mContext.getString(R.string.reviews_text) + i);
                    holder.mImageView.setImageResource(R.drawable.reviews_icon);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unknown Item Type");
            }

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // set intent
                    Intent intent = new Intent(Intent.ACTION_VIEW, mUrisList.get(position));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // verify that there is app to get the implicit intent.
                    PackageManager packageManager = mContext.getPackageManager();
                    List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
                    boolean isIntentSafe = activities.size() > 0;
                    if (isIntentSafe){
                        // start intent
                        mContext.startActivity(intent);
                    }
                }
            });
    }
}