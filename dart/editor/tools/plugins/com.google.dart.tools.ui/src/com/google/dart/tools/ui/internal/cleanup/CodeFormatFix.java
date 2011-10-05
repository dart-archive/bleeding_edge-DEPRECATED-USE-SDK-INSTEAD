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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.internal.refactoring.util.TextEditUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.cleanup.ICleanUpFix;
import com.google.dart.tools.ui.internal.util.CodeFormatterUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CodeFormatFix implements ICleanUpFix {

  public static ICleanUpFix createCleanUp(CompilationUnit cu, IRegion[] regions, boolean format,
      boolean removeTrailingWhitespacesAll, boolean removeTrailingWhitespacesIgnorEmpty,
      boolean correctIndentation) throws CoreException {
    if (!format && !removeTrailingWhitespacesAll && !removeTrailingWhitespacesIgnorEmpty
        && !correctIndentation) {
      return null;
    }

    ArrayList<CategorizedTextEditGroup> groups = new ArrayList<CategorizedTextEditGroup>();

    MultiTextEdit formatEdit = new MultiTextEdit();
    if (format) {
      Map<String, String> formatterSettings = new HashMap<String, String>(
          cu.getDartProject().getOptions(true));

      String content = cu.getBuffer().getContents();
      Document document = new Document(content);
      String lineDelemiter = TextUtilities.getDefaultLineDelimiter(document);

      TextEdit edit;
      if (regions == null) {
        edit = CodeFormatterUtil.reformat(CodeFormatter.K_COMPILATION_UNIT
            | CodeFormatter.F_INCLUDE_COMMENTS, content, 0, lineDelemiter, formatterSettings);
      } else {
        if (regions.length == 0) {
          return null;
        }

        edit = CodeFormatterUtil.reformat(CodeFormatter.K_COMPILATION_UNIT
            | CodeFormatter.F_INCLUDE_COMMENTS, content, 0, lineDelemiter, formatterSettings);
      }
      if (edit != null && (!(edit instanceof MultiTextEdit) || edit.hasChildren())) {
        formatEdit.addChild(edit);
        if (!TextEditUtil.isPacked(formatEdit)) {
          formatEdit = TextEditUtil.flatten(formatEdit);
        }

        String label = MultiFixMessages.CodeFormatFix_description;
        CategorizedTextEditGroup group = new CategorizedTextEditGroup(label, new GroupCategorySet(
            new GroupCategory(label, label, label)));
        group.addTextEdit(edit);

        groups.add(group);
      }
    }

    MultiTextEdit otherEdit = new MultiTextEdit();
    if ((removeTrailingWhitespacesAll || removeTrailingWhitespacesIgnorEmpty || correctIndentation)
        && (!format || regions != null)) {
      try {
        if (correctIndentation && removeTrailingWhitespacesAll) {
          removeTrailingWhitespacesAll = false;
          removeTrailingWhitespacesIgnorEmpty = true;
        }

        Document document = new Document(cu.getBuffer().getContents());
        if (removeTrailingWhitespacesAll || removeTrailingWhitespacesIgnorEmpty) {
          String label = MultiFixMessages.CodeFormatFix_RemoveTrailingWhitespace_changeDescription;
          CategorizedTextEditGroup group = new CategorizedTextEditGroup(label,
              new GroupCategorySet(new GroupCategory(label, label, label)));

          int lineCount = document.getNumberOfLines();
          for (int i = 0; i < lineCount; i++) {

            IRegion region = document.getLineInformation(i);
            if (region.getLength() == 0) {
              continue;
            }

            int lineStart = region.getOffset();
            int lineExclusiveEnd = lineStart + region.getLength();
            int j = getIndexOfRightMostNoneWhitspaceCharacter(lineStart, lineExclusiveEnd - 1,
                document);

            if (removeTrailingWhitespacesAll) {
              j++;
              if (j < lineExclusiveEnd) {
                DeleteEdit edit = new DeleteEdit(j, lineExclusiveEnd - j);
                if (!TextEditUtil.overlaps(formatEdit, edit)) {
                  otherEdit.addChild(edit);
                  group.addTextEdit(edit);
                }
              }
            } else if (removeTrailingWhitespacesIgnorEmpty) {
              if (j >= lineStart) {
                if (document.getChar(j) == '*'
                    && getIndexOfRightMostNoneWhitspaceCharacter(lineStart, j - 1, document) < lineStart) {
                  j++;
                }
                j++;
                if (j < lineExclusiveEnd) {
                  DeleteEdit edit = new DeleteEdit(j, lineExclusiveEnd - j);
                  if (!TextEditUtil.overlaps(formatEdit, edit)) {
                    otherEdit.addChild(edit);
                    group.addTextEdit(edit);
                  }
                }
              }
            }
          }

          if (otherEdit.hasChildren()) {
            groups.add(group);
          }
        }

//        if (correctIndentation) {
//          DartToolsPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document,
//              IJavaPartitions.JAVA_PARTITIONING);
//          TextEdit edit = IndentAction.indent(document, cu.getDartProject());
//          if (edit != null) {
//
//            String label = MultiFixMessages.CodeFormatFix_correctIndentation_changeGroupLabel;
//            CategorizedTextEditGroup group = new CategorizedTextEditGroup(label,
//                new GroupCategorySet(new GroupCategory(label, label, label)));
//
//            if (edit instanceof MultiTextEdit) {
//              TextEdit[] children = ((MultiTextEdit) edit).getChildren();
//              for (int i = 0; i < children.length; i++) {
//                TextEdit child = children[i];
//                edit.removeChild(child);
//                if (!TextEditUtil.overlaps(formatEdit, child)) {
//                  otherEdit.addChild(child);
//                  group.addTextEdit(child);
//                }
//              }
//            } else {
//              if (!TextEditUtil.overlaps(formatEdit, edit)) {
//                otherEdit.addChild(edit);
//                group.addTextEdit(edit);
//              }
//            }
//
//            groups.add(group);
//          }
//        }

      } catch (BadLocationException x) {
        throw new CoreException(new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), 0, "", x)); //$NON-NLS-1$
      }
    }

    TextEdit resultEdit = TextEditUtil.merge(formatEdit, otherEdit);
    if (!resultEdit.hasChildren()) {
      return null;
    }

    CompilationUnitChange change = new CompilationUnitChange("", cu); //$NON-NLS-1$
    change.setEdit(resultEdit);

    for (int i = 0, size = groups.size(); i < size; i++) {
      TextEditGroup group = groups.get(i);
      change.addTextEditGroup(group);
    }

    return new CodeFormatFix(change);
  }

  /**
   * Returns the index in document of a none whitespace character between start (inclusive) and end
   * (inclusive) such that if more then one such character the index returned is the largest
   * possible (closest to end). Returns start - 1 if no such character.
   * 
   * @param start the start
   * @param end the end
   * @param document the document
   * @return the position or start - 1
   * @exception BadLocationException if the offset is invalid in this document
   */
  private static int getIndexOfRightMostNoneWhitspaceCharacter(int start, int end,
      IDocument document) throws BadLocationException {
    int position = end;
    while (position >= start && Character.isWhitespace(document.getChar(position))) {
      position--;
    }

    return position;
  }

  private final CompilationUnitChange fChange;

  public CodeFormatFix(CompilationUnitChange change) {
    fChange = change;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
    return fChange;
  }
}
