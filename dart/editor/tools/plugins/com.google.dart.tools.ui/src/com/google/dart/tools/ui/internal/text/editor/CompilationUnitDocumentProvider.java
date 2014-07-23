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
package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

import java.util.Iterator;

public class CompilationUnitDocumentProvider extends TextFileDocumentProvider implements
    ICompilationUnitDocumentProvider {

  private static final class GlobalAnnotationModelListener implements IAnnotationModelListener,
      IAnnotationModelListenerExtension {

    private ListenerList listenerList;

    public GlobalAnnotationModelListener() {
      listenerList = new ListenerList(ListenerList.IDENTITY);
    }

    public void addListener(IAnnotationModelListener listener) {
      listenerList.add(listener);
    }

    @Override
    public void modelChanged(AnnotationModelEvent event) {
      for (Object listener : listenerList.getListeners()) {
        if (listener instanceof IAnnotationModelListenerExtension) {
          ((IAnnotationModelListenerExtension) listener).modelChanged(event);
        }
      }
    }

    @Override
    public void modelChanged(IAnnotationModel model) {
      Object[] listeners = listenerList.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        ((IAnnotationModelListener) listeners[i]).modelChanged(model);
      }
    }

    public void removeListener(IAnnotationModelListener listener) {
      listenerList.remove(listener);
    }
  }

  /** Annotation model listener added to all created Compilation Unit annotation models */
  private final GlobalAnnotationModelListener globalAnnotationModelListener = new GlobalAnnotationModelListener();

  @Override
  public void addGlobalAnnotationModelListener(IAnnotationModelListener listener) {
    globalAnnotationModelListener.addListener(listener);
  }

  @Override
  public ILineTracker createLineTracker(Object element) {
    return new DefaultLineTracker();
  }

  @Override
  public boolean isModifiable(Object element) {
    //TODO (pquitslund): implement modifiability querying for new elements
    return super.isModifiable(element);
  }

  @Override
  public boolean isReadOnly(Object element) {
    //TODO (pquitslund): implement read-only querying for new elements
    return false;
  }

  @Override
  public void removeGlobalAnnotationModelListener(IAnnotationModelListener listener) {
    globalAnnotationModelListener.removeListener(listener);
  }

  @Override
  public void saveDocumentContent(IProgressMonitor monitor, Object element, IDocument document,
      boolean overwrite) throws CoreException {
    super.saveDocument(monitor, element, document, overwrite);
  }

  @Override
  public void setSavePolicy(ISavePolicy savePolicy) {
    // No-op
  }

  @Override
  public void shutdown() {
    Iterator<?> e = getConnectedElementsIterator();
    while (e.hasNext()) {
      disconnect(e.next());
    }
  }
}
