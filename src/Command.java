import java.util.Arrays;
import java.util.List;

/**
 * User: rnentjes
 * Date: 11/5/11
 * Time: 5:44 PM
 */
public abstract class Command {

    public abstract void execute(Bot bot, Ants ants);

    public abstract List<Tile> skipTiles();

    public List<Tile> returnAsList(Tile ... tiles) {
        return Arrays.asList(tiles);
    }

}
