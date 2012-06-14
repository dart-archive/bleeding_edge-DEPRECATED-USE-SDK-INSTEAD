package com.google.dart.tools.core.analysis;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Internal class for reading analysis state from disk when starting a new session.
 */
public class CacheReader {
  static final String CACHE_VERSION_TAG = "v2";

  private final LineNumberReader reader;

  public CacheReader(Reader reader) throws IOException {
    if (!reader.markSupported()) {
      throw new IllegalArgumentException();
    }
    this.reader = reader instanceof LineNumberReader ? (LineNumberReader) reader
        : new LineNumberReader(reader);
    String line = this.reader.readLine();
    if (!CACHE_VERSION_TAG.equals(line)) {
      throw new IOException("Expected cache version " + CACHE_VERSION_TAG + " but found " + line);
    }
  }

  /**
   * Read a boolean originally written by {@link CacheWriter#writeBoolean(boolean)}
   */
  public boolean readBoolean() throws IOException {
    String line = reader.readLine();
    if ("true".equals(line)) {
      return true;
    }
    if ("false".equals(line)) {
      return false;
    }
    throw new IOException("Expected true or false, but found " + line);
  }

  /**
   * Read a collection of file paths originally written by {@link
   * CacheWriter#writeFilePaths(Collection<File>, String)}
   * 
   * @param files the collection into which files are placed (not <code>null</code> )
   * @param endTag the tag used to denote the end of the collection of file paths
   */
  public void readFilePaths(Collection<File> files, String endTag) throws IOException {
    while (true) {
      String path = readString();
      if (path == null) {
        throw new IOException("Expected " + endTag + " but found EOF");
      }
      if (path.equals(endTag)) {
        break;
      }
      files.add(new File(path));
    }
  }

  /**
   * Read a string originally written by {@link CacheWriter#writeString(String)}
   * 
   * @return the string or <code>null</code> if EOF
   */
  public String readString() throws IOException {

    // Normal case... read a line

    reader.mark(1);
    int ch = reader.read();
    if (ch == -1) {
      throw new IOException("Unexpected EOF when reading string");
    }
    if (ch != '!') {
      reader.reset();
      return reader.readLine();
    }

    // If the string contains a line separator, then string length is encoded
    // preceding the string as bang-number-bang

    int length = 0;
    while (true) {
      ch = reader.read();
      if (ch == '!') {
        break;
      }
      if (ch < '0' || ch > '9') {
        throw new IOException("Expected digit, but found character value " + ch);
      }
      length = length * 10 + ch - '0';
    }
    char[] buf = new char[length];
    int count = reader.read(buf);
    if (length != count) {
      throw new IOException("Expected " + length + " characters, but found only " + count);
    }
    reader.readLine();
    return new String(buf);
  }

  /**
   * Read a collection of key/value pairs originally written by
   * {@link CacheWriter#writeStringFileMap(Map, String)} where the keys are strings and the values
   * are instances of {@link java.io.File}.
   * 
   * @param endTag the tag used to denote the end of the collection
   * @return the collection (not <code>null</code>)
   */
  public HashMap<String, File> readStringFileMap(String endTag) throws IOException {
    HashMap<String, File> result = new HashMap<String, File>();
    while (true) {
      String key = readString();
      if (key == null) {
        throw new IOException("Expected " + endTag + " but found EOF");
      }
      if (key.equals(endTag)) {
        break;
      }
      String absFilePath = readString();
      if (absFilePath == null) {
        throw new IOException("Expected absolute file path but found EOF");
      }
      result.put(key, new File(absFilePath));
    }
    return result;
  }

  /**
   * Read a collection of strings originally written by
   * {@link CacheWriter#writeStringSet(Set, String)}.
   * 
   * @param endTag the tag used to denote the end of the collection of strings
   * @return the collection of strings (not <code>null</code>)
   */
  public HashSet<String> readStringSet(String endTag) throws IOException {
    HashSet<String> result = new HashSet<String>();
    while (true) {
      String string = readString();
      if (string == null) {
        throw new IOException("Expected " + endTag + " but found EOF");
      }
      if (string.equals(endTag)) {
        return result;
      }
      result.add(string);
    }
  }
}
