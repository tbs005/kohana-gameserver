package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffDodge;

/**
 *
 * @author Neo-Craft
 */
public class EffectDodge extends EffectBase {

    @Override
    public int applyEffect(EffectCast CastInfos) {
        for (Fighter Target : CastInfos.targets) {
            Target.getBuff().addBuff(new BuffDodge(CastInfos, Target));
        }

        return -1;
    }

}
