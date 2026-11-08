package kr.travel.hitokoto;
import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import java.io.*;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(246,243,236),GREEN=Color.rgb(29,85,69),INK=Color.rgb(30,43,35),MUTED=Color.rgb(105,108,97),LINE=Color.rgb(213,213,197),SAGE=Color.rgb(229,234,216);
    private LinearLayout root,phraseBox,header,support,rightPane;
    private SwipePages listing;
    private TextView play,pageHint;
    private EditText searchField;
    private Typeface icons;
    private final Map<String,Bitmap> images=new HashMap<>();
    private android.content.SharedPreferences prefs;
    private Set<String> favorites;
    private Speech speech;
    private FreeInputEngine freeInput;
    private FreeInputEngine.Result manualResult;
    private EditText manualField;
    private TextView manualJapaneseView,manualReadingView,manualStateView,manualCount,manualTranslate;
    private LinearLayout manualOutput;
    private String manualDraft="",manualJapanese="",manualReading="",manualStatus="한글을 입력하고 번역하거나 바로 말해 보세요.";
    private boolean manualBusy=false;
    private TextView manualDownload,manualStar,manualSlow,manualLarge;
    private final List<Phrase> savedManual=new ArrayList<>();
    private static final class CartLine {final Phrase phrase;int number;CartLine(Phrase phrase,int number){this.phrase=phrase;this.number=number;}}
    private static final int CART_MAX=10;
    private final List<CartLine> cart=new ArrayList<>();
    private TextView cartButton,cartSpeak;

    private Phrase selected;
    private int count=3,tab=0,page=0,mode=0;
    private boolean large=false,searchOpen=false,openKeyboard=false,compact=false,wide=false;
    private static final String FIRST_ORDER=OsakaContent.ORDER_SCENES[0][0],FIRST_QUESTION=OsakaContent.QUESTION_SCENES[0][0];
    private String orderCategory=FIRST_ORDER,questionCategory=FIRST_QUESTION,query="";
    private int pageSize=4;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        icons=Typeface.createFromAsset(getAssets(),"MaterialIcons.ttf");prefs=getSharedPreferences("travel",MODE_PRIVATE);
        favorites=new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));loadSavedManual();loadCart();selected=lookup(prefs.getString("selected","beer"));if(selected==null)selected=Catalog.ALL.get(0);
        count=Quantity.clamp(prefs.getInt("count",3));tab=Math.max(0,Math.min(3,prefs.getInt("tab",0)));mode=Math.max(0,Math.min(2,prefs.getInt("intent-mode",0)));
        orderCategory=prefs.getString("order-category",FIRST_ORDER);questionCategory=prefs.getString("question-category",FIRST_QUESTION);
        if(!categories(true).contains(orderCategory))orderCategory=FIRST_ORDER;if(!categories(false).contains(questionCategory))questionCategory=FIRST_QUESTION;
        freeInput=new FreeInputEngine(this);manualDraft=prefs.getString("input-draft","");manualResult=freeInput.cached(manualDraft.trim());if(manualResult!=null){manualJapanese=manualResult.japanese;manualReading=manualResult.reading;manualStatus="준비된 문장을 바로 말할 수 있어요.";}
        ensureSelection();render();speech=new Speech(this,this::updatePlay);updatePlay();
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,float size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setIncludeFontPadding(false);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable shape(int color,float radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(stroke==GREEN?2:1),stroke);return d;}
    private void click(View view,int color,float radius,int border,Runnable action){view.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(35,29,85,69)),shape(color,radius,border),null));view.setClickable(true);view.setFocusable(true);view.setOnClickListener(v->action.run());}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void gap(LinearLayout target,int height){View v=new View(this);target.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
    private void divider(LinearLayout target){View v=new View(this);v.setBackgroundColor(LINE);target.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    private TextView icon(String glyph,int size,int color){TextView t=text(glyph,size,color,false);t.setTypeface(icons);t.setGravity(Gravity.CENTER);return t;}
    private TextView iconButton(String glyph,String label,Runnable action){TextView t=icon(glyph,25,GREEN);t.setContentDescription(label);click(t,Color.TRANSPARENT,24,0,action);t.setMinWidth(dp(48));t.setMinHeight(dp(48));return t;}

    private void save(){
        if(prefs==null||selected==null)return;
        android.content.SharedPreferences.Editor e=prefs.edit().putString("selected",selected.id).putInt("count",count).putInt("tab",tab).putInt("intent-mode",mode).putString("input-draft",manualDraft).putString("order-category",orderCategory).putString("question-category",questionCategory);
        if(selected.order)e.putInt("quantity-"+selected.id,count);e.apply();
    }
    private void keyboardOff(){View focus=getCurrentFocus();if(focus!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);focus.clearFocus();}}
    private List<String> categories(boolean order){List<String> names=new ArrayList<>();for(String[] scene:order?OsakaContent.ORDER_SCENES:OsakaContent.QUESTION_SCENES)names.add(scene[0]);return names;}
    private TextView action(String label,Runnable run){TextView b=text(label,16,GREEN,true);b.setPadding(dp(12),dp(9),dp(12),dp(9));b.setGravity(Gravity.CENTER);b.setMinHeight(dp(48));click(b,BG,18,LINE,run);return b;}
    private void render(){
        compact=getResources().getConfiguration().screenHeightDp<650;wide=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;rightPane=null;
        listing=null;phraseBox=null;play=null;support=null;pageHint=null;manualField=null;manualOutput=null;cartButton=null;
        pageSize=tab==0?(compact?2:4):(compact?1:2);
        root=column();root.setFocusableInTouchMode(true);root.setBackgroundColor(BG);root.setPadding(dp(16),dp(6),dp(16),dp(6));
        FrameLayout safe=new FrameLayout(this);safe.setBackgroundColor(BG);safe.addView(root,new FrameLayout.LayoutParams(-1,-1));
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        safe.setOnApplyWindowInsetsListener((v,insets)->{
            int left,top,right,bottom;boolean ime;
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());android.graphics.Insets keyboard=insets.getInsets(WindowInsets.Type.ime());left=bars.left;top=bars.top;right=bars.right;bottom=Math.max(bars.bottom,keyboard.bottom);ime=insets.isVisible(WindowInsets.Type.ime());}
            else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();ime=bottom>dp(150);}
            v.setPadding(left,top,right,bottom);keyboardLayout(ime&&(searchOpen||tab==3));return insets.consumeSystemWindowInsets();
        });
        setContentView(safe);
        header=row();ImageView logo=new ImageView(this);logo.setImageBitmap(bitmap("brand-mark"));logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams mark=new LinearLayout.LayoutParams(dp(37),dp(40));mark.rightMargin=dp(8);header.addView(logo,mark);
        LinearLayout brand=column();brand.addView(text("여행 한마디",12,GREEN,true));gap(brand,3);TextView title=text(tab==0?"무엇을 주문할까요?":tab==1?"무엇이 궁금하세요?":tab==2?"저장한 여행 한마디":"어떤 말을 전할까요?",compact?21:24,INK,true);title.setSingleLine();title.setAutoSizeTextTypeUniformWithConfiguration(18,compact?21:24,1,android.util.TypedValue.COMPLEX_UNIT_SP);brand.addView(title);header.addView(brand,new LinearLayout.LayoutParams(0,-1,1));header.addView(iconButton("\ue8b8","앱 정보",this::settings),new LinearLayout.LayoutParams(dp(44),-1));root.addView(header,new LinearLayout.LayoutParams(-1,dp(compact?44:52)));gap(root,compact?4:8);
        LinearLayout tabs=row();tabs.setPadding(dp(3),dp(3),dp(3),dp(3));tabs.setBackground(shape(BG,25,LINE));String[] labels={"주문","질문","즐겨찾기","입력하기"};
        for(int i=0;i<4;i++){final int position=i;TextView b=text(labels[i],15,tab==i?Color.WHITE:MUTED,true);b.setGravity(Gravity.CENTER);b.setSingleLine();b.setAutoSizeTextTypeUniformWithConfiguration(12,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);b.setSelected(tab==i);click(b,tab==i?GREEN:BG,23,0,()->{keyboardOff();if(freeInput!=null){if(position==3)freeInput.cancel();else freeInput.pause();}if(manualBusy)manualStatus="말하기를 누르면 준비를 이어가요.";manualBusy=false;if(speech!=null)speech.stop();tab=position;page=0;query="";searchOpen=false;ensureSelection();save();render();});tabs.addView(b,new LinearLayout.LayoutParams(0,-1,1));}root.addView(tabs,new LinearLayout.LayoutParams(-1,dp(compact?40:44)));gap(root,compact?6:8);
        if(tab==3){buildManual(safe);return;}
        LinearLayout toolbar=row();searchField=null;
        if(searchOpen){
            EditText edit=new EditText(this);searchField=edit;edit.setSingleLine(true);edit.setTextSize(16);edit.setTextColor(INK);edit.setHintTextColor(MUTED);edit.setHint(tab==0?"메뉴·상품·장소 검색":tab==1?"질문·상황 검색":"즐겨찾기 검색");edit.setContentDescription("전체 문장 검색");edit.setPadding(dp(12),0,dp(10),0);edit.setBackground(shape(Color.rgb(239,238,229),12,0));edit.setText(query);edit.setSelection(edit.length());toolbar.addView(edit,new LinearLayout.LayoutParams(0,-1,1));toolbar.addView(iconButton("\ue5cd","검색 닫기",()->{keyboardOff();query="";searchOpen=false;page=0;render();}),new LinearLayout.LayoutParams(dp(44),-1));
            edit.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int c){query=s.toString();page=0;populateList();}});
            if(openKeyboard){openKeyboard=false;edit.requestFocus();edit.post(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(edit,InputMethodManager.SHOW_IMPLICIT));}
        }else{
            if(tab==0){
                TextView drinks=text(FIRST_ORDER,17,GREEN,true);drinks.setGravity(Gravity.CENTER);click(drinks,orderCategory.equals(FIRST_ORDER)?Color.rgb(213,224,192):BG,24,0,()->{orderCategory=FIRST_ORDER;query="";page=0;ensureSelection();save();render();});toolbar.addView(drinks,new LinearLayout.LayoutParams(dp(103),-1));
                TextView other=text(orderCategory.equals(FIRST_ORDER)?"음식·상품":orderCategory+" ▾",16,GREEN,true);other.setGravity(Gravity.CENTER);other.setMaxLines(1);other.setEllipsize(android.text.TextUtils.TruncateAt.END);click(other,!orderCategory.equals(FIRST_ORDER)?SAGE:BG,24,0,this::chooseCategory);toolbar.addView(other,new LinearLayout.LayoutParams(0,-1,1));
            }else{TextView chooser=text(tab==2?"저장한 문장":questionCategory+" ▾",16,GREEN,true);chooser.setGravity(Gravity.CENTER_VERTICAL);chooser.setPadding(dp(12),0,dp(6),0);click(chooser,BG,18,0,()->{if(tab!=2)chooseCategory();});toolbar.addView(chooser,new LinearLayout.LayoutParams(0,-1,1));}
            toolbar.addView(iconButton("\ue8b6","검색",()->{searchOpen=true;openKeyboard=true;render();}),new LinearLayout.LayoutParams(dp(44),-1));
        }
        root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(compact?40:44)));gap(root,compact?4:6);
        listing=new SwipePages(this);root.addView(listing,new LinearLayout.LayoutParams(-1,dp(compact?64:136)));
        pageHint=text("",12,MUTED,false);pageHint.setGravity(Gravity.CENTER);root.addView(pageHint,new LinearLayout.LayoutParams(-1,dp(compact?20:22)));populateList();gap(root,compact?4:6);
        phraseBox=column();root.addView(phraseBox,new LinearLayout.LayoutParams(-1,0,1));updatePhrase();gap(root,compact?8:10);
        play=text("일본어로 말하기",21,Color.WHITE,true);play.setGravity(Gravity.CENTER);click(play,GREEN,27,0,()->{if(speech!=null&&speech.speaking)speech.stop();else speak(false);});root.addView(play,new LinearLayout.LayoutParams(-1,dp(compact?48:52)));
        support=row();support.addView(supportButton(tab==0?"천천히":"천천히 듣기",()->speak(true)),new LinearLayout.LayoutParams(0,-1,1));support.addView(supportButton(tab==0?"크게":"크게 보여주기",this::showLarge),new LinearLayout.LayoutParams(0,-1,1));
        if(tab==0){support.addView(supportButton("담기",this::addToCart),new LinearLayout.LayoutParams(0,-1,1));cartButton=supportButton("",this::showCart);support.addView(cartButton,new LinearLayout.LayoutParams(0,-1,1.25f));refreshCartButton();}
        root.addView(support,new LinearLayout.LayoutParams(-1,dp(compact?40:44)));updatePlay();if(wide)splitLandscape();safe.requestApplyInsets();
    }
    private TextView supportButton(String label,Runnable action){TextView t=text(label,15,GREEN,false);t.setGravity(Gravity.CENTER);t.setSingleLine();t.setAutoSizeTextTypeUniformWithConfiguration(11,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);click(t,BG,18,0,action);return t;}
    private void buildManual(FrameLayout safe){
        LinearLayout editor=column();editor.addView(text("한글로 입력",16,GREEN,true));gap(editor,6);
        manualField=new EditText(this);manualField.setText(manualDraft);manualField.setSelection(manualField.length());manualField.setHint("예: 생맥주 두 잔이랑 하이볼 한 잔 주세요");manualField.setTextColor(INK);manualField.setHintTextColor(MUTED);manualField.setTextSize(17);manualField.setPadding(dp(14),dp(10),dp(14),dp(10));manualField.setGravity(Gravity.TOP|Gravity.START);manualField.setBackground(shape(Color.rgb(239,238,229),16,LINE));manualField.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);manualField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(140)});manualField.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);manualField.setContentDescription("일본어로 말할 한국어 입력");editor.addView(manualField,new LinearLayout.LayoutParams(-1,dp(compact?72:100)));gap(editor,5);
        LinearLayout tools=row();manualCount=text("",12,MUTED,false);manualCount.setGravity(Gravity.CENTER_VERTICAL);tools.addView(manualCount,new LinearLayout.LayoutParams(0,dp(40),1));manualDownload=action("음성 받기",this::startVoiceDownload);manualDownload.setSingleLine();manualDownload.setAutoSizeTextTypeUniformWithConfiguration(12,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);LinearLayout.LayoutParams downloadSize=new LinearLayout.LayoutParams(dp(100),dp(40));downloadSize.rightMargin=dp(6);tools.addView(manualDownload,downloadSize);manualTranslate=action("번역하기",()->requestManual(false));tools.addView(manualTranslate,new LinearLayout.LayoutParams(dp(100),dp(40)));editor.addView(tools);
        TextView notice=text("인터넷 번역 · 처음 음성 준비 약83MB",11,MUTED,false);notice.setPadding(0,dp(4),0,dp(4));editor.addView(notice);root.addView(editor,new LinearLayout.LayoutParams(-1,-2));gap(root,6);
        manualOutput=column();
        LinearLayout japanese=column();japanese.setPadding(dp(16),dp(12),dp(16),dp(10));japanese.setBackground(shape(SAGE,19,0));LinearLayout japaneseHead=row();japaneseHead.addView(text("일본어",14,GREEN,true),new LinearLayout.LayoutParams(0,-2,1));manualStar=iconButton("\ue83a","입력 문장 즐겨찾기 저장",this::toggleManualFavorite);manualStar.setMinWidth(0);manualStar.setMinHeight(0);japaneseHead.addView(manualStar,new LinearLayout.LayoutParams(dp(40),dp(32)));japanese.addView(japaneseHead,new LinearLayout.LayoutParams(-1,dp(32)));gap(japanese,2);manualJapaneseView=text("",28,GREEN,true);manualJapaneseView.setTextLocale(Locale.JAPAN);manualJapaneseView.setGravity(Gravity.CENTER_VERTICAL);manualJapaneseView.setAutoSizeTextTypeUniformWithConfiguration(14,32,1,android.util.TypedValue.COMPLEX_UNIT_SP);japanese.addView(manualJapaneseView,new LinearLayout.LayoutParams(-1,0,1));manualOutput.addView(japanese,new LinearLayout.LayoutParams(-1,0,1.3f));gap(manualOutput,8);
        LinearLayout reading=column();reading.setPadding(dp(16),dp(10),dp(16),dp(10));reading.setBackground(shape(BG,17,LINE));reading.addView(text("한글 발음",13,GREEN,true));gap(reading,4);manualReadingView=text("",19,INK,false);manualReadingView.setGravity(Gravity.CENTER_VERTICAL);manualReadingView.setAutoSizeTextTypeUniformWithConfiguration(12,22,1,android.util.TypedValue.COMPLEX_UNIT_SP);reading.addView(manualReadingView,new LinearLayout.LayoutParams(-1,0,1));manualOutput.addView(reading,new LinearLayout.LayoutParams(-1,0,0.8f));
        manualStateView=text("",12,MUTED,false);manualStateView.setMaxLines(2);manualStateView.setGravity(Gravity.CENTER_VERTICAL);manualOutput.addView(manualStateView,new LinearLayout.LayoutParams(-1,dp(34)));root.addView(manualOutput,new LinearLayout.LayoutParams(-1,0,1));gap(root,8);
        play=text("말하기",22,Color.WHITE,true);play.setGravity(Gravity.CENTER);click(play,GREEN,27,0,()->{if(manualBusy){freeInput.cancel();manualBusy=false;manualStatus="준비를 중지했어요. 다시 누르면 이어가요.";updateManual();}else if(speech!=null&&speech.speaking)speech.stop();else if(manualResult!=null&&!manualBusy&&manualResult.korean.equals(manualDraft.trim()))speakManual(false);else requestManual(true);});root.addView(play,new LinearLayout.LayoutParams(-1,dp(52)));
        support=row();manualSlow=supportButton("천천히 듣기",()->speakManual(true));support.addView(manualSlow,new LinearLayout.LayoutParams(0,-1,1));manualLarge=supportButton("크게 보여주기",this::showManualLarge);support.addView(manualLarge,new LinearLayout.LayoutParams(0,-1,1));root.addView(support,new LinearLayout.LayoutParams(-1,dp(compact?36:40)));
        manualField.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence value,int start,int before,int count){manualDraft=value.toString();if(freeInput!=null)freeInput.cancel();if(speech!=null)speech.stop();manualBusy=false;manualResult=null;manualJapanese="";manualReading="";manualStatus="한글을 입력하고 번역하거나 바로 말해 보세요.";updateManual();}public void afterTextChanged(Editable value){}});
        manualField.setOnEditorActionListener((view,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){requestManual(true);return true;}return false;});
        if(wide){LinearLayout left=column();rightPane=column();List<View> views=new ArrayList<>();for(int i=0;i<root.getChildCount();i++)views.add(root.getChildAt(i));root.removeAllViews();boolean right=false;for(View view:views){if(view==manualOutput)right=true;(right?rightPane:left).addView(view);}root.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.rightMargin=dp(14);root.addView(left,lp);root.addView(rightPane,new LinearLayout.LayoutParams(0,-1,1.15f));}
        updateManual();safe.requestApplyInsets();
    }
    private void updateManual(){
        if(tab!=3||manualJapaneseView==null)return;
        manualJapaneseView.setText(manualJapanese.isEmpty()?"번역한 일본어가 여기에 보여요.":manualJapanese);manualJapaneseView.setTextColor(manualJapanese.isEmpty()?MUTED:GREEN);
        manualReadingView.setText(manualReading.isEmpty()?"일본어의 한글 발음이 여기에 보여요.":manualReading);manualReadingView.setTextColor(manualReading.isEmpty()?MUTED:INK);
        manualStateView.setText(manualStatus);manualCount.setText(manualDraft.length()+" / 140");boolean active=!manualDraft.trim().isEmpty()&&!manualBusy;manualTranslate.setEnabled(active);manualTranslate.setAlpha(active?1:0.45f);
        if(play!=null){boolean speaking=speech!=null&&speech.speaking;play.setText(speaking?"재생 멈추기":manualBusy?"준비 중지":"말하기");play.setEnabled(active||speaking||manualBusy);play.setAlpha(active||speaking||manualBusy?1:0.45f);play.setContentDescription(manualBusy?"번역·음성 준비 중지":speaking?"재생 멈추기":"입력한 문장을 일본어 남성 음성으로 말하기");}
        boolean audible=manualResult!=null&&manualResult.korean.equals(manualDraft.trim())&&!manualBusy;
        if(manualSlow!=null){manualSlow.setEnabled(audible);manualSlow.setAlpha(audible?1:0.45f);}
        if(manualLarge!=null){boolean shown=!manualJapanese.isEmpty();manualLarge.setEnabled(shown);manualLarge.setAlpha(shown?1:0.45f);}
        if(manualStar!=null){manualStar.setEnabled(audible);manualStar.setAlpha(audible?1:0.35f);manualStar.setText(audible&&favorites.contains(manualId()+"@1")?"\ue838":"\ue83a");}
        if(manualDownload!=null){boolean busy=freeInput.downloading();manualDownload.setVisibility(busy||(freeInput.voiceSupported()&&!freeInput.resourcesReady())?View.VISIBLE:View.GONE);manualDownload.setText(busy?"받기 중지":"음성 받기");}
    }
    private void requestManual(boolean autoSpeak){
        String input=manualDraft.trim();if(input.isEmpty()){manualStatus="먼저 한글로 말할 내용을 입력해 주세요.";updateManual();return;}if(manualBusy)return;
        keyboardOff();if(speech!=null)speech.stop();manualBusy=true;manualResult=null;manualJapanese="";manualReading="";manualStatus="문장 준비 중…";save();updateManual();
        freeInput.request(input,autoSpeak,new FreeInputEngine.Listener(){
            public void progress(String message){manualStatus=message;updateManual();}
            public void japanese(String value){manualJapanese=value;updateManual();}
            public void ready(FreeInputEngine.Result value,boolean speak){manualResult=value;manualJapanese=value.japanese;manualReading=value.reading;manualBusy=false;manualStatus="준비됐어요. 한글 발음은 참고용이에요.";updateManual();if(speak&&tab==3)speakManual(false);}
            public void error(String message){manualBusy=false;manualResult=null;manualStatus=message;updateManual();}
            public void textOnly(String japanese,String message){manualBusy=false;manualResult=null;manualJapanese=japanese;manualReading="";manualStatus=message;updateManual();}
        });
    }
    private void speakManual(boolean slow){
        if(manualResult==null||speech==null)return;keyboardOff();boolean ok=manualResult.asset.isEmpty()?speech.speakFile(manualResult.wav,slow):speech.speak(manualResult.asset,slow);if(!ok){manualStatus=speech.status;manualResult=null;}updateManual();
    }
    private void showManualLarge(){if(manualJapanese.isEmpty())return;keyboardOff();boolean audible=manualResult!=null&&manualResult.korean.equals(manualDraft.trim());showLarge(manualJapanese,manualDraft.trim(),audible?()->speakManual(false):null);}
    private void startVoiceDownload(){
        if(freeInput.downloading()){freeInput.cancelDownload();return;}
        keyboardOff();
        if(!freeInput.metered()){beginVoiceDownload();return;}
        new AlertDialog.Builder(this).setTitle("음성 자료 약83MB").setMessage("지금 모바일 데이터로 연결돼 있어요. 로밍 중이면 요금이 나올 수 있어요. 받을까요?").setNegativeButton("취소",null).setPositiveButton("받기",(d,w)->beginVoiceDownload()).show();
    }
    private void voiceDownloadEnded(String message){getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);manualStatus=message;updateManual();}
    private void beginVoiceDownload(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);manualStatus="음성 자료 받는 중…";
        freeInput.download(new FreeInputEngine.DownloadListener(){
            public void progress(String message){manualStatus=message;updateManual();}
            public void done(){voiceDownloadEnded("음성 자료 준비 완료. 이제 새 문장도 소리로 들을 수 있어요.");}
            public void failed(String message){voiceDownloadEnded(message);}
        });
        updateManual();
    }
    private Phrase lookup(String id){Phrase found=Catalog.find(id);if(found!=null)return found;for(Phrase p:savedManual)if(p.id.equals(id))return p;return null;}
    private String manualId(){try{return "manual-"+FreeInputEngine.key(manualResult.japanese+"|"+manualResult.korean).substring(0,20);}catch(Exception e){return "manual-";}}
    private void loadSavedManual(){
        savedManual.clear();
        try{org.json.JSONArray all=new org.json.JSONArray(prefs.getString("manual-saved","[]"));for(int i=0;i<all.length();i++){org.json.JSONObject o=all.getJSONObject(i);Phrase p=new Phrase(o.getString("id"),"입력",o.getString("ko"),o.getString("ja"),o.optString("kana"),o.optString("reading"),"",false,false);p.audioPath=o.optString("audio");savedManual.add(p);}}catch(org.json.JSONException ignored){}
    }
    private void storeSavedManual(){
        org.json.JSONArray all=new org.json.JSONArray();
        try{for(Phrase p:savedManual)all.put(new org.json.JSONObject().put("id",p.id).put("ko",p.ko).put("ja",p.ja).put("kana",p.kana).put("reading",p.reading).put("audio",p.audioPath));}catch(org.json.JSONException ignored){}
        prefs.edit().putString("manual-saved",all.toString()).putStringSet("favorites",new HashSet<>(favorites)).apply();
    }
    private void forgetManual(Phrase phrase){savedManual.remove(phrase);favorites.remove(phrase.id+"@1");if(!phrase.audioPath.isEmpty())new File(phrase.audioPath).delete();storeSavedManual();}
    private void toggleManualFavorite(){
        if(manualResult==null)return;String id=manualId();Phrase existing=lookup(id);
        if(existing!=null){forgetManual(existing);if(selected==existing)selected=Catalog.ALL.get(0);Toast.makeText(this,"즐겨찾기에서 지웠어요",Toast.LENGTH_SHORT).show();updateManual();return;}
        Phrase saved=new Phrase(id,"입력",manualResult.korean,manualResult.japanese,manualResult.asset,manualResult.reading,"",false,false);
        if(manualResult.asset.isEmpty()){
            // The translation cache evicts audio after 20 entries, so a saved sentence keeps its own copy.
            File folder=new File(getFilesDir(),"saved-voice");folder.mkdirs();File target=new File(folder,id+".wav");
            try(InputStream in=new FileInputStream(manualResult.wav);OutputStream out=new FileOutputStream(target)){byte[] buffer=new byte[65536];for(int n;(n=in.read(buffer))!=-1;)out.write(buffer,0,n);}
            catch(IOException e){target.delete();Toast.makeText(this,"음성을 저장하지 못했어요. 다시 눌러 주세요.",Toast.LENGTH_LONG).show();return;}
            saved.audioPath=target.getAbsolutePath();
        }
        savedManual.add(saved);favorites.add(id+"@1");storeSavedManual();Toast.makeText(this,"즐겨찾기에 저장했어요",Toast.LENGTH_SHORT).show();updateManual();
    }
    private void loadCart(){cart.clear();for(String item:prefs.getString("cart","").split(",")){String[] parts=item.split("@");if(parts.length!=2)continue;Phrase p=Catalog.find(parts[0]);if(p==null||!p.order)continue;try{cart.add(new CartLine(p,Quantity.clamp(Integer.parseInt(parts[1]))));}catch(NumberFormatException ignored){}}}
    private void saveCart(){StringBuilder value=new StringBuilder();for(CartLine line:cart)value.append(line.phrase.id).append('@').append(line.number).append(',');prefs.edit().putString("cart",value.toString()).apply();refreshCartButton();}
    private void refreshCartButton(){if(cartButton!=null)cartButton.setText(cart.isEmpty()?"주문서":"주문서 "+cart.size());}
    private void addToCart(){
        if(selected==null||!selected.order||(selected.retail&&mode>0)){Toast.makeText(this,"구매 요청이나 주문 품목만 담을 수 있어요",Toast.LENGTH_SHORT).show();return;}
        for(CartLine line:cart)if(line.phrase==selected){line.number=count;saveCart();Toast.makeText(this,selected.ko+" "+count+selected.unit()+"로 바꿨어요",Toast.LENGTH_SHORT).show();return;}
        if(cart.size()>=CART_MAX){Toast.makeText(this,"주문서에는 "+CART_MAX+"개까지 담을 수 있어요",Toast.LENGTH_SHORT).show();return;}
        cart.add(new CartLine(selected,count));saveCart();Toast.makeText(this,selected.ko+" "+count+selected.unit()+" 담았어요",Toast.LENGTH_SHORT).show();
    }
    private void showCart(){
        keyboardOff();if(cart.isEmpty()){Toast.makeText(this,"품목을 고르고 담기를 눌러 주세요",Toast.LENGTH_SHORT).show();return;}
        Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar);LinearLayout sheet=column();sheet.setPadding(dp(18),dp(30),dp(18),dp(16));sheet.setBackgroundColor(BG);
        sheet.addView(text("주문서",22,GREEN,true));gap(sheet,10);
        SwipePages pages=new SwipePages(this);sheet.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=text("",12,MUTED,false);hint.setGravity(Gravity.CENTER);sheet.addView(hint,new LinearLayout.LayoutParams(-1,dp(24)));
        cartSpeak=text("이어서 말하기",20,Color.WHITE,true);cartSpeak.setGravity(Gravity.CENTER);click(cartSpeak,GREEN,26,0,()->{if(speech==null)return;if(speech.speaking){speech.stop();return;}List<String> lines=new ArrayList<>();for(CartLine line:cart)lines.add(line.phrase.spoken(line.number));if(!speech.speakAll(lines))Toast.makeText(this,speech.status,Toast.LENGTH_LONG).show();});sheet.addView(cartSpeak,new LinearLayout.LayoutParams(-1,dp(54)));gap(sheet,8);
        LinearLayout bottom=row();TextView clear=action("비우기",()->{cart.clear();saveCart();dialog.dismiss();});LinearLayout.LayoutParams clearSize=new LinearLayout.LayoutParams(0,-1,1);clearSize.rightMargin=dp(8);bottom.addView(clear,clearSize);bottom.addView(action("닫기",dialog::dismiss),new LinearLayout.LayoutParams(0,-1,1));sheet.addView(bottom,new LinearLayout.LayoutParams(-1,dp(48)));
        drawCart(dialog,pages,hint,0);
        dialog.setContentView(sheet);dialog.setOnDismissListener(d->{cartSpeak=null;if(speech!=null)speech.stop();});dialog.show();dialog.getWindow().setLayout(-1,-1);dialog.getWindow().setStatusBarColor(BG);dialog.getWindow().setNavigationBarColor(BG);
    }
    private void drawCart(Dialog dialog,SwipePages pages,TextView hint,int wanted){
        if(cart.isEmpty()){dialog.dismiss();return;}
        int perPage=wide?2:compact?3:4,total=(cart.size()+perPage-1)/perPage,start=Math.min(wanted,total-1);
        pages.configure(total,start,index->{
            LinearLayout grid=column();
            for(int row=0;row<perPage;row++){int item=index*perPage+row;View cell;
                if(item<cart.size()){CartLine line=cart.get(item);LinearLayout card=row();card.setPadding(dp(14),dp(6),dp(4),dp(6));card.setBackground(shape(SAGE,16,0));LinearLayout words=column();TextView ja=text(line.phrase.ja+" "+Quantity.display(line.number,line.phrase.counter),28,GREEN,true);ja.setTextLocale(Locale.JAPAN);ja.setGravity(Gravity.CENTER_VERTICAL);ja.setAutoSizeTextTypeUniformWithConfiguration(16,30,1,android.util.TypedValue.COMPLEX_UNIT_SP);words.addView(ja,new LinearLayout.LayoutParams(-1,0,1));TextView ko=text(line.phrase.ko+" "+line.number+line.phrase.unit(),14,MUTED,false);ko.setSingleLine();ko.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(ko,new LinearLayout.LayoutParams(-1,-2));card.addView(words,new LinearLayout.LayoutParams(0,-1,1));card.addView(iconButton("\ue5cd",line.phrase.ko+" 빼기",()->{cart.remove(line);saveCart();if(speech!=null)speech.stop();drawCart(dialog,pages,hint,index);}),new LinearLayout.LayoutParams(dp(44),-1));cell=card;}
                else cell=new View(this);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);if(row<perPage-1)lp.bottomMargin=dp(8);grid.addView(cell,lp);
            }return grid;
        },index->hint.setText((index+1)+" / "+total+"  ·  좌우로 밀기"));
        hint.setText(total>1?(start+1)+" / "+total+"  ·  좌우로 밀기":"");
    }
    private void splitLandscape(){
        LinearLayout left=column();rightPane=column();boolean right=false;
        List<View> views=new ArrayList<>();for(int i=0;i<root.getChildCount();i++)views.add(root.getChildAt(i));root.removeAllViews();
        for(View view:views){if(view==phraseBox)right=true;(right?rightPane:left).addView(view);}
        root.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.rightMargin=dp(14);root.addView(left,lp);root.addView(rightPane,new LinearLayout.LayoutParams(0,-1,1.1f));
        LinearLayout.LayoutParams list=(LinearLayout.LayoutParams)listing.getLayoutParams();list.height=0;list.weight=1;listing.setLayoutParams(list);
    }
    private void keyboardLayout(boolean open){
        if(tab==3){if(support!=null)support.setVisibility(open?View.GONE:View.VISIBLE);if(header!=null)header.setVisibility(open?View.GONE:View.VISIBLE);if(wide&&rightPane!=null)rightPane.setVisibility(open?View.GONE:View.VISIBLE);if(manualField!=null){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)manualField.getLayoutParams();lp.height=dp(open?60:(compact?72:100));manualField.setLayoutParams(lp);}return;}
        if(header==null)return;if(rightPane!=null)rightPane.setVisibility(open?View.GONE:View.VISIBLE);header.setVisibility(open?View.GONE:View.VISIBLE);
        if(phraseBox!=null)phraseBox.setVisibility(open?View.GONE:View.VISIBLE);
        if(play!=null)play.setVisibility(open?View.GONE:View.VISIBLE);
        if(support!=null)support.setVisibility(open?View.GONE:View.VISIBLE);
        if(listing!=null){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)listing.getLayoutParams();lp.height=open||wide?0:dp(compact?64:136);lp.weight=open||wide?1:0;listing.setLayoutParams(lp);}
    }
    private void ensureSelection(){
        if(tab==3)return;
        List<Entry> candidates=entries();if(candidates.isEmpty())return;
        if(tab==2)for(Entry e:candidates)if(entryKey(e).equals(favoriteKey()))return;
        for(Entry e:candidates)if(e.phrase.id.equals(selected.id)){if(tab==2){count=e.number;mode=e.mode;}return;}
        Entry first=candidates.get(0);selected=first.phrase;count=first.number;mode=first.mode;
    }
    private void chooseCategory(){
        keyboardOff();final boolean ordering=tab==0;List<String> names=categories(ordering);if(ordering)names.remove(FIRST_ORDER);
        Dialog dialog=new Dialog(this);LinearLayout content=column();content.setPadding(dp(18),dp(16),dp(18),dp(8));content.setBackground(shape(BG,24,0));
        content.addView(text(ordering?"음식·상품 분야":"질문할 상황",21,GREEN,true));gap(content,12);
        SwipePages pages=new SwipePages(this);content.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=text("",13,MUTED,false);hint.setGravity(Gravity.CENTER);content.addView(hint,new LinearLayout.LayoutParams(-1,dp(32)));
        TextView close=action("닫기",dialog::dismiss);content.addView(close,new LinearLayout.LayoutParams(-1,dp(44)));
        int capacity=compact?4:6;int total=Math.max(1,(names.size()+capacity-1)/capacity);int selectedIndex=names.indexOf(ordering?orderCategory:questionCategory);int initial=Math.max(0,selectedIndex/capacity);
        pages.configure(total,initial,index->{
            LinearLayout grid=column();int rows=capacity/2;
            for(int row=0;row<rows;row++){LinearLayout line=row();for(int col=0;col<2;col++){int item=index*capacity+row*2+col;View cell;
                if(item<names.size()){String name=names.get(item);TextView choice=text(name,16,GREEN,true);choice.setGravity(Gravity.CENTER);choice.setPadding(dp(8),dp(4),dp(8),dp(4));choice.setMaxLines(2);choice.setAutoSizeTextTypeUniformWithConfiguration(14,17,1,android.util.TypedValue.COMPLEX_UNIT_SP);boolean active=name.equals(ordering?orderCategory:questionCategory);click(choice,active?SAGE:BG,16,active?GREEN:LINE,()->{if(ordering)orderCategory=name;else questionCategory=name;query="";page=0;searchOpen=false;ensureSelection();save();dialog.dismiss();render();});cell=choice;}else cell=new View(this);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(col==0)lp.rightMargin=dp(8);line.addView(cell,lp);
            }LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);if(row<rows-1)lp.bottomMargin=dp(8);grid.addView(line,lp);}return grid;
        },index->hint.setText((index+1)+" / "+total+"  ·  좌우로 밀어 선택"));
        hint.setText((initial+1)+" / "+total+"  ·  좌우로 밀어 선택");dialog.setContentView(content);dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));window.setLayout(Math.min(root.getWidth()-dp(20),dp(480)),Math.min(dp(compact?310:390),root.getHeight()-dp(32)));}
    }
    private static final class Entry {final Phrase phrase;final int number,mode;Entry(Phrase p,int n,int mode){phrase=p;number=n;this.mode=mode;}}
    private String entryKey(Entry e){return e.phrase.order&&e.phrase.retail&&e.mode>0?e.phrase.id+"@1@"+e.mode:e.phrase.key(e.number);}
    private List<Entry> entries(){
        List<Entry> result=new ArrayList<>();String q=query.trim();String group=tab==0?orderCategory:questionCategory;
        List<Phrase> pool=new ArrayList<>(Catalog.ALL);if(tab==2)pool.addAll(savedManual);
        for(Phrase p:pool){
            if(tab!=2&&p.order!=(tab==0))continue;
            if(tab!=2&&q.isEmpty()&&!group.equals(p.scene))continue;
            if(!p.matches(q))continue;
            if(tab==2){for(String key:favorites){String[] parts=key.split("@");if(parts.length<2||!parts[0].equals(p.id))continue;try{int m=parts.length>2?Integer.parseInt(parts[2]):0;result.add(new Entry(p,Quantity.clamp(Integer.parseInt(parts[1])),Math.max(0,Math.min(2,m))));}catch(NumberFormatException ignored){}}}
            else result.add(new Entry(p,p.order?Quantity.clamp(prefs.getInt("quantity-"+p.id,defaultQuantity(p))):1,p.retail?1:0));
        }
        if(tab!=2&&q.isEmpty())result.sort(Comparator.comparingInt(e->e.phrase.sceneOrder));
        if(tab==2)result.sort(Comparator.comparing((Entry e)->e.phrase.ko).thenComparingInt(e->e.mode).thenComparingInt(e->e.number));return result;
    }
    /** Three travellers: drinks start at one each, shared food at one. */
    private static int defaultQuantity(Phrase p){return p.counter.equals("cup")?3:1;}
    private void populateList(){
        if(listing==null||pageHint==null)return;List<Entry> all=entries();int pages=Math.max(1,(all.size()+pageSize-1)/pageSize);page=Math.max(0,Math.min(page,pages-1));
        listing.configure(pages,page,index->{
            if(all.isEmpty()){TextView empty=text(tab==2?"문장 옆 별을 눌러 저장해 보세요.":"찾는 항목이 없어요. 다른 단어로 찾아보세요.",16,MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(12),0,dp(12),0);return empty;}
            int columns=tab==0?2:1,rows=(pageSize+columns-1)/columns;LinearLayout grid=column();
            for(int row=0;row<rows;row++){LinearLayout line=row();for(int col=0;col<columns;col++){int item=index*pageSize+row*columns+col;View cell;
                if(item<all.size()){Entry entry=all.get(item);Phrase p=entry.phrase;boolean active=p.id.equals(selected.id)&&(tab!=2||favoriteKey().equals(entryKey(entry)));
                    TextView label=text(tab==0?(p.id.equals("sake")?"사케":p.ko):korean(p,entry.number,entry.mode),17,active?GREEN:INK,true);label.setPadding(dp(12),dp(5),dp(12),dp(5));label.setGravity(Gravity.CENTER);label.setMaxLines(tab==0?2:3);label.setAutoSizeTextTypeUniformWithConfiguration(14,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);label.setSelected(active);label.setContentDescription(label.getText()+(active?", 선택됨":""));click(label,active?SAGE:BG,16,active?GREEN:LINE,()->select(entry,false));label.setOnLongClickListener(v->{details(p);return true;});cell=label;
                }else cell=new View(this);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(col<columns-1)lp.rightMargin=dp(8);line.addView(cell,lp);
            }LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);if(row<rows-1)lp.bottomMargin=dp(8);grid.addView(line,lp);}return grid;
        },index->{page=index;pageHint.setText((page+1)+" / "+pages+"  ·  "+all.size()+"개  ·  좌우로 밀기");});
        pageHint.setText((page+1)+" / "+pages+"  ·  "+all.size()+"개"+(pages>1?"  ·  좌우로 밀기":""));
    }
    private void select(Entry entry,boolean listen){save();keyboardOff();selected=entry.phrase;count=entry.number;mode=entry.mode;save();updatePhrase();populateList();updatePlay();if(listen)speak(false);}
    private Bitmap bitmap(String name){if(images.containsKey(name))return images.get(name);try(InputStream in=getAssets().open(name+".png")){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=1;Bitmap b=BitmapFactory.decodeStream(in,null,o);images.put(name,b);return b;}catch(IOException e){return null;}}
    private String korean(Phrase p,int n,int intent){return p.order&&p.retail&&intent>0?p.ko+(intent==1?" 있나요?":" 얼마예요?"):p.korean(n);}
    private String currentKorean(){return korean(selected,count,mode);}
    private String currentJapanese(){return selected.order&&selected.retail&&mode>0?selected.ja+(mode==1?"はありますか？":"はいくらですか？"):selected.japanese(count);}
    private String currentSpoken(){return selected.order&&selected.retail&&mode>0?selected.kana+(mode==1?"はありますか？":"はいくらですか？"):selected.spoken(count);}
    private String currentReading(){return selected.order&&selected.retail&&mode>0?selected.reading+(mode==1?"와 아리마스카":"와 이쿠라데스카"):selected.pronunciation(count);}
    private String favoriteKey(){return selected.order&&selected.retail&&mode>0?selected.id+"@1@"+mode:selected.key(count);}
    private void updatePhrase(){
        if(phraseBox==null||selected==null)return;phraseBox.removeAllViews();
        if(selected.order&&selected.retail){LinearLayout actions=row();String[] labels={"구매 요청","재고 묻기","가격 묻기"};for(int i=0;i<3;i++){final int m=i;TextView b=text(labels[i],14,mode==i?Color.WHITE:GREEN,true);b.setGravity(Gravity.CENTER);click(b,mode==i?GREEN:BG,18,mode==i?0:LINE,()->{mode=m;save();updatePhrase();updatePlay();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.rightMargin=i<2?dp(5):0;actions.addView(b,lp);}phraseBox.addView(actions,new LinearLayout.LayoutParams(-1,dp(38)));gap(phraseBox,6);}
        if(selected.order&&(!selected.retail||mode==0)){
            LinearLayout quantity=row();TextView label=text("수량",18,INK,true);quantity.addView(label,new LinearLayout.LayoutParams(dp(70),-2));LinearLayout capsule=row();capsule.setPadding(dp(4),0,dp(4),0);capsule.setBackground(shape(BG,25,Color.rgb(180,185,166)));
            TextView minus=iconButton("\ue15b","수량 줄이기",()->changeCount(-1));minus.setEnabled(count>1);minus.setAlpha(count>1?1:0.3f);capsule.addView(minus,new LinearLayout.LayoutParams(0,-1,1));
            View left=new View(this);left.setBackgroundColor(LINE);capsule.addView(left,new LinearLayout.LayoutParams(dp(1),dp(26)));TextView n=text(count+selected.unit(),26,GREEN,true);n.setGravity(Gravity.CENTER);capsule.addView(n,new LinearLayout.LayoutParams(0,-1,1.55f));View right=new View(this);right.setBackgroundColor(LINE);capsule.addView(right,new LinearLayout.LayoutParams(dp(1),dp(26)));
            TextView plus=iconButton("\ue145","수량 늘리기",()->changeCount(1));plus.setEnabled(count<10);plus.setAlpha(count<10?1:0.3f);capsule.addView(plus,new LinearLayout.LayoutParams(0,-1,1));quantity.addView(capsule,new LinearLayout.LayoutParams(0,dp(44),1));phraseBox.addView(quantity,new LinearLayout.LayoutParams(-1,dp(44)));gap(phraseBox,8);
        }
        LinearLayout panel=column();panel.setPadding(dp(16),dp(8),dp(12),dp(10));panel.setBackground(shape(SAGE,19,0));LinearLayout heading=row();TextView ko=text(currentKorean(),16,GREEN,true);ko.setMaxLines(2);ko.setAutoSizeTextTypeUniformWithConfiguration(13,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);heading.addView(ko,new LinearLayout.LayoutParams(0,-1,1));if(!selected.detail.isEmpty())heading.addView(iconButton("\ue88e","메뉴·상품 정보",()->details(selected)),new LinearLayout.LayoutParams(dp(36),-1));boolean starred=favorites.contains(favoriteKey());heading.addView(iconButton(starred?"\ue838":"\ue83a",starred?"즐겨찾기 해제":"즐겨찾기 저장",this::toggleFavorite),new LinearLayout.LayoutParams(dp(38),-1));panel.addView(heading,new LinearLayout.LayoutParams(-1,dp(36)));
        String ja=currentJapanese();if(selected.order&&(!selected.retail||mode==0)&&selected.ja.length()<18)ja=selected.ja+"を\n"+Quantity.display(count,selected.counter)+"ください。";
        TextView japanese=text(ja,32,GREEN,true);japanese.setTextLocale(Locale.JAPAN);japanese.setGravity(Gravity.CENTER_VERTICAL);japanese.setAutoSizeTextTypeUniformWithConfiguration(18,34,1,android.util.TypedValue.COMPLEX_UNIT_SP);panel.addView(japanese,new LinearLayout.LayoutParams(-1,0,1));
        TextView reading=text(currentReading(),14,MUTED,false);reading.setMaxLines(2);reading.setEllipsize(android.text.TextUtils.TruncateAt.END);reading.setPadding(0,dp(4),0,0);panel.addView(reading,new LinearLayout.LayoutParams(-1,-2));phraseBox.addView(panel,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void details(Phrase p){String message=p.detail.isEmpty()?"현장에서 메뉴와 판매 단위를 확인해 주세요.":p.detail;information(p.ko,message+"\n\n지점별 판매 여부와 당일 재고는 매장에서 확인해 주세요.");}
    private void information(String title,String message){
        List<String> chunks=new ArrayList<>();String remaining=message.trim();
        while(!remaining.isEmpty()){
            int end=Math.min(110,remaining.length());if(end<remaining.length()){int split=Math.max(remaining.lastIndexOf(' ',end),remaining.lastIndexOf('\n',end));if(split>55)end=split;if(end>0&&Character.isHighSurrogate(remaining.charAt(end-1)))end--;}
            chunks.add(remaining.substring(0,end).trim());remaining=remaining.substring(end).trim();
        }
        if(chunks.isEmpty())chunks.add("");Dialog dialog=new Dialog(this);LinearLayout content=column();content.setPadding(dp(18),dp(16),dp(18),dp(8));content.setBackground(shape(BG,24,0));TextView heading=text(title,20,GREEN,true);heading.setMaxLines(2);content.addView(heading);gap(content,10);
        SwipePages pages=new SwipePages(this);content.addView(pages,new LinearLayout.LayoutParams(-1,0,1));TextView hint=text("",12,MUTED,false);hint.setGravity(Gravity.CENTER);content.addView(hint,new LinearLayout.LayoutParams(-1,dp(26)));content.addView(action("닫기",dialog::dismiss),new LinearLayout.LayoutParams(-1,dp(44)));
        pages.configure(chunks.size(),0,index->{TextView value=text(chunks.get(index),17,INK,false);value.setGravity(Gravity.CENTER_VERTICAL);value.setAutoSizeTextTypeUniformWithConfiguration(13,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);return value;},index->hint.setText((index+1)+" / "+chunks.size()+"  ·  좌우로 밀기"));hint.setText(chunks.size()>1?"1 / "+chunks.size()+"  ·  좌우로 밀기":"");
        dialog.setContentView(content);dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));window.setLayout(Math.min(root.getWidth()-dp(20),dp(480)),Math.min(dp(390),root.getHeight()-dp(32)));}
    }
    private void changeCount(int delta){count=Quantity.clamp(count+delta);save();updatePhrase();updatePlay();}
    private void toggleFavorite(){boolean remove=favorites.remove(favoriteKey());if(!remove)favorites.add(favoriteKey());if(remove&&selected.id.startsWith("manual-")){forgetManual(selected);selected=Catalog.ALL.get(0);ensureSelection();if(tab==2)render();Toast.makeText(this,"즐겨찾기에서 지웠어요",Toast.LENGTH_SHORT).show();return;}prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).apply();updatePhrase();if(tab==2)populateList();Toast.makeText(this,remove?"즐겨찾기에서 지웠어요":"즐겨찾기에 저장했어요",Toast.LENGTH_SHORT).show();}
    private void updatePlay(){if(cartSpeak!=null)cartSpeak.setText(speech!=null&&speech.speaking?"재생 멈추기":"이어서 말하기");if(tab==3){updateManual();return;}if(play==null||selected==null)return;String label=speech!=null&&speech.speaking?"재생 멈추기":"일본어로 말하기";SpannableString styled=new SpannableString((speech!=null&&speech.speaking?"\ue047":"\ue050")+"  "+label);styled.setSpan(new android.text.style.MetricAffectingSpan(){public void updateDrawState(android.text.TextPaint p){p.setTypeface(icons);}public void updateMeasureState(android.text.TextPaint p){p.setTypeface(icons);}},0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);play.setText(styled);play.setContentDescription(label+", "+currentKorean());}
    private void speak(boolean slow){keyboardOff();if(speech==null||speech.loading){Toast.makeText(this,"음성을 준비하고 있어요. 잠시 후 다시 눌러 주세요.",Toast.LENGTH_SHORT).show();return;}if(!speech.ready){Toast.makeText(this,speech.status,Toast.LENGTH_LONG).show();return;}if(!(selected.audioPath.isEmpty()?speech.speak(currentSpoken(),slow):speech.speakFile(new File(selected.audioPath),slow)))Toast.makeText(this,speech.status,Toast.LENGTH_LONG).show();}
    private void settings(){
        information("여행 한마디 0.1.8","오사카 · 교토 · 야마자키\n\n주문·질문은 앱에 포함된 남성 음성을 인터넷 없이 재생해요.\n\n음성: VOICEVOX:玄野武宏 (CV:ガロ)\n\n입력하기에서는 한글을 일본어로 번역하고 같은 남성 음성으로 읽어요. 번역할 문장은 MyMemory로 전송돼요. 첫 사용 때 음성 자료 약83MB를 받아요. 최근 번역 20개는 기기에 저장돼요.\n\n문장과 즐겨찾기는 휴대폰에 저장돼요. 소리는 미디어 음량을 사용해요. 한글 발음은 참고용이에요.");
    }
    private void showLarge(){showLarge(currentJapanese(),currentKorean(),()->speak(false));}
    private void showLarge(String japanese,String korean,Runnable talk){
        if(large)return;large=true;Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar);LinearLayout page=column();page.setGravity(Gravity.CENTER);page.setPadding(dp(24),dp(36),dp(24),dp(24));page.setBackgroundColor(BG);
        TextView ja=text(japanese,44,GREEN,true);ja.setTextLocale(Locale.JAPAN);ja.setGravity(Gravity.CENTER);ja.setAutoSizeTextTypeUniformWithConfiguration(18,46,1,android.util.TypedValue.COMPLEX_UNIT_SP);page.addView(ja,new LinearLayout.LayoutParams(-1,0,1));gap(page,10);TextView ko=text(korean,18,MUTED,false);ko.setGravity(Gravity.CENTER);ko.setMaxLines(3);ko.setAutoSizeTextTypeUniformWithConfiguration(13,19,1,android.util.TypedValue.COMPLEX_UNIT_SP);page.addView(ko,new LinearLayout.LayoutParams(-1,dp(65)));gap(page,10);
        TextView speak=text("일본어로 말하기",20,Color.WHITE,true);speak.setGravity(Gravity.CENTER);click(speak,GREEN,26,0,talk==null?()->{}:talk);if(talk!=null){page.addView(speak,new LinearLayout.LayoutParams(-1,dp(56)));gap(page,12);}TextView close=text("닫기",17,GREEN,true);close.setGravity(Gravity.CENTER);click(close,BG,20,LINE,dialog::dismiss);page.addView(close,new LinearLayout.LayoutParams(-1,dp(52)));dialog.setContentView(page);dialog.setOnDismissListener(d->large=false);dialog.show();dialog.getWindow().setLayout(-1,-1);dialog.getWindow().setStatusBarColor(BG);dialog.getWindow().setNavigationBarColor(BG);
    }
    @Override protected void onResume(){super.onResume();if(speech!=null)speech.refresh();}
    @Override protected void onStop(){if(freeInput!=null)freeInput.pause();if(manualBusy){manualBusy=false;manualStatus="다시 누르면 문장 준비를 이어가요.";}if(speech!=null)speech.stop();save();super.onStop();}
    @Override protected void onDestroy(){if(freeInput!=null)freeInput.close();if(speech!=null)speech.destroy();super.onDestroy();}
}

