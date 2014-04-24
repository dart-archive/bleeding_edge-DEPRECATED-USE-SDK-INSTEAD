library di.test.injector_generator_spec;

import 'dart:async';

import 'package:barback/barback.dart';
import 'package:code_transformers/resolver.dart';
import 'package:code_transformers/tests.dart' as tests;
import 'package:di/transformer/injector_generator.dart';
import 'package:di/transformer/options.dart';

import 'fixed-unittest.dart';

main() {
  describe('generator', () {
    var injectableAnnotations = [];
    var options = new TransformOptions(
        injectableAnnotations: injectableAnnotations,
        sdkDirectory: dartSdkDirectory);

    var resolvers = new Resolvers(dartSdkDirectory);

    var phases = [
      [new InjectorGenerator(options, resolvers)]
    ];

    it('transforms imports', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': 'import "package:a/car.dart"; main() {}',
            'a|lib/car.dart': '''
                import 'package:inject/inject.dart';
                import 'package:a/engine.dart';
                import 'package:a/seat.dart' as seat;

                class Car {
                  @inject
                  Car(Engine e, seat.Seat s) {}
                }
                ''',
            'a|lib/engine.dart': CLASS_ENGINE,
            'a|lib/seat.dart': '''
                import 'package:inject/inject.dart';
                class Seat {
                  @inject
                  Seat();
                }
                ''',
          },
          imports: [
            "import 'package:a/car.dart' as import_0;",
            "import 'package:a/engine.dart' as import_1;",
            "import 'package:a/seat.dart' as import_2;",
          ],
          generators: [
            'import_0.Car: (f) => new import_0.Car(f(import_1.Engine), '
                'f(import_2.Seat)),',
            'import_1.Engine: (f) => new import_1.Engine(),',
            'import_2.Seat: (f) => new import_2.Seat(),',
          ]);
    });

    it('warns about parameterized classes', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': 'import "package:a/a.dart"; main() {}',
            'a|lib/a.dart': '''
                import 'package:inject/inject.dart';
                class Parameterized<T> {
                  @inject
                  Parameterized();
                }
                '''
          },
          imports: [
            "import 'package:a/a.dart' as import_0;",
          ],
          generators: [
            'import_0.Parameterized: (f) => new import_0.Parameterized(),',
          ],
          messages: [
            'warning: Parameterized is a parameterized type. '
            '(package:a/a.dart 1 16)',
          ]);
    });

    it('skips and warns about parameterized constructor parameters', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': 'import "package:a/a.dart"; main() {}',
            'a|lib/a.dart': '''
                import 'package:inject/inject.dart';
                class Foo<T> {}
                class Bar {
                  @inject
                  Bar(Foo<bool> f);
                }
                '''
          },
          messages: [
            'warning: Bar cannot be injected because Foo<bool> is a '
            'parameterized type. (package:a/a.dart 3 18)'
          ]);
    });

    it('allows un-parameterized parameters', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': '''
                import 'package:inject/inject.dart';
                class Foo<T> {}
                class Bar {
                  @inject
                  Bar(Foo f);
                }
                main() {}
                '''
          },
          imports: [
            "import 'main.dart' as import_0;",
          ],
          generators: [
            'import_0.Bar: (f) => new import_0.Bar(f(import_0.Foo)),',
          ]);
    });

    it('follows exports', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': 'import "package:a/a.dart"; main() {}',
            'a|lib/a.dart': 'export "package:a/b.dart";',
            'a|lib/b.dart': CLASS_ENGINE
          },
          imports: [
            "import 'package:a/b.dart' as import_0;",
          ],
          generators: [
            'import_0.Engine: (f) => new import_0.Engine(),',
          ]);
    });

    it('handles parts', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': 'import "package:a/a.dart"; main() {}',
            'a|lib/a.dart':
                'import "package:inject/inject.dart";\n'
                'part "b.dart";',
            'a|lib/b.dart': '''
                part of a.a;
                class Engine {
                  @inject
                  Engine();
                }
                '''
          },
          imports: [
            "import 'package:a/a.dart' as import_0;",
          ],
          generators: [
            'import_0.Engine: (f) => new import_0.Engine(),',
          ]);
    });

    it('follows relative imports', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': 'import "package:a/a.dart"; main() {}',
            'a|lib/a.dart': 'import "b.dart";',
            'a|lib/b.dart': CLASS_ENGINE
          },
          imports: [
            "import 'package:a/b.dart' as import_0;",
          ],
          generators: [
            'import_0.Engine: (f) => new import_0.Engine(),',
          ]);
    });

    it('handles relative imports', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': 'import "package:a/a.dart"; main() {}',
            'a|lib/a.dart': '''
                import "package:inject/inject.dart";
                import 'b.dart';
                class Car {
                  @inject
                  Car(Engine engine);
                }
                ''',
            'a|lib/b.dart': CLASS_ENGINE
          },
          imports: [
            "import 'package:a/a.dart' as import_0;",
            "import 'package:a/b.dart' as import_1;",
          ],
          generators: [
            'import_0.Car: (f) => new import_0.Car(f(import_1.Engine)),',
            'import_1.Engine: (f) => new import_1.Engine(),',
          ]);
    });

    it('handles web imports beside main', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': 'import "a.dart"; main() {}',
            'a|web/a.dart': CLASS_ENGINE
          },
          imports: [
            "import 'a.dart' as import_0;",
          ],
          generators: [
            'import_0.Engine: (f) => new import_0.Engine(),',
          ]);
    });

    it('handles imports in main', () {
      return generates(phases,
          inputs: {
            'a|web/main.dart': '''
                $CLASS_ENGINE
                main() {}
                '''
          },
          imports: [
            "import 'main.dart' as import_0;",
          ],
          generators: [
            'import_0.Engine: (f) => new import_0.Engine(),',
          ]);
    });

    it('skips and warns on named constructors', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  class Engine {
                    @inject
                    Engine.foo();
                  }

                  main() {}
                  '''
            },
            messages: ['warning: Named constructors cannot be injected. '
                '(web/main.dart 2 20)']);
      });

      it('handles inject on classes', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  @inject
                  class Engine {}

                  main() {}
                  '''
            },
            imports: [
            "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => new import_0.Engine(),',
            ]);
      });

      it('skips and warns when no default constructor', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  @inject
                  class Engine {
                    Engine.foo();
                  }
                  main() {}
                  '''
            },
            messages: ['warning: Engine cannot be injected because it does not '
                'have a default constructor. (web/main.dart 1 18)']);
      });

      it('skips and warns on abstract types with no factory constructor', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  @inject
                  abstract class Engine { }

                  main() {}
                  '''
            },
            messages: ['warning: Engine cannot be injected because it is an '
                'abstract type with no factory constructor. '
                '(web/main.dart 1 18)']);
      });

      it('skips and warns on abstract types with implicit constructor', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  @inject
                  abstract class Engine {
                    Engine();
                  }
                  main() {}
                  '''
            },
            messages: ['warning: Engine cannot be injected because it is an '
                'abstract type with no factory constructor. '
                '(web/main.dart 1 18)']);
      });

      it('injects abstract types with factory constructors', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  @inject
                  abstract class Engine {
                    factory Engine() => new ConcreteEngine();
                  }

                  class ConcreteEngine implements Engine {}

                  main() {}
                  '''
            },
            imports: [
              "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => new import_0.Engine(),',
            ]);
      });

      it('injects this parameters', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  class Engine {
                    final Fuel fuel;
                    @inject
                    Engine(this.fuel);
                  }

                  class Fuel {}

                  main() {}
                  '''
            },
            imports: [
              "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => new import_0.Engine(f(import_0.Fuel)),',
            ]);
      });

      it('narrows this parameters', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  class Engine {
                    final Fuel fuel;
                    @inject
                    Engine(JetFuel this.fuel);
                  }

                  class Fuel {}
                  class JetFuel implements Fuel {}

                  main() {}
                  '''
            },
            imports: [
              "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => '
                  'new import_0.Engine(f(import_0.JetFuel)),',
            ]);
      });

      it('skips and warns on unresolved types', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  @inject
                  class Engine {
                    Engine(foo);
                  }

                  @inject
                  class Car {
                    var foo;
                    Car(this.foo);
                  }

                  main() {}
                  '''
            },
            messages: ['warning: Engine cannot be injected because parameter '
                'type foo cannot be resolved. (web/main.dart 3 20)',
                'warning: Car cannot be injected because parameter type '
                'foo cannot be resolved. (web/main.dart 9 20)']);
      });

      it('supports custom annotations', () {
        injectableAnnotations.add('angular.NgInjectableService');
        return generates(phases,
            inputs: {
              'angular|lib/angular.dart': PACKAGE_ANGULAR,
              'a|web/main.dart': '''
                  import 'package:angular/angular.dart';
                  @NgInjectableService()
                  class Engine {
                    Engine();
                  }

                  class Car {
                    @NgInjectableService()
                    Car();
                  }

                  main() {}
                  '''
            },
            imports: [
              "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => new import_0.Engine(),',
              'import_0.Car: (f) => new import_0.Car(),',
            ]).whenComplete(() {
              injectableAnnotations.clear();
            });
      });

      it('supports default formal parameters', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  class Engine {
                    final Car car;

                    @inject
                    Engine([Car this.car]);
                  }

                  class Car {
                    @inject
                    Car();
                  }

                  main() {}
                  '''
            },
            imports: [
              "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => new import_0.Engine(f(import_0.Car)),',
              'import_0.Car: (f) => new import_0.Car(),',
            ]);
      });

      it('supports injectableTypes argument', () {
        return generates(phases,
            inputs: {
              'di|lib/annotations.dart': PACKAGE_DI,
              'a|web/main.dart': '''
                  @Injectables(const[Engine])
                  library a;

                  import 'package:di/annotations.dart';

                  class Engine {
                    Engine();
                  }

                  main() {}
                  '''
            },
            imports: [
              "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => new import_0.Engine(),',
            ]);
      });

      it('does not generate dart:core imports', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import 'package:inject/inject.dart';

                  class Engine {
                    @inject
                    Engine(int i);
                  }
                  main() {}
                  '''
            },
            imports: [
              "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => new import_0.Engine(f(int)),',
            ]);
      });

      it('warns on private types', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";
                  @inject
                  class _Engine {
                    _Engine();
                  }

                  main() {}
                  '''
            },
            messages: ['warning: _Engine cannot be injected because it is a '
                'private type. (web/main.dart 1 18)']);
      });

      it('warns on multiple constructors', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";

                  @inject
                  class Engine {
                    Engine();

                    @inject
                    Engine.foo();
                  }

                  main() {}
                  '''
            },
            messages: ['warning: Engine has more than one constructor '
                'annotated for injection. (web/main.dart 2 18)']);
      });

      it('transforms main', () {
        return tests.applyTransformers(phases,
            inputs: {
              'a|web/main.dart': '''
library main;
import 'package:di/auto_injector.dart';
import 'package:di/auto_injector.dart' as ai;

main() {
  var module = defaultInjector(modules: null, name: 'foo');
  print(module);

  var module2 = ai.defaultInjector(modules: null, name: 'foo');
  print(module2);
}''',
              'di|lib/auto_injector.dart': PACKAGE_AUTO
            },
            results: {
              'a|web/main.dart': '''
library main;
import 'main_static_injector.dart' as generated_static_injector;
import 'package:di/auto_injector.dart';
import 'package:di/auto_injector.dart' as ai;

main() {
  var module = generated_static_injector.createStaticInjector(modules: null, name: 'foo');
  print(module);

  var module2 = generated_static_injector.createStaticInjector(modules: null, name: 'foo');
  print(module2);
}'''

            });
      });

      it('handles annotated dependencies', () {
        return generates(phases,
            inputs: {
              'a|web/main.dart': '''
                  import "package:inject/inject.dart";

                  class Turbo {
                    const Turbo();
                  }

                  @inject
                  class Engine {}

                  @inject
                  class Car {
                    Car(@Turbo() Engine engine);
                  }

                  main() {}
                  '''
            },
            imports: [
              "import 'main.dart' as import_0;",
            ],
            generators: [
              'import_0.Engine: (f) => new import_0.Engine(),',
              'import_0.Car: (f) => new import_0.Car(f(import_0.Engine, import_0.Turbo)),',
            ]);
      });
  });
}

Future generates(List<List<Transformer>> phases,
    {Map<String, String> inputs, Iterable<String> imports: const [],
    Iterable<String> generators: const [],
    Iterable<String> messages: const []}) {

  inputs['inject|lib/inject.dart'] = PACKAGE_INJECT;

  imports = imports.map((i) => '$i\n');
  generators = generators.map((t) => '  $t\n');

  return tests.applyTransformers(phases,
      inputs: inputs,
      results: {
          'a|web/main_static_injector.dart': '''
$IMPORTS
${imports.join('')}$BOILER_PLATE
${generators.join('')}$FOOTER
''',
      },
      messages: messages);
}

const String IMPORTS = '''
library a.web.main.generated_static_injector;

import 'package:di/di.dart';
import 'package:di/static_injector.dart';
''';

const String BOILER_PLATE = '''
Injector createStaticInjector({List<Module> modules, String name,
    bool allowImplicitInjection: false}) =>
  new StaticInjector(modules: modules, name: name,
      allowImplicitInjection: allowImplicitInjection,
      typeFactories: factories);

final Map<Type, TypeFactory> factories = <Type, TypeFactory>{''';

const String FOOTER = '''
};''';

const String CLASS_ENGINE = '''
    import 'package:inject/inject.dart';
    class Engine {
      @inject
      Engine();
    }''';

const String PACKAGE_ANGULAR = '''
library angular;

class NgInjectableService {
  const NgInjectableService();
}
''';

const String PACKAGE_INJECT = '''
library inject;

class InjectAnnotation {
  const InjectAnnotation._();
}
const inject = const InjectAnnotation._();
''';

const String PACKAGE_DI = '''
library di.annotations;

class Injectables {
  final List<Type> types;
  const Injectables(this.types);
}
''';

const String PACKAGE_AUTO = '''
library di.auto_injector;

defaultInjector({List modules, String name,
    bool allowImplicitInjection: false}) => null;
''';
