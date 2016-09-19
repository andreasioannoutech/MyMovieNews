package com.kikkos.myMovieNews;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.kikkos.myMovieNews.data.Movie;
import com.kikkos.myMovieNews.data.MovieContract;
import com.kikkos.myMovieNews.sync.MyMovieNewsSyncAdapter;

import java.util.ArrayList;

/**
 * Created by kikkos on 6/23/2016.
 */
public class MainFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private MainRecyclerAdapter mRecyclerAdapter;
    private RecyclerView rv;

    ArrayList<Movie> myMovies;

    private String mSort_by;
    private String mPopular;

    private static final int MOVIES_LOADER = 100;

    private static final String[] MOVIES_COLUMNS = {
            MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_POSTER_PATH,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_POPULARITY,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_FAVOURITE,
            MovieContract.MovieEntry.COLUMN_IS_POPULAR,
            MovieContract.MovieEntry.COLUMN_IS_TOP_RATED
    };

    static final int COL_ID = 0;
    static final int COL_MOVIE_ID = 1;
    static final int COL_POSTER_PATH = 2;
    static final int COL_TITLE = 3;
    static final int COL_RATING = 4;
    static final int COL_POPULARITY = 5;
    static final int COL_RELEASE_DATE = 6;
    static final int COL_OVERVIEW = 7;
    static final int COL_FAVOURITE = 8;
    static final int COL_IS_POPULAR = 9;
    static final int COL_IS_TOP_RATED = 10;

    // variables for favourite movie usage.
    private String showFavString; // capturing the string from values\strings.xml file within the onCreateView bellow.
    private String hideFavString;
    private boolean favouriteIsEnabled = false; // variable for defining the view of favourite movies or not.

    public MainFragment(){

    }

    // creating a callback interface.
    public interface Callback{
        // public method for passing the movie id from here to main activity.
        public void onItemSelected(int id);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // Making the Menu buttons of this Fragment visible by inflating them.
        inflater.inflate(R.menu.mainfragmentmenu, menu);
        // based on our flag i change the favourites menu item title.
        if (favouriteIsEnabled){
            menu.findItem(R.id.action_favourites).setTitle(hideFavString);
        }else {
            menu.findItem(R.id.action_favourites).setTitle(showFavString);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Here i define the actions to be performed whenever a Menu button is clicked.
        if (item.getItemId() == R.id.action_settings){
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_refresh){
            updateMovies();
            return true;
        }
        if (item.getItemId() == R.id.action_favourites) {
            if (favouriteIsEnabled){
                item.setTitle(showFavString);
                favouriteIsEnabled = false;
            }else {
                item.setTitle(hideFavString);
                favouriteIsEnabled = true;
            }
            // after setting the flag and the title of the menu item i restart the loader in order to show the fav movies
            // if the flag says so.
            getLoaderManager().restartLoader(MOVIES_LOADER, null,this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // I Here i tell the Fragment Activity that it has a Menu button to show.
        setHasOptionsMenu(true);
        mPopular = getContext().getString(R.string.pref_sort_by_default);
        mSort_by = Utilities.getSortingOption(getContext());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // here i am saving the state of the favourites flag value in order to know in what to show
        // in case of screen rotation or if the fragment goes to back stack, etc..
        outState.putBoolean("favourites_enabled", favouriteIsEnabled);
        getLoaderManager().destroyLoader(MOVIES_LOADER);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Specify the layout to show all the things our Main Fragment has to show.
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // getting the string for both states of favourites menu item.
        hideFavString = getContext().getString(R.string.action_hide_favourites);
        showFavString = getContext().getString(R.string.action_show_favourites);

        // creating recycler view
        rv = (RecyclerView) rootView.findViewById(R.id.main_recycler_view);
        // creating an gridview autofit column custom class layout manager to pass in the rv.
        GridAutoFitLayoutManager layoutManager = new GridAutoFitLayoutManager(getContext(), 350);
        rv.setLayoutManager(layoutManager);
        mRecyclerAdapter = new MainRecyclerAdapter(getActivity());
        rv.setAdapter(mRecyclerAdapter);

        // Return the view with the modifications on it.
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // restoring the favourites flag.
        if (savedInstanceState != null && savedInstanceState.containsKey("favourites_enabled")){
            favouriteIsEnabled = savedInstanceState.getBoolean("favourites_enabled");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // here is a check and loader init for when main fragment restore from backstack.
        String sort_by = Utilities.getSortingOption(getContext());
        if (sort_by != null && !sort_by.equals(mSort_by)){
            updateMovies();
            mSort_by = sort_by;
        }
        getLoaderManager().initLoader(MOVIES_LOADER, null, this);
    }

    // Here I created a public Method that gets the users Preferences from the Menu Options
    // and then call the FetchMovieNews (download data from Cloud) method and pass those options in.
    public void updateMovies(){
        // starting the sync adapter, to download new movies.
        MyMovieNewsSyncAdapter.setMode(MyMovieNewsSyncAdapter.SYNC_MAIN_MODE);
        MyMovieNewsSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // quering the SQL movies table.
        switch (id){
            case MOVIES_LOADER:{
                // quering the db to get movies based on the sorting options or
                // if the favourites option is enables then i query the db for all favourite movies.
                if (favouriteIsEnabled){
                    String whereClause = MovieContract.MovieEntry.COLUMN_FAVOURITE + " = ?";
                    String[] whereArgs = new String[]{String.valueOf(1)};
                    return new CursorLoader(getActivity(), MovieContract.MovieEntry.CONTENT_URI, MOVIES_COLUMNS, whereClause, whereArgs, null);
                }else {
                    if (mSort_by.equals(mPopular)){
                        String whereClause = MovieContract.MovieEntry.COLUMN_IS_POPULAR + " = ?";
                        String[] whereArgs = new String[]{String.valueOf(1)};
                        return new CursorLoader(getActivity(), MovieContract.MovieEntry.CONTENT_URI, MOVIES_COLUMNS, whereClause, whereArgs, null);
                    }else {
                        String whereClause = MovieContract.MovieEntry.COLUMN_IS_TOP_RATED + " = ?";
                        String[] whereArgs = new String[]{String.valueOf(1)};
                        return new CursorLoader(getActivity(), MovieContract.MovieEntry.CONTENT_URI, MOVIES_COLUMNS, whereClause, whereArgs, null);
                    }
                }
            }
            default:
                break;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()){
            case MOVIES_LOADER:{
                // here i check if the the SQLite db is empty e.g. the first time you install the app it will be empty until
                // you run the updateMovies() method to download movies from the web, or in the scenario of changing sorting preference
                // in which case i delete all previous sort pref movies but the favourite ones so we need to update again to get the new ones.
                if (!favouriteIsEnabled && data.getCount() < 20){
                    updateMovies();
                    break;
                }
                if (myMovies == null){
                    myMovies = new ArrayList<Movie>();
                }else {
                    myMovies.clear();
                }
                // get movies from the cursor into the movie variable array list
                while (data.moveToNext()){
                    Movie movie = new Movie();
                    movie.setId(data.getInt(COL_MOVIE_ID));
                    movie.setPosterPath(data.getString(COL_POSTER_PATH));
                    movie.setTitle(data.getString(COL_TITLE));
                    movie.setRating(data.getDouble(COL_RATING));
                    movie.setPopularity(data.getDouble(COL_POPULARITY));
                    movie.setReleaseDate(data.getString(COL_RELEASE_DATE));
                    movie.setOverview(data.getString(COL_OVERVIEW));
                    movie.setFavourite(data.getInt(COL_FAVOURITE)>0);
                    movie.setIsPopular(data.getInt(COL_IS_POPULAR)>0);
                    movie.setIsTopRated(data.getInt(COL_IS_TOP_RATED)>0);

                    myMovies.add(movie);
                }
                // --- Adding the movies to the recyclerview adapter ---
                if (myMovies != null){
                    mRecyclerAdapter.swapItems(myMovies);
                }else {
                    Toast.makeText(getContext(), "Something went wrong, Please check your Internet connectivity and try again!", Toast.LENGTH_LONG).show();
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
            case MOVIES_LOADER:{
                if (myMovies == null){
                    myMovies = new ArrayList<Movie>();
                }else {
                    myMovies.clear();
                }
                break;
            }
            default:
                break;
        }
    }
}
