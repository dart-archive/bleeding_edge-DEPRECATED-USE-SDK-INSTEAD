@Injectables(const [
  ClassOne,
  CircularA,
  CircularB,
  MultipleConstructors,
  NumDependency,
  IntDependency,
  DoubleDependency,
  BoolDependency,
  StringDependency
])
library di.tests;

import 'fixed-unittest.dart';
import 'package:di/di.dart';
import 'package:di/dynamic_injector.dart';
import 'package:di/static_injector.dart';
import 'package:di/annotations.dart';

// Generated file. Run ../test_tf_gen.sh.
import 'type_factories_gen.dart' as type_factories_gen;

/**
 * Annotation used to mark classes for which static type factory must be
 * generated. For testing purposes not all classes are marked with this
 * annotation, some classes are included in @Injectables at the top.
 */
class Injectable {
  const Injectable();
}

// just some classes for testing
@Injectable()
class Engine {
  final String id = 'v8-id';
}

@Injectable()
class MockEngine implements Engine {
  final String id = 'mock-id';
}

@Injectable()
class MockEngine2 implements Engine {
  String id = 'mock-id-2';
}

class HiddenConstructor {
  HiddenConstructor._();
}

@Injectable()
class Car {
  Engine engine;
  Injector injector;

  Car(this.engine, this.injector);
}

class Lemon {
  final engine;
  final Injector injector;

  Lemon(this.engine, this.injector);
}

class NumDependency {
  NumDependency(num value) {}
}

class IntDependency {
  IntDependency(int value) {}
}

class DoubleDependency {
  DoubleDependency(double value) {}
}

class StringDependency {
  StringDependency(String value) {}
}

class BoolDependency {
  BoolDependency(bool value) {}
}


class CircularA {
  CircularA(CircularB b) {}
}

class CircularB {
  CircularB(CircularA a) {}
}

typedef int CompareInt(int a, int b);

int compareIntAsc(int a, int b) => b.compareTo(a);

class WithTypeDefDependency {
  CompareInt compare;

  WithTypeDefDependency(this.compare);
}

class MultipleConstructors {
  String instantiatedVia;
  MultipleConstructors() : instantiatedVia = 'default';
  MultipleConstructors.named() : instantiatedVia = 'named';
}

class InterfaceOne {
}

class ClassOne implements InterfaceOne {
  ClassOne(Log log) {
    log.add('ClassOne');
  }
}

@Injectable()
class ParameterizedType<T1, T2> {
  ParameterizedType();
}

@Injectable()
class ParameterizedDependency {
  final ParameterizedType<bool, int> _p;
  ParameterizedDependency(this._p);
}

@Injectable()
class GenericParameterizedDependency {
  final ParameterizedType _p;
  GenericParameterizedDependency(this._p);
}

@Injectable()
class Log {
  var log = [];

  add(String message) => log.add(message);
}

class EmulatedMockEngineFactory {
  call(Injector i) => new MockEngine();
}

void main() {
  createInjectorSpec('DynamicInjector',
      (modules, [name]) => new DynamicInjector(modules: modules, name: name));

  createInjectorSpec('StaticInjector',
      (modules, [name]) => new StaticInjector(modules: modules, name: name,
          typeFactories: type_factories_gen.typeFactories));

  dynamicInjectorTest();
  staticInjectorTest();
}

typedef Injector InjectorFactory(List<Module> modules, [String name]);

createInjectorSpec(String injectorName, InjectorFactory injectorFactory) {

  describe(injectorName, () {

    it('should instantiate a type', () {
      var injector = injectorFactory([new Module()..type(Engine)]);
      var instance = injector.get(Engine);

      expect(instance, instanceOf(Engine));
      expect(instance.id, toEqual('v8-id'));
    });

    it('should fail if no binding is found', () {
      var injector = injectorFactory([]);
      expect(() {
        injector.get(Engine);
      }, toThrow(NoProviderError, 'No provider found for Engine! '
      '(resolving Engine)'));
    });


    it('should resolve basic dependencies', () {
      var injector = injectorFactory([new Module()..type(Car)..type(Engine)]);
      var instance = injector.get(Car);

      expect(instance, instanceOf(Car));
      expect(instance.engine.id, toEqual('v8-id'));
    });


    it('should inject generic parameterized types', () {
      var injector = injectorFactory([new Module()
            ..type(ParameterizedType)
            ..type(GenericParameterizedDependency)
      ]);
      expect(injector.get(GenericParameterizedDependency),
          new isInstanceOf<GenericParameterizedDependency>());
    });


    xit('should error while resolving parameterized types', () {
      var injector = injectorFactory([new Module()
            ..type(ParameterizedType)
            ..type(ParameterizedDependency)
      ]);
      expect(() => injector.get(ParameterizedDependency), throws);
    });


    it('should allow modules and overriding providers', () {
      var module = new Module()..type(Engine, implementedBy: MockEngine);

      // injector is immutable
      // you can't load more modules once it's instantiated
      // (you can create a child injector)
      var injector = injectorFactory([module]);
      var instance = injector.get(Engine);

      expect(instance.id, toEqual('mock-id'));
    });


    it('should only create a single instance', () {
      var injector = injectorFactory([new Module()..type(Engine)]);
      var first = injector.get(Engine);
      var second = injector.get(Engine);

      expect(first, toBe(second));
    });


    it('should allow providing values', () {
      var module = new Module()
        ..value(Engine, 'str value')
        ..value(Car, 123);

      var injector = injectorFactory([module]);
      var abcInstance = injector.get(Engine);
      var complexInstance = injector.get(Car);

      expect(abcInstance, toEqual('str value'));
      expect(complexInstance, toEqual(123));
    });


    it('should allow providing factory functions', () {
      var module = new Module()..factory(Engine, (Injector injector) {
        return 'factory-product';
      });

      var injector = injectorFactory([module]);
      var instance = injector.get(Engine);

      expect(instance, toEqual('factory-product'));
    });


    it('should allow providing with emulated factory functions', () {
      var module = new Module();
      module.factory(Engine, new EmulatedMockEngineFactory());

      var injector = injectorFactory([module]);
      var instance = injector.get(Engine);

      expect(instance, new isInstanceOf<MockEngine>());
    });


    it('should inject injector into factory function', () {
      var module = new Module()
        ..type(Engine)
        ..factory(Car, (Injector injector) {
        return new Car(injector.get(Engine), injector);
      });

      var injector = injectorFactory([module]);
      var instance = injector.get(Car);

      expect(instance, instanceOf(Car));
      expect(instance.engine.id, toEqual('v8-id'));
    });


    it('should throw an exception when injecting a primitive type', () {
      var injector = injectorFactory([
        new Module()
          ..type(NumDependency)
          ..type(IntDependency)
          ..type(DoubleDependency)
          ..type(BoolDependency)
          ..type(StringDependency)
      ]);

      expect(() {
        injector.get(NumDependency);
      }, toThrow(NoProviderError, 'Cannot inject a primitive type of num! '
      '(resolving NumDependency -> num)'));

      expect(() {
        injector.get(IntDependency);
      }, toThrow(NoProviderError, 'Cannot inject a primitive type of int! '
      '(resolving IntDependency -> int)'));

      expect(() {
        injector.get(DoubleDependency);
      }, toThrow(NoProviderError, 'Cannot inject a primitive type of double! '
      '(resolving DoubleDependency -> double)'));

      expect(() {
        injector.get(BoolDependency);
      }, toThrow(NoProviderError, 'Cannot inject a primitive type of bool! '
      '(resolving BoolDependency -> bool)'));

      expect(() {
        injector.get(StringDependency);
      }, toThrow(NoProviderError, 'Cannot inject a primitive type of String! '
      '(resolving StringDependency -> String)'));
    });


    it('should throw an exception when circular dependency', () {
      var injector = injectorFactory([new Module()..type(CircularA)..type(CircularB)]);

      expect(() {
        injector.get(CircularA);
      }, toThrow(CircularDependencyError, 'Cannot resolve a circular dependency! '
          '(resolving CircularA -> '
      'CircularB -> CircularA)'));
    });


    it('should provide the injector as Injector', () {
      var injector = injectorFactory([]);

      expect(injector.get(Injector), toBe(injector));
    });


    it('should inject a typedef', () {
      var module = new Module()..value(CompareInt, compareIntAsc);

      var injector = injectorFactory([module]);
      var compare = injector.get(CompareInt);

      expect(compare(1, 2), toBe(1));
      expect(compare(5, 2), toBe(-1));
    });


    it('should throw an exception when injecting typedef without providing it', () {
      var injector = injectorFactory([new Module()..type(WithTypeDefDependency)]);

      expect(() {
        injector.get(WithTypeDefDependency);
      }, throws);
    });


    it('should instantiate via the default/unnamed constructor', () {
      var injector = injectorFactory([new Module()..type(MultipleConstructors)]);
      MultipleConstructors instance = injector.get(MultipleConstructors);
      expect(instance.instantiatedVia, 'default');
    });

    // CHILD INJECTORS
    it('should inject from child', () {
      var module = new Module()..type(Engine, implementedBy: MockEngine);

      var parent = injectorFactory([new Module()..type(Engine)]);
      var child = parent.createChild([module]);

      var abcFromParent = parent.get(Engine);
      var abcFromChild = child.get(Engine);

      expect(abcFromParent.id, toEqual('v8-id'));
      expect(abcFromChild.id, toEqual('mock-id'));
    });


    it('should enumerate across children', () {
      var parent = injectorFactory([new Module()..type(Engine)]);
      var child = parent.createChild([new Module()..type(MockEngine)]);

      expect(parent.types, unorderedEquals(new Set.from([Engine, Injector])));
      expect(child.types, unorderedEquals(new Set.from([Engine, MockEngine, Injector])));
    });


    it('should inject instance from parent if not provided in child', () {
      var module = new Module()..type(Car);

      var parent = injectorFactory([new Module()..type(Car)..type(Engine)]);
      var child = parent.createChild([module]);

      var complexFromParent = parent.get(Car);
      var complexFromChild = child.get(Car);
      var abcFromParent = parent.get(Engine);
      var abcFromChild = child.get(Engine);

      expect(complexFromChild, not(toBe(complexFromParent)));
      expect(abcFromChild, toBe(abcFromParent));
    });


    it('should inject instance from parent but never use dependency from child', () {
      var module = new Module()..type(Engine, implementedBy: MockEngine);

      var parent = injectorFactory([new Module()..type(Car)..type(Engine)]);
      var child = parent.createChild([module]);

      var complexFromParent = parent.get(Car);
      var complexFromChild = child.get(Car);
      var abcFromParent = parent.get(Engine);
      var abcFromChild = child.get(Engine);

      expect(complexFromChild, toBe(complexFromParent));
      expect(complexFromChild.engine, toBe(abcFromParent));
      expect(complexFromChild.engine, not(toBe(abcFromChild)));
    });


    it('should force new instance in child even if already instantiated in parent', () {
      var parent = injectorFactory([new Module()..type(Engine)]);
      var abcAlreadyInParent = parent.get(Engine);

      var child = parent.createChild([], forceNewInstances: [Engine]);
      var abcFromChild = child.get(Engine);

      expect(abcFromChild, not(toBe(abcAlreadyInParent)));
    });


    it('should force new instance in child using provider from grand parent', () {
      var module = new Module()..type(Engine, implementedBy: MockEngine);

      var grandParent = injectorFactory([module]);
      var parent = grandParent.createChild([]);
      var child = parent.createChild([], forceNewInstances: [Engine]);

      var abcFromGrandParent = grandParent.get(Engine);
      var abcFromChild = child.get(Engine);

      expect(abcFromChild.id, toEqual(('mock-id')));
      expect(abcFromChild, not(toBe(abcFromGrandParent)));
    });


    it('should provide child injector as Injector', () {
      var injector = injectorFactory([]);
      var child = injector.createChild([]);

      expect(child.get(Injector), toBe(child));
    });


    it('should set the injector name', () {
      var injector = injectorFactory([], 'foo');
      expect(injector.name, 'foo');
    });


    it('should set the child injector name', () {
      var injector = injectorFactory([], 'foo');
      var childInjector = injector.createChild(null, name: 'bar');
      expect(childInjector.name, 'bar');
    });


    it('should instantiate class only once (Issue #18)', () {
      var injector = injectorFactory([
          new Module()
            ..type(Log)
            ..type(ClassOne)
            ..factory(InterfaceOne, (i) => i.get(ClassOne))
      ]);

      expect(injector.get(InterfaceOne), same(injector.get(ClassOne)));
      expect(injector.get(Log).log.join(' '), 'ClassOne');
    });


    describe('creation strategy', () {

      it('should get called for instance creation', () {

        List creationLog = [];
        dynamic creation(Injector requesting, Injector defining, factory) {
          creationLog.add([requesting, defining]);
          return factory();
        }

        var parentModule = new Module()
          ..type(Engine, implementedBy: MockEngine, creation: creation)
          ..type(Car, creation: creation);

        var parentInjector = injectorFactory([parentModule]);
        var childInjector = parentInjector.createChild([]);
        childInjector.get(Car);
        expect(creationLog, [
          [childInjector, parentInjector],
          [childInjector, parentInjector]
        ]);
      });

      it('should be able to prevent instantiation', () {

        List creationLog = [];
        dynamic creation(Injector requesting, Injector defining, factory) {
          throw 'not allowing';
        }

        var module = new Module()
          ..type(Engine, implementedBy: MockEngine, creation: creation);
        var injector = injectorFactory([module]);
        expect(() {
          injector.get(Engine);
        }, throwsA('not allowing'));
      });
    });


    describe('visiblity', () {

      it('should hide instances', () {

        var rootMock = new MockEngine();
        var childMock = new MockEngine();

        var parentModule = new Module()
          ..value(Engine, rootMock);
        var childModule = new Module()
          ..value(Engine, childMock, visibility: (_, __) => false);

        var parentInjector = injectorFactory([parentModule]);
        var childInjector = parentInjector.createChild([childModule]);

        var val = childInjector.get(Engine);
        expect(val, same(rootMock));
      });

      it('should throw when an instance in not visible in the root injector', () {
        var module = new Module()
          ..value(Car, 'Invisible', visibility: (_, __) => false);

        var injector = injectorFactory([module]);

        expect(() {
          injector.get(Car);
        }, toThrow(
            NoProviderError,
            'No provider found for Car! (resolving Car)'
        ));
      });

    });

  });

}

void dynamicInjectorTest() {
  describe('DynamicInjector', () {

    it('should throw a comprehensible error message on untyped argument', () {
      var module = new Module()..type(Lemon)..type(Engine);
      var injector = new DynamicInjector(modules : [module]);

      expect(() {
        injector.get(Lemon);
      }, toThrow(NoProviderError, "The 'engine' parameter must be typed "
          "(resolving Lemon)"));
    });

    it('should throw a comprehensible error message when no default constructor found', () {
      var module = new Module()..type(HiddenConstructor);
      var injector = new DynamicInjector(modules: [module]);

      expect(() {
        injector.get(HiddenConstructor);
      }, toThrow(NoProviderError, startsWith('Unable to find default '
          'constructor for HiddenConstructor. Make sure class has a '
          'default constructor.')));
    });

  });
}

void staticInjectorTest() {
  describe('StaticInjector', () {

    it('should use type factories passed in the constructor', () {
      var module = new Module()
          ..type(Engine);
      var injector = new StaticInjector(modules: [module], typeFactories: {
        Engine: (f) => new Engine()
      });

      var engine;
      expect(() {
        engine = injector.get(Engine);
      }, isNot(throws));
      expect(engine, new isInstanceOf<Engine>());
    });

    it('should use type factories passes in one module', () {
      var module = new Module()
          ..type(Engine)
          ..typeFactories = {
            Engine: (f) => new Engine()
          };
      var injector = new StaticInjector(modules: [module]);

      var engine;
      expect(() {
        engine = injector.get(Engine);
      }, isNot(throws));
      expect(engine, new isInstanceOf<Engine>());
    });

    it('should use type factories passes in many modules', () {
      var module1 = new Module()
          ..type(Engine)
          ..typeFactories = {
            Engine: (f) => new Engine()
          };
      var module2 = new Module()
          ..type(Car)
          ..typeFactories = {
            Car: (f) => new Car(f(Engine), f(Injector))
          };

      var injector = new StaticInjector(modules: [module1, module2]);

      var engine;
      expect(() {
        engine = injector.get(Car);
      }, isNot(throws));
      expect(engine, new isInstanceOf<Car>());
    });

    it('should use type factories passes in hierarchical module', () {
      var module = new Module()
          ..type(Engine)
          ..typeFactories = {
            Engine: (f) => new Engine()
          };

      module.install(new Module()
         ..type(Car)
         ..typeFactories = {
            Car: (f) => new Car(f(Engine), f(Injector))
         });

      var injector = new StaticInjector(modules: [module]);

      var engine;
      expect(() {
        engine = injector.get(Car);
      }, isNot(throws));
      expect(engine, new isInstanceOf<Car>());
    });

    it('should find type factories from parent injector', () {
      var module1 = new Module()
          ..type(Engine)
          ..typeFactories = {
            Engine: (f) => new Engine()
          };
      var module2 = new Module()
          ..type(Car)
          ..typeFactories = {
            Car: (f) => new Car(f(Engine), f(Injector))
          };

      var rootInjector = new StaticInjector(modules: [module1]);
      var childInjector = rootInjector.createChild([module2]);

      expect(() {
        rootInjector.get(Car);
      }, throws);

      var engine;
      expect(() {
        engine = childInjector.get(Car);
      }, isNot(throws));
      expect(engine, new isInstanceOf<Car>());
    });

  });
}
