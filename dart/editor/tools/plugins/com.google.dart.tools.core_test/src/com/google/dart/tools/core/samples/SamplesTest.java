/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.samples;

import com.google.common.io.Files;
import com.google.common.io.LineReader;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.util.DartSourceString;
import com.google.dart.tools.core.internal.util.LibraryReferenceFinder;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Assert valid dart source in the editor "samples" directory. </br>Each source file must
 * <ul>
 * <li>have no parse errors</li>
 * <li>only #import, #source, and #resource existing files</li>
 * </ul>
 * and each html file must
 * <ul>
 * <li>only reference existing files</li>
 * </ul>
 */
public class SamplesTest extends TestCase {
  public interface Listener {
    void logParse(long elapseTime, String... comments);
  }

  private static final String[] JS_DART_TRIGGERS = new String[] {
      "http://dart.googlecode.com/svn/branches/bleeding_edge/dart/client/dart.js", // most common
      "dart.js", // used by Total
  };

  // TODO (danrubel) dynamically build this list from ???
  private static HashSet<String> KNOWN_DART_LIBS = new HashSet<String>();

  static {
    KNOWN_DART_LIBS.add("dart:core");
    KNOWN_DART_LIBS.add("dart:collection");
    KNOWN_DART_LIBS.add("dart:html");
    KNOWN_DART_LIBS.add("dart:io");
    KNOWN_DART_LIBS.add("dart:isolate");
    KNOWN_DART_LIBS.add("dart:json");
    KNOWN_DART_LIBS.add("dart:uri");
    KNOWN_DART_LIBS.add("dart:utf8");
  }

  private final Listener listener;

  private Collection<File> libraryFiles;
  private Collection<File> applicationFiles;
  private Collection<File> badSampleFiles;

  public SamplesTest() {
    this(null);
  }

  public SamplesTest(Listener listener) {
    this.listener = listener;
  }

  public void testSamples() {
    File installDir = new File(Platform.getInstallLocation().getURL().getFile());
    testSamples(new File(installDir, "samples"));
  }

  public void testSamples(File samplesDir) {
    assertTrue(samplesDir.getAbsolutePath(), samplesDir.exists());
    badSampleFiles = new ArrayList<File>();
    libraryFiles = new ArrayList<File>();
    applicationFiles = new ArrayList<File>();
    visitDirectory(samplesDir);
    for (File libFile : libraryFiles) {
      analyzeLibrary(libFile);
    }
    assertEquals("Bad sample files", 0, badSampleFiles.size());
  }

  /**
   * Analyze the library and record problems and performance
   */
  private void analyzeLibrary(File libFile) {
//    DartCompilerUtilities.secureCompileLib(libSource, config, provider, listener);
  }

  /**
   * Validate the specified relative path in the specified file
   * 
   * @return <code>true</code> if the referenced file exists, else <code>false</code>
   */
  private boolean verifyRelativePath(File file, String relPath) {
    if (relPath.startsWith("dart:") || relPath.startsWith("package:")) {
      return true;
    }

    IPath dirPath = new Path(file.getPath()).removeLastSegments(1);
    final File referencedFile = dirPath.append(relPath).toFile();
    if (!referencedFile.exists()) {
      System.out.println("Referenced file does not exist");
      System.out.println("  Source file: " + file.getPath());
      System.out.println("  Relative path: " + relPath);
      System.out.println("  Absolute path: " + referencedFile.getPath());
      return false;
    }
    return true;
  }

  /**
   * Parse the source file and record problems and performance
   */
  private void visitDartFile(File sourceFile) {

    // Assert file is readable

    String source;
    try {
      source = Files.toString(sourceFile, Charset.defaultCharset());
    } catch (IOException e) {
      badSampleFiles.add(sourceFile);
      System.out.println("Failed to read " + sourceFile.getPath());
      e.printStackTrace();
      return;
    }

    // Assert file is parsable with no exceptions

    DartSource sourceRef = new DartSourceString(sourceFile.getPath(), source);
    Collection<DartCompilationError> parseErrors = new ArrayList<DartCompilationError>();
    DartUnit unit;
    try {
      long start = System.currentTimeMillis();
      unit = DartCompilerUtilities.parseSource(sourceRef, source, parseErrors);
      if (listener != null) {
        long elapseTime = System.currentTimeMillis() - start;
        listener.logParse(elapseTime, sourceFile.getName());
      }
    } catch (Exception e) {
      badSampleFiles.add(sourceFile);
      System.out.println("Failed to parse " + sourceFile.getPath());
      e.printStackTrace();
      return;
    }

    // Assert no parse errors

    if (parseErrors.size() > 0) {
      badSampleFiles.add(sourceFile);
      System.out.println("Parse errors in " + sourceFile.getPath());
      List<String> lines;
      try {
        lines = Files.readLines(sourceFile, Charset.defaultCharset());
      } catch (IOException e) {
        System.out.println(e.getMessage());
        lines = new ArrayList<String>();
      }
      for (DartCompilationError error : parseErrors) {
        if (error.getLineNumber() <= lines.size()) {
          System.out.println(lines.get(error.getLineNumber() - 1));
          for (int count = error.getColumnNumber() - 1; count > 0; count--) {
            System.out.print(' ');
          }
          System.out.println('^');
        }
        System.out.println("  " + error);
      }
    }

    // Assert all referenced files exists and record libraries/applications

    boolean invalidReference = false;
    for (DartDirective directive : unit.getDirectives()) {
      String relPath = null;
      if (directive instanceof DartImportDirective) {
        relPath = ((DartImportDirective) directive).getLibraryUri().getValue();
        if (KNOWN_DART_LIBS.contains(relPath)) {
          continue;
        }
      } else if (directive instanceof DartSourceDirective) {
        relPath = ((DartSourceDirective) directive).getSourceUri().getValue();
      } else if (directive instanceof DartLibraryDirective) {
        libraryFiles.add(sourceFile);
        for (DartNode node : unit.getTopLevelNodes()) {
          if (node instanceof DartMethodDefinition) {
            DartMethodDefinition mth = (DartMethodDefinition) node;
            DartExpression name = mth.getName();
            if (name instanceof DartIdentifier) {
              if ("main".equals(((DartIdentifier) name).getName())) {
                applicationFiles.add(sourceFile);
              }
            }
          }
        }
      }
      if (relPath == null) {
        continue;
      }
      if (!verifyRelativePath(sourceFile, relPath)) {
        invalidReference = true;
      }
    }
    if (invalidReference) {
      badSampleFiles.add(sourceFile);
      return;
    }

  }

  private void visitDirectory(File dir) {
    // Don't try and analyze samples that depend on pub.
    if (dir.isDirectory() && new File(dir, "pubspec.yaml").exists()) {
      return;
    }

    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        visitDirectory(file);
        continue;
      }
      String name = file.getName();
      if (name.endsWith(".dart")) {
        visitDartFile(file);
        continue;
      }
      if (name.endsWith(".html") || name.endsWith(".htm")) {
        visitHmtlFile(file);
        continue;
      }
    }
  }

  /**
   * Assert that the html file only references existing source files
   */
  private void visitHmtlFile(File htmlFile) {

    // Assert file is readable

    String htmlSource;
    try {
      htmlSource = Files.toString(htmlFile, Charset.defaultCharset());
    } catch (IOException e) {
      badSampleFiles.add(htmlFile);
      System.out.println("Failed to read " + htmlFile.getPath());
      e.printStackTrace();
      return;
    }

    // Assert file only references valid content

    List<String> relativePaths = LibraryReferenceFinder.findInHTML(htmlSource);
    boolean dartReferenceFound = false;
    boolean invalidReference = false;
    for (String relPath : relativePaths) {
      verifyRelativePath(htmlFile, relPath);
      if (relPath.endsWith(".dart")) {
        dartReferenceFound = true;
      }
    }
    if (!dartReferenceFound) {
      System.out.println("Did not find any dart references in HTML file");
      System.out.println("  HTML file: " + htmlFile.getPath());
      badSampleFiles.add(htmlFile);
      return;
    }
    if (invalidReference) {
      badSampleFiles.add(htmlFile);
      return;
    }

    // Assert file contains the appropriate Dartium required JS

    boolean jsDartTriggerFound = false;
    LineReader reader = new LineReader(new StringReader(htmlSource));
    while (true) {
      String line;
      try {
        line = reader.readLine();
      } catch (IOException e) {
        // Should not happen
        throw new RuntimeException(e);
      }
      if (line == null) {
        break;
      }
      if (!jsDartTriggerFound) {
        for (String trigger : JS_DART_TRIGGERS) {
          if (line.contains(trigger)) {
            jsDartTriggerFound = true;
            break;
          }
        }
      }
    }
    if (!jsDartTriggerFound) {
      System.out.println("Did not find required JS in HTML file");
      System.out.println("  HTML file: " + htmlFile.getPath());
      System.out.println("  Missing " + JS_DART_TRIGGERS[0]);
      badSampleFiles.add(htmlFile);
      return;
    }
  }
}
