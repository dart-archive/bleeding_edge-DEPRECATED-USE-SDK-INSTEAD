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
package com.google.dart.indexer.index.configuration.internal;

import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.LayerId;

public abstract class LayerInfo implements Comparable<LayerInfo> {
  private final LayerId id;

  private int ordinal = -1;

  public LayerInfo(LayerId id) {
    if (id == null) {
      throw new NullPointerException("id is null");
    }
    this.id = id;
  }

  @Override
  public int compareTo(LayerInfo o) {
    LayerInfo other = o;
    return getOrdinal() - other.getOrdinal();
  }

  public abstract Layer create();

  public LayerId getId() {
    return id;
  }

  public int getOrdinal() {
    if (ordinal < 0) {
      throw new IllegalStateException("Ordinal has not yet been set for layer " + id);
    }
    return ordinal;
  }

  public void initializeOrdinal(int ordinal) {
    if (this.ordinal >= 0) {
      throw new IllegalStateException("Ordinal has already been set for layer " + id);
    }
    this.ordinal = ordinal;
  }
}
