/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.internal.text.functions.DartPresentationReconciler;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.IColorManager;
import com.google.dart.tools.ui.text.IColorManagerExtension;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic highlighting manager
 */
public class SemanticHighlightingManager implements IPropertyChangeListener {

  /**
   * Highlighted Positions.
   */
  public static class HighlightedPosition extends Position {

    /** Highlighting of the position */
    private Highlighting fStyle;

    /** Lock object */
    private Object fLock;

    /**
     * Initialize the styled positions with the given offset, length and foreground color.
     * 
     * @param offset The position offset
     * @param length The position length
     * @param highlighting The position's highlighting
     * @param lock The lock object
     */
    public HighlightedPosition(int offset, int length, Highlighting highlighting, Object lock) {
      super(offset, length);
      fStyle = highlighting;
      fLock = lock;
    }

    /**
     * @return Returns a corresponding style range.
     */
    public StyleRange createStyleRange() {
      int len = 0;
      if (fStyle.isEnabled()) {
        len = getLength();
      }

      TextAttribute textAttribute = fStyle.getTextAttribute();
      int style = textAttribute.getStyle();
      int fontStyle = style & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL);
      StyleRange styleRange = new StyleRange(
          getOffset(),
          len,
          textAttribute.getForeground(),
          textAttribute.getBackground(),
          fontStyle);
      styleRange.strikeout = (style & TextAttribute.STRIKETHROUGH) != 0;
      styleRange.underline = (style & TextAttribute.UNDERLINE) != 0;

      return styleRange;
    }

    @Override
    public void delete() {
      synchronized (fLock) {
        super.delete();
      }
    }

    /**
     * @return Returns the highlighting.
     */
    public Highlighting getHighlighting() {
      return fStyle;
    }

    /**
     * Is this position contained in the given range (inclusive)? Synchronizes on position updater.
     * 
     * @param off The range offset
     * @param len The range length
     * @return <code>true</code> iff this position is not delete and contained in the given range.
     */
    public boolean isContained(int off, int len) {
      synchronized (fLock) {
        return !isDeleted() && off <= getOffset() && off + len >= getOffset() + getLength();
      }
    }

    /**
     * Uses reference equality for the highlighting.
     * 
     * @param off The offset
     * @param len The length
     * @param highlighting The highlighting
     * @return <code>true</code> iff the given offset, length and highlighting are equal to the
     *         internal ones.
     */
    public boolean isEqual(int off, int len, Highlighting highlighting) {
      synchronized (fLock) {
        return !isDeleted() && getOffset() == off && getLength() == len && fStyle == highlighting;
      }
    }

    @Override
    public void setLength(int length) {
      synchronized (fLock) {
        super.setLength(length);
      }
    }

    @Override
    public void setOffset(int offset) {
      synchronized (fLock) {
        super.setOffset(offset);
      }
    }

    @Override
    public void undelete() {
      synchronized (fLock) {
        super.undelete();
      }
    }

    public void update(int off, int len) {
      synchronized (fLock) {
        super.setOffset(off);
        super.setLength(len);
      }
    }
  }

  /**
   * Highlighted ranges.
   */
  public static class HighlightedRange extends Region {
    /**
     * The highlighting key as returned by {@link SemanticHighlighting#getPreferenceKey()}.
     */
    private String fKey;

    /**
     * Initialize with the given offset, length and highlighting key.
     * 
     * @param offset
     * @param length
     * @param key the highlighting key as returned by
     *          {@link SemanticHighlighting#getPreferenceKey()}
     */
    public HighlightedRange(int offset, int length, String key) {
      super(offset, length);
      fKey = key;
    }

    @Override
    public boolean equals(Object o) {
      return super.equals(o) && o instanceof HighlightedRange
          && fKey.equals(((HighlightedRange) o).getKey());
    }

    /**
     * @return the highlighting key as returned by {@link SemanticHighlighting#getPreferenceKey()}
     */
    public String getKey() {
      return fKey;
    }

    @Override
    public int hashCode() {
      return super.hashCode() | fKey.hashCode();
    }
  }

  /**
   * Highlighting.
   */
  public static class Highlighting { // TODO: rename to HighlightingStyle

    /** Text attribute */
    private TextAttribute fTextAttribute;
    /** Enabled state */
    private boolean fIsEnabled;

    /**
     * Initialize with the given text attribute.
     * 
     * @param textAttribute The text attribute
     * @param isEnabled the enabled state
     */
    public Highlighting(TextAttribute textAttribute, boolean isEnabled) {
      setTextAttribute(textAttribute);
      setEnabled(isEnabled);
    }

    /**
     * @return Returns the text attribute.
     */
    public TextAttribute getTextAttribute() {
      return fTextAttribute;
    }

    /**
     * @return the enabled state
     */
    public boolean isEnabled() {
      return fIsEnabled;
    }

    /**
     * @param isEnabled the new enabled state
     */
    public void setEnabled(boolean isEnabled) {
      fIsEnabled = isEnabled;
    }

    /**
     * @param textAttribute The background to set.
     */
    public void setTextAttribute(TextAttribute textAttribute) {
      fTextAttribute = textAttribute;
    }
  }

  /** Semantic highlighting presenter */
  private SemanticHighlightingPresenter fPresenter;
  /** Semantic highlighting reconciler */
  private SemanticHighlightingReconciler fReconciler;

  /** Semantic highlightings */
  private SemanticHighlighting[] fSemanticHighlightings;
  /** Highlightings */
  private Highlighting[] fHighlightings;

  /** The editor */
  private DartEditor fEditor;
  /** The source viewer */
  private DartSourceViewer fSourceViewer;
  /** The color manager */
  private IColorManager fColorManager;
  /** The preference store */
  private IPreferenceStore fPreferenceStore;
  /** The source viewer configuration */
  private DartSourceViewerConfiguration fConfiguration;
  /** The presentation reconciler */
  private DartPresentationReconciler fPresentationReconciler;

  /** The hard-coded ranges */
  private HighlightedRange[][] fHardcodedRanges;

  /**
   * Returns this hightlighter's reconciler.
   * 
   * @return the semantic highlighter reconciler or <code>null</code> if none
   */
  public SemanticHighlightingReconciler getReconciler() {
    return fReconciler;
  }

  /**
   * Install the semantic highlighting on the given editor infrastructure
   * 
   * @param editor The Java editor
   * @param sourceViewer The source viewer
   * @param colorManager The color manager
   * @param preferenceStore The preference store
   */
  public void install(DartEditor editor, DartSourceViewer sourceViewer, IColorManager colorManager,
      IPreferenceStore preferenceStore) {
    fEditor = editor;
    fSourceViewer = sourceViewer;
    fColorManager = colorManager;
    fPreferenceStore = preferenceStore;
    if (fEditor != null) {
      fConfiguration = editor.createDartSourceViewerConfiguration();
      fPresentationReconciler = (DartPresentationReconciler) fConfiguration.getPresentationReconciler(sourceViewer);
    } else {
      fConfiguration = null;
      fPresentationReconciler = null;
    }

    fPreferenceStore.addPropertyChangeListener(this);

    if (isEnabled()) {
      enable();
    }
  }

  /**
   * Install the semantic highlighting on the given source viewer infrastructure. No reconciliation
   * will be performed.
   * 
   * @param sourceViewer the source viewer
   * @param colorManager the color manager
   * @param preferenceStore the preference store
   * @param hardcodedRanges the hard-coded ranges to be highlighted
   */
  public void install(DartSourceViewer sourceViewer, IColorManager colorManager,
      IPreferenceStore preferenceStore, HighlightedRange[][] hardcodedRanges) {
    fHardcodedRanges = hardcodedRanges;
    install(null, sourceViewer, colorManager, preferenceStore);
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    handlePropertyChangeEvent(event);
  }

  /**
   * Uninstall the semantic highlighting
   */
  public void uninstall() {
    disable();

    if (fPreferenceStore != null) {
      fPreferenceStore.removePropertyChangeListener(this);
      fPreferenceStore = null;
    }

    fEditor = null;
    fSourceViewer = null;
    fColorManager = null;
    fConfiguration = null;
    fPresentationReconciler = null;
    fHardcodedRanges = null;
  }

  private void adaptToEnablementChange(Highlighting highlighting, PropertyChangeEvent event) {
    Object value = event.getNewValue();
    boolean eventValue;
    if (value instanceof Boolean) {
      eventValue = ((Boolean) value).booleanValue();
    } else if (IPreferenceStore.TRUE.equals(value)) {
      eventValue = true;
    } else {
      eventValue = false;
    }
    highlighting.setEnabled(eventValue);
  }

  private void adaptToTextForegroundChange(Highlighting highlighting, PropertyChangeEvent event) {
    RGB rgb = null;

    Object value = event.getNewValue();
    if (value instanceof RGB) {
      rgb = (RGB) value;
    } else if (value instanceof String) {
      rgb = StringConverter.asRGB((String) value);
    }

    if (rgb != null) {

      String property = event.getProperty();
      Color color = fColorManager.getColor(property);

      if ((color == null || !rgb.equals(color.getRGB()))
          && fColorManager instanceof IColorManagerExtension) {
        IColorManagerExtension ext = (IColorManagerExtension) fColorManager;
        ext.unbindColor(property);
        ext.bindColor(property, rgb);
        color = fColorManager.getColor(property);
      }

      TextAttribute oldAttr = highlighting.getTextAttribute();
      highlighting.setTextAttribute(new TextAttribute(
          color,
          oldAttr.getBackground(),
          oldAttr.getStyle()));
    }
  }

  private void adaptToTextStyleChange(Highlighting highlighting, PropertyChangeEvent event,
      int styleAttribute) {
    boolean eventValue = false;
    Object value = event.getNewValue();
    if (value instanceof Boolean) {
      eventValue = ((Boolean) value).booleanValue();
    } else if (IPreferenceStore.TRUE.equals(value)) {
      eventValue = true;
    }

    TextAttribute oldAttr = highlighting.getTextAttribute();
    boolean activeValue = (oldAttr.getStyle() & styleAttribute) == styleAttribute;

    if (activeValue != eventValue) {
      highlighting.setTextAttribute(new TextAttribute(
          oldAttr.getForeground(),
          oldAttr.getBackground(),
          eventValue ? oldAttr.getStyle() | styleAttribute : oldAttr.getStyle() & ~styleAttribute));
    }
  }

  private void addColor(String colorKey) {
    if (fColorManager != null && colorKey != null && fColorManager.getColor(colorKey) == null) {
      RGB rgb = PreferenceConverter.getColor(fPreferenceStore, colorKey);
      if (fColorManager instanceof IColorManagerExtension) {
        IColorManagerExtension ext = (IColorManagerExtension) fColorManager;
        ext.unbindColor(colorKey);
        ext.bindColor(colorKey, rgb);
      }
    }
  }

  /**
   * Computes the hard-coded positions from the hard-coded ranges
   * 
   * @return the hard-coded positions
   */
  private HighlightedPosition[] createHardcodedPositions() {
    List<HighlightedPosition> positions = new ArrayList<HighlightedPosition>();
    for (int i = 0; i < fHardcodedRanges.length; i++) {
      HighlightedRange range = null;
      Highlighting hl = null;
      for (int j = 0; j < fHardcodedRanges[i].length; j++) {
        hl = getHighlighting(fHardcodedRanges[i][j].getKey());
        if (hl.isEnabled()) {
          range = fHardcodedRanges[i][j];
          break;
        }
      }

      if (range != null) {
        positions.add(fPresenter.createHighlightedPosition(range.getOffset(), range.getLength(), hl));
      }
    }
    return positions.toArray(new HighlightedPosition[positions.size()]);
  }

  /**
   * Disable semantic highlighting.
   */
  private void disable() {
    if (fReconciler != null) {
      fReconciler.uninstall();
      fReconciler = null;
    }

    if (fPresenter != null) {
      fPresenter.uninstall();
      fPresenter = null;
    }

    if (fSemanticHighlightings != null) {
      disposeHighlightings();
    }
  }

  /**
   * Dispose the semantic highlightings.
   */
  private void disposeHighlightings() {
    for (int i = 0, n = fSemanticHighlightings.length; i < n; i++) {
      removeColor(SemanticHighlightings.getColorPreferenceKey(fSemanticHighlightings[i]));
    }

    fSemanticHighlightings = null;
    fHighlightings = null;
  }

  /**
   * Enable semantic highlighting.
   */
  private void enable() {
    initializeHighlightings();

    fPresenter = new SemanticHighlightingPresenter();
    fPresenter.install(fSourceViewer, fPresentationReconciler);

    if (fEditor != null) {
      fReconciler = new SemanticHighlightingReconciler();
      fReconciler.install(
          fEditor,
          fSourceViewer,
          fPresenter,
          fSemanticHighlightings,
          fHighlightings);
    } else {
      fPresenter.updatePresentation(null, createHardcodedPositions(), new HighlightedPosition[0]);
    }
  }

  /**
   * Returns the highlighting corresponding to the given key.
   * 
   * @param key the highlighting key as returned by {@link SemanticHighlighting#getPreferenceKey()}
   * @return the corresponding highlighting
   */
  private Highlighting getHighlighting(String key) {
    for (int i = 0; i < fSemanticHighlightings.length; i++) {
      SemanticHighlighting semanticHighlighting = fSemanticHighlightings[i];
      if (key.equals(semanticHighlighting.getPreferenceKey())) {
        return fHighlightings[i];
      }
    }
    return null;
  }

  /**
   * Handle the given property change event
   * 
   * @param event The event
   */
  private void handlePropertyChangeEvent(PropertyChangeEvent event) {
    if (fPreferenceStore == null) {
      return; // Uninstalled during event notification
    }

    if (fConfiguration != null) {
      fConfiguration.handlePropertyChangeEvent(event);
    }

    if (!isEnabled()) {
      return;
    }

    boolean refreshNeeded = false;

    for (int i = 0, n = fSemanticHighlightings.length; i < n; i++) {
      SemanticHighlighting semanticHighlighting = fSemanticHighlightings[i];

      String colorKey = SemanticHighlightings.getColorPreferenceKey(semanticHighlighting);
      if (colorKey.equals(event.getProperty())) {
        adaptToTextForegroundChange(fHighlightings[i], event);
        fPresenter.highlightingStyleChanged(fHighlightings[i]);
        refreshNeeded = true;
        continue;
      }

      String boldKey = SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting);
      if (boldKey.equals(event.getProperty())) {
        adaptToTextStyleChange(fHighlightings[i], event, SWT.BOLD);
        fPresenter.highlightingStyleChanged(fHighlightings[i]);
        refreshNeeded = true;
        continue;
      }

      String italicKey = SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting);
      if (italicKey.equals(event.getProperty())) {
        adaptToTextStyleChange(fHighlightings[i], event, SWT.ITALIC);
        fPresenter.highlightingStyleChanged(fHighlightings[i]);
        refreshNeeded = true;
        continue;
      }

      String strikethroughKey = SemanticHighlightings.getStrikethroughPreferenceKey(semanticHighlighting);
      if (strikethroughKey.equals(event.getProperty())) {
        adaptToTextStyleChange(fHighlightings[i], event, TextAttribute.STRIKETHROUGH);
        fPresenter.highlightingStyleChanged(fHighlightings[i]);
        refreshNeeded = true;
        continue;
      }

      String underlineKey = SemanticHighlightings.getUnderlinePreferenceKey(semanticHighlighting);
      if (underlineKey.equals(event.getProperty())) {
        adaptToTextStyleChange(fHighlightings[i], event, TextAttribute.UNDERLINE);
        fPresenter.highlightingStyleChanged(fHighlightings[i]);
        refreshNeeded = true;
        continue;
      }

      String enabledKey = SemanticHighlightings.getEnabledPreferenceKey(semanticHighlighting);
      if (enabledKey.equals(event.getProperty())) {
        adaptToEnablementChange(fHighlightings[i], event);
        fPresenter.highlightingStyleChanged(fHighlightings[i]);
        refreshNeeded = true;
        continue;
      }
    }

    if (refreshNeeded && fReconciler != null) {
      fReconciler.refresh();
    }
  }

  /**
   * Initialize semantic highlightings.
   */
  private void initializeHighlightings() {
    fSemanticHighlightings = SemanticHighlightings.getSemanticHighlightings();
    fHighlightings = new Highlighting[fSemanticHighlightings.length];

    for (int i = 0, n = fSemanticHighlightings.length; i < n; i++) {
      SemanticHighlighting semanticHighlighting = fSemanticHighlightings[i];
      String colorKey = SemanticHighlightings.getColorPreferenceKey(semanticHighlighting);
      addColor(colorKey);

      String boldKey = SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting);
      int style = fPreferenceStore.getBoolean(boldKey) ? SWT.BOLD : SWT.NORMAL;

      String italicKey = SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting);
      if (fPreferenceStore.getBoolean(italicKey)) {
        style |= SWT.ITALIC;
      }

      String strikethroughKey = SemanticHighlightings.getStrikethroughPreferenceKey(semanticHighlighting);
      if (fPreferenceStore.getBoolean(strikethroughKey)) {
        style |= TextAttribute.STRIKETHROUGH;
      }

      String underlineKey = SemanticHighlightings.getUnderlinePreferenceKey(semanticHighlighting);
      if (fPreferenceStore.getBoolean(underlineKey)) {
        style |= TextAttribute.UNDERLINE;
      }

      boolean isEnabled = fPreferenceStore.getBoolean(SemanticHighlightings.getEnabledPreferenceKey(semanticHighlighting));

      fHighlightings[i] = new Highlighting(new TextAttribute(
          fColorManager.getColor(PreferenceConverter.getColor(fPreferenceStore, colorKey)),
          null,
          style), isEnabled);
    }
  }

  /**
   * @return <code>true</code> iff semantic highlighting is enabled in the preferences
   */
  private boolean isEnabled() {
//  return SemanticHighlightings.isEnabled(fPreferenceStore);
    return true;
  }

  private void removeColor(String colorKey) {
    if (fColorManager instanceof IColorManagerExtension) {
      ((IColorManagerExtension) fColorManager).unbindColor(colorKey);
    }
  }
}
