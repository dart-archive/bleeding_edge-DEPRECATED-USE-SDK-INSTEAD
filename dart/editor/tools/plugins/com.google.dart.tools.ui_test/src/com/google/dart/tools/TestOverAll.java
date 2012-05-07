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
package com.google.dart.tools;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Build Bot uses this as an entry point to run the tests. Important note: All tests that are run by
 * this suite must be able to run headlessly.
 */
public class TestOverAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in all test plug-ins");

    suite.addTest(com.google.dart.tools.core.TestAll.suite());
    //these can only be run locally
//    suite.addTest(com.google.dart.tools.ui.TestAll.suite());
    return suite;
  }

}
