/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.core.text.IXMLPartitions;

/**
 * ContentAssistProcessor to handle special cases in content assist where the partitioner cannot
 * determine a partition type at the current cursor position (usually at EOF).
 * 
 * @author pavery
 * @deprecated This class is no longer used locally and will be removed in the future
 */
public class NoRegionContentAssistProcessor implements IContentAssistProcessor, IReleasable {

  private static final boolean DEBUG = false;
  protected char completionProposalAutoActivationCharacters[] = null;
  protected char contextInformationAutoActivationCharacters[] = null;

  private final ICompletionProposal[] EMPTY_PROPOSAL_SET = new ICompletionProposal[0];
  protected String fErrorMessage = null;
  private HashMap fNameToProcessorMap = null;
  private HashMap fPartitionToProcessorMap = null;

  public NoRegionContentAssistProcessor() {
    super();
  }

  protected void addPartitionProcessor(String key, IContentAssistProcessor processor) {
    addProcessor(getPartitionToProcessorMap(), key, processor);
  }

  protected void addNameProcessor(String key, IContentAssistProcessor processor) {
    addProcessor(getNameToProcessorMap(), key, processor);
  }

  protected IContentAssistProcessor getPartitionProcessor(String key) {
    return (IContentAssistProcessor) getPartitionToProcessorMap().get(key);
  }

  /**
   * Ensures release if it's a duplicate partition type.
   * 
   * @param map
   * @param key
   * @param processor
   */
  private void addProcessor(HashMap map, String key, IContentAssistProcessor processor) {
    Object o = map.remove(key);
    if (o != null) {
      if (o instanceof IReleasable) {
        ((IReleasable) o).release();
      }
    }
    map.put(key, processor);
  }

  /**
   * Figures out what the correct ICompletionProposalProcessor is and computesCompletionProposals on
   * that.
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
   *      int)
   */
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
    IContentAssistProcessor p = null;
    ICompletionProposal[] results = EMPTY_PROPOSAL_SET;

    p = guessContentAssistProcessor(viewer, documentOffset);
    if (p != null) {
      results = p.computeCompletionProposals(viewer, documentOffset);
    }

    return (results != null) ? results : EMPTY_PROPOSAL_SET;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.
   * eclipse.jface.text.ITextViewer, int)
   */
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
    // get context info from processor that we end up using...
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * getCompletionProposalAutoActivationCharacters()
   */
  public char[] getCompletionProposalAutoActivationCharacters() {
    return completionProposalAutoActivationCharacters;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * getContextInformationAutoActivationCharacters()
   */
  public char[] getContextInformationAutoActivationCharacters() {
    return contextInformationAutoActivationCharacters;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
   */
  public IContextInformationValidator getContextInformationValidator() {
    // return the validator for the content assist processor that we
    // used...
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
   */
  public String getErrorMessage() {
    return fErrorMessage;
  }

  private HashMap getNameToProcessorMap() {
    if (fNameToProcessorMap == null) {
      fNameToProcessorMap = new HashMap();
      initNameToProcessorMap();
    }
    return fNameToProcessorMap;
  }

  private HashMap getPartitionToProcessorMap() {
    if (fPartitionToProcessorMap == null) {
      fPartitionToProcessorMap = new HashMap();
      initPartitionToProcessorMap();
    }
    return fPartitionToProcessorMap;
  }

  /**
   * Gives you the document partition type (String) for the given StructuredTextViewer and
   * documentPosition.
   * 
   * @param viewer
   * @param documentPosition
   * @return String
   */
  protected String getPartitionType(StructuredTextViewer viewer, int documentPosition) {
    IDocument document = viewer.getDocument();
    String partitionType = null;
    ITypedRegion partition = null;
    try {
      partition = document.getPartition(documentPosition);
      partitionType = partition.getType();
    } catch (BadLocationException e) {
      partitionType = null;
    }
    return partitionType;
  }

  /**
   * Guesses a ContentAssistProcessor based on the TextViewer and documentOffset.
   * 
   * @param viewer
   * @param documentOffset
   */
  protected IContentAssistProcessor guessContentAssistProcessor(ITextViewer viewer,
      int documentOffset) {
    // mapping logic here...
    // look @ previous region
    // look @ previous doc partition type
    // look @ page language
    IContentAssistProcessor p = null;
    IStructuredDocumentRegion sdRegion = ContentAssistUtils.getStructuredDocumentRegion(viewer,
        documentOffset);
    if (sdRegion != null) {
      String currentRegionType = sdRegion.getType();
      // System.out.println("current region type is >> " +
      // currentRegionType);
      if (currentRegionType == DOMRegionContext.UNDEFINED) {
        IStructuredDocumentRegion sdPrev = sdRegion.getPrevious();
        if (sdPrev != null) {
          if (DEBUG) {
            String prevRegionType = sdPrev.getType();
            System.out.println("previous region type is >> " + prevRegionType); //$NON-NLS-1$
          }
        }
      }
    }
    // working w/ viewer & document partition
    if ((p == null) && (viewer.getDocument().getLength() > 0)) {
      String prevPartitionType = getPartitionType((StructuredTextViewer) viewer, documentOffset - 1);
      // System.out.println("previous partition type is > " +
      // prevPartitionType);
      p = (IContentAssistProcessor) getPartitionToProcessorMap().get(prevPartitionType);
    }
    return p;
  }

  /**
   * Inits map for extra ContentAssistProcessors (useBean, get/setProperty)
   */
  protected void initNameToProcessorMap() {
    // nothing to do in this case
  }

  /**
   * Adds all relevent ContentAssistProcessors to the partition to processor map (just XML here)
   */
  protected void initPartitionToProcessorMap() {
    XMLContentAssistProcessor xmlProcessor = new XMLContentAssistProcessor();
    addProcessor(getPartitionToProcessorMap(), IXMLPartitions.XML_DEFAULT, xmlProcessor);
  }

  public void release() {
    releasePartitionToProcessorMap();
    releaseNameToProcessorMap();
  }

  protected void releaseMap(HashMap map) {
    if ((map != null) && !map.isEmpty()) {
      Iterator it = map.keySet().iterator();
      Object key = null;
      while (it.hasNext()) {
        key = it.next();
        if (map.get(key) instanceof IReleasable) {
          ((IReleasable) map.get(key)).release();
        }
      }
      map.clear();
      map = null;
    }
  }

  protected void releaseNameToProcessorMap() {
    releaseMap(fNameToProcessorMap);
  }

  protected void releasePartitionToProcessorMap() {
    releaseMap(fPartitionToProcessorMap);
  }
}
