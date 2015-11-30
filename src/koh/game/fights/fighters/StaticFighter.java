package koh.game.fights.fighters;

import koh.game.entities.mob.MonsterGrade;
import koh.game.fights.Fighter;
import koh.game.fights.IFightObject;
import koh.protocol.client.enums.StatsEnum;

/**
 *
 * @author Neo-Craft
 */
public abstract class StaticFighter extends Fighter {

    public MonsterGrade Grade;

    public StaticFighter(koh.game.fights.Fight Fight, Fighter Summoner) {
        super(Fight, Summoner);
    }
    
    public void AdjustStats() {
        this.Stats.addBase(StatsEnum.Vitality, (short) ((double) this.Stats.getEffect(StatsEnum.Vitality).Base * (1.0 + (double) this.Summoner.Level() / 100.0)));
        this.Stats.addBase(StatsEnum.Intelligence, (short) ((double) this.Stats.getEffect(StatsEnum.Intelligence).Base * (1.0 + (double) this.Summoner.Level() / 100.0)));
        this.Stats.addBase(StatsEnum.Chance, (short) ((double) this.Stats.getEffect(StatsEnum.Chance).Base * (1.0 + (double) this.Summoner.Level() / 100.0)));
        this.Stats.addBase(StatsEnum.Strength, (short) ((double) this.Stats.getEffect(StatsEnum.Strength).Base * (1.0 + (double) this.Summoner.Level() / 100.0)));
        this.Stats.addBase(StatsEnum.Agility, (short) ((double) this.Stats.getEffect(StatsEnum.Agility).Base * (1.0 + (double) this.Summoner.Level() / 100.0)));
        this.Stats.addBase(StatsEnum.Wisdom, (short) ((double) this.Stats.getEffect(StatsEnum.Wisdom).Base * (1.0 + (double) this.Summoner.Level() / 100.0)));
    }

    @Override
    public int MaxAP() {
        return 0;
    }

    @Override
    public int MaxMP() {
        return 0;
    }

    @Override
    public int AP() {
        return 0;

    }

    @Override
    public int MP() {
        return 0;
    }

    private boolean firstTurn = true;

    public void onBeginTurn() {
        if (firstTurn) {
            this.Fight.AffectSpellTo(this, this, this.Grade.Grade, this.Grade.Monster().spells);
            this.firstTurn = false;
        }
    }

    @Override
    public int compareTo(IFightObject obj) {
        return Priority().compareTo(obj.Priority());
    }

    @Override
    public FightObjectType ObjectType() {
        return FightObjectType.OBJECT_STATIC;
    }

}
