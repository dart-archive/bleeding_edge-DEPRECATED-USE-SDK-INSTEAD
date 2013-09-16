/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.validation.core.errorinfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.views.markers.MarkerViewHandler;
import org.eclipse.wst.xml.core.internal.validation.core.ValidationMessage;
import org.eclipse.wst.xml.ui.internal.validation.XMLValidationUIMessages;

/**
 * Handler for the referenced file errors command. This class replaces the old
 * ReferencedFileErrorActionDelegate.
 */
public class ReferencedFileErrorsHandler extends MarkerViewHandler {
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final IMarker[] selectedMarkers = getSelectedMarkers(event);

    IMarker selectedMarker = selectedMarkers[0];

    if (selectedMarker != null) {
      try {

        IResource resource = selectedMarker.getResource();
        Map map = (Map) resource.getSessionProperty(ValidationMessage.ERROR_MESSAGE_MAP_QUALIFIED_NAME);
        if (map == null) {
          String infoUnavailable = XMLValidationUIMessages._UI_DETAILS_INFORMATION_UNAVAILABLE;
          String revalidateToRegenerateErrors = XMLValidationUIMessages._UI_DETAILS_INFO_REVALIDATE_TO_REGENERATE;
          MessageDialog.openInformation(Display.getCurrent().getActiveShell(), infoUnavailable,
              revalidateToRegenerateErrors);
        } else {
          String uri = null;

          String groupName = (String) selectedMarker.getAttribute("groupName"); //$NON-NLS-1$
          if (groupName.startsWith("referencedFileError")) //$NON-NLS-1$
          {
            int index1 = groupName.indexOf("("); //$NON-NLS-1$
            int index2 = groupName.lastIndexOf(")"); //$NON-NLS-1$
            if ((index1 != -1) && (index2 > index1)) {
              uri = groupName.substring(index1 + 1, index2);
            }
          }

          if (uri != null) {
            List list = Collections.EMPTY_LIST;

            ValidationMessage message = (ValidationMessage) map.get(uri);
            if (message != null) {
              list = message.getNestedMessages();
            }

            IPath resourceLocation = resource.getLocation();

            if (resourceLocation != null) {
              String validatedFileURI = resourceLocation.toOSString();
              validatedFileURI = "file:/" + validatedFileURI; //$NON-NLS-1$

              ReferencedFileErrorDialog dialog = new ReferencedFileErrorDialog(
                  HandlerUtil.getActiveShell(event), list, validatedFileURI, uri);
              dialog.createAndOpen();
            }
          }
        }
      } catch (CoreException e) {
        // Do nothing.
      }
    }

    return this;
  }
}
