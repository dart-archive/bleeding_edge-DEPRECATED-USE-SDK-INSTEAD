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

package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.source.WorkspaceSourceContainer;
import com.google.dart.tools.debug.core.sourcemaps.SourceMap;
import com.google.dart.tools.debug.core.sourcemaps.SourceMapInfo;
import com.google.dart.tools.debug.core.util.ResourceChangeManager;
import com.google.dart.tools.debug.core.util.ResourceChangeParticipant;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO(devoncarew): use the symbol name information in the maps?
// it's possible this will help us de-mangle the method names for frames

/**
 * A class to help manage parsing and querying source maps. It automatically parses source maps and
 * keeps that info up to date. It also helps retrieve information about map sources and map targets.
 * A source map contains information mapping locations in source files to locations in target files.
 * For instance foo.dart.js ==> [foo.dart, bar.dart, baz.dart]. The reverse direction is from
 * targets ==> sources.
 * 
 * @see SourceMap
 */
public class SourceMapManager implements ResourceChangeParticipant {
  public static class SourceLocation {
    public IFile file;
    public int line;
    public int column;

    public SourceLocation() {

    }

    public SourceLocation(IFile file, int line) {
      this.file = file;
      this.line = line;
    }

    public SourceLocation(IFile file, int line, int column) {
      this.file = file;
      this.line = line;
      this.column = column;
    }

    public int getColumn() {
      return column;
    }

    public IFile getFile() {
      return file;
    }

    public int getLine() {
      return line;
    }

    public void setLine(int line) {
      this.line = line;
    }

    @Override
    public String toString() {
      return "[" + file + "," + line + "," + column + "]";
    }
  }

  private static final int MAX_SOURCE_MAPS = 10;

  private Map<IFile, SourceMap> sourceMaps = new LinkedHashMap<IFile, SourceMap>();

  public SourceMapManager(IProject project) {
    // Collect all maps in the current project.
    try {
      project.accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile && isMapFileName((IFile) resource)) {
            handleFileAdded((IFile) resource);
          }

          return true;
        }
      });
    } catch (CoreException e) {

    }

    // Listen for changes to the maps.
    ResourceChangeManager.getManager().addChangeParticipant(this);
  }

  public void dispose() {
    sourceMaps.clear();

    ResourceChangeManager.removeChangeParticipant(this);
  }

  /**
   * Given a source (foo.dart.js) file and a location, return the corresponding target location (in
   * foo.dart).
   * 
   * @param file
   * @param line
   * @param column
   * @return
   */
  public SourceLocation getMappingFor(IFile file, int line, int column) {
    IFile mapFile = file.getParent().getFile(new Path(file.getName() + SourceMap.SOURCE_MAP_EXT));

    SourceMap map = sourceMaps.get(mapFile);

    if (map != null) {
      // Re-order the map.
      sourceMaps.remove(mapFile);
      sourceMaps.put(mapFile, map);

      // Return mapping info.
      SourceMapInfo mapping = map.getMappingFor(line, column);

      if (mapping != null) {
        IFile resolvedFile = resolveFile(mapFile, mapping.getFile());

        if (resolvedFile != null) {
          return new SourceLocation(resolvedFile, mapping.getLine());
        }
      }
    }

    return null;
  }

  /**
   * Given a target location (in foo.dart), return the corresponding source location (in
   * foo.dart.js).
   * 
   * @param file
   * @param line
   * @return
   */
  public List<SourceLocation> getReverseMappingsFor(IFile targetFile, int line) {
    List<SourceLocation> mappings = new ArrayList<SourceMapManager.SourceLocation>();

    Set<IFile> foundMappings = new HashSet<IFile>();

    synchronized (sourceMaps) {
      for (IFile sourceFile : sourceMaps.keySet()) {
        SourceMap map = sourceMaps.get(sourceFile);

        for (String path : map.getSourceNames()) {
          // TODO(devoncarew): the files in the maps should all be pre-resolved
          IFile file = resolveFile(sourceFile, path);

          if (file != null && file.equals(targetFile)) {
            List<SourceMapInfo> reverseMappings = map.getReverseMappingsFor(path, line);

            foundMappings.add(file);

            for (SourceMapInfo reverseMapping : reverseMappings) {
              if (reverseMapping != null) {
                IFile mapSource = map.getMapSource();

                if (mapSource != null) {
                  mappings.add(new SourceLocation(
                      mapSource,
                      reverseMapping.getLine(),
                      reverseMapping.getColumn()));
                }
              }
            }
          }
        }
      }
    }

    for (IFile mapFile : foundMappings) {
      // Re-order the map.
      SourceMap map = sourceMaps.remove(mapFile);
      sourceMaps.put(mapFile, map);
    }

    return mappings;
  }

  @Override
  public void handleFileAdded(IFile file) {
    handleFileChanged(file);
  }

  @Override
  public final void handleFileChanged(IFile file) {
    if (isMapFileName(file)) {
      if (shouldFilterMapFile(file)) {
        synchronized (sourceMaps) {
          sourceMaps.remove(file);
        }
      } else {
        try {
          // We speculatively parse the .map file to determine if it is indeed a source map.
          SourceMap sourceMap = SourceMap.createFrom(file);

          synchronized (sourceMaps) {
            // It's a source map file; put it in the source map map.
            sourceMaps.remove(file);
            sourceMaps.put(file, sourceMap);

            // Make sure we bound how many source maps we remember. The source map hashmap is sorted
            // by least recently used.
            if (sourceMaps.size() > MAX_SOURCE_MAPS) {
              IFile firstFile = sourceMaps.keySet().iterator().next();
              sourceMaps.remove(firstFile);
            }
          }
        } catch (Exception ce) {
          synchronized (sourceMaps) {
            sourceMaps.remove(file);
          }
        }
      }
    }
  }

  @Override
  public void handleFileRemoved(IFile file) {
    if (isMapFileName(file)) {
      synchronized (sourceMaps) {
        sourceMaps.remove(file);
      }
    }
  }

  /**
   * Returns true if the the source map manager contains mapping information for the given file back
   * to original resources.
   * 
   * @param resource
   * @return true if the the source map manager contains mapping information for the given file
   */
  public boolean isMapSource(IFile file) {
    if (file != null) {
      IFile mapFile = file.getParent().getFile(new Path(file.getName() + SourceMap.SOURCE_MAP_EXT));

      if (mapFile.exists() && sourceMaps.containsKey(mapFile)) {
        return true;
      }
    }

    return false;
  }

  public boolean isMapTarget(IFile targetFile) {
    if (targetFile != null) {
      synchronized (sourceMaps) {
        for (IFile sourceFile : sourceMaps.keySet()) {
          SourceMap map = sourceMaps.get(sourceFile);

          for (String path : map.getSourceNames()) {
            // TODO(devoncarew): the files in the maps should all be pre-resolved
            IFile file = resolveFile(sourceFile, path);

            if (file != null && file.equals(targetFile)) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  /**
   * Filter out any source map file that lives in a packages directory.
   */
  boolean shouldFilterMapFile(IFile file) {
    IPath path = file.getLocation();

    if (path != null) {
      String str = path.toPortableString();

      return str.contains("/packages/");
    }

    return false;
  }

  private boolean isMapFileName(IFile file) {
    return file.getName().endsWith(SourceMap.SOURCE_MAP_EXT);
  }

  private IFile resolveFile(IFile relativeFile, String path) {
    if (path.startsWith("file:")) {
      try {
        // These incoming uris are not properly uri encoded. If we uri encoded them, we would then
        // not be able to handle properly uri encoded uris. Instead we handle certain illegal chars.
        //String encodedPath = URIUtilities.uriEncode(path);
        String encodedPath = path.replaceAll(" ", "%20");

        URI uri = new URI(encodedPath);

        IResource resource = WorkspaceSourceContainer.locatePathAsResource(uri.getPath());

        if (resource instanceof IFile) {
          return (IFile) resource;
        }
      } catch (URISyntaxException ex) {
        DartDebugCorePlugin.logError(ex);
      }

      return null;
    } else {
      IFile file = relativeFile.getParent().getFile(new Path(path));

      if (file.exists()) {
        return file;
      } else {
        return null;
      }
    }
  }

}
