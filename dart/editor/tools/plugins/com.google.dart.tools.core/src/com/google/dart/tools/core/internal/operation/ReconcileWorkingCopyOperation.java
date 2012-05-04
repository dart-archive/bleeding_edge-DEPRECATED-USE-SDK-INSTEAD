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
package com.google.dart.tools.core.internal.operation;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.internal.model.PerWorkingCopyInfo;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaBuilder;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.internal.problem.CategorizedProblem;
import com.google.dart.tools.core.internal.problem.DefaultProblem;
import com.google.dart.tools.core.internal.problem.ProblemSeverities;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.problem.ProblemRequestor;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Instances of the class <code>ReconcileWorkingCopyOperation</code> implement an operation that
 * will reconcile a working copy and signal the changes through a delta.
 * <p>
 * High level summary of what a reconcile does:
 * <ul>
 * <li>populates the model with the new working copy contents</li>
 * <li>fires a fine grained delta (flag F_FINE_GRAINED) describing the difference between the
 * previous content and the new content (which method was added/removed, which field was changed,
 * etc.)</li>
 * <li>computes problems and reports them to the ProblemRequestor (begingReporting(), n x
 * acceptProblem(...), endReporting()) iff (working copy is not consistent with its buffer ||
 * forceProblemDetection is set) && problem requestor is active</li>
 * <li>optionally produces a DOM AST that is optionally resolved</li>
 * <li>notifies compilation participants of the reconcile allowing them to participate in this
 * operation and report problems</li>
 * </ul>
 */
public class ReconcileWorkingCopyOperation extends DartModelOperation {
  public static boolean PERF = false;

  public boolean resolveBindings;
  public HashMap<String, CategorizedProblem[]> problems;
  public boolean forceProblemDetection;
  WorkingCopyOwner workingCopyOwner;
  public DartUnit ast;
  public DartElementDeltaBuilder deltaBuilder;
  public boolean requestorIsActive;

  public ReconcileWorkingCopyOperation(DartElement workingCopy, boolean forceProblemDetection,
      WorkingCopyOwner workingCopyOwner) {
    super(new DartElement[] {workingCopy});
    this.forceProblemDetection = forceProblemDetection;
    this.workingCopyOwner = workingCopyOwner;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  /**
   * Make the given working copy consistent, computes the delta and computes an AST if needed.
   * Returns the AST.
   */
  public DartUnit makeConsistent(CompilationUnitImpl workingCopy) throws DartModelException {
    if (!workingCopy.isConsistent()) {
      // make working copy consistent
      if (problems == null) {
        problems = new HashMap<String, CategorizedProblem[]>();
      }
      resolveBindings = requestorIsActive;
      ast = workingCopy.makeConsistent(
          resolveBindings,
          forceProblemDetection,
          problems,
          progressMonitor);
      deltaBuilder.buildDeltas();
      if (ast != null && deltaBuilder.delta != null) {
        deltaBuilder.delta.changedAST(ast);
      }
      return ast;
    }
    if (ast != null) {
      // no need to recompute AST if known already
      return ast;
    }
//     CompilationUnitDeclaration unit = null;
    try {
      DartModelManager.getInstance().abortOnMissingSource.set(Boolean.TRUE);
      CompilationUnitImpl source = workingCopy.cloneCachingContents();
      // find problems if needed
      if (DartProjectNature.hasDartNature(workingCopy.getDartProject().getProject())
          && forceProblemDetection) {
        resolveBindings = requestorIsActive;
        if (problems == null) {
          problems = new HashMap<String, CategorizedProblem[]>();
        }
//         unit = CompilationUnitProblemFinder.process(
//             source,
//             workingCopyOwner,
//             problems,
//             astLevel != CompilationUnit.NO_AST/*creating AST if level is not NO_AST */,
//             reconcileFlags,
//             progressMonitor);
        List<DartCompilationError> parseErrors = new ArrayList<DartCompilationError>();
        try {
          ast = DartCompilerUtilities.resolveUnit(source, parseErrors);
        } catch (Exception exception) {
          DartCore.logInformation("Could not reconcile \""
              + source.getCorrespondingResource().getLocation() + "\"", exception);
        }
        convertErrors(parseErrors, problems);
        if (progressMonitor != null) {
          progressMonitor.worked(1);
        }
      }

      // create AST if needed
//        if (unit != null) {
//         /*
//          * unit is null if working copy is consistent && (problem detection
//          * not forced || non-Java project) -> don't create AST as per API.
//          */
//         Map<String, String> options = workingCopy.getDartProject().getOptions(true);
//         // convert AST
//         ast = AST.convertCompilationUnit(
//             astLevel,
//             unit,
//             options,
//             resolveBindings,
//             source,
//             reconcileFlags,
//             progressMonitor);
      if (ast != null) {
        if (deltaBuilder.delta == null) {
          deltaBuilder.delta = new DartElementDeltaImpl(workingCopy);
        }
        deltaBuilder.delta.changedAST(ast);
      }
      if (progressMonitor != null) {
        progressMonitor.worked(1);
      }
//       }
    } catch (DartModelException exception) {
      if (DartProjectNature.hasDartNature(workingCopy.getDartProject().getProject())) {
        throw exception;
        // else DartProject has lost its nature (or most likely was closed/deleted) while reconciling -> ignore
        // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=100919)
      }
    } finally {
      DartModelManager.getInstance().abortOnMissingSource.set(null);
//       if (unit != null) {
//         unit.cleanUp();
//       }
    }
    return ast;
  }

  /**
   * @throws DartModelException if setting the source of the original compilation unit fails
   */
  @Override
  protected void executeOperation() throws DartModelException {
    checkCanceled();
    try {
      beginTask(Messages.operation_reconcilingWorkingCopy, 2);

      CompilationUnitImpl workingCopy = getWorkingCopy();
      boolean wasConsistent = workingCopy.isConsistent();

      // check is problem requestor is active
      ProblemRequestor problemRequestor = workingCopy.getPerWorkingCopyInfo();
      if (problemRequestor != null) {
        problemRequestor = ((PerWorkingCopyInfo) problemRequestor).getProblemRequestor();
      }
      boolean defaultRequestorIsActive = problemRequestor != null && problemRequestor.isActive();
      ProblemRequestor ownerProblemRequestor = workingCopyOwner.getProblemRequestor(workingCopy);
      boolean ownerRequestorIsActive = ownerProblemRequestor != null
          && ownerProblemRequestor != problemRequestor && ownerProblemRequestor.isActive();
      requestorIsActive = defaultRequestorIsActive || ownerRequestorIsActive;

      // create the delta builder (this remembers the current content of the cu)
      deltaBuilder = new DartElementDeltaBuilder(workingCopy);

      // make working copy consistent if needed and compute AST if needed
      makeConsistent(workingCopy);

      // notify reconcile participants only if working copy was not consistent
      // or if forcing problem detection (see
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=177319)
      if (!wasConsistent || forceProblemDetection) {
        notifyParticipants(workingCopy);
        // recreate ast if one participant reset it
        if (ast == null) {
          makeConsistent(workingCopy);
        }
      }

      // report problems
      if (problems != null && (forceProblemDetection || !wasConsistent)) {
        if (defaultRequestorIsActive) {
          reportProblems(workingCopy, problemRequestor);
        }
        if (ownerRequestorIsActive) {
          reportProblems(workingCopy, ownerProblemRequestor);
        }
      }

      // report delta
      DartElementDeltaImpl delta = deltaBuilder.delta;
      if (delta != null) {
        addReconcileDelta(workingCopy, delta);
      }
    } finally {
      done();
    }
  }

  /**
   * Returns the working copy this operation is working on.
   */
  protected CompilationUnitImpl getWorkingCopy() {
    return (CompilationUnitImpl) getElementToProcess();
  }

  @Override
  protected DartModelStatus verify() {
    DartModelStatus status = super.verify();
    if (!status.isOK()) {
      return status;
    }
    CompilationUnitImpl workingCopy = getWorkingCopy();
    if (!workingCopy.isWorkingCopy()) {
      return new DartModelStatusImpl(DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST, workingCopy); // was
                                                                                                    // destroyed
    }
    return status;
  }

  private void convertErrors(List<DartCompilationError> parseErrors,
      HashMap<String, CategorizedProblem[]> problems) {
    int count = parseErrors.size();
    CategorizedProblem[] problemArray = new CategorizedProblem[count];
    int nextIndex = 0;
    String[] arguments = new String[0];
    for (DartCompilationError error : parseErrors) {
      int startPosition = error.getStartPosition();
      DartCore.notYetImplemented();
      // TODO(brianwilkerson) We don't currently have any way to get arguments,
      // severity, or id.
      Source source = error.getSource();
      problemArray[nextIndex++] = new DefaultProblem(
          (source == null ? "" : error.getSource().getName()).toCharArray(),
          error.getMessage(),
          0,
          arguments,
          ProblemSeverities.Error,
          startPosition,
          startPosition + error.getLineNumber(),
          error.getLineNumber(),
          error.getColumnNumber());
    }
    problems.put(DartCore.DART_PROBLEM_MARKER_TYPE, problemArray);
  }

  private void notifyParticipants(final CompilationUnitImpl workingCopy) {
    DartCore.notYetImplemented();
//    DartProject project = getWorkingCopy().getDartProject();
//    CompilationParticipant[] participants =
//    DartModelManager.getInstance().compilationParticipants.getCompilationParticipants(project);
//    if (participants == null) return;
//   
//    final ReconcileContext context = new ReconcileContext(this, workingCopy);
//    for (int i = 0, length = participants.length; i < length; i++) {
//    final CompilationParticipant participant = participants[i];
//    SafeRunner.run(new ISafeRunnable() {
//    public void handleException(Throwable exception) {
//    if (exception instanceof Error) {
//    throw (Error) exception; // errors are not supposed to be caught
//    } else if (exception instanceof OperationCanceledException)
//    throw (OperationCanceledException) exception;
//    else if (exception instanceof UnsupportedOperationException) {
//    // might want to disable participant as it tried to modify the buffer of
//    the working copy being reconciled
//               Util.log(exception, "Reconcile participant attempted to modify the buffer of the working copy being reconciled"); //$NON-NLS-1$
//    } else
//               Util.log(exception, "Exception occurred in reconcile participant"); //$NON-NLS-1$
//    }
//    public void run() throws Exception {
//    participant.reconcile(context);
//    }
//    });
//    }
  }

  /**
   * Report working copy problems to a given requestor.
   * 
   * @param workingCopy
   * @param problemRequestor
   */
  private void reportProblems(CompilationUnitImpl workingCopy, ProblemRequestor problemRequestor) {
    try {
      problemRequestor.beginReporting();
      for (CategorizedProblem[] categorizedProblems : problems.values()) {
        if (categorizedProblems == null) {
          continue;
        }
        for (int i = 0, length = categorizedProblems.length; i < length; i++) {
          CategorizedProblem problem = categorizedProblems[i];
          // if (DartModelManager.VERBOSE){
          //   System.out.println("PROBLEM FOUND while reconciling : " + problem.getMessage());//$NON-NLS-1$
          // }
          if (progressMonitor != null && progressMonitor.isCanceled()) {
            break;
          }
          problemRequestor.acceptProblem(problem);
        }
      }
    } finally {
      problemRequestor.endReporting();
    }
  }
}
