package fr.neamar.lolgamedata;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import fr.neamar.lolgamedata.service.RegistrationIntentService;

/**
 * Created by neamar on 29/03/16.
 */
public class LolApplication extends Application {
    public static final String API_URL = "https://teamward.herokuapp.com";
    public static final String MIXPANEL_TOKEN = "1a7075d95ff6db6d08714db52edb706a";
    private static final String TAG = "LolApplication";

    private MixpanelAPI mixpanel = null;

    @Override
    public void onCreate() {
        super.onCreate();
        // Create default options which will be used for every
        //  displayImage(...) call if no options will be passed to this method
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .build();


        ImageLoader.getInstance().init(config);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        // Register for push notifications, send token again in case it changed
        Intent intent = new Intent(this, RegistrationIntentService.class);
        Log.e(TAG, "Starting Service");
        startService(intent);

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.APP_OPEN, new Bundle());
    }

    public MixpanelAPI getMixpanel() {
        if (mixpanel == null) {
            mixpanel = MixpanelAPI.getInstance(this, MIXPANEL_TOKEN);
            mixpanel.getPeople().identify(mixpanel.getDistinctId());
        }

        return mixpanel;

    }
}