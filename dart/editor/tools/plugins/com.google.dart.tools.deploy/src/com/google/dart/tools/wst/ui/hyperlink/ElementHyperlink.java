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

package com.google.dart.tools.wst.ui.hyperlink;

import com.google.dart.engine.element.Element;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

/**
 * {@link IHyperlink} that opens {@link Element}.
 */
public class ElementHyperlink implements IHyperlink {
  private final IRegion region;
  private final Element element;

  public ElementHyperlink(IRegion region, Element element) {
    this.region = region;
    this.element = element;
  }

  @Override
  public IRegion getHyperlinkRegion() {
    return region;
  }

  @Override
  public String getHyperlinkText() {
    return null;
  }

  @Override
  public String getTypeLabel() {
    return "Hyperlink";
  }

  @Override
  public void open() {
    try {
      DartUI.openInEditor(element);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }
}
