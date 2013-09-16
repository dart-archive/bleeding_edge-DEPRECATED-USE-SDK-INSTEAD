/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.wst.sse.ui.internal.IReleasable;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Content assistant that uses {@link CompoundContentAssistProcessor}s so that multiple processors
 * can be registered for each partition type
 * </p>
 */
public class StructuredContentAssistant extends ContentAssistant {
  /** need to retain copy of all releasable processors so they can be released on uninstall */
  private List fReleasableProcessors;

  /**
   * <code>true</code> if a content assist processor has been added to this assistant,
   * <code>false</code> otherwise
   */
  private boolean fIsInitalized;

  private boolean fProcessorsReleased = false;

  /**
   * <p>
   * Construct the assistant
   * </p>
   */
  public StructuredContentAssistant() {
    this.fIsInitalized = false;
    this.fReleasableProcessors = new ArrayList();
  }

  /**
   * <p>
   * Each set content assist processor is placed inside a CompoundContentAssistProcessor which
   * allows multiple processors per partition type
   * </p>
   * 
   * @param processor the content assist processor to register, or <code>null</code> to remove an
   *          existing one
   * @param contentType the content type under which to register
   * @see org.eclipse.jface.text.contentassist.ContentAssistant#setContentAssistProcessor(org.eclipse.jface.text.contentassist.IContentAssistProcessor,
   *      java.lang.String)
   */
  public void setContentAssistProcessor(IContentAssistProcessor processor, String partitionType) {
    this.fIsInitalized = true;

    CompoundContentAssistProcessor compoundProcessor = getExistingContentAssistProcessor(partitionType);
    if (compoundProcessor == null) {
      compoundProcessor = new CompoundContentAssistProcessor();
      this.fReleasableProcessors.add(compoundProcessor);
    }

    compoundProcessor.add(processor);
    super.setContentAssistProcessor(compoundProcessor, partitionType);
  }

  /**
   * Returns true if content assist has been initialized with some content assist processors. False
   * otherwise.
   * 
   * @return true if content assistant has been initialized
   */
  public boolean isInitialized() {
    return this.fIsInitalized;
  }

  /**
   * @param partitionType
   * @return
   */
  private CompoundContentAssistProcessor getExistingContentAssistProcessor(String partitionType) {
    CompoundContentAssistProcessor compoundContentAssistProcessor = null;
    IContentAssistProcessor processor = super.getContentAssistProcessor(partitionType);
    if (processor != null) {
      if (processor instanceof CompoundContentAssistProcessor) {
        compoundContentAssistProcessor = (CompoundContentAssistProcessor) processor;
      }
    }
    return compoundContentAssistProcessor;
  }

  public void install(ITextViewer textViewer) {
    if (fProcessorsReleased) {
      if (this.fReleasableProcessors != null && !this.fReleasableProcessors.isEmpty()) {
        for (int i = 0; i < this.fReleasableProcessors.size(); ++i) {
          ((CompoundContentAssistProcessor) this.fReleasableProcessors.get(i)).install(textViewer);
        }
      }
      fProcessorsReleased = false;
    }
    super.install(textViewer);
  }

  /**
   * @see org.eclipse.jface.text.contentassist.ContentAssistant#uninstall()
   */
  public void uninstall() {
    // dispose of all content assist processors
    if (this.fReleasableProcessors != null && !this.fReleasableProcessors.isEmpty()) {
      for (int i = 0; i < this.fReleasableProcessors.size(); ++i) {
        ((IReleasable) this.fReleasableProcessors.get(i)).release();
      }
    }
    fProcessorsReleased = true;
    this.fReleasableProcessors.clear();
    super.uninstall();
  }
}
