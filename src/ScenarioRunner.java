import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: rnentjes
 * Date: 11/12/11
 * Time: 2:37 PM
 */
public class ScenarioRunner {

    Aim[] aims = {Aim.NORTH, Aim.WEST, Aim.SOUTH, Aim.EAST, null};
    List<Tile> enemyAnts;
    List<Tile> friendlyAnts;
    Ants ants;
    Bot bot;

    public ScenarioRunner(Bot bot, Ants ants, List<Tile> enemyAnts, List<Tile> friendlyAnts) {
        this.bot = bot;
        this.ants = ants;
        this.enemyAnts = enemyAnts;
        this.friendlyAnts = friendlyAnts;
    }

    public Scenario findBestScenarioOld() {
        Scenario best = null;

        for (int scenarioNr = 0; scenarioNr < enemyAnts.size() * 5 && bot.hasRemainingTime(); scenarioNr++) {
            int nr = scenarioNr;
            Map<Tile, Aim> targets = new HashMap<Tile, Aim>();

            boolean valid = true;
            for (Tile ant : enemyAnts) {
                Aim aim = aims[nr % 5];

                if (!bot.goodOrder(ant, aim)) {
                    // this scenario is not valid
                    valid = false;
                    break;
                }
                targets.put(ant, aim);
                nr = nr / 5;
            }

            if (valid) {
                for (int scenarioNr2 = 0; scenarioNr2 < friendlyAnts.size() * 5 && bot.hasRemainingTime(); scenarioNr2++) {
                    int nr2 = scenarioNr2;
                    Map<Tile, Aim> friendlies = new HashMap<Tile, Aim>();

                    for (Tile ant : friendlyAnts) {
                        Aim aim = aims[nr2 % 5];

                        if (!bot.goodOrder(ant, aim)) {
                            // this scenario is not valid
                            valid = false;
                            break;
                        }
                        friendlies.put(ant, aim);
                        nr2 = nr2 / 5;
                    }

                    if (valid) {
                        Scenario scenario = new Scenario(bot, ants, targets, friendlies);

                        scenario.calculate();

                        if (best == null || best.compareTo(scenario) < 0) {
                            best = scenario;
                        }
                    }
                }
            }
        }

        return best;
    }

    public Map<Tile, Aim> findBestScenario() {
        Map<Tile, Aim> best = null;

        int bestKills = 0;
        int bestLosses = 99999999;

        int friendlyCount = 1;
        int enemyCount = 1;

        for (int index = 0; index < friendlyAnts.size(); index++) {
            friendlyCount *= 5;
        }

        for (int index = 0; index < enemyAnts.size(); index++) {
            enemyCount *= 5;
        }

        for (int scenarioNr2 = 0; scenarioNr2 < friendlyCount && bot.hasRemainingTime(); scenarioNr2++) {
            boolean valid = true;
            int nr2 = scenarioNr2;
            Map<Tile, Aim> friendlies = new HashMap<Tile, Aim>();

            for (Tile ant : friendlyAnts) {
                Aim aim = aims[nr2 % 5];

                if (!bot.goodOrder(ant, aim)) {
                    // this scenario is not valid
                    valid = false;
                    break;
                }
                
                friendlies.put(ant, aim);
                nr2 = nr2 / 5;
            }

            if (valid) {
                int totalKills = 0;
                int totalLosses = 0;

                for (int scenarioNr = 0; scenarioNr < enemyCount && bot.hasRemainingTime(); scenarioNr++) {
                    int nr = scenarioNr;
                    Map<Tile, Aim> targets = new HashMap<Tile, Aim>();

                    for (Tile ant : enemyAnts) {
                        Aim aim = aims[nr % 5];

                        Tile target = ants.getTile(ant, aim);

                        if (!ants.getIlk(target).isPassable()) {
                            // this scenario is not valid
                            valid = false;
                            break;
                        }
                        targets.put(ant, aim);
                        nr = nr / 5;
                    }

                    if (valid) {
                        Scenario scenario = new Scenario(bot, ants, targets, friendlies);

                        scenario.calculate();

                        totalKills += scenario.getKills();
                        totalLosses += scenario.getLosses();
                    }
                }

                bot.debug("Scenario("+totalKills+","+totalLosses+") -> "+friendlies);

                if (bestLosses > totalLosses) {
                    bestLosses = totalLosses;
                    bestKills = totalKills;
                    best = friendlies;
                } else if (bestLosses == totalLosses && totalKills > bestKills) {
                    bestKills = totalKills;
                    best = friendlies;
                }
            }
        }

        //if (bestKills == 0 && bestLosses == 0) {
            // ignore resolution
        //    best = null;
        //}


        return best;
    }

    public static void main(String[] args) {
        Ants ants = new Ants(1000, 300000, 10, 10, 100, 49, 25, 16, 1);

        List<Tile> en = new LinkedList<Tile>();
        List<Tile> fr = new LinkedList<Tile>();

        en.add(new Tile(8, 5));
        fr.add(new Tile(2, 5));
        fr.add(new Tile(2, 6));

        MyBot bot = new MyBot();
        bot.setAnts(ants);
        bot.setDebug(true);

        ScenarioRunner runner = new ScenarioRunner(bot, ants, en, fr);

        Map<Tile, Aim> scenario = runner.findBestScenario();

        System.out.println(scenario);
    }

}
