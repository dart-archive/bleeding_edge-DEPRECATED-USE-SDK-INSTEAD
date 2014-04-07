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
package com.google.dart.tools.core.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;

import junit.framework.TestCase;

public class DartSdkManagerTest extends TestCase {

  public void test_getManager() {
    assertNotNull(DartSdkManager.getManager());
  }

  public void test_noSdk() {
    DirectoryBasedDartSdk sdk = DartSdkManager.NONE;
    assertEquals(0, sdk.getUris().length);
    assertEquals(0, sdk.getSdkLibraries().length);
    assertNull(sdk.getSdkLibrary("dart:core"));
    assertNull(sdk.mapDartUri("dart:core"));
    assertEquals(DartSdk.DEFAULT_VERSION, sdk.getSdkVersion());
    AnalysisContext context = sdk.getContext();
    assertNull(context.performAnalysisTask().getChangeNotices());
  }
}
