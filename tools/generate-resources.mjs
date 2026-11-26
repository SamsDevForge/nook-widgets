import fs from 'node:fs';
import './generate-dot-font.mjs';
import './generate-music-resources.mjs';
const root = new URL('../app/src/main/res/', import.meta.url);
function write(file, value) { const url = new URL(file, root); fs.mkdirSync(new URL('.', url), {recursive:true}); fs.writeFileSync(url, value.replace(/[ \t]+$/gm, '').trimEnd() + '\n'); }
const palettes = [
  ['moss','#243128','#E9F0E4','#AEBFAE','#394D3D'],
  ['ink','#20252E','#F0F2F7','#A6B0C1','#343B48'],
  ['sand','#F3E9D9','#3C372D','#7D7567','#E2D4BD'],
  ['lilac','#DDD9F0','#353047','#746B90','#C5BEDF'],
  ['rose','#F0DCD6','#4C3432','#916E66','#DFBEB5'],
  ['sky','#D9E8EF','#253E4A','#5F7E8C','#BBD4DF']
];
const smokedTints = ['#182A21','#182234','#352C20','#2B243C','#382628','#182E3A'];
function lift(color, amount) {
  return '#' + color.slice(1).match(/../g).map(v=>Math.round(parseInt(v,16)*(1-amount)+255*amount).toString(16).padStart(2,'0')).join('');
}
function glassSurface(color) {
  // Keep fill colors opaque: a separate ImageView applies the user's exact
  // transparency percentage to the surface without fading text or icons.
  return `<shape xmlns:android="http://schemas.android.com/apk/res/android"><corners android:radius="28dp"/><gradient android:angle="315" android:startColor="${lift(color,.22)}" android:centerColor="${lift(color,.025)}" android:endColor="${color}"/><stroke android:width="1dp" android:color="#70FFFFFF"/></shape>\n`;
}
for (const [name,bg,, ,chip] of palettes) {
  for (const [suffix,color,radius] of [['surface',bg,28],['chip',chip,16]])
    write(`drawable/${name}_${suffix}.xml`, `<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="${color}"/><corners android:radius="${radius}dp"/></shape>\n`);
  for(const style of ['frosted','smoked']) {
    write(`drawable/${name}_${style}.xml`,glassSurface(style==='frosted'?bg:smokedTints[palettes.findIndex(p=>p[0]===name)]));
    write(`drawable/${name}_${style}_chip.xml`,`<shape xmlns:android="http://schemas.android.com/apk/res/android"><corners android:radius="16dp"/><gradient android:angle="270" android:startColor="#32FFFFFF" android:endColor="#15FFFFFF"/><stroke android:width="1dp" android:color="#45FFFFFF"/></shape>\n`);
  }
}
// An illustrative backdrop belongs to app previews only. Native glass widgets
// remain transparent over the actual launcher wallpaper, without reading it.
write('drawable/preview_wallpaper.xml',`<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
  <item><shape><corners android:radius="28dp"/><gradient android:angle="315" android:startColor="#172A38" android:centerColor="#4A3C70" android:endColor="#976D83"/></shape></item>
  <item><shape><corners android:radius="28dp"/><gradient android:type="radial" android:centerX="0.15" android:centerY="0.2" android:gradientRadius="250dp" android:startColor="#D06FC7BB" android:endColor="#006FC7BB"/></shape></item>
  <item><shape><corners android:radius="28dp"/><gradient android:type="radial" android:centerX="0.92" android:centerY="0.92" android:gradientRadius="230dp" android:startColor="#CCEAAD8E" android:endColor="#00EAAD8E"/></shape></item>
</layer-list>\n`);
const paths = {
universal:'M3,3 L10,3 L10,10 L3,10 Z M14,3 L21,3 L21,10 L14,10 Z M3,14 L10,14 L10,21 L3,21 Z M14,14 L21,14 L21,21 L14,21 Z',
notes:'M5,3 L19,3 L19,21 L5,21 Z M9,8 L15,8 M9,12 L15,12 M9,16 L12,16',
reminders:'M6,11 C6,2 18,2 18,11 L18,16 L20,18 L4,18 L6,16 Z M10,21 L14,21',
timers:'M12,6 A8,8 0,1 0,12.01,6 M9,2 L15,2 M12,2 L12,6 M12,10 L12,14 L15,16',
alarms:'M12,5 A8,8 0,1 0,12.01,5 M12,9 L12,13 L15,15 M3,5 L6,2 M18,2 L21,5 M5,20 L3,22 M19,20 L21,22',
checklist:'M4,3 L20,3 L20,21 L4,21 Z M7,9 L9,11 L13,7 M8,16 L16,16',
habits:'M12,3 L14.5,9.5 L21,12 L14.5,14.5 L12,21 L9.5,14.5 L3,12 L9.5,9.5 Z',
clock:'M12,3 A9,9 0,1 0,12.01,3 M12,6 L12,12 L16,14',
battery:'M8,2 L16,2 M5,5 L19,5 L19,22 L5,22 Z M9,10 L15,10 M9,14 L15,14 M9,18 L13,18',
calendar:'M3,5 L21,5 L21,22 L3,22 Z M7,2 L7,8 M17,2 L17,8 M3,10 L21,10 M7,14 L9,14 M14,14 L16,14 M7,18 L9,18',
agenda:'M5,3 L19,3 L19,22 L5,22 Z M8,7 L16,7 M8,12 L10,12 M13,12 L16,12 M8,17 L10,17 M13,17 L16,17',
weather:'M9,5 L9,2 M3,8 L1,8 M4,3 L2,1 M14,3 L16,1 M8,13 A5,5 0,1 1,14,9 M5,20 C0,20 1,12 6,13 C8,6 17,8 17,13 C24,11 24,20 19,20 Z',
photos:'M3,3 L21,3 L21,21 L3,21 Z M4,18 L10,11 L15,16 L18,12 L21,16 M14,7 A1,1 0,1 0,14.1,7',
worldclock:'M12,2 A10,10 0,1 0,12.01,2 M2,12 L22,12 M12,2 C5,8 5,16 12,22 C19,16 19,8 12,2',
countdown:'M5,3 L19,3 M5,21 L19,21 M7,3 C7,10 10,9 12,12 C14,15 17,14 17,21 M17,3 C17,10 14,9 12,12 C10,15 7,14 7,21',
contacts:'M12,3 A4,4 0,1 0,12.01,3 M4,22 C4,12 20,12 20,22',
search:'M10,3 A7,7 0,1 0,10.01,3 M15,15 L22,22',
controls:'M3,7 L21,7 M8,3 L8,11 M3,17 L21,17 M16,13 L16,21',
music:'M9,17 L9,4 L21,2 L21,15 M9,8 L21,6 M9,17 C3,13 1,20 5,21 C9,22 9,18 9,17 M21,15 C15,11 13,18 17,19 C21,20 21,16 21,15'
};
const stockTitles={"stopwatch":"Stopwatch","compass":"Compass","recorder":"Voice memos","screentime":"Screen time","devicecare":"Device care","tracker":"Personal trackers","files":"Files","bookmarks":"Bookmarks","shortcuts":"App shortcuts","maps":"Saved places","translate":"Translate","breathing":"Breathing","news":"Headlines","stocks":"Market watchlist","service":"Connected app cards","tips":"Daily tips","animation":"Animated scenes","slideshow":"Photo slideshow","video":"Video carousel"};
const stockAliases={"stopwatch":"timers","compass":"worldclock","recorder":"music","screentime":"clock","devicecare":"battery","tracker":"habits","files":"notes","bookmarks":"notes","shortcuts":"universal","maps":"worldclock","translate":"search","breathing":"habits","news":"notes","stocks":"habits","service":"controls","tips":"notes","animation":"habits","slideshow":"photos","video":"music"};
for(const [key,source] of Object.entries(stockAliases))paths[key]=paths[source];
paths.animation='M12,4 A8,8 0,1 0,20,12 M8,8 C3,13 12,21 17,16 C22,11 13,3 8,8 M12,10 A2,2 0,1 0,12.01,10';
paths.slideshow='M3,7 L19,7 L19,22 L3,22 Z M7,3 L23,3 L23,18 M4,19 L9,13 L13,17 L16,13 L19,17';
paths.video='M3,5 L21,5 L21,20 L3,20 Z M10,9 L16,12.5 L10,16 Z';
for (const [name,path] of Object.entries(paths)) {
  const quick = ['timers','alarms','search','controls'].includes(name);
  const universal = name === 'universal';
  write(`drawable/ic_${name}.xml`, `<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#00000000" android:strokeColor="#C4D7B2" android:strokeWidth="1.7" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="${path}"/></vector>\n`);
  write(`xml/widget_${name}.xml`, `<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android" android:minWidth="250dp" android:minHeight="${quick?110:180}dp" android:minResizeWidth="${universal?230:180}dp" android:minResizeHeight="${universal||['animation','slideshow','video'].includes(name)?180:quick?110:130}dp" android:targetCellWidth="4" android:targetCellHeight="${universal?3:quick?1:2}" android:resizeMode="horizontal|vertical" android:updatePeriodMillis="1800000" android:initialLayout="@layout/${universal?'widget_universal':'preview_'+name}" android:previewLayout="@layout/${universal?'widget_universal':'preview_'+name}" android:configure="com.nook.widgets.WidgetConfigActivity" android:widgetFeatures="reconfigurable" android:widgetCategory="home_screen" android:description="@string/widget_${name}_description"/>\n`);
}
write('drawable/ic_controls.xml','<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#00000000" android:strokeColor="#C4D7B2" android:strokeWidth="1.7" android:strokeLineCap="round" android:pathData="M4,7 L20,7 M9,4 L9,10 M4,17 L20,17 M15,14 L15,20"/></vector>\n');
write('drawable/widget_touch.xml','<ripple xmlns:android="http://schemas.android.com/apk/res/android" android:color="#22FFFFFF"><item android:id="@android:id/mask"><shape><solid android:color="#FFFFFF"/><corners android:radius="16dp"/></shape></item></ripple>\n');
write('values/strings.xml', `<resources><string name="app_name">Nook</string>${Object.keys(paths).map(name=>`<string name="widget_${name}_description">${stockTitles[name]||({universal:'Switch between everyday tools inside one widget',notes:'A little space for your thoughts',reminders:'Your next reminders, at a glance',timers:'One tap for a timer that fits your day',alarms:'Your favorite alarm times, ready to set',checklist:'A small list. A lighter mind.',habits:'Small steps, every single day',clock:'Time, with a little breathing room',battery:'Phone charge and charging status',calendar:'Month or week calendar',agenda:'Upcoming events from your calendars',weather:'Conditions and forecast for your chosen city',photos:'Your favorite photo in a frame',worldclock:'Live clocks for your cities',countdown:'Days until your next occasion',contacts:'Favorite people ready to dial',search:'Search your favorite web provider',controls:'Useful settings and app shortcuts',music:'Active media and playback controls'})[name]}</string>`).join('')}</resources>\n`);
write('layout/widget.xml', `<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent">
<ImageView android:id="@+id/widget_surface" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitXY" android:src="@drawable/moss_surface" android:importantForAccessibility="no" android:contentDescription="@null"/>
<LinearLayout android:id="@+id/widget_root" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:padding="18dp">
  <LinearLayout android:id="@+id/widget_header" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical">
    <ImageView android:id="@+id/widget_icon" android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_notes" android:contentDescription="@null"/>
    <TextView android:id="@+id/widget_label" android:layout_width="0dp" android:layout_weight="1" android:layout_height="40dp" android:gravity="center_vertical" android:paddingStart="9dp" android:paddingEnd="4dp" android:text="QUICK NOTE" android:textColor="#E9F0E4" android:textSize="11sp" android:fontFamily="sans-serif-medium" android:letterSpacing="0.12"/>
    <TextView android:id="@+id/widget_settings" android:layout_width="40dp" android:layout_height="40dp" android:text="···" android:gravity="center" android:textSize="24sp" android:textColor="#E9F0E4" android:contentDescription="Customize widget"/>
  </LinearLayout>
  <TextView android:id="@+id/widget_title" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="A little space for your thoughts." android:textColor="#E9F0E4" android:textSize="22sp" android:fontFamily="sans-serif-medium" android:maxLines="2" android:ellipsize="end" android:layout_marginTop="4dp"/>
  <TextView android:id="@+id/widget_meta" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:textSize="11sp" android:textColor="#AEBFAE" android:maxLines="1" android:ellipsize="end" android:visibility="gone"/>
  <TextClock android:id="@+id/widget_time" android:layout_width="match_parent" android:layout_height="wrap_content" android:format12Hour="h:mm" android:format24Hour="HH:mm" android:textSize="52sp" android:textColor="#E9F0E4" android:fontFamily="sans-serif-light" android:visibility="gone"/>
  <TextClock android:id="@+id/widget_date" android:layout_width="match_parent" android:layout_height="wrap_content" android:format12Hour="EEEE, d MMM" android:format24Hour="EEEE, d MMM" android:textSize="14sp" android:textColor="#AEBFAE" android:visibility="gone"/>
  <TextView android:id="@+id/widget_body" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:text="Tap to write something worth keeping." android:textColor="#AEBFAE" android:textSize="14sp" android:maxLines="4" android:ellipsize="end"/>
  ${[0,1,2].map(n=>`<TextView android:id="@+id/widget_row${n}" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="44dp" android:gravity="center_vertical" android:textSize="14sp" android:maxLines="1" android:ellipsize="end" android:textColor="#E9F0E4" android:visibility="gone"/>`).join('\n  ')}
  <ProgressBar android:id="@+id/widget_progress" style="?android:attr/progressBarStyleHorizontal" android:layout_width="match_parent" android:layout_height="6dp" android:layout_marginTop="12dp" android:visibility="gone"/>
  <LinearLayout android:id="@+id/widget_actions" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:gravity="center_vertical" android:visibility="gone">
    ${[0,1,2].map(n=>`<FrameLayout android:layout_width="0dp" android:layout_weight="1" android:layout_height="48dp" android:layout_marginEnd="${n===2?0:8}dp">
      <ImageView android:id="@+id/widget_action_bg${n}" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitXY" android:src="@drawable/moss_chip" android:importantForAccessibility="no" android:contentDescription="@null"/>
      <TextView android:id="@+id/widget_action${n}" android:layout_width="match_parent" android:layout_height="match_parent" android:gravity="center" android:textSize="14sp" android:fontFamily="sans-serif-medium" android:textColor="#E9F0E4" android:maxLines="1" android:text="${[1,5,10][n]} min"/>
    </FrameLayout>`).join('\n    ')}
  </LinearLayout>
</LinearLayout>
</FrameLayout>\n`);
const baseWidget=fs.readFileSync(new URL('layout/widget.xml',root),'utf8');
const previewCopy={notes:['Quick notes','A thought worth keeping?','Permanent or temporary notes.'],reminders:['Gentle reminders','Your next little nudge.','Your upcoming reminders, in view.'],timers:['Quick timers','Time for a little focus.','Three timer shortcuts, ready to set.'],alarms:['Quick alarms','A good day starts here.','Three favorite alarm times.'],checklist:['Daily checklist','A little less to remember.','Check off your everyday tasks.'],habits:['Daily habits','One small step.','Your daily goal and progress.'],clock:['Day clock','Time, with a little space.','A live clock and date.'],battery:['Phone battery','Your charge, in view.','Phone percentage and charging status.'],calendar:['Month calendar','A month of possibilities.','Your month or current week.'],agenda:['Upcoming agenda','Make space for your day.','Read-only events from your calendars.'],weather:['Local weather','Your city, your forecast.','Current conditions or a three-day forecast.'],photos:['Photo frame','A favorite little moment.','Choose a photo to frame.'],worldclock:['World clocks','Keep your cities closer.','One or three live city clocks.'],countdown:['Event countdown','Something to look forward to.','Count the days to an occasion.'],contacts:['Favorite contacts','Your people, close by.','One or three named dialer shortcuts.'],search:['Web search','Your next idea starts here.','Search with your chosen provider.'],controls:['Quick controls','Shortcuts, within reach.','Phone settings and everyday apps.'],music:['Now playing','A little room for music.','Artwork and controls for your active player.']};
for(const [kind,title]of Object.entries(stockTitles))previewCopy[kind]=[title,title,'Choose your options to get started.'];
for(const [kind,[label,title,body]]of Object.entries(previewCopy))write(`layout/preview_${kind}.xml`,baseWidget.replace('@drawable/ic_notes',`@drawable/ic_${kind}`).replace('android:text="QUICK NOTE"',`android:text="${label.toUpperCase()}"`).replace('A little space for your thoughts.',title).replace('Tap to write something worth keeping.',body));
const extraViews=`<Chronometer android:id="@+id/extra_chrono" android:layout_width="match_parent" android:layout_height="wrap_content" android:textSize="48sp" android:fontFamily="sans-serif-light" android:visibility="gone"/><ImageView android:id="@+id/tool_visual" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:layout_marginTop="6dp" android:scaleType="fitCenter" android:visibility="gone"/>
<LinearLayout android:id="@+id/tool_clocks" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:visibility="gone">
${[0,1,2].map(n=>`<LinearLayout android:id="@+id/tool_clockrow${n}" android:layout_width="match_parent" android:layout_height="38dp" android:gravity="center_vertical"><TextView android:id="@+id/tool_city${n}" android:layout_width="0dp" android:layout_weight="1" android:layout_height="match_parent" android:gravity="center_vertical" android:textSize="12sp" android:maxLines="1" android:ellipsize="end"/><TextClock android:id="@+id/tool_clock${n}" android:layout_width="wrap_content" android:layout_height="match_parent" android:gravity="center_vertical" android:format12Hour="HH:mm" android:format24Hour="HH:mm" android:textSize="24sp" android:fontFamily="sans-serif-light" android:maxLines="1"/></LinearLayout>`).join('')}
</LinearLayout>`;
write('layout/widget_tools.xml',baseWidget.replace('<TextView android:id="@+id/widget_body"',extraViews+'\n<TextView android:id="@+id/widget_body"'));
write('layout/widget_photo.xml',fs.readFileSync(new URL('layout/widget_tools.xml',root),'utf8').replace('android:scaleType="fitCenter"','android:scaleType="centerCrop"'));
for(const base of ['widget_tools','widget_photo'])write(`layout/${base}_compact.xml`,fs.readFileSync(new URL(`layout/${base}.xml`,root),'utf8').replaceAll('android:layout_height="40dp"','android:layout_height="32dp"'));
function surfaceImage(id) {return `<ImageView android:id="@+id/${id}" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitXY" android:src="@drawable/moss_chip" android:importantForAccessibility="no" android:contentDescription="@null"/>`;}
const names=['Notes','Reminders','Timers','Alarms','Checklist','Habits','Library','Customize','More tools'];
write('layout/widget_universal.xml',`<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent">
  <ImageView android:id="@id/widget_surface" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitXY" android:src="@drawable/moss_surface" android:importantForAccessibility="no" android:contentDescription="@null"/>
  <LinearLayout android:id="@+id/universal_root" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:padding="18dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="40dp" android:gravity="center_vertical">
      <FrameLayout android:layout_width="96dp" android:layout_height="match_parent">${surfaceImage('hub_menu_bg')}<TextView android:id="@+id/hub_menu" android:layout_width="match_parent" android:layout_height="match_parent" android:gravity="center" android:text="▦  All tools" android:textColor="#E9F0E4" android:textSize="12sp" android:fontFamily="sans-serif-medium" android:background="@drawable/widget_touch" android:contentDescription="Show all widget tools"/></FrameLayout>
      <TextView android:id="@+id/hub_label" android:layout_width="0dp" android:layout_weight="1" android:layout_height="match_parent" android:paddingStart="8dp" android:paddingEnd="4dp" android:gravity="center_vertical" android:text="Nook toolbox" android:textSize="13sp" android:textColor="#E9F0E4" android:fontFamily="sans-serif-medium" android:maxLines="1" android:ellipsize="end"/>
      <TextView android:id="@+id/hub_settings" android:layout_width="40dp" android:layout_height="40dp" android:text="···" android:gravity="center" android:textColor="#E9F0E4" android:textSize="24sp" android:contentDescription="Customize universal widget"/>
    </LinearLayout>
    <TextView android:id="@+id/hub_heading" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="10dp" android:text="Your everyday toolbox." android:textSize="20sp" android:fontFamily="sans-serif-medium" android:textColor="#E9F0E4" android:maxLines="1" android:ellipsize="end"/>
    <TextView android:id="@+id/hub_description" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="4dp" android:text="Choose a view to use right here." android:textSize="12sp" android:textColor="#AEBFAE" android:maxLines="1" android:ellipsize="end"/>
    <LinearLayout android:id="@+id/hub_grid" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:orientation="vertical" android:layout_marginTop="12dp">
      ${[0,1,2].map(row=>`<LinearLayout android:id="@+id/hub_row${row}" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:minHeight="56dp" android:layout_marginBottom="${row===2?0:8}dp">
        ${[0,1,2].map(col=>{let n=row*3+col;return `<FrameLayout android:layout_width="0dp" android:layout_weight="1" android:layout_height="match_parent" android:layout_marginEnd="${col===2?0:8}dp">
          ${surfaceImage('hub_bg'+n)}
          <LinearLayout android:id="@+id/hub_tile${n}" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:gravity="center" android:background="@drawable/widget_touch" android:importantForAccessibility="yes">
            <ImageView android:id="@+id/hub_icon${n}" android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_universal" android:contentDescription="@null" android:importantForAccessibility="no"/>
            <TextView android:id="@+id/hub_text${n}" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="5dp" android:text="${names[n]}" android:textColor="#E9F0E4" android:textSize="11sp" android:fontFamily="sans-serif-medium" android:gravity="center" android:maxLines="1" android:ellipsize="end" android:importantForAccessibility="no"/>
          </LinearLayout></FrameLayout>`;}).join('')}
      </LinearLayout>`).join('')}
    </LinearLayout>
    <FrameLayout android:id="@+id/hub_content" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:layout_marginTop="10dp" android:visibility="gone"/>
    <LinearLayout android:id="@+id/hub_nav" android:layout_width="match_parent" android:layout_height="40dp" android:layout_marginTop="10dp" android:visibility="gone">
      ${[['prev','‹ Previous','Previous widget view'],['count','1 / 7','Show all tools'],['next','Next ›','Next widget view']].map(([id,label,description],n)=>`<FrameLayout android:layout_width="0dp" android:layout_weight="1" android:layout_height="match_parent" android:layout_marginEnd="${n===2?0:8}dp">${surfaceImage('hub_'+id+'_bg')}<TextView android:id="@+id/hub_${id}" android:layout_width="match_parent" android:layout_height="match_parent" android:text="${label}" android:gravity="center" android:textColor="#E9F0E4" android:textSize="11sp" android:fontFamily="sans-serif-medium" android:contentDescription="${description}" android:background="@drawable/widget_touch"/></FrameLayout>`).join('')}
    </LinearLayout>
  </LinearLayout>
</FrameLayout>\n`);
const universalLayout = fs.readFileSync(new URL('layout/widget_universal.xml',root),'utf8');
write('layout/widget_universal_compact.xml',universalLayout
  .replaceAll('android:layout_height="40dp"','android:layout_height="32dp"')
  .replaceAll('android:layout_marginTop="10dp"','android:layout_marginTop="6dp"')
  .replaceAll('android:minHeight="56dp"','android:minHeight="32dp"')
  .replaceAll('android:layout_marginBottom="8dp"','android:layout_marginBottom="4dp"')
  .replaceAll('android:layout_width="20dp" android:layout_height="20dp"','android:layout_width="16dp" android:layout_height="16dp"')
  .replaceAll('android:layout_marginTop="5dp"','android:layout_marginTop="3dp"')
  .replaceAll('android:textSize="11sp"','android:textSize="10sp"'));
// XML font families are supported by RemoteViews back to Android 9. Typeface
// setters are deliberately avoided because TextView does not expose them remotely.
// The platform-inspired families use a content-first hierarchy, with a small
// overlay customize target instead of the original app-card toolbar.
const replicaHeader=`<LinearLayout android:id="@id/widget_header" android:layout_width="match_parent" android:layout_height="28dp" android:gravity="center_vertical" android:paddingEnd="28dp"><ImageView android:id="@id/widget_icon" android:layout_width="16dp" android:layout_height="16dp" android:src="@drawable/ic_notes" android:contentDescription="@null"/><TextView android:id="@id/widget_label" android:layout_width="0dp" android:layout_weight="1" android:layout_height="match_parent" android:paddingStart="6dp" android:gravity="center_vertical" android:textSize="13sp" android:fontFamily="sans-serif-medium" android:maxLines="1" android:ellipsize="end"/></LinearLayout>`;
const replicaSettings=`<TextView android:id="@id/widget_settings" android:layout_width="40dp" android:layout_height="40dp" android:layout_gravity="top|end" android:layout_margin="4dp" android:text="···" android:textSize="18sp" android:gravity="center" android:fontFamily="sans-serif" android:contentDescription="Customize widget"/>`;
const clockFaces=['apple','dot','pixel','samsung','glassdark','glasslight'].map(face=>`<AnalogClock android:id="@+id/replica_clock_${face}" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:dial="@drawable/dial_${face}" android:hand_hour="@drawable/hand_${face}_hour" android:hand_minute="@drawable/hand_${face}_minute" android:visibility="gone" android:contentDescription="Live analog clock"/>`).join('');
let replica=baseWidget;
const headerStart=replica.indexOf('  <LinearLayout android:id="@+id/widget_header"');
const headerEnd=replica.indexOf('</LinearLayout>',headerStart)+'</LinearLayout>'.length;
replica=replica.slice(0,headerStart)+replicaHeader+replica.slice(headerEnd);
replica=replica.replace('<TextClock android:id="@+id/widget_time"','<ImageView android:id="@+id/replica_dial" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:scaleType="fitCenter" android:visibility="gone" android:contentDescription="Timer presets"/>'+clockFaces+'<TextClock android:id="@+id/widget_time"')
  .replace('<LinearLayout android:id="@+id/widget_actions"','<LinearLayout android:id="@+id/replica_spacer" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:orientation="vertical"/><LinearLayout android:id="@+id/widget_actions"')
  .replace(/\n<\/LinearLayout>\n<\/FrameLayout>/,`\n</LinearLayout>\n${replicaSettings}\n</FrameLayout>`)
  .replaceAll('android:minHeight="44dp"','android:minHeight="36dp"')
  .replaceAll('android:layout_marginTop="12dp"','android:layout_marginTop="8dp"');
write('layout/replica_widget.xml',replica);
write('layout/replica_notes.xml',replica.replace(/(<TextView android:id="@\+id\/widget_body"[^>]*?)android:layout_height="wrap_content"/,'$1android:layout_height="0dp" android:layout_weight="1"'));
let replicaTools=replica.replace('<TextView android:id="@+id/widget_body"',extraViews+'\n<TextView android:id="@+id/widget_body"');
const toolMeta=replicaTools.match(/<TextView android:id="@\+id\/widget_meta"[^>]*\/>/)[0];
replicaTools=replicaTools.replace(toolMeta,'').replace('<LinearLayout android:id="@+id/replica_spacer"',toolMeta+'<LinearLayout android:id="@+id/replica_spacer"');
write('layout/replica_tools.xml',replicaTools);
write('layout/replica_tools_compact.xml',replicaTools);
const musicVisual=replicaTools.match(/<ImageView android:id="@\+id\/tool_visual"[^>]*\/>/)[0],musicTitle=replicaTools.match(/<TextView android:id="@\+id\/widget_title"[^>]*\/>/)[0],musicBody=replicaTools.match(/<TextView android:id="@\+id\/widget_body"[^>]*\/>/)[0];
let replicaMusic=replicaTools.replace(musicVisual,'').replace(musicTitle,'').replace(musicBody,'');
replicaMusic=replicaMusic.replace(replicaHeader,replicaHeader+`<LinearLayout android:id="@+id/replica_music_row" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:gravity="center_vertical">${musicVisual.replace('android:layout_width="match_parent"','android:layout_width="96dp" android:layout_marginEnd="14dp"').replace('android:layout_height="0dp" android:layout_weight="1"','android:layout_height="match_parent"').replace('android:scaleType="fitCenter"','android:scaleType="centerCrop"')}<LinearLayout android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:orientation="vertical">${musicTitle}${musicBody}</LinearLayout></LinearLayout>`);
write('layout/replica_music.xml',replicaMusic);
let searchSlot=0;
const replicaSearch=replicaTools.replace(/<FrameLayout android:layout_width="0dp" android:layout_weight="1"/g,tag=>tag.replace('layout_weight="1"',`layout_weight="${searchSlot++===0?4:1}"`)).replace('android:layout_gravity="top|end"','android:layout_gravity="center_vertical|end"');
write('layout/replica_search.xml',replicaSearch);
// Photos occupy the complete frame; the caption and controls overlay the image.
const photoVisual=replicaTools.match(/<ImageView android:id="@\+id\/tool_visual"[^>]*\/>/)[0];
let replicaPhoto=replicaTools.replace(photoVisual,'').replace('<LinearLayout android:id="@+id/widget_root"',photoVisual.replace('android:layout_height="0dp" android:layout_weight="1"','android:layout_height="match_parent"').replace('android:layout_marginTop="6dp"','').replace('android:scaleType="fitCenter"','android:scaleType="fitXY"')+'<LinearLayout android:id="@+id/widget_root"');
replicaPhoto=replicaPhoto.replace('<TextView android:id="@+id/widget_title"','<LinearLayout android:id="@+id/replica_photo_space" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:orientation="vertical"/><TextView android:id="@+id/widget_title"');
write('layout/replica_photo.xml',replicaPhoto);write('layout/replica_photo_compact.xml',replicaPhoto);
// Original vector dials and hands; AnalogClock keeps time on the launcher.
// Build a rotationally symmetric face around the hands' (100, 100) pivot.
// Cubic tangents keep the lobes smooth without shifting the visible centroid.
function pixelDialPath(){
  const count=64,step=Math.PI*2/count;
  function point(angle){const r=86+7*Math.cos(8*angle),dr=-56*Math.sin(8*angle),co=Math.cos(angle),si=Math.sin(angle);return {x:100+r*co,y:100+r*si,dx:dr*co-r*si,dy:dr*si+r*co};}
  const number=n=>n.toFixed(3);
  let previous=point(-Math.PI/2),path=`M${number(previous.x)},${number(previous.y)}`;
  for(let n=1;n<=count;n++){const next=point(-Math.PI/2+n*step);path+=` C${number(previous.x+previous.dx*step/3)},${number(previous.y+previous.dy*step/3)} ${number(next.x-next.dx*step/3)},${number(next.y-next.dy*step/3)} ${number(next.x)},${number(next.y)}`;previous=next;}
  return path+' Z';
}
for(const [face,fill,ink,accent]of [['apple','#FFFFFF','#202124','#E74036'],['dot','#F5F5F1','#181818','#D52822'],['pixel','#F5F2E6','#2A3B05','#9ABF20'],['samsung','#FFFFFF','#202124','#2775F5'],['glassdark','#00000000','#FFFFFF','#FFFFFF'],['glasslight','#00000000','#151C27','#151C27']]){
  let dial='';
  if(face==='pixel')dial=`<path android:fillColor="${fill}" android:pathData="${pixelDialPath()}"/>`;
  else{dial=`<path android:fillColor="${fill}" android:pathData="M100,4 A96,96 0,1 0,100.01,4"/>`;for(let n=0;n<60;n++){const a=n*Math.PI/30,r=n%5===0?76:87;dial+=`<path android:strokeColor="${ink}" android:strokeWidth="${n%5===0?2.5:1}" android:strokeLineCap="round" android:pathData="M${(100+Math.sin(a)*r).toFixed(2)},${(100-Math.cos(a)*r).toFixed(2)} L${(100+Math.sin(a)*92).toFixed(2)},${(100-Math.cos(a)*92).toFixed(2)}"/>`;}}
  write(`drawable/dial_${face}.xml`,`<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="200dp" android:height="200dp" android:viewportWidth="200" android:viewportHeight="200">${dial}</vector>`);
  for(const [hand,length,width]of [['hour',49,face==='pixel'?10:6],['minute',74,face==='pixel'?5:3]])write(`drawable/hand_${face}_${hand}.xml`,`<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="200dp" android:height="200dp" android:viewportWidth="200" android:viewportHeight="200"><path android:strokeColor="${face==='samsung'?accent:ink}" android:strokeWidth="${width}" android:strokeLineCap="round" android:pathData="M100,110 L100,${100-length}"/><path android:fillColor="${accent}" android:pathData="M100,96 A4,4 0,1 0,100.01,96"/></vector>`);
}

// Launcher-native motion: only supported RemoteViews layouts and views.
const motionBase=fs.readFileSync(new URL('layout/widget_tools.xml',root),'utf8');
const motionVisual=motionBase.match(/<ImageView android:id="@\+id\/tool_visual"[^>]*\/>/)[0];
const motionStage='<FrameLayout android:id="@+id/media_stage" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:layout_marginTop="4dp">'+motionVisual+'</FrameLayout>';
write('layout/widget_motion.xml',motionBase.replace(motionVisual,motionStage));
write('layout/widget_motion_compact.xml',motionBase.replace(motionVisual,motionStage).replaceAll('android:layout_height="48dp"','android:layout_height="36dp"').replaceAll('android:layout_height="40dp"','android:layout_height="32dp"').replaceAll('android:layout_marginTop="12dp"','android:layout_marginTop="8dp"'));
write('layout/motion_frame.xml','<ImageView xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/motion_image" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitCenter" android:contentDescription="@null" android:importantForAccessibility="no"/>');
for(const fade of [false,true])write('layout/'+(fade?'motion_slideshow':'motion_sequence')+'.xml','<ViewFlipper xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/motion_flipper" android:layout_width="match_parent" android:layout_height="match_parent" android:autoStart="true" android:importantForAccessibility="noHideDescendants" android:flipInterval="200"'+(fade?' android:inAnimation="@anim/motion_fade_in" android:outAnimation="@anim/motion_fade_out"':'')+'/>');
write('anim/motion_fade_in.xml','<alpha xmlns:android="http://schemas.android.com/apk/res/android" android:duration="600" android:fromAlpha="0" android:toAlpha="1"/>');
write('anim/motion_fade_out.xml','<alpha xmlns:android="http://schemas.android.com/apk/res/android" android:duration="600" android:fromAlpha="1" android:toAlpha="0"/>');
write('values/media_ids.xml','<resources><item type="id" name="video_player"/></resources>');

// Full silent video uses a lazy frame collection, preserving the source aspect ratio.
for(const [name,path] of Object.entries({video_previous:'M5,5 L5,19 M19,5 L8,12 L19,19 Z',video_next:'M19,5 L19,19 M5,5 L16,12 L5,19 Z',video_play:'M7,4 L20,12 L7,20 Z',video_pause:'M7,4 L7,20 M17,4 L17,20',video_options:'M4,12 A1,1 0,1 0,4.1,12 M12,12 A1,1 0,1 0,12.1,12 M20,12 A1,1 0,1 0,20.1,12'}))write('drawable/ic_'+name+'.xml','<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="2" android:strokeLineJoin="round" android:strokeLineCap="round" android:pathData="'+path+'"/></vector>');
const videoControls=[0,1,2].map(n=>'<FrameLayout android:layout_width="44dp" android:layout_height="44dp" android:layout_marginEnd="'+(n===2?0:6)+'dp"><ImageView android:id="@+id/widget_action_bg'+n+'" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitXY" android:importantForAccessibility="no"/><ImageView android:id="@+id/widget_action'+n+'" android:layout_width="match_parent" android:layout_height="match_parent" android:padding="11dp" android:scaleType="fitCenter" android:background="@drawable/widget_touch" android:contentDescription="Video control"/></FrameLayout>').join('');
const videoLayout=(live,interval)=>'<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent"><ImageView android:id="@+id/widget_surface" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitXY" android:importantForAccessibility="no"/><LinearLayout android:id="@+id/widget_root" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical"><FrameLayout android:id="@+id/media_stage" android:layout_width="match_parent" android:layout_height="match_parent">'+(live?'<AdapterViewFlipper android:id="@+id/video_flipper" android:layout_width="match_parent" android:layout_height="match_parent" android:autoStart="true" android:flipInterval="'+interval+'" android:inAnimation="@animator/video_instant_in" android:outAnimation="@animator/video_instant_out" android:importantForAccessibility="noHideDescendants"/>':'<ImageView android:id="@+id/video_poster" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitCenter" android:importantForAccessibility="no"/>')+'<FrameLayout android:id="@+id/video_overlay" android:layout_width="match_parent" android:layout_height="match_parent"><LinearLayout android:id="@+id/video_text" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingEnd="38dp" android:orientation="vertical"><LinearLayout android:id="@+id/widget_header" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical"><TextView android:id="@+id/widget_label" android:layout_width="match_parent" android:layout_height="wrap_content" android:textSize="10sp" android:textColor="#FFFFFFFF" android:maxLines="1" android:ellipsize="end"/></LinearLayout><TextView android:id="@+id/widget_title" android:layout_width="match_parent" android:layout_height="wrap_content" android:textSize="14sp" android:textColor="#FFFFFFFF" android:fontFamily="sans-serif-medium" android:maxLines="1" android:ellipsize="end" android:shadowColor="#CC000000" android:shadowRadius="3"/><TextView android:id="@+id/widget_meta" android:layout_width="match_parent" android:layout_height="wrap_content" android:textSize="10sp" android:textColor="#FFFFFFFF" android:maxLines="1" android:ellipsize="end" android:shadowColor="#CC000000" android:shadowRadius="3"/></LinearLayout><ImageView android:id="@+id/widget_settings" android:layout_width="36dp" android:layout_height="36dp" android:layout_gravity="top|end" android:padding="6dp" android:background="@drawable/widget_touch" android:contentDescription="Video options"/><LinearLayout android:id="@+id/widget_actions" android:layout_width="wrap_content" android:layout_height="44dp" android:layout_gravity="bottom|center_horizontal" android:orientation="horizontal">'+videoControls+'</LinearLayout></FrameLayout></FrameLayout></LinearLayout></FrameLayout>';
for(const [name,live,interval] of [['still',false,200],['live',true,200],['live_slow',true,333],['live_fast',true,125]])write('layout/widget_video_'+name+'.xml',videoLayout(live,interval));
write('layout/widget_video_still_crop.xml',videoLayout(false,200).replace('android:scaleType="fitCenter"','android:scaleType="centerCrop"'));
for(const crop of [false,true])write('layout/video_frame_'+(crop?'crop':'fit')+'.xml','<ImageView xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/video_frame_image" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="'+(crop?'centerCrop':'fitCenter')+'" android:importantForAccessibility="no"/>');
write('animator/video_instant_in.xml','<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android" android:propertyName="alpha" android:valueFrom="1" android:valueTo="1" android:valueType="floatType" android:duration="0"/>');
write('animator/video_instant_out.xml','<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android" android:propertyName="alpha" android:valueFrom="0" android:valueTo="0" android:valueType="floatType" android:duration="0"/>');
for(const [name,w,h,cw,ch] of [['video',250,110,4,2],['video_portrait',130,260,2,3],['video_square',200,200,3,3]])write('xml/widget_'+name+'.xml','<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android" android:minWidth="'+w+'dp" android:minHeight="'+h+'dp" android:minResizeWidth="96dp" android:minResizeHeight="96dp" android:targetCellWidth="'+cw+'" android:targetCellHeight="'+ch+'" android:resizeMode="horizontal|vertical" android:updatePeriodMillis="1800000" android:initialLayout="@layout/preview_video" android:previewLayout="@layout/preview_video" android:configure="com.nook.widgets.WidgetConfigActivity" android:widgetFeatures="reconfigurable" android:widgetCategory="home_screen" android:description="@string/widget_video_description"/>');

for(const base of ['widget_music_bar','widget_music_narrow','widget_music_card','widget_music_compact','widget_music_cover','widget_video_still','widget_video_still_crop','widget_video_live','widget_video_live_slow','widget_video_live_fast','widget_motion','widget_motion_compact','widget','widget_tools','widget_tools_compact','widget_photo','widget_photo_compact','widget_universal','widget_universal_compact','replica_widget','replica_notes','replica_tools','replica_tools_compact','replica_photo','replica_photo_compact','replica_search','replica_music']){
  const original=fs.readFileSync(new URL(`layout/${base}.xml`,root),'utf8');
  for(let font=1;font<=4;font++){
    const xml=original.replace(/<(TextView|TextClock|Chronometer)\b[^>]*\/>/g,tag=>{
      const display=/\/(widget_title|widget_time|widget_label|hub_heading|hub_label|extra_chrono|tool_clock[0-2])"/.test(tag);
      const settings=/\/(widget_settings|hub_settings)"/.test(tag);
      const family=settings?'sans-serif':font===1?(display?'sans-serif-medium':'sans-serif'):font===2?'monospace':font===3?(display?'serif':'sans-serif'):(display?'@font/nook_dots':'monospace');
      return tag.includes('android:fontFamily=')?tag.replace(/android:fontFamily="[^"]*"/,`android:fontFamily="${family}"`):tag.replace('/>',` android:fontFamily="${family}"/>`);
    });
    write(`layout/${base}_font${font}.xml`,xml);
  }
}

console.log('Generated widget layouts, metadata, icons, six palettes and four surfaces.');
