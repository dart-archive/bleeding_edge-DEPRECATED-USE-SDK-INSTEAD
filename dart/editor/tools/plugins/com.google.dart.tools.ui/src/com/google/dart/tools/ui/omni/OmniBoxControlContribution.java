/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.omni;

import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Contributes the omnibox toolbar control.
 */
public class OmniBoxControlContribution extends WorkbenchWindowControlContribution {

  /**
   * Map of windows to control contributions. Needed in order to calculate an appropriate location
   * for the omnibox popup when invoked via a command.
   */
  private static Map<IWorkbenchWindow, OmniBoxControlContribution> CONTROL_MAP = new HashMap<IWorkbenchWindow, OmniBoxControlContribution>();

  private static final String MOD_KEY = SWTKeySupport.getKeyFormatterForPlatform().format(SWT.MOD1);

  private static final String WATERMARK_TEXT;

  private static final Color REGULAR_TEXT_COLOR = Display.getDefault().getSystemColor(
      SWT.COLOR_WIDGET_FOREGROUND);
  private static final Color WATERMARK_TEXT_COLOR = Display.getDefault().getSystemColor(
      SWT.COLOR_DARK_GRAY);

  private static int SEARCH_BOX_STYLE_BITS = SWT.SEARCH;

  static {
    if (Util.isMac()) {
      SEARCH_BOX_STYLE_BITS |= SWT.ICON_SEARCH;
    }
  }

  static {
    final String spacer = "                                                                ";//$NON-NLS-1$

    if (Util.isMac()) {
      WATERMARK_TEXT = spacer + MOD_KEY + "3 ";//$NON-NLS-1$
    } else {
      WATERMARK_TEXT = spacer + MOD_KEY + "-3 ";//$NON-NLS-1$
    }
  }

  private static final String CONTRIB_ID = "omnibox.control";//$NON-NLS-1$

  /*
   * Pixel offest for popup.
   */
  private static final int POPUP_PIXEL_HORIZ_NUDGE = 4;
  private static final int POPUP_PIXEL_VERT_NUDGE = 3;

  public static OmniBoxControlContribution getControlForWindow(IWorkbenchWindow window) {
    return CONTROL_MAP.get(window);
  }

  /**
   * Get a location relative to the active workbench window for presenting the omnibox popup. This
   * service is required outside the control in case the omnibox is invoked by a command (e.g.,
   * keybinding).
   * 
   * @param window the host window
   * @return the location to root the popup
   */
  public static Point getPopupLocation(IWorkbenchWindow window) {
    OmniBoxControlContribution contrib = CONTROL_MAP.get(window);
    if (contrib == null) {
      return new Point(0, 0);
    }
    return locationRelativeToControl(contrib.control);
  }

  private static Point locationRelativeToControl(Text control) {
    return control.toDisplay(0 + POPUP_PIXEL_HORIZ_NUDGE, control.getSize().y
        + POPUP_PIXEL_VERT_NUDGE);
  }

  private Text control;

  private boolean inControl;

  private OmniBoxPopup popup;

  //used to track whether text is being modified programmatically (e.g., watermark-setting)
  private boolean listenForTextModify = true;

  //used when we want to advance focus off of the text control (ideally to restore previous)
  private Control previousFocusControl;

  public OmniBoxControlContribution() {
    super(CONTRIB_ID);
  }

  @Override
  public void dispose() {
    //Remove this control contribution from the cached control map.
    IWorkbenchWindow disposedWindow = null;
    for (Entry<IWorkbenchWindow, OmniBoxControlContribution> entry : CONTROL_MAP.entrySet()) {
      if (entry.getValue() == this) {
        disposedWindow = entry.getKey();
        break;
      }
    }
    if (disposedWindow != null) {
      CONTROL_MAP.remove(disposedWindow);
    }
  }

  public void giveFocus() {
    previousFocusControl = control.getDisplay().getFocusControl();
    control.setFocus();
    clearWatermark();
  }

  @Override
  protected Control createControl(Composite parent) {
    control = createTextControl(parent);
    setWatermarkText();
    hookupListeners();
    CONTROL_MAP.put(getWorkbenchWindow(), this);
    return control;
  }

  protected void defocus() {
    if (previousFocusControl != null && !previousFocusControl.isDisposed()) {
      previousFocusControl.setFocus();
    } else {
      control.getParent().setFocus();
    }
  }

  protected void handleMouseEnter() {
    inControl = true;
    //cache on mouse enter to ensure we can restore focus after an invocation initiated by a mouse click
    previousFocusControl = Display.getDefault().getFocusControl();
  }

  protected void handleMouseExit() {
    inControl = false;
  }

  protected void handleSelection() {
    clearWatermark();
  }

  protected void refreshPopup() {
    if (popup != null && !popup.isDisposed()) {
      popup.refresh(getFilterText());
    }
  }

  private void clearWatermark() {
    //ensure watermark (or valid text) does not get re-cleared
    if (control.getForeground().equals(REGULAR_TEXT_COLOR)) {
      return;
    }
    silentSetControlText(""); //$NON-NLS-1$
    control.setForeground(REGULAR_TEXT_COLOR);
  }

  private Text createTextControl(Composite parent) {
    Text text = new Text(parent, SEARCH_BOX_STYLE_BITS);
    text.setToolTipText(OmniBoxMessages.OmniBoxControlContribution_control_tooltip);
    // Disables the default context menu for native SWT text boxes
    text.setMenu(new Menu(parent));
    return text;
  }

  private String getFilterText() {
    return control.getText().toLowerCase();
  }

  private void hookupListeners() {
    control.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        if (inControl) {
          handleSelection();
        }
      }
    });

    control.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        handleMouseEnter();
      }

      @Override
      public void mouseExit(MouseEvent e) {
        handleMouseExit();
      }
    });

    control.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (!listenForTextModify) {
          return;
        }
        String filterText = getFilterText();
        if (filterText.length() > 0) {
          if (popupClosed()) {
            openPopup();
          }
          refreshPopup();
        } else {
          popup.close();
        }
      }
    });

    control.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {

        if (SWT.ARROW_DOWN == e.keyCode) {
          if (popupClosed()) {
            openPopup();
            refreshPopup();
          }

        }

        if (popupClosed()) {
          //have escape defocus
          if (SWT.ESC == e.character) {
            defocus();
            return;
          }
          //and don't let other control characters invoke the popup
          if (Character.isISOControl(e.character)) {
            return;
          }
          openPopup();
        }

        if (popup != null && !popup.isDisposed()) {
          popup.sendKeyPress(e);
        }
      }
    });

    control.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        clearWatermark();
      }

      @Override
      public void focusLost(FocusEvent e) {
        //GTK linux requires special casing to handle the case where a click
        //outside the search box (or popup) should cause the popup to close
        //We identify this case by keying off focus changes --- if focus
        //is transfered to another control we trigger a close
        if (Util.isLinux()) {
          //Exec async to esnure that it occurs after the focus change
          Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
              Control focusControl = Display.getDefault().getFocusControl();
              if (focusControl != control && popup != null && focusControl != popup.table) {
                popup.close();
              }
            }
          });
        }
        if (popupClosed()) {
          setWatermarkText();
        }
      }
    });

  }

  private void openPopup() {
    popup = new OmniBoxPopup(getWorkbenchWindow(), null) {
      @Override
      public boolean close() {
        setWatermarkText();
        defocus();
        return super.close();
      }

      @Override
      protected Point getDefaultLocation(Point initialSize) {
        return locationRelativeToControl(control);
      }

      @Override
      protected Point getDefaultSize() {
        return new Point(control.getSize().x - POPUP_PIXEL_HORIZ_NUDGE * 2, 360);
      }

    };
    popup.setFilterControl(control);
    popup.open();

    if (Util.isMac()) {
      control.addListener(SWT.Deactivate, new Listener() {
        @Override
        public void handleEvent(Event event) {
          //selecting the scrollbar will deactivate but in that case we don't want to close
          if (event.display.getFocusControl() != popup.table) {
            popup.close();
          }
          control.removeListener(SWT.Deactivate, this);
        }
      });
    }
  }

  private boolean popupClosed() {
    return popup == null || popup.isDisposed();
  }

  private void setWatermarkText() {
    silentSetControlText(WATERMARK_TEXT);
    control.setForeground(WATERMARK_TEXT_COLOR);
  }

  //set text without notifying listeners
  private void silentSetControlText(String txt) {
    listenForTextModify = false;
    control.setText(txt);
    listenForTextModify = true;
  }

}
