package koh.game.fights.fighters;

import java.util.Arrays;
import koh.game.entities.actors.Player;
import koh.game.entities.actors.character.GenericStats;
import koh.game.fights.Fighter;
import koh.look.EntityLookParser;
import koh.protocol.client.Message;
import koh.protocol.messages.game.actions.fight.GameActionFightVanishMessage;
import koh.protocol.types.game.context.GameContextActorInformations;
import koh.protocol.types.game.context.fight.FightTeamMemberCharacterInformations;
import koh.protocol.types.game.context.fight.FightTeamMemberInformations;
import koh.protocol.types.game.context.fight.GameFightCharacterInformations;
import koh.protocol.types.game.look.EntityLook;

/**
 *
 * @author Neo-Craft
 */
public class IllusionFighter extends StaticFighter {

    public IllusionFighter(koh.game.fights.Fight Fight, Fighter Summoner) {
        super(Fight, Summoner);
        this.Stats = new GenericStats();
        this.Stats.merge(Summoner.Stats);
        super.InitFighter(this.Stats, Fight.GetNextContextualId());
        this.entityLook = EntityLookParser.Copy(Summoner.getEntityLook());
        super.setLife(Summoner.Life());
        super.setLifeMax(Summoner.MaxLife());
    }

    @Override
    public int TryDie(int CasterId) {
        if (Arrays.stream(new Exception().getStackTrace()).anyMatch(Method -> Method.getMethodName().equalsIgnoreCase("JoinFightTeam"))) { //Il viens de rejoindre la team rofl on le tue pas
            return -1;
        }
        this.setLife(0);

        this.Fight.sendToField(new GameActionFightVanishMessage(1029, this.Summoner(), ID));

        if (this.Fight.m_activableObjects.containsKey(this)) {
            this.Fight.m_activableObjects.get(this).stream().forEach(y -> y.Remove());
        }

        myCell.RemoveObject(this);

        if (!this.Team.GetAliveFighters().anyMatch(x -> x instanceof IllusionFighter && x.Summoner() == this.Summoner())) {
            ((CharacterFighter) this.Summoner).onCloneCleared();
        }

        if (this.Fight.TryEndFight()) {
            return -3;
        }
        if (this.Fight.CurrentFighter == this) {
            this.Fight.FightLoopState = Fight.FightLoopState.STATE_END_TURN;
        }
        return -2;
    }

    @Override
    public int Level() {
        return Summoner.Level();
    }

    @Override
    public short MapCell() {
        return 0;
    }

    @Override
    public GameContextActorInformations getGameContextActorInformations(Player character) {
        return new GameFightCharacterInformations(this.ID, this.getEntityLook(), this.getEntityDispositionInformations(character), this.Team.Id, this.wave, this.IsAlive(), this.GetGameFightMinimalStats(character), this.previousPositions, ((CharacterFighter) this.Summoner).Character.nickName, ((CharacterFighter) this.Summoner).Character.getPlayerStatus(), (byte) this.Level(), ((CharacterFighter) this.Summoner).Character.getActorAlignmentInformations(), ((CharacterFighter) this.Summoner).Character.breed, ((CharacterFighter) this.Summoner).Character.hasSexe());
    }

    @Override
    public FightTeamMemberInformations GetFightTeamMemberInformations() {
        return new FightTeamMemberCharacterInformations(this.ID, ((CharacterFighter) this.Summoner).Character.nickName, (byte) this.Level());
    }

    @Override
    public void send(Message Packet) {

    }

    @Override
    public void JoinFight() {

    }

    @Override
    public EntityLook getEntityLook() {
        return this.entityLook;
    }

}
