part of di;

class Injector {

  /**
   * Name of the injector or null of none is given.
   */
  final String name;

  static const List<Type> _PRIMITIVE_TYPES = const <Type>[
    num, int, double, String, bool
  ];

  /**
   * Returns the parent injector or null if root.
   */
  final Injector parent;

  Injector _root;

  Map<Type, _Provider> _providers = <Type, _Provider>{};

  final Map<Type, Object> instances = <Type, Object>{};

  final List<Type> resolving = <Type>[];

  final bool allowImplicitInjection;

  Iterable<Type> _typesCache;

  /**
   * List of all types which the injector can return
   */
  Iterable<Type> get _types {
    if (_typesCache == null) {
      _typesCache = _providers.keys;
    }
    return _typesCache;
  }

  Injector({List<Module> modules, String name,
           bool allowImplicitInjection: false})
      : this.fromParent(modules, null,
          name: name, allowImplicitInjection: allowImplicitInjection);

  Injector.fromParent(List<Module> modules,
      Injector this.parent, {this.name, this.allowImplicitInjection}) {
    _root = parent == null ? this : parent._root;
    if (modules != null) {
      modules.forEach((module) {
        _providers.addAll(module._bindings);
      });
    }
    _providers[Injector] = new _ValueProvider(this);
  }

  Injector get root => _root;

  Set<Type> get types {
    var types = new Set.from(_types);
    var parent = this.parent;
    while (parent != null) {
      types.addAll(parent._types);
      parent = parent.parent;
    }
    return types;
  }

  String _error(message, [appendDependency]) {
    if (appendDependency != null) {
      resolving.add(appendDependency);
    }

    String graph = resolving.join(' -> ');

    resolving.clear();

    return '$message (resolving $graph)';
  }

  dynamic _getInstanceByType(Type typeName, Injector requester) {
    _checkTypeConditions(typeName);

    if (resolving.contains(typeName)) {
      throw new CircularDependencyError(
          _error('Cannot resolve a circular dependency!', typeName));
    }

    var providerWithInjector = _getProviderWithInjectorForType(typeName);
    var provider = providerWithInjector.provider;
    var injector = providerWithInjector.injector;
    var visible = provider.visibility != null ?
        provider.visibility(requester, injector) :
        _defaultVisibility(requester, injector);

    if (visible && instances.containsKey(typeName)) {
      return instances[typeName];
    }

    if (providerWithInjector.injector != this || !visible) {
      if (!visible) {
        if (injector.parent == null) {
          throw new NoProviderError(
              _error('No provider found for ${typeName}!', typeName));
        }
        injector =
            injector.parent._getProviderWithInjectorForType(typeName).injector;
      }
      return injector._getInstanceByType(typeName, requester);
    }

    var value;
    try {
      var strategy = provider.creationStrategy != null ?
          provider.creationStrategy : _defaultCreationStrategy;
      value = strategy(requester, injector, () {
        resolving.add(typeName);
        var val = provider.get(this, requester, _getInstanceByType, _error);
        resolving.removeLast();
        return val;
      });
    } catch(e) {
      resolving.clear();
      rethrow;
    }

    // cache the value.
    providerWithInjector.injector.instances[typeName] = value;
    return value;
  }

  /// Returns a pair for provider and the injector where it's defined.
  _ProviderWithDefiningInjector _getProviderWithInjectorForType(Type typeName) {
    if (_providers.containsKey(typeName)) {
      return new _ProviderWithDefiningInjector(_providers[typeName], this);
    }

    if (parent != null) {
      return parent._getProviderWithInjectorForType(typeName);
    }

    if (allowImplicitInjection) {
      return new _ProviderWithDefiningInjector(
          new _TypeProvider(typeName), this);
    }

    throw new NoProviderError(_error('No provider found for '
        '${typeName}!', typeName));
  }

  void _checkTypeConditions(Type typeName) {
    if (_PRIMITIVE_TYPES.contains(typeName)) {
      throw new NoProviderError(_error('Cannot inject a primitive type '
          'of $typeName!', typeName));
    }
  }


  // PUBLIC API

  /**
   * Get an instance for given token ([Type]).
   *
   * If the injector already has an instance for this token, it returns this
   * instance. Otherwise, injector resolves all its dependencies, instantiate
   * new instance and returns this instance.
   *
   * If there is no binding for given token, injector asks parent injector.
   *
   * If there is no parent injector, an implicit binding is used. That is,
   * the token ([Type]) is instantiated.
   */
  dynamic get(Type type) => _getInstanceByType(type, this);

  /**
   * Create a child injector.
   *
   * Child injector can override any bindings by adding additional modules.
   *
   * It also accepts a list of tokens that a new instance should be forced.
   * That means, even if some parent injector already has an instance for this
   * token, there will be a new instance created in the child injector.
   */
  Injector createChild(List<Module> modules,
                       {List<Type> forceNewInstances, String name}) {
    if (forceNewInstances != null) {
      Module forceNew = new Module();
      forceNewInstances.forEach((type) {
        var providerWithInjector = _getProviderWithInjectorForType(type);
        var provider = providerWithInjector.provider;
        forceNew.factory(type,
            (Injector inj) => provider.get(this, inj, inj._getInstanceByType,
                inj._error),
            creation: provider.creationStrategy,
            visibility: provider.visibility);
      });

      modules = modules.toList(); // clone
      modules.add(forceNew);
    }

    return newFromParent(modules, name);
  }

  newFromParent(List<Module> modules, String name) {
    throw new UnimplementedError('This method must be overriden.');
  }

  Object newInstanceOf(Type type, ObjectFactory factory, Injector requestor,
                       errorHandler(message, [appendDependency])) {
    throw new UnimplementedError('This method must be overriden.');
  }
}

class _ProviderWithDefiningInjector {
  final _Provider provider;
  final Injector injector;
  _ProviderWithDefiningInjector(this.provider, this.injector);
}
