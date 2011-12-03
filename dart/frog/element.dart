// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Any abstract representation of a dart element.  This includes
 * [Library], [Type] and [Member].
 */
class Element implements Hashable {
  // TODO(jimhug): Make name final when we can do it for Library.
  /** The user-visible name of this [Element]. */
  String name;

  /** A safe name to use for this [Element] in generated JS code. */
  String _jsname;

  /** The lexically/logically enclosing [Element] for lookups. */
  Element _enclosingElement;

  Element(this.name, this._enclosingElement) {
    _jsname = world.toJsIdentifier(name);
  }

  // TODO - walk tree
  Library get library() => null;

  /** A source location for messages to the user about this [Element]. */
  SourceSpan get span() => null;

  /** Should this element be treated as native JS? */
  bool get isNative() => false;

  int hashCode() => name.hashCode();

  /** Will return a safe name to refer to this element with in JS code. */
  String get jsname() => _jsname;

  /** Resolve types and other references in the [Element]. */
  void resolve() {}

  /**
   * Any type parameters that this element defines to setup a generic
   * type resolution context.  This is currently used for both generic
   * types and the semi-magical generic factory methods - but it will
   * not be used for any other members in the current dart language.
   */
  List<ParameterType> get typeParameters() => null;

  // TODO(jimhug): Probably kill this.
  Element get enclosingElement() =>
    _enclosingElement === null ? library : _enclosingElement;

  // TODO(jimhug): Absolutely kill this one.
  set enclosingElement(Element e) => _enclosingElement = e;


  /**
   * Resolves [node] in the context of this element.  Will
   * search up the tree of [enclosingElement] to look for matches.
   * If [typeErrors] then types that are not found will create errors,
   * otherwise they will only signal warnings.
   */
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
      if (typeRef.type != null) {
        return typeRef.type;
      }

      return enclosingElement.resolveType(node, typeErrors);
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
        typeArgs.add(resolveType(typeRef.typeArguments[i], typeErrors));
      }
      typeRef.type = baseType.getOrMakeConcreteType(typeArgs);
    } else if (node is FunctionTypeReference) {
      FunctionTypeReference typeRef = node;
      var name = '';
      if (typeRef.func.name != null) {
        name = typeRef.func.name.name;
      }
      // Totally bogus!
      typeRef.type = library.getOrAddFunctionType(this, name, typeRef.func);
    } else {
      world.internalError('unknown type reference', node.span);
    }
    return node.type;
  }
}
