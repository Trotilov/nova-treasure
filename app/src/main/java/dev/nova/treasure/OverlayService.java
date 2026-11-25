package dev.nova.treasure;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.*;
public class OverlayService extends Service {
 static final String MAP="https://burnaviour.dev/map/?layers=chests%2Crare-chests";
 static final String BACKUP="https://fortnitechestcounter.org/map/";
 final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 boolean failed=false; int loadId=0;
 static final int RED=0xfff04460, BG=0xff171015, GOLD=0xffffc16d;
 WindowManager wm;WindowManager.LayoutParams p;
 LinearLayout root,panel;TextView bubble,count,loading;WebView web;
 int w,h,n;boolean expanded;
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 public IBinder onBind(Intent i){return null;}
 public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  if(root!=null)return START_NOT_STICKY;
  NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("nova","NOVA overlay",NotificationManager.IMPORTANCE_LOW));
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,OverlayService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
  startForeground(18,new Notification.Builder(this,"nova").setSmallIcon(android.R.drawable.ic_menu_mapmode).setContentTitle("NOVA Treasure").setContentText("Плавающая карта").setOngoing(true).addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,"Выключить",stop).build()).build());
  if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
  try{setup();}catch(RuntimeException e){stopSelf();}
  return START_NOT_STICKY;
 }
 Rect bounds(){if(android.os.Build.VERSION.SDK_INT>=30)return wm.getCurrentWindowMetrics().getBounds();android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getMetrics(m);return new Rect(0,0,m.widthPixels,m.heightPixels);}
 TextView txt(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);return t;}
 TextView key(String s,Runnable r){TextView t=txt(s,16,0xfff4e9ee);t.setOnClickListener(v->r.run());return t;}
 void setup(){
  wm=getSystemService(WindowManager.class);Rect b=bounds();w=Math.min(dp(530),b.width()-dp(16));h=Math.min(dp(360),b.height()-dp(24));n=getSharedPreferences("nova",0).getInt("count",0);
  root=new LinearLayout(this);root.setOrientation(1);GradientDrawable d=new GradientDrawable();d.setColor(BG);d.setCornerRadius(dp(10));d.setStroke(dp(1),RED);root.setBackground(d);root.setClipToOutline(true);
  bubble=txt("NOVA",15,RED);bubble.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(bubble,new LinearLayout.LayoutParams(-1,dp(48)));drag(bubble,()->toggle(true));
  p=new WindowManager.LayoutParams(dp(76),dp(48),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.x=dp(12);p.y=dp(64);wm.addView(root,p);
 }
 void button(LinearLayout l,String s,Runnable r){l.addView(key(s,r),new LinearLayout.LayoutParams(dp(42),dp(40)));}
 void panel(){
  panel=new LinearLayout(this);panel.setOrientation(1);
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(0xff301721);
  TextView title=txt("NOVA / MAP",13,RED);bar.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));drag(title,null);
  button(bar,"−",()->resize(-60));button(bar,"+",()->resize(60));button(bar,"▁",()->toggle(false));button(bar,"×",()->stopSelf());panel.addView(bar);
  LinearLayout tools=new LinearLayout(this);count=txt("",12,GOLD);tools.addView(count,new LinearLayout.LayoutParams(0,dp(40),1));button(tools,"+1",()->inc(1));button(tools,"−1",()->inc(-1));button(tools,"↻",()->loadMap(MAP));button(tools,"B",()->loadMap(BACKUP));panel.addView(tools);inc(0);
  loading=txt("Загрузка онлайн-карты…",12,GOLD);panel.addView(loading,new LinearLayout.LayoutParams(-1,dp(26)));
  web=new WebView(this);web.setLayerType(View.LAYER_TYPE_HARDWARE,null);web.setBackgroundColor(BG);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setCacheMode(WebSettings.LOAD_DEFAULT);s.setMediaPlaybackRequiresUserGesture(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);
  web.setWebChromeClient(new WebChromeClient(){public void onProgressChanged(WebView v,int progress){if(!failed && loading.getVisibility()==View.VISIBLE)loading.setText("Загрузка карты: "+progress+"%");}});
  web.setWebViewClient(new WebViewClient(){
   public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();return r.isForMainFrame() && (!"https".equals(u.getScheme())||!("burnaviour.dev".equalsIgnoreCase(u.getHost())||"www.burnaviour.dev".equalsIgnoreCase(u.getHost())||"fortnitechestcounter.org".equalsIgnoreCase(u.getHost())||"www.fortnitechestcounter.org".equalsIgnoreCase(u.getHost())));}
   public void onPageStarted(WebView v,String url,Bitmap i){if(!failed){loading.setText("Загрузка карты…");loading.setVisibility(View.VISIBLE);}}
   public void onPageFinished(WebView v,String url){if(!failed)loading.setVisibility(View.GONE);}
   public void onPageCommitVisible(WebView v,String url){if(!failed)loading.setVisibility(View.GONE);}
   public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){failed=true;loading.setText("Ошибка "+e.getErrorCode()+". ↻ повтор / B другая карта");loading.setVisibility(View.VISIBLE);}}
   public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame()){failed=true;loading.setText("HTTP "+e.getStatusCode()+". ↻ повтор / B другая карта");loading.setVisibility(View.VISIBLE);}}
  });
  panel.addView(web,new LinearLayout.LayoutParams(-1,0,1));panel.addView(txt("Burnaviour / Chest Counter · B: запасная карта",10,0xffc7abb5),new LinearLayout.LayoutParams(-1,dp(20)));root.addView(panel,new LinearLayout.LayoutParams(-1,-1));loadMap(MAP);
 }
 void loadMap(String url){if(web==null)return;web.stopLoading();failed=false;final int token=++loadId;loading.setText("Загрузка карты: 0%");loading.setVisibility(View.VISIBLE);web.loadUrl(url);handler.postDelayed(()->{if(web!=null&&token==loadId&&!failed&&loading.getVisibility()==View.VISIBLE){failed=true;web.stopLoading();loading.setText("Тайм-аут. ↻ повтор / B другая карта");}},20000);}

 void inc(int delta){n=Math.max(0,n+delta);getSharedPreferences("nova",0).edit().putInt("count",n).apply();count.setText("Сундуки: "+n);}
 void toggle(boolean show){if(show&&panel==null)panel();expanded=show;bubble.setVisibility(show?View.GONE:View.VISIBLE);if(panel!=null)panel.setVisibility(show?View.VISIBLE:View.GONE);if(web!=null){if(show)web.onResume();else web.onPause();}p.width=show?w:dp(76);p.height=show?h:dp(48);apply();}
 void resize(int d){Rect b=bounds();w=Math.max(Math.min(dp(290),b.width()-dp(12)),Math.min(b.width()-dp(12),w+dp(d)));h=Math.max(Math.min(dp(230),b.height()-dp(12)),Math.min(b.height()-dp(12),h+dp(d)));if(expanded){p.width=w;p.height=h;apply();}}
 void apply(){if(root==null||p==null)return;Rect b=bounds();p.x=Math.max(0,Math.min(p.x,b.width()-p.width));p.y=Math.max(0,Math.min(p.y,b.height()-p.height));try{wm.updateViewLayout(root,p);}catch(IllegalArgumentException ignored){}}
 void drag(View v,Runnable click){v.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int x,y;boolean moved;public boolean onTouch(View view,MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:sx=e.getRawX();sy=e.getRawY();x=p.x;y=p.y;moved=false;return true;case MotionEvent.ACTION_MOVE:float dx=e.getRawX()-sx,dy=e.getRawY()-sy;if(Math.abs(dx)>dp(5)||Math.abs(dy)>dp(5))moved=true;if(moved){p.x=x+(int)dx;p.y=y+(int)dy;apply();}return true;case MotionEvent.ACTION_UP:if(!moved&&click!=null){view.performClick();click.run();}return true;case MotionEvent.ACTION_CANCEL:return true;default:return false;}}});}
 public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(root!=null){resize(0);apply();}}
 public void onDestroy(){handler.removeCallbacksAndMessages(null);if(root!=null&&wm!=null){try{wm.removeView(root);}catch(Exception ignored){}}if(web!=null){panel.removeView(web);web.destroy();}root=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
