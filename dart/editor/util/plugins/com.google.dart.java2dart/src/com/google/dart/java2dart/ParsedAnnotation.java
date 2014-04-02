/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.java2dart;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Value object for an annotation.
 */
public class ParsedAnnotation {
  private final String name;
  private final Map<String, Object> values = Maps.newHashMap();

  public ParsedAnnotation(String name) {
    this.name = name;
  }

  public Object get(String key) {
    return values.get(key);
  }

  public String getName() {
    return name;
  }

  public void put(String key, Object value) {
    values.put(key, value);
  }

  @Override
  public String toString() {
    return name + values;
  }
}
