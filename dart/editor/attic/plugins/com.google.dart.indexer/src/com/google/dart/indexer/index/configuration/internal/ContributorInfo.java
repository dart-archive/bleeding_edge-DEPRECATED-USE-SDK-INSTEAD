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

import com.google.dart.indexer.index.configuration.Contributor;
import com.google.dart.indexer.index.layers.LayerId;

public abstract class ContributorInfo implements Comparable<ContributorInfo> {
  private final String id;
  private final String version;
  private final LayerId layerId;

  public ContributorInfo(String id, String version, LayerId layerId) {
    if (id == null) {
      throw new NullPointerException("id is null");
    }
    if (version == null) {
      throw new NullPointerException("version is null");
    }
    if (layerId == null) {
      throw new NullPointerException("layerId is null");
    }
    this.id = id;
    this.version = version;
    this.layerId = layerId;
  }

  @Override
  public int compareTo(ContributorInfo o) {
    ContributorInfo peer = o;
    return getId().compareTo(peer.getId());
  }

  public abstract Contributor create();

  public void describe(StringBuilder builder) {
    builder.append(id).append(' ').append(version);
  }

  public String getId() {
    return id;
  }

  public LayerId getLayerId() {
    return layerId;
  }

  public String getVersion() {
    return version;
  }
}
