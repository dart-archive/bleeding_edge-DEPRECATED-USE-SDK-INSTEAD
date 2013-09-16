/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.wst.css.core.text.ICSSPartitions;
import org.eclipse.wst.css.ui.internal.style.LineStyleProviderForEmbeddedCSS;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.format.HTMLFormatProcessorImpl;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.html.core.internal.provisional.contenttype.ContentTypeIdForHTML;
import org.eclipse.wst.html.core.internal.text.StructuredTextPartitionerForHTML;
import org.eclipse.wst.html.core.text.IHTMLPartitions;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.autoedit.AutoEditStrategyForTabs;
import org.eclipse.wst.html.ui.internal.contentassist.HTMLStructuredContentAssistProcessor;
import org.eclipse.wst.html.ui.internal.preferences.HTMLUIPreferenceNames;
import org.eclipse.wst.html.ui.internal.style.LineStyleProviderForHTML;
import org.eclipse.wst.sse.core.text.IStructuredPartitions;
import org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration;
import org.eclipse.wst.sse.ui.internal.format.StructuredFormattingStrategy;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;
import org.eclipse.wst.xml.core.internal.text.rules.StructuredTextPartitionerForXML;
import org.eclipse.wst.xml.core.text.IXMLPartitions;
import org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeLabelProvider;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Configuration for a source viewer which shows HTML content.
 * <p>
 * Clients can subclass and override just those methods which must be specific to their needs.
 * </p>
 * 
 * @see org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration
 * @since 1.0
 */
public class StructuredTextViewerConfigurationHTML extends StructuredTextViewerConfiguration {
  /*
   * One instance per configuration because not sourceviewer-specific and it's a String array
   */
  private String[] fConfiguredContentTypes;
  /*
   * One instance per configuration
   */
  private LineStyleProvider fLineStyleProviderForEmbeddedCSS;
  /*
   * One instance per configuration
   */
  private LineStyleProvider fLineStyleProviderForHTML;
  /*
   * One instance per configuration
   */
  private StructuredTextViewerConfiguration fXMLSourceViewerConfiguration;
  private ILabelProvider fStatusLineLabelProvider;

  /**
   * Create new instance of StructuredTextViewerConfigurationHTML
   */
  public StructuredTextViewerConfigurationHTML() {
    // Must have empty constructor to createExecutableExtension
    super();
  }

  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    List allStrategies = new ArrayList(0);

    IAutoEditStrategy[] superStrategies = super.getAutoEditStrategies(sourceViewer, contentType);
    for (int i = 0; i < superStrategies.length; i++) {
      allStrategies.add(superStrategies[i]);
    }

    // be sure this is added last in list, so it has a change to modify
    // previous results.
    // add auto edit strategy that handles when tab key is pressed
    allStrategies.add(new AutoEditStrategyForTabs());

    return (IAutoEditStrategy[]) allStrategies.toArray(new IAutoEditStrategy[allStrategies.size()]);
  }

  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    if (fConfiguredContentTypes == null) {
      String[] xmlTypes = StructuredTextPartitionerForXML.getConfiguredContentTypes();
      String[] htmlTypes = StructuredTextPartitionerForHTML.getConfiguredContentTypes();
      fConfiguredContentTypes = new String[2 + xmlTypes.length + htmlTypes.length];

      fConfiguredContentTypes[0] = IStructuredPartitions.DEFAULT_PARTITION;
      fConfiguredContentTypes[1] = IStructuredPartitions.UNKNOWN_PARTITION;

      int index = 0;
      System.arraycopy(xmlTypes, 0, fConfiguredContentTypes, index += 2, xmlTypes.length);
      System.arraycopy(htmlTypes, 0, fConfiguredContentTypes, index += xmlTypes.length,
          htmlTypes.length);
    }

    return fConfiguredContentTypes;
  }

  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    final IContentAssistant assistant = super.getContentAssistant(sourceViewer);
    if (assistant instanceof ContentAssistant) {
      ((ContentAssistant) assistant).enableAutoInsert(HTMLUIPlugin.getInstance().getPreferenceStore().getBoolean(
          HTMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION));
    }
    return assistant;
  }

  protected IContentAssistProcessor[] getContentAssistProcessors(ISourceViewer sourceViewer,
      String partitionType) {

    IContentAssistProcessor processor = new HTMLStructuredContentAssistProcessor(
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
          IHTMLPartitions.HTML_DEFAULT);
    ((MultiPassContentFormatter) formatter).setMasterStrategy(new StructuredFormattingStrategy(
        new HTMLFormatProcessorImpl()));

    return formatter;
  }

  public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer,
      String contentType) {
    if (contentType == IHTMLPartitions.HTML_DEFAULT) {
      // use xml's doubleclick strategy
      return getXMLSourceViewerConfiguration().getDoubleClickStrategy(sourceViewer,
          IXMLPartitions.XML_DEFAULT);
    } else
      return super.getDoubleClickStrategy(sourceViewer, contentType);

  }

  public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
    Vector vector = new Vector();

    // prefix[0] is either '\t' or ' ' x tabWidth, depending on preference
    Preferences preferences = HTMLCorePlugin.getDefault().getPluginPreferences();
    int indentationWidth = preferences.getInt(HTMLCorePreferenceNames.INDENTATION_SIZE);
    String indentCharPref = preferences.getString(HTMLCorePreferenceNames.INDENTATION_CHAR);
    boolean useSpaces = HTMLCorePreferenceNames.SPACE.equals(indentCharPref);

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

    // workaround IXMLPartitions.XML_PI
    if (partitionType == IHTMLPartitions.HTML_DEFAULT
        || partitionType == IHTMLPartitions.HTML_COMMENT
        || partitionType == IHTMLPartitions.HTML_DECLARATION
        || partitionType == IXMLPartitions.XML_PI) {
      providers = new LineStyleProvider[] {getLineStyleProviderForHTML()};
    } else if (partitionType == ICSSPartitions.STYLE || partitionType == ICSSPartitions.COMMENT) {
      providers = new LineStyleProvider[] {getLineStyleProviderForEmbeddedCSS()};
    }

    return providers;
  }

  private LineStyleProvider getLineStyleProviderForEmbeddedCSS() {
    if (fLineStyleProviderForEmbeddedCSS == null) {
      fLineStyleProviderForEmbeddedCSS = new LineStyleProviderForEmbeddedCSS();
    }
    return fLineStyleProviderForEmbeddedCSS;
  }

  private LineStyleProvider getLineStyleProviderForHTML() {
    if (fLineStyleProviderForHTML == null) {
      fLineStyleProviderForHTML = new LineStyleProviderForHTML();
    }
    return fLineStyleProviderForHTML;
  }

  public ILabelProvider getStatusLineLabelProvider(ISourceViewer sourceViewer) {
    if (fStatusLineLabelProvider == null) {
      fStatusLineLabelProvider = new JFaceNodeLabelProvider() {
        public String getText(Object element) {
          if (element == null)
            return null;

          StringBuffer s = new StringBuffer();
          Node node = (Node) element;
          while (node != null) {
            if (node.getNodeType() != Node.DOCUMENT_NODE) {
              s.insert(0, super.getText(node));
            }

            if (node.getNodeType() == Node.ATTRIBUTE_NODE)
              node = ((Attr) node).getOwnerElement();
            else
              node = node.getParentNode();

            if (node != null && node.getNodeType() != Node.DOCUMENT_NODE) {
              s.insert(0, IPath.SEPARATOR);
            }
          }
          return s.toString();
        }

      };
    }
    return fStatusLineLabelProvider;
  }

  private StructuredTextViewerConfiguration getXMLSourceViewerConfiguration() {
    if (fXMLSourceViewerConfiguration == null) {
      fXMLSourceViewerConfiguration = new StructuredTextViewerConfigurationXML();
    }
    return fXMLSourceViewerConfiguration;
  }

  protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
    Map targets = super.getHyperlinkDetectorTargets(sourceViewer);
    targets.put(ContentTypeIdForHTML.ContentTypeID_HTML, null);

    // also add xml since there could be xml content in html
    // (just hope the hyperlink detectors will do additional checking)
    targets.put(ContentTypeIdForXML.ContentTypeID_XML, null);
    return targets;
  }
}
