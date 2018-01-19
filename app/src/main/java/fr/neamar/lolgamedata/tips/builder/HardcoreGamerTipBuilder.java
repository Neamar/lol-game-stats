package fr.neamar.lolgamedata.tips.builder;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.lolgamedata.R;
import fr.neamar.lolgamedata.pojo.Game;
import fr.neamar.lolgamedata.pojo.Player;
import fr.neamar.lolgamedata.pojo.Team;
import fr.neamar.lolgamedata.tips.ChampionStandardTip;
import fr.neamar.lolgamedata.tips.Tip;

public class HardcoreGamerTipBuilder extends TipBuilder {
    @Override
    public ArrayList<Tip> getTips(Game game, Context context) {
        ArrayList<Tip> tips = new ArrayList<>();

        for (Team team : game.teams) {
            for (Player player : team.players) {
                if (player.averageTimeBetweenGames < 24*3600 / 10) {
                    String descriptionTemplate = context.getString(R.string.hardcore_gamer_description);
                    String description = String.format(descriptionTemplate, player.summoner.name);
                    tips.add(new ChampionStandardTip(game, player, player.champion, context.getString(R.string.hardcore_gamer), description));
                }
            }
        }

        return tips;
    }
}