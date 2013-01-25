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

import com.google.dart.engine.ExtendedTestSuite;
import com.google.dart.engine.source.ContentCacheTest;
import com.google.dart.engine.source.DirectoryBasedSourceContainerTest;
import com.google.dart.engine.source.FileBasedSourceTest;
import com.google.dart.engine.source.FileUriResolverTest;
import com.google.dart.engine.source.PackageUriResolverTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());

    // Engine
    // TODO (jwren) currently only a subset of the engine tests run on the buildbot
    //suite.addTest(com.google.dart.engine.TestAll.suite());
    suite.addTest(engineTests());

    // Core
    suite.addTest(com.google.dart.tools.core.TestAll.suite());

    // UI
    //suite.addTest(com.google.dart.tools.ui.TestAll.suite());

    return suite;
  }

  private static TestSuite engineTests() {
    // Copy of TestAll from engine_test
    TestSuite suite = new ExtendedTestSuite("Tests in " + TestAll.class.getPackage().getName());
    //suite.addTestSuite(AnalysisEngineTest.class);
    suite.addTest(com.google.dart.engine.ast.TestAll.suite());
    suite.addTest(com.google.dart.engine.index.TestAll.suite());
    //suite.addTest(com.google.dart.engine.internal.TestAll.suite());
    suite.addTest(com.google.dart.engine.parser.TestAll.suite());
    //suite.addTest(com.google.dart.engine.resolver.TestAll.suite());
    suite.addTest(com.google.dart.engine.scanner.TestAll.suite());
    //suite.addTest(com.google.dart.engine.sdk.TestAll.suite());
    suite.addTest(com.google.dart.engine.search.TestAll.suite());

    // only a subset from "source" TestAll:
    //suite.addTest(com.google.dart.engine.source.TestAll.suite());
    suite.addTestSuite(ContentCacheTest.class);
    //suite.addTestSuite(DartUriResolverTest.class);
    suite.addTestSuite(FileUriResolverTest.class);
    suite.addTestSuite(PackageUriResolverTest.class);
    suite.addTestSuite(DirectoryBasedSourceContainerTest.class);
    //suite.addTestSuite(SourceFactoryTest.class);
    suite.addTestSuite(FileBasedSourceTest.class);
    ///////// end of source subset

    suite.addTest(com.google.dart.engine.utilities.TestAll.suite());
    return suite;
  }
}
