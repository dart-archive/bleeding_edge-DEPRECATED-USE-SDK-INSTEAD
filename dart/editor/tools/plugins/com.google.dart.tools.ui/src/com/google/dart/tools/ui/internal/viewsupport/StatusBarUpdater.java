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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.Messages;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Add the <code>StatusBarUpdater</code> to your ViewPart to have the statusbar describing the
 * selected elements.
 */
public class StatusBarUpdater implements ISelectionChangedListener {

  private final long LABEL_FLAGS = DartElementLabels.DEFAULT_QUALIFIED
      | DartElementLabels.ROOT_POST_QUALIFIED | DartElementLabels.APPEND_ROOT_PATH
      | DartElementLabels.M_PARAMETER_TYPES | DartElementLabels.M_PARAMETER_NAMES
      | DartElementLabels.M_APP_RETURNTYPE | DartElementLabels.M_EXCEPTIONS
      | DartElementLabels.F_APP_TYPE_SIGNATURE | DartElementLabels.T_TYPE_PARAMETERS;

  private IStatusLineManager fStatusLineManager;

  public StatusBarUpdater(IStatusLineManager statusLineManager) {
    fStatusLineManager = statusLineManager;
  }

  /*
   * @see ISelectionChangedListener#selectionChanged
   */
  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    String statusBarMessage = formatMessage(event.getSelection());
    fStatusLineManager.setMessage(statusBarMessage);
  }

  protected String formatMessage(ISelection sel) {
    if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
      IStructuredSelection selection = (IStructuredSelection) sel;

      int nElements = selection.size();
      if (nElements > 1) {
        return Messages.format(DartUIMessages.StatusBarUpdater_num_elements_selected,
            String.valueOf(nElements));
      } else {
        Object elem = selection.getFirstElement();
        if (elem instanceof DartElement) {
          return formatJavaElementMessage((DartElement) elem);
        } else if (elem instanceof IResource) {
          return formatResourceMessage((IResource) elem);
//        } else if (elem instanceof PackageFragmentRootContainer) {
//          PackageFragmentRootContainer container = (PackageFragmentRootContainer) elem;
//          return container.getLabel() + DartElementLabels.CONCAT_STRING
//              + container.getJavaProject().getElementName();
//        } else if (elem instanceof IJarEntryResource) {
//          IJarEntryResource jarEntryResource = (IJarEntryResource) elem;
//          StringBuffer buf = new StringBuffer(jarEntryResource.getName());
//          buf.append(DartElementLabels.CONCAT_STRING);
//          IPath fullPath = jarEntryResource.getFullPath();
//          if (fullPath.segmentCount() > 1) {
//            buf.append(fullPath.removeLastSegments(1).makeRelative());
//            buf.append(DartElementLabels.CONCAT_STRING);
//          }
//          DartElementLabels.getPackageFragmentRootLabel(
//              jarEntryResource.getPackageFragmentRoot(),
//              DartElementLabels.ROOT_POST_QUALIFIED, buf);
//          return buf.toString();
        } else if (elem instanceof IAdaptable) {
          IWorkbenchAdapter wbadapter = (IWorkbenchAdapter) ((IAdaptable) elem).getAdapter(IWorkbenchAdapter.class);
          if (wbadapter != null) {
            return wbadapter.getLabel(elem);
          }
        }
      }
    }
    return ""; //$NON-NLS-1$
  }

  private String formatJavaElementMessage(DartElement element) {
    return DartElementLabels.getElementLabel(element, LABEL_FLAGS);
  }

  private String formatResourceMessage(IResource element) {
    IContainer parent = element.getParent();
    if (parent != null && parent.getType() != IResource.ROOT) {
      return element.getName() + DartElementLabels.CONCAT_STRING
          + parent.getFullPath().makeRelative().toString();
    } else {
      return element.getName();
    }
  }

}
