/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.internal.corext.refactoring.base;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.server.generated.types.Location;
import com.google.dart.tools.ui.internal.refactoring.WorkbenchSourceAdapter_NEW;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ui.model.IWorkbenchAdapter;

import java.io.File;

/**
 * {@link DartStatusContext_NEW} is the wrapper of the {@link Source} and {@link SourceRange} in it
 * where some problem was detected.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class DartStatusContext_NEW extends RefactoringStatusContext implements IAdaptable {
  private final String file;
  private String content;
  private IRegion region;

  public DartStatusContext_NEW(Location location) {
    file = location.getFile();
    Assert.isNotNull(file);
    try {
      this.content = Files.toString(new File(file), Charsets.UTF_8);
    } catch (Throwable e) {
      this.content = e.getMessage();
    }
    this.region = new Region(location.getOffset(), location.getLength());
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter) {
    if (adapter == IWorkbenchAdapter.class) {
      return new WorkbenchSourceAdapter_NEW(file);
    }
    return null;
  }

  public String getContent() {
    return content;
  }

  @Override
  public Object getCorrespondingElement() {
    return null;
  }

  public IRegion getRegion() {
    return region;
  }
}
