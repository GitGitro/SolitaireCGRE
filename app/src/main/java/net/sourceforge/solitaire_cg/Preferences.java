/*
  Copyright 2016, 2017 Curtis Gedak

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package net.sourceforge.solitaire_cg;

import net.sourceforge.solitaire_cg.R;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Force landscape for Android API < 14 (Ice Cream Sandwich)
    //   Earlier versions do not change screen size on orientation change
    if (   Integer.valueOf(android.os.Build.VERSION.SDK) < 14
        || PreferenceManager.getDefaultSharedPreferences(this).getBoolean("LockLandscape", false)
       ) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    addPreferencesFromResource(R.xml.preferences);
  }
}
