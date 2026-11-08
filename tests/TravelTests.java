import kr.travel.hitokoto.*;
import java.util.*;
public class TravelTests {
    static void eq(Object got,Object expected){if(!Objects.equals(got,expected))throw new AssertionError(got+" != "+expected);}
    public static void main(String[] args){
        Phrase beer=Catalog.find("beer");
        eq(beer.korean(3),"생맥주 3잔 주세요");
        eq(beer.japanese(3),"生ビールを3杯ください。");
        eq(beer.spoken(3),"なまビールをさんばいください。");
        eq(beer.spoken(1),"なまビールをいっぱいください。");
        eq(beer.spoken(6),"なまビールをろっぱいください。");
        eq(beer.spoken(8),"なまビールをはっぱいください。");
        eq(beer.japanese(0),"生ビールを1杯ください。");
        eq(beer.japanese(99),"生ビールを10杯ください。");
        Phrase food=Catalog.find("gyoza");
        eq(food.spoken(2),"ぎょうざをににんまえください。");
        eq(food.spoken(10),"ぎょうざをじゅうにんまえください。");
        eq(food.korean(2),"교자 2인분 주세요");
        eq(Catalog.find("rice").korean(2),"공깃밥 2공기 주세요");
        eq(Catalog.find("this").spoken(2),"これをふたつください。");
        if(beer.key(2).equals(beer.key(3)))throw new AssertionError("Saved orders must retain quantity");
        Set<String> ids=new HashSet<>();
        for(Phrase p:Catalog.ALL){
            if(!ids.add(p.id))throw new AssertionError("duplicate id: "+p.id);
            if(p.ko.isEmpty()||p.ja.isEmpty()||p.reading.isEmpty())throw new AssertionError("incomplete "+p.id);
            if(!p.order)eq(p.japanese(3),p.japanese(1));
        }
        System.out.println("Passed: counter pronunciation, limits, quantity-specific favorites, catalog integrity ("+Catalog.ALL.size()+" entries)");
    }
}
