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
package com.google.dart.engine.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class <code>Platform</code> represent a platform for which Dart applications can
 * be targeted.
 */
public class Platform {
  /**
   * The name of the platform.
   */
  private String name;

  /**
   * A table mapping platform names to platforms.
   */
  private static final Map<String, Platform> PlatformMap = new HashMap<String, Platform>();

  /**
   * Return the platform with the given name.
   * 
   * @param platformName the name of the platform to be returned
   * @return the platform with the given name
   */
  public static Platform getPlatform(String platformName) {
    Platform platform = PlatformMap.get(platformName);
    if (platform == null) {
      platform = new Platform(platformName);
      PlatformMap.put(platformName, platform);
    }
    return platform;
  }

  /**
   * Prevent the direct creation of instances of this class. Instances should be accessed using the
   * method {@link #getPlatform(String)}.
   * 
   * @param name the name of the platform being created
   */
  private Platform(String name) {
    this.name = name;
  }

  /**
   * Return the name of this platform.
   * 
   * @return the name of this platform
   */
  public String getName() {
    return name;
  }
}
