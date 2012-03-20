package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.tools.core.model.DartElement;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;

/**
 * Rename type arguments describe the data that a rename type processor provides to its rename type
 * participants.
 * <p>
 * This class is not intended to be subclassed by clients.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RenameTypeArguments extends RenameArguments {

  private boolean updateSimilarDeclarations;
  private DartElement[] similarDeclarations;

  /**
   * Creates new rename type arguments.
   * 
   * @param newName the new name of the element to be renamed
   * @param updateReferences <code>true</code> if reference updating is requested;
   *          <code>false</code> otherwise
   * @param updateSimilarDeclarations <code>true</code> if similar declaration updating is
   *          requested; <code>false</code> otherwise
   * @param similarDeclarations the similar declarations that will be updated or <code>null</code>
   *          if similar declaration updating is not requested
   */
  public RenameTypeArguments(String newName,
      boolean updateReferences,
      boolean updateSimilarDeclarations,
      DartElement[] similarDeclarations) {
    super(newName, updateReferences);
    if (updateSimilarDeclarations) {
      Assert.isNotNull(similarDeclarations);
    }
    this.updateSimilarDeclarations = updateSimilarDeclarations;
    this.similarDeclarations = similarDeclarations;
  }

  /**
   * Returns the similar declarations that get updated. Returns <code>null</code> if similar
   * declaration updating is not requested.
   * 
   * @return the similar elements that get updated
   */
  public DartElement[] getSimilarDeclarations() {
    return similarDeclarations;
  }

  /**
   * Returns whether similar declaration updating is requested or not.
   * 
   * @return returns <code>true</code> if similar declaration updating is requested;
   *         <code>false</code> otherwise
   */
  public boolean getUpdateSimilarDeclarations() {
    return updateSimilarDeclarations;
  }

  @Override
  public String toString() {
    return super.toString()
        + (updateSimilarDeclarations
            ? " (update derived elements)" : " (don't update derived elements)"); //$NON-NLS-1$//$NON-NLS-2$
  }
}