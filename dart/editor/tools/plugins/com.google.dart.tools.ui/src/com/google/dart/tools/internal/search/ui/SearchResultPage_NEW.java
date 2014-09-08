/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.internal.search.ui;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.ElementKind;
import com.google.dart.server.generated.types.Location;
import com.google.dart.server.generated.types.SearchResult;
import com.google.dart.server.generated.types.SearchResultKind;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.text.editor.ElementLabelProvider_NEW;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.progress.UIJob;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract {@link SearchPage} for displaying {@link SearchResult}s.
 * 
 * @coverage dart.editor.ui.search
 */
public abstract class SearchResultPage_NEW extends SearchPage {
  /**
   * Item for an element in search results tree.
   */
  private static class ElementItem {
    private final Element element;
    private final List<ElementItem> children = Lists.newArrayList();
    private final List<LineItem> lines = Lists.newArrayList();
    private ElementItem parent;
    private ElementItem prev;
    private ElementItem next;
    private int numMatches;

    public ElementItem(Element element) {
      this.element = element;
    }

    /**
     * Adds new {@link SearchResult}, on the same or new {@link LineItem}.
     */
    public void addMatch(FileLineProvider lineProvider, SearchResult match) {
      ReferenceKind referenceKind = ReferenceKind.of(match.getKind());
      Location location = match.getLocation();
      String filePath = location.getFile();
      FileLine sourceLine = lineProvider.getLine(filePath, location.getOffset());
      // find target LineItem
      LineItem targetLineItem = null;
      for (LineItem lineItem : lines) {
        if (lineItem.line.equals(sourceLine)) {
          targetLineItem = lineItem;
          break;
        }
      }
      // new LineItem
      if (targetLineItem == null) {
        boolean potential = FILTER_POTENTIAL.apply(match);
        targetLineItem = new LineItem(this, filePath, potential, sourceLine);
        lines.add(targetLineItem);
      }
      // prepare position
      Position position = new Position(location.getOffset(), location.getLength());
      // add new position
      targetLineItem.addPosition(position, referenceKind);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ElementItem)) {
        return false;
      }
      return Objects.equal(((ElementItem) obj).element, element);
    }

    @Override
    public int hashCode() {
      return element != null ? element.hashCode() : 0;
    }

    void addChild(ElementItem child) {
      if (child.parent == null) {
        child.parent = this;
        children.add(child);
      }
    }
  }
  /**
   * Information about a single line in some file.
   */
  private static class FileLine {
    final String file;
    final int start;
    final String content;

    public FileLine(String file, int start, String content) {
      this.file = file;
      this.start = start;
      this.content = content;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof FileLine)) {
        return false;
      }
      FileLine other = (FileLine) obj;
      return other.file.equals(file) && other.start == start;
    }
  }
  /**
   * Helper for transforming offsets in some file into {@link FileLine} objects.
   */
  private static class FileLineProvider {
    private final Map<String, String> fileContentMap = Maps.newHashMap();

    /**
     * @return the {@link FileLine} for the given file and offset; may be {@code null}.
     */
    public FileLine getLine(String filePath, int offset) {
      String content = getContent(filePath);
      // find start of line
      int start = offset;
      while (start > 0 && content.charAt(start - 1) != '\n') {
        start--;
      }
      // find end of line
      int end = offset;
      while (end < content.length() && content.charAt(end) != '\r' && content.charAt(end) != '\n') {
        end++;
      }
      // done
      String text = content.substring(start, end);
      return new FileLine(filePath, start, text);
    }

    private String getContent(String filePath) {
      String content = fileContentMap.get(filePath);
      if (content == null) {
        try {
          File javaFile = new File(filePath);
          IFile resource = ResourceUtil.getFile(javaFile);
          if (resource instanceof IFile) {
            // IFile in workspace, can be open and modified - get contents using FileBuffers.
            IFile file = resource;
            ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
            IPath path = file.getFullPath();
            manager.connect(path, LocationKind.IFILE, null);
            try {
              ITextFileBuffer buffer = manager.getTextFileBuffer(path, LocationKind.IFILE);
              IDocument document = buffer.getDocument();
              content = document.get();
              fileContentMap.put(filePath, content);
            } finally {
              manager.disconnect(path, LocationKind.IFILE, null);
            }
          } else {
            // It must be an external file, cannot be modified - request its contents directly.
            content = Files.toString(javaFile, Charsets.UTF_8);
            fileContentMap.put(filePath, content);
          }
        } catch (Throwable e) {
          return null;
        }
      }
      return content;
    }
  }

  /**
   * Helper for navigating {@link ElementItem} and {@link LineItem} hierarchy.
   */
  private static class ItemCursor {
    ElementItem item;
    int lineIndex;

    ItemCursor(ElementItem item) {
      this(item, -1);
    }

    ItemCursor(ElementItem item, int positionIndex) {
      this.item = item;
      this.lineIndex = positionIndex;
    }

    LineItem getLine() {
      if (item == null) {
        return null;
      }
      if (lineIndex < 0 || lineIndex > item.lines.size() - 1) {
        return null;
      }
      return item.lines.get(lineIndex);
    }

    Position getPosition() {
      LineItem lineItem = getLine();
      LinePosition linePosition = lineItem.positions.get(0);
      return linePosition.position;
    }

    /**
     * Moves this {@link ItemCursor} to the next {@link LineItem} in the same or next
     * {@link ElementItem}.
     * 
     * @return {@code true} if was moved, or {@code false} if cursor is at the last line.
     */
    boolean next() {
      ElementItem _item = item;
      int _lineIndex = lineIndex;
      // try to go to next
      if (_next()) {
        return true;
      }
      // rollback
      item = _item;
      lineIndex = _lineIndex;
      return false;
    }

    /**
     * Moves this {@link ItemCursor} to the previous {@link LineItem} in the same or previous
     * {@link ElementItem}.
     * 
     * @return {@code true} if was moved, or {@code false} if cursor is at the first line.
     */
    boolean prev() {
      ElementItem _item = item;
      int _lineIndex = lineIndex;
      // try to go to previous
      if (_prev()) {
        return true;
      }
      // rollback
      item = _item;
      lineIndex = _lineIndex;
      return false;
    }

    private boolean _next() {
      if (item == null) {
        return false;
      }
      // in the same leaf
      if (lineIndex < item.lines.size() - 1) {
        lineIndex++;
        return true;
      }
      // next leaf
      while (true) {
        item = item.next;
        if (item == null) {
          return false;
        }
        if (!item.lines.isEmpty()) {
          lineIndex = 0;
          break;
        }
      }
      return true;
    }

    private boolean _prev() {
      if (item == null) {
        return false;
      }
      // in the same leaf
      if (lineIndex > 0) {
        lineIndex--;
        return true;
      }
      // previous leaf
      while (true) {
        item = item.prev;
        if (item == null) {
          return false;
        }
        if (!item.lines.isEmpty()) {
          lineIndex = item.lines.size() - 1;
          break;
        }
      }
      return true;
    }
  }

  /**
   * Item for a line with one or more matches.
   */
  private static class LineItem {
    private ElementItem item;
    private final String filePath;
    private boolean potential;
    private final FileLine line;
    private final List<LinePosition> positions = Lists.newArrayList();

    public LineItem(ElementItem item, String filePath, boolean potential, FileLine line) {
      this.item = item;
      this.filePath = filePath;
      this.potential = potential;
      this.line = line;
    }

    /**
     * Adds new the {@link LinePosition} with given parameters.
     */
    public void addPosition(Position position, ReferenceKind kind) {
      positions.add(new LinePosition(position, kind));
    }
  }

  /**
   * Value object with {@link Position} and its {@link ReferenceKind}.
   */
  private static class LinePosition {
    private final Position position;
    private final Position positionSrc;
    private final ReferenceKind kind;

    public LinePosition(Position position, ReferenceKind kind) {
      this.position = position;
      this.positionSrc = new Position(position.offset, position.length);
      this.kind = kind;
    }
  }

  private static class PreferenceBackgroundStyler extends StyledString.Styler {
    private final IPreferenceStore store;
    private final String key;

    public PreferenceBackgroundStyler(IPreferenceStore store, String key) {
      this.store = store;
      this.key = key;
    }

    @Override
    public void applyStyles(TextStyle textStyle) {
      RGB rgb = PreferenceConverter.getColor(store, key);
      if (rgb != null) {
        textStyle.background = DartUI.getColorManager().getColor(rgb);
      }
    }
  }

  /**
   * Coarse-grained kind of the reference. We don't need all details of {@link SearchResultKind}.
   */
  private static enum ReferenceKind {
    REFERENCE,
    READ,
    WRITE;
    public static ReferenceKind of(String searchResultKind) {
      if (searchResultKind.equals(SearchResultKind.READ)) {
        return READ;
      }
      if (searchResultKind.equals(SearchResultKind.DECLARATION)
          || searchResultKind.equals(SearchResultKind.READ_WRITE)
          || searchResultKind.equals(SearchResultKind.WRITE)) {
        return WRITE;
      }
      return REFERENCE;
    }
  }

  /**
   * {@link ITreeContentProvider} for {@link ElementItem}s.
   */
  private static class SearchContentProvider implements ITreeContentProvider {
    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      // prepare item
      if (!(parentElement instanceof ElementItem)) {
        return ArrayUtils.EMPTY_OBJECT_ARRAY;
      }
      ElementItem item = (ElementItem) parentElement;
      // lines and sub-items
      List<Object> children = Lists.newArrayList();
      children.addAll(item.lines);
      children.addAll(item.children);
      return children.toArray(new Object[children.size()]);
    }

    @Override
    public Object[] getElements(Object inputElement) {
      return getChildren(inputElement);
    }

    @Override
    public Object getParent(Object element) {
      if (element instanceof ElementItem) {
        return ((ElementItem) element).parent;
      }
      if (element instanceof LineItem) {
        return ((LineItem) element).item;
      }
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      if (element instanceof ElementItem) {
        ElementItem item = (ElementItem) element;
        return !item.children.isEmpty() || !item.lines.isEmpty();
      }
      return false;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }

  /**
   * {@link ILabelProvider} for {@link ElementItem}s.
   */
  private static class SearchLabelProvider extends LabelProvider implements IStyledLabelProvider {
    private final ElementLabelProvider_NEW elementLabelProvider = new ElementLabelProvider_NEW();

    @Override
    public Image getImage(Object elem) {
      // element
      if (elem instanceof ElementItem) {
        ElementItem item = (ElementItem) elem;
        return elementLabelProvider.getImage(item.element);
      }
      // line
      if (elem instanceof LineItem) {
        LineItem item = (LineItem) elem;
        // has any write?
        for (LinePosition position : item.positions) {
          if (position.kind == ReferenceKind.WRITE) {
            return DartPluginImages.get(DartPluginImages.IMG_OBJS_SEARCH_WRITEACCESS);
          }
        }
        // has any read?
        for (LinePosition position : item.positions) {
          if (position.kind == ReferenceKind.READ) {
            return DartPluginImages.get(DartPluginImages.IMG_OBJS_SEARCH_READACCESS);
          }
        }
        // just some reference
        return DartPluginImages.get(DartPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
      }
      // unknown
      return null;
    }

    @Override
    public StyledString getStyledText(Object elem) {
      if (elem instanceof ElementItem) {
        ElementItem item = (ElementItem) elem;
        StyledString styledText = new StyledString(item.element.getName());
        if (item.numMatches == 1) {
          styledText.append(" (1 match)", StyledString.COUNTER_STYLER);
        } else if (item.numMatches > 1) {
          styledText.append(" (" + item.numMatches + " matches)", StyledString.COUNTER_STYLER);
        }
        return styledText;
      } else {
        LineItem item = (LineItem) elem;
        StyledString styledText = new StyledString(item.line.content);
        for (LinePosition linePosition : item.positions) {
          StyledString.Styler style = linePosition.kind == ReferenceKind.WRITE
              ? HIGHLIGHT_WRITE_STYLE : HIGHLIGHT_STYLE;
          int styleOffset = linePosition.positionSrc.offset - item.line.start;
          int styleLength = linePosition.positionSrc.length;
          styledText.setStyle(styleOffset, styleLength, style);
        }
        // may be potential match
        if (item.potential) {
          styledText.append(" ");
          styledText.append(" (potential match)", StyledString.DECORATIONS_STYLER);
        }
        // done
        return styledText;
      }
    }
  }

  @SuppressWarnings("restriction")
  private static final StyledString.Styler HIGHLIGHT_WRITE_STYLE = new PreferenceBackgroundStyler(
      org.eclipse.ui.internal.editors.text.EditorsPlugin.getDefault().getPreferenceStore(),
      "writeOccurrenceIndicationColor");

  @SuppressWarnings("restriction")
  private static final StyledString.Styler HIGHLIGHT_STYLE = new PreferenceBackgroundStyler(
      org.eclipse.ui.internal.editors.text.EditorsPlugin.getDefault().getPreferenceStore(),
      "searchResultIndicationColor");

  private static final String SETTINGS_ID = "SearchMatchPage";
  private static final String FILTER_SDK_ID = "filter_SDK";
  private static final String FILTER_POTENTIAL_ID = "filter_potential";
  private static final String FILTER_PROJECT_ID = "filter_project";

  private static final ITreeContentProvider CONTENT_PROVIDER = new SearchContentProvider();

  /**
   * Adds new {@link ElementItem} to the tree.
   */
  private static ElementItem addElementItem(Map<Element, ElementItem> itemMap, Element[] elements,
      int elementIndex) {
    // prepare child Element
    Element childElement = null;
    while (elementIndex < elements.length) {
      Element element = elements[elementIndex];
      if (isLocalElement(element)) {
        elementIndex++;
        continue;
      }
      childElement = element;
      break;
    }
    // put child
    ElementItem child;
    {
      ElementItem existingChild = itemMap.get(childElement);
      if (existingChild == null) {
        child = new ElementItem(childElement);
        itemMap.put(childElement, child);
      } else {
        child = existingChild;
      }
    }
    // bind child to parent
    if (childElement != null) {
      elementIndex++;
      ElementItem parent = addElementItem(itemMap, elements, elementIndex);
      parent.addChild(child);
    }
    // done
    return child;
  }

  /**
   * Builds {@link ElementItem} tree out of the given {@link SearchResult}s.
   */
  private static ElementItem buildElementItemTree(List<SearchResult> searchResults) {
    FileLineProvider sourceLineProvider = new FileLineProvider();
    ElementItem rootItem = new ElementItem(null);
    Map<Element, ElementItem> itemMap = Maps.newHashMap();
    itemMap.put(null, rootItem);
    for (SearchResult searchResult : searchResults) {
      List<Element> elements = searchResult.getPath();
      Element[] elementArray = elements.toArray(new Element[elements.size()]);
      ElementItem elementItem = addElementItem(itemMap, elementArray, 0);
      elementItem.addMatch(sourceLineProvider, searchResult);
    }
    calculateNumMatches(rootItem);
    sortLines(rootItem);
    linkLeaves(rootItem, null);
    return rootItem;
  }

  /**
   * Recursively calculates {@link ElementItem#numMatches} fields.
   */
  private static int calculateNumMatches(ElementItem item) {
    int result = 0;
    for (LineItem lineItem : item.lines) {
      result += lineItem.positions.size();
    }
    for (ElementItem child : item.children) {
      result += calculateNumMatches(child);
    }
    item.numMatches = result;
    return result;
  }

  private static IDialogSettings getDialogSettings() {
    return DartToolsPlugin.getDefault().getDialogSettingsSection(SETTINGS_ID);
  }

  private static boolean isLocalElement(Element element) {
    String kind = element.getKind();
    if (kind.equals(ElementKind.LOCAL_VARIABLE) || kind.equals(ElementKind.PARAMETER)) {
      return true;
    }
    // TODO(scheglov) consider adding LOCAL_FUNCTION
    return false;
  }

  /**
   * Recursively visits {@link ElementItem} and links leaves.
   * 
   * @return the last {@link ElementItem} leaf in the sub-tree.
   */
  private static ElementItem linkLeaves(ElementItem item, ElementItem prev) {
    // leaf
    if (item.children.isEmpty()) {
      if (prev != null) {
        prev.next = item;
      }
      item.prev = prev;
      prev = item;
      return item;
    }
    // container
    ElementItem lastLeaf = prev;
    item.next = item.children.get(0);
    for (ElementItem child : item.children) {
      lastLeaf = linkLeaves(child, lastLeaf);
    }
    return lastLeaf;
  }

  /**
   * Reveals the given {@link Position} in the {@link IEditorPart}.
   */
  private static void revealInEditor(IEditorPart editor, Position position) {
    SourceRange sourceRange = new SourceRange(position.offset, position.length);
    EditorUtility.revealInEditor(editor, sourceRange);
  }

  /**
   * Recursively sorts {@link ElementItem}s and {@link LineItem}s.
   */
  private static void sortLines(ElementItem item) {
    // process lines
    Collections.sort(item.lines, new Comparator<LineItem>() {
      @Override
      public int compare(LineItem o1, LineItem o2) {
        return o1.line.start - o2.line.start;
      }
    });
    for (LineItem lineItem : item.lines) {
      Collections.sort(lineItem.positions, new Comparator<LinePosition>() {
        @Override
        public int compare(LinePosition o1, LinePosition o2) {
          return o1.position.offset - o2.position.offset;
        }
      });
    }
    // process children
    Collections.sort(item.children, new Comparator<ElementItem>() {
      @Override
      public int compare(ElementItem o1, ElementItem o2) {
        return o1.element.getLocation().getOffset() - o2.element.getLocation().getOffset();
      }
    });
    for (ElementItem child : item.children) {
      sortLines(child);
    }
  }

  private IAction removeAction = new Action() {
    {
      setToolTipText("Remove Selected Matches");
      DartPluginImages.setLocalImageDescriptors(this, "search_rem.gif");
    }

    @Override
    public void run() {
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
      for (Iterator<?> iter = selection.toList().iterator(); iter.hasNext();) {
        Object obj = iter.next();
        // LineItem
        if (obj instanceof LineItem) {
          LineItem lineItem = (LineItem) obj;
          ElementItem parentItem = lineItem.item;
          List<LineItem> parentLines = parentItem.lines;
          // remove this line
          parentLines.remove(lineItem);
          // it no more lines, remove parent item too
          if (parentLines.isEmpty()) {
            obj = parentItem;
          }
        }
        // ResultItem
        if (obj instanceof ElementItem) {
          ElementItem item = (ElementItem) obj;
          while (item != null && item.element != null) {
            ElementItem parent = item.parent;
            parent.children.remove(item);
            if (!parent.children.isEmpty()) {
              break;
            }
            item = parent;
          }
        }
      }
      calculateNumMatches(rootItem);
      // update viewer
      viewer.refresh();
      // update markers
      addMarkers();
    }
  };

  private IAction removeAllAction = new Action() {
    {
      setToolTipText("Remove All Matches");
      DartPluginImages.setLocalImageDescriptors(this, "search_remall.gif");
    }

    @Override
    public void run() {
      close();
    }
  };

  private IAction refreshAction = new Action() {
    {
      setToolTipText("Refresh the Current Search");
      DartPluginImages.setLocalImageDescriptors(this, "refresh.gif");
    }

    @Override
    public void run() {
      refresh();
    }
  };

  private IAction expandAllAction = new Action() {
    {
      setToolTipText("Expand All");
      DartPluginImages.setLocalImageDescriptors(this, "expandall.gif");
    }

    @Override
    public void run() {
      viewer.expandAll();
    }
  };

  private IAction collapseAllAction = new Action() {
    {
      setToolTipText("Collapse All");
      DartPluginImages.setLocalImageDescriptors(this, "collapseall.gif");
    }

    @Override
    public void run() {
      viewer.collapseAll();
    }
  };

  private IAction nextAction = new Action() {
    {
      setToolTipText("Show Next Match");
      DartPluginImages.setLocalImageDescriptors(this, "search_next.gif");
    }

    @Override
    public void run() {
      openItemNext();
    }
  };

  private IAction prevAction = new Action() {
    {
      setToolTipText("Show Previous Match");
      DartPluginImages.setLocalImageDescriptors(this, "search_prev.gif");
    }

    @Override
    public void run() {
      openItemPrev();
    }
  };

  private IAction filterSdkAction = new Action(null, IAction.AS_CHECK_BOX) {
    {
      setToolTipText("Hide SDK and package matches");
      DartPluginImages.setLocalImageDescriptors(this, "search_filter_sdk.png");
    }

    @Override
    public void run() {
      filterEnabledSdk = isChecked();
      getDialogSettings().put(FILTER_SDK_ID, filterEnabledSdk);
      refresh();
    }
  };

  private IAction filterPotentialAction = new Action(null, IAction.AS_CHECK_BOX) {
    {
      setToolTipText("Hide potential matches");
      DartPluginImages.setLocalImageDescriptors(this, "search_filter_potential.png");
    }

    @Override
    public void run() {
      filterEnabledPotential = isChecked();
      getDialogSettings().put(FILTER_POTENTIAL_ID, filterEnabledPotential);
      refresh();
    }
  };

  private IAction filterProjectAction = new Action(null, IAction.AS_CHECK_BOX) {
    {
      setToolTipText("Show only current project actions");
      DartPluginImages.setLocalImageDescriptors(this, "search_filter_project.gif");
    }

    @Override
    public void run() {
      filterEnabledProject = isChecked();
      getDialogSettings().put(FILTER_PROJECT_ID, filterEnabledProject);
      refresh();
    }
  };

  private final SearchView searchView;
  private final String taskName;

  private final Set<IResource> markerResources = Sets.newHashSet();

  private TreeViewer viewer;
  private IPreferenceStore preferences;

  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      updateColors();
    }
  };
  private ElementItem rootItem;
  private ItemCursor itemCursor;

  private PositionTracker positionTracker;
  private boolean filterEnabledSdk = false;
  private boolean filterEnabledPotential = false;
  private boolean filterEnabledProject = false;
  private int filteredCountSdk = 0;
  private int filteredCountPotential = 0;
  private int filteredCountProject = 0;

  private static final Predicate<SearchResult> FILTER_SDK = new Predicate<SearchResult>() {
    @Override
    public boolean apply(SearchResult input) {
      // TODO (jwren/scheglov) SearchResult API has changed
//      Source source = input.getSource();
//      UriKind uriKind = source.getUriKind();
//      return uriKind == UriKind.DART_URI || uriKind == UriKind.PACKAGE_URI;
      return false;
    }
  };

  private static final Predicate<SearchResult> FILTER_POTENTIAL = new Predicate<SearchResult>() {
    @Override
    public boolean apply(SearchResult input) {
      return input.isPotential();
    }
  };

  private final Predicate<SearchResult> FILTER_PROJECT = new Predicate<SearchResult>() {
    @Override
    public boolean apply(SearchResult input) {
      IProject currentProject = getCurrentProject();
      String file = input.getLocation().getFile();
      IFile resource = DartUI.getSourceFile(file);
      return resource != null && currentProject != null
          && currentProject.equals(resource.getProject());
    }
  };

  private long lastQueryStartTime = 0;

  private long lastQueryFinishTime = 0;

  public SearchResultPage_NEW(SearchView searchView, String taskName) {
    this.searchView = searchView;
    this.taskName = taskName;
  }

  @Override
  public void createControl(Composite parent) {
    initFilters();
    viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
    viewer.setContentProvider(CONTENT_PROVIDER);
    // NB(scheglov): don't attempt to share label provider - it is not allowed in JFace
    viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new SearchLabelProvider()));
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        ISelection selection = event.getSelection();
        openSelectedElement(selection);
      }
    });
    // update colors
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    preferences.addPropertyChangeListener(propertyChangeListener);
    updateColors();
    SWTUtil.bindJFaceResourcesFontToControl(viewer.getControl());
  }

  @Override
  public void dispose() {
    preferences.removePropertyChangeListener(propertyChangeListener);
    removeMarkers();
    disposePositionTracker();
    super.dispose();
  }

  @Override
  public Control getControl() {
    return viewer.getControl();
  }

  @Override
  public long getLastQueryFinishTime() {
    return lastQueryFinishTime;
  }

  @Override
  public long getLastQueryStartTime() {
    return lastQueryStartTime;
  }

  @Override
  public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
      IStatusLineManager statusLineManager) {
    toolBarManager.add(filterProjectAction);
    toolBarManager.add(filterSdkAction);
    toolBarManager.add(filterPotentialAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(nextAction);
    toolBarManager.add(prevAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(removeAction);
    toolBarManager.add(removeAllAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(expandAllAction);
    toolBarManager.add(collapseAllAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(refreshAction);
  }

  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  @Override
  public void show() {
    refresh();
  }

  /**
   * This is the first method called before performing refresh.
   */
  protected void beforeRefresh() {
  }

  /**
   * @return {@code true} if potential filter can be used.
   */
  protected boolean canUseFilterPotential() {
    return true;
  }

  /**
   * Clients may implement this method to allow "Only current project" filter.
   */
  protected IProject getCurrentProject() {
    return null;
  }

  /**
   * @return the description of the element we searched for.
   */
  protected abstract String getQueryElementName();

  /**
   * @return the description of the query we executed.
   */
  protected abstract String getQueryKindName();

  /**
   * Runs a search query.
   * 
   * @return the {@link SearchResult}s to display.
   */
  protected abstract List<SearchResult> runQuery();

  /**
   * Closes this page and removes all search markers
   */
  void close() {
    searchView.showPage(null);
  }

  /**
   * Adds markers for all {@link ElementItem}s starting from {@link #rootItem}.
   */
  private void addMarkers() {
    try {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          removeMarkers();
          markerResources.clear();
          addMarkers(rootItem);
        }
      }, null);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }

  /**
   * Adds markers for the given {@link ElementItem} and its children.
   */
  private void addMarkers(ElementItem item) throws CoreException {
    // add marker if leaf
    try {
      for (LineItem lineItem : item.lines) {
        IFile resource = DartUI.getSourceFile(lineItem.filePath);
        if (resource != null && resource.exists()) {
          markerResources.add(resource);
          List<LinePosition> positions = lineItem.positions;
          for (LinePosition linePosition : positions) {
            Position position = linePosition.position;
            IMarker marker = resource.createMarker(SearchView.SEARCH_MARKER);
            marker.setAttribute(IMarker.CHAR_START, position.getOffset());
            marker.setAttribute(IMarker.CHAR_END, position.getOffset() + position.getLength());
          }
        }
      }
    } catch (Throwable e) {
    }
    // process children
    for (ElementItem child : item.children) {
      addMarkers(child);
    }
  }

  private List<SearchResult> applyFilters(List<SearchResult> matches) {
    filteredCountSdk = 0;
    filteredCountPotential = 0;
    filteredCountProject = 0;
    IProject currentProject = getCurrentProject();
    List<SearchResult> filtered = Lists.newArrayList();
    for (SearchResult match : matches) {
      // SDK filter
      if (FILTER_SDK.apply(match)) {
        filteredCountSdk++;
        if (filterEnabledSdk) {
          continue;
        }
      }
      // potential filter
      if (canUseFilterPotential()) {
        if (FILTER_POTENTIAL.apply(match)) {
          filteredCountPotential++;
          if (filterEnabledPotential) {
            continue;
          }
        }
      }
      // project filter
      if (currentProject != null) {
        if (FILTER_PROJECT.apply(match)) {
          filteredCountProject++;
        } else if (filterEnabledProject) {
          continue;
        }
      }
      // OK
      filtered.add(match);
    }
    return filtered;
  }

  /**
   * Disposes {@link #positionTracker}.
   */
  private void disposePositionTracker() {
    if (positionTracker == null) {
      return;
    }
    positionTracker.dispose();
    positionTracker = null;
  }

  /**
   * Worker method for {@link #expandTreeItemsTimeBoxed(List, long)}.
   */
  private long expandTreeItemsTimeBoxed(List<ElementItem> items, int childrenLimit, long nanoBudget) {
    for (ElementItem item : items) {
      if (nanoBudget < 0) {
        return -1;
      }
      if (item.children.size() <= childrenLimit) {
        // expand single item
        {
          long startNano = System.nanoTime();
          viewer.setExpandedState(item, true);
          nanoBudget -= System.nanoTime() - startNano;
        }
        // expand children
        nanoBudget = expandTreeItemsTimeBoxed(item.children, childrenLimit, nanoBudget);
        if (nanoBudget < 0) {
          return -1;
        }
      }
    }
    return nanoBudget;
  }

  /**
   * Analyzes each {@link ElementItem} and expends it in {@link #viewer} only if it has not too much
   * children. So, user will see enough information, but not too much.
   */
  private void expandTreeItemsTimeBoxed(List<ElementItem> items, long nanoBudget) {
    int numIterations = 10;
    int childrenLimit = 10;
    for (int i = 0; i < numIterations; i++) {
      if (nanoBudget < 0) {
        break;
      }
      nanoBudget = expandTreeItemsTimeBoxed(items, childrenLimit, nanoBudget);
      childrenLimit *= 2;
    }
  }

  /**
   * Initializes filters from {@link IDialogSettings}.
   */
  private void initFilters() {
    IDialogSettings settings = getDialogSettings();
    if (settings.getBoolean(FILTER_SDK_ID)) {
      filterEnabledSdk = true;
      filterSdkAction.setChecked(true);
    }
    if (settings.getBoolean(FILTER_POTENTIAL_ID)) {
      filterEnabledPotential = true;
      filterPotentialAction.setChecked(true);
    }
    if (settings.getBoolean(FILTER_PROJECT_ID)) {
      filterEnabledProject = true;
      filterProjectAction.setChecked(true);
    }
    if (!canUseFilterPotential()) {
      filterPotentialAction.setEnabled(false);
      filterPotentialAction.setChecked(false);
    }
    if (getCurrentProject() == null) {
      filterProjectAction.setEnabled(false);
      filterProjectAction.setChecked(false);
    }
  }

  /**
   * Opens {@link DartEditor} with the next {@link Position} in the same of the next
   * {@link ElementItem}.
   */
  private void openItemNext() {
    boolean changed = itemCursor.next();
    if (changed) {
      showCursor();
    }
  }

  /**
   * Opens {@link DartEditor} with the previous {@link Position} in the same of the previous
   * {@link ElementItem}.
   */
  private void openItemPrev() {
    boolean changed = itemCursor.prev();
    if (changed) {
      showCursor();
    }
  }

  /**
   * Opens "position" in the "filePath" file.
   */
  private void openPosition(String filePath, Position position) {
    try {
      DartUI.openFilePath(filePath, true, position.offset, position.length);
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Search", "Exception during open.");
    }
  }

  /**
   * Opens selected {@link ElementItem} in the editor.
   */
  private void openSelectedElement(ISelection selection) {
    // need IStructuredSelection
    if (!(selection instanceof IStructuredSelection)) {
      return;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    // only single element
    if (structuredSelection.size() != 1) {
      return;
    }
    Object firstElement = structuredSelection.getFirstElement();
    // line item
    if (firstElement instanceof LineItem) {
      LineItem lineItem = (LineItem) firstElement;
      Position position = lineItem.positions.get(0).position;
      openPosition(lineItem.filePath, position);
    }
    // element item
    if (firstElement instanceof ElementItem) {
      ElementItem item = (ElementItem) firstElement;
      // use ResultCursor to find first occurrence in the requested subtree
      itemCursor = new ItemCursor(item);
      boolean found = itemCursor.next();
      if (!found) {
        return;
      }
      String filePath = itemCursor.getLine().filePath;
      Position position = itemCursor.getPosition();
      // open position
      openPosition(filePath, position);
    }
  }

  /**
   * Runs background {@link Job} to fetch {@link SearchMatch}s and then displays them in the
   * {@link #viewer}.
   */
  private void refresh() {
    try {
      lastQueryStartTime = System.currentTimeMillis();
      new Job(taskName) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          beforeRefresh();
          // do query
          List<SearchResult> results = runQuery();
          int totalCount = results.size();
          results = applyFilters(results);
          // set description
          String filtersDesc;
          {
            filtersDesc = "";
            filtersDesc += ",   SDK: " + filteredCountSdk;
            if (filterEnabledSdk) {
              filtersDesc += " (filtered)";
            }
            if (canUseFilterPotential()) {
              filtersDesc += ",   potential: " + filteredCountPotential;
              if (filterEnabledPotential) {
                filtersDesc += " (filtered)";
              }
            }
            if (getCurrentProject() != null) {
              filtersDesc += ",   in project: " + filteredCountProject;
              if (filterEnabledProject) {
                filtersDesc += " (only)";
              }
            }
          }
          setContentDescription("'" + getQueryElementName() + "' - " + results.size() + " "
              + getQueryKindName() + ",   total: " + totalCount + filtersDesc);
          // process query results
          rootItem = buildElementItemTree(results);
          itemCursor = new ItemCursor(rootItem);
          trackPositions();
          // add markers
          addMarkers();
          // schedule UI update
          new UIJob("Displaying search results...") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
              // may be already disposed (e.g. new search was done)
              if (viewer.getControl().isDisposed()) {
                return Status.OK_STATUS;
              }
              // set new input
              Object[] expandedElements = viewer.getExpandedElements();
              viewer.setInput(rootItem);
              viewer.setExpandedElements(expandedElements);
              // expand
              expandTreeItemsTimeBoxed(rootItem.children, 75L * 1000000L);
              lastQueryFinishTime = System.currentTimeMillis();
              return Status.OK_STATUS;
            }
          }.schedule();
          // done
          return Status.OK_STATUS;
        }
      }.schedule();
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Search", "Exception during search.");
    }
  }

  /**
   * Removes all search markers from {@link #markerResources}.
   */
  private void removeMarkers() {
    try {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          for (IResource resource : markerResources) {
            if (resource.exists()) {
              try {
                resource.deleteMarkers(SearchView.SEARCH_MARKER, false, IResource.DEPTH_ZERO);
              } catch (Throwable e) {
              }
            }
          }
        }
      }, null);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }

  /**
   * Shows given text as description for {@link SearchView}.
   */
  private void setContentDescription(final String description) {
    ExecutionUtils.runRethrowUI(new RunnableEx() {
      @Override
      public void run() throws Exception {
        searchView.setContentDescription(description);
      }
    });
  }

  /**
   * Shows current {@link #itemCursor} state.
   */
  private void showCursor() {
    try {
      LineItem lineItem = itemCursor.getLine();
      viewer.setSelection(new StructuredSelection(lineItem), true);
      // open editor
      IEditorPart editor = DartUI.openFilePath(lineItem.filePath, false);
      // show Position
      Position position = itemCursor.getPosition();
      if (position != null) {
        revealInEditor(editor, position);
      }
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Search", "Exception during open.");
    }
  }

  /**
   * Starts tracking all search result positions in {@link #positionTracker}.
   */
  private void trackPositions() {
    disposePositionTracker();
    positionTracker = new PositionTracker();
    trackPositions(rootItem);
  }

  /**
   * Recursively visits {@link ElementItem} and tracks all {@link Position}s.
   */
  private void trackPositions(ElementItem item) {
    // do track positions
    if (item.element != null) {
      for (LineItem lineItem : item.lines) {
        IFile file = DartUI.getSourceFile(lineItem.filePath);
        if (file != null) {
          List<LinePosition> positions = lineItem.positions;
          for (LinePosition linePosition : positions) {
            Position position = linePosition.position;
            positionTracker.trackPosition(file, position);
          }
        }
      }
    }
    // process children
    for (ElementItem child : item.children) {
      trackPositions(child);
    }
  }

  private void updateColors() {
    if (viewer.getTree().isDisposed()) {
      return;
    }
    SWTUtil.setColors(viewer.getTree(), preferences);
    viewer.refresh();
  }
}
