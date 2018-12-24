package fr.neamar.lolgamedata.tips.holder;

import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import fr.neamar.lolgamedata.R;
import fr.neamar.lolgamedata.holder.TipHolder;
import fr.neamar.lolgamedata.pojo.ChampionInGame;
import fr.neamar.lolgamedata.pojo.Game;
import fr.neamar.lolgamedata.pojo.Player;
import fr.neamar.lolgamedata.pojo.Team;
import fr.neamar.lolgamedata.tips.PremadeTip;
import fr.neamar.lolgamedata.tips.Tip;

public class PremadeTipHolder extends TipHolder {
    private final LinearLayout redTeamLayout;
    private final LinearLayout blueTeamLayout;
    private final View disclaimer;

    private PremadeTipHolder(View itemView) {
        super(itemView);
        redTeamLayout = itemView.findViewById(R.id.redTeam);
        blueTeamLayout = itemView.findViewById(R.id.blueTeam);
        disclaimer = itemView.findViewById(R.id.disclaimer);
    }

    public static TipHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View view = inflater.inflate(R.layout.item_tip_premade, parent, false);

        return new PremadeTipHolder(view);
    }

    public void bind(Tip tip) {
        PremadeTip premadeTip = (PremadeTip) tip;
        Game game = premadeTip.game;

        drawChampions(game.teams.get(0));

        // Custom game
        if (game.teams.size() != 2) {
            return;
        }

        drawChampions(game.teams.get(1));

        if (game.teams.get(0).premades.size() == 1 && game.teams.get(1).premades.size() == 1) {
            // One big premade on both teams, we can't really be wrong...
            disclaimer.setVisibility(View.GONE);
        } else {
            // Never forget that this is no exact science
            disclaimer.setVisibility(View.VISIBLE);
        }
    }

    private void drawChampions(Team team) {
        LinearLayout linearLayout = team.teamId == 100 ? blueTeamLayout : redTeamLayout;
        int dpConversion = (int) itemView.getResources().getDimension(R.dimen.tip_premade_champion_thumbnail);
        int spConversion = (int) itemView.getResources().getDimension(R.dimen.tip_premade_champion_text_separator);

        // Clean up old views
        linearLayout.removeAllViews();
        for (List<String> subPremade : team.premades) {
            for (String summonerId : subPremade) {
                Player p = findPlayerById(team, summonerId);
                if (p == null) {
                    continue;
                }

                ChampionInGame champion = p.champion;

                ImageView imageview = new ImageView(itemView.getContext());
                imageview.setImageResource(R.drawable.default_champion);
                imageview.setLayoutParams(new LinearLayout.LayoutParams(dpConversion, dpConversion));
                linearLayout.addView(imageview);

                ImageLoader.getInstance().displayImage(champion.imageUrl, imageview);
                imageview.setContentDescription(champion.name);
            }

            if (subPremade != team.premades.get(team.premades.size() - 1)) {
                TextView textView = new TextView(itemView.getContext());
                textView.setText("—");
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(spConversion);
                linearLayout.addView(textView);
            }
        }
    }

    @Nullable
    private Player findPlayerById(Team team, String summonerId) {
        for (Player player : team.players) {
            if (player.summoner.id.equals(summonerId)) {
                return player;
            }
        }

        return null;
    }
}
