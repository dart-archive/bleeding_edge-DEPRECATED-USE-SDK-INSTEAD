// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('fs');

#import('node.dart');
#import('nodeimpl.dart');


typedef ReadStreamOpenListener(int fd);

class ReadStream implements ReadableStream, FsStream native "*ReadStream" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
    native "return this.listeners(key);";
  
  // CommonStream
  
  // Error event
  void emitError(Error error)
    native "this.emit('error', error);";
  void addListenerError(StreamErrorListener listener)
    native "this.addListener('error', listener);";
  void onError(StreamErrorListener listener)
    native "this.on('error', listener);";
  void onceError(StreamErrorListener listener)
    native "this.once('error', listener);";
  void removeListenerError(StreamErrorListener listener)
    native "this.removeListener('error', listener);";
  List<StreamErrorListener> listenersError()
    => _listeners('error');

  // Close event
  void emitClose()
    native "this.emit('close');";
  void addListenerClose(StreamCloseListener listener)
    native "this.addListener('close', listener);";
  void onClose(StreamCloseListener listener)
    native "this.on('close', listener);";
  void onceClose(StreamCloseListener listener)
    native "this.once('close', listener);";
  void removeListenerClose(StreamCloseListener listener)
    native "this.removeListener('close', listener);";
  List<StreamCloseListener> listenersClose()
    => _listeners('close');

  // ReadableStream
  
  // Data event
  void emitData(var data)
    native "this.emit('data', data);";
  void addListenerData(ReadableStreamDataListener listener)
    native "this.addListener('data', listener);";
  void onData(ReadableStreamDataListener listener)
    native "this.on('data', listener);";
  void onceData(ReadableStreamDataListener listener)
    native "this.once('data', listener);";
  void removeListenerData(ReadableStreamDataListener listener)
    native "this.removeListener('data', listener);";
  List<ReadableStreamDataListener> listenerData()
    => _listeners('data');
    
  // End event
  void emitEnd()
    native "this.emit('end');";
  void addListenerEnd(ReadableStreamEndListener listener)
    native "this.addListener('end', listener);";
  void onEnd(ReadableStreamEndListener listener)
    native "this.on('end', listener);";
  void onceEnd(ReadableStreamEndListener listener)
    native "this.once('end', listener);";
  void removeListenerEnd(ReadableStreamEndListener listener)
    native "this.removeListener('end', listener);";
  List<ReadableStreamEndListener> listenersEnd()
    => _listeners('end');

  // FsStream
  
  // Open event
  void emitOpen(int fd)
    native "this.emit('open', fd);";
  void addListenerOpen(FsStreamOpenListener listener)
    native "this.addListener('open', listener);";
  void onOpen(FsStreamOpenListener listener)
    native "this.on('open', listener);";
  void onceOpen(FsStreamOpenListener listener)
    native "this.once('open', listener);";
  void removeListenerOpen(FsStreamOpenListener listener)
    native "this.removeListener('open', listener);";
  List<FsStreamOpenListener> listenersOpen()
    => _listeners('open');
    
  bool readable;
  void setEncoding(String encoding) native;
  void pause() native;
  void resume() native;
  void destroy() native;
  void destroySoon() native;
  WritableStream pipe(WritableStream destination, [Map options]) native;
}

class WriteStream implements WritableStream, FsStream native "*WriteStream" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
    native "return this.listeners(key);";

  // CommonStream
  
  // Error event
  void emitError(Error error)
    native "this.emit('error', error);";
  void addListenerError(StreamErrorListener listener)
    native "this.addListener('error', listener);";
  void onError(StreamErrorListener listener)
    native "this.on('error', listener);";
  void onceError(StreamErrorListener listener)
    native "this.once('error', listener);";
  void removeListenerError(StreamErrorListener listener)
    native "this.removeListener('error', listener);";
  List<StreamErrorListener> listenersError()
    => _listeners('error');

  // Close event
  void emitClose()
    native "this.emit('close');";
  void addListenerClose(StreamCloseListener listener)
    native "this.addListener('close', listener);";
  void onClose(StreamCloseListener listener)
    native "this.on('close', listener);";
  void onceClose(StreamCloseListener listener)
    native "this.once('close', listener);";
  void removeListenerClose(StreamCloseListener listener)
    native "this.removeListener('close', listener);";
  List<StreamCloseListener> listenersClose()
    => _listeners('close');

  // WritableStream
  
  // Drain event
  void emitDrain()
    native "this.emit('drain');";
  void addListenerDrain(WritableStreamDrainListener listener)
    native "this.addListener('drain', listener);";
  void onDrain(WritableStreamDrainListener listener)
    native "this.on('drain', listener);";
  void onceDrain(WritableStreamDrainListener listener)
    native "this.once('drain', listener);";
  void removeListenerDrain(WritableStreamDrainListener listener)
    native "this.removeListener('drain', listener);";
  List<WritableStreamDrainListener> listenersDrain()
    => _listeners('drain');
    
  // Pipe event
  void emitPipe(ReadableStream src)
    native "this.emit('pipe', src);";
  void addListenerPipe(WritableStreamPipeListener listener)
    native "this.addListener('pipe', listener);";
  void onPipe(WritableStreamPipeListener listener)
    native "this.on('pipe', listener);";
  void oncePipe(WritableStreamPipeListener listener)
    native "this.once('pipe', listener);";
  void removeListenerPipe(WritableStreamPipeListener listener)
    native "this.removeListener('pipe', listener);";
  List<WritableStreamPipeListener> listenersPipe()
    => _listeners('pipe');

  // FsStream

  // Open event
  void emitOpen(int fd)
    native "this.emit('open', fd);";
  void addListenerOpen(FsStreamOpenListener listener)
    native "this.addListener('open', listener);";
  void onOpen(FsStreamOpenListener listener)
    native "this.on('open', listener);";
  void onceOpen(FsStreamOpenListener listener)
    native "this.once('open', listener);";
  void removeListenerOpen(FsStreamOpenListener listener)
    native "this.removeListener('open', listener);";
  List<FsStreamOpenListener> listenersOpen()
    => _listeners('open');
      
  bool writable;
  bool write(String string, [String encoding='utf8', int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
  void destroy() native;
  void destroySoon() native;
  
  List<int> getWindowSize() native;
  
  int bytesWritten;
}

typedef void FsRenameCallback(Error err);
typedef void FsTruncateCallback(Error err);
typedef void FsChownCallback(Error err);
typedef void FsFchownCallback(Error err);
typedef void FsLchownCallback(Error err);
typedef void FsChmodCallback(Error err);
typedef void FsFchmodCallback(Error err);
typedef void FsLchmodCallback(Error err);
typedef void FsStatCallback(Error err, Stats stats);
typedef void FsLstatCallback(Error err, Stats stats);
typedef void FsFstatCallback(Error err, Stats stats);
typedef void FsLinkCallback(Error err);
typedef void FsSymlinkCallback(Error err);
typedef void FsReadlinkCallback(Error err, String linkString);
typedef void FsRealpathCallback(Error err, String resolvedPath);
typedef void FsUnlinkCallback(Error err);
typedef void FsRmdirCallback(Error err);
typedef void FsMkdirCallback(Error err);
typedef void FsReaddirCallback(Error err, List<String> files);
typedef void FsCloseCallback(Error err);
typedef void FsOpenCallback(Error err);
typedef void FsUtimesCallback(Error err);
typedef void FsFutimesCallback(Error err);
typedef void FsFsyncCallback(Error err);
typedef void FsWriteCallback(Error err, int written, Buffer buffer);
typedef void FsReadCallback(Error err, int bytesRead, Buffer buffer);
typedef void FsReadFileCallback(Error err, String data);
typedef void FsReadFileBufferCallback(Error err, Buffer data);
typedef void FsWriteFileCallback(Error err);
typedef void FsWriteFileBufferCallback(Error err);
typedef void FsWatchFileListener(Stats curr, Stats prev);

class Fs {
  var _fs;
  Fs.from(this._fs);
  
  void rename(String path1, String path2, [FsRenameCallback callback])
      native "this._fs.rename(path1, path2, callback);";
  void renameSync(String path1, String path2)
      native "this._fs.renameSync(path1, path2);";

  void truncate(int fd, int len, [FsTruncateCallback callback])
      native "this._fs.truncate(fd, len, callback);";
  void truncateSync(int fd, int len)
      native "this._fs.truncateSync(fd, len);";

  void chown(String path, int uid, int gid, [FsChownCallback callback])
      native "this._fs.chown(path, uid, gid, callback);";
  void chownSync(String path, int uid, int gid)
      native "this._fs.chownSync(path, uid, gid);";

  void fchown(int fd, int uid, int gid, [FsFchownCallback callback])
      native "this._fs.fchown(fd, uid, gid, callback);";
  void fchownSync(int fd, int uid, int gid)
      native "this._fs.fchownSync(fd, uid, gid);";

  void lchown(String path, int uid, int gid, [FsFchownCallback callback])
      native "this._fs.lchown(path, uid, gid, callback);";
  void lchownSync(String path, int uid, int gid)
      native "this._fs.lchownSync(path, uid, gid);";

  void chmod(String path, int mode, [FsChmodCallback callback])
      native "this._fs.chmod(path, mode, callback);";
  void chmodSync(String path, int mode)
      native "this._fs.chmodSync(path, mode);";

  void fchmod(int fd, int mode, [FsFchmodCallback callback])
      native "this._fs.fchmod(fd, mode, callback);";
  void fchmodSync(int fd, int mode)
      native "this._fs.fchmodSync(fd, mode);";

  void lchmod(String path, int mode, [FsLchmodCallback callback])
      native "this._fs.lchmod(path, mode, callback);";
  void lchmodSync(String path, int mode)
      native "this._fs.lchmodSync(path, mode);";

  void stat(String path, [FsStatCallback callback])
      native "this._fs.stat(path, callback);";
  void lstat(String path, [FsLstatCallback callback])
      native "this._fs.lstat(path, callback);";
  void fstat(int fd, [FsFstatCallback callback])
      native "this._fs.fstat(fd, callback);";

  Stats statSync(String path)
      native "return this._fs.statSync(path);";
  Stats lstatSync(String path)
      native "return this._fs.lstatSync(path);";
  Stats fstatSync(int fd)
      native "return this._fs.fstatSync(fd);";

  void link(String srcpath, String dstpath, [FsLinkCallback callback])
      native "this._fs.link(srcpath, dstpath, callback);";
  void linkSync(String srcpath, String dstpath)
      native "this._fs.linkSync(srcpath, dstpath);";

  void symlink(String linkdata, String path, [FsSymlinkCallback callback])
      native "this._fs.symlink(linkdata, path, callback);";
  void symlinkSync(String linkdata, String path)
      native "this._fs.symlinkSync(linkdata, path);";

  void readlink(String path, [FsReadlinkCallback callback])
      native "this._fs.readlink(path, callback);";
  String readlinkSync(String path)
      native "return this._fs.readlinkSync(path);";

  void realpath(String path, [FsRealpathCallback callback])
      native "this._fs.realpath(path, callback);";
  String realpathSync(String path)
      native "return this._fs.realpathSync(path);";

  void unlink(String path, [FsUnlinkCallback callback])
      native "this._fs.unlink(path, callback);";
  void unlinkSync(String path)
      native "this._fs.unlinkSync(path);";

  void rmdir(String path, [FsRmdirCallback callback])
      native "this._fs.rmdir(path, callback);";
  void rmdirSync(String path)
      native "this._fs.rmdirSync(path);";

  void mkdir(String path, [int mode = 511 /* 0777 octal */, FsMkdirCallback
      callback])
      native "this._fs.mkdir(path, mode, callback);";
  void mkdirSync(String path, [int mode = 511 /* 0777 octal */])
      native "this._fs.mkdirSync(path, mode);";

  void readdir(String path, [FsReaddirCallback callback])
      native "this._fs.readdir(path, callback);";
  List<String> readdirSync(String path)
      native "return this._fs.readdirSync(path);";

  void close(int fd, [FsCloseCallback callback])
      native "this._fs.close(fd, callback);";
  void closeSync(int fd)
      native "this._fs.closeSync(fd);";

  void open(String path, String flags, [int mode = 438 /* 0666 octal */,
      FsOpenCallback callback])
      native "this._fs.open(path, flags, mode, callback);";
  void openSync(String path, String flags, [int mode = 438 /* 0666 octal */])
      native "this._fs.openSync(path, flags, mode);";

  void utimes(String path, int atime, int mtime, [FsUtimesCallback callback])
      native "this._fs.utimes(path, atime, mtime, callback);";
  void utimesSync(String path, int atime, int mtime)
      native "this._fs.utimesSync(path, atime, mtime);";

  void futimes(int fd, int atime, int mtime, [FsUtimesCallback callback])
      native "this._fs.futimes(fd, atime, mtime, callback);";
  void futimesSync(int fd, int atime, int mtime)
      native "this._fs.futimesSync(fd, atime, mtime);";

  void fsync(int fd, [FsFsyncCallback callback])
      native "this._fs.fsync(fd, callback);";
  void fsyncSync(int fd)
      native "this._fs.fsyncSync(fd);";

  void write(int fd, Buffer buffer, int offset, int length, int position,
      [FsWriteCallback callback])
      native "this._fs.write(fd, buffer, offset, length, position, callback);";
  int writeBufferSync(int fd, Buffer buffer, int offset, int length, int
      position)
      native
          "return this._fs.writeBufferSync(fd, buffer, offset, length, position);";
  int writeSync(int fd, String str, [int position, String encoding='utf8'])
      native "return this._fs.writeSync(fd, str, position, encoding);";

  void read(int fd, String buffer, int offset, int length, int position,
      [FsReadCallback callback])
      native "this._fs.read(fd, buffer, offset, length, position, callback);";
  int readBufferSync(int fd, String buffer, int offset, int length, int position
      )
      native "return this._fs.readSync(fd, buffer, offset, length, position);";
  String readSync(int fd, int length, int position, String encoding)
      native "return this._fs.readSync(fd, length, position, encoding);";

  void readFile(String filename, [String encoding=
      'utf8', FsReadFileCallback callback])
      native "this._fs.readFile(filename, encoding, callback);";
  String readFileSync(String filename, [String encoding='utf8'])
      native "return this._fs.readFileSync(filename, encoding);";

  void readFileBuffer(String filename, [FsReadFileBufferCallback callback])
      native "this._fs.readFile(filename, callback);";
  Buffer readFileBufferSync(String filename)
      native "return this._fs.readFileSync(filename);";

  void writeFile(String filename, String data, [String encoding=
      'utf8',  FsWriteFileCallback callback])
      native "this._fs.writeFile(filename, data, encoding, callback);";
  void writeFileSync(String filename, String data, [String encoding='utf8'])
      native "this._fs.writeFileSync(filename, data, encoding);";

  void writeFileBuffer(String filename, Buffer data, [FsWriteFileBufferCallback
      callback])
      native "this._fs.writeFile(filename, data, callback);";
  void writeFileBufferSync(String filename, Buffer data)
      native "this._fs.writeFile(filename, data);";

  void watchFile(String filename, FsWatchFileListener listener, [Map options])
      native "this._fs.watchFile(filename, options, listener);";
  void unwatchFile(String filename)
      native "this._fs.unwatchFile(filename);";
  FSWatcher watch(String filename, FSWatcherChangeListener listener, [Map
      options])
      native "return this._fs.watch(filename, options, listener);";

  ReadStream createReadStream(String path, [Map options])
      native "return this._fs.createReadStream(path, options);";
  WriteStream createWriteStream(String path, [Map options])
      native "return this._fs.createWriteStream(path, options);";
}

Fs get fs() => require('fs');
  
class Stats native "fs.Stats" {
  bool isFile() native;
  bool isDirectory() native;
  bool isBlockDevice() native;
  bool isCharacterDevice() native;
  bool isSymbolicLink() native;
  bool isFIFO() native;
  bool isSocket() native;

  int dev;
  int ino;
  int mode;
  int nlink;
  int uid;
  int gid;
  int rdev;
  int size;
  int blksize;
  int blocks;
  Date atime;
  Date mtime;
  Date ctime;
}

typedef void FSWatcherErrorListener(Error err);
typedef void FSWatcherChangeListener(String event, String filename);

class FSWatcher native "*FSWatcher" {
  void close() native;

  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
    native "return this.listeners(key);";

  // Error event
  void emitError(Error error)
    native "this.emit('error', error);";
  void addListenerError(FSWatcherErrorListener listener)
    native "this.addListener('error', listener);";
  void onError(FSWatcherErrorListener listener)
    native "this.on('error', listener);";
  void onceError(FSWatcherErrorListener listener)
    native "this.once('error', listener);";
  void removeListenerError(FSWatcherErrorListener listener)
    native "this.removeListener('error', listener);";
  List<FSWatcherErrorListener> listenersError()
    => _listeners('error');
    
  // Change event
  void emitChange(String event, String filename)
    native "this.emit('change', event, filename);";
  void addListenerChange(FSWatcherChangeListener listener)
    native "this.addListener('change', listener);";
  void onChange(FSWatcherChangeListener listener)
    native "this.on('change', listener);";
  void onceChange(FSWatcherChangeListener listener)
    native "this.once('change', listener);";
  void removeListenerChange(FSWatcherChangeListener listener)
    native "this.removeListener('change', listener);";
  List<FSWatcherChangeListener> listenersChange()
    => _listeners('change');
}