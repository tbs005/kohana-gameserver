package koh.game.fights;

import koh.game.dao.DAO;
import koh.game.entities.actors.TaxCollector;
import koh.game.entities.actors.character.PlayerInst;
import koh.game.entities.actors.character.ScoreType;
import koh.game.entities.guilds.GuildMember;
import koh.game.entities.item.EffectHelper;
import koh.game.entities.mob.MonsterDrop;
import koh.game.entities.mob.MonsterGrade;
import koh.game.fights.fighters.CharacterFighter;
import koh.game.fights.fighters.MonsterFighter;
import koh.protocol.client.enums.AlignmentSideEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.messages.game.context.mount.MountSetMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author Neo-Craft
 */
public class FightFormulas {

    private static final Logger logger = LogManager.getLogger(FightFormulas.class);

    private static final double[] GROUP_COEFFICIENTS = new double[]
            {
                    1.0,
                    1.1,
                    1.5,
                    2.3,
                    3.1,
                    3.6,
                    4.2,
                    4.7
            };

    public static int computeXpWin(CharacterFighter fighter, MonsterFighter[] droppersResults, double butin) {
        int result;
        if (droppersResults.length == 0) {
            result = 0;
        } else {
            final int num = fighter.getTeam().getFighters().mapToInt(entry -> entry.getLevel()).sum();
            final int maxPlayerLevel = fighter.getTeam().getFighters().mapToInt(entry -> entry.getLevel()).max().orElse(0);
            final int num2 = Arrays.stream(droppersResults).mapToInt(dr -> dr.getLevel()).sum();
            final int b = Arrays.stream(droppersResults).mapToInt(dr -> dr.getLevel()).max().orElse(0);
            final int num3 = Arrays.stream(droppersResults).mapToInt(dr -> dr.getGrade().getGradeXp()).sum();
            double num4 = 1.0;
            if (num - 5 > num2) {
                num4 = (double) num2 / (double) num;
            } else {
                if (num + 10 < num2) {
                    num4 = (double) (num + 10) / (double) num2;
                }
            }
            final double num5 = Math.min((double) fighter.getLevel(), truncate(2.5 * (double) b)) / (double) num * 100.0;
            int num6 = Arrays.stream(droppersResults).
                    filter(mob -> mob.getLevel() >= maxPlayerLevel / 3)
                    .mapToInt(x -> 1)
                    .sum();
            if (num6 <= 0) {
                num6 = 1;
            }
            double num7 = truncate(num5 / 100.0 * truncate((double) num3 * GROUP_COEFFICIENTS[num6 - 1] * num4));
            double num8 = (fighter.getFight().ageBonus <= 0) ? 1.0 : (1.0 + (double) fighter.getFight().ageBonus / 100.0);
            result = (int) truncate(truncate(num7 * (double) (100 + fighter.getStats().getTotal(StatsEnum.WISDOM)) / 100.0) * num8 * (fighter.getCharacter().getExpBonus() + butin -1 ));
            if(result < 0){
                logger.error(num7+" "+num8+" "+num6+" "+num5+" "+num4+" "+num3+" "+num2+" "+num);
            }
        }

        return result;
    }

    public static int computeXpPercoWin(TaxCollector fighter, MonsterFighter[] droppersResults, double butin, FightTeam winners) {
        int result;
        if (droppersResults.length == 0) {
            result = 0;
        } else {
            final int num = winners.getFighters().mapToInt(entry -> entry.getLevel()).sum();
            final int maxPlayerLevel = winners.getFighters().mapToInt(entry -> entry.getLevel()).max().orElse(0);
            final int num2 = Arrays.stream(droppersResults).mapToInt(dr -> dr.getLevel()).sum();
            final int b = Arrays.stream(droppersResults).mapToInt(dr -> dr.getLevel()).max().orElse(0);
            final int num3 = Arrays.stream(droppersResults).mapToInt(dr -> dr.getGrade().getGradeXp()).sum();
            double num4 = 1.0;
            if (num - 5 > num2) {
                num4 = (double) num2 / (double) num;
            } else {
                if (num + 10 < num2) {
                    num4 = (double) (num + 10) / (double) num2;
                }
            }
            final double num5 = Math.min((double) fighter.getLevel(), truncate(2.5 * (double) b)) / (double) num * 100.0;
            int num6 = Arrays.stream(droppersResults).
                    filter(mob -> mob.getLevel() >= maxPlayerLevel / 3)
                    .mapToInt(x -> 1)
                    .sum();
            if (num6 <= 0) {
                num6 = 1;
            }
            double num7 = truncate(num5 / 100.0 * truncate((double) num3 * GROUP_COEFFICIENTS[num6 - 1] * num4));
            double num8 = (winners.fight.ageBonus <= 0) ? 1.0 : (1.0 + (double) winners.fight.ageBonus / 100.0);
            result = (int) truncate(truncate(num7 * (double) (100 + fighter.getGuild().getEntity().wisdom) / 100.0) * num8 * (butin * 0.1f * DAO.getSettings().getDoubleElement("Rate.PvM") ) );
            if(result < 0){
                logger.error(num7+" "+num8+" "+num6+" "+num5+" "+num4+" "+num3+" "+num2+" "+num);
            }
        }

        return result;
    }

    private static double truncate(double value) {
        return value - value % 1;
    }

    public static short calculateEarnedDishonor(Fighter Character) {
        return Character.getFight().getEnnemyTeam(Character.getTeam()).alignmentSide != AlignmentSideEnum.ALIGNMENT_NEUTRAL ? (short) 0 : (short) 1;
    }

    public static int cotePoint(CharacterFighter fighter, Stream<Fighter> winners, Stream<Fighter> lossers, boolean isLosser) {
        if (System.currentTimeMillis() - fighter.getFight().getFightTime() > 120 * 1000) {
           fighter.getCharacter().addScore(isLosser ? ScoreType.ARENA_LOOSE : ScoreType.ARENA_WIN);
        }
        final int oldCote = fighter.getPlayer().getKolizeumRate().getRating();
        final PlayerInst inst = PlayerInst.getPlayerInst(fighter.getID());
        inst.updateCote(fighter.getCharacter(), !isLosser);
        if(isLosser){
            winners.map(f -> f.getPlayer().getKolizeumRate()).forEach(fighter.getPlayer().getKolizeumRate()::addLoss);
        }
        else{
            lossers.map(f -> f.getPlayer().getKolizeumRate()).forEach(fighter.getPlayer().getKolizeumRate()::addWin);
        }
        if(fighter.getPlayer().getKolizeumRate().getRating() > fighter.getPlayer().getScores().get(ScoreType.BEST_COTE)){
            fighter.getPlayer().getScores().replace(ScoreType.BEST_COTE, fighter.getPlayer().getKolizeumRate().getRating());
        }
        fighter.getPlayer().getKolizeumRate().update();
        return fighter.getPlayer().getKolizeumRate().getRating() - oldCote;
    }

    public static short honorPoint(Fighter fighter, Stream<Fighter> winners, Stream<Fighter> lossers, boolean isLosser) {
        return honorPoint(fighter, winners, lossers, isLosser, true);
    }

    public static short honorPoint(Fighter fighter, Stream<Fighter> winners, Stream<Fighter> lossers, boolean isLosser, boolean end) {

        if (fighter.getFight().getEnnemyTeam(fighter.getTeam()).alignmentSide == AlignmentSideEnum.ALIGNMENT_NEUTRAL) {
            return (short) 0;
        }

        if (System.currentTimeMillis() - fighter.getFight().getFightTime() > 2 * 60 * 1000 ) {
            ((CharacterFighter) fighter).getCharacter().addScore(isLosser ? ScoreType.PVP_LOOSE : ScoreType.PVP_WIN);
        }


        if (end && fighter.getFight().getWinners().getFighters().count() == 1L && fighter.getFight().getWinners().getFighters().count() == fighter.getFight().getEnnemyTeam(fighter.getFight().getWinners()).getFighters().count()) {
            return isLosser ? computeHonorLost(winners, lossers,false) : computeHonorWon(winners, lossers,false);
        }

        final double num1 = (double) winners.mapToInt(x -> x.getLevel()).sum();
        final double num2 = (double) lossers.mapToInt(x -> x.getLevel()).sum();
        double num3 = Math.floor(Math.sqrt((double) fighter.getLevel()) * 10.0 * (num2 / num1));
        if (isLosser) {
            if (num3 > fighter.getPlayer().getHonor()) {
                num3 = -(short) fighter.getPlayer().getHonor();
            } else {
                num3 = -num3;
            }
        }
        return (short) num3;
    }

    public static short koliseoPoint(Fighter fighter, Stream<Fighter> winners, Stream<Fighter> lossers, boolean isLosser, boolean end) {


        if (System.currentTimeMillis() - fighter.getFight().getFightTime() < (120 * 1000)
                || fighter.getPlayer().getInventoryCache().getEquipedItems().count() < 5) {
            return 0;
        }


        //if (end && fighter.getFight().getWinners().getFighters().count() == 1L && fighter.getFight().getWinners().getFighters().count() == fighter.getFight().getEnnemyTeam(fighter.getFight().getWinners()).getFighters().count()) {
        return isLosser ? computeHonorLost(winners, lossers,true) : computeHonorWon(winners, lossers,true);
        //}

        /*final double num1 = (double) winners.mapToInt(x -> x.getLevel()).sum();
        final double num2 = (double) lossers.mapToInt(x -> x.getLevel()).sum();
        double num3 = Math.floor(Math.sqrt((double) fighter.getLevel()) * 10.0 * (num2 / num1));
        if (isLosser) {
            if (num3 > fighter.getPlayer().getHonor()) {
                num3 = -(short) fighter.getPlayer().getHonor();
            } else {
                num3 = -num3;
            }
        }
        return (short) num3;*/
    }

    public static int XPDefie(Fighter fighter, Stream<Fighter> winners, Stream<Fighter> lossers) {

        int lvlLoosers = lossers.mapToInt(x -> x.getLevel()).sum();
        int lvlWinners = winners.mapToInt(x -> x.getLevel()).sum();

        int taux = DAO.getSettings().getIntElement("Rate.Challenge");
        float rapport = (float) lvlLoosers / (float) lvlWinners;
        int malus = 1;
        if ((double) rapport < 0.84999999999999998D) {
            malus = 6;
        }
        if (rapport >= 1.0F) {
            malus = 1;
        }
        int xpWin = (int) (((((rapport * (float) xpNeededAtLevel(fighter.getLevel())) / 10F) * (float) taux) / (long) malus) * (1 + (fighter.getStats().getTotal(StatsEnum.WISDOM) * 0.01)));
        if (xpWin < 0) {
            logger.error("xpWin <0 on lvlLoosers {} lvlWinners {} rapport {} need {} sasa {}", lvlLoosers, lvlWinners, rapport, ((((rapport * (float) xpNeededAtLevel(fighter.getLevel())) / 10F) * (float) taux) / (long) malus), (1 + (fighter.getStats().getTotal(StatsEnum.WISDOM) * 0.01)));
        }
        return xpWin;
    }

    private static long xpNeededAtLevel(int lvl) {
        return (DAO.getExps().getPlayerMaxExp(lvl) - DAO.getExps().getPlayerMinExp(lvl == 200 ? 198 : lvl));
    }

    public static int guildXpEarned(CharacterFighter Fighter, AtomicInteger xpWin) {
        if (Fighter.getCharacter() == null || xpWin.get() == 0) {
            return 0;
        }
        if (Fighter.getCharacter().getGuild() == null) {
            return 0;
        }

        final GuildMember gm = Fighter.getCharacter().getGuildMember();

        double xp = (double) xpWin.get(), Lvl = Fighter.getLevel(), LvlGuild = Fighter.getCharacter().getGuild().getEntity().level, pXpGive = (double) gm.experienceGivenPercent / 100;

        double maxP = xp * pXpGive * 0.10;    //Le maximum donné à la guilde est 10% du montant prélevé sur l'xp du combat
        double diff = Math.abs(Lvl - LvlGuild);    //Calcul l'écart entre le niveau du personnage et le niveau de la guilde
        double toGuild;
        if (diff >= 70) {
            toGuild = maxP * 0.10;    //Si l'écart entre les deux level est de 70 ou plus, l'experience donnée a la guilde est de 10% la valeur maximum de don
        } else if (diff >= 31 && diff <= 69) {
            toGuild = maxP - ((maxP * 0.10) * (Math.floor((diff + 30) / 10)));
        } else if (diff >= 10 && diff <= 30) {
            toGuild = maxP - ((maxP * 0.20) * (Math.floor(diff / 10)));
        } else //Si la différence est [0,9]
        {
            toGuild = maxP;
        }
        xpWin.set((int) (xp - xp * pXpGive));

        Fighter.getCharacter().getGuild().onFighterAddedExperience(gm,  Math.round(toGuild));

        return (int) Math.round(toGuild);
    }

    public static int mountXpEarned(CharacterFighter fighter, AtomicInteger xpWin) {
        if (fighter == null || xpWin.get() <= 0) {
            return 0;
        }
        if (fighter.getCharacter().getMountInfo() == null) {
            return 0;
        }
        if (!fighter.getCharacter().getMountInfo().isToogled
                || fighter.getCharacter().getMountInfo().mount == null
                || fighter.getCharacter().getMountInfo().ratio <= 0) {
            return 0;
        }

        final int diff = Math.abs(fighter.getLevel() - fighter.getCharacter().getMountInfo().mount.level);

        double coeff = 0;
        final double xp =  xpWin.get();
        double pToMount = fighter.getCharacter().getMountInfo().ratio / 100f + 0.2;

        if (diff >= 0 && diff <= 9) {
            coeff = 0.1;
        } else if (diff >= 10 && diff <= 19) {
            coeff = 0.08;
        } else if (diff >= 20 && diff <= 29) {
            coeff = 0.06;
        } else if (diff >= 30 && diff <= 39) {
            coeff = 0.04;
        } else if (diff >= 40 && diff <= 49) {
            coeff = 0.03;
        } else if (diff >= 50 && diff <= 59) {
            coeff = 0.02;
        } else if (diff >= 60 && diff <= 69) {
            coeff = 0.015;
        } else {
            coeff = 0.01;
        }

        if (pToMount > 0.2) {
            xpWin.set((int) (xp - (xp * (pToMount - 0.2))));
        }

        fighter.getCharacter().getMountInfo().addExperience((long) Math.round(xp * pToMount * coeff));

        if (xp > 0) {
            fighter.getCharacter().send(new MountSetMessage(fighter.getCharacter().getMountInfo().mount));
        }

        return (int) Math.round(xp * pToMount * coeff);
    }

    public static short computeHonorWon(Stream<Fighter> winners, Stream<Fighter> loosers,boolean isKoli) {
        try {
            int totalGradeWinner = 0, totalPhWinner = 0;
            int totalGradeLooser = 0, totalPhLooser = 0;
            int totalGradeWinnerForEached = 0;
            int totalGradeLooserForEached = 0;
            for (Fighter fighter : (Iterable<Fighter>) winners::iterator) {
                if (!(fighter instanceof CharacterFighter) /*&& fighter.getPrisme() == null*/) {
                    continue;
                }
                if (fighter instanceof CharacterFighter) {
                    if(isKoli) {
                        totalGradeWinner += fighter.getPlayer().getKoliseoGrade();
                        totalPhWinner += fighter.getPlayer().getKoliseoPoints();
                    }
                    else {
                        totalGradeWinner += fighter.getPlayer().getAlignmentGrade();
                        totalPhWinner += fighter.getPlayer().getHonor();
                    }

                    totalGradeWinnerForEached++;
                } /*else {
                 TotalGradeWinner += fighter.getPrisme().getLevel();
                 }*/

            }
            for (Fighter fighter : (Iterable<Fighter>) loosers::iterator) {
                if (!(fighter instanceof CharacterFighter) /*&& fighter.getPrisme() == null*/) {
                    continue;
                }
                if (fighter instanceof CharacterFighter) {
                    if(isKoli) {
                        totalGradeLooser += fighter.getPlayer().getKoliseoGrade();
                        totalPhLooser += fighter.getPlayer().getKoliseoPoints();
                    }
                    else {
                        totalGradeLooser += fighter.getPlayer().getAlignmentGrade();
                        totalPhLooser += fighter.getPlayer().getHonor();
                    }

                    totalGradeLooserForEached++;
                } /*else {
                 TotalGradeLooser += fighter.getPrisme().getLevel();
                 }*/

            }
            final int ecartPoint = (totalPhWinner / totalGradeWinnerForEached) - (totalPhLooser / totalGradeLooserForEached);
            if(ecartPoint > 17000 && totalPhWinner > totalPhLooser){
                return (short) EffectHelper.randomValue(40, 75);
            }
            final int ecartGrade = (totalGradeWinner / totalGradeWinnerForEached) - (totalGradeLooser / totalGradeLooserForEached);
            final int randomGain = EffectHelper.randomValue(100, 120);
            final int randomValue = EffectHelper.randomValue(140, 160);
            return (short) (totalGradeWinner <= 5 ? (randomGain + (ecartGrade < 0 ? 10 * -ecartGrade : 0)) : (randomValue + (ecartGrade < 0 ? 10 * -ecartGrade : 7 * -ecartGrade)));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    public static short computeHonorLost(Stream<Fighter> loosers, Stream<Fighter> winners, boolean isKoli) {
        try {
            int totalGradeWinner = 0, totalPhWinner = 0;
            int totalGradeLooser = 0, totalPhLooser = 0;
            int totalGradeWinnerForEached = 0;
            int totalGradeLooserForEached = 0;
            for (Fighter fighter : (Iterable<Fighter>) winners::iterator) {
                if (!(fighter instanceof CharacterFighter) /*&& fighter.getPrisme() == null*/) {
                    continue;
                }
                if (fighter instanceof CharacterFighter) {
                    if(isKoli) {
                        totalGradeWinner += fighter.getPlayer().getKoliseoGrade();
                        totalPhWinner += fighter.getPlayer().getKoliseoPoints();
                    }
                    else {
                        totalGradeWinner += fighter.getPlayer().getAlignmentGrade();
                        totalPhWinner += fighter.getPlayer().getHonor();
                    }

                    totalGradeWinnerForEached++;
                } /*else {
                 TotalGradeWinner += fighter.getPrisme().getLevel();
                 }*/


            }
            for (Fighter fighter : (Iterable<Fighter>) loosers::iterator) {
                if (!(fighter instanceof CharacterFighter) /*&& fighter.getPrisme() == null*/) {
                    continue;
                }
                if (fighter instanceof CharacterFighter) {
                    if(isKoli) {
                        totalGradeLooser += fighter.getPlayer().getKoliseoGrade();
                        totalPhLooser += fighter.getPlayer().getKoliseoPoints();
                    }
                    else {
                        totalGradeLooser += fighter.getPlayer().getAlignmentGrade();
                        totalPhLooser += fighter.getPlayer().getHonor();
                    }

                    totalGradeLooserForEached++;

                } /*else {
                 TotalGradeLooser += fighter.getPrisme().getLevel();
                 }*/


            }
            final int ecartGrade = (totalGradeWinner / totalGradeWinnerForEached) - (totalGradeLooser / totalGradeLooserForEached);
            final int ecartPoint = (totalPhWinner / totalGradeWinnerForEached) - (totalPhLooser / totalGradeLooserForEached);
            if(ecartPoint > 17000 && totalPhWinner < totalPhLooser){
                return (short) -EffectHelper.randomValue(20, 30);
            }
            int randomPerte = 0;
            switch (totalGradeWinner) {
                case 1:
                    randomPerte = EffectHelper.randomValue(30, 40);
                    break;
                case 2:
                    randomPerte = EffectHelper.randomValue(50, 60);
                    break;
                case 3:
                    randomPerte = EffectHelper.randomValue(60, 70);
                    break;
                case 4:
                    randomPerte = EffectHelper.randomValue(70, 80);
                    break;
                case 5:
                    randomPerte = EffectHelper.randomValue(80, 90);
                    break;
                case 6:
                    randomPerte = EffectHelper.randomValue(100, 200);
                    break;
                case 7:
                    randomPerte = EffectHelper.randomValue(200, 300);
                    break;
                case 8:
                    randomPerte = EffectHelper.randomValue(300, 400);
                    break;
                case 9:
                    randomPerte = EffectHelper.randomValue(400, 500);
                    break;
                case 10:
                    //randomPerte = 500;
                    randomPerte = EffectHelper.randomValue(650, 800);
                    break;
            }
            return (short) -(totalGradeWinner <= 5 ? (randomPerte + (ecartGrade > 0 ? 10 * ecartGrade : 0)) : (randomPerte + (ecartGrade > 0 ? 20 * ecartGrade : 0)));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    public static double adjustDropChance(Fighter looter, MonsterDrop item, MonsterGrade dropper, int monsterAgeBonus, double butin) {
        return item.getDropRate((int) dropper.getGrade()) * ((double) looter.getStats().getTotal(StatsEnum.PROSPECTING) / 100.0) * ((double) monsterAgeBonus / 100.0 + 1.0) * DAO.getSettings().getDoubleElement("Rate.Drop") * butin;
    }

    public static double adjustDropChance(TaxCollector looter, MonsterDrop item, MonsterGrade dropper, int monsterAgeBonus, double butin) {
        return item.getDropRate((int) dropper.getGrade()) * ((double) looter.getGuild().getEntity().prospecting / 100.0) * ((double) monsterAgeBonus / 100.0 + 1.0) * DAO.getSettings().getDoubleElement("Rate.Drop") * butin;
    }


    public static void rollLoot(Fighter looter, MonsterGrade mob, int prospectingSum, Map<MonsterDrop, Integer> droppedItems, List<DroppedItem> list, double butin) {
        mob.getMonster().getDrops()
                .stream()
                .filter(drop -> prospectingSum >= drop.getProspectingLock())
                .forEach(current -> {
                    if ((current.getDropLimit() <= 0 || !droppedItems.containsKey(current) || droppedItems.get(current) < current.getDropLimit())) {
                        double num2 = (double) looter.getRANDOM().nextInt(100) + looter.getRANDOM().nextDouble();
                        double num3 = adjustDropChance(looter, current, mob, (int) looter.getFight().ageBonus,butin);
                        if (num3 >= num2) {
                            final Optional<DroppedItem> item = list.stream()
                                    .filter(dr -> dr.getItem() == current.getObjectId())
                                    .findFirst();
                            if (item.isPresent()) {
                                item.get().accumulateQuantity();
                            } else
                                list.add(new DroppedItem(current.getObjectId(), 1));

                            if (!droppedItems.containsKey(current)) {
                                droppedItems.put(current, 1);
                            } else {
                                droppedItems.put(current, droppedItems.get(current) + 1);
                            }
                        }
                    }
                });
    }

    public static void rollLoot(TaxCollector looter, MonsterGrade mob, int prospectingSum, Map<MonsterDrop, Integer> droppedItems, List<DroppedItem> list, double butin, Fight fight) {
        mob.getMonster().getDrops()
                .stream()
                .filter(drop -> prospectingSum >= drop.getProspectingLock())
                .forEach(current -> {
                    if ((current.getDropLimit() <= 0 || !droppedItems.containsKey(current) || droppedItems.get(current) < current.getDropLimit())) {
                        double num2 = (double) Fight.RANDOM.nextInt(100) + Fight.RANDOM.nextDouble();
                        double num3 = adjustDropChance(looter, current, mob, (int) fight.ageBonus,butin);
                        if (num3 >= num2) {
                            final Optional<DroppedItem> item = list.stream()
                                    .filter(dr -> dr.getItem() == current.getObjectId())
                                    .findFirst();
                            if (item.isPresent()) {
                                item.get().accumulateQuantity();
                            } else
                                list.add(new DroppedItem(current.getObjectId(), 1));

                            if (!droppedItems.containsKey(current)) {
                                droppedItems.put(current, 1);
                            } else {
                                droppedItems.put(current, droppedItems.get(current) + 1);
                            }
                        }
                    }
                });
    }


    public static int computeKamas(Fighter fighter, int baseKamas, int teamPP) {
        double num = (fighter.getFight().ageBonus <= 0) ? 1.0 : (1.0 + (double) fighter.getFight().ageBonus / 100.0);
        return (int) ((double) baseKamas * ((double) fighter.getStats().getTotal(StatsEnum.PROSPECTING) / (double) teamPP) * num * DAO.getSettings().getDoubleElement("Rate.Kamas"));
    }

    public static int computeKamas(TaxCollector fighter, int baseKamas, int teamPP, Fight fight) {
        double num = (fight.ageBonus <= 0) ? 1.0 : (1.0 + (double) fight.ageBonus / 100.0);
        return (int) ((double) baseKamas * ((double) fighter.getGuild().getEntity().prospecting / (double) teamPP) * num * DAO.getSettings().getDoubleElement("Rate.Kamas") * 0.10f);
    }
}
