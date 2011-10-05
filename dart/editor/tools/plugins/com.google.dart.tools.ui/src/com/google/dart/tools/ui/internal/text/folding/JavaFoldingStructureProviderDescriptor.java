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
package com.google.dart.tools.ui.internal.text.folding;

import com.google.dart.tools.ui.text.folding.IDartFoldingPreferenceBlock;
import com.google.dart.tools.ui.text.folding.IDartFoldingStructureProvider;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Describes a contribution to the folding provider extension point.
 */
public final class JavaFoldingStructureProviderDescriptor {

  /* extension point attribute names */

  private static final String PREFERENCES_CLASS = "preferencesClass"; //$NON-NLS-1$
  private static final String CLASS = "class"; //$NON-NLS-1$
  private static final String NAME = "name"; //$NON-NLS-1$
  private static final String ID = "id"; //$NON-NLS-1$

  /** The identifier of the extension. */
  private String fId;
  /** The name of the extension. */
  private String fName;
  /** The class name of the provided <code>IJavaFoldingStructureProvider</code>. */
  private String fClass;
  /**
   * <code>true</code> if the extension specifies a custom <code>IJavaFoldingPreferenceBlock</code>.
   */
  private boolean fHasPreferences;
  /** The configuration element of this extension. */
  private IConfigurationElement fElement;

  /**
   * Creates a new descriptor.
   * 
   * @param element the configuration element to read
   */
  JavaFoldingStructureProviderDescriptor(IConfigurationElement element) {
    fElement = element;
    fId = element.getAttribute(ID);
    Assert.isLegal(fId != null);

    fName = element.getAttribute(NAME);
    if (fName == null) {
      fName = fId;
    }

    fClass = element.getAttribute(CLASS);
    Assert.isLegal(fClass != null);

    if (element.getAttribute(PREFERENCES_CLASS) == null) {
      fHasPreferences = false;
    } else {
      fHasPreferences = true;
    }
  }

  /**
   * Creates a preferences object as described in the extension's xml.
   * 
   * @return a new instance of the reference provider described by this descriptor
   * @throws CoreException if creation fails
   */
  public IDartFoldingPreferenceBlock createPreferences() throws CoreException {
    if (fHasPreferences) {
      IDartFoldingPreferenceBlock prefs = (IDartFoldingPreferenceBlock) fElement.createExecutableExtension(PREFERENCES_CLASS);
      return prefs;
    } else {
      return new EmptyJavaFoldingPreferenceBlock();
    }
  }

  /**
   * Creates a folding provider as described in the extension's xml.
   * 
   * @return a new instance of the folding provider described by this descriptor
   * @throws CoreException if creation fails
   */
  public IDartFoldingStructureProvider createProvider() throws CoreException {
    IDartFoldingStructureProvider prov = (IDartFoldingStructureProvider) fElement.createExecutableExtension(CLASS);
    return prov;
  }

  /**
   * Returns the identifier of the described extension.
   * 
   * @return Returns the id
   */
  public String getId() {
    return fId;
  }

  /**
   * Returns the name of the described extension.
   * 
   * @return Returns the name
   */
  public String getName() {
    return fName;
  }
}
