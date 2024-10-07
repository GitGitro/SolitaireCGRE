package net.sourceforge.solitaire_cg_re;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

public class Modes extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.modes);
        View modesView = findViewById(R.id.modes_view);
        modesView.setFocusable(true);
        modesView.setFocusableInTouchMode(true);

        SolitaireView view = SolitaireViewManager.getInstance();

        Button buttonBlackWidow = modesView.findViewById(R.id.button_blackwidow);
        Button buttonBakersGame = modesView.findViewById(R.id.button_bakersgame);
        Button buttonFreecell = modesView.findViewById(R.id.button_freecell);
        Button buttonFortyThieves = modesView.findViewById(R.id.button_fortythieves);
        Button buttonGolf = modesView.findViewById(R.id.button_golf);
        Button buttonKlondikeDealOne = modesView.findViewById(R.id.button_klondike_dealone);
        Button buttonKlondikeDealThree = modesView.findViewById(R.id.button_klondike_dealthree);
        Button buttonSpider = modesView.findViewById(R.id.button_spider);
        Button buttonTarantula = modesView.findViewById(R.id.button_tarantula);
        Button buttonTripeaks = modesView.findViewById(R.id.button_tripeaks);
        Button buttonTripeaksWrapcards = modesView.findViewById(R.id.button_tripeaks_wrapcards);
        Button buttonVegasDealOne = modesView.findViewById(R.id.button_vegas_dealone);
        Button buttonVegasDealThree = modesView.findViewById(R.id.button_vegas_dealthree);
        Button buttonGolfWrapcards = modesView.findViewById(R.id.button_golf_wrapcards);


        final SharedPreferences settings = getSharedPreferences("YourPreferences", MODE_PRIVATE);
        final SharedPreferences.Editor editor = settings.edit();


        buttonBlackWidow.setOnClickListener(v -> {
            editor.putInt("SpiderSuits", 1);
            editor.apply();
            view.resetGameState();
            view.InitGame(Rules.SPIDER);
            finish();
        });

        buttonBakersGame.setOnClickListener(v -> {
            editor.putBoolean("FreecellBuildBySuit", true);
            editor.apply();
            view.InitGame(Rules.FREECELL);
            finish();
        });

        buttonFreecell.setOnClickListener(v -> {
            editor.putBoolean("FreecellBuildBySuit", false);
            editor.apply();
            view.InitGame(Rules.FREECELL);
            finish();
        });

        buttonFortyThieves.setOnClickListener(v -> {
            view.InitGame(Rules.FORTYTHIEVES);
            finish();
        });

        buttonGolf.setOnClickListener(v -> {
            editor.putBoolean("GolfWrapCards", false);
            editor.apply();
            view.InitGame(Rules.GOLF);
            finish();
        });

        buttonKlondikeDealOne.setOnClickListener(v -> {
            editor.putBoolean("KlondikeDealThree", false);
            editor.putBoolean("KlondikeStyleNormal", true);
            editor.apply();
            view.InitGame(Rules.KLONDIKE);
            finish();
        });

        buttonKlondikeDealThree.setOnClickListener(v -> {
            editor.putBoolean("KlondikeDealThree", true);
            editor.putBoolean("KlondikeStyleNormal", true);
            editor.apply();
            view.InitGame(Rules.KLONDIKE);
            finish();
        });

        buttonSpider.setOnClickListener(v -> {
            editor.putInt("SpiderSuits", 4);
            editor.apply();
            view.InitGame(Rules.SPIDER);
            finish();
        });

        buttonTarantula.setOnClickListener(v -> {
            editor.putInt("SpiderSuits", 2);
            editor.apply();
            view.InitGame(Rules.SPIDER);
            finish();
        });

        buttonTripeaks.setOnClickListener(v -> {
            editor.putBoolean("GolfWrapCards", false);
            editor.apply();
            view.InitGame(Rules.TRIPEAKS);
            finish();
        });

        buttonTripeaksWrapcards.setOnClickListener(v -> {
            editor.putBoolean("GolfWrapCards", true);
            editor.apply();
            view.InitGame(Rules.TRIPEAKS);
            finish();
        });

        buttonVegasDealOne.setOnClickListener(v -> {
            editor.putBoolean("KlondikeDealThree", false);
            editor.putBoolean("KlondikeStyleNormal", false);
            editor.apply();
            view.InitGame(Rules.KLONDIKE);
            finish();
        });

        buttonVegasDealThree.setOnClickListener(v -> {
            editor.putBoolean("KlondikeDealThree", true);
            editor.putBoolean("KlondikeStyleNormal", false);
            editor.apply();
            view.InitGame(Rules.KLONDIKE);
            finish();
        });

        buttonGolfWrapcards.setOnClickListener(v -> {
            editor.putBoolean("GolfWrapCards", true);
            editor.apply();
            view.InitGame(Rules.GOLF);
            finish();
        });
    }
}
