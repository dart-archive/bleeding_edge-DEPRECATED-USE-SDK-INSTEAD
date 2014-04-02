/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());
    suite.addTestSuite(AnalysisEngineParticipantTest.class);
    suite.addTestSuite(AnalysisMarkerManagerTest.class);
    suite.addTestSuite(AnalysisManagerTest.class);
    suite.addTestSuite(AnalysisWorkerTest.class);
    suite.addTestSuite(BuildDartParticipantTest.class);
    suite.addTestSuite(BuildParticipantDeclarationTest.class);
//    suite.addTestSuite(CachingArtifactProviderTest.class);
    suite.addTestSuite(DartBuilderTest.class);
    suite.addTestSuite(DeltaProcessorTest.class);
    suite.addTestSuite(DeltaProcessorCanonicalTest.class);
    suite.addTestSuite(IgnoreResourceFilterTest.class);
//    suite.addTestSuite(LocalArtifactProviderTest.class);
//    suite.addTestSuite(RootArtifactProviderTest.class);
    return suite;
  }
}
