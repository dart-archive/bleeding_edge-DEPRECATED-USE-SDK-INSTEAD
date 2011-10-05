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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.tools.ui.cleanup.CleanUpOptions;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MapCleanUpOptions extends CleanUpOptions {

  private final Map<String, String> fOptions;

  public MapCleanUpOptions() {
    this(new Hashtable<String, String>());
  }

  /**
   * Create new CleanUpOptions instance. <code>options</code> maps named clean ups keys to
   * {@link CleanUpOptions#TRUE}, {@link CleanUpOptions#FALSE} or any String value
   * 
   * @param options map from String to String
   * @see CleanUpConstants
   */
  public MapCleanUpOptions(Map<String, String> options) {
    super(options);
    fOptions = options;
  }

  /**
   * @param options the options to add to this options
   */
  public void addAll(CleanUpOptions options) {
    if (options instanceof MapCleanUpOptions) {
      fOptions.putAll(((MapCleanUpOptions) options).getMap());
    } else {
      Set<String> keys = options.getKeys();
      for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
        String key = iterator.next();
        fOptions.put(key, options.getValue(key));
      }
    }
  }

  /**
   * @return all options as map, modifying the map modifies this object
   */
  public Map<String, String> getMap() {
    return fOptions;
  }

}
