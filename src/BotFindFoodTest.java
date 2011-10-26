/**
 * User: rnentjes
 * Date: 10/23/11
 * Time: 11:45 AM
 */
public class BotFindFoodTest {


    public static void main(String [] args) {
        MyBot bot = new MyBot();

        bot.setup(0, 0, 10, 10, 100, 9, 9, 9, 42);

        bot.addFood(7,7);
        bot.addAnt(5,5,0);

        bot.getAnts().printMap();

        bot.doTurn();

        bot.showStandingOrders();
        bot.addAnt(5,6,0);

        bot.doTurn();

        bot.showStandingOrders();

    }
}
