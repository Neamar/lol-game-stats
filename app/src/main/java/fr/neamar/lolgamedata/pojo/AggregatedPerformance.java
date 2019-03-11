package fr.neamar.lolgamedata.pojo;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import fr.neamar.lolgamedata.R;

/**
 * Created by neamar on 23/09/17.
 */

public class AggregatedPerformance {

    public double kills;
    public double deaths;
    public double assists;
    private double csPerMinAvg;
    private double firstBloodPercent;
    private double firstTowerPercent;

    public int gamesCount;
    public double winsPercent;
    private double winlanePercent;

    private AggregatedPerformance(JSONObject aggregated) throws JSONException {
        kills = aggregated.getDouble("kills_average");
        deaths = aggregated.getDouble("deaths_average");
        assists = aggregated.getDouble("assists_average");
        csPerMinAvg = aggregated.getDouble("cs_per_min_average");
        firstBloodPercent = aggregated.getDouble("first_blood_percent");
        firstTowerPercent = aggregated.getDouble("first_tower_percent");
        gamesCount = aggregated.getInt("games_count");
        winsPercent = aggregated.getDouble("wins_percent");
        winlanePercent = aggregated.getDouble("winlane_percent");
    }

    public static AggregatedPerformance getAggregated(JSONObject json) {
        AggregatedPerformance aggregated = null;

        try {
           aggregated = new AggregatedPerformance(json.getJSONObject("aggregated"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return aggregated;
    }

    public String getQualities(Context context) {
        ArrayList<String> qualities = new ArrayList<>();

        if(firstBloodPercent > 15) {
            qualities.add(context.getString(R.string.often_gets_first_blood));
        }

        if(firstTowerPercent > 15) {
            qualities.add(context.getString(R.string.often_gets_first_tower));
        }

        if(csPerMinAvg > 8.5 && csPerMinAvg < 9.5) {
            qualities.add(context.getString(R.string.good_farmer));
        }
        else if(csPerMinAvg >= 9.5) {
            qualities.add(context.getString(R.string.exceptional_farmer));
        }

        if(winlanePercent >= 55 && winlanePercent < 65) {
            qualities.add(context.getString(R.string.good_laner));
        }
        else if(winlanePercent >= 65) {
            qualities.add(context.getString(R.string.exceptional_laner));
        }

        if(qualities.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("; ");

        for (String quality: qualities) {
            sb.append(quality);
            sb.append(", ");
        }
        // Remove last comma
        sb.deleteCharAt(sb.length() - 1);
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}
