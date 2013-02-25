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
package com.google.dart.indexer.storage;

import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.entries.DependentLocation;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.source.IndexableSource;

import org.eclipse.core.resources.IFile;

public abstract class FileTransaction {
  /**
   * @deprecated use {@link #addDependency(IndexableSource, DependentLocation)}
   */
  @Deprecated
  public abstract void addDependency(IFile masterFile, DependentLocation depency)
      throws IndexRequestFailed;

  public abstract void addDependency(IndexableSource masterFile, DependentLocation depency)
      throws IndexRequestFailed;

  public abstract void addReference(Layer layer, Location sourceLocation,
      Location destinationLocation) throws IndexRequestFailed;

  public abstract void addSourceLocation(Location location) throws IndexRequestFailed;

  public abstract void commit() throws IndexRequestFailed;

  public abstract FileInfo getOriginalFileInfo() throws IndexRequestFailed;
}
