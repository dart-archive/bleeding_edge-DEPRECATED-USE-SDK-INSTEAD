/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor;

/**
 * <p>
 * Implementation of {@link StructuredContentAssistProcessor} for CSS documents
 * </p>
 * <p>
 * Currently the CSS content assist processor does not do anything different then the
 * {@link StructuredContentAssistProcessor} but at some point it may have to react to user
 * preference changes
 * </p>
 * 
 * @see StructuredContentAssistProcessor
 */
public class CSSStructuredContentAssistProcessor extends StructuredContentAssistProcessor {
  /**
   * <p>
   * Constructor
   * </p>
   * 
   * @param assistant {@link ContentAssistant} to use
   * @param partitionTypeID the partition type this processor is for
   * @param viewer {@link ITextViewer} this processor is acting in
   */
  public CSSStructuredContentAssistProcessor(ContentAssistant assistant, String partitionTypeID,
      ITextViewer viewer) {

    super(assistant, partitionTypeID, viewer, null);
  }
}
