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
package com.google.dart.tools.ui.refactoring;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.MergeCompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.ChangeCorrectionProposal;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionKind;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.LinkedPositionProposal;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.internal.refactoring.ServiceUtils_OLD;
import com.google.dart.tools.ui.internal.text.correction.proposals.LinkedCorrectionProposal_OLD;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link ServiceUtils_OLD}.
 */
public class ServiceUtilsTest extends AbstractDartTest {
  private final IProgressMonitor NULL_PM = new NullProgressMonitor();

  public void test_createCoreException() throws Exception {
    Throwable e = new Throwable("msg");
    CoreException coreException = ServiceUtils_OLD.createCoreException(e);
    assertSame(e, coreException.getCause());
    // status
    IStatus status = coreException.getStatus();
    assertSame(e, status.getException());
    assertSame(IStatus.ERROR, status.getSeverity());
  }

  public void test_toLTK_Change_CompositeChange() throws Exception {
    Source source = createTestFileSource();
    // fill SourceChange
    SourceChange sourceChangeA = new SourceChange("My change A", source);
    SourceChange sourceChangeB = new SourceChange("My change B", source);
    CompositeChange compositeChange = new CompositeChange(
        "My composite change",
        sourceChangeA,
        sourceChangeB);
    // toLTK
    org.eclipse.ltk.core.refactoring.Change ltkChange_ = ServiceUtils_OLD.toLTK(compositeChange);
    org.eclipse.ltk.core.refactoring.CompositeChange ltkChange = (org.eclipse.ltk.core.refactoring.CompositeChange) ltkChange_;
    assertEquals("My composite change", ltkChange.getName());
    org.eclipse.ltk.core.refactoring.Change[] ltkChanges = ltkChange.getChildren();
    assertThat(ltkChanges).hasSize(2);
  }

  public void test_toLTK_Change_CompositeChange_noFile() throws Exception {
    Source source = new FileBasedSource(new File("no-such-file.dart"));
    SourceChange sourceChangeA = new SourceChange("My change A", source);
    SourceChange sourceChangeB = new SourceChange("My change B", source);
    CompositeChange compositeChange = new CompositeChange(
        "My composite change",
        sourceChangeA,
        sourceChangeB);
    // toLTK
    org.eclipse.ltk.core.refactoring.Change ltkChange_ = ServiceUtils_OLD.toLTK(compositeChange);
    org.eclipse.ltk.core.refactoring.CompositeChange ltkChange = (org.eclipse.ltk.core.refactoring.CompositeChange) ltkChange_;
    assertEquals("My composite change", ltkChange.getName());
    org.eclipse.ltk.core.refactoring.Change[] ltkChanges = ltkChange.getChildren();
    assertThat(ltkChanges).isEmpty();
  }

  public void test_toLTK_Change_MergeCompositeChange() throws Exception {
    IFile testFile = testProject.setFileContent("test.dart", "012345");
    Source source = createFileSource(testFile);
    // fill SourceChange
    SourceChange sourceChangeA = new SourceChange("My change A", source);
    SourceChange sourceChangeB = new SourceChange("My change B", source);
    sourceChangeA.addEdit(new Edit(0, 0, "a"));
    sourceChangeB.addEdit(new Edit(2, 0, "b"));
    CompositeChange compositeChangeA = new CompositeChange("A", sourceChangeA);
    CompositeChange compositeChangeB = new CompositeChange("B", sourceChangeB);
    MergeCompositeChange mergeChange = new MergeCompositeChange(
        "My composite change",
        compositeChangeA,
        compositeChangeB);
    // toLTK
    org.eclipse.ltk.core.refactoring.Change ltkChange_ = ServiceUtils_OLD.toLTK(mergeChange);
    org.eclipse.ltk.core.refactoring.CompositeChange ltkChange = (org.eclipse.ltk.core.refactoring.CompositeChange) ltkChange_;
    assertEquals("My composite change", ltkChange.getName());
    org.eclipse.ltk.core.refactoring.Change[] ltkChanges = ltkChange.getChildren();
    assertThat(ltkChanges).hasSize(2);
    //
    ltkChange.perform(NULL_PM);
    String newContent = testProject.getFileString(testFile);
    assertEquals("a01b2345", newContent);
  }

  public void test_toLTK_Change_MergeCompositeChange_emptyPreview() throws Exception {
    IFile testFile = testProject.setFileContent("test.dart", "012345");
    Source source = createFileSource(testFile);
    // fill SourceChange
    SourceChange sourceChangeB = new SourceChange("My change B", source);
    sourceChangeB.addEdit(new Edit(2, 0, "b"));
    CompositeChange compositeChangeA = new CompositeChange("A");
    CompositeChange compositeChangeB = new CompositeChange("B", sourceChangeB);
    MergeCompositeChange mergeChange = new MergeCompositeChange(
        "My composite change",
        compositeChangeA,
        compositeChangeB);
    // toLTK
    org.eclipse.ltk.core.refactoring.Change ltkChange_ = ServiceUtils_OLD.toLTK(mergeChange);
    org.eclipse.ltk.core.refactoring.CompositeChange ltkChange = (org.eclipse.ltk.core.refactoring.CompositeChange) ltkChange_;
    assertEquals("My composite change", ltkChange.getName());
    org.eclipse.ltk.core.refactoring.Change[] ltkChanges = ltkChange.getChildren();
    assertThat(ltkChanges).hasSize(1);
    assertThat(ltkChanges[0]).isInstanceOf(CompilationUnitChange.class);
  }

  public void test_toLTK_Change_SourceChange() throws Exception {
    Source source = createTestFileSource();
    // fill SourceChange
    SourceChange sourceChange = new SourceChange("My change", source);
    sourceChange.addEdit(new Edit(10, 1, "a"));
    sourceChange.addEdit(new Edit(20, 2, "b"));
    // toLTK
    TextFileChange ltkChange = (TextFileChange) ServiceUtils_OLD.toLTK((Change) sourceChange);
    assertEquals("My change", ltkChange.getName());
  }

  public void test_toLTK_Change_SourceChange_externalFile() throws Exception {
    Source source = new FileBasedSource(FileUtilities2.createFile("/some/path"));
    // fill SourceChange
    SourceChange sourceChange = new SourceChange("My change", source);
    sourceChange.addEdit(new Edit(10, 1, "a"));
    sourceChange.addEdit(new Edit(20, 2, "b"));
    // no IFile for this Source, so no LTK change
    {
      org.eclipse.ltk.core.refactoring.Change ltkChange = ServiceUtils_OLD.toLTK(sourceChange);
      assertNull(ltkChange);
    }
    // try as part of CompositeChange
    {
      CompositeChange compositeChange = new CompositeChange("My composite change", sourceChange);
      org.eclipse.ltk.core.refactoring.CompositeChange ltkChange = ServiceUtils_OLD.toLTK(compositeChange);
      assertNotNull(ltkChange);
      // wrong SourceChange was ignored
      assertThat(ltkChange.getChildren()).isEmpty();
    }
  }

  public void test_toLTK_CorrectionImage() throws Exception {
    assertNotNull(ServiceUtils_OLD.toLTK(CorrectionImage.IMG_CORRECTION_CHANGE));
    assertNotNull(ServiceUtils_OLD.toLTK(CorrectionImage.IMG_CORRECTION_CLASS));
  }

  public void test_toLTK_RefactoringStatus() throws Exception {
    RefactoringStatus serviceStatus = RefactoringStatus.createErrorStatus("msg");
    org.eclipse.ltk.core.refactoring.RefactoringStatus ltkStatus = ServiceUtils_OLD.toLTK(serviceStatus);
    org.eclipse.ltk.core.refactoring.RefactoringStatusEntry[] ltkEntries = ltkStatus.getEntries();
    assertThat(ltkEntries).hasSize(1);
    // entry[0]
    assertEquals("msg", ltkEntries[0].getMessage());
    assertEquals(
        org.eclipse.ltk.core.refactoring.RefactoringStatus.ERROR,
        ltkEntries[0].getSeverity());
  }

  public void test_toLTK_SourceChange_noGroups() throws Exception {
    Source source = createTestFileSource();
    // fill SourceChange
    SourceChange sourceChange = new SourceChange("My change", source);
    sourceChange.addEdit(new Edit(10, 1, "a"));
    sourceChange.addEdit(new Edit(20, 2, "b"));
    // toLTK
    TextFileChange ltkChange = ServiceUtils_OLD.toLTK(sourceChange);
    assertEquals("My change", ltkChange.getName());
    // no groups
    TextEditBasedChangeGroup[] changeGroups = ltkChange.getChangeGroups();
    assertThat(changeGroups).isEmpty();
    // check edits
    MultiTextEdit multiTextEdit = (MultiTextEdit) ltkChange.getEdit();
    TextEdit[] textEdits = multiTextEdit.getChildren();
    assertThat(textEdits).hasSize(2);
    assertEquals("a", ((ReplaceEdit) textEdits[0]).getText());
    assertEquals("b", ((ReplaceEdit) textEdits[1]).getText());
  }

  /**
   * It is OK to apply more than one insert {@link Edit} with the same offset.
   * <p>
   * For example Quick Fix "Implement Missing Overrides" inserts several method declarations.
   */
  public void test_toLTK_SourceChange_twoInserts() throws Exception {
    Source source = createTestFileSource();
    // fill SourceChange
    SourceChange sourceChange = new SourceChange("My change", source);
    sourceChange.addEdit(new Edit(2, 0, "a"));
    sourceChange.addEdit(new Edit(2, 0, "b"));
    // toLTK
    TextFileChange ltkChange = ServiceUtils_OLD.toLTK(sourceChange);
    TextEdit textEdit = ltkChange.getEdit();
    // apply
    Document document = new Document("01234");
    textEdit.apply(document);
    assertEquals("01ab234", document.get());
  }

  public void test_toLTK_SourceChange_withGroups() throws Exception {
    Source source = createTestFileSource();
    // fill SourceChange
    SourceChange sourceChange = new SourceChange("My change", source);
    sourceChange.addEdit(new Edit(10, 1, "a1"), "groupA");
    sourceChange.addEdit(new Edit(20, 2, "a2"), "groupA");
    sourceChange.addEdit(new Edit(30, 3, "b"), "groupB");
    // toLTK
    TextFileChange ltkChange = ServiceUtils_OLD.toLTK(sourceChange);
    assertEquals("My change", ltkChange.getName());
    TextEditBasedChangeGroup[] changeGroups = ltkChange.getChangeGroups();
    assertThat(changeGroups).hasSize(2);
    {
      TextEditBasedChangeGroup group = changeGroups[0];
      assertEquals("groupA", group.getName());
      TextEdit[] textEdits = group.getTextEdits();
      assertThat(textEdits).hasSize(2);
      assertEquals("a1", ((ReplaceEdit) textEdits[0]).getText());
      assertEquals("a2", ((ReplaceEdit) textEdits[1]).getText());
    }
  }

  public void test_toLTK_Throwable() throws Exception {
    Throwable e = new Throwable("msg");
    org.eclipse.ltk.core.refactoring.RefactoringStatus ltkStatus = ServiceUtils_OLD.toLTK(e);
    org.eclipse.ltk.core.refactoring.RefactoringStatusEntry[] ltkEntries = ltkStatus.getEntries();
    assertThat(ltkEntries).hasSize(1);
    // entry[0]
    assertEquals("msg", ltkEntries[0].getMessage());
    assertEquals(
        org.eclipse.ltk.core.refactoring.RefactoringStatus.FATAL,
        ltkEntries[0].getSeverity());
  }

  public void test_toUI_ChangeCorrectionProposal() throws Exception {
    Source source = createTestFileSource();
    // fill SourceChange
    SourceChange sourceChange = new SourceChange("My change", source);
    sourceChange.addEdit(new Edit(10, 1, "a"));
    sourceChange.addEdit(new Edit(20, 1, "a"));
    sourceChange.addEdit(new Edit(30, 3, "b"));
    // create ChangeCorrectionProposal
    CorrectionProposal proposal = new ChangeCorrectionProposal(
        sourceChange,
        CorrectionKind.QA_ADD_TYPE_ANNOTATION);
    //
    com.google.dart.tools.ui.internal.text.correction.proposals.ChangeCorrectionProposal uiProposal = (com.google.dart.tools.ui.internal.text.correction.proposals.ChangeCorrectionProposal) ServiceUtils_OLD.toUI(proposal);
    ReflectionUtils.invokeMethod(uiProposal, "getChange()");
    CompilationUnitChange ltkChange = (CompilationUnitChange) uiProposal.getChange();
    assertEquals("My change", ltkChange.getName());
  }

  public void test_toUI_ChangeCorrectionProposal_noFile() throws Exception {
    Source source = new FileBasedSource(new File("no-such-file.dart"));
    SourceChange sourceChange = new SourceChange("My change", source);
    CorrectionProposal proposal = new ChangeCorrectionProposal(
        sourceChange,
        CorrectionKind.QA_ADD_TYPE_ANNOTATION);
    ICompletionProposal uiProposal = ServiceUtils_OLD.toUI(proposal);
    assertNull(uiProposal);
  }

  public void test_toUI_LinkedCorrectionProposal() throws Exception {
    Source source = createTestFileSource();
    // fill SourceChange
    SourceChange sourceChange = new SourceChange("My linked change", source);
    sourceChange.addEdit(new Edit(10, 1, "a"));
    sourceChange.addEdit(new Edit(20, 1, "a"));
    sourceChange.addEdit(new Edit(30, 3, "b"));
    // create SourceCorrectionProposal
    SourceCorrectionProposal proposal = new SourceCorrectionProposal(
        sourceChange,
        CorrectionKind.QA_ADD_TYPE_ANNOTATION);
    {
      List<SourceRange> ranges = ImmutableList.of(new SourceRange(10, 1), new SourceRange(20, 1));
      Map<String, List<SourceRange>> linkedPositons = ImmutableMap.of("a", ranges);
      proposal.setLinkedPositions(linkedPositons);
    }
    {
      List<LinkedPositionProposal> proposals = ImmutableList.of(new LinkedPositionProposal(
          CorrectionImage.IMG_CORRECTION_CHANGE,
          "proposalA"));
      Map<String, List<LinkedPositionProposal>> linkedProposals = ImmutableMap.of("a", proposals);
      proposal.setLinkedPositionProposals(linkedProposals);
    }
    //
    LinkedCorrectionProposal_OLD uiProposal = ServiceUtils_OLD.toUI(proposal);
    CompilationUnitChange ltkChange = (CompilationUnitChange) uiProposal.getChange();
    assertEquals("My linked change", ltkChange.getName());
  }

  public void test_toUI_LinkedCorrectionProposal_noFile() throws Exception {
    Source source = new FileBasedSource(new File("no-such-file.dart"));
    SourceChange sourceChange = new SourceChange("My change", source);
    SourceCorrectionProposal proposal = new SourceCorrectionProposal(
        sourceChange,
        CorrectionKind.QA_ADD_TYPE_ANNOTATION);
    LinkedCorrectionProposal_OLD uiProposal = ServiceUtils_OLD.toUI(proposal);
    assertNull(uiProposal);
  }

  /**
   * @return the {@link Source} for the given {@link IFile}.
   */
  private Source createFileSource(IFile file) {
    File ioFile = file.getLocation().toFile();
    return new FileBasedSource(ioFile);
  }

  /**
   * @return the {@link Source} for <code>test.dart</code> file.
   */
  private Source createTestFileSource() throws Exception {
    IFile testFile = testProject.setFileContent("test.dart", "");
    return createFileSource(testFile);
  }
}
