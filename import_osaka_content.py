"""Generate the bundled, offline Java catalog from the reviewed source JSON."""
import json,re
from pathlib import Path
P=Path(__file__).resolve().parent
D=json.loads((P/'app/src/main/assets/osaka-content.json').read_text(encoding='utf-8'))
L=json.loads((P/'app/src/main/assets/core-layout.json').read_text(encoding='utf-8'))
quote=lambda s:json.dumps(s,ensure_ascii=False)
place={p['id']:p['name'] for p in D['places']}
def category_for(x): return ('술·음료' if x['counter']=='cup' else '이자카야 안주') if x['category']=='술·이자카야' else x['category']
order_groups=list(dict.fromkeys(category_for(x) for x in D['items']))
question_groups=list(dict.fromkeys(x['category'] for x in D['questions']))+['공항','도움 요청']
def scenes(kind): return ','.join('{'+','.join(map(quote,[s['name'],*s['ids']]))+'}' for s in L[kind])
head='''package kr.travel.hitokoto;
import java.util.*;
/** Generated from assets/osaka-content.json; keep existing Phrase IDs on matches. */
public final class OsakaContent {
    public static final String[] ORDER_GROUPS={%s};
    public static final String[] QUESTION_GROUPS={%s};
    /** Curated on-screen lists: {scene, phrase ids...}. Everything else stays reachable through search. */
    public static final String[][] ORDER_SCENES={%s};
    public static final String[][] QUESTION_SCENES={%s};
    private static String norm(String value){return value.replaceAll("[\\\\s。？！?!（）()・·]", "");}
    /** Merge duplicates, but never two source rows sold in different units (tasting glass vs bottle). */
    private static Phrase same(List<Phrase> all,String ja,boolean order,String counter){for(Phrase p:all)if(p.order==order&&norm(p.ja).equals(norm(ja))&&(counter==null||!p.id.startsWith("osaka-")||counter.equals(p.counter)))return p;return null;}
    private static String legacyGroup(Phrase p){
        if(p.order){if(p.drink)return "술·음료";if(p.id.equals("rice"))return "돈카츠·규카츠";if(p.id.equals("this"))return "이자카야 안주";if(p.id.equals("takoyaki"))return "타코야키";if(p.id.equals("gyoza"))return "교자·중식";return "이자카야 안주";}
        switch(p.category){case "이자카야":return "식당 공통";case "교통":case "숙소":return "숙소·이동";case "야마자키":return "위스키·주류·견학";case "쇼핑":return "쇼핑 공통";case "기본 표현":return "대화 도움";default:return p.category;}
    }
    public static void extend(List<Phrase> all){
        for(Phrase p:all){p.category=legacyGroup(p);p.groups.add(p.category);}
        for(String[] r:ITEMS){
            Phrase p=same(all,r[3],true,r[6]);
            if(p==null){p=new Phrase(r[0],r[1],r[2],r[3],r[4],r[5],"",true,r[6].equals("cup"));all.add(p);}
            p.category=r[1];p.groups.clear();p.groups.add(r[1]);p.ko=r[2];p.kana=r[4];p.reading=r[5];p.counter=r[6];p.unitLabel=p.id.equals("rice")?"공기":r[7];p.retail=r[8].equals("true");p.detail=r[9];p.related=r[10];
        }
        for(String[] r:QUESTIONS){
            Phrase p=same(all,r[3],false,null);
            if(p==null){p=new Phrase(r[0],r[1],r[2],r[3],r[4],r[5],"",false,false);all.add(p);}
            p.groups.add(r[1]);if(p.kana.isEmpty())p.kana=r[4];
        }
        place(all,ORDER_SCENES);place(all,QUESTION_SCENES);
    }
    private static void place(List<Phrase> all,String[][] scenes){
        for(String[] scene:scenes)for(int i=1;i<scene.length;i++)for(Phrase p:all)if(p.id.equals(scene[i])){p.scene=scene[0];p.sceneOrder=i;break;}
    }
'''%(','.join(map(quote,order_groups)),','.join(map(quote,question_groups)),scenes('order'),scenes('question'))
items=[]
for x in D['items']:
 cat=category_for(x)
 detail=x['scope']+(' · '+x['note'] if x['note'] else '')
 related=' '.join(place[i] for i in x.get('related_place_ids',[]) if i in place)
 items.append(['osaka-'+x['id'],cat,x['ko'],x['ja'],x['kana'],x['reading'],x['counter'],x['unit_ko'],str(x['flow']=='재고 확인 후 구매').lower(),detail,related])
qs=[['osaka-'+x['id'],x['category'],x['ko'],x['ja'],x['kana'],x['reading']] for x in D['questions']]
java=head+'    private static final String[][] ITEMS={\n'+',\n'.join('        {'+','.join(map(quote,r))+'}' for r in items)+'\n    };\n'
java+='    private static final String[][] QUESTIONS={\n'+',\n'.join('        {'+','.join(map(quote,r))+'}' for r in qs)+'\n    };\n}\n'
(P/'app/src/main/java/kr/travel/hitokoto/OsakaContent.java').write_text(java,encoding='utf-8')
print('Prepared',len(items),'menu/product rows and',len(qs),'question rows')
