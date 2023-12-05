/*
  Copyright 2008 Google Inc.
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

  Modified by Curtis Gedak 2015, 2016, 2017
*/
package net.sourceforge.solitaire_cg;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

// Base activity class.
public class SolitaireCG extends Activity {

  public static String VERSION_NAME = "";

  private static final int MENU_SELECT_GAME  = 1;
  private static final int MENU_NEW          = 2;
  private static final int MENU_RESTART      = 3;
  private static final int MENU_OPTIONS      = 4;
  private static final int MENU_STATS        = 5;
  private static final int MENU_HELP         = 6;
  private static final int MENU_EXIT         = 7;
  private static final int MENU_BAKERSGAME         = 8;
  private static final int MENU_BLACKWIDOW         = 9;
  private static final int MENU_FORTYTHIEVES       = 10;
  private static final int MENU_FREECELL           = 11;
  private static final int MENU_GOLF               = 12;
  private static final int MENU_KLONDIKE_DEALONE   = 13;
  private static final int MENU_KLONDIKE_DEALTHREE = 14;
  private static final int MENU_SPIDER             = 15;
  private static final int MENU_TARANTULA          = 16;
  private static final int MENU_TRIPEAKS           = 17;
  private static final int MENU_VEGAS_DEALONE      = 18;
  private static final int MENU_VEGAS_DEALTHREE    = 19;
  private static final int MENU_GOLF_WRAPCARDS     = 20;
  private static final int MENU_TRIPEAKS_WRAPCARDS = 21;
  // Workaround for inaccessible menu items on some devices
  // and Android versions - add extra blank menu items
  private static final int MENU_BLANK = 999;

  // View extracted from main.xml.
  private View mMainView;
  private SolitaireView mSolitaireView;
  private SharedPreferences mSettings;

  // Shared preferences are where the various user settings are stored.
  public SharedPreferences GetSettings() { return mSettings; }

  // Methods to assist with tracking and maintaining state on device rotation
  private String mRestoreState;
  public void   ClearRestoreState() { mRestoreState = ""; }
  public String GetRestoreState() { return mRestoreState; }
  public void   SetRestoreState(String state) { mRestoreState = state; }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Recall last state before configuration/orientation change
    ConfigWrapper config = (ConfigWrapper)getLastNonConfigurationInstance();
    if (config != null) {
      SetRestoreState(config.screen);
    }

    // Get shared preferences
    mSettings = PreferenceManager.getDefaultSharedPreferences(this);

    // Force landscape for Android API < 14 (Ice Cream Sandwich)
    //   Earlier versions do not change screen size on orientation change
    if (   Integer.valueOf(android.os.Build.VERSION.SDK) < 14
        || mSettings.getBoolean("LockLandscape", false)
       ) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    // Force no title for extra room
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // Set the main view screen
    setContentView(R.layout.main);
    mMainView = findViewById(R.id.main_view);
    mSolitaireView = (SolitaireView) findViewById(R.id.solitaire);
    mSolitaireView.SetTextView((TextView) findViewById(R.id.text));

    //StartSolitaire(savedInstanceState);
    registerForContextMenu(mSolitaireView);

    // Set global variable for versionName
    try {
      VERSION_NAME = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (NameNotFoundException e) {
      Log.e("SolitaireCG.java", e.getMessage());
    }
  }

  // Entry point for starting the game.
  //public void StartSolitaire(Bundle savedInstanceState) {
  @Override
  public void onStart() {
    super.onStart();
    mSolitaireView.onStart();

    if (mSettings.getBoolean("SolitaireSaveValid", false)) {
      SharedPreferences.Editor editor = GetSettings().edit();
      editor.putBoolean("SolitaireSaveValid", false);
      editor.commit();
      // If save is corrupt, just start a new game.
      if (mSolitaireView.LoadSave()) {
        SplashScreen();
        return;
      }
    }

    mSolitaireView.InitGame(mSettings.getInt("LastType", Rules.KLONDIKE));
    SplashScreen();
  }

  // Force show splash screen if this is the first time played.
  private void SplashScreen() {
    if (!mSettings.getBoolean("PlayedBefore", false)) {
      mSolitaireView.DisplaySplash();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    SubMenu subMenu = menu.addSubMenu(0, MENU_SELECT_GAME, 0, R.string.menu_selectgame);
    subMenu.add(0, MENU_FORTYTHIEVES, 0, R.string.menu_fortythieves);
    subMenu.add(0, MENU_FREECELL, 0, R.string.menu_freecell);
    subMenu.add(0, MENU_BAKERSGAME, 0, R.string.menu_bakersgame);
    subMenu.add(0, MENU_GOLF, 0, R.string.menu_golf);
    subMenu.add(0, MENU_GOLF_WRAPCARDS, 0, R.string.menu_golf_wrapcards);
    subMenu.add(0, MENU_KLONDIKE_DEALONE, 0, R.string.menu_klondike_dealone);
    subMenu.add(0, MENU_KLONDIKE_DEALTHREE, 0, R.string.menu_klondike_dealthree);
    subMenu.add(0, MENU_SPIDER, 0, R.string.menu_spider);
    subMenu.add(0, MENU_TARANTULA, 0, R.string.menu_tarantula);
    subMenu.add(0, MENU_BLACKWIDOW, 0, R.string.menu_blackwidow);
    subMenu.add(0, MENU_TRIPEAKS, 0, R.string.menu_tripeaks);
    subMenu.add(0, MENU_TRIPEAKS_WRAPCARDS, 0, R.string.menu_tripeaks_wrapcards);
    subMenu.add(0, MENU_VEGAS_DEALONE, 0, R.string.menu_vegas_dealone);
    subMenu.add(0, MENU_VEGAS_DEALTHREE, 0, R.string.menu_vegas_dealthree);

    menu.add(0, MENU_NEW, 0, R.string.menu_new);
    menu.add(0, MENU_RESTART, 0, R.string.menu_restart);
    menu.add(0, MENU_OPTIONS, 0, R.string.menu_options);
    menu.add(0, MENU_STATS, 0, R.string.menu_stats);
    menu.add(0, MENU_HELP, 0, R.string.menu_help);
    menu.add(0, MENU_EXIT, 0, R.string.menu_exit);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    ClearRestoreState();
    SharedPreferences.Editor editor = GetSettings().edit();
    switch (item.getItemId()) {
      case MENU_BAKERSGAME:
        editor.putBoolean("FreecellBuildBySuit", true);
        editor.commit();
        mSolitaireView.InitGame(Rules.FREECELL);
        break;
      case MENU_BLACKWIDOW:
        editor.putInt("SpiderSuits", 1);
        editor.commit();
        mSolitaireView.InitGame(Rules.SPIDER);
        break;
      case MENU_FORTYTHIEVES:
        mSolitaireView.InitGame(Rules.FORTYTHIEVES);
        break;
      case MENU_FREECELL:
        editor.putBoolean("FreecellBuildBySuit", false); //BuildByAlternateColor
        editor.commit();
        mSolitaireView.InitGame(Rules.FREECELL);
        break;
      case MENU_GOLF:
        editor.putBoolean("GolfWrapCards", false); //No build on King
        editor.commit();
        mSolitaireView.InitGame(Rules.GOLF);
        break;
      case MENU_GOLF_WRAPCARDS:
        editor.putBoolean("GolfWrapCards", true); //WrapCards (A,Q on K, etc.)
        editor.commit();
        mSolitaireView.InitGame(Rules.GOLF);
        break;
      case MENU_KLONDIKE_DEALONE:
        editor.putBoolean("KlondikeDealThree", false);
        editor.putBoolean("KlondikeStyleNormal", true);
        editor.commit();
        mSolitaireView.InitGame(Rules.KLONDIKE);
        break;
      case MENU_KLONDIKE_DEALTHREE:
        editor.putBoolean("KlondikeDealThree", true);
        editor.putBoolean("KlondikeStyleNormal", true);
        editor.commit();
        mSolitaireView.InitGame(Rules.KLONDIKE);
        break;
      case MENU_SPIDER:
        editor.putInt("SpiderSuits", 4);
        editor.commit();
        mSolitaireView.InitGame(Rules.SPIDER);
        break;
      case MENU_TARANTULA:
        editor.putInt("SpiderSuits", 2);
        editor.commit();
        mSolitaireView.InitGame(Rules.SPIDER);
        break;
      case MENU_TRIPEAKS:
        editor.putBoolean("GolfWrapCards", false); //No build on King
        editor.commit();
        mSolitaireView.InitGame(Rules.TRIPEAKS);
        break;
      case MENU_TRIPEAKS_WRAPCARDS:
        editor.putBoolean("GolfWrapCards", true); //WrapCards (A,Q on K, etc.)
        editor.commit();
        mSolitaireView.InitGame(Rules.TRIPEAKS);
        break;
      case MENU_VEGAS_DEALONE:
        editor.putBoolean("KlondikeDealThree", false);
        editor.putBoolean("KlondikeStyleNormal", false);
        editor.commit();
        mSolitaireView.InitGame(Rules.KLONDIKE);
        break;
      case MENU_VEGAS_DEALTHREE:
        editor.putBoolean("KlondikeDealThree", true);
        editor.putBoolean("KlondikeStyleNormal", false);
        editor.commit();
        mSolitaireView.InitGame(Rules.KLONDIKE);
        break;
      case MENU_NEW:
        mSolitaireView.InitGame(mSettings.getInt("LastType", Rules.KLONDIKE));
        break;
      case MENU_RESTART:
        mSolitaireView.RestartGame();
        break;
      case MENU_OPTIONS:
        DisplayOptions();
        break;
      case MENU_STATS:
        DisplayStats();
        break;
      case MENU_HELP:
        DisplayHelp();
        break;
      case MENU_EXIT:
        finish();
        break;
    }

    return false;
  }

  // Alternate Menu
  // Invoked with long press and needed on some devices where Android
  // options menu is not accessible or available.
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
                                  ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);

    SubMenu subMenu = menu.addSubMenu(0, MENU_SELECT_GAME, 0, R.string.menu_selectgame);
    subMenu.add(0, MENU_FORTYTHIEVES, 0, R.string.menu_fortythieves);
    subMenu.add(0, MENU_FREECELL, 0, R.string.menu_freecell);
    subMenu.add(0, MENU_BAKERSGAME, 0, R.string.menu_bakersgame);
    subMenu.add(0, MENU_GOLF, 0, R.string.menu_golf);
    subMenu.add(0, MENU_GOLF_WRAPCARDS, 0, R.string.menu_golf_wrapcards);
    subMenu.add(0, MENU_KLONDIKE_DEALONE, 0, R.string.menu_klondike_dealone);
    subMenu.add(0, MENU_KLONDIKE_DEALTHREE, 0, R.string.menu_klondike_dealthree);
    subMenu.add(0, MENU_SPIDER, 0, R.string.menu_spider);
    subMenu.add(0, MENU_TARANTULA, 0, R.string.menu_tarantula);
    subMenu.add(0, MENU_BLACKWIDOW, 0, R.string.menu_blackwidow);
    subMenu.add(0, MENU_TRIPEAKS, 0, R.string.menu_tripeaks);
    subMenu.add(0, MENU_TRIPEAKS_WRAPCARDS, 0, R.string.menu_tripeaks_wrapcards);
    subMenu.add(0, MENU_VEGAS_DEALONE, 0, R.string.menu_vegas_dealone);
    subMenu.add(0, MENU_VEGAS_DEALTHREE, 0, R.string.menu_vegas_dealthree);
    // Add blank menu items to workaround context menu not centered
    // problem which causes menu items to be inaccessible.
    // https://sourceforge.net/p/solitairecg/tickets/7/
    for (int i = 0; i < 2; i++) {
      subMenu.add(0, MENU_BLANK, 0, R.string.menu_blank);
    }
    menu.add(0, MENU_NEW, 0, R.string.menu_new);
    menu.add(0, MENU_RESTART, 0, R.string.menu_restart);
    menu.add(0, MENU_OPTIONS, 0, R.string.menu_options);
    menu.add(0, MENU_STATS, 0, R.string.menu_stats);
    menu.add(0, MENU_HELP, 0, R.string.menu_help);
    menu.add(0, MENU_EXIT, 0, R.string.menu_exit);
    // Add blank menu items to workaround context menu not centered
    // problem which causes menu items to be inaccessible.
    // https://sourceforge.net/p/solitairecg/tickets/7/
    for (int i = 0; i < 2; i++) {
      menu.add(0, MENU_BLANK, 0, R.string.menu_blank);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    ClearRestoreState();
    SharedPreferences.Editor editor = GetSettings().edit();
    switch (item.getItemId()) {
      case MENU_BAKERSGAME:
        editor.putBoolean("FreecellBuildBySuit", true);
        editor.commit();
        mSolitaireView.InitGame(Rules.FREECELL);
        break;
      case MENU_BLACKWIDOW:
        editor.putInt("SpiderSuits", 1);
        editor.commit();
        mSolitaireView.InitGame(Rules.SPIDER);
        break;
      case MENU_FORTYTHIEVES:
        mSolitaireView.InitGame(Rules.FORTYTHIEVES);
        break;
      case MENU_FREECELL:
        editor.putBoolean("FreecellBuildBySuit", false); //BuildByAlternateColor
        editor.commit();
        mSolitaireView.InitGame(Rules.FREECELL);
        break;
      case MENU_GOLF:
        editor.putBoolean("GolfWrapCards", false); //No build on King
        editor.commit();
        mSolitaireView.InitGame(Rules.GOLF);
        break;
      case MENU_GOLF_WRAPCARDS:
        editor.putBoolean("GolfWrapCards", true); //WrapCards (A,Q on K, etc.)
        editor.commit();
        mSolitaireView.InitGame(Rules.GOLF);
        break;
      case MENU_KLONDIKE_DEALONE:
        editor.putBoolean("KlondikeDealThree", false);
        editor.putBoolean("KlondikeStyleNormal", true);
        editor.commit();
        mSolitaireView.InitGame(Rules.KLONDIKE);
        break;
      case MENU_KLONDIKE_DEALTHREE:
        editor.putBoolean("KlondikeDealThree", true);
        editor.putBoolean("KlondikeStyleNormal", true);
        editor.commit();
        mSolitaireView.InitGame(Rules.KLONDIKE);
        break;
      case MENU_SPIDER:
        editor.putInt("SpiderSuits", 4);
        editor.commit();
        mSolitaireView.InitGame(Rules.SPIDER);
        break;
      case MENU_TARANTULA:
        editor.putInt("SpiderSuits", 2);
        editor.commit();
        mSolitaireView.InitGame(Rules.SPIDER);
        break;
      case MENU_TRIPEAKS:
        editor.putBoolean("GolfWrapCards", false); //No build on King
        editor.commit();
        mSolitaireView.InitGame(Rules.TRIPEAKS);
        break;
      case MENU_TRIPEAKS_WRAPCARDS:
        editor.putBoolean("GolfWrapCards", true); //WrapCards (A,Q on K, etc.)
        editor.commit();
        mSolitaireView.InitGame(Rules.TRIPEAKS);
        break;
      case MENU_VEGAS_DEALONE:
        editor.putBoolean("KlondikeDealThree", false);
        editor.putBoolean("KlondikeStyleNormal", false);
        editor.commit();
        mSolitaireView.InitGame(Rules.KLONDIKE);
        break;
      case MENU_VEGAS_DEALTHREE:
        editor.putBoolean("KlondikeDealThree", true);
        editor.putBoolean("KlondikeStyleNormal", false);
        editor.commit();
        mSolitaireView.InitGame(Rules.KLONDIKE);
        break;
      case MENU_NEW:
        mSolitaireView.InitGame(mSettings.getInt("LastType", Rules.KLONDIKE));
        break;
      case MENU_RESTART:
        mSolitaireView.RestartGame();
        break;
      case MENU_OPTIONS:
        DisplayOptions();
        break;
      case MENU_STATS:
        DisplayStats();
        break;
      case MENU_HELP:
        DisplayHelp();
        break;
      case MENU_EXIT:
        finish();
        break;
      case MENU_BLANK:
        Toast.makeText(this, R.string.toast_blank_menu, Toast.LENGTH_SHORT).show();
        break;
      default:
        return super.onContextItemSelected(item);
    }

    return false;
  }

  @Override
  protected void onPause() {
    super.onPause();
    mSolitaireView.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mSolitaireView.SaveGame();
  }

  // Capture state prior to configuration/orientation change
  @Override
  public Object onRetainNonConfigurationInstance() {
    final ConfigWrapper config = new ConfigWrapper();
    config.screen = GetRestoreState();
    return config;
  }

  @Override
  protected void onResume() {
    super.onResume();
    mSettings = PreferenceManager.getDefaultSharedPreferences(this);

    // Force landscape for Android API < 14 (Ice Cream Sandwich)
    //   Earlier versions do not change screen size on orientation change
    if (   Integer.valueOf(android.os.Build.VERSION.SDK) < 14
        || mSettings.getBoolean("LockLandscape", false)
       ) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    } else {
      // Needed to clear orientation when lock landscape option not set
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    mSolitaireView.onResume();

    // Restore previous state after configuration/orientation change
    if (GetRestoreState() == "STATS") {
      DisplayStats();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  public void DisplayOptions() {
    mSolitaireView.SetTimePassing(false);
    Intent settingsActivity = new Intent(this, Preferences.class);
    startActivity(settingsActivity);
  }

  public void DisplayHelp() {
    mSolitaireView.SetTimePassing(false);
    Intent helpActivity = new Intent(this, Help.class);
    startActivity(helpActivity);
  }

  public void DisplayStats() {
    SetRestoreState("STATS");
    mSolitaireView.SetTimePassing(false);
    new Stats(this, mSolitaireView);
  }

  public void CancelOptions() {
    ClearRestoreState();
    setContentView(mMainView);
    mSolitaireView.requestFocus();
    mSolitaireView.SetTimePassing(true);
  }

  // This is called for option changes that require a refresh, but not a new game
  public void RefreshOptions() {
    ClearRestoreState();
    setContentView(mMainView);
    mSolitaireView.RefreshOptions();
  }
}
