/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.preferences.XMLUIPreferenceNames;
import org.eclipse.wst.xml.ui.internal.templates.TemplateContextTypeIdsXML;

/**
 * @deprecated This class is no longer used locally and will be removed in the future
 * @see XMLStructuredContentAssistProcessor
 */
public class XMLContentAssistProcessor extends AbstractContentAssistProcessor implements
    IPropertyChangeListener {

  protected IPreferenceStore fPreferenceStore = null;
  protected IResource fResource = null;
  private XMLTemplateCompletionProcessor fTemplateProcessor = null;
  private List fTemplateContexts = new ArrayList();

  public XMLContentAssistProcessor() {
    super();
  }

  protected void addAttributeNameProposals(ContentAssistRequest contentAssistRequest) {
    addTemplates(contentAssistRequest, TemplateContextTypeIdsXML.ATTRIBUTE);
    super.addAttributeNameProposals(contentAssistRequest);
  }

  protected void addAttributeValueProposals(ContentAssistRequest contentAssistRequest) {
    addTemplates(contentAssistRequest, TemplateContextTypeIdsXML.ATTRIBUTE_VALUE);
    super.addAttributeValueProposals(contentAssistRequest);
  }

  protected void addEmptyDocumentProposals(ContentAssistRequest contentAssistRequest) {
    addTemplates(contentAssistRequest, TemplateContextTypeIdsXML.NEW);
    super.addEmptyDocumentProposals(contentAssistRequest);
  }

  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest,
      int childPosition) {
    addTemplates(contentAssistRequest, TemplateContextTypeIdsXML.TAG);
    super.addTagInsertionProposals(contentAssistRequest, childPosition);
  }

  /**
   * Adds templates to the list of proposals
   * 
   * @param contentAssistRequest
   * @param context
   */
  private void addTemplates(ContentAssistRequest contentAssistRequest, String context) {
    addTemplates(contentAssistRequest, context, contentAssistRequest.getReplacementBeginPosition());
  }

  /**
   * Adds templates to the list of proposals
   * 
   * @param contentAssistRequest
   * @param context
   * @param startOffset
   */
  private void addTemplates(ContentAssistRequest contentAssistRequest, String context,
      int startOffset) {
    if (contentAssistRequest == null) {
      return;
    }

    // if already adding template proposals for a certain context type, do
    // not add again
    if (!fTemplateContexts.contains(context)) {
      fTemplateContexts.add(context);
      boolean useProposalList = !contentAssistRequest.shouldSeparate();

      if (getTemplateCompletionProcessor() != null) {
        getTemplateCompletionProcessor().setContextType(context);
        ICompletionProposal[] proposals = getTemplateCompletionProcessor().computeCompletionProposals(
            fTextViewer, startOffset);
        for (int i = 0; i < proposals.length; ++i) {
          if (useProposalList) {
            contentAssistRequest.addProposal(proposals[i]);
          } else {
            contentAssistRequest.addMacro(proposals[i]);
          }
        }
      }
    }
  }

  protected ContentAssistRequest computeCompletionProposals(int documentPosition,
      String matchString, ITextRegion completionRegion, IDOMNode treeNode, IDOMNode xmlnode) {
    ContentAssistRequest request = super.computeCompletionProposals(documentPosition, matchString,
        completionRegion, treeNode, xmlnode);
    // bug115927 use original document position for all/any region
    // templates
    addTemplates(request, TemplateContextTypeIdsXML.ALL, documentPosition);
    return request;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.xml.ui.contentassist.AbstractContentAssistProcessor#computeCompletionProposals
   * (org.eclipse.jface.text.ITextViewer, int)
   */
  public ICompletionProposal[] computeCompletionProposals(ITextViewer textViewer,
      int documentPosition) {
    fTemplateContexts.clear();
    return super.computeCompletionProposals(textViewer, documentPosition);
  }

  protected IPreferenceStore getPreferenceStore() {
    if (fPreferenceStore == null) {
      fPreferenceStore = XMLUIPlugin.getDefault().getPreferenceStore();
    }
    return fPreferenceStore;
  }

  private XMLTemplateCompletionProcessor getTemplateCompletionProcessor() {
    if (fTemplateProcessor == null) {
      fTemplateProcessor = new XMLTemplateCompletionProcessor();
    }
    return fTemplateProcessor;
  }

  protected void init() {
    getPreferenceStore().addPropertyChangeListener(this);
    reinit();
  }

  public void propertyChange(PropertyChangeEvent event) {
    String property = event.getProperty();

    if ((property.compareTo(XMLUIPreferenceNames.AUTO_PROPOSE) == 0)
        || (property.compareTo(XMLUIPreferenceNames.AUTO_PROPOSE_CODE) == 0)) {
      reinit();
    }
  }

  protected void reinit() {
    String key = XMLUIPreferenceNames.AUTO_PROPOSE;
    boolean doAuto = getPreferenceStore().getBoolean(key);
    if (doAuto) {
      key = XMLUIPreferenceNames.AUTO_PROPOSE_CODE;
      completionProposalAutoActivationCharacters = getPreferenceStore().getString(key).toCharArray();
    } else {
      completionProposalAutoActivationCharacters = null;
    }
  }

  public void release() {
    super.release();
    getPreferenceStore().removePropertyChangeListener(this);
  }
}
