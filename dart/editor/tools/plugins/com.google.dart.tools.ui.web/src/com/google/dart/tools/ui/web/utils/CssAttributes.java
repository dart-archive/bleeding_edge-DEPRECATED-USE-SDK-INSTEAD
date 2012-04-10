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
package com.google.dart.tools.ui.web.utils;

import com.google.dart.tools.ui.web.DartWebPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A list of CSS attributes. This class reads its values from the meta/css.txt file.
 */
public class CssAttributes {

  private static Map<String, List<String>> attributeMap = new HashMap<String, List<String>>();

  static {
    init();
  }

  public static Set<String> getAttributes() {
    return attributeMap.keySet();
  }

  public static List<String> getAttributeValues(String keyword) {
    if (attributeMap.containsKey(keyword)) {
      return attributeMap.get(keyword);
    } else {
      return Collections.emptyList();
    }
  }

  private static void init() {
    try {
      attributeMap = new HashMap<String, List<String>>();

      BufferedReader reader = new BufferedReader(new InputStreamReader(
          CssAttributes.class.getResourceAsStream("/meta/css.txt")));

      String line = reader.readLine();

      while (line != null) {
        line = line.trim();

        if (line.length() > 0 && line.charAt(0) != '#') {
          List<String> atts = Collections.emptyList();
          String keyword = line;

          if (line.indexOf('=') != -1) {
            String[] strs = line.split("=");

            keyword = strs[0];
            atts = Collections.unmodifiableList(Arrays.asList(strs[1]));
          }

          attributeMap.put(keyword, atts);
        }

        line = reader.readLine();
      }

      reader.close();

    } catch (IOException ioe) {
      DartWebPlugin.logError(ioe);
    }
  }

  private CssAttributes() {

  }

}
