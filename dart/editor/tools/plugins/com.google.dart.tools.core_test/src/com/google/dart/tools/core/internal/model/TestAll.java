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
package com.google.dart.tools.core.internal.model;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());
    suite.addTestSuite(CompilationUnitImplTest.class);
//    suite.addTestSuite(CompilationUnitImpl2Test.class);
    suite.addTestSuite(DartElementImplTest.class);
    suite.addTestSuite(DartFieldImplTest.class);
    suite.addTestSuite(DartLibraryImplTest.class);
    suite.addTestSuite(DartImportImplTest.class);
    suite.addTestSuite(DartMethodImplTest.class);
    suite.addTestSuite(DartModelImplTest.class);
    suite.addTestSuite(DartModelManagerTest.class);
    suite.addTestSuite(DartProjectImplTest.class);
    suite.addTestSuite(DartProjectNatureTest.class);
    suite.addTestSuite(DartTypeImplTest.class);
    suite.addTestSuite(DartTypeParameterImplTest.class);
    suite.addTestSuite(DartVariableImplTest.class);
    suite.addTestSuite(DartFunctionImplTest.class);
    suite.addTestSuite(HTMLFileImplTest.class);
    suite.addTestSuite(DartIgnoreFileTest.class);
    suite.addTestSuite(SystemLibraryManagerProviderAnyTest.class);

    suite.addTest(com.google.dart.tools.core.internal.model.info.TestAll.suite());
    return suite;
  }
}
