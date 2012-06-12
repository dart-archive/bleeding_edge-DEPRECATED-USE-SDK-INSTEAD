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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ide.FileStoreEditorInput;

import java.net.URI;

/**
 * Instances of the class <code>ExternalCompilationUnitEditorInputFactory</code> create instances of
 * the class {@link ExternalCompilationUnitEditorInput}.
 */
public class ExternalCompilationUnitEditorInputFactory implements IElementFactory {
  /**
   * This factory's ID.
   * <p>
   * The editor plug-in registers a factory by this name with the
   * <code>"org.eclipse.ui.elementFactories"<code> extension point.
   */
  static final String ID = "com.google.dart.tools.ui.ExternalCompilationUnitEditorInputFactory"; //$NON-NLS-1$

  /**
   * Tag for the URI string.
   */
  private static final String KEY_FILE_URI = "fileUri"; //$NON-NLS-1$

  /**
   * Tag for the URI string.
   */
  private static final String KEY_UNIT_URI = "unitUri"; //$NON-NLS-1$

  /**
   * Tag for the identifier string.
   */
  private static final String KEY_UNIT_ID = "unitId"; //$NON-NLS-1$

  /**
   * Save the state of the given editor input into the given memento.
   * 
   * @param memento the storage area for element state
   * @param input the editor input to be saved
   */
  static void saveState(IMemento memento, ExternalCompilationUnitEditorInput input) {
    memento.putString(KEY_FILE_URI, input.getURI().toString());
    memento.putString(KEY_UNIT_URI, input.getCompilationUnit().getUri().toString());
    memento.putString(KEY_UNIT_ID, input.getCompilationUnit().getHandleIdentifier());
  }

  @Override
  public IAdaptable createElement(IMemento memento) {
    String fileUriString = memento.getString(KEY_FILE_URI);
    if (fileUriString == null) {
      return null;
    }
    String unitUriString = memento.getString(KEY_UNIT_URI);
    if (unitUriString == null) {
      return null;
    }
    try {
      URI fileUri = new URI(fileUriString);
      URI unitUri = new URI(unitUriString);
      try {
        ExternalCompilationUnitImpl unit = DartModelManager.getInstance().getDartModel().getBundledCompilationUnit(
            unitUri);
        // if we can't find a bundled compilation unit, attempt to reconstitue the external CU from its handle
        if (unit == null) {

          String id = memento.getString(KEY_UNIT_ID);

          if (id != null) {
            DartElement element = DartCore.create(id);
            if (element instanceof ExternalCompilationUnitImpl) {
              return new ExternalCompilationUnitEditorInput(EFS.getStore(fileUri),
                  (ExternalCompilationUnitImpl) element);
            }

          }

          //if we have no handle, fall back on a filestore
          return new FileStoreEditorInput(EFS.getStore(fileUri));
        }
        return new ExternalCompilationUnitEditorInput(EFS.getStore(fileUri), unit);
      } catch (DartModelException exception) {
        return new FileStoreEditorInput(EFS.getStore(fileUri));
      }
    } catch (Exception exception) {
      return null;
    }
  }
}
