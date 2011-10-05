/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.IColorManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A simple
 * {@linkplain com.google.dart.tools.ui.text.DartSourceViewerConfiguration.JavaScriptSourceViewerConfiguration
 * Java source viewer configuration}.
 * <p>
 * This simple source viewer configuration basically provides syntax coloring and disables all other
 * features like code assist, quick outlines, hyperlinking, etc.
 * </p>
 */
public class SimpleDartSourceViewerConfiguration extends DartSourceViewerConfiguration {

  private boolean fConfigureFormatter;

  /**
   * Creates a new Java source viewer configuration for viewers in the given editor using the given
   * preference store, the color manager and the specified document partitioning.
   * 
   * @param colorManager the color manager
   * @param preferenceStore the preference store, can be read-only
   * @param editor the editor in which the configured viewer(s) will reside, or <code>null</code> if
   *          none
   * @param partitioning the document partitioning for this configuration, or <code>null</code> for
   *          the default partitioning
   * @param configureFormatter <code>true</code> if a content formatter should be configured
   */
  public SimpleDartSourceViewerConfiguration(IColorManager colorManager,
      IPreferenceStore preferenceStore, ITextEditor editor, String partitioning,
      boolean configureFormatter) {
    super(colorManager, preferenceStore, editor, partitioning);
    fConfigureFormatter = configureFormatter;
  }

  /*
   * @see SourceViewerConfiguration#getAnnotationHover(ISourceViewer)
   */
  @Override
  public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
    return null;
  }

  /*
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies
   * (org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
   */
  @Override
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    return null;
  }

  /*
   * @see SourceViewerConfiguration#getConfiguredTextHoverStateMasks(ISourceViewer, String)
   */
  @Override
  public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
    return null;
  }

  /*
   * @see SourceViewerConfiguration#getContentFormatter(ISourceViewer)
   */
  @Override
  public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
    if (fConfigureFormatter) {
      return super.getContentFormatter(sourceViewer);
    } else {
      return null;
    }
  }

  /*
   * @see com.google.dart.tools.ui.config.JavaScriptSourceViewerConfiguration#
   * getHierarchyPresenter(org.eclipse.jface.text.source.ISourceViewer, boolean)
   */
  @Override
  public IInformationPresenter getHierarchyPresenter(ISourceViewer sourceViewer,
      boolean doCodeResolve) {
    return null;
  }

  /*
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getHyperlinkDetectors
   * (org.eclipse.jface.text.source.ISourceViewer)
   */
  @Override
  public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
    return null;
  }

  /*
   * @see SourceViewerConfiguration#getInformationControlCreator(ISourceViewer)
   */
  @Override
  public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
    return null;
  }

  /*
   * @see SourceViewerConfiguration#getInformationPresenter(ISourceViewer)
   */
  @Override
  public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
    return null;
  }

  /*
   * @see com.google.dart.tools.ui.config.JavaScriptSourceViewerConfiguration#
   * getOutlinePresenter(org.eclipse.jface.text.source.ISourceViewer, boolean)
   */
  @Override
  public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
    return null;
  }

  /*
   * @see SourceViewerConfiguration#getOverviewRulerAnnotationHover(ISourceViewer)
   */
  @Override
  public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
    return null;
  }

  /*
   * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String)
   */
  @Override
  public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
    return null;
  }

  /*
   * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String, int)
   */
  @Override
  public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
    return null;
  }
}
