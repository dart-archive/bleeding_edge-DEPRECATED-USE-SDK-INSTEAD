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
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.omni.OmniBoxMessages;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.dialogs.SearchPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provider for files.
 */
public class FileProvider extends OmniProposalProvider {

  private class FileCollector implements IResourceProxyVisitor {

    private final IProgressMonitor progressMonitor;

    private final List<OmniElement> matches = new ArrayList<OmniElement>();

    private final List<IResource> projects;

    private final boolean showDerived = false;

    private final int filterTypeMask = IResource.FILE;

    public FileCollector(IProgressMonitor progressMonitor) throws CoreException {

      this.progressMonitor = progressMonitor;
      IResource[] resources = container.members();
      this.projects = new ArrayList<IResource>(Arrays.asList(resources));

      if (progressMonitor != null) {
        progressMonitor.beginTask(OmniBoxMessages.TextSearch_taskName, projects.size());
      }
    }

    public OmniElement[] getFiles() {
      return matches.toArray(EMPTY_ARRAY);
    }

    /**
     * @param item Must be instance of IFile, otherwise <code>false</code> will be returned.
     * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#matchItem(java.lang.Object)
     */
    public boolean matchItem(Object item) {
      if (!(item instanceof IFile)) {
        return false;
      }
      IFile resource = (IFile) item;
      if ((!this.showDerived && resource.isDerived())
          || ((this.filterTypeMask & resource.getType()) == 0)) {
        return false;
      }

      String name = resource.getName();

      //exclude .project && .children files
      //TODO (pquitslund): consider centralizing this filter when core search is integrated
      if (name.equals(".project") || name.equals(".children")) { //$NON-NLS-1$ //$NON-NLS-2$
        IPath path = resource.getFullPath();
        // e.g., /MyProject/.project
        if (path.segmentCount() == 2) {
          return false;
        }
      }

      if (nameMatches(name)) {
        if (containerPattern != null) {
          // match full container path:
          String containerPath = resource.getParent().getFullPath().toString();
          if (containerPattern.matches(containerPath)) {
            return true;
          }
          // match path relative to current selection:
          if (relativeContainerPattern != null) {
            return relativeContainerPattern.matches(containerPath);
          }
          return false;
        }
        return true;
      }

      return false;
    }

    @Override
    public boolean visit(IResourceProxy proxy) {

      if (progressMonitor.isCanceled()) {
        return false;
      }

      IResource resource = proxy.requestResource();

      if (this.projects.remove((resource.getProject())) || this.projects.remove((resource))) {
        progressMonitor.worked(1);
      }

      if (matchItem(resource)) {
        matches.add(new FileElement(FileProvider.this, (IFile) resource));
      }

      if (resource.getType() == IResource.FOLDER && !shouldTraverseFolder(resource)) {
        return false;
      }

      if (resource.getType() == IResource.FILE) {
        return false;
      }

      return true;
    }

    /**
     * Matches text with filter.
     * 
     * @param text the text to match with the filter
     * @return <code>true</code> if text matches with filter pattern, <code>false</code> otherwise
     */
    protected boolean matches(String text) {
      return patternMatcher.matches(text);
    }

    private boolean nameMatches(String name) {
      if (namePattern != null) {
        // fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=212565
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1 && namePattern.matches(name.substring(0, lastDot))
            && extensionPattern.matches(name.substring(lastDot + 1))) {
          return true;
        }
      }
      return matches(name);
    }

    private boolean shouldTraverseFolder(IResource resource) {
      if (resource.isDerived()) {
        return false;
      }

      String name = resource.getName();
      return name != null && !name.startsWith(".");
    }
  }

  /**
   * The base outer-container which will be used to search for resources. This is the root of the
   * tree that spans the search space. Often, this is the workspace root.
   */
  private final IContainer container;

  private final SearchPattern patternMatcher;

  /**
   * Container path pattern. Is <code>null</code> when only a file name pattern is used.
   */
  private SearchPattern containerPattern;

  /**
   * Container path pattern, relative to the current searchContainer. Is <code>null</code> if
   * there's no search container.
   */
  private SearchPattern relativeContainerPattern;

  /**
   * Camel case pattern for the name part of the file name (without extension). Is <code>null</code>
   * if there's no extension.
   */
  private SearchPattern namePattern;

  /**
   * Camel case pattern for the file extension. Is <code>null</code> if there's no extension.
   */
  private SearchPattern extensionPattern;

  private final IProgressMonitor progressMonitor;

  private static OmniElement[] EMPTY_ARRAY = new OmniElement[0];

  public FileProvider(IProgressMonitor progressMonitor) {
    this.progressMonitor = progressMonitor;
    this.container = ResourcesPlugin.getWorkspace().getRoot();
    this.patternMatcher = new SearchPattern();
  }

  @Override
  public OmniElement getElementForId(String id) {
    //TODO (pquitslund): this singelton match concept needs a rethink
    OmniElement[] elements = getElements(id);
    if (elements.length == 0) {
      return null;
    }
    return elements[0];
  }

  @Override
  public OmniElement[] getElements(String stringPattern) {

    String filenamePattern;

    int sep = stringPattern.lastIndexOf(IPath.SEPARATOR);
    if (sep != -1) {
      filenamePattern = stringPattern.substring(sep + 1, stringPattern.length());
      if ("*".equals(filenamePattern)) {
        filenamePattern = "**"; //$NON-NLS-1$
      }

      if (sep > 0) {
        if (filenamePattern.length() == 0) {
          filenamePattern = "**"; //$NON-NLS-1$
        }

        String containerPattern = stringPattern.substring(0, sep);

        if (container != null) {
          relativeContainerPattern = new SearchPattern(SearchPattern.RULE_EXACT_MATCH
              | SearchPattern.RULE_PATTERN_MATCH);
          relativeContainerPattern.setPattern(container.getFullPath().append(containerPattern).toString());
        }

        if (!containerPattern.startsWith("" + IPath.SEPARATOR)) {
          containerPattern = IPath.SEPARATOR + containerPattern;
        }
        this.containerPattern = new SearchPattern(SearchPattern.RULE_EXACT_MATCH
            | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_PATTERN_MATCH);
        this.containerPattern.setPattern(containerPattern);
      }
      patternMatcher.setPattern(filenamePattern);

    } else {
      filenamePattern = stringPattern;
      patternMatcher.setPattern(stringPattern);
    }

    int lastPatternDot = filenamePattern.lastIndexOf('.');
    if (lastPatternDot != -1) {
      char last = filenamePattern.charAt(filenamePattern.length() - 1);
      if (last != ' ' && last != '<' && getMatchRule() != SearchPattern.RULE_EXACT_MATCH) {
        namePattern = new SearchPattern();
        namePattern.setPattern(filenamePattern.substring(0, lastPatternDot));
        extensionPattern = new SearchPattern();
        extensionPattern.setPattern(filenamePattern.substring(lastPatternDot + 1));
      }
    }

    try {
      FileCollector collector = new FileCollector(progressMonitor);
      container.accept(collector, IResource.NONE);
      return collector.getFiles();
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
    return EMPTY_ARRAY;

  }

  @Override
  public String getId() {
    return "com.google.dart.tools.ui.files"; //$NON-NLS-1$
  }

  @Override
  public String getName() {
    return OmniBoxMessages.OmniBox_Files;
  }

  /**
   * Returns the rule to apply for matching keys.
   * 
   * @return an implementation-specific match rule
   * @see SearchPattern#getMatchRule() for match rules returned by the default implementation
   */
  private int getMatchRule() {
    return patternMatcher.getMatchRule();
  }

}
