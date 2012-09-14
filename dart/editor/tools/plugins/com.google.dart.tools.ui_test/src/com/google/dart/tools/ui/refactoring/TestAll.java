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
    // rename
    suite.addTestSuite(RenameAnalyzeUtilTest.class);
    suite.addTestSuite(RenameLocalVariableProcessorTest.class);
    suite.addTestSuite(RenameFieldProcessorTest.class);
    suite.addTestSuite(RenameMethodProcessorTest.class);
    suite.addTestSuite(RenameTypeProcessorTest.class);
    suite.addTestSuite(RenameLocalFunctionProcessorTest.class);
    suite.addTestSuite(RenameFunctionProcessorTest.class);
    suite.addTestSuite(RenameGlobalVariableProcessorTest.class);
    suite.addTestSuite(RenameFunctionTypeAliasProcessorTest.class);
    suite.addTestSuite(RenameTypeParameterProcessorTest.class);
    suite.addTestSuite(RenameImportProcessorTest.class);
    // participants
    suite.addTestSuite(DeleteResourceParticipantTest.class);
    suite.addTestSuite(RenameResourceParticipantTest.class);
    // extract
    suite.addTestSuite(ExtractUtilsTest.class);
    suite.addTestSuite(ExtractLocalRefactoringTest.class);
    suite.addTestSuite(ExtractMethodRefactoringTest.class);
    // inline
    suite.addTestSuite(InlineLocalRefactoringTest.class);
    suite.addTestSuite(InlineMethodRefactoringTest.class);
    // convert
    suite.addTestSuite(ConvertMethodToGetterRefactoringTest.class);
    suite.addTestSuite(ConvertOptionalParametersToNamedRefactoringTest.class);
    // done
    return suite;
  }
}
