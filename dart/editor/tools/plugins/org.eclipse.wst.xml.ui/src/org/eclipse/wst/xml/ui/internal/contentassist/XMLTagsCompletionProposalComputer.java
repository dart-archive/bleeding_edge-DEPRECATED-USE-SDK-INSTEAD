/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;

/**
 * <p>
 * {@link AbstractXMLModelQueryCompletionProposalComputer} used to generate XML tag content assist
 * proposals
 * </p>
 * <p>
 * <b>NOTE:</b> Currently this computer does not filter out any of the model query results so it
 * will return all proposals from the model query for the current content type. In the future this
 * may need to change.
 * </p>
 */
public class XMLTagsCompletionProposalComputer extends
    AbstractXMLModelQueryCompletionProposalComputer {

  /** the generated used to generate the proposals */
  protected XMLContentModelGenerator fGenerator;

  /** the context information validator for this computer */
  private IContextInformationValidator fContextInformationValidator;

  /**
   * <p>
   * Default constructor
   * </p>
   */
  public XMLTagsCompletionProposalComputer() {
    this.fContextInformationValidator = null;
  }

  /**
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLModelQueryCompletionProposalComputer#getContentGenerator()
   */
  protected XMLContentModelGenerator getContentGenerator() {
    if (fGenerator == null) {
      fGenerator = new XMLContentModelGenerator();
    }
    return fGenerator;
  }

  /**
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#computeContextInformation(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeContextInformation(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {

    AttributeContextInformationProvider attributeInfoProvider = new AttributeContextInformationProvider(
        (IStructuredDocument) context.getDocument(),
        (AttributeContextInformationPresenter) getContextInformationValidator());
    return Arrays.asList(attributeInfoProvider.getAttributeInformation(context.getInvocationOffset()));
  }

  /**
   * <p>
   * Filters out any model query actions that are not specific to XML
   * </p>
   * <p>
   * <b>NOTE:</b> Currently nothing is filtered so this computer returns all results from the model
   * query for the current content type
   * </p>
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLModelQueryCompletionProposalComputer#validModelQueryNode(org.eclipse.wst.xml.core.internal.contentmodel.CMNode)
   */
  protected boolean validModelQueryNode(CMNode node) {
    return true;
  }

  /**
   * Returns a validator used to determine when displayed context information should be dismissed.
   * May only return <code>null</code> if the processor is incapable of computing context
   * information. a context information validator, or <code>null</code> if the processor is
   * incapable of computing context information
   */
  private IContextInformationValidator getContextInformationValidator() {
    if (fContextInformationValidator == null) {
      fContextInformationValidator = new AttributeContextInformationPresenter();
    }
    return fContextInformationValidator;
  }
}
