/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.EngineTestCase;

public class LibraryInfoTest extends EngineTestCase {
  public void test_client_flag() throws Exception {
    LibraryInfo info = new LibraryInfo();
    // true
    info.setClient(true);
    assertTrue(info.isClient());
    assertFalse(info.isServer());
    assertFalse(info.hasInvalidClientServer());
    // invalidate
    info.invalidateClientServer();
    assertTrue(info.hasInvalidClientServer());
    // false
    info.setClient(false);
    assertFalse(info.isClient());
    assertTrue(info.isServer());
    assertFalse(info.hasInvalidClientServer());
  }

  public void test_launchable_flag() throws Exception {
    LibraryInfo info = new LibraryInfo();
    // true
    info.setLaunchable(true);
    assertTrue(info.isLaunchable());
    assertFalse(info.hasInvalidLaunchable());
    // invalidate
    info.invalidateLaunchable();
    assertTrue(info.hasInvalidLaunchable());
    // false
    info.setLaunchable(false);
    assertFalse(info.isLaunchable());
    assertFalse(info.hasInvalidLaunchable());
  }

  public void test_LibraryInfo_init() throws Exception {
    LibraryInfo info = new LibraryInfo();
    assertTrue(info.hasInvalidClientServer());
    assertTrue(info.hasInvalidElement());
    assertTrue(info.hasInvalidLaunchable());
    assertTrue(info.hasInvalidPublicNamespace());
  }
}
