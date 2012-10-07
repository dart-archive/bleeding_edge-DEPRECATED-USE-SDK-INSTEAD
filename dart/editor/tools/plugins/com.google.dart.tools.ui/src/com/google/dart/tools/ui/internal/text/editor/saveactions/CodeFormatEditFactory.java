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
package com.google.dart.tools.ui.internal.text.editor.saveactions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.cleanup.ICleanUpFix;
import com.google.dart.tools.ui.internal.cleanup.MultiFixMessages;
import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;

/**
 * A factory for creating text edits to perform simple formating tasks (such as removing trailing
 * whitespace).
 * <p>
 * NOTE: this is a dumbed-down implementation of the kind of things that {@link ICleanUpFix}s do in
 * the JDT. If we elect to enable full-blown cleanups, this can get removed or folded in.
 */
public class CodeFormatEditFactory {

  /**
   * Create an edit that removes trailing whitespace (not in a multi-line string) from the given
   * document.
   * 
   * @param document the document to clean up
   * @return an edit that removes all trailing whitespace
   * @throws CoreException if an error occurs in creating the edit
   */
  public static MultiTextEdit removeTrailingWhitespace(IDocument document) throws CoreException {

    MultiTextEdit textEdit = new MultiTextEdit();

    try {

      String label = MultiFixMessages.CodeFormatFix_RemoveTrailingWhitespace_changeDescription;
      CategorizedTextEditGroup group = new CategorizedTextEditGroup(label, new GroupCategorySet(
          new GroupCategory(label, label, label)));

      int lineCount = document.getNumberOfLines();
      for (int i = 0; i < lineCount; i++) {

        IRegion region = document.getLineInformation(i);

        if (region.getLength() == 0) {
          continue;
        }

        int lineStart = region.getOffset();
        int lineExclusiveEnd = lineStart + region.getLength();

        if (inMultilineString(document, lineExclusiveEnd)) {
          continue;
        }

        int j = getIndexOfRightMostNoneWhitespaceCharacter(
            lineStart,
            lineExclusiveEnd - 1,
            document);

        j++;
        if (j < lineExclusiveEnd) {
          DeleteEdit edit = new DeleteEdit(j, lineExclusiveEnd - j);
          textEdit.addChild(edit);
          group.addTextEdit(edit);
        }
      }

    } catch (BadLocationException e) {
      throw new CoreException(new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), 0, "", e)); //$NON-NLS-1$
    } catch (BadPartitioningException e) {
      throw new CoreException(new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), 0, "", e)); //$NON-NLS-1$
    }

    return textEdit;
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
  private static int getIndexOfRightMostNoneWhitespaceCharacter(int start, int end,
      IDocument document) throws BadLocationException {

    int position = end;
    while (position >= start && Character.isWhitespace(document.getChar(position))) {
      position--;
    }

    return position;
  }

  private static boolean inMultilineString(IDocument document, int offset)
      throws BadLocationException, BadPartitioningException {
    try {
      ITypedRegion partitionInfo = ((IDocumentExtension3) document).getPartition(
          DartPartitions.DART_PARTITIONING,
          offset,
          true);
      return (partitionInfo.getType().equals(DartPartitions.DART_MULTI_LINE_STRING));
    } catch (BadPartitioningException ex) {
      return false;
    }
  }

}
