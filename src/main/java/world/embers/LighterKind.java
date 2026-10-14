package world.embers;
public enum LighterKind {
    PIEZO("piezo", "琥珀 · 压电丁烷火机", "Amber · Piezo", 0xC48843, false, false),
    WHEEL("wheel", "海盐 · 砂轮丁烷火机", "Sea Salt · Flint", 0x547D89, false, false),
    JET_BLACK("jet_black", "夜航 · 单焰防风火机", "Nightfall · Jet", 0x292F36, false, true),
    JET_COPPER("jet_copper", "赤铜 · 双焰防风火机", "Copper · Twin Jet", 0xAC6B45, false, true),
    JET_SILVER("jet_silver", "霜银 · 三焰防风火机", "Frost · Triple Jet", 0xAEBBC0, false, true),
    OIL_BRASS("oil_brass", "旧时 · 黄铜煤油火机", "Heritage · Brass", 0xB5975D, true, false),
    OIL_CHROME("oil_chrome", "镜月 · 镀铬煤油火机", "Moonlight · Chrome", 0xC6C8C8, true, false),
    OIL_BLACK("oil_black", "玄铁 · 黑金煤油火机", "Onyx · Oil", 0x343135, true, false);
    public final String id,zh,en;public final int color;public final boolean lid,jet;
    LighterKind(String id,String zh,String en,int color,boolean lid,boolean jet){this.id=id;this.zh=zh;this.en=en;this.color=color;this.lid=lid;this.jet=jet;}
    public static LighterKind byId(int i){return values()[Math.max(0,Math.min(7,i))];}
}
