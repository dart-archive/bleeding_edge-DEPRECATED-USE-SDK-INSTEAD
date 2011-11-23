// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Type implements Named, Hashable {
  final String name;
  bool isTested;

  /**
   * For core types (int, String, etc) this is the generated type assertion
   * function (uses JS "typeof"). This field is null for all other types.
   */
  String typeCheckCode;

  String _jsname;

  Member _typeMember;

  /** Stubs used to call into this method dynamically. Lazy initialized. */
  Map<String, VarMember> varStubs;

  Type(this.name): isTested = false;

  void markUsed() {}
  abstract void genMethod(Member method);

  TypeMember get typeMember() {
    if (_typeMember == null) {
      _typeMember = new TypeMember(this);
    }
    return _typeMember;
  }

  abstract SourceSpan get span();

  abstract Type resolveType(TypeReference node, bool isRequired);

  abstract Type resolveTypeParams(ConcreteType inType);

  abstract MemberSet resolveMember(String name);
  Member getMember(String name) => null;
  abstract MethodMember getConstructor(String name);
  abstract MethodMember getFactory(Type type, String name);
  abstract Type getOrMakeConcreteType(List<Type> typeArgs);
  abstract Map<String, MethodMember> get constructors();
  abstract addDirectSubtype(Type type);
  abstract bool get isClass();
  abstract Library get library();

  // TODO(jmesserly): rename to isDynamic?
  bool get isVar() => false;
  bool get isTop() => false;

  bool get isObject() => false;
  bool get isString() => false;
  bool get isBool() => false;
  bool get isFunction() => false;
  bool get isList() => false;
  bool get isNum() => false;
  bool get isInt() => false;
  bool get isDouble() => false;
  bool get isVoid() => false;

  // True for all types in the Dart type system. We track non-nullabiltity
  // as an optimization for booleans to generate better code in checked mode.
  bool get isNullable() => true;

  // Strangely Dart treats calls on Function much like calls on var.
  bool get isVarOrFunction() => isVar || isFunction;

  bool get isVarOrObject() => isVar || isObject;

  /** Gets the $call method for a function type. */
  MethodMember getCallMethod() => null;

  /** These types may not be implemented or extended by user code. */
  bool get isClosed() => isString || isBool || isNum || isFunction || isVar;

  bool get isUsed() => false;

  bool get isGeneric() => false;
  bool get isNativeType() => false;

  bool get isNative() => isNativeType; // TODO(jimhug): remove isNativeType.

  bool get isHiddenNativeType() => false;

  bool get hasTypeParams() => false;

  String get typeofName() => null;

  String get jsname() => _jsname == null ? name : _jsname;

  set jsname(String name) => _jsname = name;

  Map<String, Member> get members() => null;
  Definition get definition() => null;
  FactoryMap get factories() => null;

  // TODO(jmesserly): should try using a const list instead of null to represent
  // the absence of type parameters.
  Collection<Type> get typeArgsInOrder() => null;
  DefinedType get genericType() => this;

  // TODO(jmesserly): what should these do for ParameterType?
  List<Type> get interfaces() => null;
  Type get parent() => null;

  Map<String, Member> getAllMembers() => {};

  int hashCode() => name.hashCode();

  void _checkOverride(Member member) {
    // always look in parents to check that any overloads are legal
    var parentMember = _getMemberInParents(member.name);
    if (parentMember != null) {
      // TODO(jimhug): Ensure that this is only done once.
      if (!member.isPrivate || member.library == parentMember.library) {
        member.override(parentMember);
      }
    }
  }

  Member _createNotEqualMember() {
    // Add a != method just like the == one.
    MethodMember eq = members['\$eq'];
    if (eq == null) {
      world.internalError('INTERNAL: object does not define ==',
        definition.span);
    }
    final ne = new MethodMember('\$ne', this, eq.definition);
    ne.isGenerated = true;
    ne.returnType = eq.returnType;
    ne.parameters = eq.parameters;
    ne.isStatic = eq.isStatic;
    ne.isAbstract = eq.isAbstract;
    // TODO - What else to fill in?
    return ne;
  }

  Member _getMemberInParents(String memberName) {
    // print('getting $memberName in parents of $name, $isClass');
    // Now look in my parents.
    if (isClass) {
      if (parent != null) {
        return parent.getMember(memberName);
      } else if (isObject) {  // Could also be a top type so need check.
        // Create synthetic != method if needed.
        if (memberName == '\$ne') {
          var ret = _createNotEqualMember();
          members[memberName] = ret;
          return ret;
        }
        return null;
      }
    } else {
      // TODO(jimhug): Will probably check types more than once - errors?
      if (interfaces != null && interfaces.length > 0) {
        for (var i in interfaces) {
          var ret = i.getMember(memberName);
          if (ret != null) {
            return ret;
          }
        }
        return null;
      } else {
        return world.objectType.getMember(memberName);
      }
    }
  }

  void ensureSubtypeOf(Type other, SourceSpan span, [bool typeErrors=false]) {
    if (!isSubtypeOf(other)) {
      var msg = 'type $name is not a subtype of ${other.name}';
      if (typeErrors) {
        world.error(msg, span);
      } else {
        world.warning(msg, span);
      }
    }
  }

  /**
   * Returns true if we need to use our .call$N$names calling convention.
   * This is only needed for calls to Functions where we lack enough type info.
   */
  bool needsVarCall(Arguments args) {
    if (isVarOrFunction) {
      return true;
    }

    var call = getCallMethod();
    if (call != null) {
      // If the call doesn't fill in all arguments, or it doesn't use the right
      // named parameter order, we need to go through a "var" call because we
      // don't know what arguments the callee wants to fill in.

      // TODO(jmesserly): we could be smarter if the optional calls look
      // similar enough, which is probably true quite often in practice.
      if (args.length != call.parameters.length || !call.namesInOrder(args)) {
        return true;
      }
    }

    // Use a normal JS call, or not a function type.
    return false;
  }

  static Type union(Type x, Type y) {
    if (x == y) return x;
    if (x.isNum && y.isNum) return world.numType;
    if (x.isString && y.isString) return world.stringType;

    // TODO(jmesserly): better abstraction for union types
    return world.varType;
  }

  // This is from the "Interface Types" section of the language spec:

  /**
   * A type T may be assigned to a type S, written T <=> S, i either T <: S
   * or S <: T.
   */
  bool isAssignable(Type other) {
    return isSubtypeOf(other) || other.isSubtypeOf(this);
  }

  /**
   * An interface I is a direct supertype of an interface J iff:
   * If I is Object, and J has no extends clause
   * if I is listed in the extends clause of J.
   */
  bool _isDirectSupertypeOf(Type other) {
    if (other.isClass) {
      return other.parent == this || isObject && other.parent == null;
    } else {
      if (other.interfaces == null || other.interfaces.isEmpty()) {
        return isObject;
      } else {
        return other.interfaces.some((i) => i == this);
      }
    }
  }

  /**
   * This implements the subtype operator <: defined in "Interface Types"
   * of the language spec. It's implemented in terms of the << "more specific"
   * operator. The spec is below:
   *
   * A type T is more specific than a type S, written T << S, if one of the
   * following conditions is met:
   *   - T is S.
   *   - T is Bottom.
   *   - S is Dynamic.
   *   - S is a direct supertype of T.
   *   - T is a type variable and S is the upper bound of T.
   *   - T is of the form I<T1,...,Tn> and S is of the form I<S1,...,Sn>
   *       and: Ti << Si, 1 <= i <= n
   *   - T << U and U << S.
   *
   * << is a partial order on types. T is a subtype of S, written T <: S, iff
   * [Bottom/Dynamic]T << S.
   */
  // TODO(jmesserly): this function could be expensive. Memoize results?
  // TODO(jmesserly): should merge this with the subtypes/directSubtypes
  // machinery? Possible issues: needing this during resolve(), integrating
  // the more accurate generics/function subtype handling.
  bool isSubtypeOf(Type other) {
    if (other is ParameterType) {
      // TODO(jmesserly): treating type variables as Dynamic is totally busted.
      // It's a workaround for bugs in our our current type system, where
      // "new List()" produces a ListFactory that inherits from List<E>
      // instead of List<Dynamic>.

      //ParameterType p = other;
      //other = p.extendsType;
      return true;
    }

    if (this == other) return true;
    // Note: the extra "isVar" check here is the difference between << and <:
    // Since we don't implement the << relation itself, we can just pretend
    // "null" literals are Dynamic and not worry about the Bottom type.
    if (isVar) return true;
    if (other.isVar) return true;
    if (other._isDirectSupertypeOf(this)) return true;

    var call = getCallMethod();
    var otherCall = other.getCallMethod();
    if (call != null && otherCall != null) {
      return _isFunctionSubtypeOf(call, otherCall);
    }

    if (genericType == other.genericType
        && typeArgsInOrder != null && other.typeArgsInOrder != null
        && typeArgsInOrder.length == other.typeArgsInOrder.length) {

      var t = typeArgsInOrder.iterator();
      var s = other.typeArgsInOrder.iterator();
      while (t.hasNext()) {
        // Type args don't have subtype relationship
        if (!t.next().isSubtypeOf(s.next())) return false;
      }
      return true;
    }

    // And now for some fun: T << U and U << S -> T << S
    // To implement this, we need to enumerate a set of types C such that
    // U will be an element of C. We can do this by either enumerating less
    // specific types of T, or more specific types of S.
    if (parent != null && parent.isSubtypeOf(other)) {
      return true;
    }
    if (interfaces != null && interfaces.some((i) => i.isSubtypeOf(other))) {
      return true;
    }

    // Unrelated types
    return false;
  }

  /**
   * A function type (T1,...,Tn, [Tx1 x1,..., Txk xk]) -> T is a subtype of the
   * function type   (S1,...,Sn, [Sy1 y1,..., Sym ym]) -> S, if all of the
   * following conditions are met:
   *   1. Either:
   *      - S is void, Or
   *      - T <=> S.
   *   2. for all i in 1..n, Ti <=> Si
   *   3. k >= m and xi = yi, i is in 1..m. It is necessary, but not sufficient,
   *      that the optional arguments of the subtype be a subset of those of the
   *      supertype. We cannot treat them as just sets, because optional
   *      arguments can be invoked positionally, so the order matters.
   *   4. For all y in {y1,..., ym}Sy <=> Ty
   * We write (T1,..., Tn) => T as a shorthand for the type (T1,...,Tn,[]) => T.
   * All functions implement the interface Function, so all function types are a
   * subtype of Function.
   */
  static bool _isFunctionSubtypeOf(MethodMember t, MethodMember s) {
    if (!s.returnType.isVoid && !s.returnType.isAssignable(t.returnType)) {
      return false; // incompatible return types
    }

    var tp = t.parameters;
    var sp = s.parameters;

    // Function subtype must have >= the total number of arguments
    if (tp.length < sp.length) return false;

    for (int i = 0; i < sp.length; i++) {
      // Mismatched required parameter count
      if (tp[i].isOptional != sp[i].isOptional) return false;

      // Mismatched optional parameter name
      if (tp[i].isOptional && tp[i].name != sp[i].name) return false;

      // Parameter types not assignable
      if (!tp[i].type.isAssignable(sp[i].type)) return false;
    }

    // Mismatched required parameter count
    if (tp.length > sp.length && !tp[sp.length].isOptional) return false;

    return true;
  }
}


/** A type parameter within the body of the type. */
class ParameterType extends Type {
  TypeParameter typeParameter;
  Type extendsType;

  bool get isClass() => false;
  Library get library() => null; // TODO(jimhug): Make right...
  SourceSpan get span()  => typeParameter.span;

  ParameterType(String name, this.typeParameter): super(name);

  Map<String, MethodMember> get constructors() {
    world.internalError('no constructors on type parameters yet');
  }

  MethodMember getCallMethod() => extendsType.getCallMethod();

  void genMethod(Member method) {
    extendsType.genMethod(method);
  }

  // TODO(jmesserly): should be like this:
  //bool isSubtypeOf(Type other) => extendsType.isSubtypeOf(other);
  bool isSubtypeOf(Type other) => true;

  MemberSet resolveMember(String memberName) {
    return extendsType.resolveMember(memberName);
  }

  MethodMember getConstructor(String constructorName) {
    world.internalError('no constructors on type parameters yet');
  }

  Type getOrMakeConcreteType(List<Type> typeArgs) {
    world.internalError('no concrete types of type parameters yet', span);
  }

  Type resolveTypeParams(ConcreteType inType) {
    return inType.typeArguments[name];
  }

  addDirectSubtype(Type type) {
    world.internalError('no subtypes of type parameters yet', span);
  }

  resolve(Type inType) {
    if (typeParameter.extendsType != null) {
      extendsType = inType.resolveType(typeParameter.extendsType, true);
    } else {
      extendsType = world.objectType;
    }
  }
}

/**
 * Non-nullable type. Currently used for bools, so we can generate better
 * asserts in checked mode. Forwards almost all operations to its real type.
 */
// NOTE: there's more work to do before this would work for types other than
// bool.
class NonNullableType extends Type {

  /** The corresponding nullable [Type]. */
  final Type type;

  NonNullableType(Type type): super(type.name), type = type;

  bool get isNullable() => false;

  // TODO(jmesserly): this would need to change if we support other types.
  bool get isBool() => type.isBool;

  // Treat it as unused so it doesn't get JS generated
  bool get isUsed() => false;

  // Augment our subtype rules with: non-nullable types are subtypes of
  // themselves, their corresponding nullable types, or anything that type is a
  // subtype of.
  bool isSubtypeOf(Type other) =>
      this == other || type == other || type.isSubtypeOf(other);

  // Forward everything. This is overkill for now; might be useful later.
  Type resolveType(TypeReference node, bool isRequired) =>
      type.resolveType(node, isRequired);
  Type resolveTypeParams(ConcreteType inType) => type.resolveTypeParams(inType);
  void addDirectSubtype(Type subtype) { type.addDirectSubtype(subtype); }
  void markUsed() { type.markUsed(); }
  void genMethod(Member method) { type.genMethod(method); }
  SourceSpan get span() => type.span;
  MemberSet resolveMember(String name) => type.resolveMember(name);
  Member getMember(String name) => type.getMember(name);
  MethodMember getConstructor(String name) => type.getConstructor(name);
  MethodMember getFactory(Type t, String name) => type.getFactory(t, name);
  Type getOrMakeConcreteType(List<Type> typeArgs) =>
      type.getOrMakeConcreteType(typeArgs);
  Map<String, MethodMember> get constructors() => type.constructors;
  bool get isClass() => type.isClass;
  Library get library() => type.library;
  MethodMember getCallMethod() => type.getCallMethod();
  bool get isGeneric() => type.isGeneric;
  bool get hasTypeParams() => type.hasTypeParams;
  String get typeofName() => type.typeofName;
  String get jsname() => type.jsname;
  set jsname(String name) => type.jsname = name;
  Map<String, Member> get members() => type.members;
  Definition get definition() => type.definition;
  FactoryMap get factories() => type.factories;
  Collection<Type> get typeArgsInOrder() => type.typeArgsInOrder;
  DefinedType get genericType() => type.genericType;
  List<Type> get interfaces() => type.interfaces;
  Type get parent() => type.parent;
  Map<String, Member> getAllMembers() => type.getAllMembers();
  bool get isNativeType() => type.isNativeType;
}

/** A concrete version of a generic type. */
class ConcreteType extends Type {
  final DefinedType genericType;
  Map<String, Type> typeArguments;
  List<Type> _interfaces;
  Type _parent;
  List<Type> typeArgsInOrder;

  bool get isList() => genericType.isList;
  bool get isClass() => genericType.isClass;
  Library get library() => genericType.library;
  SourceSpan get span()  => genericType.span;


  bool get hasTypeParams() =>
    typeArguments.getValues().some((e) => e is ParameterType);

  /**
   * Keeps a collection of members for which a concrete version needed to
   * be created.  For constructors we always create this.  For other methods,
   * we will do this for methods whose bodies need to be specialized for
   * a type parameter and in checked mode for methods that we need to
   * generate appropriate concrete checks on.
   */
  Map<String, Member> members;
  Map<String, MemberSet> _resolvedMembers;
  Map<String, MethodMember> constructors;
  FactoryMap factories;

  ConcreteType(String name,
               this.genericType,
               this.typeArguments,
               this.typeArgsInOrder):
    super(name), constructors = {}, members = {}, _resolvedMembers = {},
      factories = new FactoryMap();

  Type resolveTypeParams(ConcreteType inType) {
    var newTypeArgs = [];
    var needsNewType = false;
    for (var t in typeArgsInOrder) {
      var newType = t.resolveTypeParams(inType);
      if (newType != t) needsNewType = true;
      newTypeArgs.add(newType);
    }
    if (!needsNewType) return this;
    return genericType.getOrMakeConcreteType(newTypeArgs);
  }

  Type getOrMakeConcreteType(List<Type> typeArgs) {
    return genericType.getOrMakeConcreteType(typeArgs);
  }

  Type get parent() {
    if (_parent == null && genericType.parent != null) {
      _parent = genericType.parent.resolveTypeParams(this);
    }
    return _parent;
  }

  List<Type> get interfaces() {
    if (_interfaces == null && genericType.interfaces != null) {
      _interfaces = [];
      for (var i in genericType.interfaces) {
        _interfaces.add(i.resolveTypeParams(this));
      }
    }
    return _interfaces;
  }

  // TODO(jmesserly): fill in type args?
  // We can't look in our own members, because we'll get a ConcreteMember
  // which is not fully compatible with MethodMember.
  MethodMember getCallMethod() => genericType.getCallMethod();

  /**
   * Gets all members in the type. Some of these are concrete, others are
   * generic, depending on if we've decided to specialize it.
   */
  Map<String, Member> getAllMembers() {
    var result = genericType.getAllMembers();
    for (var memberName in result.getKeys()) {
      var myMember = members[memberName];
      if (myMember != null) {
        result[memberName] = myMember;
      }
    }
    return result;
  }

  void markUsed() {
    genericType.markUsed();
  }
  void genMethod(Member method) {
    genericType.genMethod(method);
  }


  getFactory(Type type, String constructorName) {
    return genericType.getFactory(type, constructorName);
  }

  getConstructor(String constructorName) {
    var ret = constructors[constructorName];
    if (ret != null) return ret;

    ret = factories.getFactory(name, constructorName);
    if (ret != null) return ret;

    var genericMember = genericType.getConstructor(constructorName);
    if (genericMember == null) return null;

    // In case the constructor is defined in another class.
    if (genericMember.declaringType != genericType) {
      if (!genericMember.declaringType.isGeneric) return genericMember;
      var newDeclaringType =
        genericMember.declaringType.getOrMakeConcreteType(typeArgsInOrder);
      var factory = newDeclaringType.getFactory(genericType, constructorName);
      if (factory !== null) return factory;
      return newDeclaringType.getConstructor(constructorName);
    }

    if (genericMember.isFactory) {
      ret = new ConcreteMember(genericMember.name, this, genericMember);
      factories.addFactory(name, constructorName, ret);
    } else {
      ret = new ConcreteMember(name, this, genericMember);
      constructors[constructorName] = ret;
    }
    return ret;
  }

  Member getMember(String memberName) {
    Member member = members[memberName];
    if (member != null) {
      _checkOverride(member);
      return member;
    }

    // Note: only look directly in the generic type. The transitive search
    // through superclass/interfaces is handled below.
    var genericMember = genericType.members[memberName];
    if (genericMember != null) {
      member = new ConcreteMember(genericMember.name, this, genericMember);
      members[memberName] = member;
      return member;
    }

    return _getMemberInParents(memberName);
  }

  MemberSet resolveMember(String memberName) {
    // TODO(jimhug): Cut-and-paste and tweak from Type <frown>.
    MemberSet ret = _resolvedMembers[memberName];
    if (ret != null) return ret;

    Member member = getMember(memberName);
    if (member == null) {
      // TODO(jimhug): Check for members on subtypes given dart's dynamism.
      return null;
    }

    // TODO(jimhug): Move this adding subtypes logic to MemberSet?
    ret = new MemberSet(member);
    _resolvedMembers[memberName] = ret;
    if (member.isStatic) {
      return ret;
    } else {
      for (var t in genericType.subtypes) {
        // TODO(jimhug): Make these non-generic!
        if (!isClass && t.isClass) {
          // If this is an interface, the actual implementation may
          // come from a class that does not implement this interface.
          // TODO(vsm): Use a more efficient lookup strategy.
          // TODO(jimhug): This is made uglier by need to avoid dups.
          final m = t.getMember(memberName);
          if (m != null && ret.members.indexOf(m) == -1) {
            ret.add(m);
          }
        } else {
          final m = t.members[memberName];
          if (m != null) ret.add(m);
        }
      }
      return ret;
    }
  }

  Type resolveType(TypeReference node, bool isRequired) {
    var ret = genericType.resolveType(node, isRequired);
    // add type info
    return ret;
  }


  addDirectSubtype(Type type) {
    // TODO(jimhug): Does this go on the generic type or the concrete one?
    genericType.addDirectSubtype(type);
  }
}


/** Represents a Dart type defined as source code. */
class DefinedType extends Type {
  // Not final so that we can fill this in for special types like List or num.
  Definition definition;
  final Library library;
  final bool isClass;

  // TODO(vsm): Restore the field once Issue 280 is fixed.
  // Type parent;
  Type _parent;
  Type get parent() => _parent;
  void set parent(Type p) { _parent = p; }

  List<Type> interfaces;
  Type factory_;

  Set<Type> directSubtypes;
  Set<Type> _subtypes;

  List<ParameterType> typeParameters;
  Collection<Type> _typeArgsInOrder;

  Map<String, MethodMember> constructors;
  Map<String, Member> members;
  FactoryMap factories;

  Map<String, MemberSet> _resolvedMembers;

  Map<String, ConcreteType> _concreteTypes;

  /** Methods to be generated once we know for sure that the type is used. */
  Map<String, Member> _lazyGenMethods;

  bool isUsed = false;
  bool isNativeType = false;

  DefinedType(String name, this.library, Definition definition, this.isClass)
      : super(name), directSubtypes = new Set<Type>(), constructors = {},
        members = {}, factories = new FactoryMap(), _resolvedMembers = {} {
    setDefinition(definition);
  }

  void setDefinition(Definition def) {
    assert(definition == null);
    definition = def;
    if (definition is TypeDefinition && definition.nativeType != null) {
      isNativeType = true;
    }
    if (definition != null && definition.typeParameters != null) {
      _concreteTypes = {};
      typeParameters = [];
      // TODO(jimhug): Check for duplicate names.
      for (var tp in definition.typeParameters) {
        var paramName = tp.name.name;
        typeParameters.add(new ParameterType(paramName, tp));
      }
    }
  }

  bool get isHiddenNativeType() =>
      library == world.dom ||  // Legacy, remove after DOM is updated.
      (definition.nativeType != null &&
       definition.nativeType.isConstructorHidden);

  // TODO(jmesserly): this is a workaround for generic types not filling in
  // "Dynamic" as their type arguments.
  Collection<Type> get typeArgsInOrder() {
    if (typeParameters == null) return null;
    if (_typeArgsInOrder == null) {
      _typeArgsInOrder = new FixedCollection<Type>(
          world.varType, typeParameters.length);
    }
    return _typeArgsInOrder;
  }

  bool get isVar() => this == world.varType;
  bool get isVoid() => this == world.voidType;

  /** Is this the type that holds onto top-level code for its library? **/
  bool get isTop() => name == null;

  // TODO(jimhug) -> this == world.objectType, etc.
  bool get isObject() => library.isCore && name == 'Object';

  // TODO(jimhug): Really hating on the interface + impl pattern by now...
  bool get isString() => library.isCore && name == 'String' ||
    library.isCoreImpl && name == 'StringImplementation';

  bool get isBool() => library.isCore && name == 'bool';
  bool get isFunction() => library.isCore && name == 'Function';
  bool get isList() => library.isCore && name == 'List';

  bool get isGeneric() => typeParameters != null;

  SourceSpan get span()  => definition == null ? null : definition.span;


  String get typeofName() {
    if (!library.isCore) return null;

    if (isBool) return 'boolean';
    else if (isNum) return 'number';
    else if (isString) return 'string';
    else if (isFunction) return 'function';
    else return null;
  }

  // TODO(jimhug): Reconcile different number types on JS.
  bool get isNum() {
    return library != null && library.isCore &&
      (name == 'num' || name == 'int' || name == 'double');
  }

  bool get isInt() => this == world.intType;
  bool get isDouble() => this == world.doubleType;

  MethodMember getCallMethod() => members['\$call'];

  Map<String, Member> getAllMembers() => new Map.from(members);

  void markUsed() {
    if (isUsed) return;

    isUsed = true;

    if (_lazyGenMethods != null) {
      for (var method in orderValuesByKeys(_lazyGenMethods)) {
        world.gen.genMethod(method);
      }
      _lazyGenMethods = null;
    }

    if (parent != null) parent.markUsed();
  }

  void genMethod(Member method) {
    if (isUsed) {
      world.gen.genMethod(method);
    } else if (isClass) {
      if (_lazyGenMethods == null) _lazyGenMethods = {};
      _lazyGenMethods[method.name] = method;
    }
  }

  List<Type> _resolveInterfaces(List<TypeReference> types) {
    if (types == null) return [];
    var interfaces = [];
    for (final type in types) {
      var resolvedInterface = resolveType(type, true);
      if (resolvedInterface.isClosed &&
          !(library.isCore || library.isCoreImpl)) {
        world.error(
          'can not implement "${resolvedInterface.name}": ' +
          'only native implementation allowed', type.span);
      }
      resolvedInterface.addDirectSubtype(this);
      // TODO(jimhug): if (resolveInterface.isClass) may need special handling.
      interfaces.add(resolvedInterface);
    }
    return interfaces;
  }

  addDirectSubtype(Type type) {
    // Ensure that this is only called during resolve pass
    assert(_subtypes == null);
    directSubtypes.add(type);
  }

  Set<Type> get subtypes() {
    if (_subtypes == null) {
      _subtypes = new Set<Type>();
      for (var st in directSubtypes) {
        _subtypes.add(st);
        _subtypes.addAll(st.subtypes);
      }
    }
    return _subtypes;
  }

  /** Check whether this class has a cycle in its inheritance chain. */
  bool _cycleInClassExtends() {
    final seen = new Set();
    seen.add(this);
    var ancestor = parent;
    while (ancestor != null) {
      if (ancestor === this) {
        return true;
      }
      if (seen.contains(ancestor)) {
        // there is a cycle above, but [this] is not part of it
        return false;
      }
      seen.add(ancestor);
      ancestor = ancestor.parent;
    }
    return false;
  }

  /**
   * Check whether this interface has a cycle in its inheritance chain. If so,
   * returns which of the parent interfaces creates the cycle (for error
   * reporting).
   */
  int _cycleInInterfaceExtends() {
    final seen = new Set();
    seen.add(this);

    bool _helper(var ancestor) {
      if (ancestor == null) return false;
      if (ancestor === this) return true;
      if (seen.contains(ancestor)) {
        // this detects both cycles and DAGs with interfaces not involving
        // [this]. In the case of cycles, we won't report an error here (but
        // where the cycle was first detected), with DAGs we just take advantage
        // that we detected it to avoid traversing twice.
        return false;
      }
      seen.add(ancestor);
      if (ancestor.interfaces != null) {
        for (final parent in ancestor.interfaces) {
          if (_helper(parent)) return true;
        }
      }
      return false;
    }

    for (int i = 0; i < interfaces.length; i++) {
      if (_helper(interfaces[i])) return i;
    }
    return -1;
  }

  resolve() {
    if (definition is TypeDefinition) {
      TypeDefinition typeDef = definition;
      if (isClass) {
        if (typeDef.extendsTypes != null && typeDef.extendsTypes.length > 0) {
          if (typeDef.extendsTypes.length > 1) {
            world.error('more than one base class',
              typeDef.extendsTypes[1].span);
          }
          var extendsTypeRef = typeDef.extendsTypes[0];
          if (extendsTypeRef is GenericTypeReference) {
            // If we are extending a generic type first resolve against the
            // base type, then the full generic type. This makes circular
            // "extends" checks on generic type args work correctly.
            GenericTypeReference g = extendsTypeRef;
            parent = resolveType(g.baseType, true);
          }
          parent = resolveType(extendsTypeRef, true);
          if (!parent.isClass) {
            world.error('class may not extend an interface - use implements',
              typeDef.extendsTypes[0].span);
          }
          parent.addDirectSubtype(this);
          if (_cycleInClassExtends()) {
            world.error('class "$name" has a cycle in its inheritance chain',
                extendsTypeRef.span);
          }
        } else {
          if (!isObject) {
            // Object is the default parent for everthing except Object.
            parent = world.objectType;
            parent.addDirectSubtype(this);
          }
        }
        this.interfaces = _resolveInterfaces(typeDef.implementsTypes);
        if (typeDef.factoryType != null) {
          world.error('factory not allowed on classes',
            typeDef.factoryType.span);
        }
      } else {
        if (typeDef.implementsTypes != null &&
              typeDef.implementsTypes.length > 0) {
          world.error('implements not allowed on interfaces (use extends)',
            typeDef.implementsTypes[0].span);
        }
        this.interfaces = _resolveInterfaces(typeDef.extendsTypes);
        final res = _cycleInInterfaceExtends();
        if (res >= 0) {
          world.error('interface "$name" has a cycle in its inheritance chain',
              typeDef.extendsTypes[res].span);
        }

        if (typeDef.factoryType != null) {
          factory_ = resolveType(typeDef.factoryType, true);
          if (factory_ == null) {
            // TODO(jimhug): Appropriate warning levels;
            world.warning('unresolved factory', typeDef.factoryType.span);
          }
        }
      }
    } else if (definition is FunctionTypeDefinition) {
      // Function types implement the Function interface.
      this.interfaces = [world.functionType];
    }

    if (typeParameters != null) {
      for (var tp in typeParameters) {
        tp.resolve(this);
      }
    }

    world._addType(this);

    for (var c in constructors.getValues()) c.resolve(this);
    for (var m in members.getValues()) m.resolve(this);
    factories.forEach((f) => f.resolve(this));
  }

  addMethod(String methodName, FunctionDefinition definition) {
    if (methodName == null) methodName = definition.name.name;

    var method = new MethodMember(methodName, this, definition);

    if (method.isConstructor) {
      if (constructors.containsKey(method.constructorName)) {
        world.error('duplicate constructor definition of ${method.name}',
          definition.span);
        return;
      }
      constructors[method.constructorName] = method;
      return;
    }

    if (definition.modifiers != null
        && definition.modifiers.length == 1
        && definition.modifiers[0].kind == TokenKind.FACTORY) {
      // constructorName for a factory is the type.
      if (factories.getFactory(method.constructorName, method.name) != null) {
        world.error('duplicate factory definition of "${method.name}"',
          definition.span);
        return;
      }
      factories.addFactory(method.constructorName, method.name, method);
      return;
    }

    if (methodName.startsWith('get\$') || methodName.startsWith('set\$')) {
      var propName = methodName.substring(4);
      var prop = members[propName];
      if (prop == null) {
        prop = new PropertyMember(propName, this);
        members[propName] = prop;
      }
      if (prop is! PropertyMember) {
        world.error('property conflicts with field "$propName"',
          definition.span);
        return;
      }
      if (methodName[0] == 'g') {
        if (prop.getter != null) {
          world.error('duplicate getter definition for "$propName"',
            definition.span);
        }
        // TODO(jimhug): Validate zero parameters
        prop.getter = method;
      } else {
        if (prop.setter != null) {
          world.error('duplicate setter definition for "$propName"',
            definition.span);
        }
        // TODO(jimhug): Validate one parameters - match with getter?
        prop.setter = method;
      }
      return;
    }

    if (members.containsKey(methodName)) {
      world.error('duplicate method definition of "${method.name}"',
        definition.span);
      return;
    }
    members[methodName] = method;
  }

  addField(VariableDefinition definition) {
    for (int i=0; i < definition.names.length; i++) {
      var name = definition.names[i].name;
      if (members.containsKey(name)) {
        world.error('duplicate field definition of "$name"',
          definition.span);
        return;
      }
      var value = null;
      if (definition.values != null) {
        value = definition.values[i];
      }
      var field = new FieldMember(name, this, definition, value);
      members[name] = field;
      if (isNativeType) {
        field.isNative = true;
      }
    }
  }

  getFactory(Type type, String constructorName) {
    // Try to find factory method with the given type.
    // TODO(jimhug): Use jsname as key here or something better?
    var ret = factories.getFactory(type.name, constructorName);
    if (ret != null) return ret;

    // TODO(ngeoffray): Here we should actually check if the current
    // type implements the given type.
    // Try to find a factory method of this type.
    ret = factories.getFactory(name, constructorName);
    if (ret != null) return ret;

    // Try to find a generative constructor of this type.
    ret = constructors[constructorName];
    if (ret != null) return ret;

    return _tryCreateDefaultConstructor(constructorName);
  }


  getConstructor(String constructorName) {
    var ret = constructors[constructorName];
    if (ret != null) {
      if (factory_ != null) {
        return factory_.getFactory(this, constructorName);
      }
      return ret;
    }
    ret = factories.getFactory(name, constructorName);
    if (ret != null) return ret;

    return _tryCreateDefaultConstructor(constructorName);
  }

  _tryCreateDefaultConstructor(String name) {
    // Check if we can create a default constructor.
    if (name == '' && definition != null && isClass &&
        constructors.length == 0) {
      var span = definition.span;

      var inits = null, body = null;
      if (isNativeType) {
        body = new NativeStatement(null, span);
        inits = null;
      } else {
        body = null;
        inits = [new CallExpression(new SuperExpression(span), [], span)];
      }


      TypeDefinition typeDef = definition;
      var c = new FunctionDefinition(null, null, typeDef.name, [],
        inits, body, span);
      addMethod(null, c);
      constructors[''].resolve(this);
      return constructors[''];
    }
    return null;
  }

  Member getMember(String memberName) {
    Member member = members[memberName];

    if (member != null) {
      _checkOverride(member);
      return member;
    }

    if (isTop) {
      // Let's pretend classes are members of the top-level library type
      // TODO(jmesserly): using "this." to workaround a VM bug with abstract
      // getters.
      var libType = this.library.findTypeByName(memberName);
      if (libType != null) {
        return libType.typeMember;
      }
    }

    return _getMemberInParents(memberName);
  }

  MemberSet resolveMember(String memberName) {
    MemberSet ret = _resolvedMembers[memberName];
    if (ret != null) return ret;

    Member member = getMember(memberName);
    if (member == null) {
      // TODO(jimhug): Check for members on subtypes given dart's dynamism.
      return null;
    }

    // TODO(jimhug): Move this adding subtypes logic to MemberSet?
    ret = new MemberSet(member);
    _resolvedMembers[memberName] = ret;
    if (member.isStatic) {
      return ret;
    } else {
      for (var t in subtypes) {
        if (!isClass && t.isClass) {
          // If this is an interface, the actual implementation may
          // come from a class that does not implement this interface.
          // TODO(vsm): Use a more efficient lookup strategy.
          // TODO(jimhug): This is made uglier by need to avoid dups.
          final m = t.getMember(memberName);
          if (m != null && ret.members.indexOf(m) == -1) {
            ret.add(m);
          }
        } else {
          final m = t.members[memberName];
          if (m != null) ret.add(m);
        }
      }
      return ret;
    }
  }

  static String _getDottedName(NameTypeReference type) {
    if (type.names != null) {
      var names = map(type.names, (n) => n.name);
      return type.name.name + '.' + Strings.join(names, '.');
    } else {
      return type.name.name;
    }
  }

  Type resolveType(TypeReference node, bool typeErrors) {
    if (node == null) return world.varType;

    if (node.type != null) return node.type;

    // TODO(jmesserly): if we failed to resolve a type, we need a way to save
    // that it was an error, so we don't try to resolve it again and show the
    // same message twice.

    if (node is NameTypeReference) {
      NameTypeReference typeRef = node;
      String name;
      if (typeRef.names != null) {
        name = typeRef.names.last().name;
      } else {
        name = typeRef.name.name;
      }
      if (typeParameters != null) {
        for (var tp in typeParameters) {
          if (tp.name == name) {
            typeRef.type = tp;
          }
        }
      }
      if (typeRef.type == null) {
        typeRef.type = library.findType(typeRef);
      }
      if (typeRef.type == null) {
        var message = 'cannot find type ${_getDottedName(typeRef)}';
        if (typeErrors) {
          world.error(message, typeRef.span);
          typeRef.type = world.objectType;
        } else {
          world.warning(message, typeRef.span);
          typeRef.type = world.varType;
        }
      }
    } else if (node is GenericTypeReference) {
      GenericTypeReference typeRef = node;
      // TODO(jimhug): Expand the handling of typeErrors to generics and funcs
      var baseType = resolveType(typeRef.baseType, typeErrors);
      if (!baseType.isGeneric) {
        world.error('${baseType.name} is not generic', typeRef.span);
        return null;
      }
      if (typeRef.typeArguments.length != baseType.typeParameters.length) {
        world.error('wrong number of type arguments', typeRef.span);
        return null;
      }
      var typeArgs = [];
      for (int i=0; i < typeRef.typeArguments.length; i++) {
        var extendsType = baseType.typeParameters[i].extendsType;
        var typeArg = resolveType(typeRef.typeArguments[i], typeErrors);
        typeArgs.add(typeArg);

        if (extendsType != null && typeArg is! ParameterType) {
          typeArg.ensureSubtypeOf(extendsType,
              typeRef.typeArguments[i].span, typeErrors);
        }
      }
      typeRef.type = baseType.getOrMakeConcreteType(typeArgs);
    } else if (node is FunctionTypeReference) {
      FunctionTypeReference typeRef = node;
      var name = '';
      if (typeRef.func.name != null) name = typeRef.func.name.name;
      typeRef.type = library.getOrAddFunctionType(name, typeRef.func, this);
    } else {
      world.internalError('unknown type reference', node.span);
    }
    return node.type;
  }

  Type resolveTypeParams(ConcreteType inType) => this;

  Type getOrMakeConcreteType(List<Type> typeArgs) {
    assert(isGeneric);
    var names = [name];
    var typeMap = {};
    for (int i=0; i < typeArgs.length; i++) {
      var paramName = typeParameters[i].name;
      typeMap[paramName] = typeArgs[i];
      names.add(typeArgs[i].name);
    }

    var concreteName = Strings.join(names, '\$');

    var ret = _concreteTypes[concreteName];
    if (ret == null) {
      ret = new ConcreteType(concreteName, this, typeMap, typeArgs);
      _concreteTypes[concreteName] = ret;
    }
    return ret;
  }

  VarFunctionStub getCallStub(Arguments args) {
    assert(isFunction);

    var name = _getCallStubName('call', args);
    if (varStubs == null) varStubs = {};
    var stub = varStubs[name];
    if (stub == null) {
      stub = new VarFunctionStub(name, args);
      varStubs[name] = stub;
    }
    return stub;
  }
}

/**
 * Information about a native type from the native string.
 *
 *  "Foo"  - constructor function is called 'Foo'.
 *  "*Foo" - name is 'Foo', constructor function and prototype are not available
 *      in global scope during initialization.  This is characteristic of many
 *      DOM types like CanvasPixelArray.
 */
class NativeType {
  String name;
  bool isConstructorHidden;

  NativeType(String spec) {
    if (spec.startsWith('*')) {
      name = spec.substring(1);
      isConstructorHidden = true;
    } else {
      name = spec;
      isConstructorHidden = false;
    }
  }
}
