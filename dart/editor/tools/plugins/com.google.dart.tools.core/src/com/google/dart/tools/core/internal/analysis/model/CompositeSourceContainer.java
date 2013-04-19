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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import java.util.Arrays;
import java.util.Collection;

/**
 * Instances of the class {@link CompositeSourceContainer} represent a source container that
 * contains all source containers within a given directory.
 */
public class CompositeSourceContainer implements SourceContainer {

  private final SourceContainer[] containers;

  public CompositeSourceContainer(Collection<SourceContainer> containers) {
    this(containers.toArray(new SourceContainer[containers.size()]));
  }

  public CompositeSourceContainer(SourceContainer[] containers) {
    this.containers = containers;
  }

  @Override
  public boolean contains(Source source) {
    for (SourceContainer container : containers) {
      if (container.contains(source)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof CompositeSourceContainer)) {
      return false;
    }
    CompositeSourceContainer other = (CompositeSourceContainer) obj;
    if (!Arrays.equals(containers, other.containers)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return 31 + Arrays.hashCode(containers);
  }
}
