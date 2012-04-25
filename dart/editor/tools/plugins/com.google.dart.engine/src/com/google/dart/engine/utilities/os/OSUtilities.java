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
package com.google.dart.engine.utilities.os;

/**
 * The class <code>OSUtilities</code> implements utility methods used to determine which operating
 * system we are running on.
 */
public final class OSUtilities {
  /**
   * The prefix common to all OS names that indicate that we are running on a Macintosh OS.
   */
  private static final String OS_PREFIX_MAC = "mac"; //$NON-NLS-1$

  /**
   * The prefix common to all OS names that indicate that we are running on a Windows OS.
   */
  private static final String OS_PREFIX_WIN = "win"; //$NON-NLS-1$

  /**
   * The name of the {@link System} property whose value is the name of the operating system on
   * which we are currently running.
   */
  private static final String OS_PROPERTY_NAME = "os.name"; //$NON-NLS-1$

  /**
   * Return <code>true</code> if we are running on a Linux OS. This method currently assumes that
   * there are only three possible operating systems: Linux, Macintosh, and Windows.
   * 
   * @return <code>true</code> if we are running on Linux
   */
  public static boolean isLinux() {
    return !isMac() && !isWindows();
  }

  /**
   * Return <code>true</code> if we are running on a Macintosh OS.
   * 
   * @return <code>true</code> if we are running on a Mac
   */
  public static boolean isMac() {
    return System.getProperty(OS_PROPERTY_NAME).toLowerCase().startsWith(OS_PREFIX_MAC);
  }

  /**
   * Return <code>true</code> if we are running on a Windows OS.
   * 
   * @return <code>true</code> if we are running on Windows
   */
  public static boolean isWindows() {
    return System.getProperty(OS_PROPERTY_NAME).toLowerCase().startsWith(OS_PREFIX_WIN);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private OSUtilities() {
    super();
  }
}
