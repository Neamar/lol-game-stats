package fr.neamar.lolgamedata;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import java.util.Locale;

import fr.neamar.lolgamedata.adapter.CounterCountersAdapter;
import fr.neamar.lolgamedata.adapter.CounterCountersNoDataAdapter;
import fr.neamar.lolgamedata.pojo.Counter;

public class CounterCountersActivity extends SnackBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter_counters);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Counter counter = (Counter) getIntent().getSerializableExtra("counter");

        CounterCountersAdapter adapter = new CounterCountersAdapter(counter);
        recyclerView.setAdapter(adapter);

        CounterCountersNoDataAdapter noDataAdapter = new CounterCountersNoDataAdapter(counter);
        RecyclerView noDataRecyclerView = (RecyclerView) findViewById(R.id.noData);
        noDataRecyclerView.setAdapter(noDataAdapter);

        if (counter.noData.size() == 0) {
            findViewById(R.id.noDataHolder).setVisibility(View.INVISIBLE);
        }

        getSupportActionBar().setTitle(String.format(getString(R.string.counter_counters_activity_title), counter.account.summonerName, counter.champion.name, counter.role.toLowerCase(Locale.ROOT)));

        if (counter.counters.size() == 0) {
            displaySnack(String.format(getString(R.string.no_counters), counter.champion.name));
        }

        Tracker.trackViewChampionCounters(this, counter);
    }
}
