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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.tools.internal.corext.codemanipulation.OrganizeImportsOperation;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.AbstractMigrateCleanUp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.text.edits.TextEdit;

import java.util.Map;

/**
 * {@link ICleanUp} wrapper around {@link OrganizeImportsOperation}.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class ImportsCleanUp extends AbstractMigrateCleanUp {
  public ImportsCleanUp(Map<String, String> settings) {
  }

  @Override
  protected void createFix() throws Exception {
    IProgressMonitor pm = new NullProgressMonitor();
    TextEdit textEdit = new OrganizeImportsOperation(unit).createTextEdit(pm);
    if (textEdit != null) {
      change.addEdit(textEdit);
    }
  }
}
