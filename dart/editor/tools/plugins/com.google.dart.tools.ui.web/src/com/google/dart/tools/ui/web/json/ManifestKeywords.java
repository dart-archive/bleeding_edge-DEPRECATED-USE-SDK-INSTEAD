/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.ui.web.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A list of keywords for manifest.json files.
 * 
 * @see https://developer.chrome.com/extensions/manifest.html
 */
class ManifestKeywords {
  private static final String[] KEYWORDS = new String[] {
      // required
      "name",
      "version",
      "manifest_version",

      // recommended
      "description",
      "icons",
      "default_locale",

      // everything else
      "browser_action", "page_action", "theme", "app", "background", "chrome_url_overrides",
      "content_scripts", "content_security_policy", "file_browser_handlers", "homepage_url",
      "incognito", "intents", "key", "minimum_chrome_version", "nacl_modules", "offline_enabled",
      "omnibox", "options_page", "permissions", "plugins", "requirements", "update_url",
      "web_accessible_resources", "sandbox",

      // effectively keywords as well
      "scripts"};

  private static List<String> keywords;

  public static List<String> getKeywords() {
    if (keywords == null) {
      keywords = new ArrayList<String>(Arrays.asList(KEYWORDS));
      Collections.sort(keywords);
      keywords = Collections.unmodifiableList(keywords);
    }

    return keywords;
  }

  private ManifestKeywords() {

  }

}
