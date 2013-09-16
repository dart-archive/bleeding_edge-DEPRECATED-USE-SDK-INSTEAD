/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;

public class IncrementalHelper implements IValidationContext {
  private IProject fProject;

  private String fURI = null;

  public IncrementalHelper(IDocument sourceDocument, IProject project) {
    super();
    fProject = project;
  }

  public String getPortableName(IResource resource) {
    return resource.getProjectRelativePath().toString();
  }

  public IProject getProject() {
    return fProject;
  }

  public Object loadModel(String symbolicName) {
    return null;
  }

  public Object loadModel(String symbolicName, Object[] parms) {
    return null;
  }

  public void setURI(String uri) {
    fURI = uri;
  }

  public String[] getURIs() {
    if (fURI != null)
      return new String[] {fURI};
    return new String[0];
  }
}
