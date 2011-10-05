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
package com.google.dart.tools.core.utilities.compiler;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.LibraryConfigurationFileImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.debugging.sourcemap.SourceMapConsumerFactory;
import com.google.debugging.sourcemap.SourceMapParseException;
import com.google.debugging.sourcemap.SourceMapSupplier;
import com.google.debugging.sourcemap.SourceMapping;
import com.google.debugging.sourcemap.SourceMappingReversable;
import com.google.debugging.sourcemap.proto.Mapping.OriginalMapping;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that supports mapping from Dart source locations to compiled Javascript locations, and
 * vice versa. The {@link #mapDartToJavascript(IFile, int)} direction is Dart to Javascript, and the
 * {@link #mapJavaScriptToDart(String, int, int)} location is Javascript to Dart.
 */
public class DartSourceMapping {
  private DartProject mainProject;

  private Map<IPath, SourceMapping> sourceMappingCache = new HashMap<IPath, SourceMapping>();

  /**
   * Create a new DartSourceMapper.
   * 
   * @param project
   */
  public DartSourceMapping(DartProject mainProject) {
    this.mainProject = mainProject;
  }

  /**
   * @return the Dart project that is the root of this source mapping
   */
  public DartProject getMainDartProject() {
    return mainProject;
  }

  /**
   * Given a Javascript filePath, source line, and column number, return the Dart identifier (if
   * any) at that location. This could be a method, field, or variable.
   * 
   * @param filePath the path to the Javascript file
   * @param line the source line
   * @param column the source column
   * @return the Dart identifier (if any) for that location
   */
  public String getSymbolForLocation(String filePath, int line, int column) {
    // The source mappings are 1 based, the JS VM is 0 based.
    line += 1;
    column += 1;

    SourceMapping mapping = getCachedSourceMapping(filePath);

    if (mapping != null) {
      OriginalMapping originalMapping = mapping.getMappingForLine(line, column);

      if (originalMapping != null) {
        return originalMapping.getIdentifier();
      }
    }

    return null;
  }

  /**
   * Map from the source language to the target language (Dart to Javascript).
   */
  public SourceLocation mapDartToJavascript(IFile file, int line) {
    // given the file, find the dart element
    DartElement dartElement = DartCore.create(file);

    if (dartElement == null) {
      return SourceLocation.UNKNOWN_LOCATION;
    }

    // given that, find the dart application / library
    DartLibraryImpl dartLibrary = (DartLibraryImpl) dartElement.getAncestor(DartLibrary.class);

    if (dartLibrary == null) {
      return SourceLocation.UNKNOWN_LOCATION;
    }

    try {
      // [out/DartAppFileName.app.js.map]

      IPath outputLocation = getMainDartProject().getOutputLocation();

      List<LibraryConfigurationFileImpl> libraryConfigurationFiles = dartLibrary.getChildrenOfType(LibraryConfigurationFileImpl.class);

      if (libraryConfigurationFiles.size() > 0) {
        String libraryName = libraryConfigurationFiles.get(0).getFile().getName();

        IPath sourceMapPath = outputLocation.append(libraryName + ".js.map");

        SourceMapping mapping = getCachedSourceMappingPath(sourceMapPath);

        if (mapping != null && mapping instanceof SourceMappingReversable) {
          SourceMappingReversable revMapping = (SourceMappingReversable) mapping;

          String sourcePath = findMatchingSourcePath(file, revMapping);

          if (sourcePath != null) {
            Collection<OriginalMapping> mappings = revMapping.getReverseMapping(sourcePath, line, 1);

            // TODO(devoncarew): We need to handle the case where there are more then one mappings
            // returned; this will probably involve setting one breakpoint per mapping. More then
            // one mapping ==> something like a function that's been inlined into multiple places.
            if (mappings.size() > 0) {
              OriginalMapping map = mappings.iterator().next();

              String fileName = map.getOriginalFile();
              int lineNumber = map.getLineNumber();

              return new SourceLocation(new Path(fileName), lineNumber);
            }
          }
        }
      }
    } catch (DartModelException exception) {
      DartCore.logError(exception);
    }

    return SourceLocation.UNKNOWN_LOCATION;
  }

  /**
   * Map from the target language (Javascript) to the source language (Dart).
   */
  public SourceLocation mapJavaScriptToDart(String javaScriptFileName, int line, int column) {
    // The source mappings are 1 based, the JS VM is 0 based.
    line += 1;
    column += 1;

    SourceMapping mapping = getCachedSourceMapping(javaScriptFileName);

    if (mapping != null) {
      OriginalMapping originalMapping = mapping.getMappingForLine(line, column);

      if (originalMapping != null) {
        String fileName = originalMapping.getOriginalFile();
        int lineNumber = originalMapping.getLineNumber();

        return new SourceLocation(new Path(fileName), lineNumber);
      }
    }

    return SourceLocation.UNKNOWN_LOCATION;
  }

  private String findMatchingSourcePath(IFile file, SourceMappingReversable revMapping) {
    // TODO(devoncarew): make this more bullet proof
    String fileName = file.getName();

    for (String origSource : revMapping.getOriginalSources()) {
      if (origSource.endsWith(fileName)) {
        return origSource;
      }
    }

    return null;
  }

  /**
   * Given a simple name for a javascript file, return the SourceMapping object that can map that
   * back to it's source Dart file. This methods differs from
   * {@link #getCachedSourceMappingPath(IPath)} in that it knows where on disk the named javascript
   * file might live.
   * 
   * @param jsFileName
   * @return
   */
  private SourceMapping getCachedSourceMapping(String jsFileName) {
    try {
      IPath outputLocation = getMainDartProject().getOutputLocation();

      IPath sourceMapPath = outputLocation.append(jsFileName + ".map");

      return getCachedSourceMappingPath(sourceMapPath);
    } catch (DartModelException exception) {
      DartCore.logError(exception);

      return null;
    }
  }

  /**
   * Given a path for a javascript file, return the SourceMapping object that can map that back to
   * it's source Dart file.
   * 
   * @param sourceMapPath
   * @return
   */
  private SourceMapping getCachedSourceMappingPath(IPath sourceMapPath) {
    if (sourceMappingCache.containsKey(sourceMapPath)) {
      return sourceMappingCache.get(sourceMapPath);
    } else {
      try {
        String fileContents = getContentsOf(sourceMapPath);

        //long start = System.currentTimeMillis();

        SourceMapping mapping = SourceMapConsumerFactory.parse(fileContents,
            new SourceMapSupplier() {
              @Override
              public String getSourceMap(String url) throws IOException {
                return getSourceMapContentsAt(url);
              }
            });

        //System.out.println(sourceMapPath.lastSegment() + " parse time: "
        //    + (System.currentTimeMillis() - start) + "ms");

        sourceMappingCache.put(sourceMapPath, mapping);

        return mapping;
      } catch (SourceMapParseException exception) {
        DartCore.logError(exception);

        return null;
      } catch (FileNotFoundException exception) {
        // This is an expected error - no need to log the exception.

        return null;
      } catch (IOException exception) {
        DartCore.logError(exception);

        return null;
      }
    }
  }

  private String getContentsOf(IPath filePath) throws IOException {
    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(filePath);

    if (resource == null || resource.getRawLocation() == null) {
      throw new FileNotFoundException();
    }

    return Files.toString(resource.getRawLocation().toFile(), Charset.defaultCharset());
  }

  private String getSourceMapContentsAt(String urlString) throws IOException {
    URL url = new URL(urlString);

    URLConnection connection = url.openConnection();

    return CharStreams.toString(new InputStreamReader(connection.getInputStream()));
  }

}
