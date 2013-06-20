/**
 * 
 */
package com.xored.glance.internal.ui.sources;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

import com.xored.glance.ui.sources.ITextSourceDescriptor;

/**
 * @author Yuri Strot
 */
public class TextSourceListener implements Listener {

  public TextSourceListener(ITextSourceDescriptor[] descriptors) {
    this.descriptors = descriptors;
  }

  public void addSourceProviderListener(ISourceProviderListener listener) {
    if (listeners.size() == 0) {
      getDisplay().addFilter(SWT.FocusIn, this);
    }
    listeners.add(listener);
  }

  public void removeSourceProviderListener(ISourceProviderListener listener) {
    listeners.remove(listener);
    if (listeners.size() == 0) {
      getDisplay().removeFilter(SWT.FocusIn, this);
      selection = null;
    }
  }

  public void handleEvent(Event event) {
    TextSourceMaker creator = getCreator(getDisplay().getFocusControl());
    if (!creator.equals(selection)) {
      selection = creator;
      Object[] objects = listeners.getListeners();
      for (Object object : objects) {
        ISourceProviderListener listener = (ISourceProviderListener) object;
        listener.sourceChanged(selection);
      }
    }
  }

  public TextSourceMaker getSelection() {
    return getCreator(getDisplay().getFocusControl());
  }

  private TextSourceMaker getCreator(Control control) {
    return new TextSourceMaker(getDescriptor(control), control);
  }

  private ITextSourceDescriptor getDescriptor(Control control) {
    if (control != null) {
      for (ITextSourceDescriptor descriptor : descriptors) {
        if (descriptor.isValid(control)) {
          return descriptor;
        }
      }
    }
    return null;
  }

  private Display getDisplay() {
    return PlatformUI.getWorkbench().getDisplay();
  }

  private TextSourceMaker selection;
  private ListenerList listeners = new ListenerList();
  private ITextSourceDescriptor[] descriptors;

}
