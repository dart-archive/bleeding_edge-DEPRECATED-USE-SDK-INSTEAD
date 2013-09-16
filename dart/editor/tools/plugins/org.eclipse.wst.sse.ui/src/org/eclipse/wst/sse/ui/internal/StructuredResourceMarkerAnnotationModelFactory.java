/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModelFactory;

/**
 * @author nsd Used by the org.eclipse.core.filebuffers.annotationModelCreation extension point
 */
public class StructuredResourceMarkerAnnotationModelFactory extends
    ResourceMarkerAnnotationModelFactory {

  public StructuredResourceMarkerAnnotationModelFactory() {
    super();
  }

  /*
   * @see
   * org.eclipse.core.filebuffers.IAnnotationModelFactory#createAnnotationModel(org.eclipse.core
   * .runtime.IPath)
   */
  public IAnnotationModel createAnnotationModel(IPath location) {
    IAnnotationModel model = null;
    IFile file = FileBuffers.getWorkspaceFileAtLocation(location);
    if (file != null) {
      model = new StructuredResourceMarkerAnnotationModel(file);
    } else {
      model = new StructuredResourceMarkerAnnotationModel(ResourcesPlugin.getWorkspace().getRoot(),
          location.toString());
    }
    return model;
  }
}
