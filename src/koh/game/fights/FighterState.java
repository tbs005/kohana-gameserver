package koh.game.fights;

import java.util.HashMap;
import java.util.function.Consumer;
import koh.game.entities.actors.Player;
import koh.game.fights.effects.buff.BuffEffect;
import static koh.protocol.client.enums.ActionIdEnum.ACTION_CHARACTER_MAKE_INVISIBLE;
import koh.protocol.client.enums.FightStateEnum;
import koh.protocol.client.enums.GameActionFightInvisibilityStateEnum;
import koh.protocol.messages.game.actions.fight.GameActionFightInvisibilityMessage;
import koh.protocol.messages.game.context.fight.character.GameFightRefreshFighterMessage;

/**
 *
 * @author Neo-Craft
 */
public class FighterState {

    private HashMap<FightStateEnum, BuffEffect> myStates = new HashMap<>();

    private Fighter myFighter;

    public FighterState(Fighter Fighter) {
        this.myFighter = Fighter;
    }

    public boolean CanState(FightStateEnum State) {
        switch (State) {
            case Porté:
            case Porteur:
                return !HasState(FightStateEnum.Lourd);
        }
        return !HasState(State);
    }

    public boolean HasState(FightStateEnum State) {
        return this.myStates.containsKey(State);
    }

    public BuffEffect GetBuffByState(FightStateEnum fse) {
        return myStates.get(fse);
    }

    public void AddState(BuffEffect Buff) {
        switch (Buff.CastInfos.EffectType) {
            case Invisibility:
                myFighter.VisibleState = GameActionFightInvisibilityStateEnum.INVISIBLE;
                for (Player o : this.myFighter.Fight.Observable$stream()) {
                    o.Send(new GameActionFightInvisibilityMessage(ACTION_CHARACTER_MAKE_INVISIBLE, Buff.Caster.ID, myFighter.ID, myFighter.GetVisibleStateFor(o)));
                }
                this.myStates.put(FightStateEnum.Invisible, Buff);
                return;

            case REFLECT_SPELL:
                this.myStates.put(FightStateEnum.STATE_REFLECT_SPELL, Buff);
                return;

            default:
                // Buff.Target.Fight.SendToFight(new GameActionMessage((int)EffectEnum.AddState, this.myFighter.ActorId, this.myFighter.ActorId + "," + Buff.CastInfos.Value3 + ",1"));
                break;
        }

        this.myStates.put(FightStateEnum.valueOf(Buff.CastInfos.Effect.value), Buff);
    }

    public void DelState(BuffEffect Buff) {
        switch (Buff.CastInfos.EffectType) {
            case Invisibility:
                this.myFighter.VisibleState = GameActionFightInvisibilityStateEnum.VISIBLE;
                this.myFighter.Fight.sendToField(new GameActionFightInvisibilityMessage(ACTION_CHARACTER_MAKE_INVISIBLE, Buff.Caster.ID, myFighter.ID, myFighter.GetVisibleStateFor(null)));
                this.myFighter.Fight.sendToField(new GameFightRefreshFighterMessage(myFighter.GetGameContextActorInformations(null)));
                this.myStates.remove(FightStateEnum.Invisible);
                return;
            case REFLECT_SPELL:
                this.myStates.remove(FightStateEnum.STATE_REFLECT_SPELL);
                return;

            default:
                // Buff.Target.Fight.SendToFight(new GameActionMessage((int) EffectEnum.AddState, this.myFighter.ActorId, this.myFighter.ActorId + "," + Buff.CastInfos.Value3 + ",0"));
                break;
        }

        this.myStates.remove(FightStateEnum.valueOf(Buff.CastInfos.Effect.value));
    }

    public void RemoveState(FightStateEnum State) {
        if (this.HasState(State)) {
            this.myStates.get(State).RemoveEffect();
        }
    }
    
    public BuffEffect FindState(FightStateEnum State) {
        if (this.HasState(State)) {
            return this.myStates.get(State);
        }
        return null;
    }

    public void Debuff() {
        for (BuffEffect State : this.myStates.values()) {
            State.RemoveEffect();
        }

        this.myStates.clear();
    }
    
    public void FakeState(FightStateEnum State , boolean Add){
        if(Add)
            this.myStates.put(State, null);
        else
            this.myStates.remove(State);
    }

}
