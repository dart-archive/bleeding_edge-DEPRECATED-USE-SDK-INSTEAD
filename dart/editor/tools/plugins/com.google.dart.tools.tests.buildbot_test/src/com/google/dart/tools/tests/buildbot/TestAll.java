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

package com.google.dart.tools.tests.buildbot;

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartSdkManager;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {

  public static Test suite() {
    // The engine and services plugins do not know about Eclipse, but need to know where the Dart 
    // SDK is. We initialize their system property with the location of the SDK gotten from
    // DartSdkManager (generally, eclipse/dart-sdk).
    initSdk();

    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());

    // Engine
    suite.addTest(com.google.dart.engine.TestAll.suite());
    suite.addTest(com.google.dart.server.TestAll.suite());

    // Services
    suite.addTest(com.google.dart.engine.services.TestAll.suite());

    // Core
    suite.addTest(com.google.dart.tools.core.TestAll.suite());

    // Debug
    suite.addTest(com.google.dart.tools.debug.core.TestAll.suite());
    suite.addTest(com.google.dart.tools.debug.ui.TestAll.suite());

    // UI
    suite.addTest(com.google.dart.tools.ui.TestAll.suite());
    // TODO: the UI tests are disabled on linux, due to model dialogs blocking tests -
    if (!DartCore.isLinux()) {
      suite.addTest(editor.TestAll.suite());
      //suite.addTest(views.TestAll.suite());
    }

    // Update
    suite.addTest(com.google.dart.tools.update.core.TestAll.suite());

    // Web
//    suite.addTest(com.google.dart.tools.ui.web.TestAll.suite());

    return suite;
  }

  private static void initSdk() {
    if (DartSdkManager.getManager().hasSdk()) {
      DirectoryBasedDartSdk sdk = DartSdkManager.getManager().getSdk();

      System.setProperty("com.google.dart.sdk", sdk.getDirectory().toString());
    }
  }

}
