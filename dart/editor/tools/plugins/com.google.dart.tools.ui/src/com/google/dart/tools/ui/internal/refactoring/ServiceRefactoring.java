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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.internal.corext.refactoring.base.StringStatusContext;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_OLD.createCoreException;
import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_OLD.toLTK;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * LTK wrapper around Engine Services {@link Refactoring}.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServiceRefactoring extends org.eclipse.ltk.core.refactoring.Refactoring {
  private final Refactoring refactoring;
  private final Set<Source> unsafeSources = Sets.newHashSet();

  public ServiceRefactoring(Refactoring refactoring) {
    this.refactoring = refactoring;
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkFinalConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    try {
      ProgressMonitor spm = new ServiceProgressMonitor(pm);
      RefactoringStatus status = refactoring.checkFinalConditions(spm);
      return toLTK(status);
    } catch (Throwable e) {
      DartCore.logError("Exception in ServiceRefactoring.checkFinalConditions", e);
      return toLTK(e);
    }
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkInitialConditions(
      IProgressMonitor pm) throws CoreException, OperationCanceledException {
    try {
      ProgressMonitor spm = new ServiceProgressMonitor(pm);
      RefactoringStatus status = refactoring.checkInitialConditions(spm);
      org.eclipse.ltk.core.refactoring.RefactoringStatus ltkStatus = toLTK(status);
      checkUnsafeSources(ltkStatus);
      return ltkStatus;
    } catch (Throwable e) {
      DartCore.logError("Exception in ServiceRefactoring.checkInitialConditions", e);
      return toLTK(e);
    }
  }

  @Override
  public org.eclipse.ltk.core.refactoring.Change createChange(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    try {
      ProgressMonitor spm = new ServiceProgressMonitor(pm);
      Change change = refactoring.createChange(spm);
      change = removeChangesForUnsafeSources(change);
      return toLTK(change);
    } catch (Throwable e) {
      DartCore.logError("Exception in ServiceRefactoring.createChange", e);
      throw createCoreException(e);
    }
  }

  @Override
  public String getName() {
    return refactoring.getRefactoringName();
  }

  /**
   * @return {@code true} if the {@link Change} created by refactoring may be unsafe, so we want
   *         user to review the change to ensure that he understand it.
   */
  public boolean requiresPreview() {
    return refactoring.requiresPreview();
  }

  /**
   * @return {@code true} if given {@link Source} may be affected by this refactoring, so we should
   *         warn user about it.
   */
  protected boolean shouldReportUnsafeRefactoringSource(AnalysisContext context, Source source) {
    return true;
  }

  /**
   * Checks if all {@link AnalysisContext} are actually fully analyzed, so it is safe to perform
   * refactoring. Otherwise updates given LTK refactoring status.
   */
  private void checkUnsafeSources(org.eclipse.ltk.core.refactoring.RefactoringStatus ltkStatus) {
    unsafeSources.clear();
    // prepare contexts
    Map<AnalysisContext, Project> contextToProject = Maps.newHashMap();
    for (Project project : DartCore.getProjectManager().getProjects()) {
      // default context
      AnalysisContext defaultContext = project.getDefaultContext();
      contextToProject.put(defaultContext, project);
      // separate Pub folders
      for (PubFolder pubFolder : project.getPubFolders()) {
        AnalysisContext context = pubFolder.getContext();
        if (context != defaultContext) {
          contextToProject.put(context, project);
        }
      }
    }
    // prepare description for unsafe sources
    StringBuilder sb = new StringBuilder();
    for (Entry<AnalysisContext, Project> entry : contextToProject.entrySet()) {
      AnalysisContext context = entry.getKey();
      Project project = entry.getValue();
      Source[] sources = context.getRefactoringUnsafeSources();
      // all sources are ready
      if (sources.length == 0) {
        continue;
      }
      // remember
      Collections.addAll(unsafeSources, sources);
      // append project and its sources
      boolean firstSource = true;
      for (Source source : sources) {
        if (!shouldReportUnsafeRefactoringSource(context, source)) {
          continue;
        }
        if (firstSource) {
          sb.append(project.getResource().getName() + "=[");
          firstSource = false;
        }
        sb.append(source.getFullName());
        sb.append(" ");
      }
      if (sb.length() != 0) {
        sb.setLength(sb.length() - 1);
        sb.append("]\n");
      }
    }
    // show unsafe sources
    if (sb.length() != 0) {
      String sourcesText = sb.toString();
      String msg = "Analysis of these sources is not up to date and they will be\n"
          + "excluded from the refactoring:\n\n" + sourcesText;
      DartCore.logInformation(sourcesText);
      ltkStatus.addWarning(
          unsafeSources.size() + " source(s) have not been analyzed",
          new StringStatusContext("Sources that have not been analyzed:", msg));
    }
  }

  /**
   * Checks if given {@link Change} is for one of the unsafe {@link Source}s and returns empty
   * {@link Change} or contains such {@link Change} and should be recreated without it.
   */
  private Change removeChangesForUnsafeSources(Change change) {
    // SourceChange - may be exclude it
    if (change instanceof SourceChange) {
      Source source = ((SourceChange) change).getSource();
      if (unsafeSources.contains(source)) {
        return new SourceChange(change.getName(), source);
      }
      return change;
    }
    // CompositeChange - may be exclude some of its children
    if (change instanceof CompositeChange) {
      CompositeChange compositeChange = (CompositeChange) change;
      List<Change> newChildren = Lists.newArrayList();
      for (Change child : compositeChange.getChildren()) {
        Change newChild = removeChangesForUnsafeSources(child);
        if (newChild != null) {
          newChildren.add(newChild);
        }
      }
      return new CompositeChange(compositeChange.getName(), newChildren);
    }
    // some other Change
    return change;
  }
}
