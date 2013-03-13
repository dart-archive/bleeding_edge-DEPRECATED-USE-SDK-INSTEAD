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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
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
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

// //@ sourceMappingURL=/path/to/file.js.map

/**
 * This maps from a generated file back to the original source files. It also supports the reverse
 * mapping; from locations in the source files to locations in the generated file.
 * 
 * @see http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/
 */
public class SourceMap {

  public static final String SOURCE_MAP_EXT = ".map";

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

    try {
      String contents = CharStreams.toString(reader);

      return createFrom(file.getFullPath(), contents);
    } catch (JSONException e) {
      throw new IOException(e);
    } finally {
      reader.close();
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

  private IPath path;

  /**
   * The format version; must be a positive integer. The current version of the spec is 3.
   */
  private int version;

  /**
   * The name of the generated code that this source map is associated with.
   */
  private String file;

  /**
   * An optional source root, useful for relocating source files on a server or removing repeated
   * values in the "sources" entry. This value is prepended to the individual entries in the
   * "source" field.
   */
  private String sourceRoot;

  /**
   * A list of original sources used by the "mappings" entry.
   */
  private String[] sources;

  /**
   * A list of symbol names used by the "mappings" entry.
   */
  private String[] names;

  /**
   * An optional list of source content, useful when the "source" can’t be hosted.
   */
  @SuppressWarnings("unused")
  private String sourcesContent[];

  /**
   * The full list of source map entries.
   */
  private SourceMapInfoEntry[] entries;

  public SourceMap() {

  }

  public SourceMap(IPath path, JSONObject obj) throws JSONException {
    // {
    //     version : 3,
    //     file: "out.js",
    //     sourceRoot : "",
    //     sources: ["foo.js", "bar.js"],
    //     names: ["src", "maps", "are", "fun"],
    //     mappings: "AAgBC,SAAQ,CAAEA"
    // }

    this.path = path;

    version = obj.optInt("version");
    file = obj.optString("file");
    sourceRoot = obj.optString("sourceRoot");

    sources = parseStringArray(obj.getJSONArray("sources"));
    sourcesContent = parseStringArray(obj.optJSONArray("sourcesContent"));
    names = parseStringArray(obj.getJSONArray("names"));

    // Prepend sourceRoot to the sources entries.
    if (sourceRoot != null && sourceRoot.length() > 0) {
      for (int i = 0; i < sources.length; i++) {
        sources[i] = sourceRoot + sources[i];
      }
    }

    String mapStr = obj.getString("mappings");

    List<SourceMapInfoEntry> result = SourceMapDecoder.decode(sources, names, mapStr);
    entries = result.toArray(new SourceMapInfoEntry[result.size()]);
  }

  public String getFile() {
    return file;
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

  public IFile getMapSource() {
    String name = path.lastSegment();

    if (name.endsWith(SOURCE_MAP_EXT)) {
      name = name.substring(0, name.length() - SOURCE_MAP_EXT.length());

      IPath newPath = path.removeLastSegments(1).append(name);

      IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(newPath);

      if (resource instanceof IFile) {
        return (IFile) resource;
      }
    }

    return null;
  }

  public IPath getPath() {
    return path;
  }

  /**
   * Map from a location in a source file to a location in the generated source file.
   * 
   * @param file
   * @param line
   * @param column
   * @return
   */
  public SourceMapInfo getReverseMappingFor(String file, int line) {
    // TODO(devoncarew): calculate this information once for O(1) lookup

    for (SourceMapInfoEntry entry : entries) {
      SourceMapInfo info = entry.getInfo();

      if (info == null) {
        continue;
      }

      if (line == info.getLine()) {
        if (file.equals(info.getFile())) {
          // TODO(devoncarew): there will be several entries on this line
          // We need to choose one that has a non-zero range, or is a catch-all entry

          return new SourceMapInfo(path.toString(), entry.line, entry.column);
        }
      }
    }

    return null;
  }

  public String[] getSourceNames() {
    return sources;
  }

  /**
   * The format version; must be a positive integer. The current version of the specification is 3.
   */
  public int getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return "[" + getPath().lastSegment() + ", "
        + NumberFormat.getNumberInstance().format(entries.length) + " lines]";
  }

  private int findIndexForLine(int line) {
    // TODO(devoncarew): test this binary search

    int location = Arrays.binarySearch(
        entries,
        SourceMapInfoEntry.forLine(line),
        SourceMapInfoEntry.lineComparator());

    if (location < 0) {
      return -1;
    }

    while (location > 0 && entries[location - 1].line == line) {
      location--;
    }

    return location;

//    for (int i = 0; i < entries.length; i++) {
//      SourceMapInfoEntry entry = entries[i];
//
//      if (entry.line == line) {
//        return i;
//      } else if (entry.line > line) {
//        return -1;
//      }
//    }
//
//    return -1;
  }

  private String[] parseStringArray(JSONArray arr) throws JSONException {
    if (arr == null) {
      return null;
    } else {
      String[] strs = new String[arr.length()];

      for (int i = 0; i < arr.length(); i++) {
        strs[i] = arr.getString(i);
      }

      return strs;
    }
  }

}
