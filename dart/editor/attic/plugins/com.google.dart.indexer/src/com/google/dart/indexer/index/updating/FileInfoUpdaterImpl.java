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
package com.google.dart.indexer.index.updating;

import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.LayerId;
import com.google.dart.indexer.storage.FileTransaction;

import org.eclipse.core.resources.IFile;

import java.util.HashMap;
import java.util.Map;

public class FileInfoUpdaterImpl implements FileInfoUpdater {
  private Map<LayerId, LayerUpdater> layerIdsToUpdaters = new HashMap<LayerId, LayerUpdater>();
  private FileTransaction fileTransaction;

  public FileInfoUpdaterImpl(FileTransaction fileTransaction) {
    if (fileTransaction == null) {
      throw new NullPointerException("fileTransaction is null");
    }
    this.fileTransaction = fileTransaction;
  }

  @Deprecated
  public FileInfoUpdaterImpl(FileTransaction fileTransaction, IFile file) {
    if (fileTransaction == null) {
      throw new NullPointerException("fileTransaction is null");
    }
    if (file == null) {
      throw new NullPointerException("file is null");
    }
    this.fileTransaction = fileTransaction;
  }

  @Override
  public LayerUpdater getLayerUpdater(Layer layer) {
    LayerId layerId = layer.getId();
    LayerUpdater layerUpdater = layerIdsToUpdaters.get(layerId);
    if (layerUpdater == null) {
      layerUpdater = layer.createLayerUpdater(fileTransaction);
      layerIdsToUpdaters.put(layerId, layerUpdater);
    }
    return layerUpdater;
  }
}
