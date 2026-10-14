package world.embers;

/** Pure gameplay rules, shared by live server code and regression tests. */
public final class Rules {
    private Rules() {}
    public static int clampStress(int value){return Math.max(0,Math.min(100,value));}
    public static int tier(int stress){return stress<=50?0:stress<=70?1:stress<=90?2:stress<100?3:4;}
    public static int relief(boolean premium,int dailyOrdinal){return premium?(dailyOrdinal==1?11:dailyOrdinal==2?8:5):(dailyOrdinal==1?7:dailyOrdinal==2?5:3);}
    public static int hungerPeriod(int stress){return switch(tier(stress)){case 1->800;case 2->600;case 3->400;case 4->200;default->0;};}
    public static float failChance(int stress){return stress>=100?.7f:stress>=71?.4f:0;}
    public static int dailyGrant(int used,int requested,int cap){return Math.max(0,Math.min(requested,cap-used));}
    public static float collapseHealth(float health){return health<=4?health:Math.max(4,health-1);}
    public static final int[] SOURCE_LIMITS={20,20,10,10,15,15,20};
    public static final int BURN_TICKS=2400;
    public static final int INHALE_TARGET=120;
}
