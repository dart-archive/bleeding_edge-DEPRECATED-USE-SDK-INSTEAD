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

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Test for general Java semantics to Dart translation.
 */
public class AbstractSemanticTest extends TestCase {

  /**
   * @return the formatted Dart source dump of the given {@link ASTNode}.
   */
  protected static String getFormattedSource(ASTNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToFormattedSourceVisitor(writer));
    return writer.toString();
  }

  protected static void printFormattedSource(ASTNode node) {
    String source = getFormattedSource(node);
    String[] lines = StringUtils.split(source, '\n');
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      line = StringUtils.replace(line, "\"", "\\\"");
      System.out.print("\"");
      System.out.print(line);
      if (i != lines.length - 1) {
        System.out.println("\",");
      } else {
        System.out.println("\"");
      }
    }
  }

  /**
   * @return the single {@link String} with "\n" separated lines.
   */
  protected static String toString(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  protected File tmpFolder;

  /**
   * Sets the content of the file with given path relative to {@link #tmpFolder}.
   */
  protected File setFileLines(String path, String content) throws Exception {
    File toFile = new File(tmpFolder, path);
    Files.createParentDirs(toFile);
    Files.write(content, toFile, Charsets.UTF_8);
    return toFile;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    tmpFolder = Files.createTempDir();
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtils.deleteDirectory(tmpFolder);
    super.tearDown();
  }
}
