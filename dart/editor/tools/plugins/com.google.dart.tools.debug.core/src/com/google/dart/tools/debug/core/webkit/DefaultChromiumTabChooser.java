/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.debug.core.webkit;

import java.util.List;

/**
 * A IChromiumTabChooser which tries to determine the best tab to debug algorithmically, without
 * asking the user.
 */
public class DefaultChromiumTabChooser implements IChromiumTabChooser {
  /** A fragment of the initial page, used to search for it in a list of open tabs. */
  private static final String INITIAL_PAGE_FRAGMENT = "chrome://version";

  public DefaultChromiumTabChooser() {

  }

  @Override
  public ChromiumTabInfo chooseTab(List<ChromiumTabInfo> tabs) {
    for (ChromiumTabInfo tab : tabs) {
      if (tab.getTitle().contains(INITIAL_PAGE_FRAGMENT)) {
        return tab;
      }

      if (tab.getUrl().contains(INITIAL_PAGE_FRAGMENT)) {
        return tab;
      }
    }

    if (tabs.size() == 0) {
      // If no tabs, return null.
      return null;
    } else if (tabs.size() == 1) {
      // If one tab, return that.
      return tabs.get(0);
    } else {
      // If more then one tab, return the first visible, non-Chrome extension tab.
      for (ChromiumTabInfo tab : tabs) {
        if (!tab.isChromeExtension()) {
          return tab;
        }
      }
    }

    return null;
  }

}
