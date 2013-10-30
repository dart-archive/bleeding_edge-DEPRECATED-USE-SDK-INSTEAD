import "dart:io";

/**
 * This build script watches for changes to any .dart files and copies the root
 * packages directory to the app/packages directory. This works around an issue
 * with Chrome apps and symlinks and allows you to use pub with Chrome apps.
 */
void main(List<String> args) {
  bool fullBuild = args.contains("--full");
  bool dartFilesChanged = args.any((arg) {
    return !arg.startsWith("--changed=app/packages") && arg.endsWith(".dart");
  });

  if (fullBuild || dartFilesChanged || args.isEmpty) {
    copyDirectory(new Uri.file('packages/'), new Uri.file('app/packages/'));
  }
}

void copyDirectory(Uri src, Uri dest) {
  Directory srcDir = new Directory(src.toFilePath());

  for (FileSystemEntity entity in srcDir.listSync()) {
    String name = new Uri.file(entity.path).pathSegments.last;

    if (entity is File) {
      copyFile(src.resolve(name), dest);
    } else {
      copyDirectory(src.resolve("$name/"), dest.resolve("$name/"));
    }
  }
}

void copyFile(Uri src, Uri dest) {
  File srcFile = new File(src.toFilePath());
  File destFile = new File(dest.resolve(src.pathSegments.last).toFilePath());

  if (!destFile.existsSync() ||
      srcFile.lastModifiedSync() != destFile.lastModifiedSync()) {
    new Directory(dest.toFilePath()).createSync(recursive: true);

    destFile.writeAsBytesSync(srcFile.readAsBytesSync());
  }
}
