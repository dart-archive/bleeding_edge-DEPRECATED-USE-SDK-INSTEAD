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

import java.util.ArrayList;
import java.util.List;

class SourceMapDecoder {

  public static List<SourceMapInfoEntry> decode(String[] sources, String[] names, String mapStr) {
    return new SourceMapDecoder(sources, names).decodeLines(mapStr);
  }

  private String[] sources;
  private String[] names;

  private List<SourceMapInfoEntry> entries;

  int originalFileIndex = 0;
  int originalLine = 0;
  int originalColumn = 0;
  int nameIndex = 0;

  private SourceMapDecoder(String[] sources, String[] names) {
    this.sources = sources;
    this.names = names;
  }

  List<SourceMapInfoEntry> decodeLines(String mapStr) {
    // In the given string, semi-colons demarcate lines and commas demarcate groups.
    // A;A;;;;;;;A;A;;A;A;A,mB,W,C,C,I,C,C;A,cAyVEA;AAAiB,QAAK,MAAFC

    entries = new ArrayList<SourceMapInfoEntry>();

    int lineNumber = 0;

    for (String line : mapStr.split(";")) {
      if (line.length() > 0) {
        decodeLine(line.split(","), lineNumber);
      }

      lineNumber++;
    }

    return entries;
  }

  private void decodeLine(String[] mappings, int line) {
    int generatedColumn = 0;

    SourceMapInfoEntry previousEntry = null;

    for (String mapping : mappings) {
      int[] indexes = VlqDecoder.decode(mapping);

      if (indexes.length == 1 || indexes.length == 4 || indexes.length == 5) {
        generatedColumn += indexes[0];

        if (previousEntry != null) {
          previousEntry.setEndColumn(generatedColumn);
        }

        if (indexes.length < 4) {
          continue;
        }

        originalFileIndex += indexes[1];
        originalLine += indexes[2];
        originalColumn += indexes[3];

        String originalFile = getString(sources, originalFileIndex);

        SourceMapInfo info = new SourceMapInfo(originalFile, originalLine, originalColumn);

        if (indexes.length > 4) {
          nameIndex += indexes[4];
          info.setName(getString(names, nameIndex));
        }

        previousEntry = new SourceMapInfoEntry(line, generatedColumn, info);

        entries.add(previousEntry);
      }
    }
  }

  private String getString(String[] strs, int index) {
    if (index >= 0 && index < strs.length) {
      return strs[index];
    } else {
      return null;
    }
  }

}
