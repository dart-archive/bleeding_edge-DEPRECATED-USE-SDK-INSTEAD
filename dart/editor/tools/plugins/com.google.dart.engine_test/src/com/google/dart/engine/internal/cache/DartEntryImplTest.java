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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.context.CacheState;

public class DartEntryImplTest extends EngineTestCase {
  public void test_creation() throws Exception {
    DartEntryImpl info = new DartEntryImpl();
    assertSame(CacheState.INVALID, info.getState(DartEntry.ELEMENT));
    assertSame(CacheState.INVALID, info.getState(DartEntry.INCLUDED_PARTS));
    assertSame(CacheState.INVALID, info.getState(DartEntry.IS_CLIENT));
    assertSame(CacheState.INVALID, info.getState(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.INVALID, info.getState(DartEntry.LINE_INFO));
    assertSame(CacheState.INVALID, info.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.INVALID, info.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.INVALID, info.getState(DartEntry.PUBLIC_NAMESPACE));
  }

  public void test_isClient() throws Exception {
    DartEntryImpl info = new DartEntryImpl();
    // true
    info.setValue(DartEntry.IS_CLIENT, true);
    assertTrue(info.getValue(DartEntry.IS_CLIENT));
    assertSame(CacheState.VALID, info.getState(DartEntry.IS_CLIENT));
    // invalidate
    info.setState(DartEntry.IS_CLIENT, CacheState.INVALID);
    assertSame(CacheState.INVALID, info.getState(DartEntry.IS_CLIENT));
    // false
    info.setValue(DartEntry.IS_CLIENT, false);
    assertFalse(info.getValue(DartEntry.IS_CLIENT));
    assertSame(CacheState.VALID, info.getState(DartEntry.IS_CLIENT));
  }

  public void test_isLaunchable() throws Exception {
    DartEntryImpl info = new DartEntryImpl();
    // true
    info.setValue(DartEntry.IS_LAUNCHABLE, true);
    assertTrue(info.getValue(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.VALID, info.getState(DartEntry.IS_LAUNCHABLE));
    // invalidate
    info.setState(DartEntry.IS_LAUNCHABLE, CacheState.INVALID);
    assertSame(CacheState.INVALID, info.getState(DartEntry.IS_LAUNCHABLE));
    // false
    info.setValue(DartEntry.IS_LAUNCHABLE, false);
    assertFalse(info.getValue(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.VALID, info.getState(DartEntry.IS_LAUNCHABLE));
  }
}
