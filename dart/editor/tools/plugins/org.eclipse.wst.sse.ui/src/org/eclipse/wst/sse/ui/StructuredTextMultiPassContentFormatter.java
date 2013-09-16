/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.wst.sse.core.internal.Logger;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends the MultiPassContentFormatter to allow clients to contribute additional slave formatting
 * strategies via the <code>org.eclipse.wst.sse.ui.editorConfiguration</code> extension point
 */
class StructuredTextMultiPassContentFormatter extends MultiPassContentFormatter {
  /**
   * The value of "type" attribute in the provisionalConfiguration element
   */
  private static final String SLAVE_FORMATTING_STRATEGY_EXTENDED_ID = "slaveformattingstrategy"; //$NON-NLS-1$
  /**
   * list of partition types that already have a formatting strategy
   */
  private List fInstalledPartitionTypes;

  /**
   * Creates a new content formatter.
   * 
   * @param partitioning the document partitioning for this formatter
   * @param type the default content type
   */
  public StructuredTextMultiPassContentFormatter(final String partitioning, final String type) {
    super(partitioning, type);
  }

  protected void formatMaster(IFormattingContext context, IDocument document, int offset, int length) {
    // for debugging purposes
    long startTime = System.currentTimeMillis();

    super.formatMaster(context, document, offset, length);

    if (Logger.DEBUG_FORMAT) {
      long endTime = System.currentTimeMillis();
      System.out.println("formatModel time: " + (endTime - startTime)); //$NON-NLS-1$
    }
  }

  /*
   * Overwritten to check for additional slave formatting strategies contributed via the
   * editorConfiguration extension point.
   */
  protected void formatSlave(IFormattingContext context, IDocument document, int offset,
      int length, String type) {
    List installedTypes = getInstalledPartitionTypes();
    if (installedTypes.contains(type)) {
      // we've already set the slave formatter strategy so just perform
      // as normal
      super.formatSlave(context, document, offset, length, type);
    } else {
      boolean findExtendedSlaveFormatter = true;

      // need to figure out if there's already a slave formatter set, so
      // just attempt to format as normal
      super.formatSlave(context, document, offset, length, type);

      // now, determine if slave formatter was already set by checking
      // context (it would be set if there's already one)
      Object contextPartition = context.getProperty(FormattingContextProperties.CONTEXT_PARTITION);
      if (contextPartition instanceof TypedPosition) {
        String contextType = ((TypedPosition) contextPartition).getType();
        if (contextType == type) {
          // there's already a slave formatter set, so just add it
          // to the list of installed partition types for future
          // reference
          installedTypes.add(type);
          findExtendedSlaveFormatter = false;
        }
      }
      // no slave formatter is set yet, so try to find one contributed
      // via the editorConfiguration extension point
      if (findExtendedSlaveFormatter) {
        Object configuration = ExtendedConfigurationBuilder.getInstance().getConfiguration(
            SLAVE_FORMATTING_STRATEGY_EXTENDED_ID, type);
        if (configuration instanceof IFormattingStrategy) {
          // found a formatter, so add it in
          setSlaveStrategy((IFormattingStrategy) configuration, type);
          // try to format slave again now that one is set
          super.formatSlave(context, document, offset, length, type);
        }
        // note that we've already checked this partition type for
        // future reference
        installedTypes.add(type);
      }
    }
  }

  /**
   * Get the list of partition types that have already been evaluated for slave formatting
   * strategies for this formatter.
   * 
   * @return List
   */
  private List getInstalledPartitionTypes() {
    if (fInstalledPartitionTypes == null)
      fInstalledPartitionTypes = new ArrayList();
    return fInstalledPartitionTypes;
  }
}
