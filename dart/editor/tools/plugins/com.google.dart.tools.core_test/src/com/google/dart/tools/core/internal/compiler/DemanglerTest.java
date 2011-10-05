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
package com.google.dart.tools.core.internal.compiler;

import com.google.dart.compiler.DartCompiler;
import com.google.dart.tools.core.utilities.compiler.DartJsUtilities;

import junit.framework.TestCase;

import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class DemanglerTest extends TestCase {

  private static final String FUNCTION = "function";

  public void testDecodeJsFunctionName() throws Exception {
    TestLibrarySource app = new TestLibrarySource("MyApp.app");
    app.setEntryPoint("MyMain.main");
    app.addSource(new TestDartSource("MyMain.dart",
        "class MyMain { static void main() { int local = 7; } }"));
    TestCompilerConfiguration config = new TestCompilerConfiguration();
    TestArtifactProvider provider = new TestArtifactProvider();
    TestCompilerListener listener = new TestCompilerListener();

    String result = DartCompiler.compileLib(app, config, provider, listener);
    assertNull(result);
    listener.assertAllErrorsReported();

    LineNumberReader lineReader = new LineNumberReader(provider.getArtifactReader(app, "", "js"));
    List<String> oldLines = new ArrayList<String>();
    String line = null;
    Map<String, String> demangledMap = new HashMap<String, String>();
    try {
      while (true) {
        line = lineReader.readLine();
        if (line == null) {
          break;
        }
        oldLines.add(line);
        if (oldLines.size() > 5) {
          oldLines.remove(0);
        }
        if (line.startsWith(FUNCTION)) {
          String functName = line.substring(FUNCTION.length(), line.indexOf('(')).trim();
          String dartName = DartJsUtilities.demangleJsFunctionName(functName);
          demangledMap.put(dartName, functName);
        }
      }
    } finally {
      for (String dartName : new TreeSet<String>(demangledMap.keySet())) {
        String functName = demangledMap.get(dartName);
        System.out.println(pad(functName, 70) + " " + dartName);
      }

      // If all lines have not been read, then the test must have failed
      // so print lines before and after the failure for debugging
      if (line != null) {
        for (String eachLine : oldLines) {
          System.out.println(eachLine);
        }
        for (int i = 0; i < 5; i++) {
          line = lineReader.readLine();
          if (line == null) {
            break;
          }
          System.out.println(line);
        }
      }

      lineReader.close();
    }
  }

  private String pad(String text, int len) {
    StringBuilder builder = new StringBuilder(len);
    builder.append(text);
    while (builder.length() < len) {
      builder.append(' ');
    }
    return builder.toString();
  }
}
