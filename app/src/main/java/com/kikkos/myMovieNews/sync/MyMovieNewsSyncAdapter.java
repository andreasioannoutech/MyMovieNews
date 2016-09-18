package com.kikkos.myMovieNews.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kikkos.myMovieNews.R;
import com.kikkos.myMovieNews.Utilities;
import com.kikkos.myMovieNews.data.MovieContract;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kikkos on 9/12/2016.
 */
public class MyMovieNewsSyncAdapter extends AbstractThreadedSyncAdapter {

    public final String LOG_TAG = MyMovieNewsSyncAdapter.class.getSimpleName();
    private RequestQueue requestQueue = Volley.newRequestQueue(getContext());

    // variables for filtering what network operation should be used.
    // i am using volley library to perform Network Ops.
    private static int mMode;
    public static final int SYNC_MAIN_MODE = 1;
    public static final int SYNC_DETAILS_MODE = 2;

    public static int MOVIE_ID;

    public MyMovieNewsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        // choosing which network operation to use for each time the syncadapter is initiated.
        switch (mMode){
            case SYNC_MAIN_MODE:
                String sort_by = Utilities.getSortingOption(getContext());
                if (sort_by == null){
                    return;
                }
                FetchMovies(sort_by);
                break;
            case SYNC_DETAILS_MODE:
                FetchVideo();
                FetchReviews();
                break;
            default:
                throw new UnsupportedOperationException("Unknown SyncAdapter Mode");
        }
        Log.v(LOG_TAG, " => onPerformSync executed.");
    }

    // here is set the mode flag which will determine the functions to be used in onPerformSync method.
    public static void setMode(int mode){
        mMode = mode;
    }

    // inserting the movie id so that i can use it to download reviews and trailers with the functions bellow.
    public static void insertMovieId(int id){
        MOVIE_ID = id;
    }

    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context){

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context){
        getSyncAccount(context);
    }

    // Performing a network request with the VOLLEY library for downloading all the movies from themoviedb.org using the
    // sorting option passed in the function.
    public void FetchMovies(String sort_by){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                Utilities.buildMoviesDiscoveryUri(getContext(), sort_by).toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // json object data are passed into a helper method for writing the movies into the SQLite db.
                            Utilities.storeNewMoviesLocally(getContext(), response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        jsonObjectRequest.setTag("FETCH_MOVIES");
        requestQueue.add(jsonObjectRequest);
    }

    // Performing a network request with the VOLLEY library for downloading all the trailers url from themoviedb.org using the
    // movie id passed in the function.
    public void FetchVideo(){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                // building the Uri for reviews.
                Utilities.buildMovieTrailerReviewUri(getContext(), MOVIE_ID, Utilities.SELECTION_VIDEOS).toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // writting the trailers in SQL
                        Utilities.storeTrailerUrlsLocally(getContext(), response);
                        // initiating the callback on the SQL observers in details activity and notifying
                        // that the data have been downloaded and stored in SQL successfully.
                        getContext().getContentResolver().notifyChange(MovieContract.TrailerEntry.CONTENT_URI, null, false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        jsonObjectRequest.setTag("FETCH_VIDEO");
        requestQueue.add(jsonObjectRequest);
    }

    // Performing a network request with the VOLLEY library for downloading all the reviews url from themoviedb.org using the
    // movie id passed in the function.
    public void FetchReviews(){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                // building the Uri for reviews.
                Utilities.buildMovieTrailerReviewUri(getContext(), MOVIE_ID, Utilities.SELECTION_REVIEWS).toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // writting the reviews in SQL.
                        Utilities.storeReviewUrlsLocally(getContext(), response);
                        // initiating the callback on the SQL observers in details activity and notifying
                        // that the data have been downloaded and stored in SQL successfully.
                        getContext().getContentResolver().notifyChange(MovieContract.ReviewEntry.CONTENT_URI, null, false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        jsonObjectRequest.setTag("FETCH_REVIEW");
        requestQueue.add(jsonObjectRequest);
    }
}
