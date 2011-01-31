package org.jared.commons.ui.example;


import org.jared.commons.ui.R;
import org.jared.commons.ui.WorkspaceView;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class TestActivity extends Activity {

  private String lv_arr[] = { "Android", "iPhone", "BlackBerry", "AndroidPeople", "Symbian", "iPad","Windows Mobile", "Sony","HTC","Motorola" };
  private String lv_arr2[] = { "Eric Taix", "eric.taix@gmail.com" };

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    WorkspaceView work = (WorkspaceView) findViewById(R.id.workspace);
    // Car il y a toujours un petit d�calage du doigt m�me lors d'un scrolling vertical
    work.setTouchSlop(32);
    // Chargement de l'image d fond (peut �tre enlev�e)
    Bitmap backGd = BitmapFactory.decodeResource(getResources(), R.drawable.background_black_1280x1024);
    //work.loadWallpaper(backGd);
    
    ListView lv1 = (ListView) findViewById(R.id.list1);
    lv1.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lv_arr));
    ListView lv2 = (ListView) findViewById(R.id.list2);
    lv2.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lv_arr2));
    
    // Just to test ListView listener: OnItemClick AND OnItemLongListener
    lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> arg0P, View arg1P, int arg2P, long arg3P) {
        Toast toast = Toast.makeText(TestActivity.this, "Click", Toast.LENGTH_SHORT);
        toast.show();
      }
    });    
    lv1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      public boolean onItemLongClick(AdapterView<?> arg0P, View arg1P, int arg2P, long arg3P) {
        Toast toast = Toast.makeText(TestActivity.this, "Long Click......", Toast.LENGTH_SHORT);
        toast.show();
        return true;
      }
    });
  }
}