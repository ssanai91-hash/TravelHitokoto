package kr.travel.hitokoto;

/** Japanese selling units. The same 1..10 limit is used for saved order quantities. */
public final class Quantity {
    private Quantity() {}
    private static final String[] HAI={"","いっぱい","にはい","さんばい","よんはい","ごはい","ろっぱい","ななはい","はっぱい","きゅうはい","じゅっぱい"};
    private static final String[] HAI_KO={"","잇파이","니하이","산바이","욘하이","고하이","롯파이","나나하이","핫파이","큐우하이","쥽파이"};
    private static final String[] TSU={"","ひとつ","ふたつ","みっつ","よっつ","いつつ","むっつ","ななつ","やっつ","ここのつ","じゅっこ"};
    private static final String[] TSU_KO={"","히토츠","후타츠","밋츠","욧츠","이츠츠","뭇츠","나나츠","얏츠","코코노츠","쥿코"};
    private static final String[] SERVE={"","いちにんまえ","ににんまえ","さんにんまえ","よにんまえ","ごにんまえ","ろくにんまえ","ななにんまえ","はちにんまえ","きゅうにんまえ","じゅうにんまえ"};
    private static final String[] SERVE_KO={"","이치닌마에","니닌마에","산닌마에","요닌마에","고닌마에","로쿠닌마에","나나닌마에","하치닌마에","큐우닌마에","주우닌마에"};
    private static final String[] HON={"","いっぽん","にほん","さんぼん","よんほん","ごほん","ろっぽん","ななほん","はっぽん","きゅうほん","じゅっぽん"};
    private static final String[] HON_KO={"","잇폰","니혼","산본","욘혼","고혼","롯폰","나나혼","핫폰","큐우혼","쥿폰"};
    private static final String[] NUM={"","いち","に","さん","よん","ご","ろく","なな","はち","きゅう","じゅう"};
    private static final String[] NUM_KO={"","이치","니","산","욘","고","로쿠","나나","하치","큐우","주우"};
    private static final String[] SARA={"","ひとさら","ふたさら","さんさら","よんさら","ごさら","ろくさら","ななさら","はちさら","きゅうさら","じゅっさら"};
    private static final String[] SARA_KO={"","히토사라","후타사라","산사라","욘사라","고사라","로쿠사라","나나사라","하치사라","큐우사라","쥿사라"};
    private static final String[] CHAKU={"","いっちゃく","にちゃく","さんちゃく","よんちゃく","ごちゃく","ろくちゃく","ななちゃく","はっちゃく","きゅうちゃく","じゅっちゃく"};
    private static final String[] CHAKU_KO={"","잇차쿠","니차쿠","산차쿠","욘차쿠","고차쿠","로쿠차쿠","나나차쿠","핫차쿠","큐우차쿠","쥿차쿠"};
    private static final String[] SOKU={"","いっそく","にそく","さんぞく","よんそく","ごそく","ろくそく","ななそく","はっそく","きゅうそく","じゅっそく"};
    private static final String[] SOKU_KO={"","잇소쿠","니소쿠","산조쿠","욘소쿠","고소쿠","로쿠소쿠","나나소쿠","핫소쿠","큐우소쿠","쥿소쿠"};
    private static final String[] TEN={"","いってん","にてん","さんてん","よんてん","ごてん","ろくてん","ななてん","はってん","きゅうてん","じゅってん"};
    private static final String[] TEN_KO={"","잇텐","니텐","산텐","욘텐","고텐","로쿠텐","나나텐","핫텐","큐우텐","쥿텐"};
    private static final String[] FUKURO={"","ひとふくろ","ふたふくろ","さんふくろ","よんふくろ","ごふくろ","ろくふくろ","ななふくろ","はちふくろ","きゅうふくろ","じゅっふくろ"};
    private static final String[] FUKURO_KO={"","히토후쿠로","후타후쿠로","산후쿠로","욘후쿠로","고후쿠로","로쿠후쿠로","나나후쿠로","하치후쿠로","큐우후쿠로","쥿후쿠로"};
    private static final String[] KAN={"","いっかん","にかん","さんかん","よんかん","ごかん","ろっかん","ななかん","はっかん","きゅうかん","じゅっかん"};
    private static final String[] KAN_KO={"","잇칸","니칸","산칸","욘칸","고칸","롯칸","나나칸","핫칸","큐우칸","쥿칸"};
    public static int clamp(int n){return Math.max(1,Math.min(10,n));}
    public static String unitLabel(String unit){switch(unit){case "cup":return "잔";case "bowl":return "그릇";case "serving":return "인분";case "dish":return "접시";case "stick":return "꼬치";case "bottle":return "병";case "set":return "세트";case "garment":return "벌";case "pair":return "켤레";case "item":return "점";case "bag":return "봉지";default:return "개";}}
    public static String display(int n,String unit){n=clamp(n);switch(unit){case "cup":case "bowl":return n+"杯";case "serving":return n+"人前";case "dish":return n+"皿";case "stick":case "bottle":return n+"本";case "set":return n+"セット";case "garment":return n+"着";case "pair":return n+"足";case "item":return n+"点";case "bag":return n+"袋";case "sushi_piece":return n+"貫";default:return n==10?"10個":n+"つ";}}
    private static String value(int n,String unit,boolean ko){n=clamp(n);switch(unit){case "cup":case "bowl":return (ko?HAI_KO:HAI)[n];case "serving":return (ko?SERVE_KO:SERVE)[n];case "dish":return (ko?SARA_KO:SARA)[n];case "stick":case "bottle":return (ko?HON_KO:HON)[n];case "set":return (ko?NUM_KO:NUM)[n]+(ko?"셋토":"セット");case "garment":return (ko?CHAKU_KO:CHAKU)[n];case "pair":return (ko?SOKU_KO:SOKU)[n];case "item":return (ko?TEN_KO:TEN)[n];case "bag":return (ko?FUKURO_KO:FUKURO)[n];case "sushi_piece":return (ko?KAN_KO:KAN)[n];default:return (ko?TSU_KO:TSU)[n];}}
    public static String spoken(int n,String unit){return value(n,unit,false);}
    public static String reading(int n,String unit){return value(n,unit,true);}
    public static String spoken(int n,boolean drink){return spoken(n,drink?"cup":"piece");}
    public static String reading(int n,boolean drink){return reading(n,drink?"cup":"piece");}
    public static String display(int n,boolean drink){return display(n,drink?"cup":"piece");}
}
