/*
  Copyright 2016 Curtis Gedak

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

package net.sourceforge.solitaire_cg_re;

import androidx.lifecycle.ViewModel;

// Storage to maintain state over a configuration/orientation change
public class ConfigWrapper extends ViewModel {
  public String screen;

  //private final ConfigWrapper config = new ConfigWrapper();

  public String getConfig() {
    return screen;
  }
}
