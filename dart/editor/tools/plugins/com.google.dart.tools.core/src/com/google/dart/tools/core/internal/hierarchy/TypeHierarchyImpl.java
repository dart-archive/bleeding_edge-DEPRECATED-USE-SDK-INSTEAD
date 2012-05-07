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
package com.google.dart.tools.core.internal.hierarchy;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.RegionImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.ElementChangedListener;
import com.google.dart.tools.core.model.OpenableElement;
import com.google.dart.tools.core.model.Region;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeHierarchy;
import com.google.dart.tools.core.model.TypeHierarchyChangedListener;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>TypeHierarchyImpl</code> implement a type hierarchy.
 */
public class TypeHierarchyImpl implements ElementChangedListener, TypeHierarchy {
  public static boolean DEBUG = false;

  static final byte VERSION = 0x0000;
  // SEPARATOR
  static final byte SEPARATOR1 = '\n';
  static final byte SEPARATOR2 = ',';
  static final byte SEPARATOR3 = '>';
  static final byte SEPARATOR4 = '\r';
  // general info
  static final byte COMPUTE_SUBTYPES = 0x0001;

  // type info
  static final byte CLASS = 0x0000;
  static final byte INTERFACE = 0x0001;
  static final byte COMPUTED_FOR = 0x0002;
  static final byte ROOT = 0x0004;

  // cst
  static final byte[] NO_FLAGS = new byte[] {};
  static final int SIZE = 10;

  public static TypeHierarchy load(Type type, InputStream input, WorkingCopyOwner owner)
      throws DartModelException {
    try {
      TypeHierarchyImpl typeHierarchy = new TypeHierarchyImpl();
      typeHierarchy.initialize(1);

      Type[] types = new Type[SIZE];
      int typeCount = 0;

      byte version = (byte) input.read();

      if (version != VERSION) {
        throw new DartModelException(new DartModelStatusImpl(IStatus.ERROR));
      }
      byte generalInfo = (byte) input.read();
      if ((generalInfo & COMPUTE_SUBTYPES) != 0) {
        typeHierarchy.computeSubtypes = true;
      }

      byte b;
      byte[] bytes;

      // read project
      bytes = readUntil(input, SEPARATOR1);
      if (bytes.length > 0) {
        typeHierarchy.project = (DartProject) DartCore.create(new String(bytes));
//        typeHierarchy.scope = SearchScopeFactory.createDartSearchScope(new DartElement[] {typeHierarchy.project});
      } else {
        typeHierarchy.project = null;
//        typeHierarchy.scope = SearchScopeFactory.createWorkspaceScope();
      }

      // read missing type
      {
        bytes = readUntil(input, SEPARATOR1);
        byte[] missing;
        int j = 0;
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
          b = bytes[i];
          if (b == SEPARATOR2) {
            missing = new byte[i - j];
            System.arraycopy(bytes, j, missing, 0, i - j);
            typeHierarchy.missingTypes.add(new String(missing));
            j = i + 1;
          }
        }
        System.arraycopy(bytes, j, missing = new byte[length - j], 0, length - j);
        typeHierarchy.missingTypes.add(new String(missing));
      }

      // read types
      while ((b = (byte) input.read()) != SEPARATOR1 && b != -1) {
        bytes = readUntil(input, SEPARATOR4, 1);
        bytes[0] = b;
        Type element = (Type) DartCore.create(new String(bytes), owner);

        if (types.length == typeCount) {
          System.arraycopy(types, 0, types = new Type[typeCount * 2], 0, typeCount);
        }
        types[typeCount++] = element;

        //      // read flags
        //      bytes = readUntil(input, SEPARATOR4);
        //      Integer flags = bytesToFlags(bytes);
        //      if(flags != null) {
        //        typeHierarchy.cacheFlags(element, flags.intValue());
        //      }

        // read info
        byte info = (byte) input.read();

        if ((info & INTERFACE) != 0) {
          typeHierarchy.addInterface(element);
        }
        if ((info & COMPUTED_FOR) != 0) {
          if (!element.equals(type)) {
            throw new DartModelException(new DartModelStatusImpl(IStatus.ERROR));
          }
          typeHierarchy.focusType = element;
        }
        if ((info & ROOT) != 0) {
          typeHierarchy.addRootClass(element);
        }
      }

      // read super class
      while ((b = (byte) input.read()) != SEPARATOR1 && b != -1) {
        bytes = readUntil(input, SEPARATOR3, 1);
        bytes[0] = b;
        int subClass = new Integer(new String(bytes)).intValue();

        // read super type
        bytes = readUntil(input, SEPARATOR1);
        int superClass = new Integer(new String(bytes)).intValue();

        typeHierarchy.cacheSuperclass(types[subClass], types[superClass]);
      }

      // read super interface
      while ((b = (byte) input.read()) != SEPARATOR1 && b != -1) {
        bytes = readUntil(input, SEPARATOR3, 1);
        bytes[0] = b;
        int subClass = new Integer(new String(bytes)).intValue();

        // read super interface
        bytes = readUntil(input, SEPARATOR1);
        Type[] superInterfaces = new Type[(bytes.length / 2) + 1];
        int interfaceCount = 0;

        int j = 0;
        byte[] b2;
        for (int i = 0; i < bytes.length; i++) {
          if (bytes[i] == SEPARATOR2) {
            b2 = new byte[i - j];
            System.arraycopy(bytes, j, b2, 0, i - j);
            j = i + 1;
            superInterfaces[interfaceCount++] = types[new Integer(new String(b2)).intValue()];
          }
        }
        b2 = new byte[bytes.length - j];
        System.arraycopy(bytes, j, b2, 0, bytes.length - j);
        superInterfaces[interfaceCount++] = types[new Integer(new String(b2)).intValue()];
        System.arraycopy(
            superInterfaces,
            0,
            superInterfaces = new Type[interfaceCount],
            0,
            interfaceCount);

        typeHierarchy.cacheSuperInterfaces(types[subClass], superInterfaces);
      }
      if (b == -1) {
        throw new DartModelException(new DartModelStatusImpl(IStatus.ERROR));
      }
      return typeHierarchy;
    } catch (IOException e) {
      throw new DartModelException(e, DartModelStatusConstants.IO_EXCEPTION);
    }
  }

  protected static byte[] readUntil(InputStream input, byte separator) throws DartModelException,
      IOException {
    return readUntil(input, separator, 0);
  }

  protected static byte[] readUntil(InputStream input, byte separator, int offset)
      throws IOException, DartModelException {
    int length = 0;
    byte[] bytes = new byte[SIZE];
    byte b;
    while ((b = (byte) input.read()) != separator && b != -1) {
      if (bytes.length == length) {
        System.arraycopy(bytes, 0, bytes = new byte[length * 2], 0, length);
      }
      bytes[length++] = b;
    }
    if (b == -1) {
      throw new DartModelException(new DartModelStatusImpl(IStatus.ERROR));
    }
    System.arraycopy(bytes, 0, bytes = new byte[length + offset], offset, length);
    return bytes;
  }

//  private static Integer bytesToFlags(byte[] bytes) {
//    if (bytes != null && bytes.length > 0) {
//      return new Integer(new String(bytes));
//    } else {
//      return null;
//    }
//  }

//  private static byte[] flagsToBytes(Integer flags) {
//    if (flags != null) {
//      return flags.toString().getBytes();
//    } else {
//      return NO_FLAGS;
//    }
//  }

  /**
   * The Dart Project in which the hierarchy is being built - this provides the context for
   * determining a classpath and name lookup rules. Possibly null.
   */
  protected DartProject project;
  /**
   * The type the hierarchy was specifically computed for, possibly null.
   */
  protected Type focusType;
  /*
   * The working copies that take precedence over original compilation units
   */
  protected CompilationUnit[] workingCopies;

  protected Map<Type, Type> classToSuperclass;

  protected Map<Type, Type[]> typeToSuperInterfaces;

  protected Map<Type, ArrayList<Type>> typeToSubtypes;

  protected ArrayList<Type> rootClasses = new ArrayList<Type>();

  protected ArrayList<Type> interfaces = new ArrayList<Type>(10);

  public ArrayList<String> missingTypes = new ArrayList<String>(4);

  protected static final Type[] NO_TYPE = new Type[0];

  /**
   * The progress monitor to report work completed too.
   */
  protected IProgressMonitor progressMonitor = null;

  /**
   * Change listeners - null if no one is listening.
   */
  protected ArrayList<TypeHierarchyChangedListener> changeListeners = null;

  /*
   * A map from Openables to ArrayLists of Types
   */
  public Map<OpenableElement, ArrayList<Type>> files = null;

  /**
   * A region describing the packages considered by this hierarchy. Null if not activated.
   */
  protected Region packageRegion = null;

  /**
   * A region describing the projects considered by this hierarchy. Null if not activated.
   */
  protected Region projectRegion = null;

  /**
   * Whether this hierarchy should contains subtypes.
   */
  protected boolean computeSubtypes;

//  /**
//   * The scope this hierarchy should restrain itself in.
//   */
//  SearchScope scope;

  /*
   * Whether this hierarchy needs refresh
   */
  public boolean needsRefresh = true;

  /*
   * Collects changes to types
   */
  protected ChangeCollector changeCollector;

  /**
   * Initialize a newly created type hierarchy to be empty.
   */
  public TypeHierarchyImpl() {
    super();
  }

  /**
   * Initialize a newly created type hierarchy on the given type.
   */
  public TypeHierarchyImpl(Type focusType, CompilationUnit[] workingCopies,
  /* SearchScope scope, */boolean computeSubtypes) {
//    this.focusType = focusType == null ? null
//        : (Type) ((DartElementImpl) focusType).unresolved(); // unsure the focus type is unresolved (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=92357)
    this.focusType = focusType;
    this.workingCopies = workingCopies;
    this.computeSubtypes = computeSubtypes;
//    this.scope = scope;
  }

  /**
   * Initialize a newly created type hierarchy on the given type.
   */
  public TypeHierarchyImpl(Type focusType, CompilationUnit[] workingCopies, DartProject project,
      boolean computeSubtypes) {
    this(focusType, workingCopies,
    /* SearchScopeFactory.createDartSearchScope(new DartElement[] {project}), */
    computeSubtypes);
    this.project = project;
  }

  @Override
  public synchronized void addTypeHierarchyChangedListener(TypeHierarchyChangedListener listener) {
    if (changeListeners == null) {
      changeListeners = new ArrayList<TypeHierarchyChangedListener>();
    }

    // register with DartCore to get Dart element delta on first listener added
    if (changeListeners.size() == 0) {
      DartCore.addElementChangedListener(this);
    }

    // add listener only if it is not already present
    if (!changeListeners.contains(listener)) {
      changeListeners.add(listener);
    }
  }

  @Override
  public boolean contains(Type type) {
    // classes
    if (classToSuperclass.get(type) != null) {
      return true;
    }

    // root classes
    if (rootClasses.contains(type)) {
      return true;
    }

    // interfaces
    if (interfaces.contains(type)) {
      return true;
    }

    return false;
  }

  /**
   * Determine whether the change affects this hierarchy, and fire change notification if required.
   */
  @Override
  public void elementChanged(ElementChangedEvent event) {
    // type hierarchy change has already been fired
    if (needsRefresh) {
      return;
    }

    if (isAffected(event.getDelta(), event.getType())) {
      needsRefresh = true;
      fireChange();
    }
  }

  @Override
  public boolean exists() {
    if (!needsRefresh) {
      return true;
    }
    return (focusType == null || focusType.exists()) && javaProject().exists();
  }

  /**
   * Notifies listeners that this hierarchy has changed and needs refreshing. Note that listeners
   * can be removed as we iterate through the list.
   */
  public void fireChange() {
    ArrayList<TypeHierarchyChangedListener> listeners = getClonedChangeListeners(); // clone so that a listener cannot have a side-effect on this list when being notified
    if (listeners == null) {
      return;
    }
    if (DEBUG) {
      System.out.println("FIRING hierarchy change [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
      if (focusType != null) {
        System.out.println("    for hierarchy focused on " + ((DartElementImpl) focusType).toStringWithAncestors()); //$NON-NLS-1$
      }
    }

    for (int i = 0; i < listeners.size(); i++) {
      final TypeHierarchyChangedListener listener = listeners.get(i);
      SafeRunner.run(new ISafeRunnable() {
        @Override
        public void handleException(Throwable exception) {
          DartCore.logError(
              "Exception occurred in listener of Type hierarchy change notification", exception); //$NON-NLS-1$
        }

        @Override
        public void run() throws Exception {
          listener.typeHierarchyChanged(TypeHierarchyImpl.this);
        }
      });
    }
  }

  @Override
  public Type[] getAllClasses() {
    ArrayList<Type> classes = new ArrayList<Type>(rootClasses);
    for (Iterator<Type> iter = classToSuperclass.keySet().iterator(); iter.hasNext();) {
      classes.add(iter.next());
    }
    return classes.toArray(new Type[classes.size()]);
  }

  @Override
  public Type[] getAllInterfaces() {
    return interfaces.toArray(new Type[interfaces.size()]);
  }

  @Override
  public Type[] getAllSubtypes(Type type) {
    return getAllSubtypesForType(type);
  }

  @Override
  public Type[] getAllSuperclasses(Type type) {
    Type superclass = getSuperclass(type);
    ArrayList<Type> supers = new ArrayList<Type>();
    while (superclass != null) {
      supers.add(superclass);
      superclass = getSuperclass(superclass);
    }
    return supers.toArray(new Type[supers.size()]);
  }

  @Override
  public Type[] getAllSuperInterfaces(Type type) {
    ArrayList<Type> supers = getAllSuperInterfaces0(type, null);
    if (supers == null) {
      return NO_TYPE;
    }
    return supers.toArray(new Type[supers.size()]);
  }

  @Override
  public Type[] getAllSupertypes(Type type) {
    ArrayList<Type> supers = getAllSupertypes0(type, null);
    if (supers == null) {
      return NO_TYPE;
    }
    return supers.toArray(new Type[supers.size()]);
  }

  @Override
  public Type[] getAllTypes() {
    Type[] classes = getAllClasses();
    int classesLength = classes.length;
    Type[] allInterfaces = getAllInterfaces();
    int interfacesLength = allInterfaces.length;
    Type[] all = new Type[classesLength + interfacesLength];
    System.arraycopy(classes, 0, all, 0, classesLength);
    System.arraycopy(allInterfaces, 0, all, classesLength, interfacesLength);
    return all;
  }

  @Override
  public Type[] getExtendingInterfaces(Type type) {
    if (!isInterface(type)) {
      return NO_TYPE;
    }
    return getExtendingInterfaces0(type);
  }

  @Override
  public Type[] getImplementingClasses(Type type) {
    if (!isInterface(type)) {
      return NO_TYPE;
    }
    return getImplementingClasses0(type);
  }

  @Override
  public Type[] getRootClasses() {
    return rootClasses.toArray(new Type[rootClasses.size()]);
  }

  @Override
  public Type[] getRootInterfaces() {
    Type[] allInterfaces = getAllInterfaces();
    Type[] roots = new Type[allInterfaces.length];
    int rootNumber = 0;
    for (int i = 0; i < allInterfaces.length; i++) {
      Type[] superInterfaces = getSuperInterfaces(allInterfaces[i]);
      if (superInterfaces == null || superInterfaces.length == 0) {
        roots[rootNumber++] = allInterfaces[i];
      }
    }
    Type[] result = new Type[rootNumber];
    if (result.length > 0) {
      System.arraycopy(roots, 0, result, 0, rootNumber);
    }
    return result;
  }

  @Override
  public Type[] getSubclasses(Type type) {
    if (isInterface(type)) {
      return NO_TYPE;
    }
    ArrayList<Type> subtypes = typeToSubtypes.get(type);
    if (subtypes == null) {
      return NO_TYPE;
    }
    return subtypes.toArray(new Type[subtypes.size()]);
  }

  @Override
  public Type[] getSubtypes(Type type) {
    return getSubtypesForType(type);
  }

  @Override
  public Type getSuperclass(Type type) {
    if (isInterface(type)) {
      return null;
    }
    return classToSuperclass.get(type);
  }

  @Override
  public Type[] getSuperInterfaces(Type type) {
    Type[] types = typeToSuperInterfaces.get(type);
    if (types == null) {
      return NO_TYPE;
    }
    return types;
  }

  @Override
  public Type[] getSupertypes(Type type) {
    Type superclass = getSuperclass(type);
    if (superclass == null) {
      return getSuperInterfaces(type);
    } else {
      ArrayList<Type> superTypes = new ArrayList<Type>();
      for (Type superType : getSuperInterfaces(type)) {
        superTypes.add(superType);
      }
      superTypes.add(superclass);
      return superTypes.toArray(new Type[superTypes.size()]);
    }
  }

  @Override
  public Type getType() {
    return focusType;
  }

  /*
   * Whether fine-grained deltas where collected and affects this hierarchy.
   */
  public boolean hasFineGrainChanges() {
    ChangeCollector collector = changeCollector;
    return collector != null && collector.needsRefresh();
  }

//@Override
//public int getCachedFlags(Type type) {
//  Integer flagObject = (Integer) typeFlags.get(type);
//  if (flagObject != null){
//    return flagObject.intValue();
//  }
//  return -1;
//}

  /**
   * Returns true if the given delta could change this type hierarchy
   * 
   * @param eventType TODO
   */
  public synchronized boolean isAffected(DartElementDelta delta, int eventType) {
    DartElement element = delta.getElement();
    switch (element.getElementType()) {
      case DartElement.DART_MODEL:
        return isAffectedByJavaModel(delta, element, eventType);
      case DartElement.DART_PROJECT:
        return isAffectedByJavaProject(delta, element, eventType);
//    case DartElement.PACKAGE_FRAGMENT_ROOT:
//      return isAffectedByPackageFragmentRoot(delta, element, eventType);
//    case DartElement.PACKAGE_FRAGMENT:
//      return isAffectedByPackageFragment(delta, (PackageFragment) element, eventType);
//    case DartElement.CLASS_FILE:
      case DartElement.COMPILATION_UNIT:
        return isAffectedByOpenable(delta, element, eventType);
    }
    return false;
  }

  /**
   * Return the Dart project this hierarchy was created in.
   */
  public DartProject javaProject() {
    return focusType.getDartProject();
  }

  /*
   * TODO (jerome) should use a PerThreadObject to build the hierarchy instead of synchronizing (see
   * also isAffected(DartElementDelta))
   */
  @Override
  public synchronized void refresh(IProgressMonitor monitor) throws DartModelException {
    try {
      progressMonitor = monitor;
      if (monitor != null) {
        monitor.beginTask(
            focusType != null ? Messages.bind(
                Messages.hierarchy_creatingOnType,
                focusType.getElementName()) : Messages.hierarchy_creating,
            100);
      }
      long start = -1;
      if (DEBUG) {
        start = System.currentTimeMillis();
        if (computeSubtypes) {
          System.out.println("CREATING TYPE HIERARCHY [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
          System.out.println("CREATING SUPER TYPE HIERARCHY [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (focusType != null) {
          System.out.println("  on type " + ((DartElementImpl) focusType).toStringWithAncestors()); //$NON-NLS-1$
        }
      }

      compute();
      initializeRegions();
      needsRefresh = false;
      changeCollector = null;

      if (DEBUG) {
        if (computeSubtypes) {
          System.out.println("CREATED TYPE HIERARCHY in " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
          System.out.println("CREATED SUPER TYPE HIERARCHY in " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        System.out.println(toString());
      }
    } catch (DartModelException e) {
      throw e;
    } catch (CoreException e) {
      throw new DartModelException(e);
    } finally {
      if (monitor != null) {
        monitor.done();
      }
      progressMonitor = null;
    }
  }

  @Override
  public synchronized void removeTypeHierarchyChangedListener(TypeHierarchyChangedListener listener) {
    ArrayList<TypeHierarchyChangedListener> listeners = changeListeners;
    if (listeners == null) {
      return;
    }
    listeners.remove(listener);

    // deregister from DartCore on last listener removed
    if (listeners.isEmpty()) {
      DartCore.removeElementChangedListener(this);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void store(OutputStream output, IProgressMonitor monitor) throws DartModelException {
    try {
      // compute types in hierarchy
      Hashtable<Type, Integer> hashtable = new Hashtable<Type, Integer>();
      Hashtable<Integer, Type> hashtable2 = new Hashtable<Integer, Type>();
      int count = 0;

      if (focusType != null) {
        Integer index = new Integer(count++);
        hashtable.put(focusType, index);
        hashtable2.put(index, focusType);
      }
      Object[] types = classToSuperclass.entrySet().toArray();
      for (int i = 0; i < types.length; i++) {
        Map.Entry<Type, Type> entry = (Map.Entry<Type, Type>) types[i];
        Type t = entry.getKey();
        if (hashtable.get(t) == null) {
          Integer index = new Integer(count++);
          hashtable.put(t, index);
          hashtable2.put(index, t);
        }
        Type superClass = entry.getValue();
        if (superClass != null && hashtable.get(superClass) == null) {
          Integer index = new Integer(count++);
          hashtable.put(superClass, index);
          hashtable2.put(index, superClass);
        }
      }
      types = typeToSuperInterfaces.entrySet().toArray();
      for (int i = 0; i < types.length; i++) {
        Map.Entry<Type, Type[]> entry = (Map.Entry<Type, Type[]>) types[i];
        Type t = entry.getKey();
        if (hashtable.get(t) == null) {
          Integer index = new Integer(count++);
          hashtable.put(t, index);
          hashtable2.put(index, t);
        }
        Type[] sp = entry.getValue();
        if (sp != null) {
          for (int j = 0; j < sp.length; j++) {
            Type superInterface = sp[j];
            if (sp[j] != null && hashtable.get(superInterface) == null) {
              Integer index = new Integer(count++);
              hashtable.put(superInterface, index);
              hashtable2.put(index, superInterface);
            }
          }
        }
      }
      // save version of the hierarchy format
      output.write(VERSION);

      // save general info
      byte generalInfo = 0;
      if (computeSubtypes) {
        generalInfo |= COMPUTE_SUBTYPES;
      }
      output.write(generalInfo);

      // save project
      if (project != null) {
        output.write(project.getHandleIdentifier().getBytes());
      }
      output.write(SEPARATOR1);

      // save missing types
      for (int i = 0; i < missingTypes.size(); i++) {
        if (i != 0) {
          output.write(SEPARATOR2);
        }
        output.write((missingTypes.get(i)).getBytes());

      }
      output.write(SEPARATOR1);

      // save types
      for (int i = 0; i < count; i++) {
        Type t = hashtable2.get(new Integer(i));

        // n bytes
        output.write(t.getHandleIdentifier().getBytes());
        output.write(SEPARATOR4);
//      output.write(flagsToBytes((Integer)typeFlags.get(t)));
        output.write(SEPARATOR4);
        byte info = CLASS;
        if (focusType != null && focusType.equals(t)) {
          info |= COMPUTED_FOR;
        }
        if (interfaces.contains(t)) {
          info |= INTERFACE;
        }
        if (rootClasses.contains(t)) {
          info |= ROOT;
        }
        output.write(info);
      }
      output.write(SEPARATOR1);

      // save superclasses
      types = classToSuperclass.entrySet().toArray();
      for (int i = 0; i < types.length; i++) {
        Map.Entry<Type, Type> entry = (Map.Entry<Type, Type>) types[i];
        DartElement key = entry.getKey();
        DartElement value = entry.getValue();

        output.write((hashtable.get(key)).toString().getBytes());
        output.write('>');
        output.write((hashtable.get(value)).toString().getBytes());
        output.write(SEPARATOR1);
      }
      output.write(SEPARATOR1);

      // save superinterfaces
      types = typeToSuperInterfaces.entrySet().toArray();
      for (int i = 0; i < types.length; i++) {
        Map.Entry<Type, Type[]> entry = (Map.Entry<Type, Type[]>) types[i];
        DartElement key = entry.getKey();
        DartElement[] values = entry.getValue();

        if (values.length > 0) {
          output.write((hashtable.get(key)).toString().getBytes());
          output.write(SEPARATOR3);
          for (int j = 0; j < values.length; j++) {
            DartElement value = values[j];
            if (j != 0) {
              output.write(SEPARATOR2);
            }
            output.write((hashtable.get(value)).toString().getBytes());
          }
          output.write(SEPARATOR1);
        }
      }
      output.write(SEPARATOR1);
    } catch (IOException e) {
      throw new DartModelException(e, DartModelStatusConstants.IO_EXCEPTION);
    }
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Focus: "); //$NON-NLS-1$
    if (focusType == null) {
      buffer.append("<NONE>\n"); //$NON-NLS-1$
    } else {
      toString(buffer, focusType, 0);
    }
    if (exists()) {
      if (focusType != null) {
        buffer.append("Super types:\n"); //$NON-NLS-1$
        toString(buffer, focusType, 0, true);
        buffer.append("Sub types:\n"); //$NON-NLS-1$
        toString(buffer, focusType, 0, false);
      } else {
        if (rootClasses.size() > 0) {
          DartElement[] roots = Util.sortCopy(getRootClasses());
          buffer.append("Super types of root classes:\n"); //$NON-NLS-1$
          int length = roots.length;
          for (int i = 0; i < length; i++) {
            DartElement root = roots[i];
            toString(buffer, root, 1);
            toString(buffer, root, 1, true);
          }
          buffer.append("Sub types of root classes:\n"); //$NON-NLS-1$
          for (int i = 0; i < length; i++) {
            DartElement root = roots[i];
            toString(buffer, root, 1);
            toString(buffer, root, 1, false);
          }
        } else if (rootClasses.size() == 0) {
          // see http://bugs.eclipse.org/bugs/show_bug.cgi?id=24691
          buffer.append("No root classes"); //$NON-NLS-1$
        }
      }
    } else {
      buffer.append("(Hierarchy became stale)"); //$NON-NLS-1$
    }
    return buffer.toString();
  }

  /**
   * Adds the type to the collection of interfaces.
   */
  protected void addInterface(Type type) {
    interfaces.add(type);
  }

  /**
   * Adds the type to the collection of root classes if the classes is not already present in the
   * collection.
   */
  protected void addRootClass(Type type) {
    if (rootClasses.contains(type)) {
      return;
    }
    rootClasses.add(type);
  }

  /**
   * Adds the given subtype to the type.
   */
  protected void addSubtype(Type type, Type subtype) {
    ArrayList<Type> subtypes = typeToSubtypes.get(type);
    if (subtypes == null) {
      subtypes = new ArrayList<Type>();
      typeToSubtypes.put(type, subtypes);
    }
    if (!subtypes.contains(subtype)) {
      subtypes.add(subtype);
    }
  }

///**
// * cacheFlags.
// */
//public void cacheFlags(Type type, int flags) {
//  typeFlags.put(type, new Integer(flags));
//}
  /**
   * Caches the handle of the superclass for the specified type. As a side effect cache this type as
   * a subtype of the superclass.
   */
  protected void cacheSuperclass(Type type, Type superclass) {
    if (superclass != null) {
      classToSuperclass.put(type, superclass);
      addSubtype(superclass, type);
    }
  }

  /**
   * Caches all of the superinterfaces that are specified for the type.
   */
  protected void cacheSuperInterfaces(Type type, Type[] superinterfaces) {
    typeToSuperInterfaces.put(type, superinterfaces);
    for (int i = 0; i < superinterfaces.length; i++) {
      Type superinterface = superinterfaces[i];
      if (superinterface != null) {
        addSubtype(superinterface, type);
      }
    }
  }

  /**
   * Checks with the progress monitor to see whether the creation of the type hierarchy should be
   * canceled. Should be regularly called so that the user can cancel.
   * 
   * @exception OperationCanceledException if cancelling the operation has been requested
   * @see IProgressMonitor#isCanceled
   */
  protected void checkCanceled() {
    if (progressMonitor != null && progressMonitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  /**
   * Compute this type hierarchy.
   */
  protected void compute() throws DartModelException, CoreException {
    processSupertypes(focusType);
    processSubtypes(focusType);
  }

  /**
   * Adds the new element to a new array that contains all of the elements of the old array. Returns
   * the new array.
   */
  protected Type[] growAndAddToArray(Type[] array, Type addition) {
    if (array == null || array.length == 0) {
      return new Type[] {addition};
    }
    Type[] old = array;
    array = new Type[old.length + 1];
    System.arraycopy(old, 0, array, 0, old.length);
    array[old.length] = addition;
    return array;
  }

  /**
   * Adds the new elements to a new array that contains all of the elements of the old array.
   * Returns the new array.
   */
  protected Type[] growAndAddToArray(Type[] array, Type[] additions) {
    if (array == null || array.length == 0) {
      return additions;
    }
    Type[] old = array;
    array = new Type[old.length + additions.length];
    System.arraycopy(old, 0, array, 0, old.length);
    System.arraycopy(additions, 0, array, old.length, additions.length);
    return array;
  }

  /**
   * Initializes this hierarchy's internal tables with the given size.
   */
  protected void initialize(int size) {
    if (size < 10) {
      size = 10;
    }
    int smallSize = (size / 2);
    classToSuperclass = new HashMap<Type, Type>(size);
    interfaces = new ArrayList<Type>(smallSize);
    missingTypes = new ArrayList<String>(smallSize);
    rootClasses = new ArrayList<Type>();
    typeToSubtypes = new HashMap<Type, ArrayList<Type>>(smallSize);
    typeToSuperInterfaces = new HashMap<Type, Type[]>(smallSize);
//  typeFlags = new HashMap(smallSize);

    projectRegion = new RegionImpl();
    packageRegion = new RegionImpl();
    files = new HashMap<OpenableElement, ArrayList<Type>>(5);
  }

  /**
   * Initializes the file, package and project regions
   */
  protected void initializeRegions() {

    Type[] allTypes = getAllTypes();
    for (int i = 0; i < allTypes.length; i++) {
      Type type = allTypes[i];
      OpenableElement o = ((DartElementImpl) type).getOpenableParent();
      if (o != null) {
        ArrayList<Type> types = files.get(o);
        if (types == null) {
          types = new ArrayList<Type>();
          files.put(o, types);
        }
        types.add(type);
      }
//      IPackageFragment pkg = type.getPackageFragment();
//      packageRegion.add(pkg);
      DartProject declaringProject = type.getDartProject();
      if (declaringProject != null) {
        projectRegion.add(declaringProject);
      }
      checkCanceled();
    }
  }

  /**
   * Returns true if the given type delta (a compilation unit delta or a class file delta) could
   * affect this type hierarchy.
   * 
   * @param eventType TODO
   */
  protected boolean isAffectedByOpenable(DartElementDelta delta, DartElement element, int eventType) {
    if (element instanceof CompilationUnitImpl) {
      CompilationUnitImpl cu = (CompilationUnitImpl) element;
      CompilationUnit focusCU = focusType != null ? focusType.getCompilationUnit() : null;
      if (focusCU != null && focusCU.getOwner() != cu.getOwner()) {
        return false;
      }
      //ADDED delta arising from getWorkingCopy() should be ignored
      if (eventType != ElementChangedEvent.POST_RECONCILE && !cu.isPrimary()
          && delta.getKind() == DartElementDelta.ADDED) {
        return false;
      }
      ChangeCollector collector = changeCollector;
      if (collector == null) {
        collector = new ChangeCollector(this);
      }
      try {
        collector.addChange(cu, delta);
      } catch (DartModelException e) {
        if (DEBUG) {
          e.printStackTrace();
        }
      }
      if (cu.isWorkingCopy() && eventType == ElementChangedEvent.POST_RECONCILE) {
        // changes to working copies are batched
        changeCollector = collector;
        return false;
      } else {
        return collector.needsRefresh();
      }
//  } else if (element instanceof ClassFile) {
//    switch (delta.getKind()) {
//      case DartElementDelta.REMOVED:
//        return files.get(element) != null;
//      case DartElementDelta.ADDED:
//        Type type = ((ClassFile)element).getType();
//        String typeName = type.getElementName();
//        if (hasSupertype(typeName)
//          || subtypesIncludeSupertypeOf(type)
//          || missingTypes.contains(typeName)) {
//
//          return true;
//        }
//        break;
//      case DartElementDelta.CHANGED:
//        DartElementDelta[] children = delta.getAffectedChildren();
//        for (int i = 0, length = children.length; i < length; i++) {
//          DartElementDelta child = children[i];
//          DartElement childElement = child.getElement();
//          if (childElement instanceof Type) {
//            type = (Type)childElement;
//            boolean hasVisibilityChange = (delta.getFlags() & DartElementDelta.F_MODIFIERS) > 0;
//            boolean hasSupertypeChange = (delta.getFlags() & DartElementDelta.F_SUPER_TYPES) > 0;
//            if ((hasVisibilityChange && hasSupertype(type.getElementName()))
//                || (hasSupertypeChange && includesTypeOrSupertype(type))) {
//              return true;
//            }
//          }
//        }
//        break;
//    }
    }
    return false;
  }

  protected void worked(int work) {
    if (progressMonitor != null) {
      progressMonitor.worked(work);
      checkCanceled();
    }
  }

  /**
   * Returns whether one of the types in this hierarchy has a supertype whose simple name is the
   * given simple name.
   */
  boolean hasSupertype(String simpleName) {
    for (Iterator<Type> iter = classToSuperclass.values().iterator(); iter.hasNext();) {
      Type superType = iter.next();
      if (superType.getElementName().equals(simpleName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether the simple name of the given type or one of its supertypes is the simple name
   * of one of the types in this hierarchy.
   */
  boolean includesTypeOrSupertype(Type type) {
    try {
      // check type
      if (hasTypeNamed(type.getElementName())) {
        return true;
      }

      // check superclass
      String superclassName = type.getSuperclassName();
      if (superclassName != null) {
        int lastSeparator = superclassName.lastIndexOf('.');
        String simpleName = superclassName.substring(lastSeparator + 1);
        if (hasTypeNamed(simpleName)) {
          return true;
        }
      }

      // check superinterfaces
      String[] superinterfaceNames = type.getSuperInterfaceNames();
      if (superinterfaceNames != null) {
        for (int i = 0, length = superinterfaceNames.length; i < length; i++) {
          String superinterfaceName = superinterfaceNames[i];
          int lastSeparator = superinterfaceName.lastIndexOf('.');
          String simpleName = superinterfaceName.substring(lastSeparator + 1);
          if (hasTypeNamed(simpleName)) {
            return true;
          }
        }
      }
    } catch (DartModelException e) {
      // ignore
    }
    return false;
  }

  /**
   * Returns whether the simple name of a supertype of the given type is the simple name of one of
   * the subtypes in this hierarchy or the simple name of this type.
   */
  boolean subtypesIncludeSupertypeOf(Type type) {
    // look for superclass
    String superclassName = null;
    try {
      superclassName = type.getSuperclassName();
    } catch (DartModelException e) {
      if (DEBUG) {
        e.printStackTrace();
      }
      return false;
    }
    if (superclassName == null) {
      superclassName = "Object"; //$NON-NLS-1$
    }
    int dot = -1;
    String simpleSuper = (dot = superclassName.lastIndexOf('.')) > -1
        ? superclassName.substring(dot + 1) : superclassName;
    if (hasSubtypeNamed(simpleSuper)) {
      return true;
    }

    // look for super interfaces
    String[] interfaceNames = null;
    try {
      interfaceNames = type.getSuperInterfaceNames();
    } catch (DartModelException e) {
      if (DEBUG) {
        e.printStackTrace();
      }
      return false;
    }
    for (int i = 0, length = interfaceNames.length; i < length; i++) {
      dot = -1;
      String interfaceName = interfaceNames[i];
      String simpleInterface = (dot = interfaceName.lastIndexOf('.')) > -1
          ? interfaceName.substring(dot) : interfaceName;
      if (hasSubtypeNamed(simpleInterface)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Adds all of the elements in the collection to the list if the element is not already in the
   * list.
   */
  private void addAllCheckingDuplicates(ArrayList<Type> list, Type[] collection) {
    for (int i = 0; i < collection.length; i++) {
      Type element = collection[i];
      if (!list.contains(element)) {
        list.add(element);
      }
    }
  }

  private Type[] getAllSubtypesForType(Type type) {
    ArrayList<Type> subTypes = new ArrayList<Type>();
    getAllSubtypesForType0(type, subTypes);
    Type[] subClasses = new Type[subTypes.size()];
    subTypes.toArray(subClasses);
    return subClasses;
  }

  private void getAllSubtypesForType0(Type type, ArrayList<Type> subs) {
    Type[] subTypes = getSubtypesForType(type);
    if (subTypes.length != 0) {
      for (int i = 0; i < subTypes.length; i++) {
        Type subType = subTypes[i];
        subs.add(subType);
        getAllSubtypesForType0(subType, subs);
      }
    }
  }

  private ArrayList<Type> getAllSuperInterfaces0(Type type, ArrayList<Type> supers) {
    Type[] superinterfaces = typeToSuperInterfaces.get(type);
    if (superinterfaces == null) {
      return supers;
    }
    if (superinterfaces.length != 0) {
      if (supers == null) {
        supers = new ArrayList<Type>();
      }
      addAllCheckingDuplicates(supers, superinterfaces);
      for (int i = 0; i < superinterfaces.length; i++) {
        supers = getAllSuperInterfaces0(superinterfaces[i], supers);
      }
    }
    Type superclass = classToSuperclass.get(type);
    if (superclass != null) {
      supers = getAllSuperInterfaces0(superclass, supers);
    }
    return supers;
  }

  private ArrayList<Type> getAllSupertypes0(Type type, ArrayList<Type> supers) {
    Type[] superinterfaces = typeToSuperInterfaces.get(type);
    if (superinterfaces == null) {
      return supers;
    }
    if (superinterfaces.length != 0) {
      if (supers == null) {
        supers = new ArrayList<Type>();
      }
      addAllCheckingDuplicates(supers, superinterfaces);
      for (int i = 0; i < superinterfaces.length; i++) {
        supers = getAllSuperInterfaces0(superinterfaces[i], supers);
      }
    }
    Type superclass = classToSuperclass.get(type);
    if (superclass != null) {
      if (supers == null) {
        supers = new ArrayList<Type>();
      }
      supers.add(superclass);
      supers = getAllSupertypes0(superclass, supers);
    }
    return supers;
  }

  private synchronized ArrayList<TypeHierarchyChangedListener> getClonedChangeListeners() {
    ArrayList<TypeHierarchyChangedListener> listeners = changeListeners;
    if (listeners == null) {
      return null;
    }
    return new ArrayList<TypeHierarchyChangedListener>(listeners);
  }

  /**
   * Assumes that the type is an interface.
   */
  private Type[] getExtendingInterfaces0(Type extendedInterface) {
    Iterator<Map.Entry<Type, Type[]>> iter = typeToSuperInterfaces.entrySet().iterator();
    ArrayList<Type> interfaceList = new ArrayList<Type>();
    while (iter.hasNext()) {
      Map.Entry<Type, Type[]> entry = iter.next();
      Type type = entry.getKey();
      if (!isInterface(type)) {
        continue;
      }
      Type[] superInterfaces = entry.getValue();
      if (superInterfaces != null) {
        for (int i = 0; i < superInterfaces.length; i++) {
          Type superInterface = superInterfaces[i];
          if (superInterface.equals(extendedInterface)) {
            interfaceList.add(type);
          }
        }
      }
    }
    Type[] extendingInterfaces = new Type[interfaceList.size()];
    interfaceList.toArray(extendingInterfaces);
    return extendingInterfaces;
  }

  /**
   * Assumes that the type is an interface.
   */
  private Type[] getImplementingClasses0(Type interfce) {

    Iterator<Map.Entry<Type, Type[]>> iter = typeToSuperInterfaces.entrySet().iterator();
    ArrayList<Type> iMenters = new ArrayList<Type>();
    while (iter.hasNext()) {
      Map.Entry<Type, Type[]> entry = iter.next();
      Type type = entry.getKey();
      if (isInterface(type)) {
        continue;
      }
      Type[] types = entry.getValue();
      for (int i = 0; i < types.length; i++) {
        Type iFace = types[i];
        if (iFace.equals(interfce)) {
          iMenters.add(type);
        }
      }
    }
    Type[] implementers = new Type[iMenters.size()];
    iMenters.toArray(implementers);
    return implementers;
  }

  /**
   * Returns an array of subtypes for the given type - will never return null.
   */
  private Type[] getSubtypesForType(Type type) {
    ArrayList<Type> subtypes = typeToSubtypes.get(type);
    if (subtypes == null) {
      return NO_TYPE;
    }
    return subtypes.toArray(new Type[subtypes.size()]);
  }

  /**
   * Returns whether one of the subtypes in this hierarchy has the given simple name or this type
   * has the given simple name.
   */
  private boolean hasSubtypeNamed(String simpleName) {
    if (focusType != null && focusType.getElementName().equals(simpleName)) {
      return true;
    }
    Type[] types = focusType == null ? getAllTypes() : getAllSubtypes(focusType);
    for (int i = 0, length = types.length; i < length; i++) {
      if (types[i].getElementName().equals(simpleName)) {
        return true;
      }
    }
    return false;
  }

///**
// * Returns <code>true</code> if an equivalent package fragment is included in the package
// * region. Package fragments are equivalent if they both have the same name.
// */
//protected boolean packageRegionContainsSamePackageFragment(PackageFragment element) {
//  DartElement[] pkgs = packageRegion.getElements();
//  for (int i = 0; i < pkgs.length; i++) {
//    PackageFragment pkg = (PackageFragment) pkgs[i];
//    if (Util.equalArraysOrNull(pkg.names, element.names))
//      return true;
//  }
//  return false;
//}

  /**
   * Returns whether one of the types in this hierarchy has the given simple name.
   */
  private boolean hasTypeNamed(String simpleName) {
    Type[] types = getAllTypes();
    for (int i = 0, length = types.length; i < length; i++) {
      if (types[i].getElementName().equals(simpleName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if any of the children of a project, package fragment root, or package fragment
   * have changed in a way that affects this type hierarchy.
   * 
   * @param eventType TODO
   */
  private boolean isAffectedByChildren(DartElementDelta delta, int eventType) {
    if ((delta.getFlags() & DartElementDelta.F_CHILDREN) > 0) {
      DartElementDelta[] children = delta.getAffectedChildren();
      for (int i = 0; i < children.length; i++) {
        if (isAffected(children[i], eventType)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if the given java model delta could affect this type hierarchy
   * 
   * @param eventType TODO
   */
  private boolean isAffectedByJavaModel(DartElementDelta delta, DartElement element, int eventType) {
    switch (delta.getKind()) {
      case DartElementDelta.ADDED:
      case DartElementDelta.REMOVED:
        return element.equals(javaProject().getDartModel());
      case DartElementDelta.CHANGED:
        return isAffectedByChildren(delta, eventType);
    }
    return false;
  }

  /**
   * Returns true if the given java project delta could affect this type hierarchy
   * 
   * @param eventType TODO
   */
  private boolean isAffectedByJavaProject(DartElementDelta delta, DartElement element, int eventType) {
    int kind = delta.getKind();
    int flags = delta.getFlags();
    if ((flags & DartElementDelta.F_OPENED) != 0) {
      kind = DartElementDelta.ADDED; // affected in the same way
    }
    if ((flags & DartElementDelta.F_CLOSED) != 0) {
      kind = DartElementDelta.REMOVED; // affected in the same way
    }
    switch (kind) {
      case DartElementDelta.ADDED:
        DartCore.notYetImplemented();
//        try {
//          // if the added project is on the classpath, then the hierarchy has changed
//          IClasspathEntry[] classpath = ((DartProjectImpl) javaProject()).getExpandedClasspath();
//          for (int i = 0; i < classpath.length; i++) {
//            if (classpath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT
//                && classpath[i].getPath().equals(element.getPath())) {
//              return true;
//            }
//          }
//          if (focusType != null) {
//            // if the hierarchy's project is on the added project classpath, then the hierarchy has changed
//            classpath = ((DartProjectImpl) element).getExpandedClasspath();
//            IPath hierarchyProject = javaProject().getPath();
//            for (int i = 0; i < classpath.length; i++) {
//              if (classpath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT
//                  && classpath[i].getPath().equals(hierarchyProject)) {
//                return true;
//              }
//            }
//          }
//          return false;
//        } catch (DartModelException e) {
//          return false;
//        }
      case DartElementDelta.REMOVED:
        // removed project - if it contains packages we are interested in
        // then the type hierarchy has changed
        DartElement[] pkgs = packageRegion.getElements();
        for (int i = 0; i < pkgs.length; i++) {
          DartProject javaProject = pkgs[i].getDartProject();
          if (javaProject != null && javaProject.equals(element)) {
            return true;
          }
        }
        return false;
      case DartElementDelta.CHANGED:
        return isAffectedByChildren(delta, eventType);
    }
    return false;
  }

//  /**
//   * Returns true if the given package fragment delta could affect this type hierarchy
//   * @param eventType TODO
//   */
//  private boolean isAffectedByPackageFragment(DartElementDelta delta, PackageFragment element, int eventType) {
//    switch (delta.getKind()) {
//      case DartElementDelta.ADDED :
//        // if the package fragment is in the projects being considered, this could
//        // introduce new types, changing the hierarchy
//        return projectRegion.contains(element);
//      case DartElementDelta.REMOVED :
//        // is a change if the package fragment contains types in this hierarchy
//        return packageRegionContainsSamePackageFragment(element);
//      case DartElementDelta.CHANGED :
//        // look at the files in the package fragment
//        return isAffectedByChildren(delta, eventType);
//    }
//    return false;
//  }
//  /**
//   * Returns true if the given package fragment root delta could affect this
//   * type hierarchy
//   * 
//   * @param eventType TODO
//   */
//  private boolean isAffectedByPackageFragmentRoot(DartElementDelta delta,
//      DartElement element, int eventType) {
//    switch (delta.getKind()) {
//      case DartElementDelta.ADDED:
//        return projectRegion.contains(element);
//      case DartElementDelta.REMOVED:
//      case DartElementDelta.CHANGED:
//        int flags = delta.getFlags();
//        if ((flags & DartElementDelta.F_ADDED_TO_CLASSPATH) > 0) {
//          // check if the root is in the classpath of one of the projects of this hierarchy
//          if (projectRegion != null) {
//            IPackageFragmentRoot root = (IPackageFragmentRoot) element;
//            IPath rootPath = root.getPath();
//            DartElement[] elements = projectRegion.getElements();
//            for (int i = 0; i < elements.length; i++) {
//              DartProjectImpl javaProject = (DartProjectImpl) elements[i];
//              try {
//                IClasspathEntry entry = javaProject.getClasspathEntryFor(rootPath);
//                if (entry != null) {
//                  return true;
//                }
//              } catch (DartModelException e) {
//                // igmore this project
//              }
//            }
//          }
//        }
//        if ((flags & DartElementDelta.F_REMOVED_FROM_CLASSPATH) > 0
//            || (flags & DartElementDelta.F_ARCHIVE_CONTENT_CHANGED) > 0) {
//          // 1. removed from classpath - if it contains packages we are interested in
//          // the the type hierarchy has changed
//          // 2. content of a jar changed - if it contains packages we are interested in
//          // then the type hierarchy has changed
//          DartElement[] pkgs = packageRegion.getElements();
//          for (int i = 0; i < pkgs.length; i++) {
//            if (pkgs[i].getParent().equals(element)) {
//              return true;
//            }
//          }
//          return false;
//        }
//    }
//    return isAffectedByChildren(delta, eventType);
//  }

  private boolean isInterface(Type type) {
//  int flags = getCachedFlags(type);
//  if (flags == -1) {
//    try {
//      return type.isInterface();
//    } catch (DartModelException e) {
//      return false;
//    }
//  } else {
//    return Flags.isInterface(flags);
//  }
    try {
      return type.isInterface();
    } catch (DartModelException exception) {
      // This shouldn't happen. In fact, the method shouldn't throw an exception.
      return false;
    }
  }

  private void processSubtypes(Type type) {
    processSubtypes(type, new HashSet<Type>());
  }

  private void processSubtypes(Type type, HashSet<Type> processedTypes) {
    if (processedTypes.contains(type)) {
      return;
    }
    processedTypes.add(type);
    try {
      List<SearchMatch> matches = SearchEngineFactory.createSearchEngine().searchSubtypes(
          type,
          SearchScopeFactory.createWorkspaceScope(),
          null,
          null);
      for (SearchMatch match : matches) {
        DartElement element = match.getElement();
        if (element instanceof Type) {
          Type subtype = (Type) element;
          addSubtype(type, subtype);
          processSubtypes(subtype, processedTypes);
        }
      }
    } catch (SearchException exception) {
      DartCore.logError("Could not search for subtypes of " + type.getElementName(), exception);
    }
  }

  private void processSupertypes(Type type) {
    processSupertypes(type, new HashSet<Type>());
  }

  private void processSupertypes(Type type, HashSet<Type> processedTypes) {
    if (processedTypes.contains(type)) {
      return;
    }
    processedTypes.add(type);
    try {
      ArrayList<Type> interfaceList = new ArrayList<Type>();
      List<SearchMatch> matches = SearchEngineFactory.createSearchEngine().searchSupertypes(
          type,
          SearchScopeFactory.createWorkspaceScope(),
          null,
          null);
      for (SearchMatch match : matches) {
        DartElement element = match.getElement();
        if (element instanceof Type) {
          Type supertype = (Type) element;
          if (isInterface(supertype)) {
            interfaceList.add(supertype);
          } else {
            cacheSuperclass(type, supertype);
          }
          processSupertypes(supertype, processedTypes);
        }
      }
      if (!interfaceList.isEmpty()) {
        cacheSuperInterfaces(type, interfaceList.toArray(new Type[interfaceList.size()]));
      }
    } catch (SearchException exception) {
      DartCore.logError("Could not search for supertypes of " + type.getElementName(), exception);
    }
  }

  private void toString(StringBuffer buffer, DartElement type, int indent) {
    for (int j = 0; j < indent; j++) {
      buffer.append("  "); //$NON-NLS-1$
    }
    buffer.append(((DartElementImpl) type).toStringWithAncestors(false));
    buffer.append('\n');
  }

  /**
   * Append a String to the given buffer representing the hierarchy for the type, beginning with the
   * specified indentation level. If ascendant, shows the super types, otherwise show the sub types.
   */
  private void toString(StringBuffer buffer, DartElement type, int indent, boolean ascendant) {
    Type[] types = ascendant ? getSupertypes((Type) type) : getSubtypes((Type) type);
    DartElement[] sortedTypes = Util.sortCopy(types);
    for (int i = 0; i < sortedTypes.length; i++) {
      toString(buffer, sortedTypes[i], indent + 1);
      toString(buffer, sortedTypes[i], indent + 1, ascendant);
    }
  }
}
