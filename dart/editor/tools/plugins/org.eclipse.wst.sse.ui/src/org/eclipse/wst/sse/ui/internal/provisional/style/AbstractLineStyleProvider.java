/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.style;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionCollection;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.util.Debug;
import org.eclipse.wst.sse.ui.internal.preferences.ui.ColorHelper;
import org.eclipse.wst.sse.ui.internal.util.EditorUtility;

import java.util.Collection;
import java.util.HashMap;

public abstract class AbstractLineStyleProvider {
  private class PropertyChangeListener implements IPropertyChangeListener {
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.
     * PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
      // have to do it this way so others can override the method
      handlePropertyChange(event);
    }
  }

  protected IStructuredDocument fDocument;
  protected Highlighter fHighlighter;
  private boolean fInitialized;
  protected PropertyChangeListener fPreferenceListener = new PropertyChangeListener();

  //private ISourceViewer fSourceViewer = null;
  protected ReconcilerHighlighter fRecHighlighter = null;

  /** Contains all text attributes pertaining to this line style provider */
  private HashMap fTextAttributes = null;

  // we keep track of LogMessage to avoid writing hundreds of messages,
  // but still give a hint that something is wrong with attributeProviders
  // and/or regions.
  // It's only written in the case of a program error, but there's no use
  // adding
  // salt to the wound.
  // private boolean wroteOneLogMessage;
  /**
	 */
  protected AbstractLineStyleProvider() {
  }

  /**
   * Looks up the colorKey in the preference store and adds the style information to list of
   * TextAttributes
   * 
   * @param colorKey
   */
  protected void addTextAttribute(String colorKey) {
    if (getColorPreferences() != null) {
      String prefString = getColorPreferences().getString(colorKey);
      String[] stylePrefs = ColorHelper.unpackStylePreferences(prefString);
      if (stylePrefs != null) {
        RGB foreground = ColorHelper.toRGB(stylePrefs[0]);
        RGB background = ColorHelper.toRGB(stylePrefs[1]);
        boolean bold = Boolean.valueOf(stylePrefs[2]).booleanValue();
        boolean italic = Boolean.valueOf(stylePrefs[3]).booleanValue();
        boolean strikethrough = Boolean.valueOf(stylePrefs[4]).booleanValue();
        boolean underline = Boolean.valueOf(stylePrefs[5]).booleanValue();
        int style = SWT.NORMAL;
        if (bold) {
          style = style | SWT.BOLD;
        }
        if (italic) {
          style = style | SWT.ITALIC;
        }
        if (strikethrough) {
          style = style | TextAttribute.STRIKETHROUGH;
        }
        if (underline) {
          style = style | TextAttribute.UNDERLINE;
        }

        TextAttribute createTextAttribute = createTextAttribute(foreground, background, style);
        getTextAttributes().put(colorKey, createTextAttribute);
      }
    }
  }

  protected void commonInit(IStructuredDocument document, Highlighter highlighter) {

    fDocument = document;
    fHighlighter = highlighter;
  }

  /**
   * this version does "trim" regions to match request
   */
  private StyleRange createStyleRange(ITextRegionCollection flatNode, ITextRegion region,
      TextAttribute attr, int startOffset, int length) {
    int start = flatNode.getStartOffset(region);
    if (start < startOffset)
      start = startOffset;

    // Base the text end offset off of the, possibly adjusted, start
    int textEnd = start + region.getTextLength();
    int maxOffset = startOffset + length;

    int end = flatNode.getEndOffset(region);
    // Use the end of the text in the region to avoid applying background color to trailing whitespace
    if (textEnd < end)
      end = textEnd;
    // instead of end-start?
    if (end > maxOffset)
      end = maxOffset;
    StyleRange result = new StyleRange(start, end - start, attr.getForeground(),
        attr.getBackground(), attr.getStyle());
    if ((attr.getStyle() & TextAttribute.STRIKETHROUGH) != 0) {
      result.strikeout = true;
    }
    if ((attr.getStyle() & TextAttribute.UNDERLINE) != 0) {
      result.underline = true;
    }
    return result;

  }

  protected TextAttribute createTextAttribute(RGB foreground, RGB background, boolean bold) {
    return new TextAttribute((foreground != null) ? EditorUtility.getColor(foreground) : null,
        (background != null) ? EditorUtility.getColor(background) : null, bold ? SWT.BOLD
            : SWT.NORMAL);
  }

  protected TextAttribute createTextAttribute(RGB foreground, RGB background, int style) {
    return new TextAttribute((foreground != null) ? EditorUtility.getColor(foreground) : null,
        (background != null) ? EditorUtility.getColor(background) : null, style);
  }

  abstract protected TextAttribute getAttributeFor(ITextRegion region);

  protected TextAttribute getAttributeFor(ITextRegionCollection collection, ITextRegion region) {
    return getAttributeFor(region);
  }

  abstract protected IPreferenceStore getColorPreferences();

  protected IStructuredDocument getDocument() {
    return fDocument;
  }

  public void setDocument(IStructuredDocument document) {
    fDocument = document;
  }

  /**
	 */
  protected Highlighter getHighlighter() {
    return fHighlighter;
  }

  /**
   * Returns the hashtable containing all the text attributes for this line style provider. Lazily
   * creates a hashtable if one has not already been created.
   * 
   * @return
   */
  protected HashMap getTextAttributes() {
    if (fTextAttributes == null) {
      fTextAttributes = new HashMap();
      loadColors();
    }
    return fTextAttributes;
  }

  protected void handlePropertyChange(PropertyChangeEvent event) {
    // force a full update of the text viewer
    if (fRecHighlighter != null)
      fRecHighlighter.refreshDisplay();
  }

  public void init(IStructuredDocument structuredDocument, Highlighter highlighter) {

    commonInit(structuredDocument, highlighter);

    if (isInitialized())
      return;

    registerPreferenceManager();

    setInitialized(true);
  }

  public void init(IStructuredDocument structuredDocument, ISourceViewer sourceViewer) {
    init(structuredDocument, (Highlighter) null);
  }

  public void init(IStructuredDocument structuredDocument, ReconcilerHighlighter highlighter) {
    fDocument = structuredDocument;
    fRecHighlighter = highlighter;

    if (isInitialized())
      return;

    registerPreferenceManager();

    setInitialized(true);
  }

  /**
   * @deprecated - left because it's public, but we aren't adapters any more
   */
  public boolean isAdapterForType(java.lang.Object type) {
    return type == LineStyleProvider.class;
  }

  /**
   * Returns the initialized.
   * 
   * @return boolean
   */
  public boolean isInitialized() {
    return fInitialized;
  }

  abstract protected void loadColors();

  public boolean prepareRegions(ITypedRegion typedRegion, int lineRequestStart,
      int lineRequestLength, Collection holdResults) {
    final int partitionStartOffset = typedRegion.getOffset();
    final int partitionLength = typedRegion.getLength();
    IStructuredDocumentRegion structuredDocumentRegion = getDocument().getRegionAtCharacterOffset(
        partitionStartOffset);
    boolean handled = false;

    handled = prepareTextRegions(structuredDocumentRegion, partitionStartOffset, partitionLength,
        holdResults);

    return handled;
  }

  /**
   * @param region
   * @param start
   * @param length
   * @param holdResults
   * @return
   */
  private boolean prepareTextRegion(ITextRegionCollection blockedRegion, int partitionStartOffset,
      int partitionLength, Collection holdResults) {
    boolean handled = false;
    final int partitionEndOffset = partitionStartOffset + partitionLength - 1;
    ITextRegion region = null;
    ITextRegionList regions = blockedRegion.getRegions();
    int nRegions = regions.size();
    StyleRange styleRange = null;
    for (int i = 0; i < nRegions; i++) {
      region = regions.get(i);
      TextAttribute attr = null;
      TextAttribute previousAttr = null;
      if (blockedRegion.getStartOffset(region) > partitionEndOffset)
        break;
      if (blockedRegion.getEndOffset(region) <= partitionStartOffset)
        continue;

      if (region instanceof ITextRegionCollection) {
        handled = prepareTextRegion((ITextRegionCollection) region, partitionStartOffset,
            partitionLength, holdResults);
      } else {

        attr = getAttributeFor(blockedRegion, region);
        if (attr != null) {
          handled = true;
          // if this region's attr is the same as previous one, then
          // just adjust the previous style range
          // instead of creating a new instance of one
          // note: to use 'equals' in this case is important, since
          // sometimes
          // different instances of attributes are associated with a
          // region, even the
          // the attribute has the same values.
          // TODO: this needs to be improved to handle readonly
          // regions correctly
          if ((styleRange != null) && (previousAttr != null) && (previousAttr.equals(attr))) {
            styleRange.length += region.getLength();
          } else {
            styleRange = createStyleRange(blockedRegion, region, attr, partitionStartOffset,
                partitionLength);
            holdResults.add(styleRange);
            // technically speaking, we don't need to update
            // previousAttr
            // in the other case, because the other case is when
            // it hasn't changed
            previousAttr = attr;
          }
        } else {
          previousAttr = null;
        }
      }
    }
    return handled;
  }

  private boolean prepareTextRegions(IStructuredDocumentRegion structuredDocumentRegion,
      int partitionStartOffset, int partitionLength, Collection holdResults) {
    boolean handled = false;
    final int partitionEndOffset = partitionStartOffset + partitionLength - 1;
    while (structuredDocumentRegion != null
        && structuredDocumentRegion.getStartOffset() <= partitionEndOffset) {
      ITextRegion region = null;
      ITextRegionList regions = structuredDocumentRegion.getRegions();
      int nRegions = regions.size();
      StyleRange styleRange = null;
      for (int i = 0; i < nRegions; i++) {
        region = regions.get(i);
        TextAttribute attr = null;
        TextAttribute previousAttr = null;
        if (structuredDocumentRegion.getStartOffset(region) > partitionEndOffset)
          break;
        if (structuredDocumentRegion.getEndOffset(region) <= partitionStartOffset)
          continue;

        if (region instanceof ITextRegionCollection) {
          boolean handledCollection = (prepareTextRegion((ITextRegionCollection) region,
              partitionStartOffset, partitionLength, holdResults));
          handled = (!handled) ? handledCollection : handled;
        } else {

          attr = getAttributeFor(structuredDocumentRegion, region);
          if (attr != null) {
            handled = true;
            // if this region's attr is the same as previous one,
            // then just adjust the previous style range
            // instead of creating a new instance of one
            // note: to use 'equals' in this case is important,
            // since sometimes
            // different instances of attributes are associated
            // with a region, even the
            // the attribute has the same values.
            // TODO: this needs to be improved to handle readonly
            // regions correctly
            if ((styleRange != null) && (previousAttr != null) && (previousAttr.equals(attr))) {
              styleRange.length += region.getLength();
            } else {
              styleRange = createStyleRange(structuredDocumentRegion, region, attr,
                  partitionStartOffset, partitionLength);
              holdResults.add(styleRange);
              // technically speaking, we don't need to update
              // previousAttr
              // in the other case, because the other case is
              // when it hasn't changed
              previousAttr = attr;
            }
          } else {
            previousAttr = null;
          }
        }

        if (Debug.syntaxHighlighting && !handled) {
          System.out.println("not handled in prepareRegions"); //$NON-NLS-1$
        }
      }
      structuredDocumentRegion = structuredDocumentRegion.getNext();
    }
    return handled;
  }

  protected void registerPreferenceManager() {
    IPreferenceStore pref = getColorPreferences();
    if (pref != null) {
      pref.addPropertyChangeListener(fPreferenceListener);
    }
  }

  public void release() {
    unRegisterPreferenceManager();
    if (fTextAttributes != null) {
      fTextAttributes.clear();
      fTextAttributes = null;
    }
    setInitialized(false);
  }

  /**
   * Sets the initialized.
   * 
   * @param initialized The initialized to set
   */
  private void setInitialized(boolean initialized) {
    this.fInitialized = initialized;
  }

  protected void unRegisterPreferenceManager() {
    IPreferenceStore pref = getColorPreferences();
    if (pref != null) {
      pref.removePropertyChangeListener(fPreferenceListener);
    }
  }
}
