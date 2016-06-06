package fr.neamar.lolgamedata;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.neamar.lolgamedata.adapter.SectionsPagerAdapter;
import fr.neamar.lolgamedata.pojo.Account;
import fr.neamar.lolgamedata.pojo.Game;

public class GameActivity extends BaseActivity {
    public static final String TAG = "GameActivity";

    public static final int UI_MODE_LOADING = 0;
    public static final int UI_MODE_IN_GAME = 1;
    public static final int UI_MODE_NOT_IN_GAME = 2;

    public Account account;
    public Game game = null;
    public String summonerName;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private View mEmptyView;

    private FloatingActionButton mFab;

    private TabLayout mTabLayout;

    private SectionsPagerAdapter sectionsPagerAdapter;

    private Date lastLoaded = null;

    @StringRes
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

    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // First run: open accounts activity, finish this one
        AccountManager accountManager = new AccountManager(this);
        if (accountManager.getAccounts().isEmpty()) {
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, new Bundle());
            Intent i = new Intent(this, AccountsActivity.class);
            startActivity(i);
            finish();
            return;
        }


        // Get account
        if (getIntent() != null && getIntent().hasExtra("account")) {
            account = (Account) getIntent().getSerializableExtra("account");

            if(getIntent().getStringExtra("source").equals("notification")) {
                mFirebaseAnalytics.logEvent("notification_opened", new Bundle());
            }
        } else {
            account = accountManager.getAccounts().get(0);
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
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mEmptyView = findViewById(android.R.id.empty);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);

        // Set up the ViewPager with the sections adapter.
        assert mViewPager != null;
        assert mTabLayout != null;
        mViewPager.setAdapter(sectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        assert mFab != null;
        mFab.setOnClickListener(new View.OnClickListener() {
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

        ((LolApplication) getApplication()).getMixpanel().getPeople().increment("games_viewed_count", 1);
        ((LolApplication) getApplication()).getMixpanel().timeEvent("Game viewed");


        if (savedInstanceState == null || !savedInstanceState.containsKey("game")) {
            loadCurrentGame(account.summonerName, account.region);
        }
    }

    @Override
    protected void onResume() {
        if (lastLoaded != null) {
            long timeSinceLastView = new Date().getTime() - lastLoaded.getTime();
            long timeSinceGameStart = new Date().getTime() - game.startTime.getTime();
            Log.i(TAG, "Game started since " + Math.floor(timeSinceGameStart / 1000 / 60));
            if (timeSinceLastView > 30000 && timeSinceGameStart > 60000 * 15) {
                displaySnack("Stale data?", "Reload", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFirebaseAnalytics.logEvent("reload_stale_data", account.toAnalyticsBundle());
                        loadCurrentGame(account.summonerName, account.region);
                    }
                });
            }
        }

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the AccountsActivity/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_about) {
            displayAboutDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setUiMode(int uiMode) {
        assert mTabLayout != null;
        assert mEmptyView != null;
        assert mFab != null;

        if(uiMode == UI_MODE_LOADING) {
            mTabLayout.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mFab.setVisibility(View.GONE);
        }
        else if(uiMode == UI_MODE_NOT_IN_GAME) {
            mTabLayout.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mFab.setVisibility(View.VISIBLE);
        }
        else if(uiMode == UI_MODE_IN_GAME) {
            mTabLayout.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mFab.setVisibility(View.GONE);
        }
    }
    public void loadCurrentGame(final String summonerName, final String region) {
        final ProgressDialog dialog = ProgressDialog.show(this, "",
                String.format(getString(R.string.loading_game_data), summonerName), true);
        dialog.show();

        // Instantiate the RequestQueue.
        final RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonRequest = null;
        try {
            jsonRequest = new JsonObjectRequest(Request.Method.GET, LolApplication.API_URL + "/game/data?summoner=" + URLEncoder.encode(summonerName, "UTF-8") + "&region=" + region, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                game = new Game(response);
                                GameActivity.this.summonerName = summonerName;
                                displayGame(summonerName, game);

                                Log.i(TAG, "Displaying game #" + game.gameId);

                                Bundle b = account.toAnalyticsBundle();
                                b.putString("gameId", Long.toString(game.gameId));
                                b.putInt("mapId", game.mapId);
                                b.putString("mapName", getString(GameActivity.getMapName(game.mapId)));
                                b.putString("gameMode", game.gameMode);
                                b.putString("gameType", game.gameType);

                                mFirebaseAnalytics.logEvent("view_game", b);
                                // Timing automatically added (see timeEvent() call)
                                JSONObject j = account.toJsonObject();
                                ((LolApplication) getApplication()).getMixpanel().track("Game viewed", j);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            dialog.dismiss();

                            lastLoaded = new Date();

                            queue.stop();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
                    Log.e(TAG, error.toString());

                    setUiMode(UI_MODE_NOT_IN_GAME);
                    try {
                        String responseBody = new String(error.networkResponse.data, "utf-8");
                        Log.i(TAG, responseBody);

                        if(!responseBody.contains("ummoner not in game")) {
                            displaySnack(responseBody);
                            Bundle b = account.toAnalyticsBundle();
                            b.putString("error", responseBody);

                            mFirebaseAnalytics.logEvent("error_viewing_game", b);
                            JSONObject j = account.toJsonObject();
                            j.put("error", responseBody);
                            ((LolApplication) getApplication()).getMixpanel().track("Error viewing game", j);
                        }
                    } catch (UnsupportedEncodingException | JSONException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        // Do nothing, no text content in the HTTP reply.
                    }

                    queue.stop();
                }
            });

            jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(jsonRequest);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void displayGame(String summonerName, Game game) {
        String titleTemplate = getString(R.string.game_data_title);

        @StringRes Integer stringRes = getMapName(game.mapId);

        setTitle(String.format(titleTemplate, summonerName, getString(stringRes)));
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        sectionsPagerAdapter.setTeams(game.teams);

        setUiMode(UI_MODE_IN_GAME);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (game != null) {
            outState.putSerializable("summonerName", summonerName);
            outState.putSerializable("game", game);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("game")) {
            summonerName = savedInstanceState.getString("summonerName");
            game = (Game) savedInstanceState.getSerializable("game");
            displayGame(summonerName, game);
        }
    }

    @NonNull
    public static Integer getMapName(int mapId) {
        return MAP_NAMES.containsKey(mapId) ? MAP_NAMES.get(mapId) : R.string.unknown_map;
    }
}
