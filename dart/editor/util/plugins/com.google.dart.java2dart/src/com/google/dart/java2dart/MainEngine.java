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

package com.google.dart.java2dart;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import java.io.File;

/**
 * Translates some parts of "com.google.dart.engine" project.
 */
public class MainEngine {
  public static void main(String[] args) throws Exception {
    File engineFolder = new File("../../../tools/plugins/com.google.dart.engine/src");
    engineFolder = engineFolder.getCanonicalFile();
    // configure Context
    Context context = new Context();
    context.addSourceFolder(engineFolder);
    context.addSourceFiles(new File(engineFolder, "com/google/dart/engine/scanner"));
    // translate into single CompilationUnit
    CompilationUnit dartUnit = context.translate();
    // TODO(scheglov) dump as single source
    String dartSource = getFormattedSource(dartUnit);
    System.out.println(dartSource);
  }

  /**
   * @return the formatted Dart source dump of the given {@link ASTNode}.
   */
  private static String getFormattedSource(ASTNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToFormattedSourceVisitor(writer));
    return writer.toString();
  }
}
