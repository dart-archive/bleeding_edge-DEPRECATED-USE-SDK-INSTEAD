library di.generated.type_factories;
import "/usr/local/google/home/blois/src/dart/dart/third_party/pkg/di/test/main.dart" as import_0;
import "package:di/di.dart" as import_2;
var typeFactories = new Map();
main() {
typeFactories[import_0.ClassOne] = (f) => new import_0.ClassOne(f(import_0.Log));
typeFactories[import_0.CircularA] = (f) => new import_0.CircularA(f(import_0.CircularB));
typeFactories[import_0.CircularB] = (f) => new import_0.CircularB(f(import_0.CircularA));
typeFactories[import_0.MultipleConstructors] = (f) => new import_0.MultipleConstructors();
typeFactories[import_0.NumDependency] = (f) => new import_0.NumDependency(f(num));
typeFactories[import_0.IntDependency] = (f) => new import_0.IntDependency(f(int));
typeFactories[import_0.DoubleDependency] = (f) => new import_0.DoubleDependency(f(double));
typeFactories[import_0.BoolDependency] = (f) => new import_0.BoolDependency(f(bool));
typeFactories[import_0.StringDependency] = (f) => new import_0.StringDependency(f(String));
typeFactories[import_0.Engine] = (f) => new import_0.Engine();
typeFactories[import_0.MockEngine] = (f) => new import_0.MockEngine();
typeFactories[import_0.MockEngine2] = (f) => new import_0.MockEngine2();
typeFactories[import_0.Car] = (f) => new import_0.Car(f(import_0.Engine), f(import_2.Injector));
typeFactories[import_0.Log] = (f) => new import_0.Log();
}
