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
package com.google.dart.tools.ui.test.model;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.dart.tools.ui.internal.intro.SampleDescription;
import com.google.dart.tools.ui.internal.intro.SampleDescriptionHelper;
import com.google.dart.tools.ui.internal.intro.SampleHelper;
import com.google.dart.tools.ui.test.model.internal.workbench.ProjectImpl;
import com.google.dart.tools.ui.test.runnable.Result;
import com.google.dart.tools.ui.test.runnable.VoidResult;

import static com.google.dart.tools.ui.test.runnable.UIThreadRunnable.syncExec;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.eclipse.finder.finders.WorkbenchContentsFinder;

import static org.junit.Assert.assertNotNull;

import java.util.List;

/**
 * Test model of the DartEditor workspace.
 */
@SuppressWarnings("restriction")
public class Workspace {

  /**
   * Represents a top-level folder which corresponds with an underlying dart {@link IProject}.
   */
  public static interface Project {

    /**
     * Deletes this project from the workspace.
     * 
     * @throws CoreException if delete fails.
     */
    void delete() throws CoreException;

    /**
     * Returns the name of this project.
     * 
     * @return the project name
     */
    String getName();

  };

  /**
   * Represents a bundled editor sample.
   */
  public static enum Sample {

    CLOCK("Clock"),
    SLIDER("Slider"),
    SOLAR("Solar"),
    SOLAR_3D("Solar 3D"),
    SWIPE("Swipe"),
    SUNFLOWER("Sunflower"),
    TIME("Time server"),
    TODO_MVC("TodoMVC");

    private static SampleDescription getDescription(String name) {
      for (SampleDescription desc : SampleDescriptionHelper.getDescriptions()) {
        if (desc.name.equals(name)) {
          return desc;
        }
      }
      return null;
    }

    private static void openSample(final SampleDescription sample) {
      assertNotNull(sample);
      syncExec(new VoidResult() {
        @Override
        public void run() {
          SampleHelper.openSample(sample, new WorkbenchContentsFinder().activeWorkbenchWindow());
        }
      });
    }

    private final SampleDescription description;

    private Sample(String name) {
      this.description = getDescription(name);
    }

    public void dispose() throws CoreException {

      CoreException exception = syncExec(new Result<CoreException>() {

        @Override
        public CoreException run() {
          try {
            Project project = getProject();
            project.delete();
            return null;
          } catch (CoreException e) {
            return e;
          }
        }

      });

      if (exception != null) {
        throw exception;
      }
    }

    public SampleDescription getDescription() {
      return description;
    }

    public Project getProject() {

      List<Project> projects = findProjects(new Predicate<IProject>() {
        @Override
        public boolean apply(IProject project) {
          //TODO (pquitslund): consider getting a handle on the created project from the SampleHelper 
          //(in a future) so we can do this more cleanly
          return project.getName().startsWith(description.directory.getName());
        }
      });

      if (!projects.isEmpty()) {
        return projects.get(0);
      }

      throw new IllegalStateException("no project found for : " + toString());
    }

    /**
     * Open this sample.
     */
    public void open() {
      openSample(description);
    }

  }

  /**
   * Find the project with the given name.
   * 
   * @param name the project name
   * @return the project or <code>null</code> if it was not found
   */
  public static Project findProject(final String name) {

    List<Project> projects = findProjects(new Predicate<IProject>() {
      @Override
      public boolean apply(IProject project) {
        return project.getName().equals(name);
      }
    });

    if (!projects.isEmpty()) {
      return projects.get(0);
    }
    return null;
  }

  /**
   * Returns the collection of projects which exist in the workspace.
   * 
   * @return a list of projects
   */
  public static List<Project> getProjects() {
    return findProjects(new Predicate<IProject>() {
      @Override
      public boolean apply(IProject input) {
        return true;
      }
    });
  }

  private static List<Project> findProjects(Predicate<IProject> matcher) {

    List<Project> projects = Lists.newArrayList();

    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (matcher.apply(project)) {
        projects.add(new ProjectImpl(project));
      }
    }

    return projects;
  }

}
