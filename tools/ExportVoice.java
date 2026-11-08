package kr.travel.hitokoto;
import java.util.*;
import java.nio.charset.StandardCharsets;
/** Build-time corpus: every phrase and all supported quantity/purchase modes. */
public final class ExportVoice {
    public static void main(String[] args) {
        Set<String> texts=new LinkedHashSet<>();
        for(Phrase p:Catalog.ALL){
            if(p.order){
                for(int n=1;n<=10;n++)texts.add(p.spoken(n));
                if(p.retail){texts.add(p.kana+"はありますか？");texts.add(p.kana+"はいくらですか？");}
            } else texts.add(p.spoken(1));
        }
        for(String t:texts)System.out.println(Base64.getEncoder().encodeToString(t.getBytes(StandardCharsets.UTF_8)));
    }
}
