/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.pub;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.ListenerList;

/**
 * The unique instance of the class <code>PubManager</code> is used to manage the listeners for pub
 * updates.
 */
public class PubManager {

  private static final PubManager INSTANCE = new PubManager();

  public synchronized static PubManager getInstance() {
    return INSTANCE;
  }

  private final ListenerList listeners = new ListenerList();

  public void addListener(IPubUpdateListener listener) {
    listeners.add(listener);
  }

  public void notifyListeners(IContainer container) {
    for (Object listener : listeners.getListeners()) {
      ((IPubUpdateListener) listener).packagesUpdated(container);
    }
  }

  public void removeListener(IPubUpdateListener listener) {
    listeners.remove(listener);
  }

}
