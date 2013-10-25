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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DartEntryImpl;
import com.google.dart.engine.internal.context.IncrementalAnalysisCache;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.internal.context.IncrementalAnalysisCache.update;

public class IncrementalAnalysisTaskTest extends EngineTestCase {
  private final Source source = new TestSource();
  private DartEntryImpl entry = new DartEntryImpl();

  public void test_accept() throws AnalysisException {
    IncrementalAnalysisTask task = new IncrementalAnalysisTask(null, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitIncrementalAnalysisTask(IncrementalAnalysisTask task)
          throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_perform() throws Exception {
    String oldCode = createSource(//
        "main() {",
        "  ",
        "}");
    String newCode = createSource(//
        "main() {",
        "  S",
        "}");

    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    CompilationUnit oldUnit = context.resolveCompilationUnit(source, source);
    assertNotNull(oldUnit);
    entry.setValue(DartEntry.RESOLVED_UNIT, source, oldUnit);

    int offset = newCode.indexOf('S');
    IncrementalAnalysisCache cache = update(null, source, oldCode, newCode, offset, 0, 1, entry);
    assertNotNull(cache);

    final IncrementalAnalysisTask task = new IncrementalAnalysisTask(context, cache);
    CompilationUnit newUnit = task.perform(new TestTaskVisitor<CompilationUnit>() {
      @Override
      public CompilationUnit visitIncrementalAnalysisTask(
          IncrementalAnalysisTask incrementalAnalysisTask) throws AnalysisException {
        return task.getCompilationUnit();
      }
    });
    assertNotNull(newUnit);

    final boolean[] found = new boolean[1];
    newUnit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        assertEquals("main", node.getName().getName());
        return super.visitMethodDeclaration(node);
      }

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        if ("S".equals(node.getName())) {
          found[0] = true;
        }
        return super.visitSimpleIdentifier(node);
      }
    });
//    assertTrue(found[0]);
  }
}
