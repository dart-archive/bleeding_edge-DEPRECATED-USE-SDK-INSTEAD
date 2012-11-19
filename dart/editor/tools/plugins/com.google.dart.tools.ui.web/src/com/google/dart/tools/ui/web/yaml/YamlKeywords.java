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
package com.google.dart.tools.ui.web.yaml;

import com.google.dart.tools.ui.web.DartWebPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A comprehensive list of keywords used in yaml files for pub and tools. This class reads its
 * content from the meta/yamml.txt file.
 */
class YamlKeywords {

  private static List<String> globalAttributes;

  private static List<String> keywords;
  private static Map<String, List<String>> attributeMap;

  static {
    init();
  }

  public static List<String> getAttributes(String keyword) {
    if (attributeMap.containsKey(keyword)) {
      List<String> atts = new ArrayList<String>();

      atts.addAll(attributeMap.get(keyword));
      atts.addAll(globalAttributes);

      return atts;
    } else {
      return Collections.emptyList();
    }
  }

  public static List<String> getKeywords() {
    return keywords;
  }

  private static void init() {
    try {
      keywords = new ArrayList<String>();
      attributeMap = new HashMap<String, List<String>>();
      globalAttributes = Collections.emptyList();

      BufferedReader reader = new BufferedReader(new InputStreamReader(
          YamlKeywords.class.getResourceAsStream("/meta/yaml.txt")));

      String line = reader.readLine();

      while (line != null) {
        line = line.trim();

        if (line.length() > 0 && line.charAt(0) != '#') {
          if (line.startsWith("[")) {
            // handle the global attributes
            line = line.substring(1, line.length() - 1);

            globalAttributes = Collections.unmodifiableList(Arrays.asList(line.split(",")));
          } else {
            List<String> atts = Collections.emptyList();
            String keyword = line;

            if (line.indexOf('=') != -1) {
              String[] strs = line.split("=");

              keyword = strs[0];
              atts = Collections.unmodifiableList(Arrays.asList(strs[1]));
            }

            attributeMap.put(keyword, atts);
          }
        }

        line = reader.readLine();
      }

      reader.close();

      keywords.addAll(attributeMap.keySet());
      Collections.sort(keywords);
    } catch (IOException ioe) {
      DartWebPlugin.logError(ioe);
    }
  }

  private YamlKeywords() {

  }

}
