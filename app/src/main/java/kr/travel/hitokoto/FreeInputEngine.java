package kr.travel.hitokoto;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.*;
import android.text.Html;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Explicit button-triggered translation. No requests while the user types. */
public final class FreeInputEngine {
    public interface Listener {void progress(String message);void japanese(String value);void ready(Result value,boolean speak);void error(String message);void textOnly(String japanese,String message);}
    public interface DownloadListener {void progress(String message);void done();void failed(String message);}
    public static final class Result {
        public final String korean,japanese,reading,asset;public final File wav;
        Result(String ko,String ja,String reading,String asset,File wav){this.korean=ko;this.japanese=ja;this.reading=reading;this.asset=asset;this.wav=wav;}
    }
    private final Context context;private final SharedPreferences cache;private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final FreeInputVoice voice;
    private final ExecutorService network=new ThreadPoolExecutor(0,2,10,TimeUnit.SECONDS,new SynchronousQueue<Runnable>(),job->{Thread thread=new Thread(job,"hitokoto-translation");thread.setDaemon(true);return thread;},new ThreadPoolExecutor.AbortPolicy());
    private volatile NetworkCall activeConnection;
    private volatile Thread downloadThread;
    private volatile int generation;private Future<?> pending;private boolean closed;
    private static final Pattern NOISE=Pattern.compile("[\\s?!？！。.,，·]");
    private static final Pattern COUNTS=Pattern.compile("(한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)(잔|병|개|인분|접시|꼬치|공기|그릇|세트|벌|켤레|봉지|점)(?=주세요|부탁|$)");
    private static final List<String> COUNT_WORDS=Arrays.asList("한","두","세","네","다섯","여섯","일곱","여덟","아홉","열");
    public FreeInputEngine(Context context){this.context=context.getApplicationContext();cache=context.getSharedPreferences("input-translations-v1",Context.MODE_PRIVATE);voice=new FreeInputVoice(context);}
    public static String key(String text)throws Exception {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    private File file(String japanese)throws Exception{return new File(context.getCacheDir(),"input-male-11-"+key(japanese)+".wav");}
    private static String normalized(String value){
        String clean=NOISE.matcher(value).replaceAll("");Matcher matcher=COUNTS.matcher(clean);StringBuffer out=new StringBuffer();
        while(matcher.find())matcher.appendReplacement(out,Matcher.quoteReplacement((COUNT_WORDS.indexOf(matcher.group(1))+1)+matcher.group(2)));matcher.appendTail(out);return out.toString();
    }
    private Result known(String input){
        String wanted=normalized(input);
        for(Phrase p:Catalog.ALL){
            for(int n=1;n<=(p.order?10:1);n++)if(wanted.equals(normalized(p.korean(n))))return new Result(input,p.japanese(n),p.pronunciation(n),p.spoken(n),null);
            if(p.order&&p.retail)for(int mode=1;mode<=2;mode++)if(wanted.equals(normalized(p.ko+(mode==1?" 있나요?":" 얼마예요?"))))return new Result(input,p.ja+(mode==1?"はありますか？":"はいくらですか？"),p.reading+(mode==1?"와 아리마스카":"와 이쿠라데스카"),p.kana+(mode==1?"はありますか？":"はいくらですか？"),null);
        }
        return null;
    }
    public Result cached(String input){
        if(input.trim().isEmpty())return null;
        Result result=known(input);if(result!=null)return result;
        try{JSONObject obj=new JSONObject(cache.getString(key(input),""));String ja=obj.getString("japanese"),reading=obj.getString("reading");File wav=file(ja);if(wav.length()>44)return new Result(input,ja,reading,"",wav);}catch(Exception ignored){}
        return null;
    }
    private void deliver(int id,Runnable callback){main.post(()->{if(!closed&&id==generation)callback.run();});}
    public void request(String input,boolean speak,Listener listener){
        cancel();if(closed)return;final int id=generation;
        pending=worker.submit(()->{
            try{
                Result existing=cached(input);if(existing!=null){deliver(id,()->listener.ready(existing,speak));return;}
                String japanese="";try{japanese=new JSONObject(cache.getString(key(input),"{}")).optString("japanese");}catch(Exception ignored){}
                if(japanese.isEmpty()){deliver(id,()->listener.progress("일본어로 번역 중 · 최대 15초"));japanese=translate(input);}
                cache.edit().putString(key(input),new JSONObject().put("japanese",japanese).put("time",System.currentTimeMillis()).toString()).apply();
                final String translated=japanese;deliver(id,()->listener.japanese(translated));
                if(voiceSupported()&&!resourcesReady()){
                    // The 83MB voice data is never fetched as a side effect of checking a translation or on roaming.
                    String reason=downloadThread!=null?"음성 자료를 받는 중이에요. 끝나면 소리로 들을 수 있어요.":!speak?"번역했어요. 한글 발음과 소리는 '음성 받기' 후에 나와요.":metered()?"모바일 데이터라 음성 자료는 받지 않았어요. '음성 받기'로 받을 수 있어요.":null;
                    if(reason!=null){deliver(id,()->listener.textOnly(translated,reason));return;}
                }
                if(Thread.currentThread().isInterrupted())return;
                voice.prepare(message->deliver(id,()->listener.progress(message)));
                if(Thread.currentThread().isInterrupted())return;
                deliver(id,()->listener.progress("한글 발음과 남성 음성 만드는 중…"));
                FreeInputVoice.Query query=voice.query(japanese);File wav=file(japanese);
                if(Thread.currentThread().isInterrupted())return;
                if(wav.length()<=44)voice.synthesize(query,wav);
                Result result=new Result(input,japanese,query.reading,"",wav);
                JSONObject value=new JSONObject().put("japanese",japanese).put("reading",query.reading).put("time",System.currentTimeMillis());cache.edit().putString(key(input),value.toString()).apply();trim();
                deliver(id,()->listener.ready(result,speak));
            }catch(ApiFailure e){deliver(id,()->listener.error(e.getMessage()));}
            catch(SocketTimeoutException e){deliver(id,()->listener.error("연결 응답이 늦어 중단했어요. 다시 눌러 주세요."));}
            catch(InterruptedIOException e){if(!Thread.currentThread().isInterrupted()&&id==generation)deliver(id,()->listener.error("연결 시간이 초과됐어요. 다시 눌러 주세요."));}
            catch(Exception e){if(!Thread.currentThread().isInterrupted()){String message=e instanceof IOException&&e.getMessage()!=null&&e.getMessage().matches("(?s).*[가-힣].*")?e.getMessage():"번역·음성을 준비하지 못했어요. 인터넷 연결을 확인하고 다시 눌러 주세요.";deliver(id,()->listener.error(message));}}
            catch(LinkageError e){deliver(id,()->listener.error("이 기기에서 새 문장 음성을 실행하지 못했어요. 기존 주문·질문은 계속 사용할 수 있어요."));}
        });
    }
    private static final class ApiFailure extends Exception {ApiFailure(String message){super(message);}}
    /** Separate bounded network calls keep stalled DNS/TLS off the sequential synthesis worker. */
    private static final class NetworkCall {
        volatile HttpURLConnection connection;
        volatile Future<String> future;
        void abort(){
            Future<String> task=future;boolean unfinished=task!=null&&!task.isDone();if(task!=null)task.cancel(true);
            HttpURLConnection current=connection;
            if(unfinished&&current!=null){Thread closer=new Thread(current::disconnect,"hitokoto-close-translation");closer.setDaemon(true);closer.start();}
        }
    }
    private String translate(String text)throws Exception {
        NetworkCall call=new NetworkCall();activeConnection=call;
        try{
            call.future=network.submit(()->translateNetwork(text,call));
            return call.future.get(15,TimeUnit.SECONDS);
        }catch(TimeoutException e){throw new ApiFailure("번역 서비스 응답이 15초 넘게 지연돼 중단했어요. 다시 눌러 주세요.");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException();}
        catch(RejectedExecutionException e){throw new ApiFailure("이전 연결을 정리 중이에요. 잠시 후 다시 눌러 주세요.");}
        catch(ExecutionException e){Throwable cause=e.getCause();if(cause instanceof ApiFailure)throw (ApiFailure)cause;if(cause instanceof SocketTimeoutException)throw new ApiFailure("번역 서비스 응답이 늦어요. 다시 눌러 주세요.");if(cause instanceof InterruptedIOException&&Thread.currentThread().isInterrupted())throw new InterruptedIOException();throw new ApiFailure("번역 서비스에 연결하지 못했어요. 와이파이·모바일 데이터를 확인하고 다시 눌러 주세요.");}
        finally{call.abort();if(activeConnection==call)activeConnection=null;}
    }
    private String translateNetwork(String text,NetworkCall call)throws Exception {
        if(text.contains("\n\n"))throw new ApiFailure("한 번에 한 문단씩 입력해 주세요.");
        if(text.getBytes(StandardCharsets.UTF_8).length>500)throw new ApiFailure("문장을 짧게 나누어 입력해 주세요.");
        String url="https://api.mymemory.translated.net/get?q="+URLEncoder.encode(text,"UTF-8")+"&langpair=ko%7Cja";
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();call.connection=connection;
        try{
            connection.setConnectTimeout(7000);connection.setReadTimeout(10000);connection.setRequestProperty("User-Agent","TravelHitokoto/0.1.8");
            int code=connection.getResponseCode();if(code==429)throw new ApiFailure("오늘 번역 서비스의 이용 한도에 도달했어요. 저장된 문장은 계속 쓸 수 있어요.");if(code!=200)throw new ApiFailure("번역 서비스에 연결하지 못했어요. 잠시 후 다시 눌러 주세요.");
            ByteArrayOutputStream body=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];for(int n;(n=in.read(buffer))!=-1;){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(body.size()+n>256*1024)throw new IOException("response too large");body.write(buffer,0,n);}}
            JSONObject data=new JSONObject(body.toString("UTF-8"));int status=data.optInt("responseStatus",500);
            if(data.optBoolean("quotaFinished")||status==429)throw new ApiFailure("오늘 번역 서비스의 이용 한도에 도달했어요. 저장된 문장은 계속 쓸 수 있어요.");
            if(status!=200)throw new ApiFailure("번역하지 못했어요. 짧은 한 문장으로 다시 입력해 주세요.");
            String translated=Html.fromHtml(data.getJSONObject("responseData").getString("translatedText"),Html.FROM_HTML_MODE_LEGACY).toString().trim();
            if(translated.isEmpty()||translated.length()>600||!translated.matches("(?s).*[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}].*"))throw new ApiFailure("일본어 번역 결과를 확인하지 못했어요. 문장을 조금 바꿔 주세요.");
            return translated;
        }finally{connection.disconnect();call.connection=null;}
    }
    private void trim(){
        List<Map.Entry<String,?>> entries=new ArrayList<>(cache.getAll().entrySet());if(entries.size()<=20)return;
        entries.sort(Comparator.comparingLong(e->{try{return new JSONObject(String.valueOf(e.getValue())).optLong("time");}catch(Exception ignored){return 0;}}));
        SharedPreferences.Editor edit=cache.edit();for(int i=0;i<entries.size()-20;i++){try{JSONObject old=new JSONObject(String.valueOf(entries.get(i).getValue()));file(old.getString("japanese")).delete();}catch(Exception ignored){}edit.remove(entries.get(i).getKey());}edit.apply();
    }
    public void cancel(){generation++;NetworkCall call=activeConnection;if(call!=null)call.abort();if(pending!=null){pending.cancel(true);pending=null;}}
    public void pause(){cancel();if(!closed)worker.submit(voice::close);}
    public boolean resourcesReady(){return VoiceResources.ready(context);}
    public boolean voiceSupported(){List<String> abis=Arrays.asList(Build.SUPPORTED_ABIS);return abis.contains("arm64-v8a")||abis.contains("x86_64");}
    public boolean metered(){android.net.ConnectivityManager manager=(android.net.ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);return manager==null||manager.isActiveNetworkMetered();}
    public boolean downloading(){return downloadThread!=null;}
    /** Pre-trip download. Survives tab changes and backgrounding; a partial file resumes on the next attempt. */
    public void download(DownloadListener listener){
        if(downloadThread!=null||closed)return;
        Thread thread=new Thread(()->{
            String failure=null;
            try{VoiceResources.ensure(context,message->main.post(()->{if(!closed)listener.progress(message);}));}
            catch(InterruptedIOException e){failure="받기를 멈췄어요. 다시 누르면 이어서 받아요.";}
            catch(Exception e){failure=e instanceof IOException&&e.getMessage()!=null&&e.getMessage().matches("(?s).*[가-힣].*")?e.getMessage():"음성 자료를 받지 못했어요. 인터넷을 확인하고 다시 누르면 이어서 받아요.";}
            downloadThread=null;final String result=failure;
            main.post(()->{if(closed)return;if(result==null)listener.done();else listener.failed(result);});
        },"hitokoto-voice-download");
        thread.setDaemon(true);downloadThread=thread;thread.start();
    }
    public void cancelDownload(){Thread thread=downloadThread;if(thread!=null)thread.interrupt();}
    public void close(){closed=true;cancelDownload();cancel();network.shutdownNow();worker.submit(voice::close);worker.shutdown();}
}
