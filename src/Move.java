import java.util.List;

/**
 * User: rnentjes
 * Date: 11/5/11
 * Time: 5:46 PM
 */
public class Move extends Command {

    protected List<Tile> route;

    public Move(List<Tile> route) {
        this.route = route;
    }

    public Tile getTarget() {
        return route.get(route.size() - 1);
    }

    public Tile getSource() {
        return route.get(0);
    }

    @Override
    public void execute(Bot bot, Ants ants) {
        Tile ant = route.remove(0);

        Tile target = route.get(0);

        Aim aim = bot.getDirection(ant, target);

        if (bot.goodOrder(ant, aim)) {
            bot.executeOrder(ant, aim);
        }

        if (route.size() == 1) {
            bot.removeCommand(this);
        }
    }

    public String toString() {
        return "Moving - " + route;
    }

    @Override
    public List<Tile> skipTiles() {
        return returnAsList(route.get(0));
    }

}
