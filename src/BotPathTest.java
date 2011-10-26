import java.util.List;

/**
 * User: rnentjes
 * Date: 10/23/11
 * Time: 11:45 AM
 */
public class BotPathTest {


    public static void main(String [] args) {
        Bot bot = new MyBot();

        bot.setup(0,5000,20,20,100,5,5,5,42);

        bot.addWater(10,0);
        bot.addWater(10,1);
        bot.addWater(10,2);
        bot.addWater(10,3);
        bot.addWater(10,4);
        bot.addWater(10,5);
        bot.addWater(10,6);
        bot.addWater(10,7);
        bot.addWater(10,8);
        bot.addWater(10,9);
        bot.addWater(10,10);
        bot.addWater(10,11);
        bot.addWater(10,12);
        bot.addWater(10,13);
        bot.addWater(9,13);
        bot.addWater(8,13);
        bot.addWater(7,13);

        bot.getAnts().printMap();

        List<Tile> path = bot.findShortestPath(new Tile(7,10), new Tile(13,14));

        System.out.println("\nPATH FOUND:");
        for(Tile tile : path) {
            System.out.println(tile);
            bot.removeAnt(tile.getRow(), tile.getCol(), 0);
        }
        System.out.println();
        bot.getAnts().printMap();
    }
}
