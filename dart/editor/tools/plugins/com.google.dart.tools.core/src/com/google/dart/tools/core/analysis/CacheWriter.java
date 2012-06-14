package com.google.dart.tools.core.analysis;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Internal class for writing analysis state to disk between sessions.
 */
public class CacheWriter {
  private PrintWriter writer;

  public CacheWriter(Writer writer) {
    this.writer = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);
    this.writer.println(CacheReader.CACHE_VERSION_TAG);
  }

  /**
   * Write a value that can be read by {@link CacheReader#readBoolean()}
   */
  public void writeBoolean(boolean value) {
    writer.println(value ? "true" : "false");
  }

  /**
   * Write a collection of file paths to be read by
   * {@link CacheReader#readFilePaths(java.util.Collection, String)}
   * 
   * @param files the collection of files (not <code>null</code> )
   * @param endTag the tag used to denote the end of the collection of file paths
   */
  public void writeFilePaths(ArrayList<File> files, String endTag) {
    for (File libFile : files) {
      writeString(libFile.getPath());
    }
    writeString(endTag);
  }

  /**
   * Write a string that can be read by {@link CacheReader#readString()}
   */
  public void writeString(String string) {

    // Normal case ... write a line

    if (!string.startsWith("!") && string.indexOf('\r') == -1 && string.indexOf('\n') == -1) {
      writer.println(string);
      return;
    }

    // If the string contains a line separator, then string length is encoded
    // preceding the string as bang-number-bang

    writer.print('!');
    writer.print(string.length());
    writer.print('!');
    writer.println(string);
  }

  /**
   * Write a collection of key/value pairs to be read by
   * {@link CacheReader#readStringFileMap(String)} where the keys are strings and the values are
   * instances of {@link java.io.File}.
   * 
   * @param endTag the tag used to denote the end of the collection
   * @return the collection (not <code>null</code>)
   */
  public void writeStringFileMap(Map<String, File> map, String endTag) {
    for (Entry<String, File> entry : map.entrySet()) {
      writeString(entry.getKey());
      writeString(entry.getValue().getPath());
    }
    writeString(endTag);
  }

  /**
   * Write a collection of strings that can be read by {@link CacheReader#readStringSet(String)}
   * 
   * @param strings the collection of strings to be written (not <code>null</code>, contains no
   *          <code>null</code>s)
   * @param endTag the tag used to denote the end of the collection of strings (not
   *          <code>null</code>)
   */
  public void writeStringSet(Set<String> strings, String endTag) {
    for (String string : strings) {
      writeString(string);
    }
    writeString(endTag);
  }
}
