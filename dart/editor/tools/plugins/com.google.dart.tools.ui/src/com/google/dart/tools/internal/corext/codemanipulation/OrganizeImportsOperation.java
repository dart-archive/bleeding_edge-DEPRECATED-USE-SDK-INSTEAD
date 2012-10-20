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

package com.google.dart.tools.internal.corext.codemanipulation;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Organize Imports runnable.
 * 
 * @coverage dart.editor.ui.code_manipulation
 */
public class OrganizeImportsOperation implements IWorkspaceRunnable {

  private final CompilationUnit unit;
  private final String unitSource;

  public OrganizeImportsOperation(CompilationUnit unit) throws Exception {
    this.unit = unit;
    this.unitSource = unit.getSource();
  }

  public TextEdit createTextEdit(IProgressMonitor monitor) throws CoreException,
      OperationCanceledException {
    try {
      monitor.beginTask(
          NLS.bind(
              DartUIMessages.OrganizeImportsOperation_description,
              BasicElementLabels.getFileName(unit)),
          4);
      // only library unit can be processed
      DartLibrary library = unit.getLibrary();
      if (library == null || !Objects.equal(library.getDefiningCompilationUnit(), unit)) {
        return null;
      }
      // prepare environment
      ExtractUtils utils = new ExtractUtils(unit);
      DartUnit unitNode = utils.getUnitNode();
      String eol = utils.getEndOfLine();
      // prepare imports
      List<DartImportDirective> imports = Lists.newArrayList();
      for (DartDirective directive : unitNode.getDirectives()) {
        if (directive instanceof DartImportDirective) {
          imports.add((DartImportDirective) directive);
        }
      }
      if (imports.size() == 0) {
        return null;
      }
      int firstImportOffset = imports.get(0).getSourceInfo().getOffset();
      monitor.worked(1);
      // remove old imports
      MultiTextEdit multiTextEdit = new MultiTextEdit();
      for (DartImportDirective imp : imports) {
        SourceRange range = SourceRangeFactory.create(imp);
        int offset = range.getOffset();
        int length = range.getLength();
        length = utils.getTokenOrNextLineOffset(offset + length) - offset;
        multiTextEdit.addChild(new ReplaceEdit(offset, length, ""));
      }
      monitor.worked(1);
      // sort imports
      Collections.sort(imports, new Comparator<DartImportDirective>() {
        @Override
        public int compare(DartImportDirective o1, DartImportDirective o2) {
          String uri1 = getUriSource(o1);
          String uri2 = getUriSource(o2);
          int cat1 = getUriCategory(uri1);
          int cat2 = getUriCategory(uri2);
          if (cat1 != cat2) {
            return cat1 - cat2;
          }
          return uri1.compareTo(uri2);
        }

        private int getUriCategory(String uri) {
          if (uri.startsWith("dart:")) {
            return 0;
          }
          if (uri.startsWith("package:")) {
            return 0;
          }
          return Integer.MAX_VALUE;
        }

        private String getUriSource(DartImportDirective imp) {
          DartStringLiteral uriLiteral = imp.getLibraryUri();
          SourceRange range = SourceRangeFactory.create(uriLiteral);
          String source = getSource(range);
          source = StringUtils.strip(source, "'");
          source = StringUtils.strip(source, "\"");
          return source;
        }
      });
      monitor.worked(1);
      // write sorted imports
      for (DartImportDirective imp : imports) {
        SourceRange range = SourceRangeFactory.create(imp);
        String source = getSource(range);
        multiTextEdit.addChild(new ReplaceEdit(firstImportOffset, 0, source + eol));
      }
      monitor.worked(1);
      // done
      return multiTextEdit;
    } finally {
      monitor.done();
    }
  }

  public ISchedulingRule getScheduleRule() {
    return unit.getResource();
  }

  @Override
  public void run(IProgressMonitor monitor) throws CoreException {
    try {
      monitor.beginTask(
          NLS.bind(
              DartUIMessages.OrganizeImportsOperation_description,
              BasicElementLabels.getFileName(unit)),
          5);
      // prepare TextEdit
      TextEdit edit = createTextEdit(new SubProgressMonitor(monitor, 4));
      if (edit == null) {
        return;
      }
      // apply TextEdit
      boolean doSave = !unit.isWorkingCopy();
      DartModelUtil.applyEdit(unit, edit, doSave, new SubProgressMonitor(monitor, 1));
    } finally {
      monitor.done();
    }

  }

  /**
   * @return the {@link SourceRange} from {@link #unitSource}.
   */
  private String getSource(SourceRange range) {
    int start = range.getOffset();
    int end = start + range.getLength();
    return unitSource.substring(start, end);
  }

}
