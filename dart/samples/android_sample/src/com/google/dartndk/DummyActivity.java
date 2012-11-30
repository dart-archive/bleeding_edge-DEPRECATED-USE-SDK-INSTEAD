package com.google.dartndk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.NativeActivity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

public class DummyActivity extends NativeActivity {
  static {
    System.loadLibrary("android_embedder");
    System.loadLibrary("DartNDK");
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);    
    try { 
      File localDir = getApplicationContext().getDir("dart", 0);
      String fileSystemPath = localDir.toString();
      String assetPath = "dart";
      AssetManager assetManager = getAssets();
      String[] files = assetManager.list(assetPath);
      byte[] buffer = new byte[1024];
      int read;    
      for (String filename : files) {
        String dest = fileSystemPath + "/" + filename;
        Log.w("Dart", "Copying " + dest);      
        InputStream in = assetManager.open(assetPath + "/" + filename);
        OutputStream out = new FileOutputStream(dest);
        while((read = in.read(buffer)) != -1){
          out.write(buffer, 0, read);
        }      
        in.close();
        out.flush();
        ((FileOutputStream)out).getFD().sync();
        out.close();
      }
    } catch (IOException ex) {
    }
  }
}
