/*
 * Copyright 2013 Dart project authors.
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

/**
 * Reads a configuration file to determine what should be translated and how. The translator can
 * produce output on standard out
 * 
 * <pre>
 * java2dart &lt;source-folder> &lt;file> (&lt;file>)*
 * </pre>
 * 
 * or translate based upon information located in a configuration file
 * 
 * <pre>
 * java2dart &lt;config-file>.j2d
 * </pre>
 */
public class Config {

  /**
   * Determine the configuration based upon the specified arguments
   * 
   * @param args the arguments (not <code>null</code>, contains no <code>null</code>s)
   * @return the configuration or <code>null</code> if there was a problem determining the
   *         configuration
   */
  public static Config from(String[] args) {
    if (args == null || args.length == 0) {
      printUsage();
      return null;
    }
    if (args.length == 1) {
      if (args[0].equals("help")) {
        printHelp();
        return null;
      }
      try {
        return fromFile(args[0]);
      } catch (IOException e) {
        System.out.println("Failed reading config file: " + args[0]);
        e.printStackTrace();
        return null;
      }
    }
    Context context = new Context();
    File folder = toDirectory(args[0]);
    if (folder == null) {
      return null;
    }
    context.addSourceFolder(folder);
    for (int index = 1; index < args.length; index++) {
      File file = toFile(folder, args[index]);
      if (file == null) {
        return null;
      }
      context.addSourceFile(file);
    }
    return new Config(context);
  }

  private static Config fromFile(String configFilePath) throws IOException {
    File file = toFile(null, configFilePath);
    if (file == null) {
      return null;
    }
    LineNumberReader reader = new LineNumberReader(new FileReader(file));
    try {
      return fromReader(reader);
    } finally {
      reader.close();
    }
  }

  private static Config fromReader(LineNumberReader reader) throws IOException {
    Context context = new Context();
    Config config = new Config(context);
    File folder = null;
    while (true) {
      String line = reader.readLine();
      if (line == null) {
        break;
      }
      line = line.trim();

      // Ignore blank lines and lines starting with #
      if (line.length() == 0 || line.startsWith("#")) {
        continue;
      }

      // Line starting with "out:" is file into which source should be placed
      if (line.startsWith("out:")) {
        line = line.substring(4).trim();
        File file = new File(line);
        if (!file.getParentFile().exists()) {
          System.out.println("Expected existing directory to contain output file: " + line);
          return null;
        }
        config.setOutput(file);
        continue;
      }

      // Lines starting with "src:" are source folders
      if (line.startsWith("src:")) {
        line = line.substring(4).trim();
        folder = toDirectory(line);
        if (folder == null) {
          return null;
        }
        context.addSourceFolder(folder);
        continue;
      }

      // All other lines are source files and can be relative to the last defined folder
      File file = toFile(folder, line);
      if (file == null) {
        return null;
      }
      context.addSourceFile(file);
    }
    return config;
  }

  private static void printHelp() {
    printUsage();
  }

  private static void printUsage() {
    System.out.println("Usage: java2dart <source-folder> <file> (<file)*");
    System.out.println("   or: java2dart <config-file>.j2d");
  }

  private static File toDirectory(String path) {
    File directory = new File(path);
    if (directory.isDirectory()) {
      return directory;
    }
    System.out.println("Expected existing directory: " + path);
    printUsage();
    return null;
  }

  private static File toFile(File folder, String path) {
    File file = new File(path);
    if (!file.isAbsolute() && folder != null) {
      file = new File(folder, path);
    }
    if (file.isFile()) {
      return file;
    }
    System.out.println("Expected existing file: " + path);
    if (!new File(path).isAbsolute() && folder != null) {
      System.out.println("  in: " + folder);
    }
    printUsage();
    return null;
  }

  private File outputFile;

  private final Context context;

  private Config(Context context) {
    this.context = context;
  }

  public Context getContext() {
    return context;
  }

  public File getOutputFile() {
    return outputFile;
  }

  private void setOutput(File file) {
    this.outputFile = file;
  }
}
