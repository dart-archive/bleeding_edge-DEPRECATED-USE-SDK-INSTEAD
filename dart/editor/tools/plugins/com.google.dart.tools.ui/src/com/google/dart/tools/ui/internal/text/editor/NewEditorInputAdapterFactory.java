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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

/**
 * Adapts {@link IEditorInput}s to Dart {@link Element}s.
 * <p>
 * To replace {@link EditorInputAdapterFactory}.
 */
@SuppressWarnings("rawtypes")
public class NewEditorInputAdapterFactory implements IAdapterFactory {

  @Override
  public Object getAdapter(Object element, Class key) {

    if (Element.class.equals(key) && element instanceof IFileEditorInput) {

      IFile file = ((IFileEditorInput) element).getFile();

      
      //TODO (pquitslund): handle more cases (and better)
      
      LibraryElement libraryElement = DartCore.getProjectManager().getLibraryElementOrNull(file);
      if (libraryElement != null) {
        return libraryElement.getDefiningCompilationUnit();
      }

      if (element instanceof IStorageEditorInput) {
        try {
          return ((IStorageEditorInput) element).getStorage().getAdapter(key);
        } catch (CoreException ex) {
          DartToolsPlugin.log(ex);
        }
      }
    }

    return null;
  }

  @Override
  public Class[] getAdapterList() {
    return new Class[] {Element.class};
  }

}
