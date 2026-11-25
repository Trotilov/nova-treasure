package dev.nova.treasure;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {
 TextView status;
 static final String MAP_FILE="offline-map.png";
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 void button(LinearLayout l,String title,Runnable r){Button b=new Button(this);b.setText(title);b.setOnClickListener(v->r.run());l.addView(b);}
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  ScrollView scroll=new ScrollView(this);
  LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setGravity(Gravity.CENTER_HORIZONTAL);l.setPadding(dp(24),dp(24),dp(24),dp(24));l.setBackgroundColor(0xff110c10);scroll.setFillViewport(true);scroll.addView(l);
  TextView title=new TextView(this);title.setText("NOVA // TREASURE 0.3");title.setTextSize(28);title.setTextColor(0xfff04460);l.addView(title);
  status=new TextView(this);status.setTextSize(16);status.setTextColor(0xffffffff);status.setPadding(0,dp(18),0,dp(18));l.addView(status);
  button(l,"1. Открыть сайт карты",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://burnaviour.dev/map/?layers=chests%2Crare-chests")));}catch(Exception e){status.setText("Нет браузера");}});
  button(l,"2. Выбрать PNG / JPG карты",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,42);});
  button(l,"3. Запустить оверлей",()->{if(!Settings.canDrawOverlays(this)){try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){status.setText("Разреши оверлей в настройках Android.");}}else{try{startForegroundService(new Intent(this,OverlayService.class));status.setText("NOVA запущена");}catch(Exception e){status.setText("Ошибка запуска: "+e.getClass().getSimpleName());}}});
  button(l,"Выключить",()->{stopService(new Intent(this,OverlayService.class));refresh();});
  TextView note=new TextView(this);note.setText("На сайте включи нужные слои и нажми Download map (или сделай скриншот). Выбери сохранённую картинку. После этого карта работает без интернета. Если картинки нет, оверлей показывает рейтинг районов 43.11. Сундуки не отслеживаются в реальном времени.");note.setTextColor(0xffc7abb5);note.setMaxWidth(dp(500));note.setPadding(0,dp(16),0,0);l.addView(note);
  setContentView(scroll);
 }
 void refresh(){if(status!=null)status.setText((new File(getFilesDir(),MAP_FILE).isFile()?"Карта сохранена офлайн":"Карта ещё не импортирована")+"\n"+(Settings.canDrawOverlays(this)?"Оверлей разрешён":"Нужно разрешить отображение поверх приложений"));}
 @Override protected void onResume(){super.onResume();refresh();}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request!=42||result!=RESULT_OK||data==null||data.getData()==null)return;
  Uri uri=data.getData();status.setText("Сохраняю карту…");
  new Thread(()->{
   File tmp=new File(getFilesDir(),"map.tmp"); Bitmap bitmap=null;
   try{
    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
    try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);}
    if(o.outWidth<=0||o.outHeight<=0)throw new IOException("Некорректный файл");
    o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>4096)o.inSampleSize*=2;o.inJustDecodeBounds=false;
    try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,o);}
    if(bitmap==null)throw new IOException("Не удалось прочитать изображение");
    try(FileOutputStream out=new FileOutputStream(tmp)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Ошибка сохранения");}
    if(!tmp.renameTo(new File(getFilesDir(),MAP_FILE)))throw new IOException("Не удалось заменить карту");
    runOnUiThread(()->{stopService(new Intent(this,OverlayService.class));refresh();});
   }catch(Exception|OutOfMemoryError e){tmp.delete();runOnUiThread(()->status.setText("Ошибка импорта. Выбери PNG или JPG меньшего размера."));}
   finally{if(bitmap!=null)bitmap.recycle();}
  }).start();
 }
}
