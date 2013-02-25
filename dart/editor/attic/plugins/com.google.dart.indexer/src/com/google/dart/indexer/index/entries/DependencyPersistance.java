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
package com.google.dart.indexer.index.entries;

import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationPersitence;
import com.google.dart.indexer.utils.PathUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class DependencyPersistance {
  private static final char KIND_FILE = 'F';

  private static final char KIND_LOCATION = 'L';

  public static DependentEntity load(RandomAccessFile file, IndexConfigurationInstance configuration)
      throws IOException {
    char kind = file.readChar();
    if (kind == KIND_LOCATION) {
      Location dependentLocation = LocationPersitence.getInstance().load(file);
      Layer dependentLayer = configuration.getLayer(file.readInt());
      return new DependentLocation(dependentLocation, dependentLayer);
    } else if (kind == KIND_FILE) {
      IPath path = PathUtils.fromPortableString(file.readUTF());
      IFile dependentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
      return new DependentFileInfo(dependentFile);
    } else {
      throw new AssertionError("Unknown dependent entity code: " + kind);
    }
  }

  public static List<DependentEntity> loadDependencies(RandomAccessFile file,
      IndexConfigurationInstance configuration) throws IOException {
    int count = file.readInt();
    ArrayList<DependentEntity> result = new ArrayList<DependentEntity>();
    for (int i = 0; i < count; i++) {
      result.add(load(file, configuration));
    }
    return result;
  }

  public static void save(DependentEntity entity, RandomAccessFile file) throws IOException {
    if (entity instanceof DependentLocation) {
      DependentLocation dependency = (DependentLocation) entity;
      file.writeChar(KIND_LOCATION);
      LocationPersitence.getInstance().save(dependency.getDependentLocation(), file);
      file.writeInt(dependency.getDependentLayer().ordinal());
    } else if (entity instanceof DependentFileInfo) {
      DependentFileInfo dependency = (DependentFileInfo) entity;
      file.writeChar(KIND_FILE);
      file.writeUTF(PathUtils.toPortableString(dependency.getFile().getFullPath()));
    }
  }

  public static void saveDependencies(RandomAccessFile file,
      final Collection<DependentEntity> dependencies) throws IOException {
    file.writeInt(dependencies.size());
    for (Iterator<DependentEntity> iterator = dependencies.iterator(); iterator.hasNext();) {
      save(iterator.next(), file);
    }
  }
}
