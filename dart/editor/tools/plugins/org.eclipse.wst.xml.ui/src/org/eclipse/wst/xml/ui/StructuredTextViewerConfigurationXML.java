/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.wst.sse.core.text.IStructuredPartitions;
import org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;
import org.eclipse.wst.xml.core.internal.text.rules.StructuredTextPartitionerForXML;
import org.eclipse.wst.xml.core.text.IXMLPartitions;
import org.eclipse.wst.xml.ui.internal.XMLFormattingStrategy;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.autoedit.AutoEditStrategyForTabs;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLStructuredContentAssistProcessor;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeLabelProvider;
import org.eclipse.wst.xml.ui.internal.preferences.XMLUIPreferenceNames;
import org.eclipse.wst.xml.ui.internal.style.LineStyleProviderForXML;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Configuration for a source viewer which shows XML content.
 * <p>
 * Clients can subclass and override just those methods which must be specific to their needs.
 * </p>
 * 
 * @see org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration
 * @since 1.0
 */
public class StructuredTextViewerConfigurationXML extends StructuredTextViewerConfiguration {
  /*
   * One instance per configuration because not sourceviewer-specific and it's a String array
   */
  private String[] fConfiguredContentTypes;
  /*
   * One instance per configuration
   */
  private LineStyleProvider fLineStyleProviderForXML;
  private ILabelProvider fStatusLineLabelProvider;

  /**
   * Create new instance of StructuredTextViewerConfigurationXML
   */
  public StructuredTextViewerConfigurationXML() {
    // Must have empty constructor to createExecutableExtension
    super();
  }

  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    List allStrategies = new ArrayList(0);

    IAutoEditStrategy[] superStrategies = super.getAutoEditStrategies(sourceViewer, contentType);
    for (int i = 0; i < superStrategies.length; i++) {
      allStrategies.add(superStrategies[i]);
    }

    // be sure this is last, so it can modify any results form previous
    // commands that might on on same partiion type.
    // add auto edit strategy that handles when tab key is pressed
    allStrategies.add(new AutoEditStrategyForTabs());

    return (IAutoEditStrategy[]) allStrategies.toArray(new IAutoEditStrategy[allStrategies.size()]);
  }

  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {

    if (fConfiguredContentTypes == null) {
      String[] xmlTypes = StructuredTextPartitionerForXML.getConfiguredContentTypes();
      fConfiguredContentTypes = new String[xmlTypes.length + 2];
      fConfiguredContentTypes[0] = IStructuredPartitions.DEFAULT_PARTITION;
      fConfiguredContentTypes[1] = IStructuredPartitions.UNKNOWN_PARTITION;
      int index = 0;
      System.arraycopy(xmlTypes, 0, fConfiguredContentTypes, index += 2, xmlTypes.length);
    }
    return fConfiguredContentTypes;
  }

  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    final IContentAssistant assistant = super.getContentAssistant(sourceViewer);
    if (assistant instanceof ContentAssistant) {
      ((ContentAssistant) assistant).enableAutoInsert(XMLUIPlugin.getInstance().getPreferenceStore().getBoolean(
          XMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION));
    }
    return assistant;
  }

  /**
   * @see org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration#getContentAssistProcessors(org.eclipse.jface.text.source.ISourceViewer,
   *      java.lang.String)
   */
  protected IContentAssistProcessor[] getContentAssistProcessors(ISourceViewer sourceViewer,
      String partitionType) {
    IContentAssistProcessor processor = new XMLStructuredContentAssistProcessor(
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
          IXMLPartitions.XML_DEFAULT);
    ((MultiPassContentFormatter) formatter).setMasterStrategy(new XMLFormattingStrategy());
//		((MultiPassContentFormatter) formatter).setMasterStrategy(new StructuredFormattingStrategy(new FormatProcessorXML()));

    return formatter;
  }

  public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
    Vector vector = new Vector();

    // prefix[0] is either '\t' or ' ' x tabWidth, depending on preference
    Preferences preferences = XMLCorePlugin.getDefault().getPluginPreferences();
    int indentationWidth = preferences.getInt(XMLCorePreferenceNames.INDENTATION_SIZE);
    String indentCharPref = preferences.getString(XMLCorePreferenceNames.INDENTATION_CHAR);
    boolean useSpaces = XMLCorePreferenceNames.SPACE.equals(indentCharPref);

    for (int i = 0; i <= indentationWidth; i++) {
      StringBuffer prefix = new StringBuffer();
      boolean appendTab = false;

      if (useSpaces) {
        for (int j = 0; j + i < indentationWidth; j++) {
          prefix.append(' ');
        }

        if (i != 0) {
          appendTab = true;
        }
      } else {
        for (int j = 0; j < i; j++) {
          prefix.append(' ');
        }

        if (i != indentationWidth) {
          appendTab = true;
        }
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

    if ((partitionType == IXMLPartitions.XML_DEFAULT)
        || (partitionType == IXMLPartitions.XML_CDATA)
        || (partitionType == IXMLPartitions.XML_COMMENT)
        || (partitionType == IXMLPartitions.XML_DECLARATION)
        || (partitionType == IXMLPartitions.XML_PI)
        || (partitionType == IXMLPartitions.PROCESSING_INSTRUCTION_PREFIX)) {
      providers = new LineStyleProvider[] {getLineStyleProviderForXML()};
    }

    return providers;
  }

  private LineStyleProvider getLineStyleProviderForXML() {
    if (fLineStyleProviderForXML == null) {
      fLineStyleProviderForXML = new LineStyleProviderForXML();
    }
    return fLineStyleProviderForXML;
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

  protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
    Map targets = super.getHyperlinkDetectorTargets(sourceViewer);
    targets.put(ContentTypeIdForXML.ContentTypeID_XML, null);
    return targets;
  }
}
