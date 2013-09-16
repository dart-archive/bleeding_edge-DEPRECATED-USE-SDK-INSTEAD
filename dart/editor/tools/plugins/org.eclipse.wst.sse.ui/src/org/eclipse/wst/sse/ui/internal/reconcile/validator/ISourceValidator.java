/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;

/**
 * Interface to allow for "partial document" as you type validation.
 */
public interface ISourceValidator {

  /**
   * As you type validation is getting "hooked up" to this IDocument. This is the instance of
   * IDocument that the validator should operate on for each validate call.
   * 
   * @param document
   */
  void connect(IDocument document);

  /**
   * The same IDocument passed in from the connect() method. This indicates that as you type
   * validation is "shutting down" for this IDocument.
   * 
   * @param document
   */
  void disconnect(IDocument document);

  /**
   * Like IValidator#validate(IValidationContext helper, IReporter reporter) except passes the dirty
   * region, so document validation can be better optimized.
   * 
   * @param dirtyRegion
   * @param helper
   * @param reporter
   */
  void validate(IRegion dirtyRegion, IValidationContext helper, IReporter reporter);
}
