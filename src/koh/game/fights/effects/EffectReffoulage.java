package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffReffoulage;

/**
 *
 * @author Neo-Craft
 */
public class EffectReffoulage extends EffectBase {

    @Override
    public int applyEffect(EffectCast CastInfos) {
       for (Fighter Target : CastInfos.targets) {
            Target.getBuff().addBuff(new BuffReffoulage(CastInfos, Target));
        }

        return -1;
    }

    
}
