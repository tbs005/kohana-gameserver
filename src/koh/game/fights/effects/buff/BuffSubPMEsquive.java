package koh.game.fights.effects.buff;

import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import koh.protocol.client.enums.ActionIdEnum;
import koh.protocol.client.enums.FightDispellableEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.messages.game.actions.fight.GameActionFightDodgePointLossMessage;
import koh.protocol.messages.game.actions.fight.GameActionFightPointsVariationMessage;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import koh.protocol.types.game.actions.fight.FightTriggeredEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public class BuffSubPMEsquive extends BuffEffect {

    public BuffSubPMEsquive(EffectCast CastInfos, Fighter Target) {
        super(CastInfos, Target, BuffActiveType.ACTIVE_STATS, BuffDecrementType.TYPE_ENDTURN);
    }

    @Override
    public int ApplyEffect(MutableInt DamageValue, EffectCast DamageInfos) {
        MutableInt LostPM = new MutableInt(CastInfos.RandomJet(Target));
        LostPM.setValue(LostPM.getValue() > Target.AP() ? Target.AP() : LostPM.getValue());
        CastInfos.DamageValue = Target.CalculDodgeAPMP(CastInfos.Caster, LostPM.intValue(), true, CastInfos.Duration > 0);

        if (CastInfos.DamageValue != LostPM.intValue()) {
            Target.Fight.sendToField(new GameActionFightDodgePointLossMessage(ActionIdEnum.ACTION_FIGHT_SPELL_DODGED_PM, Caster.ID, Target.ID, LostPM.getValue() - CastInfos.DamageValue));
        }

        if (CastInfos.DamageValue > 0) {
            BuffStats BuffStats = new BuffStats(new EffectCast(StatsEnum.Sub_PM, this.CastInfos.SpellId, (short) this.CastInfos.SpellId, 0, null, this.CastInfos.Caster, null, false, StatsEnum.NOT_DISPELLABLE, CastInfos.DamageValue, CastInfos.SpellLevel, Duration, 0), this.Target);
            BuffStats.ApplyEffect(LostPM, null);
            this.Target.Buffs.AddBuff(BuffStats);
            if (Target.ID == Target.Fight.CurrentFighter.ID) {
                // Target.fight.sendToField(new GameActionFightPointsVariationMessage(ActionIdEnum.ACTION_CHARACTER_MOVEMENT_POINTS_LOST, this.Caster.id, Target.id, (short) -CastInfos.DamageValue));
            }
        }

        return super.ApplyEffect(DamageValue, DamageInfos);
    }

    @Override
    public AbstractFightDispellableEffect GetAbstractFightDispellableEffect() {
        return new FightTriggeredEffect(this.GetId(), this.Target.ID, (short) this.Duration, FightDispellableEnum.REALLY_NOT_DISPELLABLE, this.CastInfos.SpellId, this.CastInfos.Effect.effectUid, 0, (short) this.CastInfos.Effect.diceNum, (short) this.CastInfos.Effect.diceSide, (short) this.CastInfos.Effect.value, (short) 0/*(this.CastInfos.Effect.delay)*/);
    }

}
