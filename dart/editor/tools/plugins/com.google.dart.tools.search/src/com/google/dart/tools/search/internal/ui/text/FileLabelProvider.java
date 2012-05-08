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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.internal.ui.Messages;
import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.internal.ui.SearchPluginImages;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FileLabelProvider extends LabelProvider implements IStyledLabelProvider {

  public static final int SHOW_LABEL = 1;
  public static final int SHOW_LABEL_PATH = 2;
  public static final int SHOW_PATH_LABEL = 3;

  private static final String fgSeparatorFormat = "{0} - {1}"; //$NON-NLS-1$

  private static final String fgEllipses = " ... "; //$NON-NLS-1$

  private final WorkbenchLabelProvider fLabelProvider;
  private final AbstractTextSearchViewPage fPage;
  @SuppressWarnings("rawtypes")
  private final Comparator fMatchComparator;

  private final Image fLineMatchImage;

  private int fOrder;

  private static final int MIN_MATCH_CONTEXT = 10; // minimal number of characters shown after and before a match

  @SuppressWarnings("rawtypes")
  public FileLabelProvider(AbstractTextSearchViewPage page, int orderFlag) {
    fLabelProvider = new WorkbenchLabelProvider();
    fOrder = orderFlag;
    fPage = page;
    fLineMatchImage = SearchPluginImages.get(SearchPluginImages.IMG_OBJ_TEXT_SEARCH_LINE);
    fMatchComparator = new Comparator() {
      @Override
      public int compare(Object o1, Object o2) {
        return ((FileMatch) o1).getOriginalOffset() - ((FileMatch) o2).getOriginalOffset();
      }
    };
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    super.addListener(listener);
    fLabelProvider.addListener(listener);
  }

  @Override
  public void dispose() {
    super.dispose();
    fLabelProvider.dispose();
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof FileResource<?>) {
      element = ((FileResource<?>) element).getResource();
    }
    if (element instanceof File) {
      //TODO(pquitslund): improve image fetching
      IFileStore file = EFS.getLocalFileSystem().fromLocalFile((File) element);
      return fLabelProvider.getImage(file);
    }

    if (element instanceof LineElement) {
      return fLineMatchImage;
    }

    IResource resource = (IResource) element;
    Image image = fLabelProvider.getImage(resource);
    return image;
  }

  public int getOrder() {
    return fOrder;
  }

  public StyledString getStyledString(File resource) {
    if (!resource.exists()) {
      return new StyledString(SearchMessages.FileLabelProvider_removed_resource_label);
    }

    String name = BasicElementLabels.getResourceName(resource);
    if (fOrder == SHOW_LABEL) {
      return getColoredLabelWithCounts(resource, new StyledString(name));
    }

    String pathString = BasicElementLabels.getParentPathLabel(resource, false);
    if (fOrder == SHOW_LABEL_PATH) {
      StyledString str = new StyledString(name);
      String decorated = Messages.format(fgSeparatorFormat, new String[] {
          str.getString(), pathString});

      StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, str);
      return getColoredLabelWithCounts(resource, str);
    }

    StyledString str = new StyledString(Messages.format(fgSeparatorFormat, new String[] {
        pathString, name}));
    return getColoredLabelWithCounts(resource, str);

  }

  public StyledString getStyledString(IResource resource) {

    if (!resource.exists()) {
      return new StyledString(SearchMessages.FileLabelProvider_removed_resource_label);
    }

    String name = BasicElementLabels.getResourceName(resource);
    if (fOrder == SHOW_LABEL) {
      return getColoredLabelWithCounts(resource, new StyledString(name));
    }

    String pathString = BasicElementLabels.getPathLabel(resource.getParent().getFullPath(), false);
    if (fOrder == SHOW_LABEL_PATH) {
      StyledString str = new StyledString(name);
      String decorated = Messages.format(fgSeparatorFormat, new String[] {
          str.getString(), pathString});

      StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, str);
      return getColoredLabelWithCounts(resource, str);
    }

    StyledString str = new StyledString(Messages.format(fgSeparatorFormat, new String[] {
        pathString, name}));
    return getColoredLabelWithCounts(resource, str);

  }

  @Override
  public StyledString getStyledText(Object element) {
    if (element instanceof LineElement) {
      return getLineElementLabel((LineElement) element);
    }

    if (element instanceof WorkspaceFile) {
      element = ((WorkspaceFile) element).getResource();
    }

    if (element instanceof IResource) {
      return getStyledString((IResource) element);
    }

    if (element instanceof File) {
      return getStyledString((File) element);
    }

    return new StyledString("skipped [" + element.getClass() + "]: " + element.toString());
  }

  @Override
  public String getText(Object object) {
    return getStyledText(object).getString();
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return fLabelProvider.isLabelProperty(element, property);
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    super.removeListener(listener);
    fLabelProvider.removeListener(listener);
  }

  public void setOrder(int orderFlag) {
    fOrder = orderFlag;
  }

  private int appendShortenedGap(String content, int start, int end, int charsToCut,
      boolean isFirst, StyledString str) {
    int gapLength = end - start;
    if (!isFirst) {
      gapLength -= MIN_MATCH_CONTEXT;
    }
    if (end < content.length()) {
      gapLength -= MIN_MATCH_CONTEXT;
    }
    if (gapLength < MIN_MATCH_CONTEXT) { // don't cut, gap is too small
      str.append(content.substring(start, end));
      return charsToCut;
    }

    int context = MIN_MATCH_CONTEXT;
    if (gapLength > charsToCut) {
      context += gapLength - charsToCut;
    }

    if (!isFirst) {
      str.append(content.substring(start, start + context)); // give all extra context to the right side of a match
      context = MIN_MATCH_CONTEXT;
    }

    str.append(fgEllipses, StyledString.QUALIFIER_STYLER);

    if (end < content.length()) {
      str.append(content.substring(end - context, end));
    }
    return charsToCut - gapLength + fgEllipses.length();
  }

  private int evaluateLineStart(Match[] matches, String lineContent, int lineOffset) {
    int max = lineContent.length();
    if (matches.length > 0) {
      FileMatch match = (FileMatch) matches[0];
      max = match.getOriginalOffset() - lineOffset;
      if (max < 0) {
        return 0;
      }
    }
    for (int i = 0; i < max; i++) {
      char ch = lineContent.charAt(i);
      if (!Character.isWhitespace(ch) || ch == '\n' || ch == '\r') {
        return i;
      }
    }
    return max;
  }

  private int getCharsToCut(int contentLength, Match[] matches) {
    if (contentLength <= 256 || !"win32".equals(SWT.getPlatform()) || matches.length == 0) { //$NON-NLS-1$
      return 0; // no shortening required
    }
    // XXX: workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=38519
    return contentLength - 256 + Math.max(matches.length * fgEllipses.length(), 100);
  }

  private StyledString getColoredLabelWithCounts(Object element, StyledString coloredName) {
    AbstractTextSearchResult result = fPage.getInput();
    if (result == null) {
      return coloredName;
    }

    int matchCount = result.getMatchCount(element);
    if (matchCount <= 1) {
      return coloredName;
    }

    String countInfo = Messages.format(SearchMessages.FileLabelProvider_count_format, new Integer(
        matchCount));
    coloredName.append(' ').append(countInfo, StyledString.COUNTER_STYLER);
    return coloredName;
  }

  @SuppressWarnings("unchecked")
  private StyledString getLineElementLabel(LineElement lineElement) {
    int lineNumber = lineElement.getLine();
    String lineNumberString = Messages.format(
        SearchMessages.FileLabelProvider_line_number,
        new Integer(lineNumber));

    StyledString str = new StyledString(lineNumberString, StyledString.QUALIFIER_STYLER);

    Match[] matches = lineElement.getMatches(fPage.getInput());
    Arrays.sort(matches, fMatchComparator);

    String content = lineElement.getContents();

    int pos = evaluateLineStart(matches, content, lineElement.getOffset());

    int length = content.length();

    int charsToCut = getCharsToCut(length, matches); // number of characters to leave away if the line is too long
    for (int i = 0; i < matches.length; i++) {
      FileMatch match = (FileMatch) matches[i];
      int start = Math.max(match.getOriginalOffset() - lineElement.getOffset(), 0);
      // append gap between last match and the new one
      if (pos < start) {
        if (charsToCut > 0) {
          charsToCut = appendShortenedGap(content, pos, start, charsToCut, i == 0, str);
        } else {
          str.append(content.substring(pos, start));
        }
      }
      // append match
      int end = Math.min(
          match.getOriginalOffset() + match.getOriginalLength() - lineElement.getOffset(),
          lineElement.getLength());
      str.append(content.substring(start, end), DecoratingFileSearchLabelProvider.HIGHLIGHT_STYLE);
      pos = end;
    }
    // append rest of the line
    if (charsToCut > 0) {
      appendShortenedGap(content, pos, length, charsToCut, false, str);
    } else {
      str.append(content.substring(pos));
    }
    return str;
  }

}
