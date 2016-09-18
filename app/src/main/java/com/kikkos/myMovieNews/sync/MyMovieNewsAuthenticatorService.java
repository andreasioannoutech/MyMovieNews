package com.kikkos.myMovieNews.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by kikkos on 9/12/2016.
 */
public class MyMovieNewsAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private MyMovieNewsAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new MyMovieNewsAuthenticator(this);
    }


    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
