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
      "app", "author", "automation", "background", "persistent", "background_page",
      "chrome_settings_overrides", "chrome_ui_overrides", "bookmarks_ui",
      "remove_bookmark_shortcut", "remove_button", "chrome_url_overrides", "commands",
      "content_pack", "content_scripts", "content_security_policy", "policyString",
      "converted_from_user_script", "current_locale", "devtools_page", "externally_connectable",
      "matches", "file_browser_handlers", "homepage_url", "import", "incognito",
      "input_components", "key", "minimum_chrome_version", "nacl_modules", "oauth2",
      "offline_enabled", "omnibox", "keyword", "optional_permissions", "options_page",
      "page_actions", "permissions", "platforms", "plugins", "requirements", "sandbox",
      "script_badge", "short_name", "signature", "spellcheck", "storage", "managed_schema",
      "system_indicator", "tts_engine", "update_url", "web_accessible_resources",

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

  public static boolean getStringType(String keyword) {
    final String[] stringTypes = new String[] {"name", "description", "version", "default_locale"};

    for (String type : stringTypes) {
      if (keyword.equals(type)) {
        return true;
      }
    }

    return false;
  }

  private ManifestKeywords() {

  }

}
