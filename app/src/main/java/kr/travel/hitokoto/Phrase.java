package kr.travel.hitokoto;

public final class Phrase {
    public final String id;
    public String category, ko, ja, kana, reading, image;
    public final boolean order, drink;
    public String counter, unitLabel="", detail="", related="";
    public boolean retail;
    /** On-screen list this phrase belongs to; empty means search-only. */
    public String scene="";
    public int sceneOrder;
    /** Set only for saved free-input sentences, which carry their own synthesized audio. */
    public String audioPath="";
    public final java.util.Set<String> groups=new java.util.LinkedHashSet<>();
    public Phrase(String id, String category, String ko, String ja, String kana, String reading, String image, boolean order, boolean drink) {
        this.id=id; this.category=category; this.ko=ko; this.ja=ja; this.kana=kana; this.reading=reading; this.image=image; this.order=order; this.drink=drink; this.counter=drink?"cup":id.equals("this")?"piece":id.equals("rice")?"bowl":"serving";
    }
    public String unit() { return !unitLabel.isEmpty()?unitLabel:counter.equals("bowl")&&id.equals("rice")?"공기":Quantity.unitLabel(counter); }
    public String korean(int n) { return order ? ko + " " + Quantity.clamp(n) + unit() + " 주세요" : ko; }
    public String japanese(int n) { return order ? ja + "を" + Quantity.display(n,counter) + "ください。" : ja; }
    public String spoken(int n) { return order ? kana + "を" + Quantity.spoken(n,counter) + "ください。" : (kana.isEmpty() ? ja : kana); }
    public String pronunciation(int n) { return order ? reading + "오 " + Quantity.reading(n,counter) + " 쿠다사이" : reading; }
    public String key(int n) { return id + "@" + (order ? Quantity.clamp(n) : 1); }
    private static final String[][] SPELLINGS={{"돈가스","돈카츠"},{"돈까스","돈카츠"},{"까스","카츠"},{"타코야끼","타코야키"},{"다코야끼","타코야키"},{"다코야키","타코야키"},{"오꼬노미","오코노미"},{"야끼","야키"},{"라면","라멘"},{"메뉴판","메뉴"},{"교통카드","이코카"},{"사케","일본주"},{"락커","로커"},{"담배","흡연"},{"우설","소혀"}};
    private static String squeeze(String value){return value.replaceAll("\\s","").toLowerCase(java.util.Locale.ROOT);}
    /** Spacing-insensitive search that also accepts common Korean spellings of Japanese menu names. */
    public boolean matches(String query){
        String wanted=squeeze(query);if(wanted.isEmpty())return true;
        String text=squeeze(ko+ja+kana+reading+groups+related);if(text.contains(wanted))return true;
        for(String[] pair:SPELLINGS)if(wanted.contains(pair[0])&&text.contains(wanted.replace(pair[0],pair[1])))return true;
        return false;
    }
}
