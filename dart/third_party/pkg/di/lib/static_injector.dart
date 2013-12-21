library di.static_injector;

import 'di.dart';

typedef Object TypeFactory(factory(Type));

/**
 * Static implementation of [Injector] that uses type factories
 */
class StaticInjector extends Injector {
  Map<Type, TypeFactory> typeFactories;

  StaticInjector({List<Module> modules, String name,
                 bool allowImplicitInjection: false, this.typeFactories})
      : super(modules: modules, name: name,
          allowImplicitInjection: allowImplicitInjection);

  StaticInjector._fromParent(List<Module> modules, Injector parent, {name})
      : super.fromParent(modules, parent, name: name);

  newFromParent(List<Module> modules, String name) {
    return new StaticInjector._fromParent(modules, this, name: name);
  }

  Object newInstanceOf(Type type, ObjectFactory getInstanceByType,
                       Injector requestor, error) {
    TypeFactory typeFactory = (root as StaticInjector).typeFactories[type];
    if (typeFactory == null) {
      throw new NoProviderError(error('No type factory provided for $type!'));
    }
    return typeFactory((type) => getInstanceByType(type, requestor));
  }
}