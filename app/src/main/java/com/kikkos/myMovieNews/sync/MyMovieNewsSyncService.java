package com.kikkos.myMovieNews.sync;

import android.animation.ObjectAnimator;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by kikkos on 9/12/2016.
 */
public class MyMovieNewsSyncService extends Service {

    private static final Object sSyncAdapterLock = new ObjectAnimator();
    private static MyMovieNewsSyncAdapter syncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock){
            if (syncAdapter == null){
                syncAdapter = new MyMovieNewsSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
