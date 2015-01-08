Pub is currently dogfooding the new Dart async/await syntax. Since the Dart VM
doesn't natively support it yet, we are using the [async-await][] compiler
package.

[async-await]: https://github.com/dart-lang/async_await

We run that to compile pub-using-await from sdk/lib/_internal/pub down to
vanilla Dart code which is what you see here. To interoperate more easily with
the rest of the repositry, we check in that generated code.

When bug #104 is fixed, we can remove this entirely.

The code here was compiled using the async-await compiler at commit:

    04e540562a04380a3e20a422c8bfe306e6f79f95

(Note: this file is also parsed by a tool to update the above commit, so be
careful not to reformat it.)
