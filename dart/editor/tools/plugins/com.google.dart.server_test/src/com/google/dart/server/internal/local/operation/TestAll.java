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
package com.google.dart.server.internal.local.operation;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());
    suite.addTestSuite(ApplyAnalysisDeltaOperationTest.class);
    suite.addTestSuite(ApplyChangesOperationTest.class);
    suite.addTestSuite(ApplyRefactoringOperationTest.class);
    suite.addTestSuite(ComputeCompletionSuggestionsOperationTest.class);
    suite.addTestSuite(ComputeFixesOperationTest.class);
    suite.addTestSuite(ComputeMinorRefactoringsOperationTest.class);
    suite.addTestSuite(ComputeTypeHierarchyOperationTest.class);
    suite.addTestSuite(CreateContextOperationTest.class);
    suite.addTestSuite(CreateRefactoringExtractLocalOperationTest.class);
    suite.addTestSuite(DeleteContextOperationTest.class);
    suite.addTestSuite(DeleteRefactoringOperationTest.class);
    suite.addTestSuite(GetContextOperationTest.class);
    suite.addTestSuite(GetFixableErrorCodesOperationTest.class);
    suite.addTestSuite(GetVersionOperationTest.class);
    suite.addTestSuite(PerformAnalysisOperationTest.class);
    suite.addTestSuite(SearchClassMemberDeclarationsOperationTest.class);
    suite.addTestSuite(SearchClassMemberReferencesOperationTest.class);
    suite.addTestSuite(SearchElementReferencesOperationTest.class);
    suite.addTestSuite(SearchTopLevelDeclarationsOperationTest.class);
    suite.addTestSuite(ServerOperationQueueTest.class);
    suite.addTestSuite(SetOptionsOperationTest.class);
    suite.addTestSuite(SetPrioritySourcesOperationTest.class);
    suite.addTestSuite(SetRefactoringExtractLocalOptionsOperationTest.class);
    suite.addTestSuite(ShutdownOperationTest.class);
    suite.addTestSuite(SubscribeOperationTest.class);
    return suite;
  }
}
