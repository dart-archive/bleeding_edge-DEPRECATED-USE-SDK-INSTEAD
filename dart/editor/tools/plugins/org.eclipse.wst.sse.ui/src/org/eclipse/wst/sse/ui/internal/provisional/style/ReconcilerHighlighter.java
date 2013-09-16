/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.style;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ReconcilerHighlighter {

  private static final String LINE_STYLE_PROVIDER_EXTENDED_ID = Highlighter.LINE_STYLE_PROVIDER_EXTENDED_ID;

  private Map fTableOfProviders = null;

  private Map fExtendedProviders = null;

  private ITextViewer fTextViewer = null;

  private final static boolean _trace = Boolean.valueOf(
      Platform.getDebugOption("org.eclipse.wst.sse.ui/structuredPresentationReconciler")).booleanValue(); //$NON-NLS-1$
  private final static String TRACE_PREFIX = "StructuredPresentationReconciler: "; //$NON-NLS-1$
  private long time0;

  /**
   * instance for older LineStyleProviders loaded by extension point, created if needed
   */
  private CompatibleHighlighter fCompatibleHighlighter = null;

  public void refreshDisplay() {
    if (_trace) {
      time0 = System.currentTimeMillis();
    }
    if (fTextViewer != null)
      fTextViewer.invalidateTextPresentation();
    if (_trace) {
      System.out.println(TRACE_PREFIX
          + "ReconcilerHighlighter refreshDisplay took " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
      System.out.flush();
    }
  }

  /**
   * Registers a given line style provider for a particular partition type. If there is already a
   * line style provider registered for this type, the new line style provider is registered instead
   * of the old one.
   * 
   * @param partitionType the partition type under which to register
   * @param the line style provider to register, or <code>null</code> to remove an existing one
   */
  public void addProvider(String partitionType, LineStyleProvider provider) {
    getTableOfProviders().put(partitionType, provider);
  }

  public void removeProvider(String partitionType) {
    getTableOfProviders().remove(partitionType);
  }

  public LineStyleProvider getProvider(String partitionType) {
    LineStyleProvider result = (LineStyleProvider) getTableOfProviders().get(partitionType);

    // The provider was not within the default set of providers. Use the
    // extended configuration
    // to look up additional providers
    if (result == null) {
      // NOT YET FINALIZED - DO NOT CONSIDER AS API
      synchronized (getExtendedProviders()) {
        if (!getExtendedProviders().containsKey(partitionType)) {
          result = (LineStyleProvider) ExtendedConfigurationBuilder.getInstance().getConfiguration(
              LINE_STYLE_PROVIDER_EXTENDED_ID, partitionType);
          getExtendedProviders().put(partitionType, result);

          if (result != null && fTextViewer != null
              && fTextViewer.getDocument() instanceof IStructuredDocument) {
            if (result instanceof AbstractLineStyleProvider) {
              ((AbstractLineStyleProvider) result).init(
                  (IStructuredDocument) fTextViewer.getDocument(), this);
            } else {
              Logger.log(Logger.INFO_DEBUG,
                  "CompatibleHighlighter installing compatibility for " + result.getClass()); //$NON-NLS-1$
              if (fCompatibleHighlighter == null) {
                fCompatibleHighlighter = new CompatibleHighlighter();
                fCompatibleHighlighter.install(fTextViewer);
              }
              result.init((IStructuredDocument) fTextViewer.getDocument(), fCompatibleHighlighter);
            }
          }
        } else {
          result = (LineStyleProvider) getExtendedProviders().get(partitionType);
        }
      }
    }
    return result;
  }

  private Map getTableOfProviders() {
    if (fTableOfProviders == null) {
      fTableOfProviders = new HashMap();
    }
    return fTableOfProviders;
  }

  private Map getExtendedProviders() {
    if (fExtendedProviders == null) {
      fExtendedProviders = new HashMap(3);
    }
    return fExtendedProviders;
  }

  public void install(ITextViewer textViewer) {
    fTextViewer = textViewer;
    if (fCompatibleHighlighter != null) {
      fCompatibleHighlighter.uninstall();
      fCompatibleHighlighter.install(fTextViewer);
    }
    refreshDisplay();
  }

  public void uninstall() {
    Iterator it = getTableOfProviders().values().iterator();

    while (it.hasNext()) {
      LineStyleProvider provider = (LineStyleProvider) it.next();
      if (provider != null)
        provider.release();
    }

    it = getExtendedProviders().values().iterator();
    while (it.hasNext()) {
      LineStyleProvider provider = (LineStyleProvider) it.next();
      if (provider != null)
        provider.release();
    }

    getTableOfProviders().clear();
    getExtendedProviders().clear();
    fTableOfProviders = null;
    if (fCompatibleHighlighter != null) {
      fCompatibleHighlighter.uninstall();
    }
    fTextViewer = null;
  }
}
