/*
 * /* Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.dialogs;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.FindTopLevelDeclarationsConsumer;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.ElementKind;
import com.google.dart.server.generated.types.RequestError;
import com.google.dart.server.generated.types.SearchResult;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.SearchResultsListener;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.dialogs.ITypeInfoFilterExtension;
import com.google.dart.tools.ui.dialogs.ITypeInfoImageProvider;
import com.google.dart.tools.ui.dialogs.ITypeSelectionComponent;
import com.google.dart.tools.ui.dialogs.TypeSelectionExtension;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.util.CamelUtil;
import com.google.dart.tools.ui.internal.util.Strings;
import com.google.dart.tools.ui.internal.util.TypeNameMatchLabelProvider_NEW;
import com.google.dart.tools.ui.internal.viewsupport.ColoredDartElementLabels;
import com.google.dart.tools.ui.internal.viewsupport.ColoredString;
import com.google.dart.tools.ui.internal.viewsupport.ColoredViewersManager;
import com.google.dart.tools.ui.internal.viewsupport.OwnerDrawSupport;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.WorkingSetFilterActionGroup;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SearchPattern;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Shows a list of Dart types to the user with a text entry field for a string pattern used to
 * filter the list of types.
 */
public class FilteredTypesSelectionDialog_NEW extends FilteredItemsSelectionDialog implements
    ITypeSelectionComponent {

  /**
   * The <code>ShowContainerForDuplicatesAction</code> provides means to show/hide container
   * information for duplicate elements.
   */
  private class ShowContainerForDuplicatesAction extends Action {

    /**
     * Creates a new instance of the class
     */
    public ShowContainerForDuplicatesAction() {
      super(
          DartUIMessages.FilteredTypeSelectionDialog_showContainerForDuplicatesAction,
          IAction.AS_CHECK_BOX);
    }

    @Override
    public void run() {
      fTypeInfoLabelProvider.setContainerInfo(isChecked());
    }
  }

  private class TopLevelElement {

    private Element element;
    private SearchResult searchResult;

    public TopLevelElement(SearchResult result) {
      this.element = result.getPath().get(0);
      this.searchResult = result;
    }

    public Element getElement() {
      return element;
    }

    public String getInfo() {
      String info = "";
      String file = element.getLocation().getFile();
      IFile resource = DartUI.getSourceFile(file);
      if (resource != null) {
        info = resource.getFullPath().toString();
      }
      if (info.startsWith("/")) {
        return info.substring(1);
      }
      return info;
    }

    public String getLibraryName() {
      Element e = getLibraryElement();
      if (e != null) {
        return e.getName();
      }
      return "";
    }

    public String getName() {
      return element.getName();
    }

    public void saveState(IMemento memento) {
      memento.putString("SEARCHRESULT", searchResult.toJson().toString());
    }

    private Element getLibraryElement() {
      for (Element element : searchResult.getPath()) {
        if (element.getKind().equals(ElementKind.LIBRARY)) {
          return element;
        }
      }
      return null;
    }
  }

  private static class TypeInfoUtil {

    public TypeInfoUtil(ITypeInfoImageProvider extension) {

    }

    public String getFullyQualifiedText(TopLevelElement element) {
      StringBuffer result = new StringBuffer();
      result.append(element.getName());
//      String libraryName = element.getLibraryName();
//      if (libraryName.length() > 0) {
//        result.append(DartElementLabels.CONCAT_STRING);
//        result.append(libraryName);
//      }
//      String containerName = element.getInfo();
//      if (containerName.length() > 0) {
//        result.append(DartElementLabels.CONCAT_STRING);
//        result.append(containerName);
//      }
      return result.toString();
    }

//    public ImageDescriptor getImageDescriptor(Object object) {
//      DartElement element = (DartElement) object;
//      if (fProviderExtension != null) {
//        try {
//          fAdapter.matchFound(new SearchMatch(
//              MatchQuality.EXACT,
//              element,
//              ((SourceReference) element).getSourceRange()));
//        } catch (DartModelException e) {
//          DartToolsPlugin.log(e);
//        }
////        ImageDescriptor descriptor = fProviderExtension.getImageDescriptor(fAdapter);
//
//        // TODO(brianwilkerson) This needs to return a different descriptor for function type aliases.
//        ImageDescriptor descriptor = DartElementImageProvider.getTypeImageDescriptor(false, false);
//
//        if (descriptor != null) {
//          return descriptor;
//        }
//      }
//      // TODO(brianwilkerson) This needs to return a different descriptor for function type aliases.
//      return DartElementImageProvider.getTypeImageDescriptor(false, false);
//    }

    public String getQualificationText(TopLevelElement element) {
      StringBuffer result = new StringBuffer();
      String libraryName = element.getLibraryName();
      if (libraryName.length() > 0) {
        result.append(element.getLibraryName());
      }
      String path = element.getInfo();
      if (path.length() > 0) {
        if (libraryName.length() > 0) {
          result.append(DartElementLabels.CONCAT_STRING);
        }
        result.append(path);
      }
      return result.toString();
    }

    public String getQualifiedText(TopLevelElement element) {
      StringBuffer result = new StringBuffer();
      result.append(element.getName());
      String libraryName = element.getLibraryName();
      if (libraryName.length() > 0) {
        result.append(DartElementLabels.CONCAT_STRING);
        result.append(libraryName);
      }
      return result.toString();
    }

    public String getText(Object element) {
      return ((TopLevelElement) element).getName();
    }

  }

  /**
   * A <code>LabelProvider</code> for the label showing type details.
   */
  private static class TypeItemDetailsLabelProvider extends LabelProvider {

    //   private final DartElementLabelProvider imageProvider = new DartElementLabelProvider();

    private final TypeInfoUtil fTypeInfoUtil;

    public TypeItemDetailsLabelProvider(TypeInfoUtil typeInfoUtil) {
      fTypeInfoUtil = typeInfoUtil;
    }

    @Override
    public Image getImage(Object object) {
      return super.getImage(object);
    }

    @Override
    public String getText(Object element) {
      if (element instanceof TopLevelElement) {
        return fTypeInfoUtil.getQualificationText((TopLevelElement) element);
      }

      return super.getText(element);
    }
  }

  /**
   * A <code>LabelProvider</code> for (the table of) types.
   */
  private class TypeItemLabelProvider extends LabelProvider implements ILabelDecorator {

    private final TypeNameMatchLabelProvider_NEW fLabelProvider = new TypeNameMatchLabelProvider_NEW(
        TypeNameMatchLabelProvider_NEW.SHOW_TYPE_CONTAINER_ONLY
            + TypeNameMatchLabelProvider_NEW.SHOW_ROOT_POSTFIX);

    private boolean fContainerInfo;

    /**
     * Construct a new <code>TypeItemLabelProvider</code>. F
     */
    public TypeItemLabelProvider() {

    }

    @Override
    public Image decorateImage(Image image, Object element) {
      return null;
    }

    @Override
    public String decorateText(String text, Object element) {
      if (!(element instanceof TopLevelElement)) {
        return null;
      }

      if (fContainerInfo && isDuplicateElement(element)) {
        return fTypeInfoUtil.getFullyQualifiedText((TopLevelElement) element);
      }

      return fTypeInfoUtil.getQualifiedText((TopLevelElement) element);
    }

    @Override
    public Image getImage(Object element) {
      if (!(element instanceof TopLevelElement)) {
        return super.getImage(element);
      }
      return fLabelProvider.getImage(((TopLevelElement) element).getElement());
    }

    @Override
    public String getText(Object element) {
      if (!(element instanceof TopLevelElement)) {
        return super.getText(element);
      }

      if (fContainerInfo && isDuplicateElement(element)) {
        return fTypeInfoUtil.getFullyQualifiedText((TopLevelElement) element);
      }

      if (!fContainerInfo && isDuplicateElement(element)) {
        return fTypeInfoUtil.getQualifiedText((TopLevelElement) element);
      }

      return fTypeInfoUtil.getText(element);
    }

    public void setContainerInfo(boolean containerInfo) {
      fContainerInfo = containerInfo;
      fireLabelProviderChanged(new LabelProviderChangedEvent(this));
    }

  }

  /**
   * Compares TypeItems is used during sorting
   */
  @SuppressWarnings("rawtypes")
  private static class TypeItemsComparator implements Comparator {

//    private final Map fLib2Name = new HashMap();

//    private final String[] fInstallLocations;
//
//    private final String[] fVMNames;

    /**
     * Creates new instance of TypeItemsComparator
     */
    public TypeItemsComparator() {
//      List locations = new ArrayList();
//      List labels = new ArrayList();
//      IVMInstallType[] installs = JavaRuntime.getVMInstallTypes();
//      for (int i = 0; i < installs.length; i++) {
//        processVMInstallType(installs[i], locations, labels);
//      }
//      fInstallLocations = (String[]) locations.toArray(new String[locations.size()]);
//      fVMNames = (String[]) labels.toArray(new String[labels.size()]);
    }

    @Override
    public int compare(Object left, Object right) {

      TopLevelElement leftInfo = (TopLevelElement) left;
      TopLevelElement rightInfo = (TopLevelElement) right;
      return compareName(leftInfo.getName(), rightInfo.getName());
    }

    private int compareName(String leftString, String rightString) {
      int result = leftString.compareToIgnoreCase(rightString);
      if (result != 0 || rightString.length() == 0) {
        return result;
      } else if (Strings.isLowerCase(leftString.charAt(0))
          && !Strings.isLowerCase(rightString.charAt(0))) {
        return +1;
      } else if (Strings.isLowerCase(rightString.charAt(0))
          && !Strings.isLowerCase(leftString.charAt(0))) {
        return -1;
      } else {
        return leftString.compareTo(rightString);
      }
    }
  }

  /**
   * Filters types using pattern, scope, element kind and filter extension.
   */
  @SuppressWarnings("unused")
  private class TypeItemsFilter extends ItemsFilter {

    private final boolean fIsWorkspaceScope;

    private final int fElemKind;

    private final ITypeInfoFilterExtension fFilterExt;

//    private final TypeInfoRequestorAdapter fAdapter = new TypeInfoRequestorAdapter();

    private SearchPattern fPackageMatcher;

    private boolean fMatchEverything = false;

    private final int fMyTypeFilterVersion = fTypeFilterVersion;

    /**
     * Creates instance of TypeItemsFilter
     * 
     * @param scope
     * @param elementKind
     * @param extension
     */
    public TypeItemsFilter(int elementKind, ITypeInfoFilterExtension extension) {
      super(new TypeSearchPattern());
      fIsWorkspaceScope = true;
      fElemKind = elementKind;
      fFilterExt = extension;
      String stringPackage = ((TypeSearchPattern) patternMatcher).getPackagePattern();
      if (stringPackage != null) {
        fPackageMatcher = new SearchPattern();
        fPackageMatcher.setPattern(stringPackage);
      } else {
        fPackageMatcher = null;
      }
    }

    @Override
    public boolean equalsFilter(ItemsFilter iFilter) {
      if (!super.equalsFilter(iFilter)) {
        return false;
      }
      if (!(iFilter instanceof TypeItemsFilter)) {
        return false;
      }
      TypeItemsFilter typeItemsFilter = (TypeItemsFilter) iFilter;

      if (fMyTypeFilterVersion != typeItemsFilter.getMyTypeFilterVersion()) {
        return false;
      }
      return true;
    }

    public int getElementKind() {
      return fElemKind;
    }

    public ITypeInfoFilterExtension getFilterExtension() {
      return fFilterExt;
    }

    public int getMyTypeFilterVersion() {
      return fMyTypeFilterVersion;
    }

    public int getPackageFlags() {
      if (fPackageMatcher == null) {
        return SearchPattern.RULE_PREFIX_MATCH;
      }

      return fPackageMatcher.getMatchRule();
    }

    public String getPackagePattern() {
      if (fPackageMatcher == null) {
        return null;
      }
      return fPackageMatcher.getPattern();
    }

    @Override
    public boolean isConsistentItem(Object item) {
      return true;
    }

    @Override
    public boolean isSubFilter(ItemsFilter filter) {
      if (!super.isSubFilter(filter)) {
        return false;
      }
      TypeItemsFilter typeItemsFilter = (TypeItemsFilter) filter;

      if (fMyTypeFilterVersion != typeItemsFilter.getMyTypeFilterVersion()) {
        return false;
      }
      //TODO(pquitslund): this forces a full refresh which works-around filter application refresh issues
      return true;
    }

    public boolean matchesCachedResult(SearchResult result) {
      if (!(matchesFilterExtension(result))) {
        return false;
      }
      return matchesName(result);
    }

    public boolean matchesFilterExtension(Object result) {
      return true;
    }

    public boolean matchesHistoryElement(SearchResult result) {
      if (!(matchesScope(result) && matchesFilterExtension(result))) {
        return false;
      }
      return matchesName(result);
    }

    @Override
    public boolean matchesRawNamePattern(Object item) {
      Element element = (Element) item;
      return !element.getName().endsWith(".dart");
    }

    public boolean matchesRawNamePattern(SearchResult result) {
      List<Element> elements = result.getPath();
      if (!result.getPath().isEmpty()) {
        return Strings.startsWithIgnoreCase(result.getPath().get(0).getName(), getPattern());
      }
      return false;
    }

    @Override
    public boolean matchItem(Object item) {

      if (fMatchEverything) {
        return true;
      }

      TopLevelElement element = (TopLevelElement) item;
      if (!(matchesScope(element) && matchesFilterExtension(element))) {
        return false;
      }
      return matchesName(element);
    }

    /**
     * Set filter to "match everything" mode.
     * 
     * @param matchEverything if <code>true</code>, {@link #matchItem(Object)} always returns true.
     *          If <code>false</code>, the filter is enabled.
     */
    public void setMatchEverythingMode(boolean matchEverything) {
      this.fMatchEverything = matchEverything;
    }

    private boolean matchesName(SearchResult result) {
      return matches(result.getPath().get(0).getName());
    }

    private boolean matchesName(TopLevelElement element) {
      return matches(element.getName());
    }

    private boolean matchesScope(Object element) {
      return true;
    }

  }

  /**
   * Extends functionality of SearchPatterns
   */
  private static class TypeSearchPattern extends SearchPattern {

    private String packagePattern;

    /**
     * @return the packagePattern
     */
    public String getPackagePattern() {
      return packagePattern;
    }

    @Override
    public void setPattern(String stringPattern) {
      String pattern = stringPattern;
      String packPattern = null;
      int index = stringPattern.lastIndexOf("."); //$NON-NLS-1$
      if (index != -1) {
        packPattern = evaluatePackagePattern(stringPattern.substring(0, index));
        pattern = stringPattern.substring(index + 1);
        if (pattern.length() == 0) {
          pattern = "**"; //$NON-NLS-1$
        }
      }
      super.setPattern(pattern);
      packagePattern = packPattern;
    }

    @Override
    protected boolean isNameCharAllowed(char nameChar) {
      return super.isNameCharAllowed(nameChar);
    }

    @Override
    protected boolean isPatternCharAllowed(char patternChar) {
      return super.isPatternCharAllowed(patternChar);
    }

    @Override
    protected boolean isValidCamelCaseChar(char ch) {
      return super.isValidCamelCaseChar(ch);
    }

    /*
     * Transforms o.e.j to o*.e*.j*
     */
    private String evaluatePackagePattern(String s) {
      StringBuffer buf = new StringBuffer();
      boolean hasWildCard = false;
      for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        if (ch == '.') {
          if (!hasWildCard) {
            buf.append('*');
          }
          hasWildCard = false;
        } else if (ch == '*' || ch == '?') {
          hasWildCard = true;
        }
        buf.append(ch);
      }
      if (!hasWildCard) {
        buf.append('*');
      }
      return buf.toString();
    }

  }

  /**
   * A <code>TypeSearchRequestor</code> collects matches filtered using <code>TypeItemsFilter</code>
   * . The attached content provider is filled on the basis of the collected entries (instances of
   * <code>TypeNameMatch</code> ).
   */
  private class TypeSearchRequestor implements SearchResultsListener {

    private final AbstractContentProvider fContentProvider;

    private final TypeItemsFilter fTypeItemsFilter;

    public TypeSearchRequestor(AbstractContentProvider contentProvider,
        TypeItemsFilter typeItemsFilter) {
      super();
      fContentProvider = contentProvider;
      fTypeItemsFilter = typeItemsFilter;
    }

    @Override
    public void computedSearchResults(List<SearchResult> searchResults, boolean last) {
      for (SearchResult searchResult : searchResults) {
        TopLevelElement result = new TopLevelElement(searchResult);
        results.add(result);

        //      if (fTypeItemsFilter.matchesFilterExtension(result)) {
        fContentProvider.add(result, fTypeItemsFilter);
        //    }
      }
      //TODO(pquitslund): this shouldn't be necessary (possibly remove once history is working)
      scheduleRefresh();
    }
  }

  private class TypeSelectionHistory extends SelectionHistory {

    @Override
    protected Object restoreItemFromMemento(IMemento memento) {
      SearchResult result = SearchResult.fromJson((JsonObject) new JsonParser().parse(memento.getString("SEARCHRESULT")));
      return new TopLevelElement(result);
    }

    @Override
    protected void storeItemToMemento(Object item, IMemento memento) {
      TopLevelElement element = (TopLevelElement) item;
      element.saveState(memento);
    }

  }

  /**
   * Disabled "Show Container for Duplicates because of
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=184693 .
   */
  private static final boolean BUG_184693 = true;

  private static final String DIALOG_SETTINGS = "com.google.dart.tools.ui.dialogs.FilteredTypesSelectionDialog"; //$NON-NLS-1$

  private static final String SHOW_CONTAINER_FOR_DUPLICATES = "ShowContainerForDuplicates"; //$NON-NLS-1$

  private static final String WORKINGS_SET_SETTINGS = "WorkingSet"; //$NON-NLS-1$

  private static String getIdentifierCharacters(String str) {
    int length = str.length();
    StringBuilder buf = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = str.charAt(i);
      if (Character.isJavaIdentifierPart(c)) {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  private WorkingSetFilterActionGroup fFilterActionGroup;

  private final TypeItemLabelProvider fTypeInfoLabelProvider;

  private String fTitle;

  private ShowContainerForDuplicatesAction fShowContainerForDuplicatesAction;

  private boolean fAllowScopeSwitching;

  private final int fElementKinds;

  private final ITypeInfoFilterExtension fFilterExtension;

  private final TypeSelectionExtension fExtension;

  private ISelectionStatusValidator fValidator;

  private final TypeInfoUtil fTypeInfoUtil;

  @SuppressWarnings("unused")
  private static boolean fgFirstTime = true;

  private final TypeItemsComparator fTypeItemsComparator;

  private int fTypeFilterVersion = 0;

  private List<TopLevelElement> results = new ArrayList<TopLevelElement>();

//  /**
//   * Creates new FilteredTypesSelectionDialog instance
//   * 
//   * @param parent shell to parent the dialog on
//   * @param multi <code>true</code> if multiple selection is allowed
//   * @param context context used to execute long-running operations associated with this dialog
//   * @param scope scope used when searching for types
//   * @param elementKinds flags defining nature of searched elements; the only valid values are:
//   *          <code>IJavaScriptSearchConstants.TYPE</code>
//   *          <code>IJavaScriptSearchConstants.ANNOTATION_TYPE</code>
//   *          <code>IJavaScriptSearchConstants.INTERFACE</code>
//   *          <code>IJavaScriptSearchConstants.ENUM</code>
//   *          <code>IJavaScriptSearchConstants.CLASS_AND_INTERFACE</code>
//   *          <code>IJavaScriptSearchConstants.CLASS_AND_ENUM</code>. Please note that the bitwise
//   *          OR combination of the elementary constants is not supported.
//   */
//  public FilteredTypesSelectionDialog(Shell parent, boolean multi, IRunnableContext context,
//      SearchScope scope, int elementKinds) {
//    this(parent, multi, context, scope, elementKinds, null);
//  }

  /**
   * Creates new FilteredTypesSelectionDialog instance.
   * 
   * @param shell shell to parent the dialog on
   * @param multi <code>true</code> if multiple selection is allowed
   * @param context context used to execute long-running operations associated with this dialog
   * @param scope scope used when searching for types. If the scope is <code>null</code>, then
   *          workspace is scope is used as default, and the user can choose a working set as scope.
   * @param elementKinds flags defining nature of searched elements (<em>currently ignored</em>).
   * @param extension an extension of the standard type selection dialog; See
   *          {@link TypeSelectionExtension}
   */
  public FilteredTypesSelectionDialog_NEW(Shell shell, boolean multi, IRunnableContext context,
      int elementKinds, TypeSelectionExtension extension) {
    super(shell, multi);

    setSelectionHistory(new TypeSelectionHistory());

    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        shell,
        DartHelpContextIds.TYPE_SELECTION_DIALOG2);

    fElementKinds = elementKinds;
    fExtension = extension;
    fFilterExtension = (extension == null) ? null : extension.getFilterExtension();

    if (extension != null) {
      fValidator = extension.getSelectionValidator();
    }

    fTypeInfoUtil = new TypeInfoUtil(extension != null ? extension.getImageProvider() : null);

    fTypeInfoLabelProvider = new TypeItemLabelProvider();

    setListLabelProvider(fTypeInfoLabelProvider);
    setListSelectionLabelDecorator(fTypeInfoLabelProvider);
    setDetailsLabelProvider(new TypeItemDetailsLabelProvider(fTypeInfoUtil));

    fTypeItemsComparator = new TypeItemsComparator();
  }

  @Override
  public void create() {
    super.create();
    Control patternControl = getPatternControl();
    if (patternControl instanceof Text) {
      TextFieldNavigationHandler.install((Text) patternControl);
    }
  }

  @Override
  public String getElementName(Object item) {
    return fTypeInfoUtil.getText(item);
  }

  @Override
  public int open() {
    if (getInitialPattern() == null) {
      IWorkbenchWindow window = DartToolsPlugin.getActiveWorkbenchWindow();
      if (window != null) {
        ISelection selection = window.getSelectionService().getSelection();
        if (selection instanceof ITextSelection) {
          String text = ((ITextSelection) selection).getText();
          if (text != null) {
            text = text.trim();
            if (text.length() > 0 && DartConventions.validateTypeName(text).isOK()) {
              setInitialPattern(text, FULL_SELECTION);
            }
          }
        }
      }
    }
    return super.open();
  }

  @Override
  public void reloadCache(boolean checkDuplicates, IProgressMonitor monitor) {
    IProgressMonitor remainingMonitor = monitor;
//TODO: enable history consistency runnable
//    if (ConsistencyRunnable.needsExecution()) {
//      monitor.beginTask(
//          DartUIMessages.TypeSelectionDialog_progress_consistency, 10);
//      try {
//        ConsistencyRunnable runnable = new ConsistencyRunnable();
//        runnable.run(new SubProgressMonitor(monitor, 1));
//      } catch (InvocationTargetException e) {
//        ExceptionHandler.handle(e,
//            DartUIMessages.TypeSelectionDialog_error3Title,
//            DartUIMessages.TypeSelectionDialog_error3Message);
//        close();
//        return;
//      } catch (InterruptedException e) {
//        // cancelled by user
//        close();
//        return;
//      }
//      remainingMonitor = new SubProgressMonitor(monitor, 9);
//    } else {
//      remainingMonitor = monitor;
//    }
    super.reloadCache(checkDuplicates, remainingMonitor);
    monitor.done();
  }

  @Override
  public void setTitle(String title) {
    super.setTitle(title);
    fTitle = title;
  }

  /**
   * Sets a new validator.
   * 
   * @param validator the new validator
   */
  public void setValidator(ISelectionStatusValidator validator) {
    fValidator = validator;
  }

  @Override
  public void triggerSearch() {
    fTypeFilterVersion++;
    applyFilter();
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);
    if (ColoredViewersManager.showColoredLabels()) {
      if (contents instanceof Composite) {
        Table listControl = findTableControl((Composite) contents);
        if (listControl != null) {
          installOwnerDraw(listControl);
        }
      }
    }
    return contents;
  }

  @Override
  protected Control createExtendedContentArea(Composite parent) {
    Control addition = null;

    if (fExtension != null) {

      addition = fExtension.createContentArea(parent);
      if (addition != null) {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        addition.setLayoutData(gd);

      }

      fExtension.initialize(this);
    }

    return addition;
  }

  @Override
  protected ItemsFilter createFilter() {
    return new TypeItemsFilter(fElementKinds, fFilterExtension);
  }

  @Override
  protected void fillContentProvider(final AbstractContentProvider provider,
      ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
    TypeItemsFilter typeSearchFilter = (TypeItemsFilter) itemsFilter;

    final SearchResultsListener requestor = new TypeSearchRequestor(provider, typeSearchFilter);

    //SearchEngine engine = SearchEngineFactory.createSearchEngine((WorkingCopyOwner) null);
    progressMonitor.setTaskName(DartUIMessages.FilteredTypesSelectionDialog_searchJob_taskName);

    /*
     * Setting the filter into match everything mode avoids filtering twice by the same pattern (the
     * search engine only provides filtered matches). For the case when the pattern is a camel case
     * pattern with a terminator, the filter is not set to match everything mode because jdt.core's
     * SearchPattern does not support that case.
     */
    String typePattern = itemsFilter.getPattern();
//    int matchRule = typeSearchFilter.getMatchRule();
//    if (matchRule == SearchPattern.RULE_CAMELCASE_MATCH) {
//      // If the pattern is empty, the RULE_BLANK_MATCH will be chosen, so we
//      // don't have to check the pattern length
//      char lastChar = typePattern.charAt(typePattern.length() - 1);
//
//      if (lastChar == '<' || lastChar == ' ') {
//        typePattern = typePattern.substring(0, typePattern.length() - 1);
//      } else {
//        typeSearchFilter.setMatchEverythingMode(true);
//      }
//    } else {
//      typeSearchFilter.setMatchEverythingMode(true);
//    }
    typePattern = getIdentifierCharacters(typePattern);
    final String pattern = "^" + CamelUtil.getCamelCaseRegExp(typePattern) + ".*";

    try {
      //     boolean searchComplete = false;
      results.clear();
      final CountDownLatch latch = new CountDownLatch(1);
      DartCore.getAnalysisServer().search_findTopLevelDeclarations(
          pattern,
          new FindTopLevelDeclarationsConsumer() {
            @Override
            public void computedSearchId(String searchId) {
              DartCore.getAnalysisServerData().addSearchResultsListener(searchId, requestor);
            }

            @Override
            public void onError(RequestError requestError) {
            }
          });
      Uninterruptibles.awaitUninterruptibly(latch, 5, TimeUnit.SECONDS);
      //     searchComplete = true;
    } finally {
      //  typeSearchFilter.setMatchEverythingMode(false);
    }
  }

  @Override
  protected void fillViewMenu(IMenuManager menuManager) {
    super.fillViewMenu(menuManager);

    if (!BUG_184693) {
      fShowContainerForDuplicatesAction = new ShowContainerForDuplicatesAction();
      menuManager.add(fShowContainerForDuplicatesAction);
    }

    menuManager.add(new Separator());
//TODO: add/remove type filter preferences
//    menuManager.add(new TypeFiltersPreferencesAction());
  }

  @Override
  protected IDialogSettings getDialogSettings() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettings().getSection(
        DIALOG_SETTINGS);

    if (settings == null) {
      settings = DartToolsPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
    }

    return settings;
  }

  @SuppressWarnings("rawtypes")
  @Override
  protected Comparator getItemsComparator() {
    return fTypeItemsComparator;
  }

  @Override
  protected void restoreDialog(IDialogSettings settings) {
    super.restoreDialog(settings);

    if (!BUG_184693) {
      boolean showContainer = settings.getBoolean(SHOW_CONTAINER_FOR_DUPLICATES);
      fShowContainerForDuplicatesAction.setChecked(showContainer);
      fTypeInfoLabelProvider.setContainerInfo(showContainer);
    } else {
      fTypeInfoLabelProvider.setContainerInfo(true);
    }

    if (fAllowScopeSwitching) {
      String setting = settings.get(WORKINGS_SET_SETTINGS);
      if (setting != null) {
        try {
          IMemento memento = XMLMemento.createReadRoot(new StringReader(setting));
          fFilterActionGroup.restoreState(memento);
        } catch (WorkbenchException e) {
          // don't do anything. Simply don't restore the settings
          DartToolsPlugin.log(e);
        }
      }

      Object[] items = getSelectionHistory().getHistoryItems();
      for (Object o : items) {
        accessedHistoryItem(o);
      }

      //TODO: adding workingset scope support
//      IWorkingSet ws = fFilterActionGroup.getWorkingSet();
//      if (ws == null || (ws.isAggregateWorkingSet() && ws.isEmpty())) {
//      setSearchScope(SearchScopeFactory.createUniverseScope());
      setSubtitle(null);
//      } else {
//        setSearchScope(JavaSearchScopeFactory.getInstance().createJavaSearchScope(
//            ws, true));
//        setSubtitle(ws.getLabel());
//      }
    }

    // TypeNameMatch[] types = OpenTypeHistory.getInstance().getTypeInfos();
    //
    // for (int i = 0; i < types.length; i++) {
    // TypeNameMatch type = types[i];
    // accessedHistoryItem(type);
    // }
  }

  @Override
  protected void setResult(@SuppressWarnings("rawtypes") List newResult) {

    List<Element> resultToReturn = new ArrayList<Element>();

    for (Object result : newResult) {
      if (result instanceof TopLevelElement) {
        Element element = ((TopLevelElement) result).getElement();
        // items are added to history in the
        // org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#computeResult()
        // method
        resultToReturn.add(element);
      }
    }

    super.setResult(resultToReturn);
  }

  @Override
  protected void storeDialog(IDialogSettings settings) {
    super.storeDialog(settings);

    if (!BUG_184693) {
      settings.put(SHOW_CONTAINER_FOR_DUPLICATES, fShowContainerForDuplicatesAction.isChecked());
    }

    if (fFilterActionGroup != null) {
      XMLMemento memento = XMLMemento.createWriteRoot("workingSet"); //$NON-NLS-1$
      fFilterActionGroup.saveState(memento);
      fFilterActionGroup.dispose();
      StringWriter writer = new StringWriter();
      try {
        memento.save(writer);
        settings.put(WORKINGS_SET_SETTINGS, writer.getBuffer().toString());
      } catch (IOException e) {
        // don't do anything. Simply don't store the settings
        DartToolsPlugin.log(e);
      }
    }
  }

  @Override
  protected IStatus validateItem(Object item) {

    if (item == null) {
      return new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), IStatus.ERROR, "", null); //$NON-NLS-1$
    }

    if (fValidator != null) {
      Object[] elements = {item};
      return fValidator.validate(elements);
    } else {
      return new Status(IStatus.OK, DartToolsPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
    }
  }

  private Table findTableControl(Composite composite) {
    Control[] children = composite.getChildren();
    for (int i = 0; i < children.length; i++) {
      Control curr = children[i];
      if (curr instanceof Table) {
        return (Table) curr;
      } else if (curr instanceof Composite) {
        Table res = findTableControl((Composite) curr);
        if (res != null) {
          return res;
        }
      }
    }
    return null;
  }

  private void installOwnerDraw(Table tableControl) {
    new OwnerDrawSupport(tableControl) { // installs the owner draw listeners
      @Override
      public Color getColor(String foregroundColorName, Display display) {
        return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(
            foregroundColorName);
      }

      @Override
      public ColoredString getColoredLabel(Item item) {
        String text = item.getText();
        ColoredString str = new ColoredString(text);
        int index = text.indexOf('-');
        if (index != -1) {
          str.colorize(index, str.length() - index, ColoredDartElementLabels.QUALIFIER_STYLE);
        }
        return str;
      }
    };
  }

  /**
   * Adds or replaces subtitle of the dialog
   * 
   * @param text the new subtitle for this dialog
   */
  private void setSubtitle(String text) {
    if (text == null || text.length() == 0) {
      getShell().setText(fTitle);
    } else {
      getShell().setText(
          Messages.format(DartUIMessages.FilteredTypeSelectionDialog_titleFormat, new String[] {
              fTitle, text}));
    }
  }

}
