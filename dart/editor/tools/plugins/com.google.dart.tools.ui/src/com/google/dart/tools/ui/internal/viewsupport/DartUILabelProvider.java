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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.DartElementLabels;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;

public class DartUILabelProvider implements IColorProvider, IRichLabelProvider,
    IStyledLabelProvider {

  public static ILabelDecorator[] getDecorators(boolean errortick, ILabelDecorator extra) {
    if (errortick) {
      if (extra == null) {
        return new ILabelDecorator[] {};
      } else {
        return new ILabelDecorator[] {extra};
      }
    }
    if (extra != null) {
      return new ILabelDecorator[] {extra};
    }
    return null;
  }

  protected ListenerList fListeners = new ListenerList();
  protected DartElementImageProvider fImageLabelProvider;

  protected StorageLabelProvider fStorageLabelProvider;

  private ArrayList<ILabelDecorator> fLabelDecorators;
  private int fImageFlags;

  private long fTextFlags;

  /**
   * Creates a new label provider with default flags.
   */
  public DartUILabelProvider() {
    this(DartElementLabels.ALL_DEFAULT, DartElementImageProvider.OVERLAY_ICONS);
  }

  /**
   * @param textFlags Flags defined in <code>DartElementLabels</code>.
   * @param imageFlags Flags defined in <code>DartElementImageProvider</code>.
   */
  public DartUILabelProvider(long textFlags, int imageFlags) {
    fImageLabelProvider = new DartElementImageProvider();
    fLabelDecorators = null;

    fStorageLabelProvider = new StorageLabelProvider();
    fImageFlags = imageFlags;
    fTextFlags = textFlags;
  }

  /**
   * Adds a decorator to the label provider
   * 
   * @param decorator the decorator to add
   */
  public void addLabelDecorator(ILabelDecorator decorator) {
    if (fLabelDecorators == null) {
      fLabelDecorators = new ArrayList<ILabelDecorator>(2);
    }
    fLabelDecorators.add(decorator);
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    if (fLabelDecorators != null) {
      for (int i = 0; i < fLabelDecorators.size(); i++) {
        ILabelDecorator decorator = fLabelDecorators.get(i);
        decorator.addListener(listener);
      }
    }
    fListeners.add(listener);
  }

  @Override
  public void dispose() {
    if (fLabelDecorators != null) {
      for (int i = 0; i < fLabelDecorators.size(); i++) {
        ILabelDecorator decorator = fLabelDecorators.get(i);
        decorator.dispose();
      }
      fLabelDecorators = null;
    }
    fStorageLabelProvider.dispose();
    fImageLabelProvider.dispose();
  }

  @Override
  public Color getBackground(Object element) {
    return null;
  }

  @Override
  public Color getForeground(Object element) {
    return null;
  }

  @Override
  public Image getImage(Object element) {
    Image result = fImageLabelProvider.getImageLabel(element, evaluateImageFlags(element));
    if (result == null && (element instanceof IStorage)) {
      result = fStorageLabelProvider.getImage(element);
    }

    return decorateImage(result, element);
  }

  /**
   * Gets the image flags. Can be overwritten by super classes.
   * 
   * @return Returns a int
   */
  public final int getImageFlags() {
    return fImageFlags;
  }

  @Override
  public ColoredString getRichTextLabel(Object element) {
    ColoredString string = ColoredDartElementLabels.getTextLabel(
        element,
        evaluateTextFlags(element) | ColoredDartElementLabels.COLORIZE);
    if (string.length() == 0 && (element instanceof IStorage)) {
      string = new ColoredString(fStorageLabelProvider.getText(element));
    }
    String decorated = decorateText(string.getString(), element);
    if (decorated != null) {
      return ColoredDartElementLabels.decorateColoredString(
          string,
          decorated,
          ColoredDartElementLabels.DECORATIONS_STYLE);
    }
    return string;
  }

  @Override
  public StyledString getStyledText(Object element) {
    StyledString string = DartElementLabels.getStyledTextLabel(
        element,
        (evaluateTextFlags(element) | DartElementLabels.COLORIZE));
    if (string.length() == 0 && (element instanceof IStorage)) {
      string = new StyledString(fStorageLabelProvider.getText(element));
    }
    String decorated = decorateText(string.getString(), element);
    if (decorated != null) {
      return StyledCellLabelProvider.styleDecoratedString(
          decorated,
          StyledString.DECORATIONS_STYLER,
          string);
    }
    return string;
  }

  @Override
  public String getText(Object element) {
    String result = DartElementLabels.getTextLabel(element, evaluateTextFlags(element));
    if (result.length() == 0 && (element instanceof IStorage)) {
      result = fStorageLabelProvider.getText(element);
    }

    return decorateText(result, element);
  }

  /**
   * Gets the text flags.
   * 
   * @return Returns a int
   */
  public final long getTextFlags() {
    return fTextFlags;
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return true;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    if (fLabelDecorators != null) {
      for (int i = 0; i < fLabelDecorators.size(); i++) {
        ILabelDecorator decorator = fLabelDecorators.get(i);
        decorator.removeListener(listener);
      }
    }
    fListeners.remove(listener);
  }

  /**
   * Sets the imageFlags
   * 
   * @param imageFlags The imageFlags to set
   */
  public final void setImageFlags(int imageFlags) {
    fImageFlags = imageFlags;
  }

  /**
   * Sets the textFlags.
   * 
   * @param textFlags The textFlags to set
   */
  public final void setTextFlags(long textFlags) {
    fTextFlags = textFlags;
  }

  protected Image decorateImage(Image image, Object element) {
    if (fLabelDecorators != null && image != null) {
      for (int i = 0; i < fLabelDecorators.size(); i++) {
        ILabelDecorator decorator = fLabelDecorators.get(i);
        image = decorator.decorateImage(image, element);
      }
    }
    return image;
  }

  protected String decorateText(String text, Object element) {
    if (fLabelDecorators != null && text.length() > 0) {
      for (int i = 0; i < fLabelDecorators.size(); i++) {
        ILabelDecorator decorator = fLabelDecorators.get(i);
        String decorated = decorator.decorateText(text, element);
        if (decorated != null) {
          text = decorated;
        }
      }
    }
    return text;
  }

  /**
   * Evaluates the image flags for a element. Can be overwritten by super classes.
   * 
   * @param element the element to compute the image flags for
   * @return Returns a int
   */
  protected int evaluateImageFlags(Object element) {
    return getImageFlags();
  }

  /**
   * Evaluates the text flags for a element. Can be overwritten by super classes.
   * 
   * @param element the element to compute the text flags for
   * @return Returns a int
   */
  protected long evaluateTextFlags(Object element) {
    return getTextFlags();
  }

  /**
   * Fires a label provider changed event to all registered listeners Only listeners registered at
   * the time this method is called are notified.
   * 
   * @param event a label provider changed event
   * @see ILabelProviderListener#labelProviderChanged
   */
  protected void fireLabelProviderChanged(final LabelProviderChangedEvent event) {
    Object[] listeners = fListeners.getListeners();
    for (int i = 0; i < listeners.length; ++i) {
      final ILabelProviderListener l = (ILabelProviderListener) listeners[i];
      SafeRunner.run(new SafeRunnable() {
        @Override
        public void run() {
          l.labelProviderChanged(event);
        }
      });
    }
  }

}
