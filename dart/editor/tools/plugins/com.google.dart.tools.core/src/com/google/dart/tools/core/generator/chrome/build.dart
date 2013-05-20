import "dart:io";

/**
 * This build script watches for changes to any .dart files and copies the root
 * packages directory to the app/packages directory. This works around an issue
 * with Chrome apps and symlinks and allows you to use pub with Chrome apps.
 */
void main() {
  List<String> args = new Options().arguments;

  bool fullBuild = args.contains("--full");
  bool dartFilesChanged = args.any((arg) {
    return !arg.startsWith("--changed=app/packages") && arg.endsWith(".dart");
  });

  if (fullBuild || dartFilesChanged || args.isEmpty) {
    copyDirectory(directory('packages'), directory('app/packages'));
  }
}

Path directory(String path) => new Path(path);

void copyDirectory(Path srcDirPath, Path destDirPath) {
  Directory srcDir = new Directory.fromPath(srcDirPath);
  
  for (FileSystemEntity entity in srcDir.listSync()) {
    String name = new Path(entity.path).filename;
    
    if (entity is File) {
      copyFile(srcDirPath.join(new Path(name)), destDirPath);
    } else {
      copyDirectory(
          srcDirPath.join(new Path(name)),
          destDirPath.join(new Path(name)));
    }
  }
}

void copyFile(Path srcFilePath, Path destDirPath) {
  File srcFile = new File.fromPath(srcFilePath);
  File destFile = new File.fromPath(
      destDirPath.join(new Path(srcFilePath.filename)));
  
  if (!destFile.existsSync() || srcFile.lastModifiedSync() != destFile.lastModifiedSync()) {
    new Directory.fromPath(destDirPath).createSync(recursive: true);
    
    destFile.writeAsBytesSync(srcFile.readAsBytesSync());
  }
}
