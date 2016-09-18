package com.kikkos.myMovieNews;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kikkos.myMovieNews.data.MovieContract;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by kikkos on 9/14/2016.
 */
public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // just a TAG so that i know the arguments is for details activity.
    static final String DETAIL_URI = "URI";

    public static final String LOG_TAG = DetailsFragment.class.getSimpleName();

    // SQL data change observers.
    private ContentObserver mTrailersObserver;
    private ContentObserver mReviewsObserver;

    ArrayList<Uri> trailers_uri_list = new ArrayList<Uri>();
    ArrayList<Uri> reviews_uri_list = new ArrayList<Uri>();

    // loader identification variables.
    private static final int TRAILERS_LOADER = 10;
    private static final int REVIEWS_LOADER = 20;
    private static final int DETAILS_LOADER = 30;

    ImageView imageDetailView;
    TextView title;
    TextView rating;
    TextView date;
    TextView fav_text;
    TextView overview;
    RecyclerView rv;
    // checkbox to be used for the favourites action.
    CheckBox star;

    private DetailsRecyclerAdapter mRecyclerAdapter;
    int movie_id = -1;

    private static final String[] MOVIES_COLUMNS = {
            MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_POSTER_PATH,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_FAVOURITE
    };

    static final int COL_ID = 0;
    static final int COL_MOVIE_ID = 1;
    static final int COL_POSTER_PATH = 2;
    static final int COL_TITLE = 3;
    static final int COL_RATING = 4;
    static final int COL_RELEASE_DATE = 5;
    static final int COL_OVERVIEW = 6;
    static final int COL_FAVOURITE = 7;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_details, container, false);

        // i get the data passed from main activity via intent which is only the movie id as integer, as i will do query the SQL
        // db again to get all the data. (just practising the loaders. :))
        getMovie_id(getActivity());
        // initializing variables
        imageDetailView = (ImageView) rootView.findViewById(R.id.detail_image_view);
        title = (TextView) rootView.findViewById(R.id.txtOriginalTitle);
        rating = (TextView) rootView.findViewById(R.id.txtVoteAverage);
        date = (TextView) rootView.findViewById(R.id.txtReleaseDate);
        overview = (TextView) rootView.findViewById(R.id.txtOverview);

        // setting up the favourites star check button.
        fav_text = (TextView) rootView.findViewById(R.id.fav_textview);
        star = (CheckBox) rootView.findViewById(R.id.details_star);
        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // passing the movie id into the helper method so that i can proceed with marking it
                // as a favourite with the checkbox checked status.
                Utilities.setFavouriteSetting(getContext(), movie_id, star.isChecked());
            }
        });

        // setting up recycler view.
        rv = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        GridAutoFitLayoutManager layoutManager = new GridAutoFitLayoutManager(getContext(), 650);
        rv.setLayoutManager(layoutManager);
        mRecyclerAdapter = new DetailsRecyclerAdapter(getContext());
        rv.setAdapter(mRecyclerAdapter);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // starting the loaders!
        getLoaderManager().initLoader(DETAILS_LOADER, null, this);

        // here i do a check whether i have any movie id save with the fragment and if so i grab it and check it with the current movie id that i am view the details for.
        // if the id is not found then i start the SQL observers for trailers and reviews to wait for syncadapter to download the reviews and trailers.
        // a callback is then created when the data are downloaded and written in the SQL and then i can init the loader to read the data.
        // If the id matches the current one it means a possible screen rotation, or backstack happened and hence we do not need to re-create the observers but only init the loaders.
        // if we again re-create the observers then no callback will happen as no new data will be downloaded. Data are already in SQL.
        if (savedInstanceState != null && savedInstanceState.containsKey("previous_movie_id") && savedInstanceState.getInt("previous_movie_id") == movie_id){
            getLoaderManager().initLoader(TRAILERS_LOADER, null, DetailsFragment.this);
            getLoaderManager().initLoader(REVIEWS_LOADER, null, DetailsFragment.this);
        }else {
            mTrailersObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    // get trailer data from SQLite
                    getLoaderManager().initLoader(TRAILERS_LOADER, null, DetailsFragment.this);
                    getContext().getContentResolver().unregisterContentObserver(mTrailersObserver);
                }
            };
            getContext().getContentResolver().registerContentObserver(MovieContract.TrailerEntry.CONTENT_URI, false, mTrailersObserver);

            mReviewsObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    // get review data from SQLite
                    getLoaderManager().initLoader(REVIEWS_LOADER, null, DetailsFragment.this);
                    getContext().getContentResolver().unregisterContentObserver(mReviewsObserver);
                }
            };
            getContext().getContentResolver().registerContentObserver(MovieContract.ReviewEntry.CONTENT_URI, false, mReviewsObserver);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // saving the current movie id for usage with the observers as described above.
        if (movie_id != -1){
            outState.putInt("previous_movie_id", movie_id);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getLoaderManager().destroyLoader(TRAILERS_LOADER);
        getLoaderManager().destroyLoader(REVIEWS_LOADER);
        getLoaderManager().destroyLoader(DETAILS_LOADER);
        // checking the observes if still there to remove them.
        if (mTrailersObserver != null){
            getContext().getContentResolver().unregisterContentObserver(mTrailersObserver);
        }
        if (mReviewsObserver != null){
            getContext().getContentResolver().unregisterContentObserver(mReviewsObserver);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // here i do use a check on the loaders id and performing a different query for each purpose.
        switch (id){
            case TRAILERS_LOADER:
                return new CursorLoader(getActivity(), MovieContract.TrailerEntry.CONTENT_URI, null, null, null, null);
            case REVIEWS_LOADER:
                return new CursorLoader(getActivity(), MovieContract.ReviewEntry.CONTENT_URI, null, null, null, null);
            case DETAILS_LOADER:{
                if (movie_id != -1){
                    String whereClause = MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?";
                    String[] whereArgs = new String[]{String.valueOf(movie_id)};
                    return new CursorLoader(getActivity(), MovieContract.MovieEntry.CONTENT_URI, MOVIES_COLUMNS, whereClause, whereArgs, null);
                }
                break;
            }
            default:
                break;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        switch (loader.getId()){
            case TRAILERS_LOADER:{
                while (data.moveToNext()){
                    trailers_uri_list.add(Uri.parse(data.getString(1)));
                }
                mRecyclerAdapter.swapItems(trailers_uri_list, null);
                break;
            }
            case REVIEWS_LOADER:{
                while (data.moveToNext()){
                    reviews_uri_list.add(Uri.parse(data.getString(1)));
                }
                mRecyclerAdapter.swapItems(null, reviews_uri_list);
                break;
            }
            case DETAILS_LOADER:{
                if (!data.moveToFirst()){
                    return;
                }
                if (imageDetailView != null){
                    imageDetailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageDetailView.setAdjustViewBounds(true);
                    Picasso.with(getActivity()).load(Utilities.assemblePosterUrl(getContext(), data.getString(COL_POSTER_PATH))).resize(450, 600).into(imageDetailView);
                }
                if (title != null)
                    title.setText(data.getString(COL_TITLE));
                if (rating != null)
                    rating.setText(getContext().getString(R.string.rating_details_text) + data.getDouble(COL_RATING));
                if (date != null)
                    date.setText(getContext().getString(R.string.release_date_details_text) + data.getString(COL_RELEASE_DATE));
                if (overview != null)
                    overview.setText(getContext().getString(R.string.overview_details_text) + data.getString(COL_OVERVIEW));
                if (star != null){
                    boolean status = data.getInt(COL_FAVOURITE) > 0;
                    star.setChecked(status);
                    star.setVisibility(View.VISIBLE);
                }
                if (fav_text != null){
                    fav_text.setText(getContext().getString(R.string.favourites_text));
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()){
            case TRAILERS_LOADER: {
                if (trailers_uri_list == null){
                    trailers_uri_list = new ArrayList<Uri>();
                }else {
                    trailers_uri_list.clear();
                }
                break;
            }
            case REVIEWS_LOADER:{
                if (reviews_uri_list == null){
                    reviews_uri_list = new ArrayList<Uri>();
                }else {
                    reviews_uri_list.clear();
                }
                break;
            }
            case DETAILS_LOADER:{
                imageDetailView = null;
                title = null;
                rating = null;
                date = null;
                overview = null;
                star.setChecked(false);
                break;
            }
            default:
                break;
        }
    }

    public void getMovie_id(Activity activity){
        // getting the movie id from the arguments passed to the fragment
        // both from the details activity in the 1 pane scenario or
        // the main activity in the 2 pane scenario.
        Bundle args = getArguments();
        if (args != null){
            movie_id = args.getInt(DetailsFragment.DETAIL_URI);
        }else {
            Log.v(LOG_TAG, "ERROR NULL ARGUMENT: COULD NOT GET MOVIE ID FROM MAIN ACTIVITY.");
            Toast.makeText(activity, "Something went wrong with this movie. Please go back to the main screen and try again!", Toast.LENGTH_LONG).show();
        }
    }
}
