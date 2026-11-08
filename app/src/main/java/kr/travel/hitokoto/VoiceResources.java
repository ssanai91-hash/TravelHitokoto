package kr.travel.hitokoto;
import android.content.Context;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/** Pinned, hash-verified speech model/data download. Native executable code stays in the APK. */
public final class VoiceResources {
    private static final String MODEL="https://github.com/VOICEVOX/voicevox_vvm/releases/download/0.17.1/4.vvm";
    private static final String DICT="https://github.com/r9y9/open_jtalk/releases/download/v1.11.1/open_jtalk_dic_utf_8-1.11.tar.gz";
    static File ensure(Context context,Consumer<String> progress)throws Exception {
        File root=new File(context.getNoBackupFilesDir(),"free-input-voice-0171");if(!root.isDirectory()&&!root.mkdirs())throw new IOException("음성 자료를 저장할 공간을 확인해 주세요.");
        File model=new File(root,"4.vvm");
        if(model.length()!=59304904)download(MODEL,model,59304904,"716d2900f6b93ba22119af0e236c32fdbf6fea81c519b5d4830afcb2db349522","남성 음성",progress);
        File dictionary=new File(root,"open_jtalk_dic_utf_8-1.11");File marker=new File(root,"dictionary.ready");
        if(!marker.isFile()||!new File(dictionary,"sys.dic").isFile()){
            File archive=new File(root,"dictionary.tar.gz");download(DICT,archive,23646843,"fe6ba0e43542cef98339abdffd903e062008ea170b04e7e2a35da805902f382a","일본어 읽기 자료",progress);
            progress.accept("일본어 읽기 자료 준비 중…");unpack(archive,root);marker.createNewFile();archive.delete();
        }
        return root;
    }
    private static void cancelled()throws InterruptedIOException {if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("중단했어요.");}
    private static void download(String address,File target,long expected,String sha,String label,Consumer<String> progress)throws Exception {
        cancelled();HttpURLConnection connection=null;File part=new File(target+".part");
        try{
            URL url=new URL(address);
            for(int redirects=0;redirects<8;redirects++){
                if(!url.getProtocol().equals("https"))throw new IOException("안전한 음성 다운로드 주소를 확인하지 못했어요.");
                connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(20000);connection.setReadTimeout(30000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent","TravelHitokoto/0.1.7");int code=connection.getResponseCode();
                if(code>=300&&code<400){String location=connection.getHeaderField("Location");connection.disconnect();url=new URL(url,location);continue;}
                if(code!=200)throw new IOException("음성 자료 다운로드에 실패했어요. 인터넷 연결을 확인해 주세요 ("+code+").");break;
            }
            if(connection==null||connection.getResponseCode()!=200)throw new IOException("음성 자료 주소에 연결하지 못했어요.");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");long bytes=0,last=0;
            progress.accept(label+" 받는 중 · 0%");
            try(InputStream in=connection.getInputStream();OutputStream out=new FileOutputStream(part)){byte[] buffer=new byte[65536];for(int n;(n=in.read(buffer))!=-1;){cancelled();bytes+=n;if(bytes>expected)throw new IOException("음성 자료 크기가 올바르지 않아요.");out.write(buffer,0,n);digest.update(buffer,0,n);long now=System.currentTimeMillis();if(now-last>500){last=now;progress.accept(label+" 받는 중 · "+bytes*100/expected+"%");}}}
            StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b&255));
            if(bytes!=expected||!sha.equals(hash.toString()))throw new IOException("음성 자료 확인에 실패했어요. 다시 눌러 주세요.");
            if(target.exists()&&!target.delete())throw new IOException("음성 자료를 갱신하지 못했어요.");if(!part.renameTo(target))throw new IOException("음성 자료를 저장하지 못했어요.");
        }finally{if(connection!=null)connection.disconnect();part.delete();}
    }
    private static void readFully(InputStream in,byte[] data,int count)throws IOException {int at=0;while(at<count){int n=in.read(data,at,count-at);if(n<0)throw new EOFException();at+=n;}}
    private static void skip(InputStream in,long count)throws IOException {byte[] buffer=new byte[8192];while(count>0){int n=in.read(buffer,0,(int)Math.min(count,buffer.length));if(n<0)throw new EOFException();count-=n;}}
    private static String field(byte[] block,int start,int length){int end=start;while(end<start+length&&block[end]!=0)end++;return new String(block,start,end-start,StandardCharsets.UTF_8).trim();}
    private static void unpack(File archive,File root)throws IOException {
        String safe=root.getCanonicalPath()+File.separator;long total=0;
        try(InputStream in=new BufferedInputStream(new GZIPInputStream(new FileInputStream(archive)))){
            byte[] header=new byte[512],buffer=new byte[65536];
            while(true){cancelled();readFully(in,header,512);boolean empty=true;for(byte b:header)if(b!=0){empty=false;break;}if(empty)break;
                String name=field(header,0,100),prefix=field(header,345,155);if(!prefix.isEmpty())name=prefix+"/"+name;
                long size;try{size=Long.parseLong(field(header,124,12),8);}catch(NumberFormatException e){throw new IOException("읽기 자료 형식이 올바르지 않아요.",e);}
                total+=size;if(total>250L*1024*1024||size<0)throw new IOException("읽기 자료가 너무 커요.");
                File file=new File(root,name);if(!file.getCanonicalPath().startsWith(safe))throw new IOException("읽기 자료 경로가 올바르지 않아요.");
                if(header[156]=='5'){if(!file.isDirectory()&&!file.mkdirs())throw new IOException("읽기 자료를 저장하지 못했어요.");skip(in,size);}
                else if(header[156]=='0'||header[156]==0){File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("읽기 자료를 저장하지 못했어요.");try(OutputStream out=new FileOutputStream(file)){long remaining=size;while(remaining>0){cancelled();int n=in.read(buffer,0,(int)Math.min(buffer.length,remaining));if(n<0)throw new EOFException();out.write(buffer,0,n);remaining-=n;}}}
                else skip(in,size);
                skip(in,(512-size%512)%512);
            }
        }
    }
}
