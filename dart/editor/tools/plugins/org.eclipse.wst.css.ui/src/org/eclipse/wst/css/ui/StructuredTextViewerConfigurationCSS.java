/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.format.FormatProcessorCSS;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.core.internal.provisional.contenttype.ContentTypeIdForCSS;
import org.eclipse.wst.css.core.text.ICSSPartitions;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.contentassist.CSSStructuredContentAssistProcessor;
import org.eclipse.wst.css.ui.internal.preferences.CSSUIPreferenceNames;
import org.eclipse.wst.css.ui.internal.style.LineStyleProviderForCSS;
import org.eclipse.wst.sse.core.text.IStructuredPartitions;
import org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration;
import org.eclipse.wst.sse.ui.internal.format.StructuredFormattingStrategy;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Configuration for a source viewer which shows CSS.
 * <p>
 * Clients can subclass and override just those methods which must be specific to their needs.
 * </p>
 * 
 * @see org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration
 * @since 1.0
 */
public class StructuredTextViewerConfigurationCSS extends StructuredTextViewerConfiguration {
  /*
   * One instance per configuration because not sourceviewer-specific and it's a String array
   */
  private String[] fConfiguredContentTypes;
  /*
   * One instance per configuration
   */
  private LineStyleProvider fLineStyleProviderForCSS;

  /**
   * Create new instance of StructuredTextViewerConfigurationCSS
   */
  public StructuredTextViewerConfigurationCSS() {
    // Must have empty constructor to createExecutableExtension
    super();
  }

  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    List allStrategies = new ArrayList(0);

    IAutoEditStrategy[] superStrategies = super.getAutoEditStrategies(sourceViewer, contentType);
    for (int i = 0; i < superStrategies.length; i++) {
      allStrategies.add(superStrategies[i]);
    }

    return (IAutoEditStrategy[]) allStrategies.toArray(new IAutoEditStrategy[0]);
  }

  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    if (fConfiguredContentTypes == null) {
      fConfiguredContentTypes = new String[] {
          ICSSPartitions.STYLE, ICSSPartitions.COMMENT, IStructuredPartitions.DEFAULT_PARTITION,
          IStructuredPartitions.UNKNOWN_PARTITION};
    }
    return fConfiguredContentTypes;
  }

  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    final IContentAssistant assistant = super.getContentAssistant(sourceViewer);
    if (assistant instanceof ContentAssistant) {
      ((ContentAssistant) assistant).enableAutoInsert(CSSUIPlugin.getDefault().getPreferenceStore().getBoolean(
          CSSUIPreferenceNames.INSERT_SINGLE_SUGGESTION));
    }
    return assistant;
  }

  /**
   * @see org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration#getContentAssistProcessors(org.eclipse.jface.text.source.ISourceViewer,
   *      java.lang.String)
   */
  protected IContentAssistProcessor[] getContentAssistProcessors(ISourceViewer sourceViewer,
      String partitionType) {
    IContentAssistProcessor processor = new CSSStructuredContentAssistProcessor(
        this.getContentAssistant(), partitionType, sourceViewer);
    return new IContentAssistProcessor[] {processor};
  }

  public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
    IContentFormatter formatter = super.getContentFormatter(sourceViewer);
    // super was unable to create a formatter, probably because
    // sourceViewer does not have document set yet, so just create a
    // generic one
    if (!(formatter instanceof MultiPassContentFormatter))
      formatter = new MultiPassContentFormatter(getConfiguredDocumentPartitioning(sourceViewer),
          ICSSPartitions.STYLE);

    ((MultiPassContentFormatter) formatter).setMasterStrategy(new StructuredFormattingStrategy(
        new FormatProcessorCSS()));

    return formatter;
  }

  public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
    Vector vector = new Vector();

    // prefix[0] is either '\t' or ' ' x tabWidth, depending on preference
    Preferences preferences = CSSCorePlugin.getDefault().getPluginPreferences();
    int indentationWidth = preferences.getInt(CSSCorePreferenceNames.INDENTATION_SIZE);
    String indentCharPref = preferences.getString(CSSCorePreferenceNames.INDENTATION_CHAR);
    boolean useSpaces = CSSCorePreferenceNames.SPACE.equals(indentCharPref);

    for (int i = 0; i <= indentationWidth; i++) {
      StringBuffer prefix = new StringBuffer();
      boolean appendTab = false;

      if (useSpaces) {
        for (int j = 0; j + i < indentationWidth; j++)
          prefix.append(' ');

        if (i != 0)
          appendTab = true;
      } else {
        for (int j = 0; j < i; j++)
          prefix.append(' ');

        if (i != indentationWidth)
          appendTab = true;
      }

      if (appendTab) {
        prefix.append('\t');
        vector.add(prefix.toString());
        // remove the tab so that indentation - tab is also an indent
        // prefix
        prefix.deleteCharAt(prefix.length() - 1);
      }
      vector.add(prefix.toString());
    }

    vector.add(""); //$NON-NLS-1$

    return (String[]) vector.toArray(new String[vector.size()]);
  }

  public LineStyleProvider[] getLineStyleProviders(ISourceViewer sourceViewer, String partitionType) {
    LineStyleProvider[] providers = null;

    if (partitionType == ICSSPartitions.STYLE || partitionType == ICSSPartitions.COMMENT) {
      providers = new LineStyleProvider[] {getLineStyleProviderForCSS()};
    }

    return providers;
  }

  private LineStyleProvider getLineStyleProviderForCSS() {
    if (fLineStyleProviderForCSS == null) {
      fLineStyleProviderForCSS = new LineStyleProviderForCSS();
    }
    return fLineStyleProviderForCSS;
  }

  protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
    Map targets = super.getHyperlinkDetectorTargets(sourceViewer);
    targets.put(ContentTypeIdForCSS.ContentTypeID_CSS, null);
    return targets;
  }
}
