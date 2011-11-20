import java.util.LinkedList;
import java.util.List;

/**
 * User: rnentjes
 * Date: 11/5/11
 * Time: 5:46 PM
 */
public class MoveToFood extends Move {

    public MoveToFood(List<Tile> route) {
        super(route);
    }

    @Override
    public void execute(Bot bot, Ants ants) {
        super.execute(bot, ants);

        // skip last move to food
        if (route.size() == 2) {
            bot.removeCommand(this);
        }
    }

    @Override
    public List<Tile> skipTiles() {
        List<Tile> result = new LinkedList<Tile>();

        result.addAll(super.skipTiles());
        result.addAll(returnAsList(route.get(route.size()-1)));

        return result;
    }

    public String toString() {
        return "Moving (F) - "+route;
    }
}
