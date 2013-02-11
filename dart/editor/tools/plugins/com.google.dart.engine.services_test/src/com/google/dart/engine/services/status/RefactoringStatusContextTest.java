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

package com.google.dart.engine.services.status;

import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.utilities.source.SourceRange;

import static org.fest.assertions.Assertions.assertThat;

public class RefactoringStatusContextTest extends AbstractDartTest {

  public void test_access() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {}",
        "");
    SourceRange range = new SourceRange(10, 20);
    RefactoringStatusContext context = RefactoringStatusContext.create(testUnit, range);
    // access
    assertSame(testUnit.getElement().getContext(), context.getContext());
    assertSame(testUnit.getElement().getSource(), context.getSource());
    assertEquals(range, context.getRange());
    // toString()
    {
      String str = context.toString();
      assertThat(str).contains("range=" + range);
      assertThat(str).startsWith("RefactoringStatusContext{source=");
    }
  }
}
