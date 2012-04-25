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

import junit.framework.TestCase;

public class OSUtilitiesTest extends TestCase {
  public void test_OSUtilities() {
    boolean isLinux = OSUtilities.isLinux();
    boolean isMac = OSUtilities.isMac();
    boolean isWindows = OSUtilities.isWindows();
    int trueCount = (isLinux ? 1 : 0) + (isMac ? 1 : 0) + (isWindows ? 1 : 0);
    assertEquals(1, trueCount);
  }
}
