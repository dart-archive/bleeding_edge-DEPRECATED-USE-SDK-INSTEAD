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
package com.google.dart.tools.core.internal.index.contributor;

import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.internal.index.store.IndexStore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

import java.lang.reflect.Method;

public class IndexContributorTest extends TestCase {
  public void test_IndexContributor_peekElement_topLevel() throws Exception {
    TestProject project = new TestProject("Test");
    try {
      CompilationUnit unit = project.setUnitContent("a.dart", "");
      IndexContributor contributor = new IndexContributor(new IndexStore(), unit);
      assertNull(peekElement(contributor));
    } finally {
      project.dispose();
    }
  }

  /**
   * Return the result of invoking {@link IndexContributor#peekElement()} on the given contributor.
   * 
   * @param contributor the contributor on which the method is to be invoked
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private Element peekElement(IndexContributor contributor) throws Exception {
    Method method = IndexContributor.class.getDeclaredMethod("peekElement");
    method.setAccessible(true);
    return (Element) method.invoke(contributor);
  }
}
