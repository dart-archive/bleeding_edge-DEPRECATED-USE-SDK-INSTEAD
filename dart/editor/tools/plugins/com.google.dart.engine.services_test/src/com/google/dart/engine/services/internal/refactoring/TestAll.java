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
package com.google.dart.engine.services.internal.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());
//    suite.addTestSuite(AngularRenameRefactoringTest.class);
    suite.addTestSuite(ConvertGetterToMethodRefactoringImplTest.class);
    suite.addTestSuite(ConvertMethodToGetterRefactoringImplTest.class);
    suite.addTestSuite(ExtractLocalRefactoringImplTest.class);
    suite.addTestSuite(ExtractMethodRefactoringImplTest.class);
    suite.addTestSuite(InlineLocalRefactoringImplTest.class);
    suite.addTestSuite(InlineMethodRefactoringImplTest.class);
    suite.addTestSuite(NamingConventionsTest.class);
    suite.addTestSuite(RenameClassMemberRefactoringImplTest.class);
    suite.addTestSuite(RenameConstructorRefactoringImplTest.class);
    suite.addTestSuite(RenameImportRefactoringImplTest.class);
    suite.addTestSuite(RenameLibraryRefactoringImplTest.class);
    suite.addTestSuite(RenameLocalRefactoringImplTest.class);
    suite.addTestSuite(RenameUnitMemberRefactoringImplTest.class);
    return suite;
  }
}
