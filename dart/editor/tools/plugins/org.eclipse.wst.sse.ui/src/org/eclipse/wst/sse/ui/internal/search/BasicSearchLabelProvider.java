/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.search;

import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;
import com.google.dart.tools.search.ui.text.Match;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImageHelper;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImages;

/**
 * <p>
 * The label provider designed for use with <code>BasicSearchMatchElement</code>
 * <p>
 * <p>
 * Displays in the format of:<br/>
 * lineNum: Message (# matches)</br> 1: <a></a> (2 matches)
 * </p>
 */
public class BasicSearchLabelProvider extends LabelProvider implements IStyledLabelProvider {
  /**
   * ID of match highlighting background color
   */
  private static final String MATCH_BG_ID = "org.eclipse.wst.sse.ui.search.MATCH_BG";

  //register the match highlighting background color once
  static {
    JFaceResources.getColorRegistry().put(MATCH_BG_ID, new RGB(206, 204, 247));
  }

  /**
   * Match highlighting background color styler
   */
  private static final Styler HIGHLIGHT_WRITE_STYLE = StyledString.createColorRegistryStyler(null,
      MATCH_BG_ID);

  /**
   * Need the page if want to determine the number of matches, but this can be <code>null</code>
   */
  private AbstractTextSearchViewPage fPage;

  /**
   * <p>
   * Construct the provider without a <code>AbstractTextSearchViewPage</code>
   * <p>
   * <p>
   * <b>NOTE:</b>If this constructor is used then the provider will not be able to determine the
   * number of matches that are all on the same line for a given element
   * </p>
   */
  public BasicSearchLabelProvider() {
    this(null);
  }

  /**
   * <p>
   * Construct the provider with a <code>AbstractTextSearchViewPage</code> so that the number of
   * matches that are all on the same line for a given element can be determined.
   * </p>
   * 
   * @param page Will be used to determine the number of matches that are all on the same line
   */
  public BasicSearchLabelProvider(AbstractTextSearchViewPage page) {
    fPage = page;
  }

  /**
   * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
   */
  @Override
  public Image getImage(Object element) {
    return EditorPluginImageHelper.getInstance().getImage(EditorPluginImages.IMG_OBJ_OCC_MATCH);
  }

  /**
   * <p>
   * Given a <code>Match</code> object containing a <code>BasicSearchMatchElement</code> element
   * returns a <code>StyledString</code> in the form of:
   * </p>
   * <p>
   * lineNum: Message (# matches)</br> 1: <a></a> (2 matches)
   * </p>
   * 
   * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
   */
  @Override
  public StyledString getStyledText(Object obj) {
    StyledString styledText = new StyledString();
    BasicSearchMatchElement element = null;
    if (obj instanceof Match) {
      Match match = (Match) obj;

      if (match.getElement() instanceof BasicSearchMatchElement) {
        element = (BasicSearchMatchElement) match.getElement();
      }
    } else if (obj instanceof BasicSearchMatchElement) {
      element = (BasicSearchMatchElement) obj;
    }

    //get the match count if possible
    int matchCount = 0;
    Match[] matches = new Match[0];
    if (fPage != null) {
      matches = fPage.getInput().getMatches(obj);
      matchCount = matches.length;
    }

    //if everything was of the right type create our formated message,
    //else use the toString of the given object for the message
    if (element != null) {
      String message = element.getLine().trim(); //$NON-NLS-1$
      int trimedAmount = element.getLine().indexOf(message);
      String lineNum = element.getLineNum() + 1 + ": "; //$NON-NLS-1$

      styledText.append(lineNum, StyledString.QUALIFIER_STYLER);
      styledText.append(message);

      //get the match count if possible
      for (int i = 0; i < matchCount; ++i) {
        int offset = matches[i].getOffset() - element.geLineOffset() + lineNum.length()
            - trimedAmount;
        styledText.setStyle(offset, matches[i].getLength(), HIGHLIGHT_WRITE_STYLE);
      }

    } else {
      styledText.append(obj.toString());
    }

    //append the match count if its worth appending
    if (matchCount > 1) {
      String matchesMsg = " "
          + MessageFormat.format(SSEUIMessages.TextSearchLabelProvider_matchCountFormat,
              new Object[] {new Integer(matchCount)});
      styledText.append(matchesMsg, StyledString.COUNTER_STYLER);
    }

    return styledText;
  }

  /**
   * <p>
   * <b>Note:</b> Because this class implements <code>IStyledLabelProvider</code> the
   * <code>getStyledText</code> function should be being called and not this one, but better save
   * then sorry
   * </p>
   * 
   * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
   * @see org.eclipse.wst.sse.ui.internal.search.BasicSearchLabelProvider#getStyledText(Object)
   */
  @Override
  public final String getText(Object element) {
    return getStyledText(element).getString();
  }
}
