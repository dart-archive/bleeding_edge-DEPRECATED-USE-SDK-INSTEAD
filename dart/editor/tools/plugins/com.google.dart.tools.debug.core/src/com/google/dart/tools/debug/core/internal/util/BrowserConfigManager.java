/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core.internal.util;

import com.google.dart.tools.debug.core.ChromeBrowserConfig;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DebugUIHelperFactory;

import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A manager class for ChromeBrowserConfigs.
 */
public class BrowserConfigManager {

  private static final String PREFS_CONFIGURED_BROWSERS = "configuredBrowsers";

  private static BrowserConfigManager manager = new BrowserConfigManager();

  public static BrowserConfigManager getManager() {
    return manager;
  }

  public BrowserConfigManager() {
    if (getConfiguredBrowsers().size() == 0) {
      autoDiscoverBrowsers();
    }
  }

  public void autoDiscoverBrowsers() {
    String platform = DebugUIHelperFactory.getDebugUIHelper().getPlatform();

    List<ChromeBrowserConfig> discoveredBrowsers = null;

    if ("cocoa".equals(platform)) {
      discoveredBrowsers = discoverMacBrowsers();
    }

    if (discoveredBrowsers != null && discoveredBrowsers.size() > 0) {
      List<ChromeBrowserConfig> browsers = new ArrayList<ChromeBrowserConfig>();

      browsers.addAll(getConfiguredBrowsers());
      browsers.addAll(discoveredBrowsers);

      setConfiguredBrowsers(browsers);
    }
  }

  public ChromeBrowserConfig getBrowserConfig(String browserName) {
    List<ChromeBrowserConfig> browsers = getConfiguredBrowsers();
    for (ChromeBrowserConfig browserConfig : browsers) {
      if (browserConfig.getName().equals(browserName)) {
        return browserConfig;
      }
    }
    return null;
  }

  public List<ChromeBrowserConfig> getConfiguredBrowsers() {
    String str = DartDebugCorePlugin.getPlugin().getPrefs().get(PREFS_CONFIGURED_BROWSERS, "");

    List<ChromeBrowserConfig> browsers = new ArrayList<ChromeBrowserConfig>();

    for (String token : str.split(",")) {
      ChromeBrowserConfig browserConfig = ChromeBrowserConfig.fromToken(token);

      if (browserConfig != null) {
        browsers.add(browserConfig);
      }
    }

    return browsers;
  }

  public void setConfiguredBrowsers(List<ChromeBrowserConfig> browsers) {
    String value = "";

    for (ChromeBrowserConfig browserConfig : browsers) {
      if (value.length() > 0) {
        value += ",";
      }

      value += browserConfig.toToken();
    }

    DartDebugCorePlugin.getPlugin().getPrefs().put(PREFS_CONFIGURED_BROWSERS, value);

    try {
      DartDebugCorePlugin.getPlugin().getPrefs().flush();
    } catch (BackingStoreException exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

  private ChromeBrowserConfig discoverMacBrowser(String browserName) {
    File file = new File("/Applications/" + browserName + ".app/Contents/MacOS/" + browserName);

    if (file.exists()) {
      ChromeBrowserConfig browser = new ChromeBrowserConfig();

      browser.setName(browserName);
      browser.setPath(file.getPath());

      return browser;
    } else {
      return null;
    }
  }

  private List<ChromeBrowserConfig> discoverMacBrowsers() {
    List<ChromeBrowserConfig> browsers = new ArrayList<ChromeBrowserConfig>();

    final String[] browserNames = new String[] {"Chromium", "Google Chrome"};

    for (String browserName : browserNames) {
      ChromeBrowserConfig browser = discoverMacBrowser(browserName);

      if (browser != null) {
        browsers.add(browser);
      }
    }

    return browsers;
  }

}
