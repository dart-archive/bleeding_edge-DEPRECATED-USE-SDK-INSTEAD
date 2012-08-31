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
package com.google.dart.tools.core;

import com.google.dart.tools.core.artifact.TestGenerateArtifacts;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());

    // Build the SDK index first
    suite.addTestSuite(TestGenerateArtifacts.class);

    suite.addTestSuite(DartCoreTest.class);
    suite.addTestSuite(PluginXMLTest.class);

    suite.addTest(com.google.dart.tools.core.analysis.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.dom.TestAll.suite());
//    suite.addTest(com.google.dart.tools.core.formatter.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.generator.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.index.TestAll.suite());
//    suite.addTest(com.google.dart.tools.core.indexer.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.internal.TestAll.suite());
    // suite.addTest(com.google.dart.tools.core.model.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.refresh.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.samples.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.search.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.utilities.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.workingcopy.TestAll.suite());
    return suite;
  }
}
