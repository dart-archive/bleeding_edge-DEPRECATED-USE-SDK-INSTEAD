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
package com.google.dart.indexer.index.layers;

import com.google.dart.indexer.index.configuration.internal.LayerInfo;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.updating.LayerUpdater;
import com.google.dart.indexer.storage.FileTransaction;

import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class Layer {
  private LayerInfo layerInfo;

  private int ordinal;

  public abstract LocationInfo createEmptyLocationInfo();

  public abstract LayerUpdater createLayerUpdater(FileTransaction fileTransaction);

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Layer other = (Layer) obj;
    if (ordinal != other.ordinal) {
      return false;
    }
    return true;
  }

  public LayerId getId() {
    return layerInfo.getId();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ordinal;
    return result;
  }

  public void initialize(LayerInfo layerInfo) {
    this.layerInfo = layerInfo;
    ordinal = layerInfo.getOrdinal();
  }

  public abstract boolean isBidirectional();

  public abstract LocationInfo loadLocationInfo(RandomAccessFile ra) throws IOException;

  public int ordinal() {
    return layerInfo.getOrdinal();
  }

  public abstract void save(LocationInfo info, RandomAccessFile ra) throws IOException;

  @Override
  public String toString() {
    String id = layerInfo.getId().stringValue();
    int pos = id.lastIndexOf('.');
    if (pos > 0) {
      return id.substring(pos + 1);
    } else {
      return id;
    }
  }
}
