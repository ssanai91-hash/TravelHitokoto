package kr.travel.hitokoto;
import java.util.*;
/** Approximate Korean reading from VOICEVOX's analyzed mora text, never from kanji spelling. */
public final class KanaReading {
    private static final Map<String,String> MAP=new HashMap<>();
    static {
        String[] rows={"アイウエオ|아 이 우 에 오","カキクケコ|카 키 쿠 케 코","ガギグゲゴ|가 기 구 게 고","サシスセソ|사 시 스 세 소","ザジズゼゾ|자 지 즈 제 조","タチツテト|타 치 츠 테 토","ダヂヅデド|다 지 즈 데 도","ナニヌネノ|나 니 누 네 노","ハヒフヘホ|하 히 후 헤 호","バビブベボ|바 비 부 베 보","パピプペポ|파 피 푸 페 포","マミムメモ|마 미 무 메 모","ヤユヨ|야 유 요","ラリルレロ|라 리 루 레 로","ワヰヱヲ|와 이 에 오","ァィゥェォャュョヮヴ|아 이 우 에 오 야 유 요 와 부"};
        for(String row:rows){String[] p=row.split("\\|");String[] values=p[1].split(" ");for(int i=0;i<p[0].length();i++)MAP.put(p[0].substring(i,i+1),values[i]);}
        String[] combinations={"キャキュキョ|캬 큐 쿄","ギャギュギョ|갸 규 교","シャシュショ|샤 슈 쇼","ジャジュジョ|자 주 조","チャチュチョ|차 추 초","ニャニュニョ|냐 뉴 뇨","ヒャヒュヒョ|햐 휴 효","ビャビュビョ|뱌 뷰 뵤","ピャピュピョ|퍄 퓨 표","ミャミュミョ|먀 뮤 묘","リャリュリョ|랴 류 료","ファフィフェフォ|파 피 페 포","ティトゥディドゥ|티 투 디 두","ツァツィツェツォ|차 치 체 초","ウィウェウォ|위 웨 워","シェジェチェ|셰 제 체","ヴァヴィヴェヴォ|바 비 베 보","フュテュデュ|퓨 튜 듀","イェキェギェニェヒェビェピェミェリェ|예 케 게 녜 혜 볘 폐 몌 례"};
        for(String row:combinations){String[] p=row.split("\\|");String[] values=p[1].split(" ");for(int i=0;i<values.length;i++)MAP.put(p[0].substring(i*2,i*2+2),values[i]);}
    }
    public static String convert(String source){
        StringBuilder kana=new StringBuilder();for(int i=0;i<source.length();i++){char c=source.charAt(i);kana.append(c>=0x3041&&c<=0x3096?(char)(c+0x60):c);}
        StringBuilder out=new StringBuilder();String s=kana.toString();
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);if(c=='\''||c=='_')continue;
            if(c=='/'||Character.isWhitespace(c)){if(out.length()>0&&out.charAt(out.length()-1)!=' ')out.append(' ');continue;}
            if(c=='ン'||c=='ッ'){int n=out.length()-1;if(n>=0){char last=out.charAt(n);if(last>=0xAC00&&last<=0xD7A3&&(last-0xAC00)%28==0){out.setCharAt(n,(char)(last+(c=='ン'?4:19)));continue;}}out.append(c=='ン'?"응":"읏");continue;}
            if(c=='ー'){if(out.length()>0){char last=out.charAt(out.length()-1);if(last>=0xAC00&&last<=0xD7A3){out.append((char)(0xC544+((last-0xAC00)/28%21)*28));}}continue;}
            String mapped=null;if(i+1<s.length())mapped=MAP.get(s.substring(i,i+2));if(mapped!=null){out.append(mapped);i++;continue;}
            mapped=MAP.get(String.valueOf(c));if(mapped!=null)out.append(mapped);else if(c=='、'||c=='，')out.append(", ");else if(c=='。')out.append('.');else if(c=='？')out.append('?');else if(c=='！')out.append('!');
        }
        return out.toString().replaceAll(" +"," ").trim();
    }
    private KanaReading(){}
}
