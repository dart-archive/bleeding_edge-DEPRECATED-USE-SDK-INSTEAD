/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.workingcopy;

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceFileElement;
import com.google.dart.tools.core.problem.ProblemRequestor;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;

public class WorkingCopyOwnerTest extends TestCase {
  public void test_WorkingCopyOwner_newWorkingCopy() throws DartModelException {
    CompilationUnit workingCopy = newExternalWorkingCopy("X.java", "public class X {\n" + "}");
    assertTrue("Working copy should exist", workingCopy.exists());
  }

  protected CompilationUnit newExternalWorkingCopy(String name,
      final ProblemRequestor problemRequestor, final String contents) throws DartModelException {
    WorkingCopyOwner owner = new WorkingCopyOwner() {
      @Override
      public Buffer createBuffer(SourceFileElement<?> wc) {
        Buffer buffer = super.createBuffer(wc);
        buffer.setContents(contents);
        return buffer;
      }

      @Override
      public ProblemRequestor getProblemRequestor(SourceFileElement<?> workingCopy) {
        return problemRequestor;
      }
    };
    return owner.newWorkingCopy(new Path(name), null);
  }

  protected CompilationUnit newExternalWorkingCopy(String name, final String contents)
      throws DartModelException {
    return newExternalWorkingCopy(name, null, contents);
  }
}
