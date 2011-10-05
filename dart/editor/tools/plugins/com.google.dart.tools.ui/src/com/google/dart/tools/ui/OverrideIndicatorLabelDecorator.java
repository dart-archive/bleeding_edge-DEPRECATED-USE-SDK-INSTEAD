/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;
import com.google.dart.tools.ui.internal.viewsupport.ImageImageDescriptor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * LabelDecorator that decorates an method's image with override or implements overlays. The viewer
 * using this decorator is responsible for updating the images on element changes.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class OverrideIndicatorLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {

  private ImageDescriptorRegistry fRegistry;
  private boolean fUseNewRegistry = false;

  /**
   * Creates a decorator. The decorator creates an own image registry to cache images.
   */
  public OverrideIndicatorLabelDecorator() {
    this(null);
    fUseNewRegistry = true;
  }

  /*
   * Creates decorator with a shared image registry.
   * 
   * @param registry The registry to use or <code>null</code> to use the JavaScript plugin's image
   * registry.
   */
  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param registry The registry to use.
   */
  public OverrideIndicatorLabelDecorator(ImageDescriptorRegistry registry) {
    fRegistry = registry;
  }

  /*
   * (non-Javadoc)
   * 
   * @see IBaseLabelProvider#addListener(ILabelProviderListener)
   */
  @Override
  public void addListener(ILabelProviderListener listener) {
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   * 
   * @param element The element to decorate
   * @return Resulting decorations (combination of DartElementImageDescriptor.IMPLEMENTS and
   *         DartElementImageDescriptor.OVERRIDES)
   */
  public int computeAdornmentFlags(Object element) {
    if (element instanceof Method) {
      try {
        Method method = (Method) element;
//        if (!method.getDartProject().isOnIncludepath(method)) {
//          return 0;
//        }
//        int flags = method.getFlags();
        if (!method.isConstructor() /* && !Flags.isPrivate(flags) */
            && !method.isStatic()) {
          int res = getOverrideIndicators(method);
          return res;
        }
      } catch (DartModelException e) {
        if (!e.isDoesNotExist()) {
          DartToolsPlugin.log(e);
        }
      }
    }
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang .Object,
   * org.eclipse.jface.viewers.IDecoration)
   */
  @Override
  public void decorate(Object element, IDecoration decoration) {
    int adornmentFlags = computeAdornmentFlags(element);
    if ((adornmentFlags & DartElementImageDescriptor.IMPLEMENTS) != 0) {
      if ((adornmentFlags & DartElementImageDescriptor.SYNCHRONIZED) != 0) {
        decoration.addOverlay(DartPluginImages.DESC_OVR_SYNCH_AND_IMPLEMENTS);
      } else {
        decoration.addOverlay(DartPluginImages.DESC_OVR_IMPLEMENTS);
      }
    } else if ((adornmentFlags & DartElementImageDescriptor.OVERRIDES) != 0) {
      if ((adornmentFlags & DartElementImageDescriptor.SYNCHRONIZED) != 0) {
        decoration.addOverlay(DartPluginImages.DESC_OVR_SYNCH_AND_OVERRIDES);
      } else {
        decoration.addOverlay(DartPluginImages.DESC_OVR_OVERRIDES);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see ILabelDecorator#decorateImage(Image, Object)
   */
  @Override
  public Image decorateImage(Image image, Object element) {
    int adornmentFlags = computeAdornmentFlags(element);
    if (adornmentFlags != 0) {
      ImageDescriptor baseImage = new ImageImageDescriptor(image);
      Rectangle bounds = image.getBounds();
      return getRegistry().get(
          new DartElementImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width,
              bounds.height)));
    }
    return image;
  }

  /*
   * (non-Javadoc)
   * 
   * @see ILabelDecorator#decorateText(String, Object)
   */
  @Override
  public String decorateText(String text, Object element) {
    return text;
  }

  /*
   * (non-Javadoc)
   * 
   * @see IBaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    if (fRegistry != null && fUseNewRegistry) {
      fRegistry.dispose();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see IBaseLabelProvider#isLabelProperty(Object, String)
   */
  @Override
  public boolean isLabelProperty(Object element, String property) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
   */
  @Override
  public void removeListener(ILabelProviderListener listener) {
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   * 
   * @param method The element to decorate
   * @return Resulting decorations (combination of DartElementImageDescriptor.IMPLEMENTS and
   *         DartElementImageDescriptor.OVERRIDES)
   * @throws DartModelException
   */
  protected int getOverrideIndicators(Method method) throws DartModelException {
    DartX.todo();
//    DartUnit astRoot = DartToolsPlugin.getDefault().getASTProvider().getAST(
//        (DartElement) method.getOpenable(), ASTProvider.WAIT_ACTIVE_ONLY, null);
//    if (astRoot != null) {
//      int res = findInHierarchyWithAST(astRoot, method);
//      if (res != -1) {
//        return res;
//      }
//    }
//
//    Type type = method.getDeclaringType();
//    if (type == null)
//      return 0;
//
//    MethodOverrideTester methodOverrideTester = SuperTypeHierarchyCache.getMethodOverrideTester(type);
//    IFunction defining = methodOverrideTester.findOverriddenMethod(method, true);
//    if (defining != null) {
//      if (JdtFlags.isAbstract(defining)) {
//        return DartElementImageDescriptor.IMPLEMENTS;
//      } else {
//        return DartElementImageDescriptor.OVERRIDES;
//      }
//    }
    return 0;
  }

  @SuppressWarnings("unused")
  private int findInHierarchyWithAST(DartUnit astRoot, Method method) throws DartModelException {
    DartX.todo();
//    DartNode node = NodeFinder.perform(astRoot, method.getNameRange());
//    if (node instanceof SimpleName
//        && node.getParent() instanceof FunctionDeclaration) {
//      IFunctionBinding binding = ((FunctionDeclaration) node.getParent()).resolveBinding();
//      if (binding != null) {
//        IFunctionBinding defining = Bindings.findOverriddenMethod(binding, true);
//        if (defining != null) {
//          if (JdtFlags.isAbstract(defining)) {
//            return DartElementImageDescriptor.IMPLEMENTS;
//          } else {
//            return DartElementImageDescriptor.OVERRIDES;
//          }
//        }
//        return 0;
//      }
//    }
    return -1;
  }

  private ImageDescriptorRegistry getRegistry() {
    if (fRegistry == null) {
      fRegistry = fUseNewRegistry ? new ImageDescriptorRegistry()
          : DartToolsPlugin.getImageDescriptorRegistry();
    }
    return fRegistry;
  }

}
