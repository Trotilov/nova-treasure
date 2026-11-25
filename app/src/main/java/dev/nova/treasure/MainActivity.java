package dev.nova.treasure;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
public class MainActivity extends Activity {
 TextView status;
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setGravity(Gravity.CENTER);l.setPadding(dp(24),dp(24),dp(24),dp(24));l.setBackgroundColor(0xff110c10);
  TextView title=new TextView(this);title.setText("NOVA // TREASURE");title.setTextSize(32);title.setTextColor(0xfff04460);l.addView(title);
  status=new TextView(this);status.setTextSize(16);status.setTextColor(0xffffffff);status.setPadding(0,dp(18),0,dp(18));l.addView(status);
  Button start=new Button(this);start.setText("Запустить оверлей");l.addView(start);
  Button stop=new Button(this);stop.setText("Выключить");l.addView(stop);
  TextView note=new TextView(this);note.setText("После запуска открой Fortnite. Нажми NOVA для карты. Двигай окно за заголовок. Интернет нужен. Это публичная карта, не ESP.");note.setTextColor(0xffc7abb5);note.setMaxWidth(dp(500));note.setPadding(0,dp(16),0,0);l.addView(note);
  start.setOnClickListener(v->{if(!Settings.canDrawOverlays(this)){try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){status.setText("Разреши оверлей в настройках Android.");}}else{try{startForegroundService(new Intent(this,OverlayService.class));status.setText("NOVA запущена");}catch(Exception e){status.setText("Ошибка запуска: "+e.getClass().getSimpleName());}}});
  stop.setOnClickListener(v->{stopService(new Intent(this,OverlayService.class));status.setText("NOVA выключена");});
  setContentView(l);
 }
 @Override protected void onResume(){super.onResume();status.setText(Settings.canDrawOverlays(this)?"Разрешение выдано. Нажми «Запустить оверлей».":"Сначала разреши отображение поверх других приложений.");}
}
