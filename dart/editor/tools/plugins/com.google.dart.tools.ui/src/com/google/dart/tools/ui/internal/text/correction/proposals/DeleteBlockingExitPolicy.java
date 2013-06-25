package com.google.dart.tools.ui.internal.text.correction.proposals;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;

/**
 * An exit policy that skips Backspace and Delete at the beginning and at the end of a linked
 * position, respectively. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=183925 .
 */
public class DeleteBlockingExitPolicy implements IExitPolicy {
  private final IDocument fDocument;

  public DeleteBlockingExitPolicy(IDocument document) {
    fDocument = document;
  }

  @Override
  public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
    if (length == 0 && (event.character == SWT.BS || event.character == SWT.DEL)) {
      LinkedPosition position = model.findPosition(new LinkedPosition(
          fDocument,
          offset,
          0,
          LinkedPositionGroup.NO_STOP));
      if (position != null) {
        if (event.character == SWT.BS) {
          if (offset - 1 < position.getOffset()) {
            //skip backspace at beginning of linked position
            event.doit = false;
          }
        } else /* event.character == SWT.DEL */{
          if (offset + 1 > position.getOffset() + position.getLength()) {
            //skip delete at end of linked position
            event.doit = false;
          }
        }
      }
    }

    return null; // don't change behavior
  }
}