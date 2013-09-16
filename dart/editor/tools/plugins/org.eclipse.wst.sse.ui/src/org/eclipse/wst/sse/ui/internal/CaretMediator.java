/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.wst.sse.core.internal.util.Debug;
import org.eclipse.wst.sse.core.internal.util.Utilities;
import org.eclipse.wst.sse.ui.internal.view.events.CaretEvent;
import org.eclipse.wst.sse.ui.internal.view.events.ICaretListener;

/**
 * Has the responsibility of listening for key events, and mouse events, deciding if the caret has
 * moved (without a text change), and if so, will notify CaretListeners that the caret has moved.
 * Objects which are interested in ALL caret postion changes will also have to listen for
 * textChanged events.
 * 
 * @deprecated - use base selection notification
 */
public class CaretMediator implements Listener {

  class CaretMediatorListener implements KeyListener, MouseListener {
    public void keyPressed(KeyEvent e) {
      internalKeyPressed(e);
    }

    public void keyReleased(KeyEvent e) {
      internalKeyReleased(e);
    }

    public void mouseDoubleClick(MouseEvent e) {
    }

    public void mouseDown(MouseEvent e) {
      internalMouseDown(e);
    }

    public void mouseUp(MouseEvent e) {
      internalMouseUp(e);
    }
  }

  class RefreshDelayJob extends Job {
    private int fDelay = 0;

    RefreshDelayJob(int delay) {
      super(SSEUIMessages.caret_update); //$NON-NLS-1$
      setSystem(true);
      fDelay = delay;
    }

    /**
     * Setup a delayed CaretEvent firing
     */
    void touch() {
      cancel();
      schedule(fDelay);
    }

    protected IStatus run(IProgressMonitor monitor) {
      handleEvent(null);
      return Status.OK_STATUS;
    }
  }

  RefreshDelayJob fDelayer = null;
  private static final int DELAY = 300;

  /** used just for debug print outs */
  private long endTime;
  private long startTime;

  protected ICaretListener[] fCaretListeners;
  protected CaretMediatorListener internalListener;
  protected StyledText textWidget;

  /**
   * CaretMediator constructor comment.
   */
  public CaretMediator() {
    super();
  }

  /**
   * CaretMediator constructor comment. Must always provide the widget its supposed to listen to.
   */
  public CaretMediator(StyledText styledTextWidget) {
    this();
    setTextWidget(styledTextWidget);
  }

  public synchronized void addCaretListener(ICaretListener listener) {
    if (Debug.debugStructuredDocument) {
      System.out.println("CaretMediator::addCaretListener. Request to add an instance of " + listener.getClass() + " as a listener on caretlistner.");//$NON-NLS-2$//$NON-NLS-1$
    }
    // make sure listener is not already in listening array
    // (and if it is, print a warning to aid debugging, if needed)

    if (Utilities.contains(fCaretListeners, listener)) {
      if (Debug.displayWarnings) {
        System.out.println("CaretMediator::addCaretListener. listener " + listener + " was added more than once. ");//$NON-NLS-2$//$NON-NLS-1$
      }
    } else {
      if (Debug.debugStructuredDocument) {
        System.out.println("CaretMediator::addCaretListener. Adding an instance of " + listener.getClass() + " as a listener on caret mediator.");//$NON-NLS-2$//$NON-NLS-1$
      }
      int oldSize = 0;
      if (fCaretListeners != null) {
        // normally won't be null, but we need to be sure, for first
        // time through
        oldSize = fCaretListeners.length;
      }
      int newSize = oldSize + 1;
      ICaretListener[] newListeners = new ICaretListener[newSize];
      if (fCaretListeners != null) {
        System.arraycopy(fCaretListeners, 0, newListeners, 0, oldSize);
      }
      // add listener to last position
      newListeners[newSize - 1] = listener;
      //
      // now switch new for old
      fCaretListeners = newListeners;

    }
  }

  protected void fireCaretEvent(CaretEvent event) {
    if (fCaretListeners != null) {
      // we must assign listeners to local variable to be thread safe,
      // since the add and remove listner methods
      // can change this object's actual instance of the listener array
      // from another thread
      // (and since object assignment is atomic, we don't need to
      // synchronize
      ICaretListener[] holdListeners = fCaretListeners;
      //
      for (int i = 0; i < holdListeners.length; i++) {
        holdListeners[i].caretMoved(event);
      }
    }
  }

  public void handleEvent(Event e) {
    Display display = null;

    if (Debug.debugCaretMediator) {
      endTime = System.currentTimeMillis();
      System.out.println("Timer fired: " + (endTime - startTime)); //$NON-NLS-1$
    }

    // check if 'okToUse'
    if (textWidget != null && !textWidget.isDisposed()) {
      display = textWidget.getDisplay();
      if ((display != null) && (!display.isDisposed())) {
        display.asyncExec(new Runnable() {
          public void run() {
            if (textWidget != null && !textWidget.isDisposed()) {
              fireCaretEvent(new CaretEvent(textWidget, textWidget.getCaretOffset()));
            }
          }
        });
      }
    }
  }

  protected void internalKeyPressed(KeyEvent e) {
    fDelayer.cancel();
  }

  protected void internalKeyReleased(KeyEvent e) {
    switch (e.keyCode) {
      case SWT.ARROW_DOWN:
      case SWT.ARROW_UP:
      case SWT.ARROW_LEFT:
      case SWT.ARROW_RIGHT:
      case SWT.HOME:
      case SWT.END:
      case SWT.PAGE_DOWN:
      case SWT.PAGE_UP: {
        fDelayer.touch();
        break;
      }
      default: {
        // always update cursor postion, even during normal typing
        // (the logic may look funny, since we always to the same
        // thing, but we haven't always done the same thing, so I
        // wanted to leave that fact documented via code.)
        fDelayer.touch();
      }
    }
  }

  protected void internalMouseDown(MouseEvent e) {
    fDelayer.cancel();
  }

  protected void internalMouseUp(MouseEvent e) {
    // Note, even during a swipe select, when the mouse button goes up,
    // and the widget is
    // queried for the current caret postion, it always returns the
    // beginning of the selection,
    // which is desirable (at least for the known use of this feature,
    // which is to signal
    // that the property sheet can update itself.
    fDelayer.touch();
  }

  public void release() {
    fDelayer.cancel();
    if (textWidget != null && !textWidget.isDisposed()) {
      textWidget.removeKeyListener(internalListener);
      textWidget.removeMouseListener(internalListener);
      textWidget = null;
    }
  }

  public synchronized void removeCaretListener(ICaretListener listener) {
    if ((fCaretListeners != null) && (listener != null)) {
      // if its not in the listeners, we'll ignore the request
      if (Utilities.contains(fCaretListeners, listener)) {
        int oldSize = fCaretListeners.length;
        int newSize = oldSize - 1;
        ICaretListener[] newListeners = new ICaretListener[newSize];
        int index = 0;
        for (int i = 0; i < oldSize; i++) {
          if (fCaretListeners[i] == listener) { // ignore
          } else {
            // copy old to new if its not the one we are removing
            newListeners[index++] = fCaretListeners[i];
          }
        }
        // now that we have a new array, let's switch it for the old
        // one
        fCaretListeners = newListeners;
      }
    }
  }

  public void setTextWidget(StyledText newTextWidget) {
    if (fDelayer == null) {
      fDelayer = new RefreshDelayJob(DELAY);
    }

    // unhook from previous, if any
    if (this.textWidget != null) {
      fDelayer.cancel();
      this.textWidget.removeKeyListener(internalListener);
      this.textWidget.removeMouseListener(internalListener);
    }

    this.textWidget = newTextWidget;

    if (internalListener == null) {
      internalListener = new CaretMediatorListener();
    }

    if (this.textWidget != null) {
      this.textWidget.addKeyListener(internalListener);
      this.textWidget.addMouseListener(internalListener);
    }
  }
}
