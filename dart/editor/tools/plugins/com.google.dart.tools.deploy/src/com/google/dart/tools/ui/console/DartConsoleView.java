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

package com.google.dart.tools.ui.console;

import com.google.dart.tools.deploy.Activator;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.InstrumentedAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.PageSite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * An Eclipse view class that displays one and only one IConsole. This is different from the normal
 * ConsoleView which displays n consoles.
 */
@SuppressWarnings("restriction")
public class DartConsoleView extends ViewPart implements IConsoleView, IPropertyChangeListener {

  private class ClearAction extends InstrumentedAction {
    public ClearAction() {
      super("Clear", Activator.getImageDescriptor("icons/full/eview16/rem_co.gif"));
    }

    public void dispose() {

    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
      Runnable r = new Runnable() {
        @Override
        public void run() {
          if (console instanceof IOConsole) {
            IOConsole ioConsole = (IOConsole) console;

            ioConsole.clearConsole();
          } else if (console instanceof MessageConsole) {
            MessageConsole messageConsole = (MessageConsole) console;

            messageConsole.clearConsole();
          }
        }
      };

      new Thread(r).start();

    }
  }

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
      if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
        updateFont();
      }
    }
  }

  private class PropertiesAction extends Action {
    public PropertiesAction() {
      super("Properties...", Activator.getImageDescriptor("icons/full/obj16/properties.gif"));
    }

    @Override
    public void run() {
      PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(
          getSite().getShell(),
          getProcess(),
          null,
          null,
          null);

      dialog.open();
    }
  }

  private class TerminateAction extends InstrumentedAction implements ILaunchesListener2 {

    public TerminateAction() {
      super("Terminate", Activator.getImageDescriptor("icons/full/eview16/terminate.gif"));

      DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);

      update();
    }

    public void dispose() {
      DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
    }

    @Override
    public void launchesAdded(ILaunch[] launches) {
      update();
    }

    @Override
    public void launchesChanged(ILaunch[] launches) {
      update();
    }

    @Override
    public void launchesRemoved(ILaunch[] launches) {
      update();
    }

    @Override
    public void launchesTerminated(ILaunch[] launches) {
      update();
    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
      Runnable r = new Runnable() {
        @Override
        public void run() {
          try {
            getProcess().terminate();
          } catch (DebugException e) {
            Activator.logError(e);
          }
        }
      };

      new Thread(r).start();
    }

    void update() {
      IProcess process = getProcess();

      if (process != null) {
        setEnabled(!process.isTerminated());
      } else {
        setEnabled(false);
      }

    }
  }

  public static final String VIEW_ID = "com.google.dart.tools.ui.console";

  private Composite parent;
  private IConsole console;

  private IPageBookViewPage page;

  private PageSite pageSite;

  private TerminateAction terminateAction;
  private ClearAction clearAction;
  private IAction propertiesAction;

  private Display display;

  private IPreferenceStore preferences;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  public DartConsoleView() {
    DartConsoleManager.getManager().consoleViewOpened(this);
  }

  @Override
  public void createPartControl(Composite parent) {
    this.parent = parent;
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();

    display = Display.getCurrent();

    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
    getPreferences().addPropertyChangeListener(propertyChangeListener);//background

    clearAction = new ClearAction();
    propertiesAction = new PropertiesAction();
    terminateAction = new TerminateAction();

    updateToolBar();
  }

  @Override
  public void display(IConsole inConsole) {
    if (this.console != null) {
      this.console.removePropertyChangeListener(this);
      if (console instanceof ProcessConsole) {
        getPreferences().removePropertyChangeListener((ProcessConsole) this.console);//in,out,err
      }

      this.console = null;
    }

    if (this.page != null) {
      page.getControl().dispose();
      page.dispose();
      page = null;
    }

    // We recycle the console; remove any contributions from the previous ProcessConsole.
    clearToolBar();

    this.console = inConsole;

    // Add back our tolbar contributions.
    updateToolBar();

    // show the new console
    if (this.console != null) {
      this.console.addPropertyChangeListener(this);
      if (console instanceof ProcessConsole) {
        getPreferences().addPropertyChangeListener((ProcessConsole) this.console);
      }
      page = console.createPage(this);

      try {
        page.init(getPageSite());
        page.createControl(parent);
      } catch (PartInitException e) {
        Activator.logError(e);
      }

      parent.layout();
    }

    updateFont();
    updateColors();

    updateContentDescription();

    updateIcon();

    terminateAction.update();
    propertiesAction.setEnabled(getProcess() != null);
  }

  @Override
  public void dispose() {
    JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);

    display = null;

    DartConsoleManager.getManager().consoleViewClosed(this);

    if (console != null && isDead()) {
      IProcess process = ((ProcessConsole) console).getProcess();

      DebugPlugin.getDefault().getLaunchManager().removeLaunch(process.getLaunch());
    }

    terminateAction.dispose();
    clearAction.dispose();

    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
      fontPropertyChangeListener = null;
    }

    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }

    super.dispose();
  }

  @Override
  public IConsole getConsole() {
    return console;
  }

  @Override
  public boolean getScrollLock() {
    return false;
  }

  public boolean isDead() {
    if (console == null) {
      return true;
    }

    if (console instanceof ProcessConsole) {
      ProcessConsole processConsole = (ProcessConsole) console;

      if (processConsole.getProcess() == null) {
        return true;
      }

      return processConsole.getProcess().isTerminated();
    }

    return false;
  }

  @Override
  public boolean isPinned() {
    return false;
  }

  @Override
  public void pin(IConsole console) {

  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (display != null) {
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          updateContentDescription();
          updateIcon();
        }
      });
    }
  }

  @Override
  public void setFocus() {
    if (page != null) {
      page.setFocus();
    }
  }

  @Override
  public void setPinned(boolean pin) {

  }

  @Override
  public void setScrollLock(boolean scrollLock) {

  }

  @Override
  public void warnOfContentChange(IConsole console) {
    IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getViewSite().getAdapter(
        IWorkbenchSiteProgressService.class);

    if (progressService != null) {
      progressService.warnOfContentChange();
    }

    bringToFront();
  }

  protected void updateColors() {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (page != null && page.getControl() != null) {
          SWTUtil.setColors((StyledText) page.getControl(), getPreferences());
        }
      }
    });
  }

  private void bringToFront() {
    if (!getViewSite().getPage().isPartVisible(this)) {
      getViewSite().getPage().activate(this);
    }
  }

  private void clearToolBar() {
    IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();

    toolbar.removeAll();
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
  }

  private PageSite getPageSite() {
    if (pageSite == null) {
      pageSite = new PageSite(getViewSite());
    }

    return pageSite;
  }

  private IPreferenceStore getPreferences() {
    return preferences;
  }

  private IProcess getProcess() {
    if (console instanceof ProcessConsole) {
      return ((ProcessConsole) console).getProcess();
    } else {
      return null;
    }
  }

  private void updateContentDescription() {
    if (console instanceof ProcessConsole) {
      IProcess process = ((ProcessConsole) console).getProcess();

      String name = "";
      String configName = process.getLaunch().getLaunchConfiguration().getName();

      if (process.isTerminated()) {
        try {
          name = "<" + configName + "> exit code=" + process.getExitValue();
        } catch (DebugException ex) {
          // ignore
        }

        bringToFront();
      }

      setContentDescription(name);
    } else {
      setContentDescription("");
    }

    if (console instanceof ProcessConsole) {
      IProcess process = ((ProcessConsole) console).getProcess();

      setPartName(process.getLaunch().getLaunchConfiguration().getName());
    } else {
      setPartName("Output");
    }
  }

  private void updateFont() {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
        if (page != null && page.getControl() != null) {
          Font oldFont = page.getControl().getFont();
          Font font = SWTUtil.changeFontSize(oldFont, newFont);
          page.getControl().setFont(font);
        }
      }
    });
  }

  private void updateIcon() {
    if (isDead()) {
      setTitleImage(Activator.getImage("icons/full/eview16/console_view_d.gif"));
    } else {
      setTitleImage(Activator.getImage("icons/full/eview16/console_view.gif"));
    }
  }

  private void updateToolBar() {
    IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();

    toolbar.add(clearAction);
    toolbar.add(propertiesAction);
    toolbar.add(new Separator());
    toolbar.add(terminateAction);
    toolbar.add(new Separator("outputGroup"));
    getViewSite().getActionBars().updateActionBars();
  }

}
