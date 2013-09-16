/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.correction;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.sse.ui.internal.correction.IQuickAssistProcessor;
import org.eclipse.wst.sse.ui.internal.correction.IQuickFixProcessor;
import org.eclipse.wst.sse.ui.internal.correction.StructuredCorrectionProcessor;

/**
 * @deprecated since 2.0 RC0 Use org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
 */
public class CorrectionProcessorXML extends StructuredCorrectionProcessor {
  protected IQuickAssistProcessor fQuickAssistProcessor;
  protected IQuickFixProcessor fQuickFixProcessor;

  public CorrectionProcessorXML(ISourceViewer sourceViewer) {
    super(sourceViewer);
  }

  protected IQuickAssistProcessor getQuickAssistProcessor() {
    if (fQuickAssistProcessor == null) {
      fQuickAssistProcessor = new QuickAssistProcessorXML();
    }

    return fQuickAssistProcessor;
  }

  protected IQuickFixProcessor getQuickFixProcessor() {
    if (fQuickFixProcessor == null) {
      fQuickFixProcessor = new QuickFixProcessorXML();
    }

    return fQuickFixProcessor;
  }
}
