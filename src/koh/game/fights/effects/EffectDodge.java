package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffDodge;

/**
 *
 * @author Neo-Craft
 */
public class EffectDodge extends EffectBase {

    @Override
    public int ApplyEffect(EffectCast CastInfos) {
        for (Fighter Target : CastInfos.Targets) {
            Target.Buffs.AddBuff(new BuffDodge(CastInfos, Target));
        }

        return -1;
    }

}
