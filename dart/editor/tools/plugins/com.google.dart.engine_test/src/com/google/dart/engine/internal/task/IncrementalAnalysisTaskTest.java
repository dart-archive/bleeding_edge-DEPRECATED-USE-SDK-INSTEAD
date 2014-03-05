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
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DartEntryImpl;
import com.google.dart.engine.internal.context.IncrementalAnalysisCache;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.internal.context.IncrementalAnalysisCache.update;

import java.io.File;

public class IncrementalAnalysisTaskTest extends EngineTestCase {

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
    // main() {} String foo;
    // main() {String} String foo;
    CompilationUnit newUnit = assertTask("main() {", "", "String", "} String foo;");

    NodeList<CompilationUnitMember> declarations = newUnit.getDeclarations();

    FunctionDeclaration main = (FunctionDeclaration) declarations.get(0);
    assertEquals("main", main.getName().getName());

    BlockFunctionBody body = (BlockFunctionBody) main.getFunctionExpression().getBody();
    ExpressionStatement statement = (ExpressionStatement) body.getBlock().getStatements().get(0);
    assertEquals("String;", statement.toSource()); // ';' is a synthetic node added by parser

    SimpleIdentifier identifier = (SimpleIdentifier) statement.getExpression();
    assertEquals("String", identifier.getName());
    assertNotNull(identifier.getStaticElement()); // assert element reference is added

    TopLevelVariableDeclaration fooDecl = (TopLevelVariableDeclaration) declarations.get(1);
    SimpleIdentifier fooName = fooDecl.getVariables().getVariables().get(0).getName();
    assertEquals("foo", fooName.getName());
    assertNotNull(fooName.getStaticElement()); // assert element reference is preserved
  }

  private CompilationUnit assertTask(String prefix, String removed, String added, String suffix)
      throws AnalysisException {
    String oldCode = createSource(prefix + removed + suffix);
    String newCode = createSource(prefix + added + suffix);

    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();

    Source source = new TestSource(new File("/test.dart"), oldCode);

    DartEntryImpl entry = new DartEntryImpl();
    CompilationUnit oldUnit = context.resolveCompilationUnit(source, source);
    assertNotNull(oldUnit);
    entry.setValueInLibrary(DartEntry.RESOLVED_UNIT, source, oldUnit);

    IncrementalAnalysisCache cache = update(
        null,
        source,
        oldCode,
        newCode,
        prefix.length(),
        removed.length(),
        added.length(),
        entry);
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
    return newUnit;
  }
}
