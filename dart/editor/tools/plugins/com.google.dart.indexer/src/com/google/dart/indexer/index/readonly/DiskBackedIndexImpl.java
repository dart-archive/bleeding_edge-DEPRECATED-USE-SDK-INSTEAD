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
package com.google.dart.indexer.index.readonly;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.entries.PathAndModStamp;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.source.IndexableSource;
import com.google.dart.indexer.storage.AbstractIntegratedStorage;
import com.google.dart.indexer.utils.Debugging;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class DiskBackedIndexImpl implements Index {
  private IPath[] errors;

  private final IndexConfigurationInstance configuration;

  private AbstractIntegratedStorage storage;

  public DiskBackedIndexImpl(IndexConfigurationInstance configuration,
      AbstractIntegratedStorage storage) {
    this.configuration = configuration;
    // errors = ErrorFile.read(folder, configuration);
    errors = new IPath[0];
    this.storage = storage;
  }

  @Override
  public String diskIndexAsString() throws IOException {
    Layer[] layers = configuration.getLayers();

    StringBuilder result = new StringBuilder();
    if (layers.length == 1) {
      toStringLocations(storage.readAllLayerLocations(layers[0]), result);
    } else {
      for (int i = 0; i < layers.length; i++) {
        result.append("=== LAYER ").append(i).append(" ===\n");
        toStringLocations(storage.readAllLayerLocations(layers[i]), result);
      }
    }
    toStringFiles(storage.readAllFileInfos(configuration), result, (layers.length > 1));
    return result.toString();
  }

  @Override
  public IPath[] getFilesWithErrors() {
    return errors.clone();
  }

  @Override
  public LocationInfo getLocationInfo(Location location, Layer layer)
      throws IndexRequiresFullRebuild {
    return storage.readLocationInfo(location, layer);
  }

  @Override
  public AbstractIntegratedStorage getUnderlyingStorage() {
    return storage;
  }

  @Override
  public boolean hasErrors() {
    return errors.length != 0;
  }

  @Override
  public PathAndModStamp[] loadAllFileHeaders() throws IOException {
    return storage.readFileNamesAndStamps(new HashSet<IFile>());
  }

  @Override
  public String toString() {
    try {
      return diskIndexAsString();
    } catch (IOException exception) {
      IndexerPlugin.getLogger().logError(exception);

      StringWriter writer = new StringWriter();
      exception.printStackTrace(new PrintWriter(writer));
      return writer.toString();
    }
  }

  @Deprecated
  private void toStringFiles(Map<IFile, FileInfo> map, StringBuilder out, boolean showLayers) {
    out.append("Files:\n");
    for (Iterator<IFile> iterator = Debugging.sortByStrings(map.keySet()).iterator(); iterator.hasNext();) {
      IFile file = iterator.next();
      FileInfo info = map.get(file);
      out.append(Debugging.INDENT).append(file).append("\n");
      info.toString(out, Debugging.INDENT + Debugging.INDENT, showLayers);
    }
  }

  private void toStringLocations(Map<Location, LocationInfo> map, StringBuilder out) {
    out.append("Locations:\n");
    for (Iterator<Location> iterator = Debugging.sortByStrings(map.keySet()).iterator(); iterator.hasNext();) {
      Location location = iterator.next();
      LocationInfo info = map.get(location);
      out.append(Debugging.INDENT).append(location).append("\n");
      info.toString(out, Debugging.INDENT + Debugging.INDENT);
    }
  }

  private void toStringSources(Map<IndexableSource, FileInfo> map, StringBuilder out,
      boolean showLayers) {
    out.append("Sources:\n");
    for (Iterator<IndexableSource> iterator = Debugging.sortByStrings(map.keySet()).iterator(); iterator.hasNext();) {
      IndexableSource source = iterator.next();
      FileInfo info = map.get(source);
      out.append(Debugging.INDENT).append(source).append("\n");
      info.toString(out, Debugging.INDENT + Debugging.INDENT, showLayers);
    }
  }
}
