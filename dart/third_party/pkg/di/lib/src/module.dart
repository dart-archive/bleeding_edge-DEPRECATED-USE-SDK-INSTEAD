part of di;

typedef dynamic FactoryFn(Injector injector);

/**
 * Visibility determines if the instance in the defining module is visible to
 * the requesting injector. If true is returned, then the instance from the
 * defining injector is provided. If false is returned, the injector keeps
 * walking up the tree to find another visible instance.
 */
typedef bool Visibility(Injector requesting, Injector defining);

typedef Object TypeFactory(factory(Type type, Type annotation));

/**
 * Module contributes configuration information to an [Injector] by providing a collection of type
 * bindings that specify how each type is created.
 *
 * When an injector is created, it copies its configuration information from a module.
 * Defining additional type bindings after an injector is created have no effect on that injector.
 *
 */
class Module {
  final _providers = <int, Provider>{};
  final _childModules = <Module>[];
  Map<Type, TypeFactory> _typeFactories = {};

  Map<Type, TypeFactory> get typeFactories {
    if (_childModules.isEmpty) return _typeFactories;

    var factories = new Map.from(_typeFactories);
    _childModules.forEach((m) {
      if (m.typeFactories != null) {
        factories.addAll(m.typeFactories);
      }
    });
    return factories;
  }

  set typeFactories(Map<Type, TypeFactory> factories) {
    _typeFactories = factories;
  }

  Map<int, Provider> _providersCache;

  /**
   * Compiles and returns a map of type bindings by performing depth-first traversal of the
   * child (installed) modules.
   */
  Map<int, Provider> get bindings {
    if (_isDirty) {
      _providersCache = <int, Provider>{};
      _childModules.forEach((child) => _providersCache.addAll(child.bindings));
      _providersCache.addAll(_providers);
    }
    return _providersCache;
  }

  /**
   * Register a binding to a concrete value.
   *
   * The [value] is what actually will be injected.
   */
  void value(Type id, value, {Type withAnnotation, Visibility visibility}) {
    _dirty();
    Key key = new Key(id, withAnnotation);
    _providers[key.id] = new ValueProvider(id, value, visibility);
  }

  /**
   * Registers a binding for a [Type].
   *
   * The default behavior is to simply instantiate the type.
   *
   * The following parameters can be specified:
   *
   * * [withAnnotation]: Type decorated with additional annotation.
   * * [implementedBy]: The type will be instantiated using the [new] operator and the
   *   resulting instance will be injected. If no type is provided, then it's
   *   implied that [type] should be instantiated.
   * * [visibility]: Function which determines fi the requesting injector can see the type in the
   *   current injector.
   */
  void type(Type type, {Type withAnnotation, Type implementedBy, Visibility visibility}) {
    _dirty();
    Key key = new Key(type, withAnnotation);
    _providers[key.id] = new TypeProvider(
        implementedBy == null ? type : implementedBy, visibility);
  }

  /**
   * Register a binding to a factory function.
   *
   * The [factoryFn] will be called and all its arguments will get injected.
   * The result of that function is the value that will be injected.
   */
  void factory(Type id, FactoryFn factoryFn, {Type withAnnotation,
      Visibility visibility}) {
    factoryByKey(new Key(id, withAnnotation), factoryFn,
        visibility: visibility);
  }

  void factoryByKey(Key key, FactoryFn factoryFn, {Visibility visibility}) {
    _dirty();
    _providers[key.id] = new FactoryProvider(key.type, factoryFn, visibility);
  }

  /**
   * Installs another module into this module. Bindings defined on this module
   * take precidence over the installed module.
   */
  void install(Module module) {
    _childModules.add(module);
    _dirty();
  }

  _dirty() {
    _providersCache = null;
  }

  bool get _isDirty =>
      _providersCache == null || _childModules.any((m) => m._isDirty);
}
