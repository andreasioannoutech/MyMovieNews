package com.kikkos.myMovieNews;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements MainFragment.Callback {

    boolean mTwoPane;
    private final static String DETAILFRAGMENT_TAG = "DFTAG";
    private final static String MAINFRAGMENT_TAG = "MFTAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null){
            MainFragment mainFragment = new MainFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_main_container_id, mainFragment, MAINFRAGMENT_TAG).commit();
        }
        // checking here if the details_activity_id layout exists --- should only exist in the sw600dp xml file which
        // is only for tablet mode. If exists then we set the 2 pane mode flag to true and we add the details fragment
        // into that layout.
        if (findViewById(R.id.details_activity_id) != null){
            mTwoPane = true;
            if (savedInstanceState == null){
                // adding default movie id value
                Bundle args = new Bundle();
                args.putInt(DetailsFragment.DETAIL_URI, -1);

                DetailsFragment detailsFragment = new DetailsFragment();
                detailsFragment.setArguments(args);

                getSupportFragmentManager().beginTransaction().replace(R.id.details_activity_id, detailsFragment, DETAILFRAGMENT_TAG).commit();
            }
        }else {
            mTwoPane = false;
        }
    }

    @Override
    public void onItemSelected(int id) {
        // here i get the movie id by the recycler view via the callback.
        // and based on the 2 pane flag i pass the movie id to the details fragment
        // via an intent if its on cellphone or via fragment argument if its on tablet with 2pane enabled.
        if (mTwoPane){
            Bundle args = new Bundle();
            args.putInt(DetailsFragment.DETAIL_URI, id);

            DetailsFragment detailsFragment = new DetailsFragment();
            detailsFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().replace(R.id.details_activity_id, detailsFragment, DETAILFRAGMENT_TAG).commit();
        }else {
            Intent intent = new Intent(this, DetailsActivity.class).putExtra("movie_id", id);
            startActivity(intent);
        }
    }
}
