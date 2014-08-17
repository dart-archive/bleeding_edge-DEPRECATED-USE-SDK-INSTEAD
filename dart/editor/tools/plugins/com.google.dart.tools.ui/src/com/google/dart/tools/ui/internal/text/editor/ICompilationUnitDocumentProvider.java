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
package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.eclipse.ui.texteditor.IDocumentProviderExtension2;
import org.eclipse.ui.texteditor.IDocumentProviderExtension3;
import org.eclipse.ui.texteditor.IDocumentProviderExtension5;

/**
 * 
 */
public interface ICompilationUnitDocumentProvider extends IDocumentProvider,
    IDocumentProviderExtension, IDocumentProviderExtension2, IDocumentProviderExtension3,
    IDocumentProviderExtension5 {

  /**
   * Adds a listener that reports changes from all compilation unit annotation models.
   * 
   * @param listener the listener
   */
  void addGlobalAnnotationModelListener(IAnnotationModelListener listener);

  /**
   * Creates a line tracker for the given element. It is of the same kind as the one that would be
   * used for a newly created document for the given element.
   * 
   * @param element the element
   * @return a line tracker for the given element
   */
  ILineTracker createLineTracker(Object element);

  /**
   * Removes the listener.
   * 
   * @param listener the listener
   */
  void removeGlobalAnnotationModelListener(IAnnotationModelListener listener);

  /**
   * Saves the content of the given document to the given element. This method has only an effect if
   * it is called when directly or indirectly inside <code>saveDocument</code>.
   * 
   * @param monitor the progress monitor
   * @param element the element to which to save
   * @param document the document to save
   * @param overwrite <code>true</code> if the save should be enforced
   */
  void saveDocumentContent(IProgressMonitor monitor, Object element, IDocument document,
      boolean overwrite) throws CoreException;

  /**
   * Shuts down this provider.
   */
  void shutdown();
}
