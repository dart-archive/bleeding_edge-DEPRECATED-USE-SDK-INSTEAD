/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.util;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

/**
 * Utility to display (and/or clear) messages on the status line.
 * 
 * @author pavery
 */
public class PlatformStatusLineUtil {

  private static class ClearMessage implements Runnable {
    public void run() {
      displayMessage(null);
    }
  }

  private static class ClearStatusLine implements Runnable {
    private IStatusLineManager fManager;
    private boolean fIsError;

    ClearStatusLine(IStatusLineManager statusLineManager, boolean isError) {
      fManager = statusLineManager;
      fIsError = isError;
    }

    public void run() {
      if (fIsError) {
        fManager.setErrorMessage(null);
      } else {
        fManager.setMessage(null);
      }
    }
  }

  /**
   * Used to clear message on focus loss, change of selection, key type, etc...
   */
  private static class OneTimeListener extends FocusAdapter implements VerifyKeyListener,
      SelectionListener, MouseListener {
    private Runnable fRunner = null;
    private StyledText fStyledText;

    OneTimeListener(StyledText target, Runnable r) {
      fStyledText = target;
      fRunner = r;
      fStyledText.addVerifyKeyListener(this);
      fStyledText.addFocusListener(this);
      fStyledText.addSelectionListener(this);
      fStyledText.addMouseListener(this);
    }

    public void focusLost(FocusEvent e) {
      unhookAndRun();
    }

    public void mouseDoubleClick(MouseEvent e) {
      unhookAndRun();
    }

    public void mouseDown(MouseEvent e) {
      unhookAndRun();
    }

    public void mouseUp(MouseEvent e) {
      //
    }

    private void unhookAndRun() {
      fStyledText.removeVerifyKeyListener(this);
      fStyledText.removeFocusListener(this);
      fStyledText.removeSelectionListener(this);
      fStyledText.removeMouseListener(this);
      fStyledText.getDisplay().asyncExec(fRunner);
    }

    public void verifyKey(VerifyEvent event) {
      unhookAndRun();
    }

    public void widgetDefaultSelected(SelectionEvent e) {
      unhookAndRun();
    }

    public void widgetSelected(SelectionEvent e) {
      unhookAndRun();
    }
  }

  /**
   * Status line error message will be cleared w/ key type or selection change
   * 
   * @deprecated
   */
  public static void addOneTimeClearListener() {
    IEditorPart editor = getActiveEditor();
    boolean added = false;
    if (editor != null) {
      Control control = (Control) editor.getAdapter(Control.class);
      if (control instanceof StyledText) {
        new OneTimeListener((StyledText) control, new ClearMessage());
        added = true;
      }
    }
    if (!added) {
      // clear the error message immediately
      displayMessage(null);
    }
  }

  /**
   * Clears the status line immediately
   * 
   * @deprecated
   */
  public static void clearStatusLine() {
    displayMessage(null);
  }

  /**
   * Display a message on the status line (with a beep)
   * 
   * @deprecated
   * @param msg
   */
  public static void displayErrorMessage(String msg) {
    displayMessage(msg);
    PlatformUI.getWorkbench().getDisplay().beep();
  }

  /**
   * Display a message on the status line (no beep)
   * 
   * @deprecated
   * @param msg
   */
  public static void displayMessage(String msg) {
    IEditorPart editor = getActiveEditor();
    if (editor != null) {
      editor.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(msg);
    }

  }

  private static IEditorPart getActiveEditor() {
    IEditorPart editor = null;
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
      if (windows.length > 0) {
        window = windows[0];
      }
    }
    if (window != null) {
      IWorkbenchPage page = window.getActivePage();
      if (page != null)
        editor = page.getActiveEditor();
    }
    return editor;
  }

  private PlatformStatusLineUtil() {
    // force use of singleton
  }

  /**
   * Display an error message on the status line
   * 
   * @param viewer
   * @param msg
   */
  public static boolean displayTemporaryErrorMessage(ITextViewer viewer, String msg) {
    return _displayTemporaryMessage(viewer, msg, true);
  }

  /**
   * Display a message on the status line
   * 
   * @param viewer
   * @param msg
   */
  public static boolean displayTemporaryMessage(ITextViewer viewer, String msg) {
    return _displayTemporaryMessage(viewer, msg, false);
  }

  static boolean _displayTemporaryMessage(ITextViewer viewer, String msg, boolean isError) {
    boolean messageShown = false;
    IEditorPart editor = getActiveEditor();
    if (editor != null) {
      ITextEditor textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
      if (textEditor != null && textEditor instanceof StructuredTextEditor) {
        if (((StructuredTextEditor) textEditor).getTextViewer() == viewer) {
          IStatusLineManager statusLineManager = editor.getEditorSite().getActionBars().getStatusLineManager();
          if (isError)
            statusLineManager.setErrorMessage(msg);
          else
            statusLineManager.setMessage(msg);
          new OneTimeListener(viewer.getTextWidget(), new ClearStatusLine(statusLineManager,
              isError));
          messageShown = true;
        }
      }
    }

    if (!messageShown) {
      displayErrorMessage(msg);
      addOneTimeClearListener();
    }

    return messageShown;
  }
}
