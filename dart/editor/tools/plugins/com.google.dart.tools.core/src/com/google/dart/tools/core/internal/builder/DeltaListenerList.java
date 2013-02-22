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
public class DeltaListenerList implements DeltaListener {

  /**
   * Answer a listener that broadcasts events to both the existing listener and the new listener
   * 
   * @param existingListener the existing listener or {@link DeltaListenerList} or {@code null} if
   *          there is no existing listener
   * @param newListener the listener to be added (not {@code null})
   * @return a new composite listener (not {@code null})
   */
  public static DeltaListener add(DeltaListener existingListener, DeltaListener newListener) {
    if (existingListener == null) {
      return newListener;
    }
    if (existingListener instanceof DeltaListenerList) {
      DeltaListenerList list = (DeltaListenerList) existingListener;
      if (list.children.size() == 0) {
        return newListener;
      }
      list.children.add(newListener);
      return list;
    }
    DeltaListenerList list = new DeltaListenerList();
    list.children.add(existingListener);
    list.children.add(newListener);
    return list;
  }

  /**
   * The listeners to which events are broadcast
   */
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

  @Override
  public void visitContext(ResourceDeltaEvent event) {
    for (DeltaListener listener : children) {
      listener.visitContext(event);
    }
  }
}
