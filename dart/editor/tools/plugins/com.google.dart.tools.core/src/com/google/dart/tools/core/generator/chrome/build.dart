import "dart:io";

final List<Path> WATCHED_FILES = [
  new Path.raw("web/background.dart"),
  new Path.raw("web/{name.lower}.dart")
];

bool get isWindows => Platform.operatingSystem == 'windows';
Path get sdkBinPath => new Path(new Options().executable).directoryPath;
Path get dart2jsPath => sdkBinPath.append(isWindows ? 'dart2js.bat' : 'dart2js');

/// This quick and dirty build script watches for changes to any .dart files
/// and re-compiles {name.lower}.dart using dart2js. The --disallow-unsafe-eval
/// flag causes dart2js to output CSP (and Chrome app) friendly code.
void main() {
  var args = new Options().arguments;

  bool fullBuild = args.contains("--full");
  bool dartFilesChanged = args.any(
      (arg) => arg.startsWith("--changed=") && arg.endsWith(".dart"));

  if (fullBuild || dartFilesChanged) {
    for (Path path in WATCHED_FILES) {
      callDart2js(path.toNativePath());
    }
  }
}

void callDart2js(String path) {
  print("dart2js --disallow-unsafe-eval ${path}");

  Process.run(dart2jsPath.toNativePath(),
    ['--disallow-unsafe-eval', '-o${path}.js', path]
  ).then((result) {
    if (result.stdout.length > 0) {
      print("${result.stdout.replaceAll('\r\n', '\n')}");
    }

    if (result.exitCode != 0) {
      exit(result.exitCode);
    }
  });
}
