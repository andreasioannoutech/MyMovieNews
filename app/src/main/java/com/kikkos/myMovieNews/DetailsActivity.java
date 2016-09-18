package com.kikkos.myMovieNews;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class DetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        if (savedInstanceState == null){
            // this activity will only happen in 1 pane situations.
            // so in those scenarios (which is for devices less than sw600dp) i get the movie id from the intent
            // and pass it to the details fragment as an argument. I do it here so i dont need to write more
            // code in the details fragment for fetching the movie id. just grab it from the fragment as an argument which is used also for the 2 pane scenarios.
            Bundle args = new Bundle();
            args.putInt(DetailsFragment.DETAIL_URI, getIntent().getIntExtra("movie_id", -1));

            DetailsFragment detailsFragment = new DetailsFragment();
            detailsFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().add(R.id.details_activity_id, detailsFragment).commit();
        }
    }
}
