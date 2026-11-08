package kr.travel.hitokoto;

public final class Phrase {
    public final String id;
    public String category, ko, ja, kana, reading, image;
    public final boolean order, drink;
    public String counter, unitLabel="", detail="", related="";
    public boolean retail;
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
}
