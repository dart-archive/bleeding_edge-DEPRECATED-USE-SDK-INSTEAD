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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Action to add (or remove) resources from the dart ignore list.
 */
public class IgnoreResourceAction extends SelectionListenerAction {

  public static interface IgnoreListener {
    void update();
  }

  private static boolean allElementsAreResources(IStructuredSelection selection) {
    for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
      Object selectedElement = iterator.next();
      if (!(selectedElement instanceof IResource)) {
        return false;
      }
    }
    return true;
  }

  private IResource resource;
  private final Shell shell;
  private final ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

  protected IgnoreResourceAction(Shell shell) {
    super(FilesViewMessages.IgnoreResourcesAction_dont_analyze_label);
    this.shell = shell;
  }

  public void addListener(IgnoreListener listener) {
    listeners.add(listener);
  }

  public void removeListener(IgnoreListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void run() {

    if (resource != null) {
      try {
        if (DartCore.isAnalyzed(resource)) {
          DartModelManager.getInstance().addToIgnores(resource);
          SystemLibraryManagerProvider.getDefaultAnalysisServer().discard(
              resource.getLocation().toFile());
        } else {
          DartModelManager.getInstance().removeFromIgnores(resource);
          File file = resource.getLocation().toFile();
          SystemLibraryManagerProvider.getDefaultAnalysisServer().scan(file, true);
        }
        for (Object listener : listeners.getListeners()) {
          if (listener instanceof IgnoreListener) {
            ((IgnoreListener) listener).update();
          }
        }
      } catch (IOException e) {
        MessageDialog.openError(shell, "Error Ignoring Resource", e.getMessage());
        DartCore.logInformation("Could not access ignore file", e);
      } catch (CoreException e) {
        MessageDialog.openError(shell, "Error Deleting Markers", e.getMessage()); //$NON-NLS-1$
      } finally {
        resource = null;
      }
    }
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    if (selection.size() != 1 || !allElementsAreResources(selection)) {
      resource = null;
      return false;
    }

    resource = (IResource) selection.getFirstElement();

    updateLabel();

    return true;
  }

  void updateLabel() {
    if (DartCore.isAnalyzed(resource)) {
      setText(FilesViewMessages.IgnoreResourcesAction_dont_analyze_label);
    } else {
      setText(FilesViewMessages.IgnoreResourcesAction_do_analyze_label);
    }
  }

}
