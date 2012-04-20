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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartElementLabels;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * Search label provider that supports sorting.
 */
public class SortingLabelProvider extends SearchLabelProvider {

  public static final int SHOW_CONTAINER_ELEMENT = 2;
  public static final int SHOW_ELEMENT_CONTAINER = 1; // default
  public static final int SHOW_PATH = 3;

  private static final long FLAGS_QUALIFIED = DEFAULT_SEARCH_TEXTFLAGS
      | DartElementLabels.F_FULLY_QUALIFIED | DartElementLabels.M_FULLY_QUALIFIED
      | DartElementLabels.I_FULLY_QUALIFIED | DartElementLabels.T_FULLY_QUALIFIED
      | DartElementLabels.D_QUALIFIED | DartElementLabels.CF_QUALIFIED
      | DartElementLabels.CU_QUALIFIED | DartElementLabels.COLORIZE;

  private int currentOrder;

  public SortingLabelProvider(DartSearchResultPage page) {
    super(page);
    currentOrder = SHOW_ELEMENT_CONTAINER;
  }

  @Override
  public Image getImage(Object element) {
    Image image = null;
    if (element instanceof DartElement || element instanceof IResource) {
      image = super.getImage(element);
    }
    if (image != null) {
      return image;
    }
    return getParticipantImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    StyledString text = super.getStyledText(element);
    if (text.length() > 0) {
      StyledString countLabel = getColoredLabelWithCounts(element, text);
      if (currentOrder == SHOW_ELEMENT_CONTAINER) {
        countLabel.append(getPostQualification(element), StyledString.QUALIFIER_STYLER);
      }
      return countLabel;
    }
    return getStyledParticipantText(element);
  }

  @Override
  public final String getText(Object element) {
    String text = super.getText(element);
    if (text.length() > 0) {
      String labelWithCount = getLabelWithCounts(element, text);
      if (currentOrder == SHOW_ELEMENT_CONTAINER) {
        labelWithCount += getPostQualification(element);
      }
      return labelWithCount;
    }
    return getParticipantText(element);
  }

  public void setOrder(int orderFlag) {
    currentOrder = orderFlag;
    long flags = 0;
    if (orderFlag == SHOW_ELEMENT_CONTAINER) {
      flags = DEFAULT_SEARCH_TEXTFLAGS;
    } else if (orderFlag == SHOW_CONTAINER_ELEMENT) {
      flags = FLAGS_QUALIFIED;
    } else if (orderFlag == SHOW_PATH) {
      flags = FLAGS_QUALIFIED | DartElementLabels.PREPEND_ROOT_PATH;
    }
    setTextFlags(flags);
  }

  private String getPostQualification(Object element) {
    String textLabel = DartElementLabels.getTextLabel(element, DartElementLabels.ALL_POST_QUALIFIED);
    int indexOf = textLabel.indexOf(DartElementLabels.CONCAT_STRING);
    if (indexOf != -1) {
      return textLabel.substring(indexOf);
    }
    return new String();
  }
}
