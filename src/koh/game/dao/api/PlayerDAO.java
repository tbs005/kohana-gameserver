package koh.game.dao.api;

import koh.d2o.Couple;
import koh.game.entities.Account;
import koh.game.entities.actors.Player;
import koh.patterns.services.api.Service;

import java.util.stream.Stream;

/**
 * Created by Melancholia on 11/29/15.
 */
public abstract class PlayerDAO implements Service {

    public abstract void addCharacter(Player character);

    public abstract Player getCharacterByAccount(int id);

    public abstract void delCharacter(Player character);

    public abstract Player getCharacter(Integer characterId);

    public abstract Player getCharacter(String characterName);

    public abstract boolean update(Player character, boolean clear);

    public abstract boolean containsName(String name);

    public abstract Stream<Couple<Long, Player>> getQueueAsSteam();

    public abstract void addCharacterInQueue(Couple<Long, Player> charr);

    public abstract boolean isCurrentlyOnProcess(int accountId);

    public abstract void getByAccount(Account account) throws Exception;

    public abstract boolean add(Player character);
}