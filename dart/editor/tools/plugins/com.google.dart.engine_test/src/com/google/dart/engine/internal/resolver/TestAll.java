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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ExtendedTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new ExtendedTestSuite("Tests in " + TestAll.class.getPackage().getName());
    suite.addTestSuite(DeclarationMatcherTest.class);
    suite.addTestSuite(ElementResolverTest.class);
    suite.addTestSuite(IncrementalResolverTest.class);
    suite.addTestSuite(InheritanceManagerTest.class);
    suite.addTestSuite(LibraryElementBuilderTest.class);
    suite.addTestSuite(LibraryResolver2Test.class);
    suite.addTestSuite(LibraryResolverTest.class);
    suite.addTestSuite(LibraryTest.class);
    suite.addTestSuite(StaticTypeAnalyzerTest.class);
    suite.addTestSuite(SubtypeManagerTest.class);
    suite.addTestSuite(TypeOverrideManagerTest.class);
    suite.addTestSuite(TypeProviderImplTest.class);
    suite.addTestSuite(TypeResolverVisitorTest.class);
    return suite;
  }
}
