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
package com.google.dart.tools.core.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

class Listener implements AnalysisListener, IdleListener {
  private final Object lock = new Object();
  private final HashMap<String, HashSet<String>> parsed = new HashMap<String, HashSet<String>>();
  private final HashSet<String> resolved = new HashSet<String>();
  private final HashSet<String> discarded = new HashSet<String>();

  private final StringWriter duplicates = new StringWriter();

  private boolean idle;

  private final ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();

  public Listener(AnalysisServer server) {
    server.addIdleListener(this);
    server.getSavedContext().addAnalysisListener(this);
    server.getEditContext().addAnalysisListener(this);
  }

  @Override
  public void discarded(AnalysisEvent event) {
    synchronized (lock) {
      discarded.add(event.getLibraryFile().getPath());
      for (File file : event.getFiles()) {
        discarded.add(file.getPath());
      }
      lock.notifyAll();
    }
  }

  @Override
  public void idle(boolean idle) {
    synchronized (lock) {
      this.idle = idle;
      lock.notifyAll();
    }
  }

  @Override
  public void parsed(AnalysisEvent event) {
    synchronized (lock) {
      String libFilePath = event.getLibraryFile().getPath();
      HashSet<String> parsedInLib = parsed.get(libFilePath);
      if (parsedInLib == null) {
        parsedInLib = new HashSet<String>();
        parsed.put(libFilePath, parsedInLib);
      }
      for (File file : event.getFiles()) {
        if (!parsedInLib.add(file.getPath())) {
          PrintWriter pw = new PrintWriter(duplicates);
          pw.println("Duplicate parse: " + file);
          pw.println("  in " + libFilePath);
        }
      }
      errors.addAll(event.getErrors());
      lock.notifyAll();
    }
  }

  @Override
  public void resolved(AnalysisEvent event) {
    synchronized (lock) {
      String libPath = event.getLibraryFile().getPath();
      if (!resolved.add(libPath)) {
        new PrintWriter(duplicates).println("Duplicate resolution: " + libPath);
      }
      errors.addAll(event.getErrors());
      lock.notifyAll();
    }
  }

  void assertDiscarded(File... files) {
    synchronized (lock) {
      if (!wasDiscarded(files)) {
        failDiscarded(files);
      }
    }
  }

  void assertErrorCount(int expectedErrorCount) {
    synchronized (lock) {
      assertEquals(expectedErrorCount, errors.size());
    }
  }

  void assertNoDiscards() {
    synchronized (lock) {
      if (discarded.size() > 0) {
        StringWriter sw = new StringWriter(200);
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Expected no discards, but found:");
        for (String path : new TreeSet<String>(discarded)) {
          pw.println("  " + path);
        }
        fail(sw.toString().trim());
      }
    }
  }

  void assertNoDuplicates() {
    synchronized (lock) {
      if (duplicates.getBuffer().length() > 0) {
        fail(duplicates.toString());
      }
    }
  }

  void assertNoErrors() {
    assertErrorCount(0);
  }

  void assertParsed(File libraryFile, File... dartFiles) {
    synchronized (lock) {
      if (!wasParsed(libraryFile, dartFiles)) {
        failParsed(libraryFile, dartFiles);
      }
    }
  }

  void assertParsedCount(int expectedParseCount) {
    assertEquals(expectedParseCount, getParsedCount());
  }

  void assertResolved(File... libraryFiles) {
    synchronized (lock) {
      if (!wasResolved(libraryFiles)) {
        failResolved(libraryFiles);
      }
    }
  }

  void assertResolvedCount(int expectedResolvedCount) {
    synchronized (lock) {
      assertEquals(expectedResolvedCount, resolved.size());
    }
  }

  int getErrorCount() {
    synchronized (lock) {
      return errors.size();
    }
  }

  int getParsedCount() {
    synchronized (lock) {
      int count = 0;
      for (HashSet<String> parsedInLib : parsed.values()) {
        count += parsedInLib.size();
      }
      return count;
    }
  }

  boolean isIdle() {
    synchronized (lock) {
      return idle;
    }
  }

  void reset() {
    parsed.clear();
    resolved.clear();
    discarded.clear();
    duplicates.getBuffer().setLength(0);
    errors.clear();
  }

  void waitForDiscarded(long milliseconds, File... files) {
    synchronized (lock) {
      long end = System.currentTimeMillis() + milliseconds;
      while (!wasDiscarded(files)) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          failDiscarded(files);
          return;
        }
        try {
          lock.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
  }

  void waitForParsed(long milliseconds, final File libraryFile, final File... dartFiles) {
    synchronized (lock) {
      long end = System.currentTimeMillis() + milliseconds;
      while (!wasParsed(libraryFile, dartFiles)) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          failParsed(libraryFile, dartFiles);
          return;
        }
        try {
          lock.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
  }

  void waitForResolved(long milliseconds, final File libraryFiles) {
    synchronized (lock) {
      long end = System.currentTimeMillis() + milliseconds;
      while (!wasResolved(libraryFiles)) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          failResolved(libraryFiles);
          return;
        }
        try {
          lock.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
  }

  boolean wasDiscarded(File... files) {
    synchronized (lock) {
      for (File file : files) {
        if (!discarded.contains(file.getPath())) {
          return false;
        }
      }
    }
    return true;
  }

  boolean wasParsed(File libraryFile, File... dartFiles) {
    synchronized (lock) {
      HashSet<String> parsedInLibrary = parsed.get(libraryFile.getPath());
      if (parsedInLibrary == null) {
        return false;
      }
      for (File dartFile : dartFiles) {
        if (!parsedInLibrary.contains(dartFile.getPath())) {
          return false;
        }

      }
      return true;
    }
  }

  boolean wasResolved(File... libraryFiles) {
    synchronized (lock) {
      for (File libraryFile : libraryFiles) {
        if (!resolved.contains(libraryFile.getPath())) {
          return false;
        }

      }
      return true;
    }
  }

  private void failDiscarded(File... files) {
    StringWriter sw = new StringWriter(200);
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Expected " + files.length + " files discarded, but found " + discarded.size());
    if (files.length > 0) {
      pw.println("  expected:");
      for (File file : files) {
        pw.println("    " + file.getPath());
      }
    }
    if (discarded.size() > 0) {
      pw.println("  found:");
      for (String path : discarded) {
        pw.println("    " + path);
      }
    }
    fail(sw.toString().trim());
  }

  private void failParsed(File libraryFile, File... dartFiles) {
    StringWriter sw = new StringWriter(200);
    PrintWriter pw = new PrintWriter(sw);
    HashSet<String> parsedInLibrary = parsed.get(libraryFile.getPath());
    pw.println("Expected at least " + dartFiles.length + " parsed files in "
        + libraryFile.getName() + ", but found " + parsedInLibrary.size());
    pw.println("  " + libraryFile.getPath());
    pw.println("  expected:");
    for (File dartFile : dartFiles) {
      pw.println("    " + dartFile.getPath());
    }
    pw.println("  found:");
    for (String path : parsedInLibrary) {
      pw.println("    " + path);
    }
    fail(sw.toString().trim());
  }

  private void failResolved(File... libraryFiles) {
    StringWriter sw = new StringWriter(200);
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Expected at least " + libraryFiles.length + " resolved libraries, but found "
        + resolved.size());
    pw.println("  expected:");
    for (File libraryFile : libraryFiles) {
      pw.println("    " + libraryFile.getPath());
    }
    pw.println("  found:");
    for (String path : resolved) {
      pw.println("    " + path);
    }
    fail(sw.toString().trim());
  }
}
