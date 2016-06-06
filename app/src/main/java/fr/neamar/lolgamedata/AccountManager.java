package fr.neamar.lolgamedata;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import fr.neamar.lolgamedata.pojo.Account;
import fr.neamar.lolgamedata.service.RegistrationIntentService;

/**
 * Created by neamar on 03/04/16.
 */
public class AccountManager {
    public static final String ACCOUNTS_KEY = "accounts";
    public static final String DEFAULT_VALUE = "[]";
    public static final String ACCOUNTS_CHANGE = "accounts_change";
    private static final String TAG = "AccountManager";

    private Context context;

    public AccountManager(Context context) {
        this.context = context;
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences("accounts", Context.MODE_PRIVATE);
    }

    private void writeAccounts(ArrayList<Account> accounts) {
        JSONArray accountsJson = new JSONArray();

        for (int i = 0; i < accounts.size(); i++) {
            accountsJson.put(accounts.get(i).toJsonObject());
        }

        getSharedPreferences().edit().putString(ACCOUNTS_KEY, accountsJson.toString()).apply();

        // Broadcast change
        Intent i = new Intent(ACCOUNTS_CHANGE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);

        // And (re-)register for push notifications
        // TODO: can we start a service automatically from the manifest on a BroadcastManager
        // Start IntentService to register this application with GCM.
        Intent intent = new Intent(context, RegistrationIntentService.class);
        Log.e(TAG, "Starting Service");
        context.startService(intent);

    }

    public ArrayList<Account> getAccounts() {
        String accountsString = getSharedPreferences().getString(ACCOUNTS_KEY, DEFAULT_VALUE);

        try {
            JSONArray accountsJson = new JSONArray(accountsString);
            ArrayList<Account> accounts = new ArrayList<>();

            for (int i = 0; i < accountsJson.length(); i += 1) {
                Account account = new Account(accountsJson.getJSONObject(i));
                accounts.add(account);
            }

            return accounts;
        } catch (JSONException e) {
            Toast.makeText(context, "Accounts got corrupted, resetting app.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            getSharedPreferences().edit().clear().apply();
            return new ArrayList<>();
        }
    }

    public void addAccount(Account account) {
        ArrayList<Account> accounts = getAccounts();
        accounts.add(account);
        writeAccounts(accounts);

        if(accounts.size() == 1) {
            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.LOGIN, account.toAnalyticsBundle());
            FirebaseAnalytics.getInstance(context).setUserProperty("region", account.region);
        }

        FirebaseAnalytics.getInstance(context).logEvent("new_account_added", account.toAnalyticsBundle());
    }

    public void removeAccount(Account account) {
        ArrayList<Account> accounts = getAccounts();
        accounts.remove(account);
        writeAccounts(accounts);

        FirebaseAnalytics.getInstance(context).logEvent("account_removed", account.toAnalyticsBundle());
    }
}
