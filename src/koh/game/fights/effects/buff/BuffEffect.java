package koh.game.fights.effects.buff;

import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import koh.protocol.client.enums.SpellIDEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public abstract class BuffEffect {

    public static final int[] EROSION_DAMAGE_EFFECTS_IDS = new int[]{1092, 1093, 1094, 1095, 1096};
    public static final int[] HEALING_EFFECTS_IDS = new int[]{81, 108, 1109, 90};
    public static final int[] IMMEDIATE_BOOST_EFFECTS_IDS = new int[]{266, 268, 269, 271, 414};
    public static final int[] BOMB_SPELLS_IDS = new int[]{2796, 2797, 2808};
    public static final int[] SPLASH_EFFECTS_IDS = new int[]{1123, 1124, 1125, 1126, 1127, 1128};
    public static final int[] MP_BASED_DAMAGE_EFFECTS_IDS = new int[]{1012, 1013, 1014, 1015, 1016};
    public static final int[] HP_BASED_DAMAGE_EFFECTS_IDS = new int[]{672, 85, 86, 87, 88, 89};
    public static final int[] TARGET_HP_BASED_DAMAGE_EFFECTS_IDS = new int[]{1067, 1068, 1069, 1070, 1071};
    public static final int[] TRIGGERED_EFFECTS_IDS = new int[]{138, 1040};
    public static final int[] NO_BOOST_EFFECTS_IDS = new int[]{144, 82};

    public BuffDecrementType DecrementType;
    public BuffActiveType ActiveType;
    public EffectCast CastInfos;
    public Fighter caster;
    public Fighter target;
    public int Duration, Delay;
    private int Uid = -1;

    public int GetId() {
        if (this.Uid == -1) {
            Uid = target.getNextBuffUid().incrementAndGet();
        }
        return this.Uid;
    }

    //TODO: Create List in Setting
    public boolean isDebuffable() {
        switch (this.CastInfos.EffectType) {
            case Damage_Armor_Reduction:
                return CastInfos.SpellId != SpellIDEnum.TREVE;
            case Add_State:
            case Change_Appearance:
            case CHATIMENT:
            //Domage de sort
            case TRANSFORMATION:
                return false;
        }
        return this.CastInfos.SubEffect != StatsEnum.NOT_DISPELLABLE;

        //return true;
    }

    public BuffEffect(EffectCast CastInfos, Fighter target, BuffActiveType ActiveType, BuffDecrementType DecrementType) {
        this.CastInfos = CastInfos;

        //this.Duration = (CastInfos.Duration == -1) ? -1 : (target.fight.currentFighter == target /*&& CastInfos.Duration == 0*/ ? CastInfos.Duration + 1 : CastInfos.Duration) - CastInfos.Delay();
        this.Duration = (CastInfos.Duration == -1) ? -1 : (DecrementType == BuffDecrementType.TYPE_ENDTURN ? CastInfos.Duration : (CastInfos.Duration) - CastInfos.Delay());
        
        //System.out.println(target.fight.currentFighter == target);
        if (DecrementType == BuffDecrementType.TYPE_ENDTURN && target.getID() == CastInfos.caster.getID()) {
            this.Duration++;
        }
        this.Delay = CastInfos.Delay();
        this.caster = CastInfos.caster;
        this.target = target;

        this.ActiveType = ActiveType;
        this.DecrementType = DecrementType;
    }

    public int applyEffect(MutableInt DamageValue, EffectCast DamageInfos) {
        return this.target.tryDie(this.caster.getID());
    }

    public abstract AbstractFightDispellableEffect getAbstractFightDispellableEffect();

    /// <summary>
    /// Fin du buff
    /// </summary>
    /// <returns></returns>
    public int removeEffect() {
        return this.target.tryDie(this.caster.getID());
    }

    /// <summary>
    /// decrement le buff
    /// </summary>
    public int decrementDuration() {
        this.Duration--;

        this.CastInfos.FakeValue = 0;

        return this.Duration;
    }

    public int decrementDuration(int Duration) {
        this.Duration -= Duration;

        this.CastInfos.FakeValue = 0;

        return this.Duration;
    }

}
