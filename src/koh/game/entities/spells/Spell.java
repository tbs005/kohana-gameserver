package koh.game.entities.spells;

/**
 *
 * @author Neo-Craft
 */
public class Spell {

    public int id;
    public int typeId;
    public int iconId;
    public boolean verbose_casttype; //UseParamCache ?
    public SpellLevel spellLevels[];

    //public String nameIdtype, descriptionIdtype;
    //public String scriptParamstype;
    //public String scriptParamsCriticaltype;
    //public int scriptIdtype, scriptIdCriticaltype;
}