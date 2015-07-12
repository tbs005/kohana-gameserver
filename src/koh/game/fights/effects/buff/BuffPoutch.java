package koh.game.fights.effects.buff;

import java.util.ArrayList;
import java.util.Arrays;
import koh.game.Main;
import koh.game.dao.SpellDAO;
import koh.game.entities.environments.cells.Zone;
import koh.game.entities.maps.pathfinding.MapPoint;
import koh.game.entities.spells.EffectInstanceDice;
import koh.game.entities.spells.SpellLevel;
import koh.game.fights.Fight;
import koh.game.fights.FightCell;
import koh.game.fights.Fighter;
import koh.game.fights.IFightObject;
import koh.game.fights.effects.EffectBase;
import koh.game.fights.effects.EffectCast;
import koh.game.fights.effects.EffectDamage;
import koh.protocol.client.enums.FightDispellableEnum;
import static koh.protocol.client.enums.SpellShapeEnum.X;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import koh.protocol.types.game.actions.fight.FightTriggeredEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public class BuffPoutch extends BuffEffect {

    public BuffPoutch(EffectCast CastInfos, Fighter Target) {
        super(CastInfos, Target, BuffActiveType.ACTIVE_ATTACKED_AFTER_JET, BuffDecrementType.TYPE_ENDTURN);
    }

    @Override
    public int ApplyEffect(MutableInt DamageValue, EffectCast DamageInfos) {
        if (DamageInfos.IsReflect || DamageInfos.IsReturnedDamages || DamageInfos.IsPoison) {
            return -1;
        }
        // mort
        if (Caster.Dead()) {
            //Target.Buffs.RemoveBuff(this);
            return -1;
        }

        SpellLevel SpellLevel = SpellDAO.Spells.get(CastInfos.Effect.diceNum).spellLevels[0];
        double num1 = Fight.RANDOM.nextDouble();
        double num2 = (double) Arrays.stream(SpellLevel.effects).mapToInt(x -> x.random).sum();
        boolean flag = false;
        for (EffectInstanceDice Effect : SpellLevel.effects) {
            Effect.parseZone();
            ArrayList<Fighter> Targets = new ArrayList<>();
            for (short Cell : (new Zone(Effect.ZoneShape(), Effect.ZoneSize(), MapPoint.fromCellId(Target.CellId()).advancedOrientationTo(MapPoint.fromCellId(Target.CellId()), true))).GetCells(Target.CellId())) {
                FightCell FightCell = Target.Fight.GetCell(Cell);
                if (FightCell != null) {
                    if (FightCell.HasGameObject(IFightObject.FightObjectType.OBJECT_FIGHTER) | FightCell.HasGameObject(IFightObject.FightObjectType.OBJECT_CAWOTTE)) {

                        for (Fighter Target : FightCell.GetObjectsAsFighter()) {
                            if (Effect.IsValidTarget(this.Target, Target) && EffectInstanceDice.verifySpellEffectMask(this.Target, Target, Effect)) {
                                if (Effect.targetMask.equals("C") && this.Target.GetCarriedActor() == Target.ID) {
                                    continue;
                                } else if (Effect.targetMask.equals("a,A") && this.Target.GetCarriedActor() != 0 & this.Target.ID == Target.ID) {
                                    continue;
                                }
                                Targets.add(Target);

                            }
                        }
                    }
                }
            }
            if (Effect.random > 0) {
                if (!flag) {
                    if (num1 > (double) Effect.random / num2) {
                        num1 -= (double) Effect.random / num2;
                        continue;
                    } else {
                        flag = true;
                    }
                } else {
                    continue;
                }
            }
            EffectCast CastInfos = new EffectCast(Effect.EffectType(), SpellLevel.spellId, this.Target.CellId(), num1, Effect, this.Target, Targets, false, StatsEnum.NONE, DamageValue.intValue(), SpellLevel);
            CastInfos.targetKnownCellId = Target.CellId();
            if (EffectBase.TryApplyEffect(CastInfos) == -3) {
                return -3;
            }
        }

        /*int Apply = -1;
         for (short Cell : (new Zone(X, (byte) 1, MapPoint.fromCellId(Target.CellId()).advancedOrientationTo(MapPoint.fromCellId(Target.CellId()), true))).GetCells(Target.CellId())) {
         FightCell FightCell = this.Target.Fight.GetCell(Cell);
         if (FightCell != null) {
         if (FightCell.HasGameObject(IFightObject.FightObjectType.OBJECT_FIGHTER) | FightCell.HasGameObject(IFightObject.FightObjectType.OBJECT_CAWOTTE)) {
         for (Fighter Target : FightCell.GetObjectsAsFighter()) {
         int newValue = EffectDamage.ApplyDamages(DamageInfos, Target, new MutableInt((DamageInfos.RandomJet(Target) * 20) / 100));
         if (newValue < Apply) {
         Apply = newValue;
         }
         }
         }
         }
         }
         return Apply;*/
        //return EffectDamage.ApplyDamages(DamageInfos, Target, new MutableInt((DamageInfos.RandomJet(Target) * 20) / 100)); //TIDO: ChangeRandom Jet to DamageJet direct
        return super.ApplyEffect(DamageValue, DamageInfos);
    }

    @Override
    public AbstractFightDispellableEffect GetAbstractFightDispellableEffect() {
        return new FightTriggeredEffect(this.GetId(), this.Target.ID, (short) this.Duration, FightDispellableEnum.REALLY_NOT_DISPELLABLE, this.CastInfos.SpellId, this.CastInfos.Effect.effectUid, 0, (short) this.CastInfos.Effect.diceNum, (short) this.CastInfos.Effect.diceSide, (short) this.CastInfos.Effect.value, (short) 0/*(this.CastInfos.Effect.delay)*/);
    }

}
