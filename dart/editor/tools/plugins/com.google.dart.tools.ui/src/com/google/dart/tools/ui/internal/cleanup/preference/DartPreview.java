/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.editor.DartSourceViewer;
import com.google.dart.tools.ui.internal.text.functions.SimpleDartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartTextTools;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.MarginPainter;
import org.eclipse.jface.text.WhitespaceCharacterPainter;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import java.util.Map;

public abstract class DartPreview {

  private final class JavaSourcePreviewerUpdater {

    final IPropertyChangeListener fontListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(PreferenceConstants.EDITOR_TEXT_FONT)) {
          final Font font = JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
          fSourceViewer.getTextWidget().setFont(font);
          if (fMarginPainter != null) {
            fMarginPainter.initialize();
          }
        }
      }
    };

    final IPropertyChangeListener propertyListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (fViewerConfiguration.affectsTextPresentation(event)) {
          fViewerConfiguration.handlePropertyChangeEvent(event);
          fSourceViewer.invalidateTextPresentation();
        }
      }
    };

    public JavaSourcePreviewerUpdater() {

      JFaceResources.getFontRegistry().addListener(fontListener);
      fPreferenceStore.addPropertyChangeListener(propertyListener);

      fSourceViewer.getTextWidget().addDisposeListener(new DisposeListener() {
        @Override
        public void widgetDisposed(DisposeEvent e) {
          JFaceResources.getFontRegistry().removeListener(fontListener);
          fPreferenceStore.removePropertyChangeListener(propertyListener);
        }
      });
    }
  }

  private static int getPositiveIntValue(String string, int defaultValue) {
    try {
      int i = Integer.parseInt(string);
      if (i >= 0) {
        return i;
      }
    } catch (NumberFormatException e) {
    }
    return defaultValue;
  }

  protected final SimpleDartSourceViewerConfiguration fViewerConfiguration;
  protected final Document fPreviewDocument;
  protected final SourceViewer fSourceViewer;

  protected final IPreferenceStore fPreferenceStore;

  protected final MarginPainter fMarginPainter;

  protected Map<String, String> fWorkingValues;
  private int fTabSize = 0;

  private WhitespaceCharacterPainter fWhitespaceCharacterPainter;

  public DartPreview(Map<String, String> workingValues, Composite parent) {
    DartTextTools tools = DartToolsPlugin.getDefault().getDartTextTools();
    fPreviewDocument = new Document();
    fWorkingValues = workingValues;
    tools.setupDartDocumentPartitioner(fPreviewDocument, DartPartitions.DART_PARTITIONING);

    PreferenceStore prioritizedSettings = new PreferenceStore();
//    prioritizedSettings.setValue(DartCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
//    prioritizedSettings.setValue(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
//    prioritizedSettings.setValue(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
//    prioritizedSettings.setValue(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);

    IPreferenceStore[] chain = {
        prioritizedSettings, DartToolsPlugin.getDefault().getCombinedPreferenceStore()};
    fPreferenceStore = new ChainedPreferenceStore(chain);
    fSourceViewer = new DartSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL
        | SWT.BORDER, fPreferenceStore);
    fSourceViewer.setEditable(false);
    Cursor arrowCursor = fSourceViewer.getTextWidget().getDisplay().getSystemCursor(
        SWT.CURSOR_ARROW);
    fSourceViewer.getTextWidget().setCursor(arrowCursor);

    // Don't set caret to 'null' as this causes https://bugs.eclipse.org/293263
//		fSourceViewer.getTextWidget().setCaret(null);

    fViewerConfiguration = new SimpleDartSourceViewerConfiguration(
        tools.getColorManager(),
        fPreferenceStore,
        null,
        DartPartitions.DART_PARTITIONING,
        true);
    fSourceViewer.configure(fViewerConfiguration);
    fSourceViewer.getTextWidget().setFont(
        JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));

    fMarginPainter = new MarginPainter(fSourceViewer);
    final RGB rgb = PreferenceConverter.getColor(
        fPreferenceStore,
        AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR);
    fMarginPainter.setMarginRulerColor(tools.getColorManager().getColor(rgb));
    fSourceViewer.addPainter(fMarginPainter);

    new JavaSourcePreviewerUpdater();
    fSourceViewer.setDocument(fPreviewDocument);
  }

  public Control getControl() {
    return fSourceViewer.getControl();
  }

  public Map<String, String> getWorkingValues() {
    return fWorkingValues;
  }

  public void setWorkingValues(Map<String, String> workingValues) {
    fWorkingValues = workingValues;
  }

  public void showInvisibleCharacters(boolean enable) {
    if (enable) {
      if (fWhitespaceCharacterPainter == null) {
        fWhitespaceCharacterPainter = new WhitespaceCharacterPainter(fSourceViewer);
        fSourceViewer.addPainter(fWhitespaceCharacterPainter);
      }
    } else {
      fSourceViewer.removePainter(fWhitespaceCharacterPainter);
      fWhitespaceCharacterPainter = null;
    }
  }

  public void update() {
    if (fWorkingValues == null) {
      fPreviewDocument.set(""); //$NON-NLS-1$
      return;
    }

    // update the print margin
    final String value = fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT);
    final int lineWidth = getPositiveIntValue(value, 0);
    fMarginPainter.setMarginRulerColumn(lineWidth);

    // update the tab size
    final int tabSize = getPositiveIntValue(
        fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE),
        0);
    if (tabSize != fTabSize) {
      fSourceViewer.getTextWidget().setTabs(tabSize);
    }
    fTabSize = tabSize;

    final StyledText widget = (StyledText) fSourceViewer.getControl();
    final int height = widget.getClientArea().height;
    final int top0 = widget.getTopPixel();

    final int totalPixels0 = getHeightOfAllLines(widget);
    final int topPixelRange0 = totalPixels0 > height ? totalPixels0 - height : 0;

    widget.setRedraw(false);
    doFormatPreview();
    fSourceViewer.setSelection(null);

    final int totalPixels1 = getHeightOfAllLines(widget);
    final int topPixelRange1 = totalPixels1 > height ? totalPixels1 - height : 0;

    final int top1 = topPixelRange0 > 0 ? (int) (topPixelRange1 * top0 / (double) topPixelRange0)
        : 0;
    widget.setTopPixel(top1);
    widget.setRedraw(true);
  }

  protected abstract void doFormatPreview();

  private int getHeightOfAllLines(StyledText styledText) {
    int height = 0;
    int lineCount = styledText.getLineCount();
    for (int i = 0; i < lineCount; i++) {
      height = height + styledText.getLineHeight(styledText.getOffsetAtLine(i));
    }
    return height;
  }
}
