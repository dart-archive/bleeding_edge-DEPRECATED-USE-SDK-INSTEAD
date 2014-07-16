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
package com.google.dart.tools.ui.omni;

import com.google.dart.tools.deploy.Activator;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.layout.GridDataFactory;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Contributes the omnibox toolbar control.
 */
public class OmniBoxControlContribution {

  private final class Popup extends OmniBoxPopup {

    private Popup(IWorkbenchWindow window, Command invokingCommand) {
      super(window, invokingCommand);
    }

    /**
     * Close the popup and reset the searchbox to it's initial state.
     */
    @Override
    public boolean close() {
      try {
        setWatermarkText();
        defocus();
      } catch (Throwable th) {
        Activator.logError(th);
      }
      return simpleClose();
    }

    @Override
    protected Point getDefaultLocation(Point initialSize) {
      return locationRelativeToControl(textControl);
    }

    @Override
    protected Point getDefaultSize() {
      return new Point(textControl.getSize().x - POPUP_PIXEL_HORIZ_NUDGE * 2, 360);
    }

    /**
     * Simple close of the pop-up (that does not reset the watermark or change focus).
     * 
     * @see OmniBoxPopup#close()
     */
    protected boolean simpleClose() {
      return super.close();
    }
  }

  /**
   * Map of windows to control contributions. Needed in order to calculate an appropriate location
   * for the omnibox popup when invoked via a command.
   */
  private static Map<IWorkbenchWindow, OmniBoxControlContribution> CONTROL_MAP = new HashMap<IWorkbenchWindow, OmniBoxControlContribution>();

  private static final String MOD_KEY = SWTKeySupport.getKeyFormatterForPlatform().format(SWT.MOD1);

  private static final String COMMAND_KEY_STRING = Util.isMac() ? "COMMAND" : "CTRL";

  private static final String WATERMARK_TEXT;

  private static int SEARCH_BOX_STYLE_BITS = SWT.SEARCH;

  static {
    if (Util.isMac()) {
      SEARCH_BOX_STYLE_BITS |= SWT.ICON_SEARCH;
    }
  }

  static {
    final String spacer = "                                                                ";//$NON-NLS-1$

    if (Util.isMac()) {
      //extra trailing space to mitigate OSX dimming at the edge of the text box
      WATERMARK_TEXT = spacer + MOD_KEY + " 3   ";//$NON-NLS-1$
    } else {
      WATERMARK_TEXT = spacer + MOD_KEY + "-3 ";//$NON-NLS-1$
    }
  }

  /*
   * Pixel offset for popup.
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
    return locationRelativeToControl(contrib.textControl);
  }

  private static Point locationRelativeToControl(Text control) {
    return control.toDisplay(0 + POPUP_PIXEL_HORIZ_NUDGE, control.getSize().y
        + POPUP_PIXEL_VERT_NUDGE);
  }

  private Text textControl;

  private boolean inControl;

  private Popup popup;

  //used to track whether text is being modified programmatically (e.g., watermark-setting)
  private boolean listenForTextModify = true;

  //used when we want to advance focus off of the text control (ideally to restore previous)
  private Control previousFocusControl;

  private final WorkbenchWindowControlContribution controlContribution;

  //used to force popup refresh in case text was selected and replaced
  private String previousFilterText;

  public OmniBoxControlContribution(WorkbenchWindowControlContribution controlContribution) {
    this.controlContribution = controlContribution;
  }

  public Control createControl(Composite parent) {
    textControl = createTextControl(parent);
    setWatermarkText();
    hookupListeners();
    CONTROL_MAP.put(getWorkbenchWindow(), this);
    updateColors();
    return textControl;
  }

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
    cacheFocusControl(textControl.getDisplay().getFocusControl());
    textControl.setFocus();
    clearWatermark();
  }

  protected void defocus() {
    if (previousFocusControl != null && !previousFocusControl.isDisposed()) {
      previousFocusControl.setFocus();
    } else {
      Shell activeWorkbenchShell = SearchBoxUtils.getActiveWorkbenchShell();
      if (activeWorkbenchShell != null) {
        activeWorkbenchShell.setFocus();
      } else {
        Activator.log("no active workbench shell for searchbox defocus");
      }
    }
  }

  protected void refreshPopup() {
    if (popup != null && !popup.isDisposed()) {
      popup.refresh(getFilterText());
    }
  }

  private void cacheFocusControl(Control focusControl) {
    //since the point of caching the control is to restore focus away from us,
    //ignore any sets to "self"
    if (focusControl != textControl) {
      previousFocusControl = focusControl;
    }
  }

  private void clearWatermark() {
    //ensure watermark (or valid text) does not get re-cleared
    if (textControl.getForeground().equals(OmniBoxColors.SEARCHBOX_TEXT_COLOR)) {
      return;
    }
    silentSetControlText(""); //$NON-NLS-1$
    textControl.setForeground(OmniBoxColors.SEARCHBOX_TEXT_COLOR);
    updateColors();
  }

  private Text createTextControl(Composite parent) {
    Text text = new Text(parent, SEARCH_BOX_STYLE_BITS);
    text.setToolTipText(OmniBoxMessages.OmniBoxControlContribution_control_tooltip);
    if (Util.isLinux()) {
      GridDataFactory.fillDefaults().indent(0, 1).grab(true, true).applyTo(text);
    }
    // Disables the default context menu for native SWT text boxes 
    text.setMenu(new Menu(parent));
    return text;
  }

  private String getFilterText() {
    return textControl.getText();
  }

  private IWorkbenchWindow getWorkbenchWindow() {
    return controlContribution.getWorkbenchWindow();
  }

  private void handleFocusGained() {
    //disable global keybinding handlers so we can trap copy/paste/etc
    ((IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class)).setKeyFilterEnabled(false);
    clearWatermark();
  }

  private void handleFocusLost() {
    //re-enable global keybinding handlers
    ((IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class)).setKeyFilterEnabled(true);

    //GTK linux requires special casing to handle the case where a click
    //outside the search box (or popup) should cause the popup to close
    //We identify this case by keying off focus changes --- if focus
    //is transfered to another control we trigger a close
    // scheglov: Actually we need to use "asyncExec" on Mac and Windows too.
    {
      //Exec async to ensure that it occurs after the focus change
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          Control focusControl = Display.getDefault().getFocusControl();
          if (focusControl != textControl && popup != null && focusControl != popup.table) {
            popup.close();
            popup = null;
          }
        }
      });
    }
    if (popupClosed()) {
      setWatermarkText();
    }
  }

  @SuppressWarnings("deprecation")
  private void handleKeyPressed(KeyEvent e) {

    if (e.keyCode == 'f' && (e.stateMask & SWT.MOD1) == SWT.MOD1) {
      if (e.character == '\0') {
        return;
      }
      // special treatment to activate the text search when requested
      IWorkbench wb = PlatformUI.getWorkbench();
      IBindingService bindings = (IBindingService) wb.getService(IBindingService.class);
      try {
        KeySequence keys = KeySequence.getInstance(COMMAND_KEY_STRING + "+F");
        Binding binding = bindings.getPerfectMatch(keys);
        if (binding != null) {
          ParameterizedCommand parameterizedCommand = binding.getParameterizedCommand();
          if (parameterizedCommand != null) {
            defocus();
            parameterizedCommand.getCommand().execute(null);
          }
        }
      } catch (ParseException ex) {
        Activator.logError(ex);
      } catch (CommandException ex) {
        Activator.logError(ex);
      }
      e.doit = false;
      return;
    }

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

  private void handleModifyText() {
    if (!listenForTextModify) {
      return;
    }
    String filterText = getFilterText();

    //we need to re-search if the leading char has changed ('a' -> HOME -> 'x')
    boolean needsResearch = hasLeadingFilterCharChanged();

    //cache for next time around
    previousFilterText = filterText;

    if (filterText.length() > 0) {
      if (needsResearch && !popupClosed()) {
        popup.simpleClose();
        popup = null;
      }
      if (popupClosed()) {
        openPopup();
      }
      refreshPopup();
    } else {
      popup.simpleClose();
      popup = null;
    }
  }

  private void handleMouseEnter() {
    inControl = true;
    //cache on mouse enter to ensure we can restore focus after an invocation initiated by a mouse click
    cacheFocusControl(textControl.getDisplay().getFocusControl());
  }

  private void handleMouseExit() {
    inControl = false;
  }

  private void handleMouseUp() {
    if (inControl) {
      handleSelection();
    }
  }

  private void handleSelection() {
    clearWatermark();
  }

  /**
   * Tests if the leading character of the filter text has changed since the last recorded text
   * modification.
   */
  private boolean hasLeadingFilterCharChanged() {
    if (previousFilterText != null && !previousFilterText.isEmpty()) {
      String filterText = getFilterText();
      if (filterText == null || filterText.isEmpty()) {
        return false;
      }
      return previousFilterText.charAt(0) != filterText.charAt(0);
    }
    return false;
  }

  private void hookupListeners() {
    textControl.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        handleMouseUp();
      }
    });

    textControl.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        handleMouseEnter();
      }

      @Override
      public void mouseExit(MouseEvent e) {
        handleMouseExit();
      }
    });

    textControl.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        handleModifyText();
      }
    });

    textControl.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        handleKeyPressed(e);
      }
    });

    textControl.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        handleFocusGained();
      }

      @Override
      public void focusLost(FocusEvent e) {
        handleFocusLost();
      }
    });
  }

  private void openPopup() {
    popup = new Popup(getWorkbenchWindow(), null);
    popup.setFilterControl(textControl);
    popup.open();

    if (Util.isMac()) {
      textControl.addListener(SWT.Deactivate, new Listener() {
        @Override
        public void handleEvent(Event event) {
          if (popup != null) {
            //selecting the scrollbar will deactivate but in that case we don't want to close
            // TODO: bug; going from scrollbar back to text entry clears text!
            Control focusControl = event.display.getFocusControl();
            //in some cases the focus control goes null even though the text still receives
            //key events (issue 1905) and we want to *not* close the popup
            if (focusControl != null && focusControl != popup.table) {
              popup.close();
              popup = null;
            }
          }
          textControl.removeListener(SWT.Deactivate, this);
        }
      });
    }

  }

  private boolean popupClosed() {
    return popup == null || popup.isDisposed();
  }

  private void setWatermarkText() {
    silentSetControlText(WATERMARK_TEXT);
    textControl.setForeground(OmniBoxColors.WATERMARK_TEXT_COLOR);
  }

  //set text without notifying listeners
  private void silentSetControlText(String txt) {
    try {
      listenForTextModify = false;
      textControl.setText(txt);
    } finally {
      listenForTextModify = true;
    }
  }

  private void updateColors() {
    //TODO(pquitslund): disabled pending investigation of regressions on ubuntu
//    Display display = textControl.getDisplay();
//    Color color = DartUI.getEditorForeground(
//        DartToolsPlugin.getDefault().getCombinedPreferenceStore(),
//        display);
//    if (color == null) {
//      color = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
//    }
//    textControl.setForeground(color);
//    color = DartUI.getEditorBackground(
//        DartToolsPlugin.getDefault().getCombinedPreferenceStore(),
//        display);
//    if (color == null) {
//      color = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
//    }
//    textControl.setBackground(color);
  }

}
