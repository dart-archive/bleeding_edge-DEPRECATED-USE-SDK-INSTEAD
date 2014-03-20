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

/**
 * Test for {@link IOSemanticProcessor}.
 */
public class IOSemanticProcessorTest extends SemanticProcessorTest {
  public void test_File_new() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.io.File;",
        "import java.net.URI;",
        "public class Test {",
        "  public File newRelative(File parent, String child) {",
        "    return new File(parent, child);",
        "  }",
        "  public File newFromURI(URI uri) {",
        "    return new File(uri);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  JavaFile newRelative(JavaFile parent, String child) => new JavaFile.relative(parent, child);",
        "  JavaFile newFromURI(Uri uri) => new JavaFile.fromUri(uri);",
        "}");
  }

  public void test_File_properties() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.io.File;",
        "public class Test {",
        "  public void main(File f) {",
        "    f.getPath();",
        "    f.getAbsolutePath();",
        "    f.getAbsoluteFile();",
        "    f.getName();",
        "    f.exists();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void main(JavaFile f) {",
        "    f.getPath();",
        "    f.getAbsolutePath();",
        "    f.getAbsoluteFile();",
        "    f.getName();",
        "    f.exists();",
        "  }",
        "}");
  }

  public void test_File_separator() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.io.File;",
        "public class Test {",
        "  public char testA() {",
        "    return File.separatorChar;",
        "  }",
        "  public String testB() {",
        "    return File.separator;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int testA() => JavaFile.separatorChar;",
        "  String testB() => JavaFile.separator;",
        "}");
  }

  public void test_IOException() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.io.IOException;",
        "public class Test {",
        "  public void test(IOException e) {",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void test(JavaIOException e) {",
        "  }",
        "}");
  }

  public void test_System_out_println() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void test() {",
        "    System.out.print(1);",
        "    System.out.println(2);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void test() {",
        "    print(1);",
        "    print(2);",
        "  }",
        "}");
  }

  public void test_URI_create() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.net.URI;",
        "public class Test {",
        "  public URI test(String uriString) {",
        "    return URI.create(uriString);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  Uri test(String uriString) => parseUriWithException(uriString);",
        "}");
  }

  public void test_URI_new() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.io.File;",
        "import java.net.URI;",
        "public class Test {",
        "  public URI newFromPath(String absolutePath) {",
        "    URI result = new URI(null, null, absolutePath, null);",
        "    return result;",
        "  }",
        "  public URI newFromStr(String str) {",
        "    return new URI(str);",
        "  }",
        "  public URI newFromFile(File f) {",
        "    return f.toURI();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  Uri newFromPath(String absolutePath) {",
        "    Uri result = new Uri(path: absolutePath);",
        "    return result;",
        "  }",
        "  Uri newFromStr(String str) => parseUriWithException(str);",
        "  Uri newFromFile(JavaFile f) => f.toURI();",
        "}");
  }

  public void test_URI_properties() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.net.URI;",
        "public class Test {",
        "  public void main(URI p) {",
        "    p.getScheme();",
        "    p.getPath();",
        "    p.getSchemeSpecificPart();",
        "    p.resolve(p);",
        "    p.normalize();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void main(Uri p) {",
        "    p.scheme;",
        "    p.path;",
        "    p.path;",
        "    p.resolveUri(p);",
        "    p;",
        "  }",
        "}");
  }

  private void runProcessor() {
    new IOSemanticProcessor(context).process(unit);
  }
}
