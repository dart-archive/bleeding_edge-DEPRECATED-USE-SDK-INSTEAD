/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStorageEditorInput;

/**
 * This IAdapterFactory adapts IEditorInputs into DartElements.
 */
@SuppressWarnings("rawtypes")
public class EditorInputAdapterFactory implements IAdapterFactory {

  @Override
  public Object getAdapter(Object element, Class key) {
    if (DartElement.class.equals(key) && element instanceof IEditorInput) {
      DartElement dartElement = DartUI.getWorkingCopyManager().getWorkingCopy(
          (IEditorInput) element);

      if (dartElement != null) {
        return dartElement;
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
    return new Class[] {DartElement.class};
  }

}
