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

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.internal.refactoring.util.MultiStateCompilationUnitChange;
import com.google.dart.tools.core.internal.refactoring.util.TextEditUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.changes.DynamicValidationStateChange;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.cleanup.CleanUpContext;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.cleanup.ICleanUpFix;
import com.google.dart.tools.ui.internal.cleanup.IMultiFix.MultiFixContext;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;
import com.google.dart.tools.ui.text.dart.IProblemLocation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringTickProvider;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.UndoEdit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("unused")
public class CleanUpRefactoring extends Refactoring {//implements IScheduledRefactoring {

  public static class CleanUpChange extends CompilationUnitChange {

    private UndoEdit fUndoEdit;

    public CleanUpChange(String name, CompilationUnit cunit) {
      super(name, cunit);
    }

    public UndoEdit getUndoEdit() {
      return fUndoEdit;
    }

    /*
     * @see
     * org.eclipse.ltk.core.refactoring.TextChange#perform(org.eclipse.core.runtime.IProgressMonitor
     * )
     */
    @Override
    public Change perform(final IProgressMonitor pm) throws CoreException {
      if (Display.getCurrent() == null) {
        final Change[] result = new Change[1];
        final CoreException[] exs = new CoreException[1];
        Display.getDefault().syncExec(new Runnable() {
          @Override
          public void run() {
            try {
              result[0] = CleanUpChange.super.perform(pm);
            } catch (CoreException e) {
              exs[0] = e;
            }
          }
        });

        if (exs[0] != null) {
          throw exs[0];
        }

        return result[0];
      } else {
        return super.perform(pm);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) {
      fUndoEdit = edit;
      return super.createUndoChange(edit, stampToRestore);
    }
  }

  public static class CleanUpTarget {

    private final CompilationUnit fCompilationUnit;

    public CleanUpTarget(CompilationUnit unit) {
      fCompilationUnit = unit;
    }

    public CompilationUnit getCompilationUnit() {
      return fCompilationUnit;
    }
  }

  public static class MultiFixTarget extends CleanUpTarget {

    private final IProblemLocation[] fProblems;

    public MultiFixTarget(CompilationUnit unit, IProblemLocation[] problems) {
      super(unit);
      fProblems = problems;
    }

    public IProblemLocation[] getProblems() {
      return fProblems;
    }
  }

  private static class CleanUpASTRequestor /*extends ASTRequestor*/{

    private final List<ParseListElement> fUndoneElements;
    private final Hashtable<CompilationUnit, List<CleanUpChange>> fSolutions;
    private final Hashtable<CompilationUnit, ParseListElement> fCompilationUnitParseElementMap;
    private final CleanUpRefactoringProgressMonitor fMonitor;

    public CleanUpASTRequestor(List<ParseListElement> parseList,
        Hashtable<CompilationUnit, List<CleanUpChange>> solutions,
        CleanUpRefactoringProgressMonitor monitor) {
      fSolutions = solutions;
      fMonitor = monitor;
      fUndoneElements = new ArrayList<ParseListElement>();
      fCompilationUnitParseElementMap = new Hashtable<CompilationUnit, ParseListElement>(
          parseList.size());
      for (Iterator<ParseListElement> iter = parseList.iterator(); iter.hasNext();) {
        ParseListElement element = iter.next();
        fCompilationUnitParseElementMap.put(element.getTarget().getCompilationUnit(), element);
      }
    }

    public void acceptAST(CompilationUnit source, DartUnit ast) {

      fMonitor.subTask(fMonitor.getSubTaskMessage(source));

      CompilationUnit primary = (CompilationUnit) source.getPrimaryElement();
      ParseListElement element = fCompilationUnitParseElementMap.get(primary);
      CleanUpTarget target = element.getTarget();

      CleanUpContext context;
      if (target instanceof MultiFixTarget) {
        context = new MultiFixContext(source, ast, ((MultiFixTarget) target).getProblems());
      } else {
        context = new CleanUpContext(source, ast);
      }
      ICleanUp[] rejectedCleanUps = calculateSolutions(context, element.getCleanUps());

      if (rejectedCleanUps.length > 0) {
        fUndoneElements.add(new ParseListElement(target, rejectedCleanUps));
        fMonitor.reset();
      } else {
        fMonitor.flush();
      }
    }

    public void acceptSource(CompilationUnit source) {
      acceptAST(source, null);
    }

    public List<ParseListElement> getUndoneElements() {
      return fUndoneElements;
    }

    private ICleanUp[] calculateSolutions(CleanUpContext context, ICleanUp[] cleanUps) {
      List<ICleanUp> result = new ArrayList<ICleanUp>();
      CleanUpChange solution;
      try {
        solution = calculateChange(context, cleanUps, result, null);
      } catch (CoreException e) {
        throw new FixCalculationException(e);
      }

      if (solution != null) {
        integrateSolution(solution, context.getCompilationUnit());
      }

      return result.toArray(new ICleanUp[result.size()]);
    }

    private void integrateSolution(CleanUpChange solution, CompilationUnit source) {
      CompilationUnit primary = source.getPrimary();

      List<CleanUpChange> changes = fSolutions.get(primary);
      if (changes == null) {
        changes = new ArrayList<CleanUpChange>();
        fSolutions.put(primary, changes);
      }
      changes.add(solution);
    }
  }

  private class CleanUpFixpointIterator {

    private List<ParseListElement> fParseList;
    private final Hashtable<CompilationUnit, List<CleanUpChange>> fSolutions;
    private final Hashtable<CompilationUnit, CompilationUnit> fWorkingCopies; // map from primary to working copy
    private final Map<String, String> fCleanUpOptions;
    private final int fSize;
    private int fIndex;

    public CleanUpFixpointIterator(CleanUpTarget[] targets, ICleanUp[] cleanUps) {
      fSolutions = new Hashtable<CompilationUnit, List<CleanUpChange>>(targets.length);
      fWorkingCopies = new Hashtable<CompilationUnit, CompilationUnit>();

      fParseList = new ArrayList<ParseListElement>(targets.length);
      for (int i = 0; i < targets.length; i++) {
        fParseList.add(new ParseListElement(targets[i], cleanUps));
      }

      fCleanUpOptions = new Hashtable<String, String>();
      for (int i = 0; i < cleanUps.length; i++) {
        ICleanUp cleanUp = cleanUps[i];
        Map<String, String> currentCleanUpOption = cleanUp.getRequirements().getCompilerOptions();
        if (currentCleanUpOption != null) {
          fCleanUpOptions.putAll(currentCleanUpOption);
        }
      }

      fSize = targets.length;
      fIndex = 1;
    }

    public void dispose() {
      for (Iterator<CompilationUnit> iterator = fWorkingCopies.values().iterator(); iterator.hasNext();) {
        CompilationUnit cu = iterator.next();
        try {
          cu.discardWorkingCopy();
        } catch (DartModelException e) {
          DartToolsPlugin.log(e);
        }
      }
      fWorkingCopies.clear();
    }

    public Change[] getResult() {

      Change[] result = new Change[fSolutions.size()];
      int i = 0;
      for (Iterator<Entry<CompilationUnit, List<CleanUpChange>>> iterator = fSolutions.entrySet().iterator(); iterator.hasNext();) {
        Entry<CompilationUnit, List<CleanUpChange>> entry = iterator.next();

        List<CleanUpChange> changes = entry.getValue();
        CompilationUnit unit = entry.getKey();

        int saveMode;
        if (fLeaveFilesDirty) {
          saveMode = TextFileChange.LEAVE_DIRTY;
        } else {
          saveMode = TextFileChange.KEEP_SAVE_STATE;
        }

        if (changes.size() == 1) {
          CleanUpChange change = changes.get(0);
          change.setSaveMode(saveMode);
          result[i] = change;
        } else {
          MultiStateCompilationUnitChange mscuc = new MultiStateCompilationUnitChange(
              getChangeName(unit),
              unit);
          for (int j = 0; j < changes.size(); j++) {
            mscuc.addChange(createGroupFreeChange(changes.get(j)));
          }
          mscuc.setSaveMode(saveMode);
          result[i] = mscuc;
        }

        i++;
      }

      return result;
    }

    public boolean hasNext() {
      return !fParseList.isEmpty();
    }

    public void next(IProgressMonitor monitor) throws CoreException {
      List<CompilationUnit> parseList = new ArrayList<CompilationUnit>();
      List<CompilationUnit> sourceList = new ArrayList<CompilationUnit>();

      try {
        for (Iterator<ParseListElement> iter = fParseList.iterator(); iter.hasNext();) {
          ParseListElement element = iter.next();

          CompilationUnit compilationUnit = element.getTarget().getCompilationUnit();
          if (fSolutions.containsKey(compilationUnit)) {
            if (fWorkingCopies.containsKey(compilationUnit)) {
              compilationUnit = fWorkingCopies.get(compilationUnit);
            } else {
              compilationUnit = compilationUnit.getWorkingCopy(new WorkingCopyOwner() {
              }, null);
              fWorkingCopies.put(compilationUnit.getPrimary(), compilationUnit);
            }
            applyChange(compilationUnit, fSolutions.get(compilationUnit.getPrimary()));
          }

          if (requiresAST(element.getCleanUps())) {
            parseList.add(compilationUnit);
          } else {
            sourceList.add(compilationUnit);
          }
        }

        CleanUpRefactoringProgressMonitor cuMonitor = new CleanUpRefactoringProgressMonitor(
            monitor,
            parseList.size() + sourceList.size(),
            fSize,
            fIndex);
        CleanUpASTRequestor requestor = new CleanUpASTRequestor(fParseList, fSolutions, cuMonitor);
        if (parseList.size() > 0) {
          // TODO(scheglov) batch parsing?
          for (CompilationUnit cu : parseList) {
            DartUnit unitNode = DartCompilerUtilities.resolveUnit(cu);
            requestor.acceptAST(cu, unitNode);
          }
//          ASTBatchParser parser = new ASTBatchParser() {
//            @Override
//            protected ASTParser createParser(DartProject project) {
//              ASTParser result = createCleanUpASTParser();
//              result.setProject(project);
//
//              Map<String, String> options = RefactoringASTParser.getCompilerOptions(project);
//              options.putAll(fCleanUpOptions);
//              result.setCompilerOptions(options);
//              return result;
//            }
//          };
//          try {
//            CompilationUnit[] units = parseList.toArray(new CompilationUnit[parseList.size()]);
////            parser.createASTs(units, new String[0], requestor, cuMonitor);
//          } catch (FixCalculationException e) {
//            throw e.getException();
//          }
        }

        for (Iterator<CompilationUnit> iterator = sourceList.iterator(); iterator.hasNext();) {
          CompilationUnit cu = iterator.next();

          monitor.worked(1);

          requestor.acceptSource(cu);

          if (monitor.isCanceled()) {
            throw new OperationCanceledException();
          }
        }

        fParseList = requestor.getUndoneElements();
        fIndex = cuMonitor.getIndex();
      } finally {
      }
    }

    private void applyChange(CompilationUnit compilationUnit, List<CleanUpChange> changes)
        throws DartModelException, CoreException {
      IDocument document = new Document(changes.get(0).getCurrentContent(new NullProgressMonitor()));
      for (int i = 0; i < changes.size(); i++) {
        CleanUpChange change = changes.get(i);
        TextEdit edit = change.getEdit().copy();

        try {
          edit.apply(document, TextEdit.UPDATE_REGIONS);
        } catch (MalformedTreeException e) {
          DartToolsPlugin.log(e);
        } catch (BadLocationException e) {
          DartToolsPlugin.log(e);
        }
      }
      compilationUnit.getBuffer().setContents(document.get());
    }

    private TextChange createGroupFreeChange(CleanUpChange change) {
      CleanUpChange result = new CleanUpChange(change.getName(), change.getCompilationUnit());
      result.setEdit(change.getEdit());
      result.setSaveMode(change.getSaveMode());
      return result;
    }

    private boolean requiresAST(ICleanUp[] cleanUps) {
      for (int i = 0; i < cleanUps.length; i++) {
        if (cleanUps[i].getRequirements().requiresAST()) {
          return true;
        }
      }
      return false;
    }
  }

  private final class CleanUpRefactoringProgressMonitor extends SubProgressMonitor {

    private double fRealWork;
    private int fFlushCount;
    private final int fSize;
    private final int fIndex;

    private CleanUpRefactoringProgressMonitor(IProgressMonitor monitor, int ticks, int size,
        int index) {
      super(monitor, ticks);
      fFlushCount = 0;
      fSize = size;
      fIndex = index;
    }

    @Override
    public void done() {
    }

    public void flush() {
      super.internalWorked(fRealWork);
      reset();
      fFlushCount++;
    }

    public int getIndex() {
      return fIndex + fFlushCount;
    }

    public String getSubTaskMessage(CompilationUnit source) {
      String typeName = source.getElementName();
      return Messages.format(
          FixMessages.CleanUpRefactoring_ProcessingCompilationUnit_message,
          new Object[] {new Integer(getIndex()), new Integer(fSize), typeName});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void internalWorked(double work) {
      fRealWork += work;
    }

    public void reset() {
      fRealWork = 0.0;
    }
  }

  private static class FixCalculationException extends RuntimeException {

    private static final long serialVersionUID = 3807273310144726165L;

    private final CoreException fException;

    public FixCalculationException(CoreException exception) {
      fException = exception;
    }

    public CoreException getException() {
      return fException;
    }
  }

  private static class ParseListElement {

    private final CleanUpTarget fTarget;
    private final ICleanUp[] fCleanUpsArray;

    public ParseListElement(CleanUpTarget cleanUpTarget, ICleanUp[] cleanUps) {
      fTarget = cleanUpTarget;
      fCleanUpsArray = cleanUps;
    }

    public ICleanUp[] getCleanUps() {
      return fCleanUpsArray;
    }

    public CleanUpTarget getTarget() {
      return fTarget;
    }
  }

  private static final RefactoringTickProvider CLEAN_UP_REFACTORING_TICK_PROVIDER = new RefactoringTickProvider(
      0,
      1,
      0,
      0);

  /**
   * A clean up is considered slow if its execution lasts longer then the value of
   * SLOW_CLEAN_UP_THRESHOLD in ms.
   */
  private static final int SLOW_CLEAN_UP_THRESHOLD = 2000;

  public static CleanUpChange calculateChange(CleanUpContext context, ICleanUp[] cleanUps,
      List<ICleanUp> undoneCleanUps, HashSet<ICleanUp> slowCleanUps) throws CoreException {
    if (cleanUps.length == 0) {
      return null;
    }

    CleanUpChange solution = null;
    int i = 0;
    do {
      ICleanUp cleanUp = cleanUps[i];
      ICleanUpFix fix;
      if (slowCleanUps != null) {
        long timeBefore = System.currentTimeMillis();
        fix = cleanUp.createFix(context);
        if (System.currentTimeMillis() - timeBefore > SLOW_CLEAN_UP_THRESHOLD) {
          slowCleanUps.add(cleanUp);
        }
      } else {
        fix = cleanUp.createFix(context);
      }
      if (fix != null) {
        CompilationUnitChange current = fix.createChange(null);
        TextEdit currentEdit = current.getEdit();

        if (solution != null) {
          if (TextEditUtil.overlaps(currentEdit, solution.getEdit())) {
            undoneCleanUps.add(cleanUp);
          } else {
            CleanUpChange merge = new CleanUpChange(
                FixMessages.CleanUpRefactoring_clean_up_multi_chang_name,
                context.getCompilationUnit());
            merge.setEdit(TextEditUtil.merge(currentEdit, solution.getEdit()));

            copyChangeGroups(merge, solution);
            copyChangeGroups(merge, current);

            solution = merge;
          }
        } else {
          solution = new CleanUpChange(current.getName(), context.getCompilationUnit());
          solution.setEdit(currentEdit);

          copyChangeGroups(solution, current);
        }
      }
      i++;
    } while (i < cleanUps.length
        && (context.getAST() == null || !cleanUps[i].getRequirements().requiresFreshAST()));

    for (; i < cleanUps.length; i++) {
      undoneCleanUps.add(cleanUps[i]);
    }
    return solution;
  }

//  public static ASTParser createCleanUpASTParser() {
//    ASTParser result = ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
//
//    result.setResolveBindings(true);
//    result.setStatementsRecovery(ASTProvider.SHARED_AST_STATEMENT_RECOVERY);
//    result.setBindingsRecovery(ASTProvider.SHARED_BINDING_RECOVERY);
//
//    return result;
//  }

  private static void copyChangeGroups(CompilationUnitChange target, CompilationUnitChange source) {
    TextEditBasedChangeGroup[] changeGroups = source.getChangeGroups();
    for (int i = 0; i < changeGroups.length; i++) {
      TextEditGroup textEditGroup = changeGroups[i].getTextEditGroup();
      TextEditGroup newGroup;
      if (textEditGroup instanceof CategorizedTextEditGroup) {
        String label = textEditGroup.getName();
        newGroup = new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(
            label,
            label,
            label)));
      } else {
        newGroup = new TextEditGroup(textEditGroup.getName());
      }
      TextEdit[] textEdits = textEditGroup.getTextEdits();
      for (int j = 0; j < textEdits.length; j++) {
        newGroup.addTextEdit(textEdits[j]);
      }
      target.addTextEditGroup(newGroup);
    }
  }

  private static String getChangeName(CompilationUnit compilationUnit) {
    StringBuffer buf = new StringBuffer();
    DartElementLabels.getCompilationUnitLabel(compilationUnit, DartElementLabels.ALL_DEFAULT, buf);
    buf.append(DartElementLabels.CONCAT_STRING);

    StringBuffer buf2 = new StringBuffer();
//    DartElementLabels.tgetPackageFragmentLabel((IPackageFragment) compilationUnit.getParent(),
//        DartElementLabels.P_QUALIFIED, buf2);
    buf.append(buf2.toString().replace('.', '/'));

    return buf.toString();
  }

  private final List<ICleanUp> fCleanUps;

  private final Hashtable<DartProject, List<CleanUpTarget>> fProjects;

  private Change fChange;

  private boolean fLeaveFilesDirty;

  private final String fName;

  private boolean fUseOptionsFromProfile;

  public CleanUpRefactoring() {
    this(FixMessages.CleanUpRefactoring_Refactoring_name);
  }

  public CleanUpRefactoring(String name) {
    fName = name;
    fCleanUps = new ArrayList<ICleanUp>();
    fProjects = new Hashtable<DartProject, List<CleanUpTarget>>();
    fUseOptionsFromProfile = false;
  }

  public void addCleanUp(ICleanUp fix) {
    fCleanUps.add(fix);
  }

  public void addCleanUpTarget(CleanUpTarget target) {

    DartProject javaProject = target.getCompilationUnit().getDartProject();
    if (!fProjects.containsKey(javaProject)) {
      fProjects.put(javaProject, new ArrayList<CleanUpTarget>());
    }

    List<CleanUpTarget> targets = fProjects.get(javaProject);
    targets.add(target);
  }

  public void addCompilationUnit(CompilationUnit unit) {
    addCleanUpTarget(new CleanUpTarget(unit));
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {

    if (pm == null) {
      pm = new NullProgressMonitor();
    }

    if (fProjects.size() == 0 || fCleanUps.size() == 0) {
      pm.beginTask("", 1); //$NON-NLS-1$
      pm.worked(1);
      pm.done();
      fChange = new NullChange();

      return new RefactoringStatus();
    }

    int cuCount = getCleanUpTargetsSize();

    RefactoringStatus result = new RefactoringStatus();

    ICleanUp[] cleanUps = getCleanUps();
    pm.beginTask("", cuCount * 2 * fCleanUps.size() + 4 * cleanUps.length); //$NON-NLS-1$
    try {
      DynamicValidationStateChange change = new DynamicValidationStateChange(getName());
      change.setSchedulingRule(getSchedulingRule());
      for (Iterator<Entry<DartProject, List<CleanUpTarget>>> projectIter = fProjects.entrySet().iterator(); projectIter.hasNext();) {
        Entry<DartProject, List<CleanUpTarget>> entry = projectIter.next();
        DartProject project = entry.getKey();
        List<CleanUpTarget> targetsList = entry.getValue();
        CleanUpTarget[] targets = targetsList.toArray(new CleanUpTarget[targetsList.size()]);

        // TODO(scheglov)
//        if (fUseOptionsFromProfile) {
//          result.merge(setOptionsFromProfile(project, cleanUps));
//          if (result.hasFatalError()) {
//            return result;
//          }
//        }

        result.merge(checkPreConditions(project, targets, new SubProgressMonitor(
            pm,
            3 * cleanUps.length)));
        if (result.hasFatalError()) {
          return result;
        }

        Change[] changes = cleanUpProject(project, targets, cleanUps, pm);

        result.merge(checkPostConditions(new SubProgressMonitor(pm, cleanUps.length)));
        if (result.hasFatalError()) {
          return result;
        }

        for (int i = 0; i < changes.length; i++) {
          change.add(changes[i]);
        }
      }
      fChange = change;

      List<IResource> files = new ArrayList<IResource>();
      findFilesToBeModified(change, files);
      result.merge(Checks.validateModifiesFiles(
          files.toArray(new IFile[files.size()]),
          getValidationContext()));
    } finally {
      pm.done();
    }

    return result;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    if (pm != null) {
      pm.beginTask("", 1); //$NON-NLS-1$
      pm.worked(1);
      pm.done();
    }
    return new RefactoringStatus();
  }

  public void clearCleanUps() {
    fCleanUps.clear();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    if (pm != null) {
      pm.beginTask("", 1); //$NON-NLS-1$
      pm.worked(1);
      pm.done();
    }
    return fChange;
  }

  public ICleanUp[] getCleanUps() {
    return fCleanUps.toArray(new ICleanUp[fCleanUps.size()]);
  }

  public CleanUpTarget[] getCleanUpTargets() {
    List<CleanUpTarget> result = new ArrayList<CleanUpTarget>();
    for (Iterator<List<CleanUpTarget>> iter = fProjects.values().iterator(); iter.hasNext();) {
      List<CleanUpTarget> projectTargets = iter.next();
      result.addAll(projectTargets);
    }
    return result.toArray(new CleanUpTarget[result.size()]);
  }

  public int getCleanUpTargetsSize() {
    int result = 0;
    for (Iterator<List<CleanUpTarget>> iter = fProjects.values().iterator(); iter.hasNext();) {
      List<CleanUpTarget> projectTargets = iter.next();
      result += projectTargets.size();
    }
    return result;
  }

  @Override
  public String getName() {
    return fName;
  }

  public DartProject[] getProjects() {
    return fProjects.keySet().toArray(new DartProject[fProjects.keySet().size()]);
  }

  public ISchedulingRule getSchedulingRule() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  public boolean hasCleanUps() {
    return !fCleanUps.isEmpty();
  }

  public void setLeaveFilesDirty(boolean leaveFilesDirty) {
    fLeaveFilesDirty = leaveFilesDirty;
  }

  public void setUseOptionsFromProfile(boolean enabled) {
    fUseOptionsFromProfile = enabled;
  }

  @Override
  protected RefactoringTickProvider doGetRefactoringTickProvider() {
    return CLEAN_UP_REFACTORING_TICK_PROVIDER;
  }

  private RefactoringStatus checkPostConditions(SubProgressMonitor monitor) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();

    ICleanUp[] cleanUps = getCleanUps();
    monitor.beginTask("", cleanUps.length); //$NON-NLS-1$
    monitor.subTask(FixMessages.CleanUpRefactoring_checkingPostConditions_message);
    try {
      for (int j = 0; j < cleanUps.length; j++) {
        result.merge(cleanUps[j].checkPostConditions(new SubProgressMonitor(monitor, 1)));
      }
    } finally {
      monitor.done();
    }
    return result;
  }

  private RefactoringStatus checkPreConditions(DartProject dartProject, CleanUpTarget[] targets,
      IProgressMonitor monitor) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();

    CompilationUnit[] compilationUnits = new CompilationUnit[targets.length];
    for (int i = 0; i < targets.length; i++) {
      compilationUnits[i] = targets[i].getCompilationUnit();
    }

    ICleanUp[] cleanUps = getCleanUps();
    monitor.beginTask("", compilationUnits.length * cleanUps.length); //$NON-NLS-1$
    monitor.subTask(Messages.format(
        FixMessages.CleanUpRefactoring_Initialize_message,
        BasicElementLabels.getResourceName(dartProject.getProject())));
    try {
      for (int j = 0; j < cleanUps.length; j++) {
        result.merge(cleanUps[j].checkPreConditions(
            dartProject,
            compilationUnits,
            new SubProgressMonitor(monitor, compilationUnits.length)));
        if (result.hasFatalError()) {
          return result;
        }
      }
    } finally {
      monitor.done();
    }

    return result;
  }

  private Change[] cleanUpProject(DartProject project, CleanUpTarget[] targets,
      ICleanUp[] cleanUps, IProgressMonitor monitor) throws CoreException {
    CleanUpFixpointIterator iter = new CleanUpFixpointIterator(targets, cleanUps);

    SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 2 * targets.length
        * cleanUps.length);
    subMonitor.beginTask("", targets.length); //$NON-NLS-1$
    subMonitor.subTask(Messages.format(
        FixMessages.CleanUpRefactoring_Parser_Startup_message,
        BasicElementLabels.getResourceName(project.getProject())));
    try {
      while (iter.hasNext()) {
        iter.next(subMonitor);
      }

      return iter.getResult();
    } finally {
      iter.dispose();
      subMonitor.done();
    }
  }

  private void findFilesToBeModified(CompositeChange change, List<IResource> result)
      throws DartModelException {
    Change[] children = change.getChildren();
    for (int i = 0; i < children.length; i++) {
      Change child = children[i];
      if (child instanceof CompositeChange) {
        findFilesToBeModified((CompositeChange) child, result);
      } else if (child instanceof MultiStateCompilationUnitChange) {
        result.add(((MultiStateCompilationUnitChange) child).getCompilationUnit().getCorrespondingResource());
      } else if (child instanceof CompilationUnitChange) {
        result.add(((CompilationUnitChange) child).getCompilationUnit().getCorrespondingResource());
      }
    }
  }

//  private RefactoringStatus setOptionsFromProfile(DartProject javaProject, ICleanUp[] cleanUps) {
//    Map<String, String> options = CleanUpPreferenceUtil.loadOptions(new ProjectScope(
//        javaProject.getProject()));
//    if (options == null) {
//      return RefactoringStatus.createFatalErrorStatus(Messages.format(
//          FixMessages.CleanUpRefactoring_could_not_retrive_profile,
//          BasicElementLabels.getResourceName(javaProject.getProject())));
//    }
//
//    CleanUpOptions cleanUpOptions = new MapCleanUpOptions(options);
//    for (int i = 0; i < cleanUps.length; i++) {
//      cleanUps[i].setOptions(cleanUpOptions);
//    }
//
//    return new RefactoringStatus();
//  }

}
