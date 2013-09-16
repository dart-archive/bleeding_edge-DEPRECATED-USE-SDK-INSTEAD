/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.typing;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.ui.internal.Logger;

abstract public class AbstractCharacterPairInserter {

  class ExitPolicy implements IExitPolicy {

    private char fExit;
    private char fEscape;
    private IDocument fDocument;

    public ExitPolicy(char exit, char escape, IDocument document) {
      fExit = exit;
      fEscape = escape;
      fDocument = document;
    }

    public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
      if (!isMasked(offset)) {
        if (event.character == fExit)
          return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
      }
      return null;
    }

    private boolean isMasked(int offset) {
      try {
        return fEscape == fDocument.getChar(offset - 1);
      } catch (BadLocationException e) {
      }
      return false;
    }
  }

  /**
   * Pair the character <code>c</code> in the source viewer <code>viewer</code>. Positions are
   * linked before and after the inserted character.
   * 
   * @param viewer the source viewer to add the linked mode to
   * @param c the character to pair with
   * @return true if the character was successfully paired; false otherwise
   */
  public boolean pair(final ISourceViewer viewer, final char c) {
    if (!shouldPair(viewer, c))
      return false;

    final char mc = getPair(c);
    final char[] chars = new char[2];
    chars[0] = c;
    chars[1] = mc;

    IDocument document = viewer.getDocument();

    final Point selection = viewer.getSelectedRange();
    final int offset = selection.x;
    final int length = selection.y;

    boolean paired = false;
    try {
      document.replace(offset, length, new String(chars));
      LinkedModeModel model = new LinkedModeModel();
      LinkedPositionGroup group = new LinkedPositionGroup();
      group.addPosition(new LinkedPosition(document, offset + 1, 0, LinkedPositionGroup.NO_STOP));
      model.addGroup(group);
      model.forceInstall();

      LinkedModeUI ui = new LinkedModeUI(model, viewer);
      ui.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
      ui.setExitPosition(viewer, offset + 2, 0, Integer.MAX_VALUE);
      ui.setExitPolicy(getExitPolicy(mc, getEscapeChar(c), document));
      ui.setSimpleMode(true);
      ui.enter();

      paired = true;
    } catch (BadLocationException e) {
      Logger.logException(e);
    }
    return paired;
  }

  /**
   * Hook to evaluate if the character should be paired. Clients may override to evaluate the case.
   * 
   * @param viewer the source viewer where the character would be paired
   * @param c the character to pair
   * @return true if the character should have its pair character inserted; false otherwise
   */
  protected boolean shouldPair(ISourceViewer viewer, char c) {
    return true;
  }

  /**
   * Can the character be paired by the inserter
   * 
   * @param c the character of interest
   * @return true if the character can be paired by the inserter; false otherwise
   */
  abstract public boolean hasPair(char c);

  /**
   * Get the paired character for <code>c</code>
   * 
   * @param c the character to find the pair of
   * @return the pair character
   */
  abstract protected char getPair(char c);

  /**
   * Gets the escape character for <code>c</code> in case <code>c</code> is to be used within the
   * paired characters
   * 
   * @param c the character to find the escape character of
   * @return the escape character
   */
  protected char getEscapeChar(char c) {
    return 0;
  }

  public void initialize() {
  }

  public void dispose() {
  }

  protected IExitPolicy getExitPolicy(char exit, char escape, IDocument document) {
    return new ExitPolicy(exit, escape, document);
  }
}
