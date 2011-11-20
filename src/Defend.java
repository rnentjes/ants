import java.util.List;

/**
 * User: rnentjes
 * Date: 11/5/11
 * Time: 5:45 PM
 */
public class Defend extends Command {

    private Tile hill;
    private Tile ant;

    public Defend(Tile tile) {
        hill = new Tile(tile.getRow(), tile.getCol());
        ant = new Tile(tile.getRow(), tile.getCol());
    }

    @Override
    public void execute(Bot bot, Ants ants) {
        for (Aim aim : Aim.values()) {
            Tile target = ants.getTile(ant, aim);

            if (bot.goodOrder(ant, aim) && !target.equals(hill)) {
                bot.executeOrder(ant, target);
                ant = target;
                break;
            }
        }

    }

    @Override
    public List<Tile> skipTiles() {
        return returnAsList(ant);
    }

    public Tile getHill() {
        return hill;
    }

    public String toString() {
        return "Ant "+ant+" defend "+hill;
    }
}
