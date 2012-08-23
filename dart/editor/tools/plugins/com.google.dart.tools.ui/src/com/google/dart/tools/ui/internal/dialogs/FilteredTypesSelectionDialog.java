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
package com.google.dart.tools.ui.internal.dialogs;

import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchPatternFactory;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.dialogs.ITypeInfoFilterExtension;
import com.google.dart.tools.ui.dialogs.ITypeInfoImageProvider;
import com.google.dart.tools.ui.dialogs.ITypeSelectionComponent;
import com.google.dart.tools.ui.dialogs.TypeSelectionExtension;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.util.Strings;
import com.google.dart.tools.ui.internal.util.TypeNameMatchLabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.ColoredDartElementLabels;
import com.google.dart.tools.ui.internal.viewsupport.ColoredString;
import com.google.dart.tools.ui.internal.viewsupport.ColoredViewersManager;
import com.google.dart.tools.ui.internal.viewsupport.OwnerDrawSupport;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
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

/**
 * Shows a list of Dart types to the user with a text entry field for a string pattern used to
 * filter the list of types.
 */
public class FilteredTypesSelectionDialog extends FilteredItemsSelectionDialog implements
    ITypeSelectionComponent {

  //TODO(pquitslund): display search-in-progress status
  //TODO(pquitslund): improve/make incremental refresh

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

// TODO: enable history
//  /**
//   * Extends the <code>SelectionHistory</code>, providing support for
//   * <code>OpenTypeHistory</code>.
//   */
//  protected class TypeSelectionHistory extends SelectionHistory {
//
//    /**
//     * Creates new instance of TypeSelectionHistory
//     */
//
//    public TypeSelectionHistory() {
//      super();
//    }
//
//    /*
//     * (non-Javadoc)
//     * @see
//     * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#
//     * accessed(java.lang.Object)
//     */
//    public synchronized void accessed(Object object) {
//      super.accessed(object);
//    }
//
//    /*
//     * (non-Javadoc)
//     * @see
//     * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#
//     * load(org.eclipse.ui.IMemento)
//     */
//    public void load(IMemento memento) {
//      TypeNameMatch[] types = OpenTypeHistory.getInstance().getTypeInfos();
//
//      for (int i = 0; i < types.length; i++) {
//        TypeNameMatch type = types[i];
//        accessed(type);
//      }
//    }
//
//    /*
//     * (non-Javadoc)
//     * @see
//     * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#
//     * remove(java.lang.Object)
//     */
//    public synchronized boolean remove(Object element) {
//      OpenTypeHistory.getInstance().remove((TypeNameMatch) element);
//      return super.remove(element);
//    }
//
//    /*
//     * (non-Javadoc)
//     * @see
//     * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#
//     * save(org.eclipse.ui.IMemento)
//     */
//    public void save(IMemento memento) {
//      persistHistory();
//    }
//
//    protected Object restoreItemFromMemento(IMemento element) {
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * @see
//     * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#
//     * storeItemToMemento(java.lang.Object, org.eclipse.ui.IMemento)
//     */
//    protected void storeItemToMemento(Object item, IMemento element) {
//
//    }
//
//    /**
//     * Stores contents of the local history into persistent history container.
//     */
//    private synchronized void persistHistory() {
//      if (getReturnCode() == OK) {
//        Object[] items = getHistoryItems();
//        for (int i = 0; i < items.length; i++) {
//          OpenTypeHistory.getInstance().accessed((TypeNameMatch) items[i]);
//        }
//      }
//    }
//
//  }
//
//  /*
//   * We only have to ensure history consistency here since the search engine
//   * takes care of working copies.
//   */
//  private static class ConsistencyRunnable implements IRunnableWithProgress {
//    public static boolean needsExecution() {
//      OpenTypeHistory history = OpenTypeHistory.getInstance();
//      return fgFirstTime || history.isEmpty() || history.needConsistencyCheck();
//    }
//
//    public void run(IProgressMonitor monitor) throws InvocationTargetException,
//        InterruptedException {
//      if (fgFirstTime) {
//        // Join the initialize after load job.
//        IJobManager manager = Job.getJobManager();
//        manager.join(DartUI.ID_PLUGIN, monitor);
//      }
//      OpenTypeHistory history = OpenTypeHistory.getInstance();
//      if (fgFirstTime || history.isEmpty()) {
//        if (history.needConsistencyCheck()) {
//          monitor.beginTask(
//              DartUIMessages.TypeSelectionDialog_progress_consistency, 100);
//          refreshSearchIndices(new SubProgressMonitor(monitor, 90));
//          history.checkConsistency(new SubProgressMonitor(monitor, 10));
//        } else {
//          refreshSearchIndices(monitor);
//        }
//        monitor.done();
//        fgFirstTime = false;
//      } else {
//        history.checkConsistency(monitor);
//      }
//    }
//
//    private void refreshSearchIndices(IProgressMonitor monitor)
//        throws InvocationTargetException {
//      try {
//        new SearchEngine().searchAllTypeNames(
//            null,
//            0,
//            // make sure we search a concrete name. This is faster according to
//            // Kent
//            "_______________".toCharArray(), //$NON-NLS-1$
//            SearchPattern.RULE_EXACT_MATCH | SearchPattern.RULE_CASE_SENSITIVE,
//            IJavaScriptSearchConstants.ENUM,
//            SearchEngine.createWorkspaceScope(), new TypeNameRequestor() {
//            }, IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
//      } catch (DartModelException e) {
//        throw new InvocationTargetException(e);
//      }
//    }
//  }

  private static class TypeInfoUtil {

//    private final ITypeInfoImageProvider fProviderExtension;

//    private final SearchListener fAdapter = new SearchListener() {
//      @SuppressWarnings("unused")
//      private SearchMatch match;
//
//      @Override
//      public void matchFound(SearchMatch match) {
//        this.match = match;
//      }
//
//      @Override
//      public void searchComplete() {
//        // Ignored
//      }
//    };

//    private final Map fLib2Name = new HashMap();

//    private final String[] fInstallLocations;
//
//    private final String[] fVMNames;

//    private boolean fFullyQualifyDuplicates;

    public TypeInfoUtil(ITypeInfoImageProvider extension) {
//      fProviderExtension = extension;
//      List locations = new ArrayList();
//      List labels = new ArrayList();
//      IVMInstallType[] installs = JavaRuntime.getVMInstallTypes();
//      for (int i = 0; i < installs.length; i++) {
//        processVMInstallType(installs[i], locations, labels);
//      }
//      fInstallLocations = (String[]) locations.toArray(new String[locations.size()]);
//      fVMNames = (String[]) labels.toArray(new String[labels.size()]);

    }

    public String getFullyQualifiedText(CompilationUnitElement element) {
      StringBuffer result = new StringBuffer();
      result.append(element.getElementName());
//      String containerName = type.getTypeContainerName();
//      if (containerName.length() > 0) {
//        result.append(DartElementLabels.CONCAT_STRING);
//        result.append(containerName);
//      }
//      result.append(DartElementLabels.CONCAT_STRING);
//      result.append(getContainerName(type));
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

    public String getQualificationText(CompilationUnitElement element) {
      StringBuffer result = new StringBuffer();

      DartLibrary library = element.getCompilationUnit().getLibrary();
      result.append(library.getDisplayName());

      IResource resource = element.getResource();
      if (resource != null) {
        result.append(DartElementLabels.CONCAT_STRING);
        IContainer container = resource.getParent();
        String path = container.getFullPath().toString();
        if (path.startsWith("/")) { //$NON-NLS-1$
          result.append(path.substring(1));
        } else {
          result.append(path);
        }
      }
      return result.toString();
    }

    public String getQualifiedText(CompilationUnitElement element) {
      StringBuffer result = new StringBuffer();
      result.append(element.getElementName());

      DartLibrary library = element.getCompilationUnit().getLibrary();
      if (library != null) {
        result.append(DartElementLabels.CONCAT_STRING);
        result.append(library.getDisplayName());
      }
      return result.toString();
    }

    public String getText(Object element) {

      return ((DartElement) element).getElementName();
    }

//    public String getText(Type last, Type current,
//        Type next) {
//      StringBuffer result = new StringBuffer();
//      int qualifications = 0;
//      String currentTN = current.getSimpleTypeName();
//      result.append(currentTN);
//      String currentTCN = getTypeContainerName(current);
//      if (last != null) {
//        String lastTN = last.getSimpleTypeName();
//        String lastTCN = getTypeContainerName(last);
//        if (currentTCN.equals(lastTCN)) {
//          if (currentTN.equals(lastTN)) {
//            result.append(JavaScriptElementLabels.CONCAT_STRING);
//            result.append(currentTCN);
//            result.append(JavaScriptElementLabels.CONCAT_STRING);
//            result.append(getContainerName(current));
//            return result.toString();
//          }
//        } else if (currentTN.equals(lastTN)) {
//          qualifications = 1;
//        }
//      }
//      if (next != null) {
//        String nextTN = next.getSimpleTypeName();
//        String nextTCN = getTypeContainerName(next);
//        if (currentTCN.equals(nextTCN)) {
//          if (currentTN.equals(nextTN)) {
//            result.append(JavaScriptElementLabels.CONCAT_STRING);
//            result.append(currentTCN);
//            result.append(JavaScriptElementLabels.CONCAT_STRING);
//            result.append(getContainerName(current));
//            return result.toString();
//          }
//        } else if (currentTN.equals(nextTN)) {
//          qualifications = 1;
//        }
//      }
//      if (qualifications > 0) {
//        result.append(JavaScriptElementLabels.CONCAT_STRING);
//        result.append(currentTCN);
//        if (fFullyQualifyDuplicates) {
//          result.append(JavaScriptElementLabels.CONCAT_STRING);
//          result.append(getContainerName(current));
//        }
//      }
//      return result.toString();
//    }

//    public void setFullyQualifyDuplicates(boolean value) {
//      fFullyQualifyDuplicates = value;
//    }

//    private String getContainerName(TypeNameMatch type) {
//      IPackageFragmentRoot root = type.getPackageFragmentRoot();
//      if (root.isExternal()) {
//        String name = root.getPath().toOSString();
//        for (int i = 0; i < fInstallLocations.length; i++) {
//          if (name.startsWith(fInstallLocations[i])) {
//            return fVMNames[i];
//          }
//        }
//        String lib = (String) fLib2Name.get(name);
//        if (lib != null)
//          return lib;
//      }
//      StringBuffer buf = new StringBuffer();
//      JavaScriptElementLabels.getPackageFragmentRootLabel(root,
//          JavaScriptElementLabels.ROOT_QUALIFIED
//              | JavaScriptElementLabels.ROOT_VARIABLE, buf);
//      return buf.toString();
//    }

//    private String getFormattedLabel(String name) {
//      return Messages.format(DartUIMessages.FilteredTypesSelectionDialog_library_name_format, name);
//    }

//    private String getTypeContainerName(TypeNameMatch info) {
//      String result = info.getTypeContainerName();
//      if (result.length() > 0)
//        return result;
//      return DartUIMessages.FilteredTypesSelectionDialog_default_package;
//    }

//    private void processLibraryLocation(LibraryLocation[] libLocations,
//        String label) {
//      for (int l = 0; l < libLocations.length; l++) {
//        LibraryLocation location = libLocations[l];
//        fLib2Name.put(location.getSystemLibraryPath().toOSString(), label);
//      }
//    }

//    private void processVMInstallType(IVMInstallType installType,
//        List locations, List labels) {
//      if (installType != null) {
//        IVMInstall[] installs = installType.getVMInstalls();
//        boolean isMac = Platform.OS_MACOSX.equals(Platform.getOS());
//        final String HOME_SUFFIX = "/Home"; //$NON-NLS-1$
//        for (int i = 0; i < installs.length; i++) {
//          String label = getFormattedLabel(installs[i].getName());
//          LibraryLocation[] libLocations = installs[i].getLibraryLocations();
//          if (libLocations != null) {
//            processLibraryLocation(libLocations, label);
//          } else {
//            String filePath = installs[i].getInstallLocation().getAbsolutePath();
//            // on MacOS X install locations end in an additional
//            // "/Home" segment; remove it
//            if (isMac && filePath.endsWith(HOME_SUFFIX))
//              filePath = filePath.substring(0,
//                  filePath.length() - HOME_SUFFIX.length() + 1);
//            locations.add(filePath);
//            labels.add(label);
//          }
//        }
//      }
//    }
  }

//TODO: enable or remove type filter preferences
//  private class TypeFiltersPreferencesAction extends Action {
//
//    public TypeFiltersPreferencesAction() {
//      super(
//          DartUIMessages.FilteredTypesSelectionDialog_TypeFiltersPreferencesAction_label);
//    }
//
//    /*
//     * (non-Javadoc)
//     * @see org.eclipse.jface.action.Action#run()
//     */
//    public void run() {
//      String typeFilterID = TypeFilterPreferencePage.TYPE_FILTER_PREF_PAGE_ID;
//      PreferencesUtil.createPreferenceDialogOn(getShell(), typeFilterID,
//          new String[]{typeFilterID}, null).open();
//      triggerSearch();
//    }
//  }

  /**
   * A <code>LabelProvider</code> for the label showing type details.
   */
  private static class TypeItemDetailsLabelProvider extends LabelProvider {

    private final DartElementLabelProvider imageProvider = new DartElementLabelProvider();

    private final TypeInfoUtil fTypeInfoUtil;

    public TypeItemDetailsLabelProvider(TypeInfoUtil typeInfoUtil) {
      fTypeInfoUtil = typeInfoUtil;
    }

    @Override
    public Image getImage(Object object) {
      if (object instanceof CompilationUnitElement) {
        CompilationUnitElement element = (CompilationUnitElement) object;
        DartLibrary library = element.getCompilationUnit().getLibrary();
        if (library != null) {
          return imageProvider.getImage(library);
        }
      }
      return super.getImage(object);
    }

    @Override
    public String getText(Object element) {
      if (element instanceof CompilationUnitElement) {
        return fTypeInfoUtil.getQualificationText((CompilationUnitElement) element);
      }

      return super.getText(element);
    }
  }

  /**
   * A <code>LabelProvider</code> for (the table of) types.
   */
  private class TypeItemLabelProvider extends LabelProvider implements ILabelDecorator {

    private final TypeNameMatchLabelProvider fLabelProvider = new TypeNameMatchLabelProvider(
        TypeNameMatchLabelProvider.SHOW_TYPE_CONTAINER_ONLY
            + TypeNameMatchLabelProvider.SHOW_ROOT_POSTFIX);

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
      if (!(element instanceof CompilationUnitElement)) {
        return null;
      }

      if (fContainerInfo && isDuplicateElement(element)) {
        return fTypeInfoUtil.getFullyQualifiedText((CompilationUnitElement) element);
      }

      return fTypeInfoUtil.getQualifiedText((CompilationUnitElement) element);
    }

    @Override
    public Image getImage(Object element) {
      if (!(element instanceof CompilationUnitElement)) {
        return super.getImage(element);
      }
      return fLabelProvider.getImage(element);
    }

    @Override
    public String getText(Object element) {
      if (!(element instanceof CompilationUnitElement)) {
        return super.getText(element);
      }

      if (fContainerInfo && isDuplicateElement(element)) {
        return fTypeInfoUtil.getFullyQualifiedText((CompilationUnitElement) element);
      }

      if (!fContainerInfo && isDuplicateElement(element)) {
        return fTypeInfoUtil.getQualifiedText((CompilationUnitElement) element);
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

      DartElement leftInfo = (DartElement) left;
      DartElement rightInfo = (DartElement) right;
      return compareName(leftInfo.getElementName(), rightInfo.getElementName());

//      int result = compareName(leftInfo.getSimpleTypeName(),
//          rightInfo.getSimpleTypeName());
//      if (result != 0)
//        return result;
//      result = compareTypeContainerName(leftInfo.getTypeContainerName(),
//          rightInfo.getTypeContainerName());
//      if (result != 0)
//        return result;
//
//      int leftCategory = getElementTypeCategory(leftInfo);
//      int rightCategory = getElementTypeCategory(rightInfo);
//      if (leftCategory < rightCategory)
//        return -1;
//      if (leftCategory > rightCategory)
//        return +1;
//      return compareContainerName(leftInfo, rightInfo);
    }

//    private int compareContainerName(Type leftType,
//        Type rightType) {
//      return getContainerName(leftType).compareTo(getContainerName(rightType));
//    }

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

//    private int compareTypeContainerName(String leftString, String rightString) {
//      int leftLength = leftString.length();
//      int rightLength = rightString.length();
//      if (leftLength == 0 && rightLength > 0)
//        return -1;
//      if (leftLength == 0 && rightLength == 0)
//        return 0;
//      if (leftLength > 0 && rightLength == 0)
//        return +1;
//      return compareName(leftString, rightString);
//    }
//
//    private String getContainerName(TypeNameMatch type) {
//      IPackageFragmentRoot root = type.getPackageFragmentRoot();
//      if (root.isExternal()) {
//        String name = root.getPath().toOSString();
//        for (int i = 0; i < fInstallLocations.length; i++) {
//          if (name.startsWith(fInstallLocations[i])) {
//            return fVMNames[i];
//          }
//        }
//        String lib = (String) fLib2Name.get(name);
//        if (lib != null)
//          return lib;
//      }
//      StringBuffer buf = new StringBuffer();
//      JavaScriptElementLabels.getPackageFragmentRootLabel(root,
//          JavaScriptElementLabels.ROOT_QUALIFIED
//              | JavaScriptElementLabels.ROOT_VARIABLE, buf);
//      return buf.toString();
//    }

//    private int getElementTypeCategory(TypeNameMatch type) {
//      try {
//        if (type.getPackageFragmentRoot().getKind() == IPackageFragmentRoot.K_SOURCE)
//          return 0;
//      } catch (DartModelException e) {
//        DartToolsPlugin.log(e);
//      }
//      return 1;
//    }

//    private String getFormattedLabel(String name) {
//      return MessageFormat.format(
//          DartUIMessages.FilteredTypesSelectionDialog_library_name_format,
//          new Object[] {name});
//    }

//    private void processLibraryLocation(LibraryLocation[] libLocations,
//        String label) {
//      for (int l = 0; l < libLocations.length; l++) {
//        LibraryLocation location = libLocations[l];
//        fLib2Name.put(location.getSystemLibraryPath().toString(), label);
//      }
//    }
//
//    private void processVMInstallType(IVMInstallType installType,
//        List locations, List labels) {
//      if (installType != null) {
//        IVMInstall[] installs = installType.getVMInstalls();
//        boolean isMac = Platform.OS_MACOSX.equals(Platform.getOS());
//        final String HOME_SUFFIX = "/Home"; //$NON-NLS-1$
//        for (int i = 0; i < installs.length; i++) {
//          String label = getFormattedLabel(installs[i].getName());
//          LibraryLocation[] libLocations = installs[i].getLibraryLocations();
//          if (libLocations != null) {
//            processLibraryLocation(libLocations, label);
//          } else {
//            String filePath = installs[i].getInstallLocation().getAbsolutePath();
//            // on MacOS X install locations end in an additional
//            // "/Home" segment; remove it
//            if (isMac && filePath.endsWith(HOME_SUFFIX))
//              filePath = filePath.substring(0,
//                  filePath.length() - HOME_SUFFIX.length() + 1);
//            locations.add(filePath);
//            labels.add(label);
//          }
//        }
//      }
//    }
  }

  /**
   * Filters types using pattern, scope, element kind and filter extension.
   */
  @SuppressWarnings("unused")
  private class TypeItemsFilter extends ItemsFilter {

    private final SearchScope fScope;

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
    public TypeItemsFilter(SearchScope scope, int elementKind, ITypeInfoFilterExtension extension) {
      super(new TypeSearchPattern());
      fScope = scope;
      fIsWorkspaceScope = scope == null ? false
          : scope.equals(SearchScopeFactory.createWorkspaceScope());
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
      if (fScope != typeItemsFilter.getSearchScope()) {
        return false;
      }
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

    public SearchScope getSearchScope() {
      return fScope;
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
      if (fScope != typeItemsFilter.getSearchScope()) {
        return false;
      }
      if (fMyTypeFilterVersion != typeItemsFilter.getMyTypeFilterVersion()) {
        return false;
      }
      //TODO(pquitslund): this forces a full refresh which works-around filter application refresh issues
      return false;
    }

    public boolean matchesCachedResult(CompilationUnitElement element) {
      if (!(matchesFilterExtension(element))) {
        return false;
      }
      return matchesName(element);
    }

    public boolean matchesFilterExtension(DartElement element) {
      return true;
//TODO: consider whether we need filter extensions
//      if (fFilterExt == null)
//        return true;
//      fAdapter.setMatch(type);
//      return fFilterExt.select(fAdapter);
    }

    public boolean matchesHistoryElement(DartElement element) {
      if (!(matchesScope(element) && matchesFilterExtension(element))) {
        return false;
      }
      return matchesName(element);
    }

    public boolean matchesRawNamePattern(DartElement type) {
      return Strings.startsWithIgnoreCase(type.getElementName(), getPattern());
    }

    @Override
    public boolean matchesRawNamePattern(Object item) {
      CompilationUnitElement element = (CompilationUnitElement) item;
      return matchesRawNamePattern(element);
    }

    @Override
    public boolean matchItem(Object item) {

      if (fMatchEverything) {
        return true;
      }

      CompilationUnitElement element = (CompilationUnitElement) item;
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

    private boolean matchesName(DartElement element) {
      return matches(element.getElementName());
    }

    private boolean matchesScope(DartElement element) {
      if (fIsWorkspaceScope) {
        return true;
      }
      return fScope.encloses(element);
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
  @SuppressWarnings("unused")
  private class TypeSearchRequestor implements SearchListener {
    private volatile boolean fStop;

    private final AbstractContentProvider fContentProvider;

    private final TypeItemsFilter fTypeItemsFilter;

    public TypeSearchRequestor(AbstractContentProvider contentProvider,
        TypeItemsFilter typeItemsFilter) {
      super();
      fContentProvider = contentProvider;
      fTypeItemsFilter = typeItemsFilter;
    }

    public void cancel() {
      fStop = true;
    }

    @Override
    public void matchFound(SearchMatch match) {
      if (fStop) {
        return;
      }
      DartElement element = match.getElement();
      if (fTypeItemsFilter.matchesFilterExtension(element)) {
        fContentProvider.add(element, fTypeItemsFilter);
      }
      //TODO(pquitslund): this shouldn't be necessary (possibly remove once history is working)
      scheduleRefresh();
    }

    @Override
    public void searchComplete() {
      // Ignored
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

  private WorkingSetFilterActionGroup fFilterActionGroup;

  private final TypeItemLabelProvider fTypeInfoLabelProvider;

  private String fTitle;

  private ShowContainerForDuplicatesAction fShowContainerForDuplicatesAction;

  private SearchScope fSearchScope;

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
  public FilteredTypesSelectionDialog(Shell shell, boolean multi, IRunnableContext context,
      SearchScope scope, int elementKinds, TypeSelectionExtension extension) {
    super(shell, multi);

//TODO: implement selection history
//    setSelectionHistory(new TypeSelectionHistory());

    if (scope == null) {
      fAllowScopeSwitching = true;
      scope = SearchScopeFactory.createWorkspaceScope();
    }
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        shell,
        DartHelpContextIds.TYPE_SELECTION_DIALOG2);

    fElementKinds = elementKinds;
    fExtension = extension;
    fFilterExtension = (extension == null) ? null : extension.getFilterExtension();
    fSearchScope = scope;

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
    return new TypeItemsFilter(fSearchScope, fElementKinds, fFilterExtension);
  }

  @Override
  protected void fillContentProvider(final AbstractContentProvider provider,
      ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
    TypeItemsFilter typeSearchFilter = (TypeItemsFilter) itemsFilter;
    SearchListener requestor = new TypeSearchRequestor(provider, typeSearchFilter);

    SearchEngine engine = SearchEngineFactory.createSearchEngine((WorkingCopyOwner) null);
    progressMonitor.setTaskName(DartUIMessages.FilteredTypesSelectionDialog_searchJob_taskName);

    /*
     * Setting the filter into match everything mode avoids filtering twice by the same pattern (the
     * search engine only provides filtered matches). For the case when the pattern is a camel case
     * pattern with a terminator, the filter is not set to match everything mode because jdt.core's
     * SearchPattern does not support that case.
     */
    String typePattern = itemsFilter.getPattern();
    int matchRule = typeSearchFilter.getMatchRule();
    if (matchRule == SearchPattern.RULE_CAMELCASE_MATCH) {
      // If the pattern is empty, the RULE_BLANK_MATCH will be chosen, so we
      // don't have to check the pattern length
      char lastChar = typePattern.charAt(typePattern.length() - 1);

      if (lastChar == '<' || lastChar == ' ') {
        typePattern = typePattern.substring(0, typePattern.length() - 1);
      } else {
        typeSearchFilter.setMatchEverythingMode(true);
      }
    } else {
      typeSearchFilter.setMatchEverythingMode(true);
    }

    com.google.dart.tools.core.search.SearchPattern searchPattern = null;

    switch (matchRule) {
      case SearchPattern.RULE_CAMELCASE_MATCH:
        searchPattern = SearchPatternFactory.createCamelCasePattern(typePattern, false);
        break;
      case SearchPattern.RULE_PATTERN_MATCH:
        searchPattern = SearchPatternFactory.createWildcardPattern(typePattern, false);
        break;
      case SearchPattern.RULE_PREFIX_MATCH:
        searchPattern = SearchPatternFactory.createPrefixPattern(typePattern, false);
        break;
      default:
        searchPattern = SearchPatternFactory.createExactPattern(typePattern, false);
        break;
    }

    try {
      engine.searchTypeDeclarations(
          typeSearchFilter.getSearchScope(),
          searchPattern,
          null,
          requestor,
          progressMonitor);
    } catch (SearchException e) {
      DartToolsPlugin.log(e);
    } finally {
      typeSearchFilter.setMatchEverythingMode(false);
    }
  }

  @Override
  protected void fillViewMenu(IMenuManager menuManager) {
    super.fillViewMenu(menuManager);

    if (!BUG_184693) {
      fShowContainerForDuplicatesAction = new ShowContainerForDuplicatesAction();
      menuManager.add(fShowContainerForDuplicatesAction);
    }
//TODO: add workingset search scope support                
//    if (fAllowScopeSwitching) {
//      fFilterActionGroup = new WorkingSetFilterActionGroup(getShell(), 
//          new IPropertyChangeListener() {
//            public void propertyChange(PropertyChangeEvent event) {
//              IWorkingSet ws = (IWorkingSet) event.getNewValue();
//              if (ws == null || (ws.isAggregateWorkingSet() && ws.isEmpty())) {
//                setSearchScope(SearchScopeFactory.createWorkspaceScope());
//                setSubtitle(null);
//              } else {
//                setSearchScope(JavaSearchScopeFactory.getInstance().createJavaSearchScope(
//                    ws, true));
//                setSubtitle(ws.getLabel());
//              }
//
//              applyFilter();
//            }
//          });
//      fFilterActionGroup.fillViewMenu(menuManager);
//    }

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
      //TODO: adding workingset scope support
//      IWorkingSet ws = fFilterActionGroup.getWorkingSet();
//      if (ws == null || (ws.isAggregateWorkingSet() && ws.isEmpty())) {
      setSearchScope(SearchScopeFactory.createWorkspaceScope());
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

    List<CompilationUnitElement> resultToReturn = new ArrayList<CompilationUnitElement>();

    for (int i = 0; i < newResult.size(); i++) {
      if (newResult.get(i) instanceof CompilationUnitElement) {
        CompilationUnitElement element = (CompilationUnitElement) newResult.get(i);
        if (element.exists()) {
          // items are added to history in the
          // org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#computeResult()
          // method
          resultToReturn.add(element);
        } else {
          String containerName = ""; //$NON-NLS-1$
//          IPackageFragmentRoot root = typeInfo.getPackageFragmentRoot();
//          String containerName = JavaScriptElementLabels.getElementLabel(root,
//              JavaScriptElementLabels.ROOT_QUALIFIED);
          String message = Messages.format(
              DartUIMessages.FilteredTypesSelectionDialog_dialogMessage,
              new String[] {element.getElementName(), containerName});
          MessageDialog.openError(getShell(), fTitle, message);
          getSelectionHistory().remove(element);
        }
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
      CompilationUnitElement element = (CompilationUnitElement) item;
      if (!element.exists()) {
        return new Status(
            IStatus.ERROR,
            DartToolsPlugin.getPluginId(),
            IStatus.ERROR,
            Messages.format(
                DartUIMessages.FilteredTypesSelectionDialog_error_type_doesnot_exist,
                ((CompilationUnitElement) item).getElementName()), null);
      }
      Object[] elements = {element};
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
   * Sets search scope used when searching for types.
   * 
   * @param scope the new scope
   */
  private void setSearchScope(SearchScope scope) {
    fSearchScope = scope;
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
