/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.style;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.wst.css.core.internal.parserz.CSSTextParser;
import org.eclipse.wst.css.core.internal.parserz.CSSTextToken;
import org.eclipse.wst.css.ui.internal.Logger;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class LineStyleProviderForEmbeddedCSS extends LineStyleProviderForCSS {

  public boolean prepareRegions(ITypedRegion typedRegion, int lineRequestStart,
      int lineRequestLength, Collection holdResults) {
    int regionStart = typedRegion.getOffset();
    int regionEnd = regionStart + typedRegion.getLength();
    IStructuredDocumentRegion wholeRegion = getDocument().getRegionAtCharacterOffset(regionStart);

    if (wholeRegion == null)
      return false;

    List tokens = null;
    int offset = typedRegion.getOffset();

    List cache = getCachedParsingResult(wholeRegion);
    if (cache == null) {
      try {
        String content = getDocument().get(typedRegion.getOffset(), typedRegion.getLength());

        //if region is an XML tag then in CSS style attribute, else in style tag
        int mode;
        if (wholeRegion.getType() == DOMRegionContext.XML_TAG_NAME) {
          mode = CSSTextParser.MODE_DECLARATION;
        } else {
          mode = CSSTextParser.MODE_STYLESHEET;
        }

        CSSTextParser parser = new CSSTextParser(mode, content);
        tokens = parser.getTokenList();
        cacheParsingResult(wholeRegion, tokens);
      } catch (BadLocationException e) {
        Logger.logException("Given a bad ITypedRegion: " + typedRegion, e);
      }
    } else {
      tokens = cache;
    }

    boolean result = false;

    if (tokens != null && 0 < tokens.size()) {
      int start = offset;
      int end = start;
      Iterator i = tokens.iterator();
      while (i.hasNext()) {
        CSSTextToken token = (CSSTextToken) i.next();
        end = start + token.length;
        int styleLength = token.length;
        /* The token starts in the region */
        if (regionStart <= start && start < regionEnd) {
          /*
           * [239415] The region may not span the total length of the token - Adjust the length so
           * that it doesn't overlap with other style ranges
           */
          if (regionEnd < end)
            styleLength = regionEnd - start;
          addStyleRange(holdResults, getAttributeFor(token.kind), start, styleLength);
        }
        /* The region starts in the token */
        else if (start <= regionStart && regionStart < end) {
          /* The token may not span the total length of the region */
          if (end < regionEnd)
            styleLength = end - regionStart;
          else
            /* Bugzilla 282218 */
            styleLength = regionEnd - regionStart;
          addStyleRange(holdResults, getAttributeFor(token.kind), regionStart, styleLength);
        }
        start += token.length;
      }
      result = true;
    }

    return result;
  }

  private void addStyleRange(Collection holdResults, TextAttribute attribute, int start, int end) {
    if (attribute != null)
      holdResults.add(new StyleRange(start, end, attribute.getForeground(),
          attribute.getBackground(), attribute.getStyle()));
    else
      holdResults.add(new StyleRange(start, end, null, null));
  }

  protected TextAttribute getAttributeFor(ITextRegion region) {
    return null;
  }

  private void cleanupCache() {
    fCacheKey = -1;
    fCacheResult = null;
  }

  private List getCachedParsingResult(IStructuredDocumentRegion region) {
    if (fCacheKey == region.getText().hashCode()) {
      return fCacheResult;
    }
    return null;
  }

  private void cacheParsingResult(IStructuredDocumentRegion region, List result) {
    fCacheKey = region.getText().hashCode();
    fCacheResult = result;
  }

  public void release() {
    super.release();
    cleanupCache();
  }

  int fCacheKey = -1;
  List fCacheResult = null;
}
