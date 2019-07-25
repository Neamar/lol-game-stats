package fr.neamar.lolgamedata.adapter;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

import fr.neamar.lolgamedata.R;
import fr.neamar.lolgamedata.fragment.TeamFragment;
import fr.neamar.lolgamedata.fragment.TipFragment;
import fr.neamar.lolgamedata.pojo.Game;
import fr.neamar.lolgamedata.pojo.Team;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
    private final Context context;
    private Game game;
    private ArrayList<Team> teams = new ArrayList<>();

    public SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    public void setGame(Game game) {
        this.game = game;
        this.teams = game.teams;
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        if (position == teams.size()) {
            return TipFragment.newInstance(position + 1, game);
        }

        // getItem is called to instantiate the fragment for the given page.
        return TeamFragment.newInstance(position + 1, teams.get(position), game);
    }

    @Override
    public int getCount() {
        if (teams.size() == 0) {
            return 0;
        }
        return teams.size() + 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == teams.size()) {
            return context.getString(R.string.tips);
        }

        return teams.get(position).getName(context);
    }

    public int getItemPosition(Object item) {
        // Fix for a very weird bug:
        // http://stackoverflow.com/questions/10849552/update-viewpager-dynamically
        // FragmentAdapter will not refresh its content unless forced to do so
        // even if notifyDatasetChanged() was called.
        // So when new data is sent, ensure old fragments are removed.
        if (item instanceof TeamFragment) {
            TeamFragment teamFragment = (TeamFragment) item;
            if (teams.indexOf(teamFragment.getTeam()) == -1) {
                return POSITION_NONE;
            }
        } else if (item instanceof TipFragment) {
            TipFragment tipFragment = (TipFragment) item;
            if (tipFragment.game != game) {
                return POSITION_NONE;
            }
        }

        return super.getItemPosition(item);
    }
}