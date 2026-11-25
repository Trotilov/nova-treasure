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
 static final int RED=0xfff04460, BG=0xff171015, GOLD=0xffffc16d;
 WindowManager wm;WindowManager.LayoutParams p;
 LinearLayout root,panel;TextView bubble,count;
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
  TextView title=txt("NOVA / OFFLINE",13,RED);bar.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));drag(title,null);
  button(bar,"−",()->resize(-60));button(bar,"+",()->resize(60));button(bar,"▁",()->toggle(false));button(bar,"×",()->stopSelf());panel.addView(bar);
  LinearLayout tools=new LinearLayout(this);count=txt("",12,GOLD);tools.addView(count,new LinearLayout.LayoutParams(0,dp(40),1));button(tools,"+1",()->inc(1));button(tools,"−1",()->inc(-1));button(tools,"↻",()->{if(mapView!=null)mapView.reset();});panel.addView(tools);inc(0);
  java.io.File file=new java.io.File(getFilesDir(),MainActivity.MAP_FILE);
  Bitmap bitmap=null;try{bitmap=BitmapFactory.decodeFile(file.getAbsolutePath());}catch(OutOfMemoryError ignored){}
  if(bitmap!=null){
   mapView=new MapView(bitmap);panel.addView(mapView,new LinearLayout.LayoutParams(-1,0,1));
   panel.addView(txt("Щипок: зум · палец: движение · ↻ сброс",10,0xffc7abb5),new LinearLayout.LayoutParams(-1,dp(20)));
  }else{
   ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(1);list.setPadding(dp(9),dp(4),dp(9),dp(4));scroll.addView(list);
   TextView help=txt("Импортируй карту в NOVA. Пока — рейтинг районов 43.11.",13,GOLD);help.setPadding(0,dp(6),0,dp(8));list.addView(help);
   String[] names={"Cursed Cavern","Buggy's Beachhead","Lil' Uzi's Stronghold","Lantern Landing","Loguetown","Busy Bay","The $.$. Rocky","Windmill Village","Plunder Port","Friendship Villa","Arlong Park","The Tiger's Eye","The Chest"};
   int[] totals={96,85,85,79,73,69,65,64,62,60,50,41,29};
   for(int i=0;i<names.length;i++){TextView item=txt(names[i]+"  ·  "+totals[i],14,0xfff4e9ee);item.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);item.setPadding(dp(6),dp(5),dp(6),dp(5));list.addView(item);}
   TextView source=txt("Burnaviour · 24.11.2026 · возможные спавны",10,0xffc7abb5);list.addView(source);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  }
  root.addView(panel,new LinearLayout.LayoutParams(-1,-1));
 }
 MapView mapView;
 class MapView extends View{
  Bitmap bitmap;Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  float zoom=1,dx=0,dy=0,lastX,lastY;boolean multi;
  ScaleGestureDetector detector;
  MapView(Bitmap b){super(OverlayService.this);bitmap=b;setBackgroundColor(0xff100c10);
   detector=new ScaleGestureDetector(OverlayService.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
    @Override public boolean onScale(ScaleGestureDetector d){float old=zoom;zoom=Math.max(1,Math.min(15,zoom*d.getScaleFactor()));float k=zoom/old;dx=d.getFocusX()-(d.getFocusX()-dx)*k;dy=d.getFocusY()-(d.getFocusY()-dy)*k;clamp();invalidate();return true;}
   });
  }
  float fit(){return Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());}
  void clamp(){float w=bitmap.getWidth()*fit()*zoom,h=bitmap.getHeight()*fit()*zoom;dx=w<=getWidth()?(getWidth()-w)/2:Math.max(getWidth()-w,Math.min(0,dx));dy=h<=getHeight()?(getHeight()-h)/2:Math.max(getHeight()-h,Math.min(0,dy));}
  void reset(){zoom=1;clamp();invalidate();}
  @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);reset();}
  @Override protected void onDraw(Canvas c){super.onDraw(c);if(bitmap==null||bitmap.isRecycled())return;float z=fit()*zoom;c.drawBitmap(bitmap,null,new RectF(dx,dy,dx+bitmap.getWidth()*z,dy+bitmap.getHeight()*z),paint);}
  @Override public boolean onTouchEvent(MotionEvent e){detector.onTouchEvent(e);int a=e.getActionMasked();
   if(a==MotionEvent.ACTION_DOWN){multi=false;lastX=e.getX();lastY=e.getY();return true;}
   if(e.getPointerCount()>1||a==MotionEvent.ACTION_POINTER_UP){multi=true;return true;}
   if(a==MotionEvent.ACTION_MOVE&&!detector.isInProgress()){if(!multi){dx+=e.getX()-lastX;dy+=e.getY()-lastY;clamp();invalidate();}lastX=e.getX();lastY=e.getY();multi=false;}
   if(a==MotionEvent.ACTION_UP)performClick();return true;
  }
  @Override public boolean performClick(){super.performClick();return true;}
  void dispose(){if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();}
 }
 void inc(int delta){n=Math.max(0,n+delta);getSharedPreferences("nova",0).edit().putInt("count",n).apply();count.setText("Сундуки: "+n);}
 void toggle(boolean show){if(show&&panel==null)panel();expanded=show;bubble.setVisibility(show?View.GONE:View.VISIBLE);if(panel!=null)panel.setVisibility(show?View.VISIBLE:View.GONE);p.width=show?w:dp(76);p.height=show?h:dp(48);apply();}
 void resize(int d){Rect b=bounds();w=Math.max(Math.min(dp(290),b.width()-dp(12)),Math.min(b.width()-dp(12),w+dp(d)));h=Math.max(Math.min(dp(230),b.height()-dp(12)),Math.min(b.height()-dp(12),h+dp(d)));if(expanded){p.width=w;p.height=h;apply();}}
 void apply(){if(root==null||p==null)return;Rect b=bounds();p.x=Math.max(0,Math.min(p.x,b.width()-p.width));p.y=Math.max(0,Math.min(p.y,b.height()-p.height));try{wm.updateViewLayout(root,p);}catch(IllegalArgumentException ignored){}}
 void drag(View v,Runnable click){v.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int x,y;boolean moved;public boolean onTouch(View view,MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:sx=e.getRawX();sy=e.getRawY();x=p.x;y=p.y;moved=false;return true;case MotionEvent.ACTION_MOVE:float dx=e.getRawX()-sx,dy=e.getRawY()-sy;if(Math.abs(dx)>dp(5)||Math.abs(dy)>dp(5))moved=true;if(moved){p.x=x+(int)dx;p.y=y+(int)dy;apply();}return true;case MotionEvent.ACTION_UP:if(!moved&&click!=null){view.performClick();click.run();}return true;case MotionEvent.ACTION_CANCEL:return true;default:return false;}}});}
 public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(root!=null){resize(0);apply();}}
 public void onDestroy(){if(root!=null&&wm!=null){try{wm.removeView(root);}catch(Exception ignored){}}if(mapView!=null){mapView.dispose();mapView=null;}root=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
