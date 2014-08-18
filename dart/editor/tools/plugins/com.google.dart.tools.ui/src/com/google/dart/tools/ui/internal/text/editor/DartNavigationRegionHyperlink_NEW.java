/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.generated.types.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;

/**
 * A hyperlink for {@link NavigationRegion}.
 */
public class DartNavigationRegionHyperlink_NEW implements IHyperlink {
  private final IFile context;
  private final NavigationRegion region;

  public DartNavigationRegionHyperlink_NEW(IFile context, NavigationRegion region) {
    this.context = context;
    this.region = region;
    Assert.isNotNull(region);
  }

  @Override
  public IRegion getHyperlinkRegion() {
    return new Region(region.getOffset(), region.getLength());
  }

  @Override
  public String getHyperlinkText() {
    return "Open Declaration";
  }

  @Override
  public String getTypeLabel() {
    return null;
  }

  @Override
  public void open() {
    InstrumentationBuilder instrumentation = Instrumentation.builder(this.getClass());
    try {
      Element[] targets = region.getTargets();
      // Server API has changed, Element not returned in getTargets anymore
      if (targets.length != 0) {
        throw new IllegalStateException("Not yet implemented: cannot open NavigationTargets.");
//        DartUI.openInEditor(context, targets[0]);
      }
      instrumentation.metric("Run", "Completed");
    } catch (Throwable e) {
      instrumentation.record(e);
    } finally {
      instrumentation.log();
    }

  }
}
