/*
 * Copyright 2013 Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import java.util.ArrayList;

/**
 * A collection of {@link DeltaListener}s
 */
class DeltaListenerList implements DeltaListener {

  /**
   * Answer a single listener that forwards events to each of the specified listeners. If there is
   * only one specified listener, then that listener is returned.
   * 
   * @param listeners the listeners (not {@code null}, contains no {@code null}s)
   * @return a listener (not {@code null})
   */
  public static DeltaListener newFor(DeltaListener... listeners) {
    if (listeners.length == 1) {
      return listeners[0];
    }
    DeltaListenerList list = new DeltaListenerList();
    for (DeltaListener listener : listeners) {
      list.add(listener);
    }
    return list;
  }

  private ArrayList<DeltaListener> children = new ArrayList<DeltaListener>();

  @Override
  public void packageSourceAdded(SourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.packageSourceAdded(event);
    }
  }

  @Override
  public void packageSourceChanged(SourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.packageSourceChanged(event);
    }
  }

  @Override
  public void packageSourceContainerRemoved(SourceContainerDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.packageSourceContainerRemoved(event);
    }
  }

  @Override
  public void packageSourceRemoved(SourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.packageSourceRemoved(event);
    }
  }

  @Override
  public void pubspecAdded(ResourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.pubspecAdded(event);
    }
  }

  @Override
  public void pubspecChanged(ResourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.pubspecChanged(event);
    }
  }

  @Override
  public void pubspecRemoved(ResourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.pubspecRemoved(event);
    }
  }

  @Override
  public void sourceAdded(SourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.sourceAdded(event);
    }
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.sourceChanged(event);
    }
  }

  @Override
  public void sourceContainerRemoved(SourceContainerDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.sourceContainerRemoved(event);
    }
  }

  @Override
  public void sourceRemoved(SourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.sourceRemoved(event);
    }
  }

  void add(DeltaListener listener) {
    children.add(listener);
  }

  void remove(DeltaListener listener) {
    children.remove(listener);
  }
}
