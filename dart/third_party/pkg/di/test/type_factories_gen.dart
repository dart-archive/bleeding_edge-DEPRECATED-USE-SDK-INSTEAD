library di.generated.type_factories;
import "main.dart" as import_0;
import "package:di/di.dart" as import_2;
import "test_annotations.dart" as import_6;
var typeFactories = {
import_0.ClassOne: (f) => new import_0.ClassOne(f(import_0.Log)),
import_0.CircularA: (f) => new import_0.CircularA(f(import_0.CircularB)),
import_0.CircularB: (f) => new import_0.CircularB(f(import_0.CircularA)),
import_0.MultipleConstructors: (f) => new import_0.MultipleConstructors(),
import_0.NumDependency: (f) => new import_0.NumDependency(f(num)),
import_0.IntDependency: (f) => new import_0.IntDependency(f(int)),
import_0.DoubleDependency: (f) => new import_0.DoubleDependency(f(double)),
import_0.BoolDependency: (f) => new import_0.BoolDependency(f(bool)),
import_0.StringDependency: (f) => new import_0.StringDependency(f(String)),
import_0.Engine: (f) => new import_0.Engine(),
import_0.MockEngine: (f) => new import_0.MockEngine(),
import_0.MockEngine2: (f) => new import_0.MockEngine2(),
import_0.TurboEngine: (f) => new import_0.TurboEngine(),
import_0.BrokenOldEngine: (f) => new import_0.BrokenOldEngine(),
import_0.Car: (f) => new import_0.Car(f(import_0.Engine), f(import_2.Injector)),
import_0.Porsche: (f) => new import_0.Porsche(f(import_0.Engine, import_6.Turbo), f(import_2.Injector)),
import_0.ParameterizedType: (f) => new import_0.ParameterizedType(),
import_0.GenericParameterizedDependency: (f) => new import_0.GenericParameterizedDependency(f(import_0.ParameterizedType)),
import_0.Log: (f) => new import_0.Log(),
import_0.AnnotatedPrimitiveDependency: (f) => new import_0.AnnotatedPrimitiveDependency(f(String, import_6.Turbo)),
import_0.ThrowOnce: (f) => new import_0.ThrowOnce(),

};
main() {}
