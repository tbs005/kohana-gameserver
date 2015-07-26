package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffEffect;
import koh.game.fights.effects.buff.BuffSpellDommage;

/**
 *
 * @author Neo-Craft
 */
public class EffectSpellDommage extends EffectBase {

    @Override
    public int ApplyEffect(EffectCast CastInfos) {
        BuffEffect Buff = null;
        for (Fighter Target : CastInfos.Targets) {
            Buff = new BuffSpellDommage(CastInfos, Target);
            if (!Target.Buffs.BuffMaxStackReached(Buff)) {
                Target.Buffs.AddBuff(Buff);
                if (Buff.ApplyEffect(null, null) == -3) {
                    return -3;
                }
            }
        }

        return -1;
    }

}