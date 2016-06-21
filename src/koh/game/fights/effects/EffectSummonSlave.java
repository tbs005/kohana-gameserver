package koh.game.fights.effects;

import koh.game.dao.DAO;
import koh.game.entities.mob.MonsterGrade;
import koh.game.entities.mob.MonsterTemplate;
import koh.game.fights.Fighter;
import koh.game.fights.fighters.CharacterFighter;
import koh.game.fights.fighters.DoubleFighter;
import koh.game.fights.fighters.SlaveFighter;
import koh.game.fights.fighters.SummonedFighter;
import koh.protocol.client.enums.ActionIdEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.messages.game.actions.fight.GameActionFightSummonMessage;
import koh.protocol.types.game.context.fight.GameFightFighterInformations;

/**
 * Created by Melancholia on 6/20/16.
 */
public class EffectSummonSlave extends EffectBase {
    @Override
    public int applyEffect(EffectCast castInfos) {
        if(castInfos.caster.getStats().getTotal(StatsEnum.ADD_SUMMON_LIMIT) <= 0){
            return -1;
        }

        if (castInfos.caster.getFight().isCellWalkable(castInfos.cellId)) {
            final MonsterTemplate monster = DAO.getMonsters().find(castInfos.effect.diceNum);
            final MonsterGrade monsterLevel = monster.getLevelOrNear(castInfos.effect.diceSide);
            final Fighter summon = new SlaveFighter(castInfos.caster.getFight(), monsterLevel,castInfos.caster);
            summon.joinFight();
            summon.getFight().joinFightTeam(summon, castInfos.caster.getTeam(), false, castInfos.cellId, true);
            castInfos.caster.getFight().sendToField(Pl -> new GameActionFightSummonMessage(ActionIdEnum.ACTION_SUMMON_SLAVE, castInfos.caster.getID(), (GameFightFighterInformations) summon.getGameContextActorInformations(Pl)));
            castInfos.caster.getFight().getFightWorker().summonFighter(summon);

            castInfos.caster.getStats().getEffect(StatsEnum.ADD_SUMMON_LIMIT).base--;
            if (castInfos.caster instanceof CharacterFighter)
                castInfos.caster.send(castInfos.caster.asPlayer().getCharacterStatsListMessagePacket());
        }


        return -1;
    }
}
