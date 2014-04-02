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
package com.google.dart.java2dart.processor;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());
    suite.addTestSuite(BeautifySemanticProcessorTest.class);
    suite.addTestSuite(TypeSemanticProcessorTest.class);
    suite.addTestSuite(ObjectSemanticProcessorTest.class);
    suite.addTestSuite(MathSemanticProcessorTest.class);
    suite.addTestSuite(DateSemanticProcessorTest.class);
    suite.addTestSuite(ConstructorSemanticProcessorTest.class);
    suite.addTestSuite(CollectionSemanticProcessorTest.class);
    suite.addTestSuite(LoggerSemanticProcessorTest.class);
    suite.addTestSuite(PropertySemanticProcessorTest.class);
    suite.addTestSuite(GuavaSemanticProcessorTest.class);
    suite.addTestSuite(JUnitSemanticProcessorTest.class);
    suite.addTestSuite(EngineSemanticProcessorTest.class);
    suite.addTestSuite(EngineAnnotationProcessorTest.class);
    return suite;
  }
}
