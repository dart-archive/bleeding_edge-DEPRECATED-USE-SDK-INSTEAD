// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

/**
 * Used to generated the file dartlib.html (which contains all the dart
 * core library files embedded in script tags on an html page).
 *
 * TODO - remove hard-coded paths and integrate into dartboard build process
 */
public class BuildDartlib {
  public final String dartDir;

  // map from script tag id to File whose contents goes in that script tag
  Map<String, File> idToFile = new TreeMap<>();

  private BuildDartlib(String dartDir) {
    this.dartDir = dartDir;
  }

  public static void main(String [] argv) throws Exception {
    BuildDartlib dartlib = new BuildDartlib("/usr/local/google/home/mattsh/dart-code1/dart/");
    dartlib.go();
  }

  public void go() throws Exception {
    loadLibraries();
    File outputFile = new File(dartDir + "samples/dartboard/dartlib.html");
    Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF8");
    writer.write("<html><head>\n");
    for (Map.Entry<String, File> entry : idToFile.entrySet()) {
      writer.write(makeScriptTag(entry.getKey(), entry.getValue()));
    }
    writer.write("</head></html>\n");
    writer.close();
  }

  /**
   * Load all the libraries that are allowed to follow "dart:", e.g. dart:html
   * dart:json, etc.
   */
  private void loadLibraries() throws Exception {
    File dartDef = new File(dartDir + "samples/dartboard/build_dartlib/core.dartdef");

    LineNumberReader reader = new LineNumberReader(
        new InputStreamReader(new FileInputStream(dartDef), "UTF8"));

    while (true) {
      String line = reader.readLine();
      if (line == null) {
        break;
      }
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }
      if (line.startsWith("#")) {
        continue;
      }
      String [] words = line.split(" +");
      String value = words[1];
      String relPath = value.substring(1, value.length() - 1);
      File file = new File(dartDir + relPath);
      if (!file.exists()) {
        throw new Exception("can't find file " + file.getCanonicalPath());
      }
      processFile(file);
    }
  }

  /**
   * load this dart file, and all dart files this file depends on
   * (search through the dart file for any #import and #source lines to
   * figure out what other files this file depends on)
   */
  void processFile(File file) throws Exception {
    File dir = file.getParentFile();
    LineNumberReader reader = new LineNumberReader(
        new InputStreamReader(new FileInputStream(file), "UTF8"));

    System.out.println("processing file " + file.getCanonicalPath());

    while (true) {
      String line = reader.readLine();
      if (line == null) {
        break;
      }
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }

      if (line.startsWith("#import")) {
        if (!line.substring("#import(\"".length()).startsWith("dart:")) {
          throw new Exception("found import line " + line);
        }
      }

      if (line.startsWith("#source") || line.startsWith("#native")) {
        char quoteChar = line.charAt("#source(".length());
        if (quoteChar != '\"' && quoteChar != '\'') {
          throw new Exception("quote not found");
        }
        int startQuote = line.indexOf(quoteChar);
        int endQuote = line.indexOf(quoteChar, startQuote + 1);
        String relPath = line.substring(startQuote + 1, endQuote);
        File sourcedFile = new File(dir + "/" + relPath);
        addFile(sourcedFile);
      }
    }
  }

  private void addFile(File file) throws Exception {
    String path = file.getCanonicalPath();
    if (!file.exists()) {
      throw new Exception("cannot find file " + path);
    }
    if (!path.startsWith(dartDir)) {
      throw new Exception("file " + path + " is outside dart tree");
    }
    String id = makeId(file);
    if (idToFile.get(id) != null) {
      throw new Exception("repeated id, same file used twice?");
    }
    idToFile.put(id, file);
  }

  private String makeId(File file) throws Exception {
    String path = file.getCanonicalPath();
    path = path.replace(dartDir, "dartdir/");
    String id = path.replaceAll("/", "_");
    id = id.replace(".", "_");
    return id;
  }

  /**
   * create a script tag whose id is derived from the file name
   */
  private static String makeScriptTag(String id, File file) throws Exception {
    System.out.println("creating script tag id " + id);
    return String.format("<script type=\"application//inert\" id=\"%s\">\n%s\n</script>\n", id, readFile(file));
  }

  /**
   * read contents of file to string
   */
  private static String readFile(File file) throws Exception {
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
    while (true) {
      String line = reader.readLine();
      if (line == null) {
        break;
      }
      sb.append(line).append("\n");
    }
    return sb.toString();
  }
}
