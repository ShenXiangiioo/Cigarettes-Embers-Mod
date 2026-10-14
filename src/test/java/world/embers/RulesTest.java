package world.embers;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RulesTest {
    @Test void allStressBoundariesAreInclusive(){int[] input={0,50,51,70,71,90,91,99,100};int[] tier={0,0,1,1,2,2,3,3,4};for(int i=0;i<input.length;i++)assertEquals(tier[i],Rules.tier(input[i]),"stress "+input[i]);}
    @Test void mixedBrandsShareOneDailySequence(){int stress=90;stress-=Rules.relief(false,1);stress-=Rules.relief(true,2);stress-=Rules.relief(false,3);assertEquals(72,stress);assertEquals(5,Rules.relief(true,20));}
    @Test void capClipsFinalDamageGrant(){assertEquals(1,Rules.dailyGrant(9,2,10));assertEquals(0,Rules.dailyGrant(10,2,10));assertEquals(0,Rules.dailyGrant(12,2,10));}
    @Test void collapseNeverKillsOrHeals(){assertEquals(4,Rules.collapseHealth(4.5f));assertEquals(4,Rules.collapseHealth(4));assertEquals(2,Rules.collapseHealth(2));assertEquals(19,Rules.collapseHealth(20));}
    @Test void hungerAndIgnitionFollowTiers(){assertEquals(0,Rules.hungerPeriod(50));assertEquals(800,Rules.hungerPeriod(51));assertEquals(600,Rules.hungerPeriod(71));assertEquals(400,Rules.hungerPeriod(91));assertEquals(200,Rules.hungerPeriod(100));assertEquals(0,Rules.failChance(70));assertEquals(.4f,Rules.failChance(71));assertEquals(.7f,Rules.failChance(100));}
}
