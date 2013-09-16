/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.validation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.Validator;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

/**
 * This performs as you type validation for XML files
 */
public class DelegatingSourceValidatorForXML extends DelegatingSourceValidator {
  private final static String Id = "org.eclipse.wst.xml.core.xml"; //$NON-NLS-1$

  private Validator _validator;

  public DelegatingSourceValidatorForXML() {
  }

  private Validator getValidator() {
    if (_validator == null)
      _validator = ValidationFramework.getDefault().getValidator(Id);
    return _validator;
  }

  protected IValidator getDelegateValidator() {
    Validator v = getValidator();
    if (v == null)
      return null;
    return v.asIValidator();
  }

  protected boolean isDelegateValidatorEnabled(IFile file) {
    Validator v = getValidator();
    if (v == null)
      return false;
    if (!file.isSynchronized(IResource.DEPTH_ZERO))
      return false;
    if (!v.shouldValidate(file, false, false))
      return false;
    return v.isBuildValidation() || v.isManualValidation();
  }
}
