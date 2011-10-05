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
package com.google.dart.tools.ui.cleanup;

import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;

import org.eclipse.core.runtime.Assert;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * Allows to set and retrieve clean up settings for given options keys.
 * 
 * @since 3.5
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CleanUpOptions {

  private final Map<String, String> fOptions;

  /**
   * True value
   */
  public static final String TRUE = "true"; //$NON-NLS-1$

  /**
   * False value
   */
  public static final String FALSE = "false"; //$NON-NLS-1$

  /**
   * Creates a new instance.
   */
  public CleanUpOptions() {
    fOptions = new Hashtable<String, String>();
  }

  /**
   * Creates a new CleanUpOptions instance with the given options.
   * 
   * @param options map that maps clean ups keys (<code>String</code>) to a non-<code>null</code>
   *          string value
   */
  protected CleanUpOptions(Map<String, String> options) {
    fOptions = options;
  }

  /**
   * Returns an unmodifiable set of all known keys.
   * 
   * @return an unmodifiable set of all keys
   */
  public Set<String> getKeys() {
    return Collections.unmodifiableSet(fOptions.keySet());
  }

  /**
   * Returns the value for the given key.
   * 
   * @param key the key of the value
   * @return the value associated with the key
   * @throws IllegalArgumentException if the key is null or unknown
   */
  public String getValue(String key) {
    Assert.isLegal(key != null);
    String value = fOptions.get(key);
    Assert.isLegal(value != null);
    return value;
  }

  /**
   * Tells whether the option with the given <code>key</code> is enabled.
   * 
   * @param key the name of the option
   * @return <code>true</code> if enabled, <code>false</code> if not enabled or unknown key
   * @throws IllegalArgumentException if the key is <code>null</code>
   * @see CleanUpConstants
   */
  public boolean isEnabled(String key) {
    Assert.isLegal(key != null);
    Object value = fOptions.get(key);
    return CleanUpOptions.TRUE == value || CleanUpOptions.TRUE.equals(value);
  }

  /**
   * Sets the option for the given key to the given value.
   * 
   * @param key the name of the option to set
   * @param value the value of the option
   * @throws IllegalArgumentException if the key is <code>null</code>
   * @see CleanUpOptions#TRUE
   * @see CleanUpOptions#FALSE
   */
  public void setOption(String key, String value) {
    Assert.isLegal(key != null);
    Assert.isLegal(value != null);
    fOptions.put(key, value);
  }
}
