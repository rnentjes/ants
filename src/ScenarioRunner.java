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

        for (int scenarioNr = 0; scenarioNr < enemyAnts.size() * 4; scenarioNr++) {
            int nr = scenarioNr;
            Map<Tile, Aim> targets = new HashMap<Tile, Aim>();

            boolean valid = true;
            for (Tile ant : enemyAnts) {
                Aim aim = aims[nr & 3];

                if (!bot.goodOrder(ant, aim)) {
                    // this scenario is not valid
                    valid = false;
                    break;
                }
                targets.put(ant, aim);
                nr = nr >>> 2;
            }

            if (valid) {
                for (int scenarioNr2 = 0; scenarioNr2 < friendlyAnts.size() * 4; scenarioNr2++) {
                    int nr2 = scenarioNr2;
                    Map<Tile, Aim> friendlies = new HashMap<Tile, Aim>();

                    for (Tile ant : friendlyAnts) {
                        Aim aim = aims[nr2 & 3];

                        if (!bot.goodOrder(ant, aim)) {
                            // this scenario is not valid
                            valid = false;
                            break;
                        }
                        friendlies.put(ant, aim);
                        nr2 = nr2 >>> 2;
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
        int bestLosses = 999;

        for (int scenarioNr2 = 0; scenarioNr2 < friendlyAnts.size() * 4 && bot.hasRemainingTime(); scenarioNr2++) {
            boolean valid = true;
            int nr2 = scenarioNr2;
            Map<Tile, Aim> friendlies = new HashMap<Tile, Aim>();

            for (Tile ant : friendlyAnts) {
                Aim aim = aims[nr2 & 3];

                if (!bot.goodOrder(ant, aim)) {
                    // this scenario is not valid
                    valid = false;
                    break;
                }
                friendlies.put(ant, aim);
                nr2 = nr2 >>> 2;
            }

            if (valid) {
                int totalKills = 0;
                int totalLosses = 0;

                for (int scenarioNr = 0; scenarioNr < enemyAnts.size() * 4 && bot.hasRemainingTime(); scenarioNr++) {
                    int nr = scenarioNr;
                    Map<Tile, Aim> targets = new HashMap<Tile, Aim>();

                    for (Tile ant : enemyAnts) {
                        Aim aim = aims[nr&3];

                        Tile target = ants.getTile(ant, aim);

                        if (!ants.getIlk(target).isPassable()) {
                            // this scenario is not valid
                            valid = false;
                            break;
                        }
                        targets.put(ant, aim);
                        nr = nr >>> 2;
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
                    best = friendlies;
                } else if (bestLosses == totalLosses && totalKills >= bestKills) {
                    bestKills = totalKills;
                    best = friendlies;
                }
            }
        }


        if (bestKills == 0 && bestLosses == 0) {
            // ignore resolution
            best = null;
        }

        return best;
    }

    public static void main(String[] args) {
        Ants ants = new Ants(1000, 1000, 10, 10, 100, 49, 25, 16, 1);

        List<Tile> en = new LinkedList<Tile>();
        List<Tile> fr = new LinkedList<Tile>();

        en.add(new Tile(3, 5));
        fr.add(new Tile(7, 5));
        fr.add(new Tile(7, 6));

        MyBot bot = new MyBot();
        bot.setAnts(ants);
        bot.setDebug(true);

        ScenarioRunner runner = new ScenarioRunner(bot, ants, en, fr);

        Map<Tile, Aim> scenario = runner.findBestScenario();

        System.out.println(scenario);
    }

}
