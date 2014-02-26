library di.dynamic_injector;

import 'di.dart';
import 'mirrors.dart';

/**
 * Dynamic implementation of [Injector] that uses mirrors.
 */
class DynamicInjector extends Injector {

  DynamicInjector({List<Module> modules, String name,
                  bool allowImplicitInjection: false})
      : super(modules: modules, name: name,
          allowImplicitInjection: allowImplicitInjection);

  DynamicInjector._fromParent(List<Module> modules, Injector parent, {name})
      : super.fromParent(modules, parent, name: name);

  newFromParent(List<Module> modules, String name) {
    return new DynamicInjector._fromParent(modules, this, name: name);
  }

  Object newInstanceOf(Type type, ObjectFactory getInstanceByType,
                       Injector requestor, error) {
    var classMirror = reflectType(type);
    if (classMirror is TypedefMirror) {
      throw new NoProviderError(error('No implementation provided '
          'for ${getSymbolName(classMirror.qualifiedName)} typedef!'));
    }

    MethodMirror ctor = classMirror.declarations[classMirror.simpleName];

    resolveArgument(int pos) {
      ParameterMirror p = ctor.parameters[pos];
      return getInstanceByType(getReflectedTypeWorkaround(p.type), requestor);
    }

    var args = new List.generate(ctor.parameters.length, resolveArgument,
        growable: false);
    return classMirror.newInstance(ctor.constructorName, args).reflectee;
  }

  /**
   * Invoke given function and inject all its arguments.
   *
   * Returns whatever the function returns.
   */
  dynamic invoke(Function fn) {
    ClosureMirror cm = reflect(fn);
    MethodMirror mm = cm.function;
    int position = 0;
    List args = mm.parameters.map((ParameterMirror parameter) {
      try {
        return get(getReflectedTypeWorkaround(parameter.type));
      } on NoProviderError catch (e) {
        throw new NoProviderError(e.message);
      } finally {
        position++;
      }
    }).toList();

    return cm.apply(args).reflectee;
  }
}
