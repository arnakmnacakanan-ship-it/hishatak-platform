package am.hishatak.field;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.File;
import java.io.IOException;
import androidx.core.content.FileProvider;

public class MainActivity extends Activity {
 private WebView web;
 private ValueCallback<Uri[]> chooser;
 private Uri cameraUri;
 private GeolocationPermissions.Callback geoCallback;
 private String geoOrigin;
 private static final int FILE_REQ=9, LOCATION_REQ=10, CAMERA_REQ=11;

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  web=new WebView(this); setContentView(web);
  WebSettings s=web.getSettings();
  s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
  s.setGeolocationEnabled(true); s.setMediaPlaybackRequiresUserGesture(false);
  web.addJavascriptInterface(new Object(){ @JavascriptInterface public void saveFile(String name,String text,String type){ runOnUiThread(()->{ try{ File dir=getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS); if(dir==null) dir=getFilesDir(); if(!dir.exists()) dir.mkdirs(); File out=new File(dir,name); try(FileOutputStream fos=new FileOutputStream(out)){ fos.write(text.getBytes(StandardCharsets.UTF_8)); } Toast.makeText(MainActivity.this,"Сохранено: "+out.getAbsolutePath(),Toast.LENGTH_LONG).show(); }catch(Exception e){ Toast.makeText(MainActivity.this,"Ошибка экспорта: "+e.getMessage(),Toast.LENGTH_LONG).show(); }}); } },"Android");
  web.setWebViewClient(new WebViewClient(){ @Override public boolean shouldOverrideUrlLoading(WebView v,String url){ return false; } });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb){
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
       checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED){
      cb.invoke(origin,true,false);
    } else {
      geoOrigin=origin; geoCallback=cb;
      requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_REQ);
    }
   }
   @Override public void onPermissionRequest(PermissionRequest r){
    runOnUiThread(()->{
      if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) r.grant(r.getResources());
      else { r.deny(); requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_REQ); }
    });
   }
   @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> c, FileChooserParams p){
    if(chooser!=null) chooser.onReceiveValue(null);
    chooser=c;
    Intent select=p.createIntent();
    Intent camera=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if(camera.resolveActivity(getPackageManager())!=null){
      try{
        File dir=new File(getCacheDir(),"camera"); if(!dir.exists()) dir.mkdirs();
        File photo=File.createTempFile("hishatak_",".jpg",dir);
        cameraUri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".fileprovider",photo);
        camera.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);
        camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // The web page has separate "Camera" and "Gallery" buttons.
        // For capture requests, launch the camera directly instead of Android's app chooser.
        boolean captureEnabled = p.isCaptureEnabled();
        if (captureEnabled) {
          startActivityForResult(camera,FILE_REQ);
        } else {
          Intent chooserIntent=new Intent(Intent.ACTION_CHOOSER);
          chooserIntent.putExtra(Intent.EXTRA_INTENT,select);
          chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{camera});
          startActivityForResult(chooserIntent,FILE_REQ);
        }
      }catch(IOException e){ startActivityForResult(select,FILE_REQ); }
    } else startActivityForResult(select,FILE_REQ);
    return true;
   }
  });
  web.loadUrl("file:///android_asset/index.html");
 }

 @Override public void onRequestPermissionsResult(int req,String[] perms,int[] results){
  super.onRequestPermissionsResult(req,perms,results);
  if(req==LOCATION_REQ && geoCallback!=null){
    boolean ok=(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED);
    geoCallback.invoke(geoOrigin,ok,false); geoCallback=null; geoOrigin=null;
  }
 }

 @Override protected void onActivityResult(int r,int c,Intent d){
  super.onActivityResult(r,c,d);
  if(r==FILE_REQ&&chooser!=null){
    Uri[] out=null;
    if(c==RESULT_OK){
      if(d==null || d.getData()==null) { if(cameraUri!=null) out=new Uri[]{cameraUri}; }
      else out=WebChromeClient.FileChooserParams.parseResult(c,d);
    }
    chooser.onReceiveValue(out); chooser=null; cameraUri=null;
  }
 }
 @Override public void onBackPressed(){ if(web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
