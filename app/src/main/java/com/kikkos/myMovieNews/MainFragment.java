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

    private static final int MOVIES_LOADER = 100;

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

    // variables for favouriting a movie usage.
    private String showFavString; // capturing the string from values\strings.xml file within the onCreateView bellow.
    private String hideFavString;
    private boolean favouriteIsEnabled = false; // variable for defining the view of favourite movies or not.

    public MainFragment(){

    }

    // creating a callback interface so that i can use to pass data (movie id) from here back to the activity.
    public interface Callback{
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
        if (savedInstanceState != null){
            if (savedInstanceState.containsKey("favourites_enabled")){
                favouriteIsEnabled = savedInstanceState.getBoolean("favourites_enabled");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // initiating the loader to read the movies from the SQL and pass them into the recycler view.
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
                return new CursorLoader(getActivity(), MovieContract.MovieEntry.CONTENT_URI, MOVIES_COLUMNS, null, null, null);
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
                    movie.setReleaseDate(data.getString(COL_RELEASE_DATE));
                    movie.setOverview(data.getString(COL_OVERVIEW));
                    movie.setFavourite(data.getInt(COL_FAVOURITE)>0);

                    // here is were i use the favourites flag variable
                    // if the option is enables then i add into the myMovies ArraList of movie objects
                    // only the movie objects that have the favourite value set to true which have been previously read from the SQL.
                    if (favouriteIsEnabled){
                        if (movie.getFavourite()){
                            myMovies.add(movie);
                        }
                    }else {
                        myMovies.add(movie);
                    }
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
