/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.typing.AbstractCharacterPairInserter;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.preferences.XMLUIPreferenceNames;

public class CharacterPairInserter extends AbstractCharacterPairInserter implements
    IPropertyChangeListener {

  // preferences
  private boolean fCloseStrings = true;
  private boolean fCloseBrackets = true;

  protected boolean shouldPair(ISourceViewer viewer, char c) {
    switch (c) {
      case '\'':
      case '"':
        return fCloseStrings ? checkRegion(viewer, c) : false;
      default:
        return fCloseBrackets;
    }
  }

  /**
   * Checks if the region should support paired quotes
   * 
   * @param viewer the viewer
   * @return true if the region is not in an XML attribute value
   */
  private boolean checkRegion(ISourceViewer viewer, char c) {
    IDocument doc = viewer.getDocument();
    final Point selection = viewer.getSelectedRange();
    final int offset = selection.x;

    if (doc instanceof IStructuredDocument) {
      IStructuredDocumentRegion[] regions = ((IStructuredDocument) doc).getStructuredDocumentRegions(
          offset, 0);
      if (regions != null && regions.length > 0) {
        ITextRegion region = regions[0].getRegionAtCharacterOffset(offset);
        if (region != null) {
          final String type = region.getType();
          if (DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS.equals(type))
            return true;
          else if (DOMRegionContext.XML_TAG_CLOSE.equals(type)
              || DOMRegionContext.XML_EMPTY_TAG_CLOSE.equals(type)) {
            if (regions[0].containsOffset(offset - 1)) {
              region = regions[0].getRegionAtCharacterOffset(offset - 1);
              if (region != null
                  && DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS.equals(region.getType()))
                return true;
            }
          }
          return c != '\'' && DOMRegionContext.XML_CONTENT.equals(type);
        }
      }
    }
    return true;
  }

  public boolean hasPair(char c) {
    switch (c) {
      case '"':
      case '\'':
      case '[':
      case '(':
        return true;
      default:
        return false;
    }
  }

  protected char getPair(char c) {
    switch (c) {
      case '\'':
      case '"':
        return c;
      case '(':
        return ')';
      case '[':
        return ']';
      default:
        throw new IllegalArgumentException();
    }
  }

  public void initialize() {
    IPreferenceStore store = XMLUIPlugin.getInstance().getPreferenceStore();
    fCloseStrings = store.getBoolean(XMLUIPreferenceNames.TYPING_CLOSE_STRINGS);
    fCloseBrackets = store.getBoolean(XMLUIPreferenceNames.TYPING_CLOSE_BRACKETS);
    store.addPropertyChangeListener(this);
  }

  public void dispose() {
    XMLUIPlugin.getInstance().getPreferenceStore().removePropertyChangeListener(this);
  }

  public void propertyChange(PropertyChangeEvent event) {
    if (XMLUIPreferenceNames.TYPING_CLOSE_BRACKETS.equals(event.getProperty()))
      fCloseBrackets = ((Boolean) event.getNewValue()).booleanValue();
    else if (XMLUIPreferenceNames.TYPING_CLOSE_STRINGS.equals(event.getProperty()))
      fCloseStrings = ((Boolean) event.getNewValue()).booleanValue();
  }
}
