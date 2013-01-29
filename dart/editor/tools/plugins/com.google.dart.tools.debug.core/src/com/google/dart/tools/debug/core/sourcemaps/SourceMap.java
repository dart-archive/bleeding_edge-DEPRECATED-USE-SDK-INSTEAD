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

package com.google.dart.tools.debug.core.sourcemaps;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

// //@ sourceMappingURL=/path/to/file.js.map

/**
 * This maps from a generated file back to the original source files. It also supports the reverse
 * mapping; from locations in the source files to locations in the generated file.
 * 
 * @see http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/
 */
public class SourceMap {

  public static SourceMap createFrom(File file) throws IOException {
    String contents = Files.toString(file, Charsets.UTF_8);

    try {
      return createFrom(Path.fromOSString(file.getAbsolutePath()), contents);
    } catch (JSONException e) {
      throw new IOException(e);
    }
  }

  public static SourceMap createFrom(IFile file) throws IOException, CoreException {
    Reader reader = new InputStreamReader(file.getContents(), file.getCharset());
    String contents = CharStreams.toString(reader);

    try {
      return createFrom(file.getParent().getFullPath(), contents);
    } catch (JSONException e) {
      throw new IOException(e);
    }
  }

  private static SourceMap createFrom(IPath path, JSONObject jsonObject) throws JSONException {
    return new SourceMap(path, jsonObject);
  }

  private static SourceMap createFrom(IPath path, String contents) throws JSONException {
    if (contents.startsWith(")]}")) {
      contents = contents.substring(3);
    }

    return createFrom(path, new JSONObject(contents));
  }

  private IPath relativePath;

  private int formatVersion;
  private String targetFile;
  private String sourceRoot;

  private String[] sources;

  private String[] names;
  private SourceMapInfoEntry[] entries;

  public SourceMap() {

  }

  public SourceMap(IPath path, JSONObject obj) throws JSONException {
    /*{
        version : 3,
        file: "out.js",
        sourceRoot : "",
        sources: ["foo.js", "bar.js"],
        names: ["src", "maps", "are", "fun"],
        mappings: "AAgBC,SAAQ,CAAEA"
    }*/

    relativePath = path;

    formatVersion = obj.optInt("version");
    targetFile = obj.optString("file");
    sourceRoot = obj.optString("sourceRoot");

    sources = parseStringArray(obj.getJSONArray("sources"));
    names = parseStringArray(obj.getJSONArray("names"));

    String mapStr = obj.getString("mappings");

    List<SourceMapInfoEntry> result = SourceMapDecoder.decode(sources, names, mapStr);
    entries = result.toArray(new SourceMapInfoEntry[result.size()]);
  }

  public int getFormatVersion() {
    return formatVersion;
  }

  /**
   * Map from a location in the generated file back to the original source.
   * 
   * @param line the line in the generated source
   * @param column the column in the generated source; -1 means the column is not interesting
   * @return the corresponding location in the original source
   */
  public SourceMapInfo getMappingFor(int line, int column) {
    int index = findIndexForLine(line);

    if (index == -1) {
      return null;
    }

    SourceMapInfoEntry entry = entries[index];

    // If column == -1, return the first mapping for that line.
    if (column == -1) {
      return entry.getInfo();
    }

    // Search for a matching mapping.
    while (index < entries.length) {
      entry = entries[index];

      if (entry.column <= column) {
        if (entry.endColumn == -1) {
          return entry.getInfo();
        }

        if (column < entry.endColumn) {
          return entry.getInfo();
        }
      }

      index++;
    }

    // no mapping found
    return null;
  }

  public IPath getRelativePath() {
    return relativePath;
  }

  /**
   * Map from a location in a source file to a location in the generated source file.
   * 
   * @param file
   * @param line
   * @param column
   * @return
   */
  public SourceMapInfo getReverseMappingFor(String file, int line, int column) {
    // TODO(devoncarew): implement this

    return null;
  }

  public String[] getSourceNames() {
    return sources;
  }

  public String getSourceRoot() {
    return sourceRoot;
  }

  public String getTargetFile() {
    return targetFile;
  }

  private int findIndexForLine(int line) {
    // TODO(devoncarew): optimize this w/ a binary search

    for (int i = 0; i < entries.length; i++) {
      SourceMapInfoEntry entry = entries[i];

      if (entry.line == line) {
        return i;
      } else if (entry.line > line) {
        return -1;
      }
    }

    return -1;
  }

  private String[] parseStringArray(JSONArray arr) throws JSONException {
    String[] strs = new String[arr.length()];

    for (int i = 0; i < arr.length(); i++) {
      strs[i] = arr.getString(i);
    }

    return strs;
  }

}
