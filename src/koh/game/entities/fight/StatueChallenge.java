package koh.game.entities.fight;

import koh.game.entities.environments.MovementPath;
import koh.game.entities.item.Weapon;
import koh.game.entities.spells.SpellLevel;
import koh.game.fights.Fight;
import koh.game.fights.FightTeam;
import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;

/**
 * Created by Melancholia on 8/28/16.
 * 2,Statue,descriptionIdtype=Finir son tour sur la même case que celle où vous l'avez commencé, pendant toute la durée du combat.
 */
public class StatueChallenge extends Challenge {

    public StatueChallenge(Fight fight, FightTeam team) {
        super(fight, team);
    }

    private short cell;

    @Override
    public void onFightStart() {

    }

    @Override
    public void onTurnStart(Fighter fighter) {
        if(fighter.getTeam() != team || !fighter.isPlayer())
            return;

        this.cell = fighter.getCellId();
    }

    @Override
    public void onTurnEnd(Fighter fighter) {
        if(fighter.getTeam() != team || !fighter.isPlayer())
            return;
        if(cell != fighter.getCellId()){
            this.failChallenge();
        }
    }

    @Override
    public void onFighterKilled(Fighter target, Fighter killer) {

    }

    @Override
    public void onFighterMove(Fighter fighter, MovementPath path) {

    }

    @Override
    public void onFighterSetCell(Fighter fighter, short startCell, short endCell) {

    }

    @Override
    public void onFighterCastSpell(Fighter fighter, SpellLevel spell) {

    }

    @Override
    public void onFighterCastWeapon(Fighter fighter, Weapon weapon) {

    }

    @Override
    public void onFighterTackled(Fighter fighter) {

    }

    @Override
    public void onFighterLooseLife(Fighter fighter, EffectCast cast, int damage) {

    }

    @Override
    public void onFighterHealed(Fighter fighter, EffectCast cast, int heal) {

    }
}
