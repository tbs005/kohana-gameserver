package koh.game.entities.actors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import koh.d2o.Couple;
import koh.game.actions.*;
import koh.game.controllers.PlayerController;
import koh.game.dao.DAO;

import koh.game.entities.Account;
import koh.game.entities.ExpLevel;
import koh.game.entities.actors.character.CharacterInventory;
import koh.game.entities.actors.character.FieldNotification;
import koh.game.entities.environments.DofusMap;
import koh.game.network.WorldClient;
import koh.game.network.handlers.game.approach.CharacterHandler;
import koh.game.entities.actors.character.*;
import koh.game.entities.guilds.Guild;
import koh.game.entities.guilds.GuildMember;
import koh.game.entities.spells.LearnableSpell;
import koh.game.fights.Fight;
import koh.game.fights.FightState;
import koh.game.fights.Fighter;
import koh.game.fights.fighters.CharacterFighter;
import koh.game.network.ChatChannel;
import koh.protocol.client.enums.*;
import koh.protocol.messages.game.character.stats.*;
import koh.protocol.messages.game.context.roleplay.TeleportOnSameMapMessage;
import koh.game.utils.Observable;
import koh.game.utils.Observer;
import koh.protocol.client.Message;
import koh.protocol.messages.game.atlas.compass.CompassUpdatePartyMemberMessage;
import koh.protocol.messages.game.basic.TextInformationMessage;
import koh.protocol.messages.game.character.status.PlayerStatus;
import koh.protocol.messages.game.context.GameContextRefreshEntityLookMessage;
import koh.protocol.messages.game.initialization.CharacterLoadingCompleteMessage;
import koh.protocol.messages.game.context.roleplay.CurrentMapMessage;
import koh.protocol.messages.game.context.roleplay.GameRolePlayShowActorMessage;
import koh.protocol.messages.game.pvp.AlignmentRankUpdateMessage;
import koh.protocol.types.game.context.GameContextActorInformations;
import koh.protocol.types.game.character.ActorRestrictionsInformations;
import koh.protocol.types.game.character.alignment.ActorAlignmentInformations;
import koh.protocol.types.game.character.alignment.ActorExtendedAlignmentInformations;
import koh.protocol.types.game.choice.CharacterBaseInformations;
import koh.protocol.types.game.context.roleplay.*;
import koh.protocol.types.game.look.EntityLook;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neo-Craft
 */
@Builder
@ToString
public class Player extends IGameActor implements Observer {

    private static final Logger logger = LogManager.getLogger(Player.class);

    @Getter @Setter
    private int owner;
    @Getter @Setter
    private String nickName;
    @Getter @Setter
    private int sexe;
    @Getter @Setter
    private byte breed;
    @Getter @Setter
    private ArrayList<Short> skins;
    private ArrayList<Integer> indexedColors;
    @Getter @Setter
    private ArrayList<Short> scales;
    @Getter @Setter
    private Account account;
    @Getter @Setter
    private int achievementPoints,level;
    @Getter @Setter
    private WorldClient client;
    @Getter @Setter
    private long regenStartTime;
    @Getter @Setter
    private volatile DofusMap currentMap;
    private List<Byte> enabledChannels;
    @Getter
    private List<Byte> disabledChannels;
    @Getter @Setter
    private ShortcutBook shortcuts;
    @Getter @Setter
    private volatile MountInformations mountInfo;
    @Getter @Setter
    private int savedMap;
    @Getter @Setter
    private short savedCell;
    @Getter @Setter
    private volatile SpellBook mySpells;
    @Getter @Setter
    private volatile JobBook myJobs;
    @Getter @Setter
    private CharacterInventory inventoryCache;
    @Setter
    private PlayerStatusEnum status;
    @Setter
    private HashMap<ScoreType, Integer> scores;
    //GenericStats
    /*public int getAP;
     public int getMP;*/
    @Setter @Getter
    private int chance, life, vitality,wisdom, strength, intell, agility;
    @Getter @Setter
    private short activableTitle, activableOrnament;
    @Getter @Setter
    private byte regenRate;
    @Getter @Setter
    private byte[] emotes;
    @Getter @Setter
    private int[] ornaments, titles;
    @Getter @Setter
    private GenericStats stats;

    //stats
    @Getter @Setter
    private long experience;
    @Getter @Setter
    private int kamas, statPoints, spellPoints;
    @Getter @Setter
    private byte alignmentValue, alignmentGrade, PvPEnabled;
    @Getter @Setter
    private AlignmentSideEnum alignmentSide;
    @Getter @Setter
    private int honor, dishonor, energy;

    @Getter @Setter
    private CopyOnWriteArrayList<Player> followers;

    @Getter @Setter
    private boolean inWorld;
    protected boolean myInitialized = false;
    private HumanInformations cachedHumanInformations;

    //Other
    @Getter @Setter
    private byte moodSmiley;
    @Getter @Setter
    private Guild guild;
    private boolean wasSitted = false; //only for regen life

    private Fight myFight;
    private Fighter myFighter;

    public synchronized void initialize() {
        if (myInitialized) {
            return;
        }

        this.disabledChannels = new ArrayList<>(14);
        for (byte i = 0; i < 14; i++) {
            if (!this.enabledChannels.contains(i)) {
                this.disabledChannels.add(i);
            }
        }
        if (this.mySpells == null || this.mySpells.haventSpell()) {
            this.mySpells = SpellBook.generateForBreed(this.breed, this.level);
        }
        if (this.myJobs == null) {
            this.myJobs = new JobBook() {
                {
                    this.deserializeEffects(new byte[0]);
                }
            };
        }

        this.guild = DAO.getGuilds().getForPlayer(this.ID);

        this.stats = new GenericStats(this);

        this.inventoryCache = new CharacterInventory(this);

        this.inventoryCache.getItems().filter(x -> x.getPosition() != 63).forEach(Item -> {
            this.stats.merge(Item.getStats());
            //this.life += item.getStats().getTotal(StatsEnum.vitality);
        });

        this.inventoryCache.generalItemSetApply();

        this.myInitialized = true;
        if (life == 0) {
            life++;
        }
    }

    public void send(Message m) {
        if (client != null) {
            client.send(m);
        }
    }

    @Override
    public boolean canBeSeen(IGameActor Actor) {
        if (this.account == null) {
            logger.error("NPE can be seen {} {}",this.nickName,this.ID);
            return false;
        }
        return true;
    }

    @Override
    public GameContextActorInformations getGameContextActorInformations(Player character) {
        if (this.account == null) {
            logger.error("NPE GameContext {}" , this.nickName);
        }
        return new GameRolePlayCharacterInformations(this.ID, this.getEntityLook(), this.getEntityDispositionInformations(character), this.nickName, this.getHumanInformations(), this.account.id, this.getActorAlignmentInformations());
    }

    public HumanInformations getHumanInformations() {
        if (cachedHumanInformations == null) {
            HumanOption[] Options = new HumanOption[0];
            if (this.activableTitle != 0) {
                Options = ArrayUtils.add(Options, new HumanOptionTitle(this.activableTitle, ""));
            }
            if (this.activableOrnament != 0) {
                Options = ArrayUtils.add(Options, new HumanOptionOrnament(this.activableOrnament));
            }
            if (this.guild != null) {
                Options = ArrayUtils.add(Options, new HumanOptionGuild(this.guild.toGuildInformations()));
            }
            // Options = ArrayUtils.add(Options, new HumanOptionAlliance(this.PvPEnabled, new AllianceInformations(1191, "a", "ta race", this.guild.getGuildEmblem())));
            this.cachedHumanInformations = new HumanInformations(new ActorRestrictionsInformations(), this.hasSexe(), Options);
        }
        return this.cachedHumanInformations;
    }

    public synchronized void removeHumanOption(Class<? extends HumanOption> klass){
        Arrays.stream(this.getHumanInformations().options)
                .filter(opt -> opt.getClass().isAssignableFrom(klass))
                .findFirst()
                .ifPresent(ho -> this.getHumanInformations().options = ArrayUtils.removeElement(this.getHumanInformations().options,ho));

    }


    public void refreshEntitie() {
        if (getFighter() != null) {
            getFight().sendToField(new GameContextRefreshEntityLookMessage(this.ID, this.getEntityLook()));
        } else {
            this.currentMap.sendToField(new GameContextRefreshEntityLookMessage(this.ID, this.getEntityLook()));
        }
    }

    public List<Integer> getIndexedColors(){
        if(this.indexedColors == null)
            this.indexedColors = new ArrayList<>(5);
        return this.indexedColors;
    }

    public List<Byte> getEnabledChannels(){
        if(this.enabledChannels == null)
            this.enabledChannels = new ArrayList<>(20);
        return this.enabledChannels;
    }

    public void refreshActor() {
        this.cachedHumanInformations = null;
        if (this.client != null) {
            currentMap.sendToField(klient -> new GameRolePlayShowActorMessage((GameRolePlayActorInformations) client.getCharacter().getGameContextActorInformations(klient)));
        }
    }

    public HashMap<ScoreType, Integer> getScores(){
        if(this.scores == null)
            this.scores = new HashMap<>(7);
        return this.scores;
    }

    public PlayerStatusEnum getStatus(){
        if(this.status == null)
            this.status = PlayerStatusEnum.PLAYER_STATUS_AVAILABLE;
        return this.status;
    }

    public GuildMember getGuildMember() {
        return this.guild.getMember(ID);
    }

    public void offlineTeleport(int newMapID, int newCellID) {
        if (this.currentMap.getId() == newMapID) {
            this.cell = newCellID == -1 ? currentMap.getAnyCellWalakable() : currentMap.getCell((short) newCellID) != null ? currentMap.getCell((short) newCellID) : cell;
            return;
        }
        DofusMap nextMap = DAO.getMaps().findTemplate(newMapID);
        if (nextMap == null) {
            logger.error("Nulled map on {}", newMapID);
            return;
        }
        nextMap.initialize();
        this.currentMap.destroyActor(this);
        this.currentMap = nextMap;
        if (nextMap.getCell((short) newCellID) == null || newCellID < 0 || newCellID > 559) {
            this.cell = nextMap.getAnyCellWalakable();
        } else {
            this.cell = nextMap.getCell((short) newCellID);
        }
    }

    public synchronized void teleport(int newMapID, int newCellID) {
        if (this.currentMap.getId() == newMapID) {
            this.cell = newCellID == -1 ? currentMap.getAnyCellWalakable() : currentMap.getCell((short) newCellID) != null ? currentMap.getCell((short) newCellID) : cell;
            this.currentMap.sendToField(new TeleportOnSameMapMessage(ID, cell.getId()));
            return;
        }
        DofusMap nextMap = DAO.getMaps().findTemplate(newMapID);
        if (nextMap == null) {
            logger.error("Nulled map on {}", newMapID);
            PlayerController.sendServerMessage(client, "Signal on the bugTracker nulled map -> " + newMapID);
            //client.sendPacket(new ErrorMapNotFoundMessage());
            return;
        }
        client.endGameAction(koh.game.actions.GameActionTypeEnum.MAP_MOVEMENT);
        nextMap.initialize();
        stopSitEmote();
        this.currentMap.destroyActor(this);
        this.currentMap = nextMap;
        this.mapid = newMapID;
        if (nextMap.getCell((short) newCellID) == null || newCellID < 0 || newCellID > 559) {
            this.cell = nextMap.getAnyCellWalakable();
        } else {
            this.cell = nextMap.getCell((short) newCellID);
        }
        this.currentMap.spawnActor(this);
        client.send(new CurrentMapMessage(currentMap.getId(), "649ae451ca33ec53bbcbcc33becf15f4"));
        if (this.followers != null) {
            this.followers.parallelStream().forEach(e -> e.send(new CompassUpdatePartyMemberMessage(CompassTypeEnum.COMPASS_TYPE_PARTY, this.currentMap.coordinates(), this.ID)));
        }
    }

    public void addScore(ScoreType Type) {
        this.scores.put(Type, this.scores.get(Type) + 1);
    }

    public void addKamas(int val){
        this.kamas += val;
    }

    public synchronized void onLogged() {
        try {
            if (!this.inWorld) {
                this.inWorld = true;
                this.account.currentIP = client.getIP();
                if (this.getFighter() == null) {
                    this.spawnToMap();
                    client.send(this.currentMap.getAgressableActorsStatus(this));
                }
                client.send(new CurrentMapMessage(currentMap.getId(), "649ae451ca33ec53bbcbcc33becf15f4")); //kdpelrkdpaielcmspekdprcvkdparkdb
                client.send(new CharacterLoadingCompleteMessage());
                ChatChannel.register(client);
                PlayerController.sendServerMessage(client, DAO.getSettings().getStringElement("World.onLogged"), DAO.getSettings().getStringElement("World.onLoggedColor"));
                // client.send(new BasicNoOperationMessage());
                client.send(new AlignmentRankUpdateMessage(this.alignmentGrade, false));
                client.sequenceMessage();
                if (this.guild != null) {
                    this.guild.registerPlayer(this);
                }

                //GuildWarn
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void onDisconnect() {
        try {
            if (!this.inWorld) {
                return;
            }
            if (this.guild != null) {
                this.guild.unregisterPlayer(this);
            }
            if (this.followers != null) {
                this.followers.clear();
                this.followers = null;
            }
            if (client != null && currentMap != null) {
                currentMap.destroyActor(this);
            }
            this.client = null;
            if (this.account != null) {
                if (this.account.characters == null) {
                    logger.error("NulledAccountCharacters {} ", this.nickName); //pas sence arriver
                    logger.error(this.toString());
                }
                for (Player p : this.account.characters) { //TODO: ALleos
                    if(DAO.getPlayers().getQueueAsSteam().anyMatch(x -> x != null && p.nickName.equalsIgnoreCase(x.second.nickName))){
                        logger.debug(p.nickName + " already aded");
                    }
                    DAO.getPlayers().addCharacterInQueue(new Couple<>(System.currentTimeMillis() + DAO.getSettings().getIntElement("Account.DeleteMemoryTime") * 60 * 1000, p));
                    logger.debug(p.nickName + " added" + this.account.characters.size());
                }
            } else {
                logger.error(nickName + " nulled account on disconnection");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.inWorld = false;
        }
    }

    public void refreshStats() {
        refreshStats(true,true);
    }

    public void refreshStats(boolean logged, boolean stopRegen) {
        if(this.life > this.getMaxLife()){
            this.setLife(this.getMaxLife());
        }
        this.updateRegenedLife(stopRegen);
        if (client != null) {
            if (client.getParty() != null) {
                client.getParty().updateMember(this);
            }
            if (logged && getFighter() != null && getFight().getFightState() == FightState.STATE_PLACE) {
                this.myFighter.getStats().reset();
                this.myFighter.getStats().merge(this.stats);
                client.send(((CharacterFighter) getFighter()).FighterStatsListMessagePacket());
            } else {
                CharacterHandler.sendCharacterStatsListMessage(this.client);
            }
        }
    }

    private static final int[] TAVERNE_MAP = new int[] { 146233,148796,146237,148786,144698,145208,145714};

    public void updateRegenedEnergy(){
        if(this.energy < PlayerEnum.MAX_ENERGY && this.regenStartTime != 0){
            long timeElapsed = TimeUnit.MILLISECONDS.toHours(Instant.now().minusMillis(this.regenStartTime).toEpochMilli());
            if(timeElapsed > 0){
                timeElapsed *= ArrayUtils.contains(TAVERNE_MAP, this.mapid) ? 100 : 50;
                if (this.energy + timeElapsed > PlayerEnum.MAX_ENERGY) {
                    timeElapsed = PlayerEnum.MAX_ENERGY - this.energy;
                }
                this.energy += timeElapsed;
                this.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE,7, String.valueOf(timeElapsed)));
            }

        }
    }

    public void updateRegenedLife(boolean stopRegen) {
        if(this.getLife() == this.getMaxLife()){
            if(this.regenStartTime != 0){
                this.stopRegen(stopRegen);
                this.regenStartTime = 0;
            }
        }
        else if(this.myFight != null){
            return;
        }
        else if(this.regenStartTime == 0){
            this.regenStartTime = Instant.now().toEpochMilli();
            this.send(new LifePointsRegenBeginMessage(this.regenRate));
        }else{
            this.stopRegen(stopRegen);
            if(this.getLife() >= this.getMaxLife()){
                this.regenStartTime = 0;
                return;
            }
            this.regenStartTime = Instant.now().toEpochMilli();
            this.send(new LifePointsRegenBeginMessage(this.regenRate));
        }
    }

    public void stopRegen(){
        stopRegen(true);
    }

    public void stopRegen(boolean stopRegen) {

        if(this.regenStartTime != 0) {
            int timeElapsed = (int) Instant.now().minusMillis(this.regenStartTime).getEpochSecond();
            if(this.regenRate == 5){
                timeElapsed *= 2;
            }
            if (this.life + timeElapsed > this.getMaxLife()) {
                timeElapsed = this.getMaxLife() - this.life;
            }
            logger.debug("Player {} regens {} lifepoints", this.nickName, timeElapsed);
            this.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE,1, String.valueOf(timeElapsed)));

            this.addLife(timeElapsed);
            if(stopRegen)
                this.send(new LifePointsRegenEndMessage(this.life, this.getMaxLife(), timeElapsed));
        }
        if(stopRegen)
            this.regenRate = 10;
    }

    public synchronized void stopSitEmote(){
        if(client.getCharacter().getRegenRate() == 5){
            client.getCharacter().removeHumanOption(HumanOptionEmote.class);
            client.getCharacter().stopRegen();
            client.getCharacter().updateRegenedLife(true);
            client.send(new UpdateLifePointsMessage(client.getCharacter().getLife(),client.getCharacter().getMaxLife()));
        }
    }

    public void addLife(int val){
        this.life += val;
    }

    public void addStatPoints(int val){
        this.statPoints += val;
    }

    public int getInitiative(boolean Base) {

        return 1 + (int) Math.floor((this.stats.getTotal(StatsEnum.STRENGTH)
                + this.stats.getTotal(StatsEnum.CHANCE)
                + this.stats.getTotal(StatsEnum.INTELLIGENCE)
                + this.stats.getTotal(StatsEnum.AGILITY)
                + (Base ? this.stats.getTotal(StatsEnum.INITIATIVE) : this.stats.getTotal(StatsEnum.INITIATIVE)))
                * ((double) life / getMaxLife())
        );

    }

    /*public int getInitiative() {
     int FORCE = 142, EAU = 0, FEU = 808, AGI = 30;
     int BONUSINI = 365;
     int TOTALCARAC = FORCE + EAU + FEU + AGI;
     double PDVACTUEL = 4260, PDVMAX = 4260;
     double INITIATIVE_TOTAL = (TOTALCARAC + BONUSINI) * (PDVACTUEL / PDVMAX);

     int fact = 4;
     int pvmax = this.getMaxLife() - D2oDaoImpl.getBreed(this.breed).getHealPoint();
     int pv = life - D2oDaoImpl.getBreed(this.breed).getHealPoint();
     if (pv < 0) {
     pv = 1;
     }
     if (this.breed == BreedEnum.Sacrieur) {
     fact = 8;
     }
     double coef = pvmax / fact;

     coef += this.stats.getTotal(StatsEnum.getInitiative);
     coef += this.stats.getTotal(StatsEnum.agility);
     coef += this.stats.getTotal(StatsEnum.chance);
     coef += this.stats.getTotal(StatsEnum.intelligence);
     coef += this.stats.getTotal(StatsEnum.strength);

     int init = 1;
     if (pvmax != 0) {
     init = (int) (coef * ((double) pv / (double) pvmax));
     }
     if (init < 0) {
     init = 0;
     }
     return init;
     }*/
    public int getProspection() {
        return (int) Math.floor((double) (this.stats.getTotal(StatsEnum.CHANCE) / 10)) + this.stats.getTotal(StatsEnum.PROSPECTING);

    }

    public void spawnToMap() {
        if (this.currentMap != null) {
            this.currentMap.spawnActor(this);
        }
    }

    public int getMaxLife() {
        return this.stats.getTotal(StatsEnum.VITALITY) + ((int) level * 5) + DAO.getD2oTemplates().getBreed(this.breed).getHealPoint();
    }

    @Override
    public EntityLook getEntityLook() {
        if (entityLook == null) {
            this.entityLook = new EntityLook((short) 1, new ArrayList<Short>() {
                {
                    this.addAll(skins);
                }
            }, new ArrayList<Integer>() {
                {
                    this.addAll(indexedColors);
                }
            }, new ArrayList<Short>() {
                {
                    this.addAll(scales);
                }
            }, new ArrayList<>());
        }
        return entityLook;
    }
    /*byte alignmentSide, byte alignmentValue, byte alignmentGrade, int characterPower, int honor, int honorGradeFloor, int honorNextGradeFloor, byte aggressable*/

    public void changeAlignementSide(AlignmentSideEnum side) {
        if (this.myFight != null) {
            return;
        }
        this.alignmentSide = side;
        this.alignmentValue = 1;
        this.PvPEnabled = AggressableStatusEnum.PvP_ENABLED_AGGRESSABLE;
        this.onAligmenentSideChanged();
        this.setHonor(0, true);
    }

    public void setEnabldPvp(byte stat) {
        this.PvPEnabled = stat;
        this.onAligmenentSideChanged();
    }

    public void addHonor(int point, boolean notice) {
        setHonor(this.honor + point, notice);
        if (notice) {
            this.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, point > 0 ? 80 : 81, new String[]{Integer.toString(point)}));
        }
    }

    public void addDishonor(int point, boolean notice) {
        this.dishonor += point;
        if (notice) {
            this.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 75, new String[]{Integer.toString(point)}));
        }
    }

    public void setHonor(int point, boolean notice) {
        this.honor = point < 0 ? 0 : point;

        byte oldGrade = this.alignmentGrade;
        if (honor >= 17500) {
            this.alignmentGrade = 10;
        } else {
            for (byte n = 1; n <= 10; n++) {
                if (honor < DAO.getExps().getLevel(n).getPvP()) {
                    this.alignmentGrade = (byte) (n - 1);

                    break;
                }
            }
        }
        if (notice && this.alignmentGrade != oldGrade) {
            this.onAligmenentSideChanged();
            this.send(new AlignmentRankUpdateMessage(this.alignmentGrade, true));
        }
    }

    private void onAligmenentSideChanged() {
        this.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 82));
        this.currentMap.sendToField(new GameRolePlayShowActorMessage((GameRolePlayActorInformations) getGameContextActorInformations(null)));
        this.currentMap.sendToField(Player -> this.currentMap.getAgressableActorsStatus(Player));
        this.refreshStats();
    }

    public byte getPlayerState() {
        if (client == null) {
            return PlayerStateEnum.NOT_CONNECTED;
        } else {
            return PlayerStateEnum.UNKNOWN_STATE;
        }
    }

    public BasicGuildInformations getBasicGuildInformations() {
        return new BasicGuildInformations(0, "");
    }

    public int getCharacterPower() {
        return this.ID + this.level;
    }

    public ActorAlignmentInformations getActorAlignmentInformations() {
        return new ActorAlignmentInformations(this.PvPEnabled == AggressableStatusEnum.NON_AGGRESSABLE ? 0 : this.alignmentSide.value, this.alignmentValue, this.PvPEnabled == AggressableStatusEnum.NON_AGGRESSABLE ? 0 : this.alignmentGrade, this.getCharacterPower());
    }

    public ActorExtendedAlignmentInformations getActorAlignmentExtendInformations() {
        if(DAO.getExps().getLevel(this.alignmentGrade) == null)
            logger.error("lga1");
        else if(alignmentSide == null)
            logger.error("lga2");
        return new ActorExtendedAlignmentInformations(this.alignmentSide.value,
                this.alignmentValue,
                this.PvPEnabled == AggressableStatusEnum.NON_AGGRESSABLE ? 0 : this.alignmentGrade,
                this.getCharacterPower(),
                this.honor,
                DAO.getExps().getLevel(this.alignmentGrade).getPvP(),
                DAO.getExps().getLevel(this.alignmentGrade == 10 ? 10 : this.alignmentGrade + 1).getPvP(),
                this.PvPEnabled);
    }

    public CharacterBaseInformations toBaseInformations() {
        return new CharacterBaseInformations(ID, (byte) level, nickName, getEntityLook(), breed, sexe == 1);
    }

    public void addFollower(Player gay) {
        if (this.followers == null) {
            this.followers = new CopyOnWriteArrayList<>();
        }
        this.followers.addIfAbsent(gay);
        gay.send(new CompassUpdatePartyMemberMessage(CompassTypeEnum.COMPASS_TYPE_PARTY, this.currentMap.coordinates(), this.ID));
    }

    public void addExperience(long Value) {
        addExperience(Value, true);
    }

    public void addExperience(long value, boolean notice) {
        if (!this.myInitialized) {
            this.initialize();
        }

        this.experience += value;

        if (this.level != DAO.getExps().getMaxLevel()) {

            ExpLevel floor;

            Integer lastLevel = this.level;
            do {
                floor = DAO.getExps().getLevel(this.level + 1);
                if (floor.getPlayer() < this.experience) {
                    this.level++;
                    this.statPoints += 5;
                    this.spellPoints++;

                    if (this.level == 100) {
                        this.stats.addBase(StatsEnum.ACTION_POINTS, 1);
                    }
                    // Apprend des nouveaux sorts
                    for (LearnableSpell learnableSpell : DAO.getSpells().findLearnableSpell(this.breed)) {
                        if ((int) learnableSpell.getObtainLevel() > (int) level && this.mySpells.hasSpell(learnableSpell.getSpell())) {
                            this.mySpells.removeSpell(this, learnableSpell.getSpell());
                        } else if ((int) learnableSpell.getObtainLevel() <= (int) level && !this.mySpells.hasSpell(learnableSpell.getSpell())) {
                            this.mySpells.addSpell(learnableSpell.getSpell(), (byte) 1, this.mySpells.getFreeSlot(), this.client);
                        }
                    }

                }
            } while (floor.getPlayer() < this.experience && this.level != 200);

            if (this.level != lastLevel) {
                this.life = this.getMaxLife();
                this.send(new CharacterLevelUpMessage((byte) this.level));
                //friends
                this.currentMap.sendToField(new CharacterLevelUpInformationMessage((byte) this.level, this.nickName, this.ID));

            }

            if (this.client != null && notice) {
                this.refreshStats();
            }
        }
    }


    public void destroyFromMap() {
        if (this.currentMap != null) {
            this.currentMap.destroyActor(this);
        }
    }

    @Override
    public void Observer$update(Observable o) {
    }

    @Override
    public void Observer$update(Observable o, Object arg) {
        if (arg instanceof Message) {
            if (o instanceof DofusMap && (getFight() != null || getFighter() != null)) {
                return;
            }
            if (client != null && inWorld) {
                client.send((Message) arg);
            }
        } else if (arg instanceof FieldNotification) {
            FieldNotification task = (FieldNotification) arg;
            if (task.can(this)) {
                if (client != null && inWorld) {
                    client.send((Message) task.packet);
                }
            }
        } else if (arg instanceof FieldOperation) {
            FieldOperation op = (FieldOperation) arg;
            op.execute(this);
        }
    }

    public boolean hasSexe() {
        return this.sexe == 1;
    }


    @Override
    public void Observer$update(Observable o, Object... args) {
    }

    @Override
    public void Observer$reset(Observable o) {
    }

    public void save(boolean Clear) {
        if (this.myInitialized) {
            if (this.inventoryCache != null) {
                this.inventoryCache.save(Clear);
            }
            DAO.getPlayers().update(this, Clear);
            if (!Clear && this.account != null && this.account.accountData != null) {
                this.account.accountData.save(false);
            }
        }
    }

    public void initScore(String result){
        this.getScores().put(ScoreType.PVP_WIN, Integer.parseInt(result.split(",")[0]));
        this.scores.put(ScoreType.PVP_LOOSE, Integer.parseInt(result.split(",")[1]));
        this.scores.put(ScoreType.ARENA_WIN, Integer.parseInt(result.split(",")[2]));
        this.scores.put(ScoreType.ARENA_LOOSE, Integer.parseInt(result.split(",")[3]));
        this.scores.put(ScoreType.PVM_WIN, Integer.parseInt(result.split(",")[4]));
        this.scores.put(ScoreType.PVM_LOOSE, Integer.parseInt(result.split(",")[5]));
        this.scores.put(ScoreType.PVP_TOURNAMENT, Integer.parseInt(result.split(",")[6]));

    }

    public void initScore(){
        this.getScores().put(ScoreType.PVP_WIN, 0);
        this.scores.put(ScoreType.PVP_LOOSE, 0);
        this.scores.put(ScoreType.ARENA_WIN, 0);
        this.scores.put(ScoreType.ARENA_LOOSE, 0);
        this.scores.put(ScoreType.PVM_WIN, 0);
        this.scores.put(ScoreType.PVM_LOOSE, 0);
        this.scores.put(ScoreType.PVP_TOURNAMENT, 0);
    }

    public int computeLife(float percent){
        return (int) (this.getMaxLife() * percent / 100.00f);
    }

    public double getExpBonus() {
        double bonusXp = 1;
        for(Player cbi2 : this.account.characters)
        {
            if (((((!((cbi2.getID() == this.ID))) && ((cbi2.level > level)))) && ((bonusXp < 4))))
            {
                bonusXp = (bonusXp + 1);
            };
        };
        return bonusXp * DAO.getSettings().getDoubleElement("Rate.PvM");
    }

    public Object $FighterLook = new Object();

    public void setFight(Fight Fight) {
        synchronized ($FighterLook) {
            this.myFight = Fight;
        }
    }

    public void setFighter(Fighter Fighter) {
        synchronized ($FighterLook) {
            this.myFighter = Fighter;
        }
    }

    public Fight getFight() {
        return this.myFight;
    }

    public Fighter getFighter() {
        return this.myFighter;
    }

    public PlayerStatus getPlayerStatus() {
        return new PlayerStatus(this.getStatus().value());
    }

    public String serializeStats(){
        StringBuilder sb = new StringBuilder();
        sb.append(this.vitality).append(',');
        sb.append(this.wisdom).append(',');
        sb.append(this.strength).append(',');
        sb.append(this.intell).append(',');
        sb.append(this.agility).append(',');
        sb.append(this.chance).append(',');
        sb.append(this.life).append(',');
        sb.append(this.experience).append(',');
        sb.append(this.activableTitle).append(',');
        sb.append(this.activableOrnament).append(',');
        sb.append(this.regenStartTime);
        return sb.toString();
    }

    public void totalClear() {
        owner = 0;
        nickName = null;
        breed = 0;
        sexe = 0;
        skins.clear();
        skins = null;
        indexedColors.clear();
        indexedColors = null;
        scales.clear();
        scales = null;
        level = 0;
        client = null;
        regenStartTime = 0;
        currentMap = null;
        $FighterLook = null;
        if (enabledChannels != null) {
            enabledChannels.clear();
        }
        enabledChannels = null;
        if (disabledChannels != null) {
            disabledChannels.clear();
        }
        disabledChannels = null;
        if (shortcuts != null) {
            shortcuts.totalClear();
        }
        shortcuts = null;
        mapid = 0;
        if (mySpells != null) {
            mySpells.totalClear();
        }
        if (myJobs != null) {
            myJobs.totalClear();
        }
        if (this.mountInfo != null) {
            this.mountInfo.totalClear();
            this.mountInfo = null;
        }
        myJobs = null;
        mySpells = null;
        inventoryCache = null;
        status = null;
        vitality = 0;
        wisdom = 0;
        strength = 0;
        intell = 0;
        agility = 0;
        chance = 0;
        life = 0;
        if (stats != null) {
            stats.totalClear();
        }
        stats = null;
        experience = 0;
        kamas = 0;
        statPoints = 0;
        spellPoints = 0;
        alignmentSide = null;
        alignmentValue = 0;
        alignmentGrade = 0;
        PvPEnabled = 0;
        honor = 0;
        dishonor = 0;
        energy = 0;
        if (account != null) {
            account.totalClear();
        }
        account = null;
        inWorld = false;
        myInitialized = false;

        myFight = null;
        myFighter = null;
        try {
            this.finalize();
        } catch (Throwable tr) {
        }
    }


}
