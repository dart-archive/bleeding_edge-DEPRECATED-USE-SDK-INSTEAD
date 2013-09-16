/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.format;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.io.IOException;
import java.util.LinkedList;

public class StructuredFormattingStrategy extends ContextBasedFormattingStrategy {

  /** Documents to be formatted by this strategy */
  private final LinkedList fDocuments = new LinkedList();
  private IStructuredFormatProcessor fFormatProcessor;
  /** Partitions to be formatted by this strategy */
  private final LinkedList fPartitions = new LinkedList();
  private IRegion fRegion;

  /**
   * @param formatProcessor
   */
  public StructuredFormattingStrategy(IStructuredFormatProcessor formatProcessor) {
    super();

    fFormatProcessor = formatProcessor;
  }

  /*
   * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#format()
   */
  public void format() {
    super.format();

    final IDocument document = (IDocument) fDocuments.removeFirst();
    final TypedPosition partition = (TypedPosition) fPartitions.removeFirst();

    if (document != null && partition != null && fRegion != null && fFormatProcessor != null) {
      try {
        fFormatProcessor.formatDocument(document, fRegion.getOffset(), fRegion.getLength());
      } catch (IOException e) {
        // log for now, unless we find reason not to
        Logger.log(Logger.INFO, e.getMessage());
      } catch (CoreException e) {
        // log for now, unless we find reason not to
        Logger.log(Logger.INFO, e.getMessage());
      }
    }
  }

  /*
   * @see
   * org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStarts(org.eclipse
   * .jface.text.formatter.IFormattingContext)
   */
  public void formatterStarts(final IFormattingContext context) {
    super.formatterStarts(context);

    fPartitions.addLast(context.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
    fDocuments.addLast(context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM));
    fRegion = (IRegion) context.getProperty(FormattingContextProperties.CONTEXT_REGION);
  }

  /*
   * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStops()
   */
  public void formatterStops() {
    super.formatterStops();

    fPartitions.clear();
    fDocuments.clear();
  }
}
