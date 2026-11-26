package com.nook.widgets;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.*;
import android.speech.RecognizerIntent;
import android.telephony.PhoneNumberUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Explicit foreground activities for external actions and optional connections. */
public final class WidgetUtilityActivity extends Activity {
    private EditText query;
    private AlertDialog searchDialog;
    public static Intent intent(Context c,String action,int widget,String value){return new Intent(c,WidgetUtilityActivity.class).putExtra("action",action).putExtra("widget",widget).putExtra("value",value).setData(Uri.parse("nook://utility/"+widget+"/"+action+"/"+Uri.encode(value)));}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);String action=getIntent().getStringExtra("action"),value=getIntent().getStringExtra("value");if(value==null)value="";
        if("calendar_access".equals(action)){if(!WidgetData.calendarAllowed(this))requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},1);else finishRefresh();return;}
        if("media_access".equals(action)){try{startActivityForResult(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),2);}catch(ActivityNotFoundException e){unavailable();}return;}
        if("search".equals(action)||"voice_search".equals(action)){search();if(saved!=null)query.setText(saved.getString("query",""));if(saved==null&&"voice_search".equals(action))voice();return;}
        if("dial".equals(action)){String phone=PhoneNumberUtils.normalizeNumber(value);if(!phone.matches("\\+?[0-9]{3,18}")){unavailable();return;}launch(new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",phone,null)));return;}
        if("calendar".equals(action)){launch(new Intent(Intent.ACTION_VIEW,CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(String.valueOf(System.currentTimeMillis())).build()));return;}
        if("event".equals(action)){try{launch(new Intent(Intent.ACTION_VIEW,ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,Long.parseLong(value))));}catch(Exception e){unavailable();}return;}
        if("photo".equals(action)){launch(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(value),"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));return;}
        if("tool".equals(action)){
            Intent target=switch(value){case "wifi"->android.os.Build.VERSION.SDK_INT>=29?new Intent(Settings.Panel.ACTION_WIFI):new Intent(Settings.ACTION_WIFI_SETTINGS);case "bluetooth"->new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);case "display"->new Intent(Settings.ACTION_DISPLAY_SETTINGS);case "location"->new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);case "sound"->new Intent(Settings.ACTION_SOUND_SETTINGS);case "battery"->new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);case "calculator"->new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR);case "camera"->new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);default->new Intent(Settings.ACTION_SETTINGS);};launch(target);return;
        }
        unavailable();
    }
    private void unavailable(){Toast.makeText(this,"No compatible app is available for this action",Toast.LENGTH_LONG).show();finish();}
    private void launch(Intent target){try{startActivity(target);finish();}catch(ActivityNotFoundException|SecurityException e){unavailable();}}
    private void finishRefresh(){Widgets.updateAll(this);finish();}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);finishRefresh();}
    private void search(){
        LinearLayout content=Ui.column(this);Ui.pad(content,24,16);content.setBackgroundColor(Ui.BG);query=new EditText(this);query.setTextColor(Ui.TEXT);query.setHintTextColor(Ui.MUTED);query.setHint("Search the web");query.setSingleLine(true);query.setInputType(android.text.InputType.TYPE_CLASS_TEXT);content.addView(query);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Search the web").setView(content).setPositiveButton("Search",null).setNeutralButton("Voice",null).setNegativeButton("Cancel",(d,n)->finish()).create();searchDialog=dialog;dialog.setOnCancelListener(d->finish());dialog.setOnShowListener(d->{dialog.getButton(-1).setOnClickListener(v->openSearch());dialog.getButton(-3).setOnClickListener(v->{try{startActivityForResult(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM),3);}catch(ActivityNotFoundException e){query.setError("Voice input isn't installed. Type your search instead.");}});query.requestFocus();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);});dialog.show();
    }
    private void openSearch(){String text=query.getText().toString().trim();if(text.isEmpty()){query.setError("Enter a search");return;}int engine;try{engine=Integer.parseInt(getIntent().getStringExtra("value"));}catch(Exception e){engine=0;}String base=new String[]{"https://www.google.com/search?q=","https://duckduckgo.com/?q=","https://www.bing.com/search?q="}[WidgetDesign.clamp(engine,0,2)];launch(new Intent(Intent.ACTION_VIEW,Uri.parse(base+Uri.encode(text))));}
    private void voice(){try{startActivityForResult(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM),3);}catch(ActivityNotFoundException e){query.setError("Voice input isn't installed. Type your search instead.");}}
    @Override protected void onSaveInstanceState(Bundle state){if(query!=null)state.putString("query",query.getText().toString());super.onSaveInstanceState(state);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==3){if(result==RESULT_OK&&data!=null){ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty()){query.setText(words.get(0));openSearch();}}}else finishRefresh();}
}
