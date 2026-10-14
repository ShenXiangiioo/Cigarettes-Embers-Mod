package world.embers;

public enum Brand {
    CABALLERO_BLACK("caballero_black", "执政官 · 黑曜", "Archon · Obsidian", true, false, 0x202327, 0xD8B773),
    CABALLERO_GREEN("caballero_green", "执政官 · 碧玉", "Archon · Jade", true, false, 0x26473E, 0xD8C58D),
    CABALLERO_BROWN("caballero_brown", "执政官 · 琥珀", "Archon · Amber", true, false, 0x54382D, 0xD7AD70),
    CHAPMAN("chapman", "北境 · 青苹果经典", "Northland · Green Apple", false, false, 0x54754B, 0xE5DEC5),
    CHAPMAN_SLIM("chapman_slim", "北境 · 青苹果纤细", "Northland · Apple Slim", false, true, 0xA6B68C, 0x304E39),
    SEVEN_STARS("seven_stars", "七曜", "Seven Luminaries", false, false, 0xECE8DE, 0x253B52),
    ZHONGHUA("zhonghua", "华章 · 经典", "Huazhang · Classic", true, false, 0x942E32, 0xE0BF79),
    ZHONGHUA_SLIM("zhonghua_slim", "华章 · 细支", "Huazhang · Slim", true, true, 0xB24442, 0xE6CB95),
    DOUBLE_HAPPINESS("double_happiness", "红韵双喜", "Crimson Duet", false, false, 0xE8DDC8, 0xAD4250),
    FURONGWANG("furongwang", "楚王", "King of Chu", false, false, 0xD1AC60, 0x44362A),
    HEHUA("hehua", "莲庭", "Lotus Court", false, false, 0x527A61, 0xE7D8AF),
    XUANHEMEN("xuanhemen", "玄夜", "Midnight", false, true, 0x547B94, 0xE2D3B5),
    HETIANXIA("hetianxia", "天下 · 经典", "Tianxia · Classic", true, false, 0x483652, 0xDAC088),
    HETIANXIA_SLIM("hetianxia_slim", "天下 · 细支", "Tianxia · Slim", true, true, 0x665071, 0xE0CAA2),
    HUANGHELOU("huanghelou", "侠心", "Wandering Heart", false, false, 0xD8C49C, 0x3E544C),
    HONGTASHAN("hongtashan", "雪山", "Snow Mountain", false, false, 0xEBE5D5, 0x9C3B38);
    public final String id, zh, en;
    public final boolean premium, slim;
    public final int color, accent;
    Brand(String id,String zh,String en,boolean premium,boolean slim,int color,int accent) {
        this.id=id;this.zh=zh;this.en=en;this.premium=premium;this.slim=slim;this.color=color;this.accent=accent;
    }
    public static Brand byId(int i) { return values()[Math.max(0,Math.min(values().length-1,i))]; }
    public boolean horizontalCase() { return ordinal()<3; }
    public boolean classic() { return !slim&&!horizontalCase(); }
    public float cigaretteRadius(){return slim?.0036f:classic()?.0068f:.0049f;}
    public float filterLength(){return horizontalCase()?.046f:.030f;}
    public float cigaretteLength(float remaining){return filterLength()+(horizontalCase()?.109f:classic()?.092f:.104f)*Math.max(0,Math.min(1,remaining));}
    public int burnTicks(){int base=Embers.BURN_SECONDS.get()*20;return (classic()?base*6/5:base)+300;}
}
