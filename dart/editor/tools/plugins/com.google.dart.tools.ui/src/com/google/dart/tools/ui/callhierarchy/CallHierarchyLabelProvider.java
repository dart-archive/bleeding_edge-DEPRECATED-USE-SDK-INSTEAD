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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.callhierarchy.CallLocation;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.callhierarchy.RealCallers;
import com.google.dart.tools.ui.internal.viewsupport.AppearanceAwareLabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.ColoringLabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.IWorkbenchAdapter;

import java.util.Collection;

class CallHierarchyLabelProvider extends AppearanceAwareLabelProvider {

  private static final long TEXTFLAGS = DEFAULT_TEXTFLAGS | DartElementLabels.ALL_POST_QUALIFIED
      | DartElementLabels.P_COMPRESSED;

  private static final int IMAGEFLAGS = DEFAULT_IMAGEFLAGS | DartElementImageProvider.SMALL_ICONS;

  private ILabelDecorator decorator;

  CallHierarchyLabelProvider() {
    super(TEXTFLAGS, IMAGEFLAGS);
    decorator = new CallHierarchyLabelDecorator();
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof MethodWrapper) {
      MethodWrapper methodWrapper;
      if (element instanceof RealCallers) {
        methodWrapper = ((RealCallers) element).getParent();
      } else {
        methodWrapper = (MethodWrapper) element;
      }
      DartElement member = methodWrapper.getMember();
      if (member != null) {
        return decorator.decorateImage(super.getImage(member), methodWrapper);
      } else {
        return null;
      }
    } else if (isPendingUpdate(element)) {
      return null;
    } else {
      return super.getImage(element);
    }
  }

  @Override
  public StyledString getStyledText(Object element) {
    if (isNormalMethodWrapper(element)) {
      MethodWrapper wrapper = (MethodWrapper) element;
      String decorated = getElementLabel(wrapper);

      StyledString styledLabel = super.getStyledText(wrapper.getMember());
      StyledString styledDecorated = StyledCellLabelProvider.styleDecoratedString(
          decorated,
          StyledString.COUNTER_STYLER,
          styledLabel);
      if (isSpecialConstructorNode(wrapper)) {
        decorated = Messages.format(
            CallHierarchyMessages.CallHierarchyLabelProvider_constructor_label,
            decorated);
        styledDecorated = StyledCellLabelProvider.styleDecoratedString(
            decorated,
            ColoringLabelProvider.INHERITED_STYLER,
            styledDecorated);
      }
      return styledDecorated;
    }

    String specialLabel = getSpecialLabel(element);
    Styler styler = element instanceof RealCallers ? ColoringLabelProvider.INHERITED_STYLER : null;
    return new StyledString(specialLabel, styler);
  }

  @Override
  public String getText(Object element) {
    if (isNormalMethodWrapper(element)) {
      MethodWrapper wrapper = (MethodWrapper) element;
      String decorated = getElementLabel(wrapper);

      if (isSpecialConstructorNode(wrapper)) {
        decorated = Messages.format(
            CallHierarchyMessages.CallHierarchyLabelProvider_constructor_label,
            decorated);
      }
      return decorated;
    }
    return getSpecialLabel(element);
  }

  private String getElementLabel(MethodWrapper methodWrapper) {
    String label = super.getText(methodWrapper.getMember());

    Collection<CallLocation> callLocations = methodWrapper.getMethodCall().getCallLocations();

    if ((callLocations != null) && (callLocations.size() > 1)) {
      return Messages.format(
          CallHierarchyMessages.CallHierarchyLabelProvider_matches,
          new String[] {label, String.valueOf(callLocations.size())});
    }

    return label;
  }

  private String getSpecialLabel(Object element) {
    if (element instanceof RealCallers) {
      return CallHierarchyMessages.CallHierarchyLabelProvider_expandWithConstructorsAction_realCallers;
    } else if (element instanceof MethodWrapper) {
      return CallHierarchyMessages.CallHierarchyLabelProvider_root;
    } else if (element == TreeTermination.SEARCH_CANCELED) {
      return CallHierarchyMessages.CallHierarchyLabelProvider_searchCanceled;
    } else if (isPendingUpdate(element)) {
      return CallHierarchyMessages.CallHierarchyLabelProvider_updatePending;
    }
    return CallHierarchyMessages.CallHierarchyLabelProvider_noMethodSelected;
  }

  private boolean isNormalMethodWrapper(Object element) {
    return element instanceof MethodWrapper && ((MethodWrapper) element).getMember() != null
        && !(element instanceof RealCallers);
  }

  private boolean isPendingUpdate(Object element) {
    return element instanceof IWorkbenchAdapter;
  }

  private boolean isSpecialConstructorNode(MethodWrapper wrapper) {
    MethodWrapper parentWrapper = wrapper.getParent();
    if (!CallHierarchyContentProvider.isExpandWithConstructors(parentWrapper)) {
      return false;
    }

    DartElement member = wrapper.getMember();
    if (member instanceof Type) {
      return true;
    }

    return member instanceof Method && ((Method) member).isConstructor();
  }
}
