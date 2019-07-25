package fr.neamar.lolgamedata;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;
import androidx.core.view.GravityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.newrelic.agent.android.NewRelic;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.neamar.lolgamedata.adapter.SectionsPagerAdapter;
import fr.neamar.lolgamedata.network.VolleyQueue;
import fr.neamar.lolgamedata.pojo.Account;
import fr.neamar.lolgamedata.pojo.Game;
import fr.neamar.lolgamedata.service.SyncTokenService;
import fr.neamar.lolgamedata.service.TokenRefreshedService;
import fr.neamar.lolgamedata.volley.NoCacheRetryJsonRequest;

public class GameActivity extends SnackBarActivity {
    private static final String TAG = "GameActivity";

    private static final int UI_MODE_LOADING = 0;
    private static final int UI_MODE_IN_GAME = 1;
    private static final int UI_MODE_NOT_IN_GAME = 2;
    private static final int UI_MODE_NO_INTERNET = 3;

    private static final Map<Integer, Integer> MAP_NAMES;

    static {
        Map<Integer, Integer> mapNames = new HashMap<>();
        mapNames.put(1, R.string.summoners_rift);
        mapNames.put(2, R.string.summoners_rift);
        mapNames.put(3, R.string.proving_grounds);
        mapNames.put(4, R.string.twisted_treeline);
        mapNames.put(8, R.string.crystal_scar);
        mapNames.put(10, R.string.twisted_treeline);
        mapNames.put(11, R.string.summoners_rift);
        mapNames.put(12, R.string.howling_abyss);
        mapNames.put(14, R.string.butchers_bridge);

        MAP_NAMES = Collections.unmodifiableMap(mapNames);
    }

    private Account account;
    private Game game = null;
    private String summonerName;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private View mEmptyView;
    private TabLayout mTabLayout;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private Date lastLoaded = null;
    private DrawerLayout mDrawerLayout;

    @NonNull
    public static Integer getMapName(int mapId) {
        return MAP_NAMES.containsKey(mapId) ? MAP_NAMES.get(mapId) : R.string.unknown_map;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!BuildConfig.DEBUG) {
            // Do not use NewRelic on DEBUG.
            NewRelic.withApplicationToken(
                    "AAcab2a6606aca33f2716f49d2c60e68234953a103"
            ).start(this.getApplication());
        }

        setContentView(R.layout.activity_game);

        // First run: open accounts activity, finish this activity
        AccountManager accountManager = new AccountManager(this);
        if (accountManager.getAccounts().isEmpty()) {
            Intent i = new Intent(this, AccountsActivity.class);
            startActivity(i);

            Tracker.trackFirstTimeAppOpen(GameActivity.this);

            finish();
            return;
        }

        // Get account
        if (getIntent() != null && getIntent().hasExtra("account")) {
            account = (Account) getIntent().getSerializableExtra("account");
        } else {
            account = accountManager.getAccounts().get(0);
            if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
                getIntent().putExtra("source", "app_open");
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.title_activity_game);

        mViewPager = (ViewPager) findViewById(R.id.container);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        Button refreshButton = (Button) findViewById(R.id.refresh);
        mEmptyView = findViewById(android.R.id.empty);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);

        // Set up the ViewPager with the sections adapter.
        assert mViewPager != null;
        assert mTabLayout != null;
        mViewPager.setAdapter(sectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        assert refreshButton != null;
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUiMode(UI_MODE_LOADING);
                loadCurrentGame(account.summonerName, account.region);
            }
        });

        TextView notInGame = ((TextView) findViewById(R.id.summoner_not_in_game_text));
        assert notInGame != null;
        notInGame.setText(String.format(getString(R.string.s_is_not_in_game_right_now), account.summonerName));

        setUiMode(UI_MODE_LOADING);

        if (savedInstanceState == null || !savedInstanceState.containsKey("game")) {
            loadCurrentGame(account.summonerName, account.region);
        }

        if (TokenRefreshedService.tokenUpdateRequired(this)) {
            Log.i(TAG, "Syncing FCM token with server");
            // Resync token with server
            Intent intent = new Intent(this, SyncTokenService.class);
            this.startService(intent);
        }
    }

    @Override
    protected void onResume() {
        if (lastLoaded != null && game != null) {
            long timeSinceLastView = new Date().getTime() - lastLoaded.getTime();
            long timeSinceGameStart = new Date().getTime() - game.startTime.getTime();
            Log.i(TAG, "Game started since " + Math.floor(timeSinceGameStart / 1000 / 60) + " minutes.");
            if (timeSinceLastView > 30000 && timeSinceGameStart > 60000 * 15) {
                displaySnack(getString(R.string.stale_data), getString(R.string.reload), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadCurrentGame(account.summonerName, account.region);
                        Tracker.trackReloadStaleGame(GameActivity.this, account);
                    }
                });
            }

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.cancel(game.getNotificationId());
        }

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the AccountsActivity/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.action_about)
                    .setMessage(getString(R.string.about_text))
                    .setPositiveButton(R.string.rammus_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();

            return true;
        } else if (id == R.id.action_counter) {
            Intent counterIntent = new Intent(GameActivity.this, CounterChampionsActivity.class);
            counterIntent.putExtra("account", account);
            startActivity(counterIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUiMode(int uiMode) {
        assert mTabLayout != null;
        assert mEmptyView != null;

        if (uiMode == UI_MODE_LOADING) {
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
        } else if (uiMode == UI_MODE_NOT_IN_GAME) {
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else if (uiMode == UI_MODE_IN_GAME) {
            mTabLayout.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        } else if (uiMode == UI_MODE_NO_INTERNET) {
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private void loadCurrentGame(final String summonerName, final String region) {
        final ProgressDialog dialog = ProgressDialog.show(this, "",
                String.format(getString(R.string.loading_game_data), summonerName), true);
        dialog.show();

        // Instantiate the RequestQueue.
        final RequestQueue queue = VolleyQueue.newRequestQueue(this);

        try {
            String version = "unknown";
            try {
                PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String url = ((LolApplication) getApplication()).getApiUrl() + "/game/data?summoner=" + URLEncoder.encode(summonerName, "UTF-8") + "&region=" + region + "&version=" + version;

            if (BuildConfig.DEBUG && summonerName.equalsIgnoreCase("MOCK")) {
                url = "https://gist.githubusercontent.com/Neamar/eb278b4d5f188546f56028c3a0310507/raw/game.json";
            }

            NoCacheRetryJsonRequest jsonRequest = new NoCacheRetryJsonRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                game = new Game(response, region, account, shouldUseRelativeTeamColor());
                                GameActivity.this.summonerName = summonerName;
                                displayGame(summonerName, game);

                                Log.i(TAG, "Displaying game #" + game.gameId);

                                String source = getIntent() != null && getIntent().hasExtra("source") && !getIntent().getStringExtra("source").isEmpty() ? getIntent().getStringExtra("source") : "unknown";
                                Tracker.trackGameViewed(GameActivity.this, account, game, getDefaultTabName(), shouldDisplayChampionName(), source);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    if (dialog.isShowing()) {
                                        dialog.dismiss();
                                    }
                                } catch (IllegalArgumentException e) {
                                    // View is not attached (rotation for instance)
                                }

                                lastLoaded = new Date();

                                queue.stop();
                            }
                        }
                    }

                    , new Response.ErrorListener()

            {
                @Override
                public void onErrorResponse(VolleyError error) {
                    game = null;
                    lastLoaded = null;

                    try {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    } catch (IllegalArgumentException e) {
                        // View is not attached (rotation for instance)
                    }

                    Log.e(TAG, error.toString());

                    queue.stop();

                    setUiMode(UI_MODE_NO_INTERNET);

                    if (error instanceof NoConnectionError) {
                        displaySnack(getString(R.string.no_internet_connection));
                        return;
                    }


                    try {
                        String responseBody = new String(error.networkResponse.data, "utf-8");
                        Log.i(TAG, responseBody);

                        if (!responseBody.contains("ummoner not in game")) {
                            displaySnack(responseBody);
                            Tracker.trackErrorViewingGame(GameActivity.this, account, responseBody.replace("Error:", ""));
                        } else {
                            Tracker.trackSummonerNotInGame(GameActivity.this, account, responseBody.replace("Error:", ""));
                            if (responseBody.length() > "Error: summoner not in game".length() + 5) {
                                displaySnack(responseBody);
                            }
                            setUiMode(UI_MODE_NOT_IN_GAME);
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        // Do nothing, no text content in the HTTP reply.
                    }
                }
            });

            queue.add(jsonRequest);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void displayGame(String summonerName, Game game) {
        getSupportActionBar().setSubtitle(PerformanceActivity.getQueueName(Integer.toString(game.queue)));
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        sectionsPagerAdapter.setGame(game);

        String defaultTabName = getDefaultTabName();

        TabLayout.Tab selectedTab;

        if (defaultTabName.equals("tips")) {
            selectedTab = mTabLayout.getTabAt(2);
        } else {
            int myTeamIndex = game.teams.get(0) == game.getPlayerOwnTeam() ? 0 : 1;
            int enemyTeamIndex = game.teams.get(0) == game.getPlayerOwnTeam() ? 1 : 0;
            if (defaultTabName.equals("enemy")) {
                selectedTab = mTabLayout.getTabAt(enemyTeamIndex);
            } else {
                selectedTab = mTabLayout.getTabAt(myTeamIndex);
            }
        }

        if (selectedTab != null) {
            selectedTab.select();
        }

        setUiMode(UI_MODE_IN_GAME);

        final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        int counter = prefs.getInt("game_counter", 0);

        prefs.edit().putInt("game_counter", counter + 1).apply();

        if (counter == 5 || counter == 10 || counter == 50) {
            displaySnack(getString(R.string.ap_ad_hint));
        } else if (counter % 100 == 0 && counter > 0 && !prefs.getBoolean("rated_app", false)) {
            displaySnack(getString(R.string.love_the_app), getString(R.string.rate_app), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(GameActivity.this, R.string.thank_you, Toast.LENGTH_SHORT).show();
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=fr.neamar.lolgamedata"));
                    startActivity(browserIntent);

                    Tracker.trackRateTheApp(GameActivity.this);

                    prefs.edit().putBoolean("rated_app", true).apply();
                }
            });
        }

        final SharedPreferences gamePrefs = PreferenceManager.getDefaultSharedPreferences(this);
        gamePrefs.edit().putLong("last_viewed_game", game.gameId).apply();

        // Remove notification if already displayed
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(Long.toString(game.gameId).hashCode());
        }
    }

    private String getDefaultTabName() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("default_game_data_tab", "enemy");
    }

    private boolean shouldDisplayChampionName() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("display_champion_name", true);
    }

    private boolean shouldUseRelativeTeamColor() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("relative_team_colors", true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (game != null) {
            outState.putSerializable("summonerName", summonerName);
            outState.putSerializable("game", game);
            outState.putSerializable("lastLoaded", lastLoaded);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("game")) {
            summonerName = savedInstanceState.getString("summonerName");
            game = (Game) savedInstanceState.getSerializable("game");
            lastLoaded = (Date) savedInstanceState.getSerializable("lastLoaded");
            displayGame(summonerName, game);
        }
    }
}
