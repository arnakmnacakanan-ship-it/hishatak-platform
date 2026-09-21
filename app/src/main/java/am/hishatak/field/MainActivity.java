package am.hishatak.field;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
 private WebView web;
 private ValueCallback<Uri[]> chooser;
 private static final int FILE_REQ=9, PERM_REQ=10;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  web=new WebView(this); setContentView(web);
  WebSettings s=web.getSettings();
  s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
  s.setGeolocationEnabled(true); s.setMediaPlaybackRequiresUserGesture(false);
  web.setWebViewClient(new WebViewClient());
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb){
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},PERM_REQ);
    cb.invoke(origin,true,false);
   }
   @Override public void onPermissionRequest(PermissionRequest r){ runOnUiThread(()->r.grant(r.getResources())); }
   @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> c, FileChooserParams p){
    chooser=c; startActivityForResult(p.createIntent(),FILE_REQ); return true;
   }
  });
  if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
    requestPermissions(new String[]{Manifest.permission.CAMERA},PERM_REQ);
  web.loadUrl("https://hishatak-field-pwa-production.up.railway.app");
 }
 @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d);
  if(r==FILE_REQ&&chooser!=null){ chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(c,d)); chooser=null; }
 }
 @Override public void onBackPressed(){ if(web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
