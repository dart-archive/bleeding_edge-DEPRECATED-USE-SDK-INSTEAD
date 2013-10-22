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
package editor.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll2 {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll2.class.getPackage().getName());
    // TODO(scheglov) too flaky :-(
//    suite.addTestSuite(RenameRefactoringTest2.class);
//    suite.addTestSuite(ExtractLocalRefactoringTest2.class);
//    suite.addTestSuite(ExtractMethodRefactoringTest2.class);
//    suite.addTestSuite(InlineLocalRefactoringTest2.class);
//    suite.addTestSuite(InlineMethodRefactoringTest2.class);
//    suite.addTestSuite(ConvertMethodToGetterRefactoringTest2.class);
//    suite.addTestSuite(ConvertGetterToMethodRefactoringTest2.class);
    return suite;
  }
}
