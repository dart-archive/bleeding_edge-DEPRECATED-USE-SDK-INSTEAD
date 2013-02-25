/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.index.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdentifierStableMappingBuilder {
  private final Set<String> uniqueIdentifiers = new HashSet<String>();

  public void addUniqueIndentifier(String id) {
    uniqueIdentifiers.add(id);
  }

  public Map<String, Integer> build() {
    List<String> ids = new ArrayList<String>(uniqueIdentifiers);
    Collections.sort(ids);
    Map<String, Integer> map = new HashMap<String, Integer>();
    for (int i = 0; i < ids.size(); i++) {
      map.put(ids.get(i), new Integer(i));
    }
    return map;
  }
}
