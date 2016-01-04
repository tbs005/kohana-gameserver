package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffPoutch;

/**
 *
 * @author Neo-Craft
 */
public class EffectPoutch extends EffectBase {

    @Override
    public int applyEffect(EffectCast CastInfos) {

        for (Fighter Target : CastInfos.targets) {
            Target.getBuff().addBuff(new BuffPoutch(CastInfos, Target));
        }

        return -1;
    }

}
