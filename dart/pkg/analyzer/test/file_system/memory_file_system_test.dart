// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.memory_file_system;

import 'dart:async';

import 'package:analyzer/file_system/file_system.dart';
import 'package:analyzer/file_system/memory_file_system.dart';
import 'package:analyzer/src/generated/engine.dart' show TimestampedData;
import 'package:analyzer/src/generated/source.dart';
import 'package:path/path.dart';
import 'package:unittest/unittest.dart';
import 'package:watcher/watcher.dart';


var _isFile = new isInstanceOf<File>();
var _isFolder = new isInstanceOf<Folder>();
var _isFileSystemException = new isInstanceOf<FileSystemException>();


main() {
  groupSep = ' | ';

  group('MemoryResourceProvider', () {
    MemoryResourceProvider provider;

    setUp(() {
      provider = new MemoryResourceProvider();
    });

    test('FileSystemException', () {
      var exception = new FileSystemException('/my/path', 'my message');
      expect(exception.path, '/my/path');
      expect(exception.message, 'my message');
      expect(
          exception.toString(),
          'FileSystemException(path=/my/path; message=my message)');
    });

    group('Watch', () {

      Future delayed(computation()) {
        return new Future.delayed(Duration.ZERO, computation);
      }

      watchingFolder(String path, test(List<WatchEvent> changesReceived)) {
        Folder folder = provider.getResource(path);
        var changesReceived = <WatchEvent>[];
        folder.changes.listen(changesReceived.add);
        return test(changesReceived);
      }

      test('create file', () {
        String rootPath = '/my/path';
        provider.newFolder(rootPath);
        watchingFolder(rootPath, (changesReceived) {
          expect(changesReceived, hasLength(0));
          String path = posix.join(rootPath, 'foo');
          provider.newFile(path, 'contents');
          return delayed(() {
            expect(changesReceived, hasLength(1));
            expect(changesReceived[0].type, equals(ChangeType.ADD));
            expect(changesReceived[0].path, equals(path));
          });
        });
      });

      test('modify file', () {
        String rootPath = '/my/path';
        provider.newFolder(rootPath);
        String path = posix.join(rootPath, 'foo');
        provider.newFile(path, 'contents 1');
        return watchingFolder(rootPath, (changesReceived) {
          expect(changesReceived, hasLength(0));
          provider.modifyFile(path, 'contents 2');
          return delayed(() {
            expect(changesReceived, hasLength(1));
            expect(changesReceived[0].type, equals(ChangeType.MODIFY));
            expect(changesReceived[0].path, equals(path));
          });
        });
      });

      test('modify file in subdir', () {
        String rootPath = '/my/path';
        provider.newFolder(rootPath);
        String subdirPath = posix.join(rootPath, 'foo');
        provider.newFolder(subdirPath);
        String path = posix.join(rootPath, 'bar');
        provider.newFile(path, 'contents 1');
        return watchingFolder(rootPath, (changesReceived) {
          expect(changesReceived, hasLength(0));
          provider.modifyFile(path, 'contents 2');
          return delayed(() {
            expect(changesReceived, hasLength(1));
            expect(changesReceived[0].type, equals(ChangeType.MODIFY));
            expect(changesReceived[0].path, equals(path));
          });
        });
      });

      test('delete file', () {
        String rootPath = '/my/path';
        provider.newFolder(rootPath);
        String path = posix.join(rootPath, 'foo');
        provider.newFile(path, 'contents 1');
        return watchingFolder(rootPath, (changesReceived) {
          expect(changesReceived, hasLength(0));
          provider.deleteFile(path);
          return delayed(() {
            expect(changesReceived, hasLength(1));
            expect(changesReceived[0].type, equals(ChangeType.REMOVE));
            expect(changesReceived[0].path, equals(path));
          });
        });
      });
    });

    group('getStateLocation', () {
      test('uniqueness', () {
        String idOne = 'one';
        Folder folderOne = provider.getStateLocation(idOne);
        expect(folderOne, isNotNull);
        String idTwo = 'two';
        Folder folderTwo = provider.getStateLocation(idTwo);
        expect(folderTwo, isNotNull);
        expect(folderTwo, isNot(equals(folderOne)));
        expect(provider.getStateLocation(idOne), equals(folderOne));
      });
    });

    group('newFolder', () {
      test('empty path', () {
        expect(() {
          provider.newFolder('');
        }, throwsA(new isInstanceOf<ArgumentError>()));
      });

      test('not absolute', () {
        expect(() {
          provider.newFolder('not/absolute');
        }, throwsA(new isInstanceOf<ArgumentError>()));
      });

      group('already exists', () {
        test('as folder', () {
          Folder folder = provider.newFolder('/my/folder');
          Folder newFolder = provider.newFolder('/my/folder');
          expect(newFolder, folder);
        });

        test('as file', () {
          provider.newFile('/my/file', 'qwerty');
          expect(() {
            provider.newFolder('/my/file');
          }, throwsA(new isInstanceOf<ArgumentError>()));
        });
      });
    });

    group('modifyFile', () {
      test('nonexistent', () {
        String path = '/my/file';
        expect(() {
          provider.modifyFile(path, 'contents');
        }, throwsA(new isInstanceOf<ArgumentError>()));
        Resource file = provider.getResource(path);
        expect(file, isNotNull);
        expect(file.exists, isFalse);
      });

      test('is folder', () {
        String path = '/my/file';
        provider.newFolder(path);
        expect(() {
          provider.modifyFile(path, 'contents');
        }, throwsA(new isInstanceOf<ArgumentError>()));
        expect(provider.getResource(path), new isInstanceOf<Folder>());
      });

      test('successful', () {
        String path = '/my/file';
        provider.newFile(path, 'contents 1');
        Resource file = provider.getResource(path);
        expect(file, new isInstanceOf<File>());
        Source source = (file as File).createSource();
        expect(source.contents.data, equals('contents 1'));
        provider.modifyFile(path, 'contents 2');
        expect(source.contents.data, equals('contents 2'));
      });
    });

    group('deleteFile', () {
      test('nonexistent', () {
        String path = '/my/file';
        expect(() {
          provider.deleteFile(path);
        }, throwsA(new isInstanceOf<ArgumentError>()));
        Resource file = provider.getResource(path);
        expect(file, isNotNull);
        expect(file.exists, isFalse);
      });

      test('is folder', () {
        String path = '/my/file';
        provider.newFolder(path);
        expect(() {
          provider.deleteFile(path);
        }, throwsA(new isInstanceOf<ArgumentError>()));
        expect(provider.getResource(path), new isInstanceOf<Folder>());
      });

      test('successful', () {
        String path = '/my/file';
        provider.newFile(path, 'contents');
        Resource file = provider.getResource(path);
        expect(file, new isInstanceOf<File>());
        expect(file.exists, isTrue);
        provider.deleteFile(path);
        expect(file.exists, isFalse);
      });
    });

    group('File', () {
      group('==', () {
        test('false', () {
          File fileA = provider.getResource('/fileA.txt');
          File fileB = provider.getResource('/fileB.txt');
          expect(fileA == new Object(), isFalse);
          expect(fileA == fileB, isFalse);
        });

        test('true', () {
          File file = provider.getResource('/file.txt');
          expect(file == file, isTrue);
        });

        test('before and after creation', () {
          String path = '/file.txt';
          File file1 = provider.getResource(path);
          provider.newFile(path, 'contents');
          File file2 = provider.getResource(path);
          expect(file1 == file2, isTrue);
        });
      });

      group('exists', () {
        test('false', () {
          File file = provider.getResource('/file.txt');
          expect(file, isNotNull);
          expect(file.exists, isFalse);
        });

        test('true', () {
          provider.newFile('/foo/file.txt', 'qwerty');
          File file = provider.getResource('/foo/file.txt');
          expect(file, isNotNull);
          expect(file.exists, isTrue);
        });
      });

      test('fullName', () {
        File file = provider.getResource('/foo/bar/file.txt');
        expect(file.path, '/foo/bar/file.txt');
      });

      test('hashCode', () {
        String path = '/foo/bar/file.txt';
        File file1 = provider.getResource(path);
        provider.newFile(path, 'contents');
        File file2 = provider.getResource(path);
        expect(file1.hashCode, equals(file2.hashCode));
      });

      test('isOrContains', () {
        String path = '/foo/bar/file.txt';
        File file = provider.getResource(path);
        expect(file.isOrContains(path), isTrue);
        expect(file.isOrContains('/foo/bar'), isFalse);
      });

      group('modificationStamp', () {
        test('exists', () {
          String path = '/foo/bar/file.txt';
          File file = provider.newFile(path, 'qwerty');
          expect(file.modificationStamp, isNonNegative);
        });

        test('does not exist', () {
          String path = '/foo/bar/file.txt';
          File file = provider.newFile(path, 'qwerty');
          provider.deleteFile(path);
          expect(() {
            file.modificationStamp;
          }, throwsA(_isFileSystemException));
        });
      });

      test('shortName', () {
        File file = provider.getResource('/foo/bar/file.txt');
        expect(file.shortName, 'file.txt');
      });

      test('toString', () {
        File file = provider.getResource('/foo/bar/file.txt');
        expect(file.toString(), '/foo/bar/file.txt');
      });

      test('parent', () {
        provider.newFile('/foo/bar/file.txt', 'content');
        File file = provider.getResource('/foo/bar/file.txt');
        Resource parent = file.parent;
        expect(parent, new isInstanceOf<Folder>());
        expect(parent.path, equals('/foo/bar'));
      });
    });

    group('Folder', () {
      Folder folder;
      const String path = '/foo/bar';

      setUp(() {
        folder = provider.newFolder(path);
      });

      test('hashCode', () {
        Folder folder2 = provider.getResource(path);
        expect(folder.hashCode, equals(folder2.hashCode));
      });

      test('equality: same path', () {
        Folder folder2 = provider.getResource(path);
        expect(folder == folder2, isTrue);
      });

      test('equality: different paths', () {
        String path2 = '/foo/baz';
        Folder folder2 = provider.newFolder(path2);
        expect(folder == folder2, isFalse);
      });

      test('contains', () {
        expect(folder.contains('/foo/bar/aaa.txt'), isTrue);
        expect(folder.contains('/foo/bar/aaa/bbb.txt'), isTrue);
        expect(folder.contains('/baz.txt'), isFalse);
        expect(folder.contains('/foo/bar'), isFalse);
      });

      test('isOrContains', () {
        expect(folder.isOrContains('/foo/bar'), isTrue);
        expect(folder.isOrContains('/foo/bar/aaa.txt'), isTrue);
        expect(folder.isOrContains('/foo/bar/aaa/bbb.txt'), isTrue);
        expect(folder.isOrContains('/baz.txt'), isFalse);
      });

      group('getChild', () {
        test('does not exist', () {
          File file = folder.getChild('file.txt');
          expect(file, isNotNull);
          expect(file.exists, isFalse);
        });

        test('file', () {
          provider.newFile('/foo/bar/file.txt', 'content');
          File child = folder.getChild('file.txt');
          expect(child, isNotNull);
          expect(child.exists, isTrue);
        });

        test('folder', () {
          provider.newFolder('/foo/bar/baz');
          Folder child = folder.getChild('baz');
          expect(child, isNotNull);
          expect(child.exists, isTrue);
        });
      });

      test('getChildren', () {
        provider.newFile('/foo/bar/a.txt', 'aaa');
        provider.newFolder('/foo/bar/bFolder');
        provider.newFile('/foo/bar/c.txt', 'ccc');
        // prepare 3 children
        List<Resource> children = folder.getChildren();
        expect(children, hasLength(3));
        children.sort((a, b) => a.shortName.compareTo(b.shortName));
        // check that each child exists
        children.forEach((child) {
          expect(child.exists, true);
        });
        // check names
        expect(children[0].shortName, 'a.txt');
        expect(children[1].shortName, 'bFolder');
        expect(children[2].shortName, 'c.txt');
        // check types
        expect(children[0], _isFile);
        expect(children[1], _isFolder);
        expect(children[2], _isFile);
      });

      test('parent', () {
        Resource parent1 = folder.parent;
        expect(parent1, new isInstanceOf<Folder>());
        expect(parent1.path, equals('/foo'));
        Resource parent2 = parent1.parent;
        expect(parent2, new isInstanceOf<Folder>());
        expect(parent2.path, equals('/'));
        expect(parent2.parent, isNull);
      });

      test('canonicalizePath', () {
        expect(folder.canonicalizePath('baz'), equals('/foo/bar/baz'));
        expect(folder.canonicalizePath('/baz'), equals('/baz'));
        expect(folder.canonicalizePath('../baz'), equals('/foo/baz'));
        expect(folder.canonicalizePath('/a/b/../c'), equals('/a/c'));
        expect(folder.canonicalizePath('./baz'), equals('/foo/bar/baz'));
        expect(folder.canonicalizePath('/a/b/./c'), equals('/a/b/c'));
      });
    });

    group('_MemoryFileSource', () {
      Source source;

      group('existent', () {
        setUp(() {
          File file = provider.newFile('/foo/test.dart', 'library test;');
          source = file.createSource();
        });

        group('==', () {
          group('true', () {
            test('self', () {
              File file = provider.newFile('/foo/test.dart', '');
              Source source = file.createSource();
              expect(source == source, isTrue);
            });

            test('same file', () {
              File file = provider.newFile('/foo/test.dart', '');
              Source sourceA = file.createSource();
              Source sourceB = file.createSource();
              expect(sourceA == sourceB, isTrue);
            });
          });

          group('false', () {
            test('not a memory Source', () {
              File file = provider.newFile('/foo/test.dart', '');
              Source source = file.createSource();
              expect(source == new Object(), isFalse);
            });

            test('different file', () {
              File fileA = provider.newFile('/foo/a.dart', '');
              File fileB = provider.newFile('/foo/b.dart', '');
              Source sourceA = fileA.createSource();
              Source sourceB = fileB.createSource();
              expect(sourceA == sourceB, isFalse);
            });
          });
        });

        test('contents', () {
          TimestampedData<String> contents = source.contents;
          expect(contents.data, 'library test;');
        });

        test('encoding', () {
          expect(source.encoding, 'file:///foo/test.dart');
        });

        test('exists', () {
          expect(source.exists(), isTrue);
        });

        test('fullName', () {
          expect(source.fullName, '/foo/test.dart');
        });

        test('hashCode', () {
          source.hashCode;
        });

        test('shortName', () {
          expect(source.shortName, 'test.dart');
        });

        test('resolveRelative', () {
          Uri relative =
              source.resolveRelativeUri(new Uri.file('bar/baz.dart'));
          expect(relative.path, '/foo/bar/baz.dart');
        });
      });

      group('non-existent', () {
        setUp(() {
          File file = provider.getResource('/foo/test.dart');
          source = file.createSource();
        });

        test('contents', () {
          expect(() {
            source.contents;
          }, throwsA(_isFileSystemException));
        });

        test('encoding', () {
          expect(source.encoding, 'file:///foo/test.dart');
        });

        test('exists', () {
          expect(source.exists(), isFalse);
        });

        test('fullName', () {
          expect(source.fullName, '/foo/test.dart');
        });

        test('modificationStamp', () {
          expect(source.modificationStamp, -1);
        });

        test('shortName', () {
          expect(source.shortName, 'test.dart');
        });

        test('resolveRelative', () {
          Uri relative =
              source.resolveRelativeUri(new Uri.file('bar/baz.dart'));
          expect(relative.path, '/foo/bar/baz.dart');
        });
      });
    });
  });
}
