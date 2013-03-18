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
package com.google.dart.tools.ui.web.yaml;

import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRulerExtension;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * An editor used for the source tab in Pubspec forms editor
 */
public class PubspecYamlEditor extends YamlEditor {

  class FontPropertyChangeListener implements IPropertyChangeListener {

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (getSourceViewer() == null) {
        return;
      }

      String property = event.getProperty();

      if (PreferenceConstants.EDITOR_TEXT_FONT.equals(property)) {
        initializeViewerFont(getSourceViewer());
        updateCaret();
        return;
      }
    }
  }

  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();

  private static final int WIDE_CARET_WIDTH = 2;

  private static final int SINGLE_CARET_WIDTH = 1;

  private Caret nonDefaultCaret;
  private Image nonDefaultCaretImage;

  private Caret initialCaret;

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    initialCaret = getSourceViewer().getTextWidget().getCaret();
    initializeViewerFont(getSourceViewer());
    updateCaret();
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
    }
    fontPropertyChangeListener = null;
    initialCaret = null;
  }

  @Override
  protected void handleInsertModeChanged() {
    updateInsertModeAction();
    updateCaret();
    updateStatusField(ITextEditorActionConstants.STATUS_CATEGORY_INPUT_MODE);
  }

  @Override
  protected boolean isPubspecEditor() {
    IEditorInput input = getEditorInput();

    if (input != null) {
      return "pubspec.yaml".equals(input.getName());
    } else {
      return false;
    }
  }

  private Caret createInsertCaret(StyledText styledText) {
    Caret caret = new Caret(styledText, SWT.NULL);
    caret.setSize(getCaretWidthPreference(), styledText.getLineHeight());
    caret.setFont(styledText.getFont());
    return caret;
  }

  private Caret createOverwriteCaret(StyledText styledText) {
    Caret caret = new Caret(styledText, SWT.NULL);
    GC gc = new GC(styledText);
    Point charSize = gc.stringExtent("a"); //$NON-NLS-1$
    caret.setSize(charSize.x, styledText.getLineHeight());
    caret.setFont(styledText.getFont());
    gc.dispose();
    return caret;
  }

  private Caret createRawInsertModeCaret(StyledText styledText) {
    if (!getLegalInsertModes().contains(SMART_INSERT)) {
      return createInsertCaret(styledText);
    }

    Caret caret = new Caret(styledText, SWT.NULL);
    Image image = createRawInsertModeCaretImage(styledText);
    if (image != null) {
      caret.setImage(image);
    } else {
      caret.setSize(getCaretWidthPreference(), styledText.getLineHeight());
    }
    caret.setFont(styledText.getFont());
    return caret;
  }

  private Image createRawInsertModeCaretImage(StyledText styledText) {
    PaletteData caretPalette = new PaletteData(new RGB[] {new RGB(0, 0, 0), new RGB(255, 255, 255)});
    int width = getCaretWidthPreference();
    int widthOffset = width - 1;
    ImageData imageData = new ImageData(
        4 + widthOffset,
        styledText.getLineHeight(),
        1,
        caretPalette);

    Display display = styledText.getDisplay();
    Image bracketImage = new Image(display, imageData);
    GC gc = new GC(bracketImage);
    gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
    gc.setLineWidth(0); // NOTE: 0 means width is 1 but with optimized performance
    int height = imageData.height / 3;
    for (int i = 0; i < width; i++) {
      gc.drawLine(i, 0, i, height - 1);
      gc.drawLine(i, imageData.height - height, i, imageData.height - 1);
    }
    gc.dispose();
    return bracketImage;
  }

  private void disposeNonDefaultCaret() {
    if (nonDefaultCaretImage != null) {
      nonDefaultCaretImage.dispose();
      nonDefaultCaretImage = null;
    }
    if (nonDefaultCaret != null) {
      nonDefaultCaret.dispose();
      nonDefaultCaret = null;
    }

  }

  private int getCaretWidthPreference() {
    if (getPreferenceStore() != null && getPreferenceStore().getBoolean(PREFERENCE_WIDE_CARET)) {
      return WIDE_CARET_WIDTH;
    }
    return SINGLE_CARET_WIDTH;
  }

  private void initializeViewerFont(ISourceViewer viewer) {
    Font font = null;

    String symbolicFontName = PreferenceConstants.EDITOR_TEXT_FONT;
    font = JFaceResources.getFont(symbolicFontName);
    if (font == null) {
      font = JFaceResources.getTextFont();
    }
    if (!font.equals(getSourceViewer().getTextWidget().getFont())) {
      setFont(viewer, font);
    } else {
      font.dispose();
    }
  }

  private void setFont(ISourceViewer sourceViewer, Font font) {

    if (sourceViewer.getDocument() != null) {
      ISelectionProvider provider = sourceViewer.getSelectionProvider();
      ISelection selection = provider.getSelection();
      int topIndex = sourceViewer.getTopIndex();

      StyledText styledText = sourceViewer.getTextWidget();
      Control parent = styledText;
      if (sourceViewer instanceof ITextViewerExtension) {
        ITextViewerExtension extension = (ITextViewerExtension) sourceViewer;
        parent = extension.getControl();
      }
      parent.setRedraw(false);
      styledText.setFont(font);

      if (getVerticalRuler() instanceof IVerticalRulerExtension) {
        IVerticalRulerExtension e = (IVerticalRulerExtension) getVerticalRuler();
        e.setFont(font);
      }

      provider.setSelection(selection);
      sourceViewer.setTopIndex(topIndex);

      if (parent instanceof Composite) {
        Composite composite = (Composite) parent;
        composite.layout(true);
      }
      parent.setRedraw(true);
    } else {
      StyledText styledText = sourceViewer.getTextWidget();
      styledText.setFont(font);
      if (getVerticalRuler() instanceof IVerticalRulerExtension) {
        IVerticalRulerExtension e = (IVerticalRulerExtension) getVerticalRuler();
        e.setFont(font);
      }
    }
  }

  private void updateCaret() {

    if (getSourceViewer() == null) {
      return;
    }

    StyledText styledText = getSourceViewer().getTextWidget();
    InsertMode mode = getInsertMode();
    styledText.setCaret(null);
    disposeNonDefaultCaret();

    if (!isInInsertMode()) {
      nonDefaultCaret = createOverwriteCaret(styledText);
    } else if (SMART_INSERT == mode) {
      nonDefaultCaret = createInsertCaret(styledText);
    } else if (INSERT == mode) {
      nonDefaultCaret = createRawInsertModeCaret(styledText);
    }

    if (nonDefaultCaret != null) {
      styledText.setCaret(nonDefaultCaret);
      nonDefaultCaretImage = nonDefaultCaret.getImage();
    } else if (initialCaret != styledText.getCaret()) {
      styledText.setCaret(initialCaret);
    }
  }

  private void updateInsertModeAction() {
    if (getSite() == null) {
      return;
    }

    IAction action = getAction(ITextEditorActionConstants.TOGGLE_INSERT_MODE);
    if (action != null) {
      action.setEnabled(isInInsertMode());
      action.setChecked(getInsertMode() == SMART_INSERT);
    }
  }

}
