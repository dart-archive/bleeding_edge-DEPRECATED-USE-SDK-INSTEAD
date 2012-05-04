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
package com.google.dart.tools.core.indexer;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperational;
import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperationalUnchecked;
import com.google.dart.indexer.index.layers.bidirectional_edges.BidirectionalEdgesLayerForwardEdgesQuery;
import com.google.dart.indexer.index.layers.bidirectional_edges.BidirectionalEdgesLayerReverseEdgesQuery;
import com.google.dart.indexer.index.layers.reverse_edges.ReverseEdgesQuery;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.standard.StandardDriver;
import com.google.dart.indexer.workspace.driver.WorkspaceIndexingDriver;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.indexer.DartLayers;
import com.google.dart.tools.core.internal.indexer.location.DartElementLocation;
import com.google.dart.tools.core.internal.indexer.location.FieldLocation;
import com.google.dart.tools.core.internal.indexer.location.FunctionLocation;
import com.google.dart.tools.core.internal.indexer.location.FunctionTypeAliasLocation;
import com.google.dart.tools.core.internal.indexer.location.MethodLocation;
import com.google.dart.tools.core.internal.indexer.location.SyntheticLocation;
import com.google.dart.tools.core.internal.indexer.location.TypeLocation;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * The class <code>DartIndexer</code> defines methods used to cross-reference {@link DartElement
 * Dart elements}.
 */
public class DartIndexer {
  /**
   * The interface <code>IndexerSearch</code> defines the behavior of objects that can perform a
   * search.
   */
  private interface IndexerSearch {
    /**
     * Perform a search, returning the results of the search.
     * 
     * @return the results of the search
     * @throws IndexTemporarilyNonOperational if the search could not be performed because the index
     *           cannot be built
     */
    public DartIndexerResult performSearch() throws IndexTemporarilyNonOperational;
  }

  /**
   * Ideas for additional queries:
   * <ul>
   * <li>all extensions of a given type</li>
   * </ul>
   */

  /**
   * An array containing locations representing all of the classes in bundled libraries.
   */
  private static Location[] bundledClassLocations;

  /**
   * An array containing locations representing all of the interfaces in bundled libraries.
   */
  private static Location[] bundledInterfaceLocations;

  /**
   * An array containing locations representing all of the function type aliases in bundled
   * libraries.
   */
  private static Location[] bundledFunctionTypeLocations;

  /**
   * Return the result of searching for all of the classes that have been defined.
   * 
   * @return all of the classes that have been defined
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @post $result != null
   */
  public static DartIndexerResult getAllClasses() throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery query = new ReverseEdgesQuery(
          SyntheticLocation.ALL_CLASSES,
          driver.getConfiguration().getLayer(DartLayers.ELEMENTS_BY_CATEGORY));
      driver.execute(query);
      return new DartIndexerResult(
          merge(query.getSources(), getBundledClassLocations()),
          getFilesWithErrors());
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding all classes");
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding all classes");
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the interfaces that have been defined.
   * 
   * @return all of the interfaces that have been defined
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @post $result != null
   */
  public static DartIndexerResult getAllInterfaces() throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery query = new ReverseEdgesQuery(
          SyntheticLocation.ALL_INTERFACES,
          driver.getConfiguration().getLayer(DartLayers.ELEMENTS_BY_CATEGORY));
      driver.execute(query);
      return new DartIndexerResult(
          merge(query.getSources(), getBundledInterfaceLocations()),
          getFilesWithErrors());
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding all interfaces");
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding all interfaces");
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the types that have been defined.
   * 
   * @return all of the types that have been defined
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @post $result != null
   */
  public static DartIndexerResult getAllTypes() throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery classesQuery = new ReverseEdgesQuery(
          SyntheticLocation.ALL_CLASSES,
          driver.getConfiguration().getLayer(DartLayers.ELEMENTS_BY_CATEGORY));
      driver.execute(classesQuery);
      ReverseEdgesQuery interfacesQuery = new ReverseEdgesQuery(
          SyntheticLocation.ALL_INTERFACES,
          driver.getConfiguration().getLayer(DartLayers.ELEMENTS_BY_CATEGORY));
      driver.execute(interfacesQuery);
      ReverseEdgesQuery functionTypesQuery = new ReverseEdgesQuery(
          SyntheticLocation.ALL_FUNCTION_TYPE_ALIASES,
          driver.getConfiguration().getLayer(DartLayers.ELEMENTS_BY_CATEGORY));
      driver.execute(functionTypesQuery);
      return new DartIndexerResult(merge(
          classesQuery.getSources(),
          interfacesQuery.getSources(),
          functionTypesQuery.getSources(),
          getBundledClassLocations(),
          getBundledInterfaceLocations(),
          getBundledFunctionTypeLocations()), getFilesWithErrors());
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding all types");
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding all types");
      throw exception.unwrap();
    }
  }

  /**
   * Return an array containing the paths to files which might be indexed with errors.
   * 
   * @return an array containing the paths to files which might be indexed with errors
   */
  public static IPath[] getFilesWithErrors() {
    WorkspaceIndexingDriver driver = StandardDriver.getInstance();
    return driver.getFilesWithErrors();
  }

  /**
   * Return the result of searching for all methods that directly override the given method.
   * <p>
   * For example, given the following source:
   * 
   * <pre>
   *    class A {
   *       int getValue() {
   *          ...
   *       }
   *    }
   *    class B extends A {
   *       int getValue() {
   *          ...
   *       }
   *    }
   *    class C extends B {
   *       int getValue() {
   *          ...
   *       }
   *    }
   * </pre>
   * If the {@link Method} representing the method <code>getValue</code> defined in the class
   * <code>A</code> were passed into this method, a location representing the method
   * <code>getValue</code> in the class <code>B</code> would both be returned, but the method
   * <code>getValue</code> in the class <code>C</code> would <b>not</b> be returned.
   * 
   * @param method the method being overridden by the results
   * @return all of the methods that directly override the given method
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @post $result != null
   */
  public static DartIndexerResult getOverridingMethods(Method method)
      throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery query = new ReverseEdgesQuery(new MethodLocation(
          method,
          method.getNameRange()), driver.getConfiguration().getLayer(DartLayers.METHOD_OVERRIDE));
      driver.execute(query);
      return new DartIndexerResult(query.getSources(), getFilesWithErrors());
    } catch (DartModelException exception) {
      throw new IndexTemporarilyNonOperational(exception);
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding overriding methods");
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding overriding methods");
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the references to the given function. The result will
   * include any methods, fields, or functions that invoke the target function.
   * <p>
   * For example, given the following source:
   * 
   * <pre>
   *    class A() {
   *       int secondsPerMinute = 60;
   *       int secondsPerHour = secondsPerMinute * 60;
   *       int getSecondsPerMinute() {
   *          return secondsPerMinute;
   *       }
   *    }
   * </pre>
   * If the {@link DartFunction} representing the function <code></code> were passed into this
   * method, locations representing ... would be returned.
   * 
   * @param function the function being referenced by the results
   * @return all of the references to the given function
   * @throws IndexTemporarilyNonOperational
   * @pre function != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(DartFunction function)
      throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery query = new ReverseEdgesQuery(new FunctionLocation(
          function,
          function.getNameRange()), driver.getConfiguration().getLayer(DartLayers.METHOD_CALLS));
      driver.execute(query);
      return new DartIndexerResult(query.getSources(), getFilesWithErrors());
    } catch (DartModelException exception) {
      throw new IndexTemporarilyNonOperational(exception);
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding invocations of the function "
              + function.getElementName());
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding invocations of the function "
              + function.getElementName());
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the references to the given function. This method is
   * equivalent to {@link #getReferences(DartFunction)} except that it will block until either
   * <ul>
   * <li>the search has been completed,</li>
   * <li>the search fails because the index cannot be built, or</li>
   * <li>the progress monitor indicates that the search should be canceled</li>
   * </ul>
   * 
   * @param function the function being referenced by the results
   * @param monitor the progress monitor used to determine whether to stop waiting for results
   * @return the result of searching for all of the invocations of the given function
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @throws InterruptedException if the progress monitor indicates that the operation should be
   *           canceled
   * @pre function != null
   * @pre monitor != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(final DartFunction function,
      IProgressMonitor monitor) throws IndexTemporarilyNonOperational, InterruptedException {
    return performSearch(monitor, new IndexerSearch() {
      @Override
      public DartIndexerResult performSearch() throws IndexTemporarilyNonOperational {
        return getReferences(function);
      }
    });
  }

  /**
   * Return the result of searching for all of the references to the given function type alias. The
   * result will include any methods, fields, or functions that reference the target function type
   * alias.
   * <p>
   * For example, given the following source:
   * 
   * <pre>
   *    interface int MapInt(int value);
   *
   *    class A() {
   *       MapInt mapFunction;
   *
   *       int mapAll(int[] values) {
   *          int[] mappedValues = new int
   *          return mappedValues;
   *       }
   *    }
   * </pre>
   * If the {@link DartFunctionTypeAlias} representing the function type alias <code></code> were
   * passed into this method, locations representing ... would be returned.
   * 
   * @param alias the function type alias being referenced by the results
   * @return all of the references to the given function
   * @throws IndexTemporarilyNonOperational
   * @pre alias != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(DartFunctionTypeAlias alias)
      throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery query = new ReverseEdgesQuery(new FunctionTypeAliasLocation(
          alias,
          alias.getNameRange()), driver.getConfiguration().getLayer(DartLayers.TYPE_REFERENCES));
      driver.execute(query);
      return new DartIndexerResult(query.getSources(), getFilesWithErrors());
    } catch (DartModelException exception) {
      throw new IndexTemporarilyNonOperational(exception);
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding references to the function type alias "
              + alias.getElementName());
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding references to the function type alias "
              + alias.getElementName());
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the references to the given function type alias. This
   * method is equivalent to {@link #getReferences(DartFunctionTypeAlias)} except that it will block
   * until either
   * <ul>
   * <li>the search has been completed,</li>
   * <li>the search fails because the index cannot be built, or</li>
   * <li>the progress monitor indicates that the search should be canceled</li>
   * </ul>
   * 
   * @param alias the function type alias being referenced by the results
   * @param monitor the progress monitor used to determine whether to stop waiting for results
   * @return the result of searching for all of the invocations of the given function
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @throws InterruptedException if the progress monitor indicates that the operation should be
   *           canceled
   * @pre alias != null
   * @pre monitor != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(final DartFunctionTypeAlias alias,
      IProgressMonitor monitor) throws IndexTemporarilyNonOperational, InterruptedException {
    return performSearch(monitor, new IndexerSearch() {
      @Override
      public DartIndexerResult performSearch() throws IndexTemporarilyNonOperational {
        return getReferences(alias);
      }
    });
  }

  /**
   * Return the result of searching for all of the references to the given field. The result will
   * include any methods, fields, or functions that reference the target field, either to access or
   * modify its value.
   * <p>
   * For example, given the following source:
   * 
   * <pre>
   *    class A() {
   *       int secondsPerMinute = 60;
   *       int secondsPerHour = secondsPerMinute * 60;
   *       int getSecondsPerMinute() {
   *          return secondsPerMinute;
   *       }
   *    }
   * </pre>
   * If the {@link Field} representing the field <code>secondsPerMinute</code> were passed into this
   * method, locations representing the field <code>secondsPerHour</code> and the method
   * <code>getSecondsPerMinute</code> would both be returned.
   * 
   * @param field the field being referenced by the results
   * @return all of the references to the given field
   * @throws IndexTemporarilyNonOperational
   * @pre field != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(Field field) throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery query = new ReverseEdgesQuery(
          new FieldLocation(field, field.getNameRange()),
          driver.getConfiguration().getLayer(DartLayers.FIELD_ACCESSES));
      driver.execute(query);
      return new DartIndexerResult(query.getSources(), getFilesWithErrors());
    } catch (DartModelException exception) {
      throw new IndexTemporarilyNonOperational(exception);
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding references to the field " + field.getElementName()
              + " in " + field.getDeclaringType().getElementName());
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding references to the field " + field.getElementName()
              + " in " + field.getDeclaringType().getElementName());
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the references to the given field. This method is
   * equivalent to {@link #getReferences(Field)} except that it will block until either
   * <ul>
   * <li>the search has been completed,</li>
   * <li>the search fails because the index cannot be built, or</li>
   * <li>the progress monitor indicates that the search should be canceled</li>
   * </ul>
   * 
   * @param field the field being referenced by the results
   * @param monitor the progress monitor used to determine whether to stop waiting for results
   * @return the result of searching for all of the references to the given field
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @throws InterruptedException if the progress monitor indicates that the operation should be
   *           canceled
   * @pre field != null
   * @pre monitor != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(final Field field, IProgressMonitor monitor)
      throws IndexTemporarilyNonOperational, InterruptedException {
    return performSearch(monitor, new IndexerSearch() {
      @Override
      public DartIndexerResult performSearch() throws IndexTemporarilyNonOperational {
        return getReferences(field);
      }
    });
  }

  /**
   * Return the result of searching for all of the references to the given method. The result will
   * include any methods, fields, or functions that invoke the target method.
   * <p>
   * For example, given the following source:
   * 
   * <pre>
   *    class A() {
   *       someBoolean = methodB(&quot;&quot;);
   *       void methodA() {
   *          methodB(&quot;string&quot;);
   *       }
   *       boolean methodB(String string) {
   *          ...
   *       }
   *    }
   * </pre>
   * If the {@link Method} representing the method <code>methodB</code> were passed into this
   * method, locations representing the field <code>someBoolean</code> and the method
   * <code>methodA</code> would both be returned.
   * 
   * @param method the method being referenced by the results
   * @return all of the references to the given method
   * @throws IndexTemporarilyNonOperational
   * @pre method != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(Method method)
      throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery query = new ReverseEdgesQuery(new MethodLocation(
          method,
          method.getNameRange()), driver.getConfiguration().getLayer(DartLayers.METHOD_CALLS));
      driver.execute(query);
      return new DartIndexerResult(query.getSources(), getFilesWithErrors());
    } catch (DartModelException exception) {
      throw new IndexTemporarilyNonOperational(exception);
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding references to the method " + method.getElementName()
              + " in " + method.getDeclaringType().getElementName());
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding references to the method " + method.getElementName()
              + " in " + method.getDeclaringType().getElementName());
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the references to the given method. This method is
   * equivalent to {@link #getReferences(Method)} except that it will block until either
   * <ul>
   * <li>the search has been completed,</li>
   * <li>the search fails because the index cannot be built, or</li>
   * <li>the progress monitor indicates that the search should be canceled</li>
   * </ul>
   * 
   * @param method the method being referenced by the results
   * @param monitor the progress monitor used to determine whether to stop waiting for results
   * @return the result of searching for all of the references to the given method
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @throws InterruptedException if the progress monitor indicates that the operation should be
   *           canceled
   * @pre method != null
   * @pre monitor != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(final Method method, IProgressMonitor monitor)
      throws IndexTemporarilyNonOperational, InterruptedException {
    return performSearch(monitor, new IndexerSearch() {
      @Override
      public DartIndexerResult performSearch() throws IndexTemporarilyNonOperational {
        return getReferences(method);
      }
    });
  }

  /**
   * Return the result of searching for all of the references to the given type. The result will
   * include any methods, fields, or functions that reference the target type.
   * <p>
   * For example, given the following source:
   * 
   * <pre>
   *    class A() {
   *       B someField;
   *       B methodA() {
   *          ...
   *       }
   *       boolean methodB(B arg) {
   *          ...
   *       }
   *       boolean methodC() {
   *          B temp;
   *          ...
   *       }
   *    }
   * </pre>
   * If the {@link Type} representing the type <code>B</code> were passed into this method,
   * locations representing the field <code>someField</code> and the methods <code>methodA</code>,
   * <code>methodB</code>, and <code>methodC</code> would all be returned.
   * 
   * @param type the type being referenced by the results
   * @return all of the references to the given type
   * @throws IndexTemporarilyNonOperational
   * @pre type != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(Type type) throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      ReverseEdgesQuery query = new ReverseEdgesQuery(
          new TypeLocation(type, type.getNameRange()),
          driver.getConfiguration().getLayer(DartLayers.TYPE_REFERENCES));
      driver.execute(query);
      return new DartIndexerResult(query.getSources(), getFilesWithErrors());
    } catch (DartModelException exception) {
      throw new IndexTemporarilyNonOperational(exception);
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding references to the type " + type.getElementName());
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding references to the type " + type.getElementName());
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the references to the given type. This method is
   * equivalent to {@link #getReferences(Type)} except that it will block until either
   * <ul>
   * <li>the search has been completed,</li>
   * <li>the search fails because the index cannot be built, or</li>
   * <li>the progress monitor indicates that the search should be canceled</li>
   * </ul>
   * 
   * @param type the type being referenced by the results
   * @param monitor the progress monitor used to determine whether to stop waiting for results
   * @return the result of searching for all of the references to the given type
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @throws InterruptedException if the progress monitor indicates that the operation should be
   *           canceled
   * @pre type != null
   * @pre monitor != null
   * @post $result != null
   */
  public static DartIndexerResult getReferences(final Type type, IProgressMonitor monitor)
      throws IndexTemporarilyNonOperational, InterruptedException {
    return performSearch(monitor, new IndexerSearch() {
      @Override
      public DartIndexerResult performSearch() throws IndexTemporarilyNonOperational {
        return getReferences(type);
      }
    });
  }

  /**
   * Return the result of searching for all of the direct subtypes of the given type. Only types can
   * be subclasses of other types.
   * <p>
   * For example, given the following source:
   * 
   * <pre>
   *    class A {}
   *    class B extends A {}
   *    class C extends B {}
   *    class D extends A {}
   * </pre>
   * If the {@link Type} representing the type <code>A</code> were passed into this method,
   * locations representing the types <code>B</code> and <code>D</code> would be returned.
   * 
   * @param type the type whose subtypes are to be returned
   * @return all of the subtypes of the given type
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @pre type != null
   * @post $result != null
   */
  public static DartIndexerResult getSubtypes(Type type) throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      BidirectionalEdgesLayerReverseEdgesQuery query = new BidirectionalEdgesLayerReverseEdgesQuery(
          new TypeLocation(type, type.getNameRange()),
          driver.getConfiguration().getLayer(DartLayers.TYPE_HIERARCHY));
      driver.execute(query);
      return new DartIndexerResult(query.getSources(), getFilesWithErrors());
    } catch (DartModelException exception) {
      throw new IndexTemporarilyNonOperational(exception);
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding subclasses of the type " + type.getElementName());
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding subclasses of the type " + type.getElementName());
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the direct subtypes of the given type. This method is
   * equivalent to {@link #getSubclasses(Type)} except that it will block until either
   * <ul>
   * <li>the search has been completed,</li>
   * <li>the search fails because the index cannot be built, or</li>
   * <li>the progress monitor indicates that the search should be canceled</li>
   * </ul>
   * 
   * @param type the type whose subtypes are to be returned
   * @param monitor the progress monitor used to determine whether to stop waiting for results
   * @return the result of searching for all of the subtypes of the given type
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @throws InterruptedException if the progress monitor indicates that the operation should be
   *           canceled
   * @pre type != null
   * @pre monitor != null
   * @post $result != null
   */
  public static DartIndexerResult getSubtypes(final Type type, IProgressMonitor monitor)
      throws IndexTemporarilyNonOperational, InterruptedException {
    return performSearch(monitor, new IndexerSearch() {
      @Override
      public DartIndexerResult performSearch() throws IndexTemporarilyNonOperational {
        return getSubtypes(type);
      }
    });
  }

  /**
   * Return the result of searching for all of the direct supertypes of the given type. Only types
   * can be supertypes of other types.
   * <p>
   * For example, given the following source:
   * 
   * <pre>
   *    interface I {}
   *    class A {}
   *    class B extends A implements I {}
   *    class C extends B {}
   * </pre>
   * If the {@link Type} representing the type <code>B</code> were passed into this method,
   * locations representing the types <code>A</code> and <code>I</code> would be returned.
   * 
   * @param type the type whose supertypes are to be returned
   * @return all of the supertypes of the given type
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @pre type != null
   * @post $result != null
   */
  public static DartIndexerResult getSupertypes(Type type) throws IndexTemporarilyNonOperational {
    try {
      WorkspaceIndexingDriver driver = StandardDriver.getInstance();
      BidirectionalEdgesLayerForwardEdgesQuery query = new BidirectionalEdgesLayerForwardEdgesQuery(
          new TypeLocation(type, type.getNameRange()),
          driver.getConfiguration().getLayer(DartLayers.TYPE_HIERARCHY));
      driver.execute(query);
      return new DartIndexerResult(query.getDestinations(), getFilesWithErrors());
    } catch (DartModelException exception) {
      throw new IndexTemporarilyNonOperational(exception);
    } catch (IndexTemporarilyNonOperational exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding superclasses of the type " + type.getElementName());
      throw exception;
    } catch (IndexTemporarilyNonOperationalUnchecked exception) {
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INDEX_NOT_AVAILABLE,
          exception,
          "Index not available while finding superclasses of the type " + type.getElementName());
      throw exception.unwrap();
    }
  }

  /**
   * Return the result of searching for all of the direct supertypes of the given type. This method
   * is equivalent to {@link #getSuperclasses(Type)} except that it will block until either
   * <ul>
   * <li>the search has been completed,</li>
   * <li>the search fails because the index cannot be built, or</li>
   * <li>the progress monitor indicates that the search should be canceled</li>
   * </ul>
   * 
   * @param type the type whose supertypes are to be returned
   * @param monitor the progress monitor used to determine whether to stop waiting for results
   * @return the result of searching for all of the supertypes of the given type
   * @throws IndexTemporarilyNonOperational if the query cannot be completed because the index could
   *           not be built
   * @throws InterruptedException if the progress monitor indicates that the operation should be
   *           canceled
   * @pre type != null
   * @pre monitor != null
   * @post $result != null
   */
  public static DartIndexerResult getSupertypes(final Type type, IProgressMonitor monitor)
      throws IndexTemporarilyNonOperational, InterruptedException {
    return performSearch(monitor, new IndexerSearch() {
      @Override
      public DartIndexerResult performSearch() throws IndexTemporarilyNonOperational {
        return getSupertypes(type);
      }
    });
  }

  /**
   * Stop the indexer.
   */
  public static void shutdown() {
    StandardDriver.shutdown();
  }

  /**
   * Given an arbitrary location, determine whether it refers to a Dart element, and if so return
   * the element, otherwise return <code>null</code>.
   * 
   * @param location the location being converted into an element
   * @return the Dart element referred to by the given location
   */
  public static DartElement unpackElementOrNull(Location location) {
    if (location instanceof DartElementLocation) {
      DartElement candidate = ((DartElementLocation) location).getDartElement();
      if (candidate.exists()) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * This method computes the index type cache; no work is performed if the cache is already built.
   */
  public static void warmUpIndexer() {
    try {
      getAllTypes();
    } catch (IndexTemporarilyNonOperational ex) {
      DartCore.logError(ex);
    }
  }

  /**
   * Return an array containing locations representing all of the classes in bundled libraries.
   * 
   * @return an array containing locations representing all of the classes in bundled libraries
   */
  private static Location[] getBundledClassLocations() {
    if (bundledClassLocations == null) {
      initializeBundledLocations();
    }
    return bundledClassLocations;
  }

  /**
   * Return an array containing locations representing all of the function type aliases in bundled
   * libraries.
   * 
   * @return an array containing locations representing all of the function type aliases in bundled
   *         libraries
   */
  private static Location[] getBundledFunctionTypeLocations() {
    if (bundledFunctionTypeLocations == null) {
      initializeBundledLocations();
    }
    return bundledFunctionTypeLocations;
  }

  /**
   * Return an array containing locations representing all of the interfaces in bundled libraries.
   * 
   * @return an array containing locations representing all of the interfaces in bundled libraries
   */
  private static Location[] getBundledInterfaceLocations() {
    if (bundledInterfaceLocations == null) {
      initializeBundledLocations();
    }
    return bundledInterfaceLocations;
  }

  private static void initializeBundledLocations() {
    List<Location> classLocations = new ArrayList<Location>();
    List<Location> interfaceLocations = new ArrayList<Location>();
    List<Location> functionTypeLocations = new ArrayList<Location>();
    try {
      for (DartLibrary library : DartCore.create(ResourcesPlugin.getWorkspace().getRoot()).getBundledLibraries()) {
        try {
          for (CompilationUnit compilationUnit : library.getCompilationUnits()) {
            try {
              for (Type type : compilationUnit.getTypes()) {
                Location location = new TypeLocation(type, type.getNameRange());
                if (type.isInterface()) {
                  interfaceLocations.add(location);
                } else {
                  classLocations.add(location);
                }
              }
              for (DartFunctionTypeAlias alias : compilationUnit.getFunctionTypeAliases()) {
                Location location = new FunctionTypeAliasLocation(alias, alias.getNameRange());
                functionTypeLocations.add(location);
              }
            } catch (DartModelException exception) {
              DartCore.logError(
                  "Cannot access elements in compilation unit " + compilationUnit.getElementName(),
                  exception);
            }
          }
        } catch (DartModelException exception) {
          DartCore.logError(
              "Cannot access compilation units in bundled library " + library.getElementName(),
              exception);
        }
      }
    } catch (DartModelException exception) {
      DartCore.logError("Cannot access DartModel", exception);
    }
    bundledClassLocations = classLocations.toArray(new Location[classLocations.size()]);
    bundledInterfaceLocations = interfaceLocations.toArray(new Location[interfaceLocations.size()]);
    bundledFunctionTypeLocations = functionTypeLocations.toArray(new Location[functionTypeLocations.size()]);
  }

  /**
   * Return the result of merging two or more arrays of locations into a single array.
   * 
   * @param firstLocations the first array of locations
   * @param allOtherLocations the other arrays of locations
   * @return the result of merging two or more arrays of locations into a single array
   */
  private static Location[] merge(Location[] firstLocations, Location[]... allOtherLocations) {
    int totalCount = firstLocations.length;
    for (Location[] otherLocations : allOtherLocations) {
      totalCount = totalCount + otherLocations.length;
    }
    Location[] result = new Location[totalCount];
    int copiedCount = firstLocations.length;
    System.arraycopy(firstLocations, 0, result, 0, copiedCount);
    for (Location[] otherLocations : allOtherLocations) {
      int count = otherLocations.length;
      System.arraycopy(otherLocations, 0, result, copiedCount, count);
      copiedCount = copiedCount + count;
    }
    return result;
  }

  /**
   * Return the result of performing the given search. This method will block until either
   * <ul>
   * <li>the search has been completed,</li>
   * <li>the search fails because the index cannot be built, or</li>
   * <li>the progress monitor indicates that the search should be canceled</li>
   * </ul>
   * Note that the search is not actually canceled if the progress monitor is canceled, this method
   * simply stops waiting for the search. The search will continue to run on a separate thread.
   * 
   * @param monitor the progress monitor used to determine whether to stop waiting for results
   * @param search the type being referenced
   * @return the result of performing the given search
   * @throws IndexTemporarilyNonOperational if the search cannot be completed because the index
   *           could not be built
   * @throws InterruptedException if the progress monitor indicates that the operation should be
   *           canceled
   * @pre monitor != null
   * @pre search != null
   * @post $result != null
   */
  private static DartIndexerResult performSearch(IProgressMonitor monitor,
      final IndexerSearch search) throws IndexTemporarilyNonOperational, InterruptedException {
    final DartIndexerResult[] result = {null};
    final IndexTemporarilyNonOperational[] thrownException = {null};

    Thread queryThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          result[0] = search.performSearch();
        } catch (IndexTemporarilyNonOperational exception) {
          thrownException[0] = exception;
        }
      }
    });
    queryThread.start();
    while (true) {
      if (result[0] != null) {
        return result[0];
      } else if (thrownException[0] != null) {
        throw thrownException[0];
      } else if (monitor.isCanceled()) {
        throw new InterruptedException();
      }
      Thread.sleep(500);
    }
  }
}
