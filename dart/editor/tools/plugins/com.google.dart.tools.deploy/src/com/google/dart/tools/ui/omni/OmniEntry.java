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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.omni.elements.HeaderElement;
import com.google.dart.tools.ui.omni.elements.TypeProvider_OLD.SearchInProgressPlaceHolder;
import com.google.dart.tools.ui.themes.Fonts;

import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;

/**
 * Abstract base class of omnibox entries.
 */
@SuppressWarnings("restriction")
public class OmniEntry {

  boolean firstInCategory;
  boolean lastInCategory;
  OmniElement element;
  OmniProposalProvider provider;
  int[][] elementMatchRegions;
  int[][] providerMatchRegions;

  public OmniEntry(OmniElement element, OmniProposalProvider provider, int[][] elementMatchRegions,
      int[][] providerMatchRegions) {
    this.element = element;
    this.provider = provider;
    this.elementMatchRegions = elementMatchRegions;
    this.providerMatchRegions = providerMatchRegions;
  }

  public void erase(Event event) {
    // We are only custom drawing the foreground.
    event.detail &= ~SWT.FOREGROUND;
  }

  public OmniElement getElement() {
    return element;
  }

  public void measure(Event event, TextLayout textLayout, ResourceManager resourceManager,
      TextStyle boldStyle) {
    Table table = ((TableItem) event.item).getParent();
    textLayout.setFont(table.getFont());
    event.width = 0;
    switch (event.index) {
      case 0:
        textLayout.setText(""); //$NON-NLS-1$
        break;
      case 1:
        Image image = getImage(element, resourceManager);
        Rectangle imageRect = image.getBounds();
        event.width += imageRect.width + 4;
        event.height = Math.max(event.height, imageRect.height + 2);
        textLayout.setText(element.getLabel());
        if (boldStyle != null) {
          for (int i = 0; i < elementMatchRegions.length; i++) {
            int[] matchRegion = elementMatchRegions[i];
            textLayout.setStyle(boldStyle, matchRegion[0], matchRegion[1]);
          }
        }
        break;
    }
    Rectangle rect = textLayout.getBounds();
    event.width += rect.width + 4;
    event.height = Math.max(event.height, rect.height + 2);
  }

  public void paint(Event event, TextLayout textLayout, ResourceManager resourceManager,
      TextStyle boldStyle, TextStyle grayStyle) {
    final Table table = ((TableItem) event.item).getParent();

    if (element instanceof SearchInProgressPlaceHolder) {
      textLayout.setFont(Fonts.getItalicFont(table.getFont()));
    } else {
      textLayout.setFont(table.getFont());
    }

    switch (event.index) {
      case 0:
        break;
      case 1:
        String label = element.getLabel();

        Image image = null;
        int xNudge = 1;

        if (!(element instanceof HeaderElement)) {
          xNudge = 9; //indent
          image = getImage(element, resourceManager);
          event.gc.drawImage(image, event.x + xNudge, event.y + 1);
        } else {
          //a lighter gray
          event.gc.setForeground(OmniBoxColors.SEARCH_ENTRY_HEADER_TEXT);
        }

        textLayout.setText(label);

        //match emphasis
        if (boldStyle != null) {
          for (int i = 0; i < elementMatchRegions.length; i++) {
            int[] matchRegion = elementMatchRegions[i];
            textLayout.setStyle(boldStyle, matchRegion[0], matchRegion[1]);
          }
        }
        //details emphasis
        if (grayStyle != null) {
          int detailOffset = element.getDetailOffset();
          if (detailOffset != -1) {
            textLayout.setStyle(grayStyle, detailOffset, label.length() - 1);
          }

        }

        Rectangle availableBounds = ((TableItem) event.item).getTextBounds(event.index);
        Rectangle requiredBounds = textLayout.getBounds();
        int imageWidth = image == null ? 0 : image.getBounds().width;
        textLayout.draw(event.gc, availableBounds.x + xNudge + imageWidth, availableBounds.y
            + (availableBounds.height - requiredBounds.height) / 2);
        break;
    }
    if (lastInCategory) {
      //a lighter gray
      event.gc.setForeground(OmniBoxColors.SEARCH_ENTRY_HEADER_TEXT);
      Rectangle bounds = ((TableItem) event.item).getBounds(event.index);
      event.gc.drawLine(Math.max(0, bounds.x - 1), bounds.y + bounds.height, bounds.x
          + bounds.width, bounds.y + bounds.height);
    }
  }

  Image getImage(OmniElement element, ResourceManager resourceManager) {
    Image image = findOrCreateImage(element.getImageDescriptor(), resourceManager);
    if (image == null) {
      //TODO (pquitslund): replace with a new "generic node" image
      image = WorkbenchImages.getImage(IWorkbenchGraphicConstants.IMG_OBJ_ELEMENT);
    }
    return image;
  }

  private Image findOrCreateImage(ImageDescriptor imageDescriptor, ResourceManager resourceManager) {
    if (imageDescriptor == null) {
      return null;
    }
    Image image = (Image) resourceManager.find(imageDescriptor);
    if (image == null) {
      try {
        image = resourceManager.createImage(imageDescriptor);
      } catch (DeviceResourceException e) {
        DartToolsPlugin.log(e);
      }
    }
    return image;
  }
}
