/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.wst.ui;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.wst.ui.autoedit.AutoEditStrategyForDart;
import com.google.dart.tools.wst.ui.contentassist.DartStructuredContentAssistProcessor;
import com.google.dart.tools.wst.ui.style.LineStyleProviderForDart;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.html.core.internal.text.StructuredTextPartitionerForHTML;
import org.eclipse.wst.html.core.text.IHTMLPartitions;
import org.eclipse.wst.html.ui.StructuredTextViewerConfigurationHTML;
import org.eclipse.wst.sse.core.text.IStructuredPartitions;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;
import org.eclipse.wst.xml.core.internal.text.rules.StructuredTextPartitionerForXML;

import java.util.Map;

@SuppressWarnings("restriction")
public class StructuredTextViewerConfigurationDart extends StructuredTextViewerConfigurationHTML {

  /**
   * One instance per configuration because not source viewer-specific and it's a String array.
   */
  private String[] configuredContentTypes;

  public static final String DART_SCRIPT = IHTMLPartitions.SCRIPT + ".type.APPLICATION/DART";

  public static final String DART_SCRIPT_EVENTHANDLER = IHTMLPartitions.SCRIPT_EVENTHANDLER
      + ".type.APPLICATION/DART";

  /**
   * Create new instance of StructuredTextViewerConfigurationDart.
   */
  public StructuredTextViewerConfigurationDart() {
    super();
  }

  @Override
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    if (contentType.equals(IHTMLPartitions.SCRIPT) || contentType.equals(DART_SCRIPT)
        || contentType.equals(IHTMLPartitions.SCRIPT_EVENTHANDLER)
        || contentType.equals(DART_SCRIPT_EVENTHANDLER)) {
      IAutoEditStrategy[] strategies = new IAutoEditStrategy[1];
      strategies[0] = new AutoEditStrategyForDart();
      return strategies;
    } else {
      return super.getAutoEditStrategies(sourceViewer, contentType);
    }
  }

  /**
   * Add Dart script tags to known content types for xml and html.
   */
  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    if (configuredContentTypes == null) {
      String[] xmlTypes = StructuredTextPartitionerForXML.getConfiguredContentTypes();
      String[] htmlTypes = StructuredTextPartitionerForHTML.getConfiguredContentTypes();
      String[] dartTypes = new String[] {
          "org.eclipse.wst.html.SCRIPT.type.APPLICATION/DART",
          "org.eclipse.wst.html.SCRIPT.EVENTHANDLER.type.APPLICATION/DART"};
      configuredContentTypes = new String[2 + xmlTypes.length + htmlTypes.length + dartTypes.length];

      configuredContentTypes[0] = IStructuredPartitions.DEFAULT_PARTITION;
      configuredContentTypes[1] = IStructuredPartitions.UNKNOWN_PARTITION;

      int index = 0;
      System.arraycopy(xmlTypes, 0, configuredContentTypes, index += 2, xmlTypes.length);
      System.arraycopy(
          htmlTypes,
          0,
          configuredContentTypes,
          index += xmlTypes.length,
          htmlTypes.length);
      System.arraycopy(
          dartTypes,
          0,
          configuredContentTypes,
          index += htmlTypes.length,
          dartTypes.length);
    }

    return configuredContentTypes;
  }

  @Override
  public LineStyleProvider[] getLineStyleProviders(ISourceViewer sourceViewer, String partitionType) {
    if (partitionType.equals("org.eclipse.wst.html.SCRIPT.type.APPLICATION/DART")
        || partitionType.equals("org.eclipse.wst.html.SCRIPT.EVENTHANDLER.type.APPLICATION/DART")) {
      // This duplicates the XML in plugin.xml. It is a place-holder for future use.
      return new LineStyleProvider[] {new LineStyleProviderForDart()};
    } else {
      return super.getLineStyleProviders(sourceViewer, partitionType);
    }
  }

  @Override
  protected IContentAssistProcessor[] getContentAssistProcessors(ISourceViewer sourceViewer,
      String partitionType) {
    if (partitionType.equals("org.eclipse.wst.html.SCRIPT.type.APPLICATION/DART")
        || partitionType.equals("org.eclipse.wst.html.SCRIPT.EVENTHANDLER.type.APPLICATION/DART")) {
      IContentAssistProcessor processor = new DartStructuredContentAssistProcessor(
          this.getContentAssistant(),
          partitionType,
          sourceViewer,
          DartToolsPlugin.getDefault().getPreferenceStore());
      return new IContentAssistProcessor[] {processor};
    } else {
      return super.getContentAssistProcessors(sourceViewer, partitionType);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Map<String, Object> getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
    Map<String, Object> targets = super.getHyperlinkDetectorTargets(sourceViewer);
    targets.put("org.eclipse.wst.html.SCRIPT.type.APPLICATION/DART", null);
    return targets;
  }
}
