/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.debug.core.coverage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.io.FileUtilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class CoverageManager {
  private static final String MARKER_TEXT = "com.google.dart.tools.debug.core.coverageText";
  private static final String MARKER_OVERVIEW = "com.google.dart.tools.debug.core.coverageOverview";

  public static String createTempDir() {
    return Files.createTempDir().getAbsolutePath();
  }

  @VisibleForTesting
  public static TreeMap<Integer, Integer> parseHitMap(JSONObject coverageEntry) throws Exception {
    TreeMap<Integer, Integer> hitMap = Maps.newTreeMap();
    JSONArray hitsArray = coverageEntry.getJSONArray("hits");
    for (int j = 0; j < hitsArray.length() / 2; j++) {
      int line = hitsArray.getInt(2 * j + 0);
      int hits = hitsArray.getInt(2 * j + 1);
      Integer prevHits = hitMap.get(line);
      if (prevHits != null) {
        hits += prevHits.intValue();
      }
      hitMap.put(line, hits);
    }
    return hitMap;
  }

  public static void registerProcess(final String tempDir, final String scriptPath,
      final Process process) {
    Thread thread = new Thread("Coverage process handler") {
      @Override
      public void run() {
        while (true) {
          try {
            process.waitFor();
            break;
          } catch (InterruptedException e) {
          }
        }
        parseCoverageDirectory(tempDir, scriptPath);
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

  private static List<SourceRange> createMarkerRanges(final IFile file,
      final TreeMap<Integer, Integer> hitMap) throws Exception {
    List<SourceRange> markerRanges = Lists.newArrayList();
    List<SourceRange> lineRanges = parseFileLines(file);
    SourceRange markerRange = null;
    for (Entry<Integer, Integer> entry : hitMap.entrySet()) {
      if (entry.getValue() == 0) {
        int line = entry.getKey();
        SourceRange range = lineRanges.get(line - 1);
        if (markerRange == null) {
          markerRange = range;
        } else {
          markerRange = markerRange.getUnion(range);
        }
      } else if (markerRange != null) {
        markerRanges.add(markerRange);
        markerRange = null;
      }
    }
    if (markerRange != null) {
      markerRanges.add(markerRange);
    }
    return markerRanges;
  }

  private static void createMarkers(final Map<IFile, List<SourceRange>> filesMarkerRanges)
      throws Exception {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        deleteMarkers();
        for (Entry<IFile, List<SourceRange>> entry : filesMarkerRanges.entrySet()) {
          IFile file = entry.getKey();
          List<SourceRange> markerRanges = entry.getValue();
          for (SourceRange range : markerRanges) {
            createMarker(file, range, MARKER_TEXT);
            createMarker(file, range, MARKER_OVERVIEW);
          }
        }
      }

      private void createMarker(IFile file, SourceRange range, String type) throws CoreException {
        IMarker marker = file.createMarker(type);
        marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
        marker.setAttribute(IMarker.CHAR_START, range.getOffset());
        marker.setAttribute(IMarker.CHAR_END, range.getEnd());
      }

      private void deleteMarkers() throws CoreException {
        IWorkspaceRoot root = workspace.getRoot();
        root.deleteMarkers(MARKER_TEXT, true, IResource.DEPTH_INFINITE);
        root.deleteMarkers(MARKER_OVERVIEW, true, IResource.DEPTH_INFINITE);
      }
    };
    workspace.run(runnable, null, IWorkspace.AVOID_UPDATE, null);
  }

  private static IFile getFile(IContainer container, JSONObject coverageEntry) throws Exception {
    String sourcePath = coverageEntry.getString("source");
    if (sourcePath.startsWith("package:")) {
      File containerFile = container.getLocation().toFile();
      int packagePrefixLength = "package:".length();
      String packageFile = "packages/" + sourcePath.substring(packagePrefixLength);
      File file = new File(containerFile, packageFile);
      {
        File canonicalFile = file.getCanonicalFile();
        if (isInWorkspace(canonicalFile)) {
          file = canonicalFile;
        }
      }
      return getResourceFile(file);
    }
    if (sourcePath.startsWith("file://")) {
      String filePath = sourcePath.substring("file://".length());
      File file = new File(filePath);
      return getResourceFile(file);
    }
    return null;
  }

  private static IFile getResourceFile(File file) {
    try {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      URI uri = URI.create("file://" + file.getPath());
      IFile[] files = root.findFilesForLocationURI(uri);
      if (files.length == 1) {
        return files[0];
      }
    } catch (Throwable e) {
    }
    return null;
  }

  private static IContainer getScriptContextContainer(String scriptPath) {
    IResource scriptResource = ResourcesPlugin.getWorkspace().getRoot().findMember(scriptPath);
    if (scriptResource == null) {
      return null;
    }
    return scriptResource.getProject();
  }

  private static boolean isInWorkspace(File file) {
    IPath rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    String fileStringPath = file.getAbsolutePath();
    Path filePath = new Path(fileStringPath);
    return rootLocation.isPrefixOf(filePath);
  }

  private static void parseCoverageDirectory(String tempPath, String scriptPath) {
    IContainer contextContainer = getScriptContextContainer(scriptPath);
    if (contextContainer == null) {
      return;
    }
    // parse coverage output
    final Map<IFile, List<SourceRange>> filesMarkerRanges = parseCoverageFileInTempDirectory(
        contextContainer,
        tempPath);
    if (filesMarkerRanges == null) {
      return;
    }
    // add not loaded Dart files
    try {
      contextContainer.accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile) {
            IFile file = (IFile) resource;
            if (!DartCore.isDartLikeFile(file)) {
              return false;
            }
            if (filesMarkerRanges.containsKey(file)) {
              return false;
            }
            int length = (int) file.getLocation().toFile().length();
            filesMarkerRanges.put(file, Lists.newArrayList(new SourceRange(0, length)));
          }
          return true;
        }
      });
    } catch (Throwable e) {
      e.printStackTrace();
    }
    // create markers for all files in the context
    try {
      createMarkers(filesMarkerRanges);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private static Map<IFile, List<SourceRange>> parseCoverageFile(IContainer container, File jsonFile)
      throws Exception {
    Map<IFile, TreeMap<Integer, Integer>> filesHitMaps = Maps.newHashMap();
    Map<IFile, List<SourceRange>> filesMarkerRanges = Maps.newHashMap();
    String fileString = Files.toString(jsonFile, Charsets.UTF_8);
    JSONObject rootObject = new JSONObject(fileString);
    JSONArray coverageArray = rootObject.getJSONArray("coverage");
    for (int i = 0; i < coverageArray.length(); i++) {
      JSONObject coverageEntry = coverageArray.getJSONObject(i);
      // prepare IFile
      IFile file = getFile(container, coverageEntry);
      if (file == null) {
        continue;
      }
      // prepare hit map
      TreeMap<Integer, Integer> hitMap = parseHitMap(coverageEntry);
      if (hitMap == null) {
        continue;
      }
      // merge hit maps
      TreeMap<Integer, Integer> hitMapOld = filesHitMaps.get(file);
      if (hitMapOld != null) {
        Set<Entry<Integer, Integer>> entrySet = hitMapOld.entrySet();
        for (Entry<Integer, Integer> entry : entrySet) {
          int line = entry.getKey();
          int hitsOld = entry.getValue();
          Integer hits = hitMap.get(line);
          if (hits != null) {
            hitMap.put(line, hitsOld + hits.intValue());
          } else {
            hitMap.put(line, hitsOld);
          }
        }
      }
      filesHitMaps.put(file, hitMap);
    }
    // add marker ranges
    for (Entry<IFile, TreeMap<Integer, Integer>> entry : filesHitMaps.entrySet()) {
      IFile file = entry.getKey();
      TreeMap<Integer, Integer> hitMap = entry.getValue();
      filesMarkerRanges.put(file, createMarkerRanges(file, hitMap));
    }
    //
    return filesMarkerRanges;
  }

  private static Map<IFile, List<SourceRange>> parseCoverageFileInTempDirectory(
      IContainer container, String tempPath) {
    File tempFile = new File(tempPath);
    File[] outputFiles = tempFile.listFiles();
    for (File file : outputFiles) {
      String fileName = file.getName().toLowerCase();
      if (fileName.startsWith("dart-cov-") && fileName.endsWith(".json")) {
        try {
          return parseCoverageFile(container, file);
        } catch (Exception e) {
        } finally {
          FileUtilities.delete(tempFile);
        }
      }
    }
    return null;
  }

  private static List<SourceRange> parseFileLines(IFile file) throws Exception {
    List<SourceRange> lineRanges = Lists.newArrayList();
    String contents = Files.toString(file.getLocation().toFile(), Charsets.UTF_8);
    int start = 0;
    int offset = 0;
    while (offset < contents.length()) {
      char c = contents.charAt(offset++);
      if (c == '\r') {
        continue;
      }
      if (c == '\n') {
        int end = offset;
        lineRanges.add(new SourceRange(start, end - start));
        start = end;
      }
    }
    return lineRanges;
  }
}
