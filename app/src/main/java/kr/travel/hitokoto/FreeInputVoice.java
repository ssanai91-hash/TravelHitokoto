package kr.travel.hitokoto;

import android.content.Context;
import android.os.Build;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/** Same normal male speaker (style 11), synthesized locally only for new sentences. */
public final class FreeInputVoice implements AutoCloseable {
    @Structure.FieldOrder({"filename"}) public static class RuntimeOptions extends Structure implements Structure.ByValue {public String filename;}
    @Structure.FieldOrder({"acceleration_mode","cpu_num_threads"}) public static class InitializeOptions extends Structure implements Structure.ByValue {public int acceleration_mode=1;public short cpu_num_threads=2;}
    @Structure.FieldOrder({"on_existing"}) public static class ModelOptions extends Structure implements Structure.ByValue {public int on_existing=0;}
    @Structure.FieldOrder({"enable_interrogative_upspeak"}) public static class SynthesisOptions extends Structure implements Structure.ByValue {public byte enable_interrogative_upspeak=1;}
    public interface Core extends Library {
        int voicevox_onnxruntime_load_once(RuntimeOptions options,PointerByReference output);
        int voicevox_open_jtalk_rc_new(String dictionary,PointerByReference output);
        void voicevox_open_jtalk_rc_delete(Pointer pointer);
        int voicevox_synthesizer_new(Pointer runtime,Pointer dictionary,InitializeOptions options,PointerByReference output);
        void voicevox_synthesizer_delete(Pointer pointer);
        int voicevox_voice_model_file_open(String model,PointerByReference output);
        void voicevox_voice_model_file_delete(Pointer pointer);
        int voicevox_synthesizer_load_voice_model(Pointer synth,Pointer model,ModelOptions options);
        int voicevox_synthesizer_create_audio_query(Pointer synth,String text,int style,PointerByReference output);
        int voicevox_synthesizer_synthesis(Pointer synth,String query,int style,SynthesisOptions options,NativeLongByReference length,PointerByReference output);
        void voicevox_string_free(Pointer pointer);
        void voicevox_wav_free(Pointer pointer);
        String voicevox_error_result_to_message(int code);
    }
    public static final class Query {public final String json,reading;Query(String json,String reading){this.json=json;this.reading=reading;}}
    private final Context context;private Core core;private Pointer synthesizer;
    public FreeInputVoice(Context context){this.context=context.getApplicationContext();}
    private void check(int result)throws IOException {if(result!=0)throw new IOException("남성 음성을 준비하지 못했어요 ("+result+"). 다시 눌러 주세요.");}
    public void prepare(Consumer<String> progress)throws Exception {
        if(synthesizer!=null)return;
        if(!Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a")&&!Arrays.asList(Build.SUPPORTED_ABIS).contains("x86_64"))throw new IOException("새 문장 남성 음성은 64비트 휴대폰에서 지원해요. 기존 주문·질문은 계속 사용할 수 있어요.");
        File directory=VoiceResources.ensure(context,progress);
        progress.accept("남성 목소리 준비 중…");
        try{
            String libs=context.getApplicationInfo().nativeLibraryDir;
            System.load(libs+"/libc++_shared.so");
            Map<String,Object> options=new HashMap<>();options.put(Library.OPTION_STRING_ENCODING,"UTF-8");core=Native.load(libs+"/libvoicevox_core.so",Core.class,options);
            RuntimeOptions runtimeOptions=new RuntimeOptions();runtimeOptions.filename=libs+"/libvoicevox_onnxruntime.so";
            PointerByReference runtime=new PointerByReference(),dictionary=new PointerByReference(),synth=new PointerByReference(),model=new PointerByReference();
            check(core.voicevox_onnxruntime_load_once(runtimeOptions,runtime));
            check(core.voicevox_open_jtalk_rc_new(new File(directory,"open_jtalk_dic_utf_8-1.11").getAbsolutePath(),dictionary));
            try{check(core.voicevox_synthesizer_new(runtime.getValue(),dictionary.getValue(),new InitializeOptions(),synth));synthesizer=synth.getValue();}finally{core.voicevox_open_jtalk_rc_delete(dictionary.getValue());}
            check(core.voicevox_voice_model_file_open(new File(directory,"4.vvm").getAbsolutePath(),model));
            try{check(core.voicevox_synthesizer_load_voice_model(synthesizer,model.getValue(),new ModelOptions()));}finally{core.voicevox_voice_model_file_delete(model.getValue());}
        }catch(LinkageError e){close();throw new IOException("이 기기에서 새 문장용 남성 음성을 실행하지 못했어요. 기존 주문·질문은 계속 사용할 수 있어요.",e);}catch(Exception e){close();throw e;}
    }
    public Query query(String japanese)throws Exception {
        PointerByReference output=new PointerByReference();check(core.voicevox_synthesizer_create_audio_query(synthesizer,japanese,11,output));
        String json;try{json=output.getValue().getString(0,"UTF-8");}finally{core.voicevox_string_free(output.getValue());}
        JSONObject obj=new JSONObject(json);String kana=obj.optString("kana");String reading=KanaReading.convert(kana);
        if(reading.isEmpty())throw new IOException("한글 발음을 만들지 못했어요. 문장을 조금 짧게 바꿔 주세요.");
        return new Query(json,reading);
    }
    public void synthesize(Query query,File destination)throws Exception {
        NativeLongByReference size=new NativeLongByReference();PointerByReference output=new PointerByReference();
        check(core.voicevox_synthesizer_synthesis(synthesizer,query.json,11,new SynthesisOptions(),size,output));
        try{long length=size.getValue().longValue();if(length<=44||length>32*1024*1024)throw new IOException("문장이 너무 길어요. 짧게 나누어 입력해 주세요.");
            File part=new File(destination+".part");try(OutputStream out=new FileOutputStream(part)){long offset=0;while(offset<length){int chunk=(int)Math.min(65536,length-offset);out.write(output.getValue().getByteArray(offset,chunk));offset+=chunk;}}
            if(!part.renameTo(destination)){part.delete();throw new IOException("음성을 저장하지 못했어요.");}
        }finally{core.voicevox_wav_free(output.getValue());}
    }
    @Override public void close(){if(synthesizer!=null&&core!=null){core.voicevox_synthesizer_delete(synthesizer);synthesizer=null;}}
}
