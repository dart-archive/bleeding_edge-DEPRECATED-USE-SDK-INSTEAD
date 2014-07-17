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
package com.google.dart.tools.ui.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());
    suite.addTestSuite(ExecutionUtilsTest.class);
    suite.addTestSuite(ReflectionUtilsTest.class);
    // participants
//    suite.addTestSuite(DeleteResourceParticipantTest.class);
//    suite.addTestSuite(RenameResourceParticipant_OLD_Test.class);
//    suite.addTestSuite(MoveResourceParticipantTest.class);
    suite.addTestSuite(ServiceUtilsTest.class);
    // extract
//    suite.addTestSuite(ExtractLocalRefactoringTest.class);
//    suite.addTestSuite(ExtractMethodRefactoringTest.class);
    // inline
//    suite.addTestSuite(InlineLocalRefactoringTest.class);
//    suite.addTestSuite(InlineMethodRefactoringTest.class);
    // convert
//    suite.addTestSuite(ConvertMethodToGetterRefactoringTest.class);
//    suite.addTestSuite(ConvertGetterToMethodRefactoringTest.class);
//    suite.addTestSuite(ConvertOptionalParametersToNamedRefactoringTest.class);
    // done
    return suite;
  }
}
