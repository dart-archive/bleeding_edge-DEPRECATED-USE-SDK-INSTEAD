/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import org.eclipse.wst.validation.internal.provisional.core.IMessage;

/**
 * Holds info to create a TemporaryAnnotation.
 * 
 * @since 1.0
 */
public class AnnotationInfo {

  public static final int NO_PROBLEM_ID = -1;
  private IMessage fMessage = null;
  private Object fAdditionalFixInfo = null;
  private int fProblemId = NO_PROBLEM_ID;

  public AnnotationInfo(IMessage message) {
    fMessage = message;

  }

  public AnnotationInfo(IMessage message, int problemId, Object additionalFixInfo) {
    this(message);
    fProblemId = problemId;
    fAdditionalFixInfo = additionalFixInfo;
  }

  public final IMessage getMessage() {
    return fMessage;
  }

  public final Object getAdditionalFixInfo() {
    return fAdditionalFixInfo;
  }

  public final int getProblemId() {
    return fProblemId;
  }
}
