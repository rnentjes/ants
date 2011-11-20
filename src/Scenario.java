import java.util.HashMap;
import java.util.Map;

/**
 * User: rnentjes
 * Date: 11/12/11
 * Time: 2:39 PM
 */
public class Scenario implements Comparable<Scenario> {

    Map<Tile, Aim> enemyAnts;
    Map<Tile, Aim> friendlyAnts;
    boolean done = false;
    int kills = 0;
    int losses = 0;
    Ants ants;
    Bot bot;

    public Scenario(Bot bot, Ants ants, Map<Tile, Aim> enemyAnts, Map<Tile, Aim> friendlyAnts) {
        this.bot = bot;
        this.ants = ants;
        this.enemyAnts = enemyAnts;
        this.friendlyAnts = friendlyAnts;
    }

    public void execute() {
        for (Tile ant : friendlyAnts.keySet()) {
            bot.executeOrder(ant, friendlyAnts.get(ant));
        }
    }

    public void calculate() {
        Map<Tile, Integer> closeBy = new HashMap<Tile, Integer>();

        for (Tile tile : enemyAnts.keySet()) {
            Tile target = ants.getTile(tile, enemyAnts.get(tile));
            int nr = 0;
            for (Tile ant : friendlyAnts.keySet()) {
                Tile target2 = ants.getTile(ant, friendlyAnts.get(ant));
                if (ants.getDistance(target, target2) <= ants.getAttackRadius2()) {
                    nr++;
                }
            }
            closeBy.put(tile, nr);
        }

        for (Tile tile : friendlyAnts.keySet()) {
            Tile target = ants.getTile(tile, friendlyAnts.get(tile));
            int nr = 0;
            for (Tile ant : enemyAnts.keySet()) {
            Tile target2 = ants.getTile(ant, enemyAnts.get(ant));
                if (ants.getDistance(target, target2) <= ants.getAttackRadius2()) {
                    nr++;
                }
            }
            closeBy.put(tile, nr);
        }

        for (Tile tile : enemyAnts.keySet()) {
            int nr = 0;
            for (Tile ant : friendlyAnts.keySet()) {
                if (closeBy.get(tile) >= closeBy.get(ant)) {
                    kills++;
                }
            }
        }

        for (Tile tile : friendlyAnts.keySet()) {
            int nr = 0;
            for (Tile ant : enemyAnts.keySet()) {
                if (closeBy.get(tile) >= closeBy.get(ant)) {
                    losses++;
                }
            }
        }
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int compareTo(Scenario o) {
        return (losses - o.losses) * 2 + (o.kills - kills);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("Kills: ");
        result.append(kills);
        result.append("\nLosses: ");
        result.append(losses);
        result.append("\n");
        for (Tile ant : friendlyAnts.keySet()) {
            result.append(ant);
            result.append(" -> ");
            result.append(friendlyAnts.get(ant));
            result.append("\n");
        }

        return result.toString();
    }
}
