package com.kikkos.myMovieNews;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kikkos.myMovieNews.data.Movie;
import com.kikkos.myMovieNews.sync.MyMovieNewsSyncAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by kikkos on 9/15/2016.
 */
public class MainRecyclerAdapter extends RecyclerView.Adapter<MainRecyclerAdapter.ViewHolder> {

    private ArrayList<Movie> movies = new ArrayList<Movie>();

    private Context mContext;
    private int mBackground;
    private final TypedValue mTypedValue = new TypedValue();

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final View mView;
        private final ImageView mImageView;

        public ViewHolder(View view){
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.main_list_item_imageview);
        }
    }

    public MainRecyclerAdapter(Context context){
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mContext = context;
    }

    public void swapItems(ArrayList<Movie> movieList){
        // Here i load all the movies into the recycler adapter for later use(download and show).
        movies = new ArrayList<Movie>(movieList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (movies != null){
            return movies.size();
        }else {
            return 0;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_main_item, parent, false);
        view.setBackgroundResource(mBackground);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        if (movies.size() > 0){
            // downloading the image and setting it into the imageview with picasso library.
            Picasso.with(mContext).load(this.movies.get(position).getPosterUrlString(mContext)).resize(350, 500).into(holder.mImageView);
            // Here i created an OnItemClickListener for all Items on the GridView
            // and specify the action to do whenever an Item is clicked
            // to firstly to initiate the download of the trailers and reviews and
            // then to create an intent and pass along with it the Items info
            // and then start the intented Activity.
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // download the trailers and reviews of the selected movie and store it in SQLite.
                    MyMovieNewsSyncAdapter.setMode(MyMovieNewsSyncAdapter.SYNC_DETAILS_MODE);
                    MyMovieNewsSyncAdapter.insertMovieId(movies.get(position).getId());
                    MyMovieNewsSyncAdapter.syncImmediately(mContext);

                    // creating a callback to the activity to pass in the selected movie's id.
                    ((MainFragment.Callback) mContext).onItemSelected( movies.get(position).getId());
                }
            });
        }
    }
}
