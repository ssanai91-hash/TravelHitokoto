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
        Phrase bottle=Catalog.find("osaka-item-089"),tasting=Catalog.find("osaka-item-093");
        eq(bottle.counter,"bottle");eq(bottle.retail,true);eq(tasting.counter,"cup");eq(tasting.scene,"위스키");eq(tasting.spoken(3),"やまざきをさんばいください。");
        eq(Catalog.find("takoyaki").spoken(3),"たこやきをみっつください。");
        Set<String> placed=new HashSet<>();
        for(String[][] scenes:new String[][][]{OsakaContent.ORDER_SCENES,OsakaContent.QUESTION_SCENES})for(String[] scene:scenes){
            if(scene.length-1>14)throw new AssertionError("scene too long: "+scene[0]);
            for(int i=1;i<scene.length;i++){Phrase p=Catalog.find(scene[i]);if(p==null)throw new AssertionError("missing "+scene[i]);if(!placed.add(scene[i]))throw new AssertionError("placed twice: "+scene[i]);if(p.order!=(scenes==OsakaContent.ORDER_SCENES))throw new AssertionError("wrong tab: "+scene[i]);eq(p.scene,scene[0]);}
        }
        for(String[] search:new String[][]{{"돈까스","돈카츠"},{"돈가스","돈카츠"},{"타코야끼","타코야키"},{"오꼬노미야끼","오코노미야키"},{"라면","라멘"},{"메뉴판","메뉴"},{"교통카드","이코카"},{"코인 락커","코인로커"},{"담배","흡연"},{"생 맥주","생맥주"}}){
            boolean found=false;for(Phrase p:Catalog.ALL)if(p.matches(search[0])&&(p.ko+p.groups).replace(" ","").contains(search[1])){found=true;break;}
            if(!found)throw new AssertionError("search: "+search[0]);
        }
        System.out.println("Passed: counter pronunciation, limits, quantity-specific favorites, catalog integrity ("+Catalog.ALL.size()+" entries)");
    }
}
