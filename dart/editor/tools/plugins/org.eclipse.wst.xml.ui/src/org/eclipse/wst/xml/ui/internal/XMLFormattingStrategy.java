/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal;

import java.util.LinkedList;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.IFormattingStrategyExtension;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.xml.core.internal.formatter.DefaultXMLPartitionFormatter;

public class XMLFormattingStrategy extends ContextBasedFormattingStrategy implements
    IFormattingStrategyExtension {

  /** Documents to be formatted by this strategy */
  private final LinkedList fDocuments = new LinkedList();
  /** Partitions to be formatted by this strategy */
  private final LinkedList fPartitions = new LinkedList();
  private IRegion fRegion;
  private DefaultXMLPartitionFormatter formatter = new DefaultXMLPartitionFormatter();

  /**
   * @param formatProcessor
   */
  public XMLFormattingStrategy() {
    super();
  }

  /*
   * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#format()
   */
  public void format() {
    super.format();

    final IDocument document = (IDocument) fDocuments.removeFirst();
    final TypedPosition partition = (TypedPosition) fPartitions.removeFirst();

    if (document != null && partition != null && fRegion != null) {
      try {
        if (document instanceof IStructuredDocument) {
          IStructuredModel model = StructuredModelManager.getModelManager().getModelForEdit(
              (IStructuredDocument) document);
          if (model != null) {
            try {
              TextEdit edit = formatter.format(model, fRegion.getOffset(), fRegion.getLength());
              if (edit != null) {
                try {
                  model.aboutToChangeModel();
                  edit.apply(document);
                } finally {
                  model.changedModel();
                }
              }
            } finally {
              model.releaseFromEdit();
            }
          }
        }
      } catch (BadLocationException e) {
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
