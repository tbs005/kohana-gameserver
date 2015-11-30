package koh.game.entities.actors.npc.replies;

import koh.game.entities.actors.Player;
import koh.game.entities.actors.npc.NpcReply;

/**
 *
 * @author Neo-Craft
 */
public class TeleportReply extends NpcReply {
    
    @Override
    public boolean execute(Player p) {
        if (!super.execute(p)) {
            return false;
        }
        p.teleport(Integer.parseInt(this.parameters[0]), Integer.parseInt(this.parameters[1]));
        return true;
    }
    
}
