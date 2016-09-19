package com.kikkos.myMovieNews;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.kikkos.myMovieNews.data.MovieContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by kikkos on 9/12/2016.
 */
public class Utilities {

    public static final int SELECTION_VIDEOS = 1;
    public static final int SELECTION_REVIEWS = 2;

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

    // Get sort option value from the preferences.
    public static String getSortingOption(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.prefs_sort_by_key), context.getString(R.string.pref_sort_by_default));
    }

    // Construct the URL for the MOVIES DISCOVERY query
    public static Uri buildMoviesDiscoveryUri(Context context, String sort_by){
        String api_id = context.getString(R.string.api_key_text);
        final String MOVIES_DISCOVERY_BASE_URL = "https://api.themoviedb.org/3/movie/";
        final String API_ID_PARAM = "api_key";

        return Uri.parse(MOVIES_DISCOVERY_BASE_URL).buildUpon()
                .appendEncodedPath(sort_by)
                .appendQueryParameter(API_ID_PARAM, api_id)
                .build();
    }

    // Construct the URL for specific Movie Trailer or Review data.
    public static Uri buildMovieTrailerReviewUri(Context context, int id, int mode){
        String mMode;
        // checking which data does the user selected to download.
        switch (mode){
            case SELECTION_VIDEOS:
                mMode = "videos";
                break;
            case SELECTION_REVIEWS:
                mMode = "reviews";
                break;
            default:
                throw new UnsupportedOperationException("Unknown mode selected");
        }
        String api_id = context.getString(R.string.api_key_text);
        final String MOVIES_DISCOVERY_BASE_URL = "https://api.themoviedb.org/3/movie/";
        final String API_ID_PARAM = "api_key";

        return Uri.parse(MOVIES_DISCOVERY_BASE_URL).buildUpon()
                .appendPath(String.valueOf(id))
                .appendPath(mMode)
                .appendQueryParameter(API_ID_PARAM, api_id)
                .build();
    }

    // Method used to extract and organize the data gathered from API within a JSONObject Variable
    // and store them in the SQLite database locally.
    public static void storeNewMoviesLocally(Context context, JSONObject moviesJson)
            throws JSONException {

        // Create variables with the JSON keys that have to be extracted
        final String RESULTS = "results";
        final String POSTER_PATH = "poster_path";
        final String OVERVIEW = "overview";
        final String RELEASE_DATE = "release_date";
        final String MOVIE_ID = "id";
        final String ORIGINAL_TITLE = "title";
        final String VOTE_AVERAGE = "vote_average";
        final String POPULARITY = "popularity";

        String sort_by = getSortingOption(context);
        String popular = context.getString(R.string.pref_sort_by_default);

        // Splitting into JSON objects and arrays
        JSONArray moviesJsonArray = moviesJson.getJSONArray(RESULTS);
        JSONObject movieJsonObject;

        Vector<ContentValues> cVVector = new Vector<ContentValues>(moviesJsonArray.length());

        ArrayList<String> movies_id_list = new ArrayList<String>();

        for(int i = 0; i < moviesJsonArray.length(); i++){
            ContentValues movieValues = new ContentValues();
            movieJsonObject = moviesJsonArray.getJSONObject(i);

            movieValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movieJsonObject.getInt(MOVIE_ID));
            movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, movieJsonObject.getString(POSTER_PATH));
            movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, movieJsonObject.getString(ORIGINAL_TITLE));
            movieValues.put(MovieContract.MovieEntry.COLUMN_RATING, movieJsonObject.getDouble(VOTE_AVERAGE));
            movieValues.put(MovieContract.MovieEntry.COLUMN_POPULARITY, movieJsonObject.getDouble(POPULARITY));
            movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, movieJsonObject.getString(RELEASE_DATE));
            movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, movieJsonObject.getString(OVERVIEW));
            movieValues.put(MovieContract.MovieEntry.COLUMN_FAVOURITE, false);

            if (sort_by.equals(popular)){
                movieValues.put(MovieContract.MovieEntry.COLUMN_IS_POPULAR, true);
                movieValues.put(MovieContract.MovieEntry.COLUMN_IS_TOP_RATED, false);
            }else {
                movieValues.put(MovieContract.MovieEntry.COLUMN_IS_POPULAR, false);
                movieValues.put(MovieContract.MovieEntry.COLUMN_IS_TOP_RATED, true);
            }

            movies_id_list.add(String.valueOf(movieJsonObject.getInt(MOVIE_ID)));

            cVVector.add(movieValues);
        }

        // Remove all non-Favourite movies from SQLite so that we get fresh movies in our db.
        removeNonFavouriteMoviesLocally(context);

        // comparing the movies just downloaded if they already exist in our SQL db.
        // if they exist in SQL then they are not written again.
        cVVector = removeAlreadySavedMovies(context, movies_id_list, cVVector, sort_by);

        int inserted = 0;
        if (cVVector.size() > 0){
            // adding new downloaded movies in SQLite.
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            inserted = context.getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, cvArray);
            Toast.makeText(context, context.getString(R.string.new_movies_downloaded_msg), Toast.LENGTH_SHORT).show();
        }
    }

    // Here is the method were i compare the downloaded movie ids with the movies already in the SQL.
    // if they already exist within the SQL then the query will find it and i compare the integer id from the data cursor.
    // if it has a match then i remove it from the Vector<ContentValues> and return it.
    public static Vector<ContentValues> removeAlreadySavedMovies(Context context, ArrayList<String> movies_id_list, Vector<ContentValues> cVVector, String sort_by){

        String whereClause = MovieContract.MovieEntry.COLUMN_MOVIE_ID + " IN (" + makeQueryPlaceholders(movies_id_list.size()) + ")";
        String[] whereArgs = movies_id_list.toArray(new String[movies_id_list.size()]);

        Cursor data = context.getContentResolver().query(MovieContract.MovieEntry.CONTENT_URI, MOVIES_COLUMNS, whereClause, whereArgs, null);

        if (data != null) {
            while (data.moveToNext()){
                for (int i = 0; i < cVVector.size(); i++){
                    int movie_id_from_cv = cVVector.get(i).getAsInteger(MovieContract.MovieEntry.COLUMN_MOVIE_ID);
                    if (movie_id_from_cv == data.getInt(COL_MOVIE_ID)){
                        // here i handling movies fetched from internet based on a sorting option so because the movie found to be already in SQL
                        // i have to change the sorting status of the matched movie in the SQLite before i remove it from the
                        // movies to be written in db array. In this way i make sure that it will be displayed in the main view
                        // if its sorting status matches the query.
                        // IF ANY MOVIE IS STILL IN THE SQL DB BUT WAS NOT MATCHED HERE IT MEANS IT ONLY EXISTS IN A DIFFERENT
                        // SORTING OPTION COLLECTION SO WE LET ITS SORTING OPTION AS IT IS.
                        swapSortingStatusLocally(context, movie_id_from_cv, sort_by);
                        cVVector.remove(i);
                        i = 0;
                    }
                }
            }
            data.close();
        }
        return cVVector;
    }

    // Here is a method i found online that can help dynamically add the placeholder within a SQLite db query.
    public static String makeQueryPlaceholders(int length){
        if (length < 1){
            throw new RuntimeException("No Placeholders for performing SQL Query.");
        }else {
            StringBuilder sb = new StringBuilder(length * 2 - 1);
            sb.append("?");
            for (int i = 1; i < length; i++){
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    // here i change the status of isTopRated and isPopular based on the sorting preference.
    public static void swapSortingStatusLocally(Context context, int id, String sort_by){
        String popular = context.getString(R.string.pref_sort_by_default);
        ContentValues cv = new ContentValues();
        if (sort_by.equals(popular)){
            cv.put(MovieContract.MovieEntry.COLUMN_IS_POPULAR, true);
            cv.put(MovieContract.MovieEntry.COLUMN_IS_TOP_RATED, false);
        }else {
            cv.put(MovieContract.MovieEntry.COLUMN_IS_POPULAR, false);
            cv.put(MovieContract.MovieEntry.COLUMN_IS_TOP_RATED, true);
        }
        String whereClause = MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(id)};
        context.getContentResolver().update(MovieContract.MovieEntry.CONTENT_URI, cv, whereClause, whereArgs);
    }

    // delete all movies in the SQLite db.
    public static void clearMoviesLocally(Context context){
        context.getContentResolver().delete(MovieContract.MovieEntry.CONTENT_URI, null, null);
    }

    // deleting all non-favourite movies from SQLite
    public static void removeNonFavouriteMoviesLocally(Context context){
        String whereClause = MovieContract.MovieEntry.COLUMN_FAVOURITE + " = ?";
        String[] whereArgs = new String[]{String.valueOf(0)};

        context.getContentResolver().delete(MovieContract.MovieEntry.CONTENT_URI, whereClause, whereArgs);
    }

    // here i store all the trailers downloaded into an SQL table used as a CACHE for grabbing, viewing and deleting
    // all the trailers.
    public static void storeTrailerUrlsLocally(Context context, JSONObject videos){
        final String YOUTUBE_BASE_URL = "https://www.youtube.com/watch";

        try {
            JSONArray array = videos.getJSONArray("results");
            if (array.length() > 0){
                Vector<ContentValues> cVVector = new Vector<ContentValues>(array.length());
                for (int i = 0; i<array.length(); i++){
                    ContentValues cv = new ContentValues();
                    JSONObject trailer = array.getJSONObject(i);
                    cv.put(MovieContract.TrailerEntry.COLUMN_URL, Uri.parse(YOUTUBE_BASE_URL).buildUpon().appendQueryParameter("v", trailer.getString("key")).build().toString());

                    cVVector.add(cv);
                }
                int inserted = 0;
                if (cVVector.size() > 0){
                    // Clearing Trailers table.
                    context.getContentResolver().delete(MovieContract.TrailerEntry.CONTENT_URI, null, null);
                    // storing new trailer url's
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    inserted = context.getContentResolver().bulkInsert(MovieContract.TrailerEntry.CONTENT_URI, cvArray);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // here i store the reviews url into another table in SQL, which is used the same way as the trailers one, like a CACHE.
    public static void storeReviewUrlsLocally(Context context, JSONObject reviews){
        try {
            JSONArray array = reviews.getJSONArray("results");
            if (array.length() > 0){
                Vector<ContentValues> cVVector = new Vector<ContentValues>(array.length());
                for (int i = 0; i<array.length(); i++){
                    ContentValues cv = new ContentValues();
                    JSONObject review = array.getJSONObject(i);
                    cv.put(MovieContract.ReviewEntry.COLUMN_URL, Uri.parse(review.getString("url")).toString());

                    cVVector.add(cv);
                }
                int inserted = 0;
                if (cVVector.size() > 0){
                    // Clearing Reviews table.
                    context.getContentResolver().delete(MovieContract.ReviewEntry.CONTENT_URI, null, null);
                    // storing new reviews url's
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    inserted = context.getContentResolver().bulkInsert(MovieContract.ReviewEntry.CONTENT_URI, cvArray);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Method for updating the favourite column of the movie within the our SQL local collection of movies.
    // just setting the value to true and false (1 and 0 in SQL).
    public static void setFavouriteSetting(final Context context, int id, final boolean status){
        if (id != -1){
            ContentValues cv = new ContentValues();
            cv.put(MovieContract.MovieEntry.COLUMN_FAVOURITE, status);

            String whereClause = MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?";
            String[] whereArgs = new String[]{String.valueOf(id)};

            AsyncQueryHandler queryHandler = new AsyncQueryHandler(context.getContentResolver()) {
                @Override
                protected void onUpdateComplete(int token, Object cookie, int result) {
                    if (status){
                        Toast.makeText(context, context.getString(R.string.add_favourites), Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(context, context.getString(R.string.remove_favourites), Toast.LENGTH_SHORT).show();
                    }
                }
            };
            queryHandler.startUpdate(1, null, MovieContract.MovieEntry.CONTENT_URI, cv,whereClause, whereArgs);
        }
    }

    // Construct Poster Url path.
    public static String assemblePosterUrl(Context c, String path){
        return c.getString(R.string.poster_path_base_url) + path;
    }
}
