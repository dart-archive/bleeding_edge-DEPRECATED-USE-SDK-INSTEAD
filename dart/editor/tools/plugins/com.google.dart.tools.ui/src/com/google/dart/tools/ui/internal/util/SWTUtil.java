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
package com.google.dart.tools.ui.internal.util;

import com.google.common.collect.Maps;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import java.util.Map;

/**
 * Utility class to simplify access to some SWT resources.
 */
public class SWTUtil {

  private static final int COMBO_VISIBLE_ITEM_COUNT = 30;

  /**
   * Font cache. We use it to don't create multiple copies of the font with the same properties.
   */
  private static Map<String, Font> fontCache = Maps.newHashMap();

  /**
   * Propagates changes in {@link JFaceResources} font to the given {@link Control}.
   */
  public static void bindJFaceResourcesFontToControl(final Control control) {
    updateFontSizeFromJFaceResources(control);
    final FontRegistry fontRegistry = JFaceResources.getFontRegistry();
    final IPropertyChangeListener propertyListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (!control.isDisposed()) {
          if (FontPreferencePage.VIEW_BASE_FONT_KEY.equals(event.getProperty())) {
            updateFontSizeFromJFaceResources(control);
          }
        }
      }
    };
    control.addListener(SWT.Dispose, new Listener() {
      @Override
      public void handleEvent(Event event) {
        fontRegistry.removeListener(propertyListener);
      }
    });
    fontRegistry.addListener(propertyListener);
  }

  /**
   * Create a new font with the same attributes as the given <code>oldFont</code> except that its
   * size is taken from the <code>newFont</code> Font.
   * 
   * @param oldFont The font whose size is to be changed (not null)
   * @param newFont The Font that defines the new font size (may be null)
   * @return A new Font like the original but with a new size
   */
  public static Font changeFontSize(Font oldFont, Font newFont) {
    if (newFont == null) {
      return oldFont;
    }
    FontData[] fontData = newFont.getFontData();
    return changeFontSize(oldFont, fontData);
  }

  /**
   * Create a new font with the same attributes as the given <code>oldFont</code> except that its
   * size is taken from the <code>sizedFontData</code> FontData.
   * 
   * @param oldFont The font whose size is to be changed (not null)
   * @param sizedFontData The FontData[] containing the new font size (may be null or zero length)
   * @return A new Font like the original but with a new size
   */
  public static Font changeFontSize(Font oldFont, FontData[] sizedFontData) {
    if (sizedFontData == null || sizedFontData.length == 0) {
      return oldFont;
    }
    int height = sizedFontData[0].getHeight();
    return changeFontSize(oldFont, height);
  }

  /**
   * Create a new font with the same attributes as the given <code>oldFont</code> except that its
   * size is given by <code>height</code>.
   * 
   * @param oldFont The font whose size is to be changed (not null)
   * @param height The new font height
   * @return A new Font like the original but with a new size
   */
  public static Font changeFontSize(Font oldFont, int height) {
    FontData[] data = oldFont.getFontData();
    FontData newData = new FontData(data[0].getName(), height, data[0].getStyle());
    return getFont(oldFont.getDevice(), new FontData[] {newData});
  }

  /**
   * Make a FontData array similar to the given <code>data</code> but having font height determined
   * by <code>height</code>.
   * 
   * @param height New font height
   * @return A font data array with the new height
   */
  public static FontData[] changeFontSize(FontData[] data, int height) {
    FontData newData = new FontData(data[0].getName(), height, data[0].getStyle());
    return new FontData[] {newData};
  }

  public static void eraseSelection(Event event, Scrollable control, IPreferenceStore prefs) {
    event.detail &= ~SWT.HOT; // do not draw hover background natively
    if ((event.detail & SWT.SELECTED) == 0) {
      return; // item not selected
    }
    int clientWidth = control.getClientArea().width;
    GC gc = event.gc;
    Color oldFG = gc.getForeground();
    Color oldBG = gc.getBackground();
    Color fgColor = DartUI.getViewerSelectionForeground(prefs, control.getDisplay());
    if (fgColor != null) {
      gc.setForeground(fgColor);
    }
    Color bgColor = DartUI.getViewerSelectionBackground(prefs, control.getDisplay());
    if (bgColor != null) {
      gc.setBackground(bgColor);
    }
    gc.fillRectangle(0, event.y, clientWidth, event.height);
    gc.setForeground(oldFG);
    gc.setBackground(oldBG);
    event.detail &= ~SWT.SELECTED;
  }

  /**
   * Returns a width hint for a button control.
   */
  public static int getButtonWidthHint(Button button) {
    button.setFont(JFaceResources.getDialogFont());
    PixelConverter converter = new PixelConverter(button);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
  }

  /**
   * Get a font for the given <code>device</code> as specified by the given <code>data</code>.
   * <p>
   * This implementation maintains a cache of fonts.
   * 
   * @param device The device the font applies to.
   * @param data The array of font data that defines the characteristics of the desired font.
   * @return The font
   */
  public static Font getFont(Device device, FontData[] data) {
    String name = data[0].getName();
    int style = data[0].getStyle();
    int height = data[0].getHeight();
    StringBuilder keyBuilder = new StringBuilder();
    keyBuilder.append(name).append(';');
    keyBuilder.append(height).append(';');
    keyBuilder.append(style);
    String key = keyBuilder.toString();
    Font font = fontCache.get(key);
    if (font == null || font.isDisposed()) {
      FontData newData = new FontData(name, height, style);
      font = new Font(device, newData);
      fontCache.put(key, font);
    }
    return font;
  }

  /**
   * Returns the shell for the given widget. If the widget doesn't represent a SWT object that
   * manage a shell, <code>null</code> is returned.
   * 
   * @return the shell for the given widget
   */
  public static Shell getShell(Widget widget) {
    if (widget instanceof Control) {
      return ((Control) widget).getShell();
    }
    if (widget instanceof Caret) {
      return ((Caret) widget).getParent().getShell();
    }
    if (widget instanceof DragSource) {
      return ((DragSource) widget).getControl().getShell();
    }
    if (widget instanceof DropTarget) {
      return ((DropTarget) widget).getControl().getShell();
    }
    if (widget instanceof Menu) {
      return ((Menu) widget).getParent().getShell();
    }
    if (widget instanceof ScrollBar) {
      return ((ScrollBar) widget).getParent().getShell();
    }

    return null;
  }

  /**
   * Returns the standard display to be used. The method first checks, if the thread calling this
   * method has an associated disaply. If so, this display is returned. Otherwise the method returns
   * the default display.
   */
  public static Display getStandardDisplay() {
    Display display;
    display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }

  public static int getTableHeightHint(Table table, int rows) {
    if (table.getFont().equals(JFaceResources.getDefaultFont())) {
      table.setFont(JFaceResources.getDialogFont());
    }
    int result = table.getItemHeight() * rows + table.getHeaderHeight();
    if (table.getLinesVisible()) {
      result += table.getGridLineWidth() * (rows - 1);
    }
    return result;
  }

  /**
   * Sets width and height hint for the button control. <b>Note:</b> This is a NOP if the button's
   * layout data is not an instance of <code>GridData</code> .
   * 
   * @param button the button for which to set the dimension hint
   */
  public static void setButtonDimensionHint(Button button) {
    Assert.isNotNull(button);
    Object gd = button.getLayoutData();
    if (gd instanceof GridData) {
      ((GridData) gd).widthHint = getButtonWidthHint(button);
      ((GridData) gd).horizontalAlignment = GridData.FILL;
    }
  }

  public static void setColors(Control ctl, IPreferenceStore store) {
    Color color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
    ctl.setForeground(color);
    color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null
        : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    ctl.setBackground(color);
  }

  public static void setColors(CTabFolder ctl, IPreferenceStore store) {
    Color fgColor = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
    ctl.setForeground(fgColor);
    Color bgColor = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    ctl.setBackground(bgColor);
    Color sfgColor = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND);
    ctl.setSelectionForeground(sfgColor);
    Color sbgColor = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND);
    ctl.setSelectionBackground(sbgColor);
  }

  public static void setColors(List ctl, IPreferenceStore store) {
    Color color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
    ctl.setForeground(color);
    color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null
        : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    ctl.setBackground(color);
//    RGB rgb = PreferenceConverter.getColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
//    ctl.setForeground(DartUI.getColorManager().getColor(rgb));
//    rgb = PreferenceConverter.getColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
//    ctl.setBackground(DartUI.getColorManager().getColor(rgb));
  }

  public static void setColors(StyledText ctl, IPreferenceStore store) {
    Color color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
    ctl.setForeground(color);
    color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null
        : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    ctl.setBackground(color);
    color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND);
    ctl.setSelectionForeground(color);
    color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND);
    ctl.setSelectionBackground(color);
  }

  public static void setColors(Table ctl, IPreferenceStore store) {
    Color color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
    ctl.setForeground(color);
    color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null
        : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    ctl.setBackground(color);
  }

  public static void setColors(Tree ctl, IPreferenceStore store) {
    Color color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
        ? null : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
    ctl.setForeground(color);
    color = store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null
        : createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    ctl.setBackground(color);
  }

  /**
   * Sets the default visible item count for {@link Combo}s. Workaround for
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=7845 .
   * 
   * @param combo the combo
   * @see Combo#setVisibleItemCount(int)
   * @see #COMBO_VISIBLE_ITEM_COUNT
   */
  public static void setDefaultVisibleItemCount(Combo combo) {
    combo.setVisibleItemCount(COMBO_VISIBLE_ITEM_COUNT);
  }

  /**
   * Changes enablement of the given {@link Control} and all its children.
   */
  public static void setEnabledHierarchy(Control control, boolean enable) {
    if (control.getEnabled() != enable) {
      control.setEnabled(enable);
    }
    if (control instanceof Composite) {
      Composite composite = (Composite) control;
      Control[] children = composite.getChildren();
      if (children != null) {
        for (Control child : children) {
          setEnabledHierarchy(child, enable);
        }
      }
    }
  }

  /**
   * Sets the size of the item to correspond size of the font.
   */
  public static void setItemHeightForFont(Control container) {
    GC gc = new GC(container);
    try {
      Point size = gc.textExtent("A");
      ReflectionUtils.invokeMethod(container, "setItemHeight(int)", Math.max(16, size.y));
    } catch (Throwable e) {
    } finally {
      gc.dispose();
    }
  }

  private static Color createColor(IPreferenceStore store, String key) {
    RGB rgb = null;
    if (store.contains(key)) {
      if (store.isDefault(key)) {
        rgb = PreferenceConverter.getDefaultColor(store, key);
      } else {
        rgb = PreferenceConverter.getColor(store, key);
      }
      if (rgb != null) {
        return DartUI.getColorManager().getColor(rgb);
      }
    }
    return null;
  }

  /**
   * Sets size of the font in the given {@link Control} to the size specified in
   * {@link JFaceResources}.
   */
  private static void updateFontSizeFromJFaceResources(Control control) {
    Font newFont = JFaceResources.getFont(FontPreferencePage.VIEW_BASE_FONT_KEY);
    Font oldFont = control.getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    control.setFont(font);
    // set Table/Tree item height
    if (control instanceof Table || control instanceof Tree) {
      setItemHeightForFont(control);
    }
    // process Composite children
    if (control instanceof Composite) {
      Composite composite = (Composite) control;
      Control[] children = composite.getChildren();
      if (children != null) {
        for (Control child : children) {
          updateFontSizeFromJFaceResources(child);
        }
      }
      composite.layout();
    }
  }

  private SWTUtil() {
  }
}
