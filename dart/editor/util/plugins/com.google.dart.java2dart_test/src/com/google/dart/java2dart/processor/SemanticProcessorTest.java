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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.java2dart.AbstractSemanticTest;
import com.google.dart.java2dart.Context;

import java.io.File;

/**
 * Base for {@link SemanticProcessor} tests.
 */
public class SemanticProcessorTest extends AbstractSemanticTest {
  protected final Context context = new Context();
  protected CompilationUnit unit;

  /**
   * Applies some {@link SemanticProcessor}s directly after {@link Context#translate()}.
   */
  protected void applyPostTranslateProcessors() {
  }

  protected final void assertFormattedSource(String... lines) {
    assertEquals(toString(lines), getFormattedSource(unit));
  }

  protected final void translateSingleFile(String... lines) throws Exception {
    File file = setFileLines("test/Test.java", toString(lines));
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    unit = context.translate();
    applyPostTranslateProcessors();
    new RenameConstructorsSemanticProcessor(context).process(unit);
  }
}
