/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.extension;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.wst.sse.ui.internal.IExtendedEditorAction;
import org.eclipse.wst.sse.ui.internal.IExtendedSimpleEditor;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.osgi.framework.Bundle;

public class ExtendedEditorActionProxyForDelayLoading implements IExtendedEditorAction,
    IExtendedEditorActionProxyForDelayLoading, IAction {
  private IAction proxy;
  private IAction dummy = new Action() { // this is for mainly its property change notify...
  };
  // proxy'ed properties
  private String p_id;
  private boolean set_p_id;
  private String p_text;
  private String p_description;
  private boolean set_p_text;
  private boolean set_p_description;
  private String p_toolTipText;
  private boolean set_p_toolTipText;
  private String p_actionDefinitionId;
  private boolean set_p_actionDefinitionId;
  private ImageDescriptor p_image;
  private boolean set_p_image;
  private ImageDescriptor p_hoverImage;
  private boolean set_p_hoverImage;
  private ImageDescriptor p_disabledImage;
  private boolean set_p_disabledImage;
  private int p_accelerator;
  private boolean set_p_accelerator;
  private int p_style = AS_PUSH_BUTTON;
  //private boolean set_p_style;
  private HelpListener p_helpListener;
  private boolean set_p_helpListener;
  private boolean p_enabled = true; // should be same as what is done in
  private boolean set_p_enabled;
  private ListenerList p_listeners = new ListenerList(3);
  private boolean set_p_listeners;
  private boolean p_checked;
  private boolean set_p_checked;
  private IExtendedSimpleEditor p_targetEditor;
  private boolean set_p_targetEditor;
  private boolean p_isvisible = true; // should be true

  private IConfigurationElement element;
  private String classAttribute;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#addPropertyChangeListener(org.eclipse.jface.util.
   * IPropertyChangeListener)
   */
  public void addPropertyChangeListener(IPropertyChangeListener listener) {
    p_listeners.add(listener);
    set_p_listeners = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.addPropertyChangeListener(listener);
    } else {
      dummy.addPropertyChangeListener(listener);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getAccelerator()
   */
  public int getAccelerator() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getAccelerator();
    }
    return p_accelerator;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getActionDefinitionId()
   */
  public String getActionDefinitionId() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getActionDefinitionId();
    }
    return p_actionDefinitionId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getDescription()
   */
  public String getDescription() {
    //        System.out.println(p_id + ": getDescription");
    //        System.out.flush();
    if (proxy != null) {
      return proxy.getDescription();
    }
    return p_description;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getDisabledImageDescriptor()
   */
  public ImageDescriptor getDisabledImageDescriptor() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getDisabledImageDescriptor();
    }
    return p_disabledImage;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getHelpListener()
   */
  public HelpListener getHelpListener() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getHelpListener();
    }
    return p_helpListener;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getHoverImageDescriptor()
   */
  public ImageDescriptor getHoverImageDescriptor() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getHoverImageDescriptor();
    }
    return p_hoverImage;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getId()
   */
  public String getId() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getId();
    }
    return p_id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getImageDescriptor()
   */
  public ImageDescriptor getImageDescriptor() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getImageDescriptor();
    }
    return p_image;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getMenuCreator()
   */
  public IMenuCreator getMenuCreator() {
    //        System.out.println(p_id + ": getMenuCreator");
    //        System.out.flush();
    realize();
    if (proxy != null) {
      return proxy.getMenuCreator();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getStyle()
   */
  public int getStyle() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getStyle();
    }
    return p_style;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getText()
   */
  public String getText() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getText();
    }
    if (p_text != null) {
      return p_text;
    }
    return "";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#getToolTipText()
   */
  public String getToolTipText() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.getToolTipText();
    }
    return p_toolTipText;
  }

  /**
   * Check if the contributing bundle is active
   */
  public boolean isBundleActive() {
    Bundle bundle = Platform.getBundle(element.getDeclaringExtension().getNamespace());
    if (bundle != null && bundle.getState() != Bundle.ACTIVE) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#isChecked()
   */
  public boolean isChecked() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.isChecked();
    }
    return p_checked;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#isEnabled()
   */
  public boolean isEnabled() {
    // don't realize class.
    // realize();
    if (proxy != null) {
      return proxy.isEnabled();
    }
    return p_enabled;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#removePropertyChangeListener(org.eclipse.jface.util.
   * IPropertyChangeListener)
   */
  public void removePropertyChangeListener(IPropertyChangeListener listener) {
    p_listeners.remove(listener);
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.removePropertyChangeListener(listener);
    }
    dummy.removePropertyChangeListener(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#run()
   */
  public void run() {
    realize();
    if (proxy != null) {
      // if this has a key accelerator, first update this action and to
      // see if this action is enabled or not.
      if ((proxy.getAccelerator() > 0) || (proxy.getActionDefinitionId() != null)) {
        update();
        if (isEnabled() == true) {
          proxy.run();
        }
      } else {
        proxy.run();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#runWithEvent(org.eclipse.swt.widgets.Event)
   */
  public void runWithEvent(Event event) {
    realize();
    if (proxy != null) {
      // same as run()
      if ((proxy.getAccelerator() > 0) || (proxy.getActionDefinitionId() != null)) {
        update();
        if (isEnabled() == true) {
          proxy.runWithEvent(event);
        }
      } else {
        proxy.runWithEvent(event);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setActionDefinitionId(java.lang.String)
   */
  public void setActionDefinitionId(String id) {
    p_actionDefinitionId = id;
    set_p_actionDefinitionId = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setActionDefinitionId(id);
    } else {
      dummy.setActionDefinitionId(id);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setChecked(boolean)
   */
  public void setChecked(boolean checked) {
    p_checked = checked;
    set_p_checked = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setChecked(checked);
    } else {
      dummy.setChecked(checked);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setDescription(java.lang.String)
   */
  public void setDescription(String description) {
    p_description = description;
    set_p_description = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setDescription(description);
    } else {
      dummy.setDescription(description);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setDisabledImageDescriptor(org.eclipse.jface.resource.
   * ImageDescriptor)
   */
  public void setDisabledImageDescriptor(ImageDescriptor newImage) {
    p_disabledImage = newImage;
    set_p_disabledImage = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setDisabledImageDescriptor(newImage);
    } else {
      dummy.setDisabledImageDescriptor(newImage);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setEnabled(boolean)
   */
  public void setEnabled(boolean enabled) {
    p_enabled = enabled;
    set_p_enabled = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setEnabled(enabled);
    } else {
      dummy.setEnabled(enabled);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setHelpListener(org.eclipse.swt.events.HelpListener)
   */
  public void setHelpListener(HelpListener listener) {
    p_helpListener = listener;
    set_p_helpListener = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setHelpListener(listener);
    } else {
      dummy.setHelpListener(listener);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.action.IAction#setHoverImageDescriptor(org.eclipse.jface.resource.ImageDescriptor
   * )
   */
  public void setHoverImageDescriptor(ImageDescriptor newImage) {
    p_hoverImage = newImage;
    set_p_hoverImage = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setHoverImageDescriptor(newImage);
    } else {
      dummy.setHoverImageDescriptor(newImage);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setId(java.lang.String)
   */
  public void setId(String id) {
    p_id = id;
    set_p_id = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setId(id);
    } else {
      dummy.setId(id);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.action.IAction#setImageDescriptor(org.eclipse.jface.resource.ImageDescriptor)
   */
  public void setImageDescriptor(ImageDescriptor newImage) {
    p_image = newImage;
    set_p_image = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setImageDescriptor(newImage);
    } else {
      dummy.setImageDescriptor(newImage);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setMenuCreator(org.eclipse.jface.action.IMenuCreator)
   */
  public void setMenuCreator(IMenuCreator creator) {
    //        System.out.println(p_id + ": setMenuCreator");
    //        System.out.flush();
    realize();
    if (proxy != null) {
      proxy.setMenuCreator(creator);
    } else {
      dummy.setMenuCreator(creator);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setText(java.lang.String)
   */
  public void setText(String text) {
    p_text = text;
    set_p_text = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setText(text);
    } else {
      dummy.setText(text);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setToolTipText(java.lang.String)
   */
  public void setToolTipText(String text) {
    p_toolTipText = text;
    set_p_toolTipText = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setToolTipText(text);
    } else {
      dummy.setToolTipText(text);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#setAccelerator(int)
   */
  public void setAccelerator(int keycode) {
    p_accelerator = keycode;
    set_p_accelerator = true;
    // don't realize class.
    // realize();
    if (proxy != null) {
      proxy.setAccelerator(keycode);
    } else {
      dummy.setAccelerator(keycode);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.IExtendedEditorAction#setActiveExtendedEditor(com.ibm.sse.editor
   * .extension.IExtendedSimpleEditor)
   */
  public void setActiveExtendedEditor(IExtendedSimpleEditor targetEditor) {
    p_targetEditor = targetEditor;
    set_p_targetEditor = true;
    // don't realize class.
    // realize();
    if ((proxy != null) && (proxy instanceof IExtendedEditorAction)) {
      ((IExtendedEditorAction) proxy).setActiveExtendedEditor(targetEditor);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.sse.editor.extension.IExtendedEditorAction#isVisible()
   */
  public boolean isVisible() {
    // don't realize class.
    // realize();
    if ((proxy != null) && (proxy instanceof IExtendedEditorAction)) {
      return ((IExtendedEditorAction) proxy).isVisible();
    }
    return p_isvisible;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#isHandled
   */
  public boolean isHandled() {
    return false;
  }

  // see ActionDescriptor#createExtension
  private static IAction newInstance(final IConfigurationElement elm, final String cla) {
    final Object[] result = new Object[1];
    // If plugin has been loaded create extension.
    // Otherwise, show busy cursor then create extension.
    Bundle bundle = Platform.getBundle(elm.getDeclaringExtension().getNamespace());
    if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
      try {
        result[0] = elm.createExecutableExtension(cla);
      } catch (Exception e) {
        // catch and log ANY exception from extension point
        handleCreateExecutableException(result, e, elm.getAttribute(cla));
      }
    } else {
      BusyIndicator.showWhile(null, new Runnable() {
        public void run() {
          try {
            result[0] = elm.createExecutableExtension(cla);
          } catch (Exception e) {
            // catch and log ANY exception from extension point
            handleCreateExecutableException(result, e, elm.getAttribute(cla));
          }
        }
      });
    }
    if ((result[0] != null) && (result[0] instanceof IAction)) {
      return (IAction) result[0];
    }
    return null;
  }

  private static void handleCreateExecutableException(final Object[] result, Throwable e,
      String classname) {
    Logger.logException("Unable to create proxy action for " + classname, e);
    result[0] = null;
  }

  /**
   * Instantiate a real class here
   */
  public void realize() {
    if ((proxy == null) && (element != null) && (classAttribute != null)) {
      proxy = newInstance(element, classAttribute);
      if (proxy != null) {
        // propagate proxy'ed properties
        if (set_p_listeners == true) {
          if (p_listeners.size() > 0) {
            Object[] l = p_listeners.getListeners();
            int ls = l.length;
            for (int i = 0; i < ls; i++) {
              IPropertyChangeListener pl = (IPropertyChangeListener) l[i];
              proxy.addPropertyChangeListener(pl);
              dummy.removePropertyChangeListener(pl); // remove listener from dymmy so that we don't send notifications twice anymore
            }
          }
        }
        if (set_p_accelerator == true) {
          proxy.setAccelerator(p_accelerator);
        }
        if (set_p_actionDefinitionId == true) {
          if (p_actionDefinitionId != null) {
            proxy.setActionDefinitionId(p_actionDefinitionId);
          }
        }
        if (set_p_checked == true) {
          proxy.setChecked(p_checked);
        }
        if (set_p_disabledImage == true) {
          if (p_disabledImage != null) {
            proxy.setDisabledImageDescriptor(p_disabledImage);
          }
        }
        if (set_p_enabled == true) {
          proxy.setEnabled(p_enabled);
        }
        if (set_p_helpListener == true) {
          if (p_helpListener != null) {
            proxy.setHelpListener(p_helpListener);
          }
        }
        if (set_p_hoverImage == true) {
          if (p_hoverImage != null) {
            proxy.setHoverImageDescriptor(p_hoverImage);
          }
        }
        if (set_p_id == true) {
          if (p_id != null) {
            proxy.setId(p_id);
          }
        }
        if (set_p_image == true) {
          if (p_image != null) {
            proxy.setImageDescriptor(p_image);
          }
        }
        if (set_p_text == true) {
          if (p_text != null) {
            proxy.setText(p_text);
          }
        }
        if (set_p_description == true) {
          if (p_description != null) {
            proxy.setDescription(p_description);
          }
        }
        if (set_p_toolTipText == true) {
          if (p_toolTipText != null) {
            proxy.setToolTipText(p_toolTipText);
          }
        }
        if (set_p_targetEditor == true) {
          if (p_targetEditor != null) {
            if (proxy instanceof IExtendedEditorAction) {
              ((IExtendedEditorAction) proxy).setActiveExtendedEditor(p_targetEditor);
            }
          }
        }
      }
    }
  }

  public boolean isRealized() {
    return (proxy != null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IUpdate#update()
   */
  public void update() {
    // don't realize class.
    // realize();
    if ((proxy != null) && (proxy instanceof IUpdate)) {
      ((IUpdate) proxy).update();
    }
  }

  /**
   * get a real action class
   */
  public IAction getAction() {
    realize();
    return proxy;
  }

  /**
   * These are Actions's constructors
   */
  ExtendedEditorActionProxyForDelayLoading() {
    super();
  }

  public ExtendedEditorActionProxyForDelayLoading(final IConfigurationElement element,
      final String classAttribute) {
    super();
    this.element = element;
    this.classAttribute = classAttribute;
  }
}
