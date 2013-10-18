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
package com.google.dart.tools.wst.ui;

import org.eclipse.jface.text.IDocument;

import java.util.HashMap;
import java.util.Map;

public class DartReconcilerManager {

  private Map<IDocument, EmbeddedDartReconcilerHook> reconcilers = new HashMap<IDocument, EmbeddedDartReconcilerHook>();

  private static final DartReconcilerManager INSTANCE = new DartReconcilerManager();

  /**
   * Retrieve the reconciler cache manager.
   * 
   * @return The singleton DartReconcilerManager
   */
  public static DartReconcilerManager getInstance() {
    return INSTANCE;
  }

  private DartReconcilerManager() {
    // This is a singleton.
  }

  /**
   * Retrieve the reconciler used for the given <code>document</code>.
   * 
   * @param document The IDocument being edited
   * @return The EmbeddedDartReconcilerHook used for reconciling
   */
  public EmbeddedDartReconcilerHook reconcilerFor(IDocument document) {
    synchronized (reconcilers) {
      EmbeddedDartReconcilerHook rec = reconcilers.get(document);
      if (rec == null) {
        rec = new EmbeddedDartReconcilerHook();
        rec.connect(document);
        reconcilers.put(document, rec);
      }
      return rec;
    }
  }

  /**
   * Set the reconciler to be used for the given <code>document</code> to <code>reconciler</code>.
   * 
   * @param document The IDocument to reconcile
   * @param reconciler The EmbeddedDartReconcilerHook that does the reconciling
   */
  public void reconcileWith(IDocument document, EmbeddedDartReconcilerHook reconciler) {
    synchronized (reconcilers) {
      if (reconciler != null) {
        EmbeddedDartReconcilerHook rec = reconcilers.get(document);
        if (rec == reconciler) {
          return;
        }
        if (rec != null) {
          rec.disconnect(document);
        }
      }
      reconcilers.put(document, reconciler);
    }
  }
}
