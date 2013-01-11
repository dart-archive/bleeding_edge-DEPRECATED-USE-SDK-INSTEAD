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

package com.google.dart.java2dart;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import java.io.FileWriter;
import java.io.Writer;

public class Main {
  public static void main(String[] args) throws Exception {
    Config config = Config.from(args);
    if (config == null) {
      System.exit(1);
    }
    Context context = config.getContext();
//    String javaSource = Files.toString(file, Charset.forName("UTF-8"));
//    org.eclipse.jdt.core.dom.CompilationUnit javaUnit = parseJava(javaSource);
//    Context context = new Context();
//    CompilationUnit dartUnit = SyntaxTranslator.translate(context, javaUnit);
    CompilationUnit dartUnit = context.translate();
    String dartSource = getFormattedSource(dartUnit);
    if (config.getOutputFile() == null) {
      System.out.println(dartSource);
      return;
    }
    Writer writer = new FileWriter(config.getOutputFile());
    try {
      writer.write(dartSource);
    } finally {
      writer.close();
    }
    System.out.println("Wrote " + config.getOutputFile());
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
