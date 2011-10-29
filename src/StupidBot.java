import java.io.IOException;

/**
 * Starter bot implementation.
 */
public class StupidBot extends Bot {
    /**
     * Main method executed by the game engine for starting the bot.
     *
     * @param args command line arguments
     * @throws java.io.IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            debug = true;
        }

        new StupidBot().readSystemInput();
    }

    public StupidBot() {
        debug("Starting");
    }

    /**
     * For every ant check every direction in fixed order (N, E, S, W) and move it if the tile is
     * passable.
     */
    @Override
    public void doSubTurn() {
    }

}
