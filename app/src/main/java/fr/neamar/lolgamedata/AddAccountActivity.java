package fr.neamar.lolgamedata;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import fr.neamar.lolgamedata.pojo.Account;

public class AddAccountActivity extends Activity {
    public static final String TAG = "AddAccountActivity";
    public static final String NEW_ACCOUNT = "new_account";
    public static final int RESULT_ERROR = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_account);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        final TextView nameText = (TextView) findViewById(R.id.summonerText);
        final Spinner regionSpinner = (Spinner) findViewById(R.id.summonerRegion);
        regionSpinner.setFocusable(true);
        regionSpinner.setFocusableInTouchMode(true);

        setFinishOnTouchOutside(false);

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameText.getText().toString();
                String region = regionSpinner.getSelectedItem().toString().replaceAll(" .+$", "");

                Log.i(TAG, "Adding new account: " + name + " (" + region + ")");

                saveAccount(name, region);
            }
        });
    }

    private Account saveAccount(String name, String region) {
        final Account newAccount = new Account(name, region, "");

        final ProgressDialog dialog = ProgressDialog.show(this, "",
                String.format(getString(R.string.loading_summoner_data), name), true);
        dialog.show();

        // Instantiate the RequestQueue.
        final RequestQueue queue = Volley.newRequestQueue(this);

        try {
            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, LolApplication.API_URL + "/summoner/data?summoner=" + URLEncoder.encode(name, "UTF-8") + "&region=" + region,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            dialog.dismiss();

                            newAccount.summonerImage = response.optString("profileIcon", "");
                            newAccount.summonerName = response.optString("name", newAccount.summonerName);

                            Intent intent = new Intent(NEW_ACCOUNT);
                            intent.putExtra("account", newAccount);
                            setResult(RESULT_OK, intent);

                            (new AccountManager(AddAccountActivity.this)).addAccount(newAccount);

                            FirebaseAnalytics.getInstance(AddAccountActivity.this).logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, newAccount.toAnalyticsBundle());

                            queue.stop();
                            finish();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
                    Log.e(TAG, error.toString());

                    try {
                        String responseBody = new String(error.networkResponse.data, "utf-8");
                        Log.i(TAG, responseBody);

                        Intent intent = new Intent();
                        intent.putExtra("type", NEW_ACCOUNT);
                        intent.putExtra("error", responseBody.isEmpty() ? "Unable to load player data" : responseBody);
                        setResult(RESULT_ERROR, intent);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                    }

                    queue.stop();
                    finish();
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

        return newAccount;
    }
}
