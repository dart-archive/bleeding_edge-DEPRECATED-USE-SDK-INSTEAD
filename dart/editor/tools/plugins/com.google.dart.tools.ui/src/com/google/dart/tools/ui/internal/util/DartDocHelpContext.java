/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContext2;
import org.eclipse.help.IHelpResource;
import org.eclipse.ui.PlatformUI;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("restriction")
public class DartDocHelpContext implements IContext2 {

  public static void displayHelp(String contextId, Object[] selected) throws CoreException {
    IContext context = HelpSystem.getContext(contextId);
    if (context != null) {
      if (selected != null && selected.length > 0) {
        context = new DartDocHelpContext(context, selected);
      }
      PlatformUI.getWorkbench().getHelpSystem().displayHelp(context);
    }
  }

  /**
   * Gets the reader content as a String
   */
  private static String getString(Reader reader) {
    StringBuffer buf = new StringBuffer();
    char[] buffer = new char[1024];
    int count;
    try {
      while ((count = reader.read(buffer)) != -1) {
        buf.append(buffer, 0, count);
      }
    } catch (IOException e) {
      return null;
    }
    return buf.toString();
  }

  private final IHelpResource[] fHelpResources;
  private String fText;

  private String fTitle;

  // see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=85719
  private static final boolean BUG_85719_FIXED = false;

  public DartDocHelpContext(IContext context, Object[] elements) throws DartModelException {
    Assert.isNotNull(elements);
    if (context instanceof IContext2) {
      fTitle = ((IContext2) context).getTitle();
    }

    List<IHelpResource> helpResources = new ArrayList<IHelpResource>();

    String javadocSummary = null;

    // Add static help topics
    if (context != null) {
      IHelpResource[] resources = context.getRelatedTopics();
      if (resources != null) {
        for (int j = 0; j < resources.length; j++) {
          helpResources.add(resources[j]);
        }
      }
    }

    fHelpResources = helpResources.toArray(new IHelpResource[helpResources.size()]);

    if (context != null) {
      fText = context.getText();
    }

    if (BUG_85719_FIXED) {
      if (javadocSummary != null && javadocSummary.length() > 0) {
        if (fText != null) {
          fText = context.getText() + "<br><br>" + javadocSummary; //$NON-NLS-1$
        } else {
          fText = javadocSummary;
        }
      }
    }

    if (fText == null) {
      fText = ""; //$NON-NLS-1$
    }

  }

  @Override
  public String getCategory(IHelpResource topic) {
//    if (topic instanceof JavaUIHelpResource) {
//      return DartUIMessages.JavaUIHelpContext_javaHelpCategory_label;
//    }

    return null;
  }

  @Override
  public IHelpResource[] getRelatedTopics() {
    return fHelpResources;
  }

  @Override
  public String getStyledText() {
    return fText;
  }

  @Override
  public String getText() {
    return fText;
  }

  /*
   * @see org.eclipse.help.IContext2#getTitle()
   */
  @Override
  public String getTitle() {
    return fTitle;
  }

}
