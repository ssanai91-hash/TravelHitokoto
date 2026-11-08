package kr.travel.hitokoto;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.os.*;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** One bundled male voice. No device TTS, voice selection or pitch conversion. */
public final class Speech {
    private final Activity activity;
    private final Runnable changed;
    private final AudioManager audio;
    private final AudioAttributes attributes;
    private final AudioFocusRequest focus;
    private MediaPlayer player;
    private int epoch;
    private boolean closed;
    private final java.util.ArrayDeque<String> queue=new java.util.ArrayDeque<>();
    private final Handler main=new Handler(Looper.getMainLooper());
    public boolean ready=true,loading=false,speaking=false;
    public String status="남성 음성 준비됨";
    public Speech(Activity activity,Runnable changed){
        this.activity=activity;this.changed=changed;
        audio=(AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);
        attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes).setOnAudioFocusChangeListener(value->{if(value<0)main.post(this::stop);}).build();
        // Retire legacy voice preferences while preserving favorites and phrase choices.
        SharedPreferences prefs=activity.getSharedPreferences("travel",Context.MODE_PRIVATE);
        SharedPreferences.Editor edit=prefs.edit();
        for(String key:prefs.getAll().keySet())if(key.equals("voice-tone")||key.startsWith("voice-choice"))edit.remove(key);
        edit.apply();
        java.io.File old=new java.io.File(activity.getCacheDir(),"voice-audio-v1");
        java.io.File[] files=old.listFiles();if(files!=null)for(java.io.File file:files)file.delete();old.delete();
    }
    public void refresh(){}
    public boolean speak(String text,boolean slow){queue.clear();return begin(text,null,slow);}
    public boolean speakFile(java.io.File file,boolean slow){queue.clear();return begin(null,file,slow);}
    /** Plays bundled clips one after another with a short pause, for a multi-item order. */
    public boolean speakAll(java.util.List<String> texts){stop();if(texts.isEmpty())return false;queue.addAll(texts);return begin(queue.poll(),null,false);}
    private boolean begin(String text,java.io.File file,boolean slow){
        if(closed)return false;
        halt();final int request=++epoch;
        if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){status="다른 소리가 끝난 뒤 다시 눌러 주세요";changed.run();return false;}
        try{
            MediaPlayer media=new MediaPlayer();player=media;media.setAudioAttributes(attributes);
            if(file!=null)media.setDataSource(file.getAbsolutePath());
            else try(AssetFileDescriptor fd=activity.getAssets().openFd("voice/male/"+digest(text)+".ogg")){media.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}
            media.setOnPreparedListener(p->{
                if(request!=epoch||closed)return;
                try{
                    // Slow playback keeps the same speaker and the original pitch.
                    if(slow)p.setPlaybackParams(new PlaybackParams().allowDefaults().setPitch(1f).setSpeed(0.8f));
                    p.start();status="남성 음성 재생 중";changed.run();
                }catch(Exception e){fail(request,"음성을 재생하지 못했어요. 다시 눌러 주세요.");}
            });
            media.setOnCompletionListener(p->{
                if(request!=epoch)return;
                if(queue.isEmpty()){stop();return;}
                p.release();if(player==p)player=null;
                main.postDelayed(()->{if(request==epoch&&!closed&&!queue.isEmpty())begin(queue.poll(),null,false);},350);
            });
            media.setOnErrorListener((p,what,extra)->{fail(request,"음성을 재생하지 못했어요. 다시 눌러 주세요.");return true;});
            speaking=true;status="남성 음성 재생 중";changed.run();media.prepareAsync();return true;
        }catch(Exception e){stop();status="이 문장의 음성을 열지 못했어요. 다시 준비한 뒤 눌러 주세요.";changed.run();return false;}
    }
    private static String digest(String text)throws Exception{
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder key=new StringBuilder();
        for(byte b:bytes)key.append(String.format(Locale.ROOT,"%02x",b&255));return key.toString();
    }
    private void fail(int request,String message){if(request!=epoch)return;stop();status=message;Toast.makeText(activity,message,Toast.LENGTH_LONG).show();changed.run();}
    public void stop(){queue.clear();halt();}
    private void halt(){epoch++;if(player!=null){player.release();player=null;}speaking=false;audio.abandonAudioFocusRequest(focus);status="남성 음성 준비됨";changed.run();}
    public void destroy(){closed=true;stop();}
}
