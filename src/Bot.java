import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Provides basic game state handling.
 */
public abstract class Bot extends AbstractSystemInputParser {
    static boolean debug = false;
    PrintWriter out = null;

    public void debug(String message) {
        if (debug) {
            if (out == null) {
                try {
                    int count = 1;
                    File file = new File(".", "bot_log_" + count + ".log");

                    while (file.exists()) {
                        count++;
                        file = new File(".", "bot_log_" + count + ".log");
                    }

                    out = new PrintWriter(file);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }

            out.println(message);
            out.flush();
        }
    }

    private Ants ants;

    protected Map<Tile, Integer> borders = new HashMap<Tile, Integer>();
    protected Map<Tile, Integer> visited = new HashMap<Tile, Integer>();
    protected Map<Tile, Integer> avoidAnts = new HashMap<Tile, Integer>();
    protected Map<Tile, Integer> attackAnts = new HashMap<Tile, Integer>();
    protected Map<Tile, Integer> foodTiles = new HashMap<Tile, Integer>();
    protected Map<Tile, Integer> attackHills = new HashMap<Tile, Integer>();

    protected final static Random r = new Random(1);

    protected Map<Tile, List<Tile>> standingOrders = new HashMap<Tile, List<Tile>>();
    protected int viewDistance1 = 3;
    protected int attackDistance1 = 3;
    protected int turn = 0;
    protected boolean init = false;

    protected List<Tile> targetedTiles = new LinkedList<Tile>();
    protected List<Tile> antsDone = new LinkedList<Tile>();
    protected List<Tile> remainingAnts = new LinkedList<Tile>();

    protected List<Command> commands = new LinkedList<Command>();

    public void doTurn() {
        long time = System.currentTimeMillis();
        turn++;
        debug("---------- Turn " + turn + ", " + getAnts().getMyAnts().size() + " ants, " + getAnts().getMyHills() + " hills. ----------");

        try {
            targetedTiles = new LinkedList<Tile>();
            antsDone = new LinkedList<Tile>();
            remainingAnts = new LinkedList<Tile>();
            avoidAnts = new HashMap<Tile, Integer>();
            attackAnts = new HashMap<Tile, Integer>();
            foodTiles = new HashMap<Tile, Integer>();
            attackHills = new HashMap<Tile, Integer>();

            doSubTurn();

            /*
            if (hasRemainingTime() && avoidAnts.size() > 0) {
                debug("AVOID MAP:");
                debug(showMap(avoidAnts));
            }
            if (hasRemainingTime() && attackAnts.size() > 0) {
                debug("ATTACK MAP:");
                debug(showMap(attackAnts));
            }
            */
        } catch (Exception t) {
            if (debug) {
                t.printStackTrace(out);
                out.flush();
            }
            throw new IllegalStateException(t);
        }

        if (debug) {
            //debug(showVisitedMap());
        }

        time = System.currentTimeMillis() - time;
        debug("********** Turn " + turn + "/" + getAnts().getTurns() + " took - " + time + "/" + getAnts().getTurnTime() + " **********");

    }

    public abstract void doSubTurn();

    protected void init() {
        debug("Init");

        setViewDistance1();
        setAttackDistance1();

        debug("Init done");
        //init = true;
    }

    protected void runCommands() {
        List<Command> tmp = new LinkedList<Command>(commands);

        for (Command command : tmp) {
            debug("Command: " + command);
            command.execute(this, getAnts());
        }
    }

    protected void removeCommand(Command command) {
        commands.remove(command);
    }

    protected void addCommand(Command command) {
        commands.add(command);
    }

    protected boolean hasRemainingTime() {
        return getAnts().getTimeRemaining() > (getAnts().getTurnTime() / 10);
    }

    protected boolean hasLittleRemainingTime() {
        return getAnts().getTimeRemaining() > (getAnts().getTurnTime() / 20);
    }

    protected int colOffset = 0;
    protected int rowOffset = 0;
    protected int currentCol = 0;
    protected int currentRow = 0;
    protected int currentDepth = 4;
    protected final int maxDepth = 4;

    protected void updateBorderMap(int value) {
        int count = (getAnts().getCols() * getAnts().getRows());
        currentRow = 0;
        currentCol = 0;
        boolean done = false;

        //Map<Tile, Integer> target = new HashMap<Tile, Integer>();
        //target.putAll(borders);

        while (!done && hasLittleRemainingTime()) {
            Tile tile = new Tile(currentRow, currentCol);

            Ilk ilk = getAnts().getIlk(tile);

            if (!ilk.isPassable()) {
                borders.put(tile, 9);
            }

            currentCol++;
            if (currentCol >= getAnts().getCols()) {
                currentCol = 0;
                currentRow++;
                if (currentRow >= getAnts().getRows()) {
                    currentRow = 0;
                    done = true;
                }
            }
        }

        while (--count > 0 && hasLittleRemainingTime()) {
            int row = (currentRow + rowOffset) % getAnts().getRows();
            int col = (currentCol + colOffset) % getAnts().getCols();

            Tile tile = new Tile(row, col);

            if (tileWithinViewRange(tile)) {
                setTileDependingonBorderingTiles(borders, tile, value);
            }

            currentCol += 3;
            if (currentCol >= getAnts().getCols()) {
                currentCol = 0;
                currentRow += 3;
                if (currentRow >= getAnts().getRows()) {
                    currentRow = 0;

                    colOffset++;
                    if (colOffset >= 3) {
                        rowOffset++;
                        colOffset = 0;
                        if (rowOffset >= 3) {
                            rowOffset = 0;
                        }
                    }
                }
            }
        }
    }

    protected void updateBorderMapO2() {
        int count = getAnts().getCols() * getAnts().getRows();

        while (--count > 0 && hasRemainingTime()) {
            Tile tile = new Tile(currentRow, currentCol);

            if (tileWithinViewRange(tile)) {

                Ilk ilk = getAnts().getIlk(tile);

                if (currentDepth == maxDepth) {
                    if (!ilk.isPassable()) {
                        borders.put(tile, 9);
                        setBorderingTiles(tile, 9);
                    }
                } else {
                    if (ants.getIlk(tile).isPassable()) {
                        Integer foundDepth = borders.get(tile);
                        if (foundDepth != null && foundDepth == (currentDepth + 1)) {
                            setBorderingTiles(tile, currentDepth);
                        }
                    }
                }
            }

            currentCol++;
            if (currentCol >= getAnts().getCols()) {
                currentCol = 0;
                currentRow++;
                if (currentRow >= getAnts().getRows()) {
                    currentRow = 0;
                    currentDepth--;
                    if (currentDepth == 0) {
                        currentDepth = maxDepth;
                    }
                }
            }
        }
    }

    protected boolean tileWithinViewRange(Tile tile) {
        boolean result = false;
        int radius = (getAnts().getViewRadius2() * 3) / 4;

        for (Tile ant : getAnts().getMyAnts()) {
            if (getAnts().getDistance(tile, ant) < radius) {
                result = true;
                break;
            }
        }

        return result;
    }


    protected void uodateBorderMapOld() {
        debug("ViewDistance 1 = " + viewDistance1);

        int depth = 3;

        for (int col = 0; col < getAnts().getCols() && hasRemainingTime(); col++) {
            for (int row = 0; row < getAnts().getRows() && hasRemainingTime(); row++) {
                Tile tile = new Tile(row, col);
                Ilk ilk = getAnts().getIlk(tile);

                if (!ilk.isPassable()) {
                    borders.put(tile, 9);
                    setBorderingTiles(tile, depth);
                }
            }
        }

        while (depth > 1 && hasRemainingTime()) {
            for (int cols = 0; cols < getAnts().getCols() && hasRemainingTime(); cols++) {
                for (int rows = 0; rows < getAnts().getRows() && hasRemainingTime(); rows++) {
                    Tile tile = new Tile(rows, cols);
                    if (ants.getIlk(tile).isPassable()) {
                        Integer foundDepth = borders.get(tile);
                        if (foundDepth != null && foundDepth == depth) {
                            setBorderingTiles(tile, depth - 1);
                        }
                    }
                }
            }

            --depth;
        }
    }

    protected void setBorderingTiles(Tile tile, int depth) {
        for (Aim aim : Aim.values()) {
            if (shouldSetBorder(tile, aim)) {
                borders.put(getAnts().getTile(tile, aim), depth);
            }
        }
    }

    protected void setTileDependingonBorderingTiles(Map<Tile, Integer> target, Tile tile, int depth) {
        if (shouldSetBorder2(tile)) {
            int distance = getWaterDistance(tile);
            if (distance < 5) {
                target.put(tile, 6 - distance);
            }
        }
    }

    /**
     * Check if the aimed border and the 3-bar after it are free
     *
     * @param tile
     * @param aim
     * @return
     */
    protected boolean shouldSetBorder(Tile tile, Aim aim) {
        boolean result = false;

        Ants ants = getAnts();
        Ilk ilk = ants.getIlk(tile, aim);

        if (ilk.isPassable()) {
            // if aim == NORTH check N-N, N-E, N-W
            Tile t2 = ants.getTile(tile, aim);
            Tile t3 = ants.getTile(t2, aim);
            Tile t4 = null, t5 = null;

            switch (aim) {
                case NORTH:
                    t4 = ants.getTile(t3, Aim.EAST);
                    t5 = ants.getTile(t3, Aim.WEST);
                    break;
                case EAST:
                    t4 = ants.getTile(t3, Aim.NORTH);
                    t5 = ants.getTile(t3, Aim.SOUTH);
                    break;
                case SOUTH:
                    t4 = ants.getTile(t3, Aim.EAST);
                    t5 = ants.getTile(t3, Aim.WEST);
                    break;
                case WEST:
                    t4 = ants.getTile(t3, Aim.NORTH);
                    t5 = ants.getTile(t3, Aim.SOUTH);
                    break;
            }

            result = ants.getIlk(t3).isPassable();
            result = result && ants.getIlk(t4).isPassable();
            result = result && ants.getIlk(t5).isPassable();

            result = result && borders.get(t2) == null;
            result = result && borders.get(t3) == null;
            result = result && borders.get(t4) == null;
            result = result && borders.get(t5) == null;
        }

        return result;
    }

    protected List<String> borderPatternsToSet = null;

    protected List<String> getBorderPatternsToSet() {
        if (borderPatternsToSet == null) {
            borderPatternsToSet = new LinkedList<String>();

            borderPatternsToSet.add("000000111");
            borderPatternsToSet.add("000100111");
            borderPatternsToSet.add("000001111");
            borderPatternsToSet.add("000101111");
            borderPatternsToSet.add("101101111");
            borderPatternsToSet.add("100101111");
            borderPatternsToSet.add("001101111");
            borderPatternsToSet.add("000100110");
            borderPatternsToSet.add("000001011");

            borderPatternsToSet.add("001001001");
            borderPatternsToSet.add("001001011");
            borderPatternsToSet.add("011001001");
            borderPatternsToSet.add("011001011");
            borderPatternsToSet.add("111001111");
            borderPatternsToSet.add("011001111");
            borderPatternsToSet.add("111001011");
            borderPatternsToSet.add("000001011");
            borderPatternsToSet.add("011001000");

            borderPatternsToSet.add("111000000");
            borderPatternsToSet.add("111001000");
            borderPatternsToSet.add("111100000");
            borderPatternsToSet.add("111101000");
            borderPatternsToSet.add("111101101");
            borderPatternsToSet.add("111101001");
            borderPatternsToSet.add("111101100");
            borderPatternsToSet.add("011001000");
            borderPatternsToSet.add("110100000");

            borderPatternsToSet.add("100100100");
            borderPatternsToSet.add("110100100");
            borderPatternsToSet.add("100100110");
            borderPatternsToSet.add("110100110");
            borderPatternsToSet.add("111100111");
            borderPatternsToSet.add("111100110");
            borderPatternsToSet.add("110100111");
            borderPatternsToSet.add("110100000");
            borderPatternsToSet.add("000100110");

            borderPatternsToSet.add("001001111");
            borderPatternsToSet.add("111001001");
            borderPatternsToSet.add("111100100");
            borderPatternsToSet.add("100100111");
        }

        return borderPatternsToSet;
    }

    protected String getBorderPattern(Tile tile) {
        StringBuilder result = new StringBuilder();

        result.append(getIsBorderPassiblePattern(tile, Aim.NORTH, Aim.WEST));
        result.append(getIsBorderPassiblePattern(tile, Aim.NORTH));
        result.append(getIsBorderPassiblePattern(tile, Aim.NORTH, Aim.EAST));
        result.append(getIsBorderPassiblePattern(tile, Aim.WEST));
        result.append(getIsBorderPassiblePattern(tile));
        result.append(getIsBorderPassiblePattern(tile, Aim.EAST));
        result.append(getIsBorderPassiblePattern(tile, Aim.SOUTH, Aim.WEST));
        result.append(getIsBorderPassiblePattern(tile, Aim.SOUTH));
        result.append(getIsBorderPassiblePattern(tile, Aim.SOUTH, Aim.EAST));

        return result.toString();
    }

    protected boolean getIsBorderPassible(Tile tile, Aim... aims) {
        for (Aim aim : aims) {
            tile = getAnts().getTile(tile, aim);
        }

        return borders.get(tile) == null;
    }

    protected String getIsBorderPassiblePattern(Tile tile, Aim... aims) {
        return getIsBorderPassible(tile, aims) ? "0" : "1";
    }

    protected boolean shouldSetBorder2(Tile tile) {
        boolean result = false;

        String pattern = getBorderPattern(tile);

        return getBorderPatternsToSet().contains(pattern);
    }

    protected int getWaterDistance(Tile tile) {
        int distance = 1;
        boolean found = false;

        while (!found && distance < 5) {
            for (Aim aim : Aim.values()) {
                Tile t1 = tile;
                Tile t2 = tile;
                Aim aim2 = null;
                switch (aim) {
                    case NORTH:
                        aim2 = Aim.EAST;
                        break;
                    case EAST:
                        aim2 = Aim.SOUTH;
                        break;
                    case SOUTH:
                        aim2 = Aim.WEST;
                        break;
                    case WEST:
                        aim2 = Aim.NORTH;
                        break;
                }
                for (int i = 0; i < distance; i++) {
                    t1 = getAnts().getTile(t1, aim);
                    t2 = getAnts().getTile(t2, aim);
                    t2 = getAnts().getTile(t2, aim2);
                }
                if (!getAnts().getIlk(t1).isPassable() || !getAnts().getIlk(t2).isPassable()) {
                    found = true;
                    break;
                }
            }

            distance++;
        }

        return distance;
    }

    protected void setViewDistance1() {
        int i = 2;

        while (i * i < getAnts().getViewRadius2()) {
            i++;
        }

        viewDistance1 = i;
    }

    protected void setAttackDistance1() {
        int i = 2;

        while (i * i < getAnts().getAttackRadius2()) {
            i++;
        }

        attackDistance1 = i;
    }

    protected boolean goodOrder(Tile myAnt, Aim aim) {
        Tile targetedTile = myAnt;
        Ilk ilk = ants.getIlk(myAnt);

        if (aim != null) {
            targetedTile = ants.getTile(myAnt, aim);
            ilk = ants.getIlk(myAnt, aim);
        }
        //debug("goodOrder: "+targetedTile+"="+ilk+" -> "+!targetedTiles.contains(targetedTile)+" - "+ilk.isPassable()+" - "+ilk.isUnoccupied()+" - "+!getAnts().getMyHills().contains(targetedTile));

        return (!targetedTiles.contains(targetedTile) && ilk.isPassable() && !getAnts().getMyHills().contains(targetedTile));
    }

    protected void executeOrder(Tile myAnt, Tile target) {
        Aim aim = null;

        if (!myAnt.equals(target)) {
            aim = getDirection(myAnt, target);
        }

        executeOrder(myAnt, aim);
    }

    protected void executeOrder(Tile myAnt, Aim aim) {
        Tile target = null;

        if (aim != null) {
            target = getAnts().getTile(myAnt, aim);
        }

        if (!targetedTiles.contains(target)) {
            if (aim != null) {
                ants.issueOrder(myAnt, aim);
            }
            targetedTiles.add(target);
            antsDone.add(myAnt);
            remainingAnts.remove(myAnt);
        }

        //adjust visited test
        /*
        Tile target = getAnts().getTile(myAnt, aim);
        switch(aim) {
            case NORTH:
                decVisited(getAnts().getTile(target, Aim.NORTH));
                break;
            case SOUTH:
                decVisited(getAnts().getTile(target, Aim.SOUTH));
                break;
            case EAST:
                decVisited(getAnts().getTile(target, Aim.EAST));
                break;
            case WEST:
                decVisited(getAnts().getTile(target, Aim.WEST));
                break;
        }*/
    }

    protected int getAntsDefendingHill(Tile hill) {
        int result = 0;

        for (Command command : commands) {
            if (command instanceof Defend) {
                Defend defend = (Defend) command;

                if (defend.getHill().equals(hill)) {
                    result++;
                }
            }
        }

        return result;
    }

    protected void incVisited(Tile tile) {
        Integer v = visited.get(tile);

        if (v == null) {
            visited.put(tile, 1);
        } else {
            visited.put(tile, v + 1);
        }
    }

    protected void decVisited(Tile tile) {
        Integer v = visited.get(tile);

        if (v == null) {
            visited.put(tile, -1);
        } else {
            visited.put(tile, v - 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
                      int attackRadius2, int spawnRadius2, int player_seed) {
        setAnts(new Ants(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2,
                spawnRadius2, player_seed));
    }

    /**
     * Returns game state information.
     *
     * @return game state information
     */
    public Ants getAnts() {
        return ants;
    }

    /**
     * Sets game state information.
     *
     * @param ants game state information to be set
     */
    protected void setAnts(Ants ants) {
        this.ants = ants;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeUpdate() {
        ants.setTurnStartTime(System.currentTimeMillis());
        ants.clearMyAnts();
        ants.clearEnemyAnts();
        ants.clearMyHills();
        ants.clearEnemyHills();
        ants.getFoodTiles().clear();
        ants.getOrders().clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addWater(int row, int col) {
        ants.update(Ilk.WATER, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnt(int row, int col, int owner) {
        ants.update(owner > 0 ? Ilk.ENEMY_ANT : Ilk.MY_ANT, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFood(int row, int col) {
        ants.update(Ilk.FOOD, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAnt(int row, int col, int owner) {
        ants.update(Ilk.DEAD, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHill(int row, int col, int owner) {
        ants.updateHills(owner, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterUpdate() {
    }

    protected Aim getRandomDirection() {
        Aim result = null;

        int i = r.nextInt(4);

        switch (i) {
            case 0:
                result = Aim.NORTH;
                break;
            case 1:
                result = Aim.EAST;
                break;
            case 2:
                result = Aim.SOUTH;
                break;
            case 3:
                result = Aim.WEST;
                break;
            default:
                throw new IllegalStateException("Impossible value!");

        }

        return result;
    }

    protected List<Tile> getCandidatesSortedByLeastVisited(Tile myAnt) {
        Ants ants = getAnts();

        Tile tileN = ants.getTile(myAnt, Aim.NORTH);
        Tile tileE = ants.getTile(myAnt, Aim.EAST);
        Tile tileS = ants.getTile(myAnt, Aim.SOUTH);
        Tile tileW = ants.getTile(myAnt, Aim.WEST);

        List<Tile> result = new LinkedList<Tile>();

        result.add(tileN);
        result.add(tileS);
        result.add(tileE);
        result.add(tileW);

        Collections.sort(result, new SortByMap(visited));

        return result;
    }

    /**/
    public static class SortByMap implements Comparator<Tile> {
        private Map<Tile, Integer> visited = null;

        public SortByMap(Map<Tile, Integer> visited) {
            this.visited = visited;
        }

        public int compare(Tile o1, Tile o2) {
            Integer c1 = visited.get(o1);
            Integer c2 = visited.get(o2);

            if (c1 != null && c2 != null) {
                return c1.compareTo(c2);
            } else if (c1 == null && c2 == null) {
                return (r.nextInt(5) - 2);
            } else if (c1 != null) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public static class SortBy2Maps implements Comparator<Tile> {
        private Map<Tile, Integer> map1 = null;
        private Map<Tile, Integer> map2 = null;
        private int mult1 = 1;
        private int mult2 = 1;

        public SortBy2Maps(Map<Tile, Integer> map1, Map<Tile, Integer> map2) {
            this.map1 = map1;
            this.map2 = map2;
        }

        public SortBy2Maps(Map<Tile, Integer> map1, Map<Tile, Integer> map2, int mult1, int mult2) {
            this.map1 = map1;
            this.map2 = map2;
            this.mult1 = mult1;
            this.mult2 = mult2;
        }

        public int compare(Tile o1, Tile o2) {
            int c1 = getFromMap(map1, o1) * mult1 + getFromMap(map2, o1) * mult2;
            int c2 = getFromMap(map1, o2) * mult1 + getFromMap(map2, o2) * mult2;

            return c1 - c2;
        }

        private int getFromMap(Map<Tile, Integer> map, Tile tile) {
            Integer result = map.get(tile);

            if (result == null) {
                result = 0;
            }
            return result;
        }
    }

    public static class SortBy4Maps implements Comparator<Tile> {
        private Map<Tile, Integer> map1 = null;
        private Map<Tile, Integer> map2 = null;
        private Map<Tile, Integer> map3 = null;
        private Map<Tile, Integer> map4 = null;
        private int mult1 = 1;
        private int mult2 = 1;
        private int mult3 = 1;
        private int mult4 = 1;

        public SortBy4Maps(Map<Tile, Integer> map1, Map<Tile, Integer> map2, Map<Tile, Integer> map3, Map<Tile, Integer> map4) {
            this.map1 = map1;
            this.map2 = map2;
            this.map3 = map3;
            this.map4 = map4;
        }

        public SortBy4Maps(Map<Tile, Integer> map1, Map<Tile, Integer> map2, Map<Tile, Integer> map3, Map<Tile, Integer> map4,
                           int mult1, int mult2, int mult3, int mult4) {
            this.map1 = map1;
            this.map2 = map2;
            this.map3 = map3;
            this.map4 = map4;
            this.mult1 = mult1;
            this.mult2 = mult2;
            this.mult3 = mult3;
            this.mult4 = mult4;
        }

        public int compare(Tile o1, Tile o2) {
            int c1 = getFromMap(map1, o1) * mult1 + getFromMap(map2, o1) * mult2 + getFromMap(map3, o1) * mult3 + getFromMap(map4, o1) * mult4;
            int c2 = getFromMap(map1, o2) * mult1 + getFromMap(map2, o2) * mult2 + getFromMap(map3, o2) * mult3 + getFromMap(map4, o2) * mult4;

            return c1 - c2;
        }

        private int getFromMap(Map<Tile, Integer> map, Tile tile) {
            Integer result = map.get(tile);

            if (result == null) {
                result = 0;
            }
            return result;
        }
    }

    public Map<Tile, Integer> addMaps(Map<Tile, Integer> map1, int multi1, Map<Tile, Integer> map2, int multi2) {
        Map<Tile, Integer> result = new HashMap<Tile, Integer>();

        for (Tile tile : map1.keySet()) {
            result.put(tile, map1.get(tile)*multi1);
        }

        for (Tile tile : map2.keySet()) {
            int total = 0;

            if (result.get(tile) != null) {
                total = result.get(tile);
            }

            total += map2.get(tile) * multi2;

            result.put(tile, total);
        }

        return result;
    }

    public static class SortByMaps implements Comparator<Tile> {
        private List<Map<Tile, Integer>> maps = new LinkedList<Map<Tile, Integer>>();
        private List<Integer> multi = new LinkedList<Integer>();

        public void addMap(Map<Tile, Integer> map, int mult) {
            maps.add(map);
            multi.add(mult);
        }

        public int compare(Tile o1, Tile o2) {
            int c1 = 0;
            int c2 = 0;
            int result = 0;

            for (int index = 0; index < maps.size(); index++) {
                c1 = getFromMap(maps.get(index), o1);
                c2 = getFromMap(maps.get(index), o2);

                result = c1-c2;

                if (result != 0) {
                    break;
                }
            }

            if (result == 0) {
                return (r.nextInt(3) - 1);
            }

            return result;
        }

        private int getFromMap(Map<Tile, Integer> map, Tile tile) {
            Integer result = map.get(tile);

            if (result == null) {
                result = 0;
            }
            return result;
        }

        public String showMap(Ants ants) {
            Map<Tile, Integer> totalMap = new HashMap<Tile, Integer>();

            for (int index = 0; index < maps.size(); index++) {
                Map<Tile, Integer> map = maps.get(index);
                int mult = multi.get(index);

                for (Tile key : map.keySet()) {
                    totalMap.put(key, map.get(key) * mult);
                }
            }

            StringBuilder result = new StringBuilder();

            for (int rows = 0; rows < ants.getRows(); rows++) {
                for (int cols = 0; cols < ants.getCols(); cols++) {
                    Tile tile = new Tile(rows, cols);

                    if (totalMap.get(tile) != null) {
                        int value = Math.abs(totalMap.get(tile));

                        if (value > 99) {
                            result.append("**");
                        } else {
                            if (value < 10) {
                                result.append("0");
                            }
                            result.append(value);
                        }
                    } else {
                        result.append("..");
                    }
                }
                result.append("\n");
            }

            return result.toString();
        }
    }

    protected List<Tile> getCandidates(Tile tile) {
        Ants ants = getAnts();

        List<Tile> result = new LinkedList<Tile>();

        for (Aim aim : Aim.values()) {
            result.add(ants.getTile(tile, aim));
        }

        return result;
    }

    protected List<Tile> getCandidatesSortedByBorderMap(Tile myAnt) {
        Ants ants = getAnts();

        Tile tileN = ants.getTile(myAnt, Aim.NORTH);
        Tile tileE = ants.getTile(myAnt, Aim.EAST);
        Tile tileS = ants.getTile(myAnt, Aim.SOUTH);
        Tile tileW = ants.getTile(myAnt, Aim.WEST);

        List<Tile> result = new LinkedList<Tile>();

        result.add(tileN);
        result.add(tileS);
        result.add(tileE);
        result.add(tileW);

        Collections.sort(result, new Comparator<Tile>() {
            public int compare(Tile o1, Tile o2) {
                Integer c1 = borders.get(o1);
                Integer c2 = borders.get(o2);

                if (c1 != null && c2 != null) {
                    return c1.compareTo(c2);
                } else if (c1 == null && c2 == null) {
                    return 0;
                } else if (c1 != null) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        return result;
    }

    protected void updateEnemyVisited(int minimalArmy) {
        for (Tile enAnt : ants.getEnemyAnts()) {
            incVisited(enAnt);
//            if (ants.getMyAnts().size() > minimalArmy) {
//                Integer v = visited.get(enAnt);
//
//                if (v != null) {
//                    visited.put(enAnt, v - 1);
//                }
//            }
        }
    }

    protected void updateVisited() {
        for (Tile myAnt : ants.getMyAnts()) {
            incVisited(myAnt);
        }
    }

    public final class TileDistance {
        public Tile tile;
        public int distance;

        public TileDistance(Tile tile, int distance) {
            this.tile = tile;
            this.distance = distance;
        }

    }

    protected List<Tile> findShortestPath(Tile t1, Tile t2) {
        HashMap<Tile, Integer> tiles = new HashMap<Tile, Integer>();

        tiles.put(t1, 0);

        //List<Tile> path = findShortestPath(new LinkedList<Tile>(), 0, t1, t2);
        List<Tile> path = findShortestPath(tiles, 1, t2);

        return path;
    }

    protected List<Tile> findShortestPath(List<Tile> visited, int currentDepth, Tile t1, Tile t2) {
        List<Tile> result = new LinkedList<Tile>();

        //System.err.println("CD: "+currentDepth);

        if (currentDepth > 15 || !hasRemainingTime()) {
            return result;
        }

        List<Tile> candidates = new LinkedList<Tile>();
        for (Aim aim : Aim.values()) {
            Tile candidate = getAnts().getTile(t1, aim);

            if (ants.getIlk(candidate).isPassable() && !visited.contains(candidate)) {
                visited.add(candidate);
                candidates.add(candidate);
            }
        }

        sortByDistance(t2, candidates);

        for (Tile candidate : candidates) {
            if (candidate.equals(t2)) {
                result.add(candidate);

                return result;
            }

            List<Tile> restOfPath = findShortestPath(visited, ++currentDepth, candidate, t2);

            // found?
            if (!restOfPath.isEmpty() && restOfPath.get(restOfPath.size() - 1).equals(t2)) {
                result.add(candidate);
                result.addAll(restOfPath);

                return result;
            }
        }

        return result;
    }

    protected List<Tile> findShortestPath(Map<Tile, Integer> visited, int currentDepth, Tile t2) {
        List<Tile> result = new LinkedList<Tile>();

        //System.err.println("CD: "+currentDepth);

        if (currentDepth > 15 || !hasRemainingTime()) {
            return result;
        }

        Map<Tile, Integer> nextRound = new HashMap<Tile, Integer>(visited);

        for (Tile tile : visited.keySet()) {
            for (Aim aim : Aim.values()) {
                Tile candidate = getAnts().getTile(tile, aim);

                if (candidate.equals(t2)) {
                    return getShortestPathFromMap(visited, t2);
                }

                if (ants.getIlk(candidate).isPassable() && visited.get(candidate) == null) {
                    nextRound.put(candidate, currentDepth);
                }
            }
        }

        return findShortestPath(nextRound, ++currentDepth, t2);
    }

    protected List<Tile> findShortestPathToOne(Map<Tile, Integer> visited, int currentDepth, List<Tile> t2) {
        return findShortestPathToOne(visited, currentDepth, 25, t2);
    }

    protected List<Tile> findShortestPathToOne(Map<Tile, Integer> visited, int currentDepth, int maxDepth, List<Tile> t2) {
        List<Tile> result = new LinkedList<Tile>();

        //System.err.println("CD: "+currentDepth);

        if (currentDepth > maxDepth || !hasRemainingTime()) {
            return result;
        }

        Map<Tile, Integer> nextRound = new HashMap<Tile, Integer>(visited);

        for (Tile tile : visited.keySet()) {
            for (Aim aim : Aim.values()) {
                Tile candidate = getAnts().getTile(tile, aim);

                if (t2.contains(candidate)) {
                    return getShortestPathFromMap(visited, candidate);
                }

                if (ants.getIlk(candidate).isPassable() && visited.get(candidate) == null) {
                    nextRound.put(candidate, currentDepth);
                }
            }
        }

        return findShortestPathToOne(nextRound, ++currentDepth, maxDepth, t2);
    }

    protected List<Tile> getShortestPathFromMap(Map<Tile, Integer> visited, Tile t2) {
        List<Tile> result = new LinkedList<Tile>();

        result.add(t2);
        int step = 999999;
//        if (debug) {
//            debug(showMap(visited));
//        }

        while (step != 0) {
            Tile target = null;
            int smallest = 999999;
            Tile current = result.get(0);

            for (Aim aim : Aim.values()) {
                Tile candidate = getAnts().getTile(current, aim);

                if (candidate != null && visited.get(candidate) != null && smallest > visited.get(candidate)) {
                    target = candidate;
                    smallest = visited.get(candidate);
                    step = smallest;
                }
            }

            result.add(0, target);
        }

        return result;
    }

    private String showMap(Map<Tile, Integer> visited) {
        StringBuilder result = new StringBuilder();

        for (int rows = 0; rows < getAnts().getRows(); rows++) {
            for (int cols = 0; cols < getAnts().getCols(); cols++) {
                Tile tile = new Tile(rows, cols);

                if (visited.get(tile) != null) {
                    int value = Math.abs(visited.get(tile));

                    while (value > 9) {
                        value -= 10;
                    }

                    result.append(value);
                } else {
                    result.append(".");
                }
            }
            result.append("\n");
        }

        return result.toString();
    }

    protected String showBorderMap() {
        StringBuilder result = new StringBuilder();

        for (int rows = 0; rows < getAnts().getRows(); rows++) {
            for (int cols = 0; cols < getAnts().getCols(); cols++) {
                Tile tile = new Tile(rows, cols);

                if (borders.get(tile) != null) {
                    int value = borders.get(tile);

                    while (value > 9) {
                        value -= 10;
                    }

                    result.append(value);
                } else {
                    result.append(".");
                }
            }
            result.append("\n");
        }

        return result.toString();
    }

    private String showVisitedMap() {
        StringBuilder result = new StringBuilder();

        for (int rows = 0; rows < getAnts().getRows(); rows++) {
            for (int cols = 0; cols < getAnts().getCols(); cols++) {
                Tile tile = new Tile(rows, cols);

                if (visited.get(tile) != null) {
                    int value = visited.get(tile);

                    if (value > 9) {
                        result.append("*");
                    } else {
                        result.append(value);
                    }
                } else {
                    result.append(" ");
                }
            }

            result.append("\n");
        }

        return result.toString();
    }

    protected void sortByDistance(final Tile target, List<Tile> candidates) {
        Collections.sort(candidates, new Comparator<Tile>() {
            public int compare(Tile o1, Tile o2) {
                Integer d1 = getAnts().getDistance(target, o1);
                Integer d2 = getAnts().getDistance(target, o2);

                return d1.compareTo(d2);
            }
        });
    }

    protected Tile findClosestAntWithinViewingDistance(List<Tile> candidates, Tile tile) {
        Tile result = null;
        List<Tile> withinDistance = findAntsWithinViewingDistance(candidates, tile);

        sortByDistance(tile, withinDistance);

        if (!withinDistance.isEmpty()) {
            result = withinDistance.get(0);
        }

        return result;
    }

    protected List<Tile> findAntsWithinViewingDistance(List<Tile> candidates, Tile tile) {
        List<Tile> result = new LinkedList<Tile>();

        for (Tile c : candidates) {
            if (ants.getDistance(tile, c) < ants.getViewRadius2()) {
                result.add(c);
            }
        }

        return result;
    }

    protected Aim getTileFurtherstAwayFromMyAnts(Tile tile) {
        Set<Tile> ants = getAnts().getMyAnts();
        final List<Tile> close = new LinkedList<Tile>();

        // remove target
        ants.remove(tile);

        for (Tile ant : ants) {
            if (getAnts().getDistance(tile, ant) < getAnts().getViewRadius2()) {
                close.add(ant);
            }
        }

        if (close.isEmpty()) {
            return null;
        }

        List<Tile> candidates = new LinkedList<Tile>();

        if (goodOrder(tile, Aim.NORTH)) {
            candidates.add(getAnts().getTile(tile, Aim.NORTH));
        }
        if (goodOrder(tile, Aim.EAST)) {
            candidates.add(getAnts().getTile(tile, Aim.EAST));
        }
        if (goodOrder(tile, Aim.SOUTH)) {
            candidates.add(getAnts().getTile(tile, Aim.SOUTH));
        }
        if (goodOrder(tile, Aim.WEST)) {
            candidates.add(getAnts().getTile(tile, Aim.WEST));
        }

        if (candidates.isEmpty()) {
            return null;
        } else {
            Collections.sort(candidates, new Comparator<Tile>() {
                public int compare(Tile o1, Tile o2) {
                    return getTotalDistanceFromOtherAnts(o2, close) - getTotalDistanceFromOtherAnts(o1, close);
                }
            });

            return getDirection(tile, candidates.get(0));
        }
    }

    protected int getTotalDistanceFromOtherAnts(Tile tile, List<Tile> ants) {
        int result = 0;

        for (Tile ant : ants) {
            result += getAnts().getDistance(ant, tile);
        }

        if (result == 0) {
            result = 999;
        }

        return result;
    }

    protected int getDistanceFromClosestAnts(Tile tile, List<Tile> ants) {
        int result = 999;

        for (Tile ant : ants) {
            result = Math.min(result, getAnts().getDistance(ant, tile));
        }

        return result;
    }

    protected Aim getDirection(Tile t1, Tile t2) {
        for (Aim aim : Aim.values()) {
            if (getAnts().getTile(t1, aim).equals(t2)) {
                return aim;
            }
        }

        return null;
    }

    protected void avoid(Tile ant) {
        avoid(avoidAnts, attackDistance1 + 2, ant);
    }

    protected void avoid(Map<Tile, Integer> map, int currentDepth, Tile ant) {
        Map<Tile, Integer> avoidMap = new HashMap<Tile, Integer>();

        avoidMap.put(ant, currentDepth);

        while (currentDepth > 1) {
            Map<Tile, Integer> tmpMap = new HashMap<Tile, Integer>();

            for (Tile tile : avoidMap.keySet()) {
                if (avoidMap.get(tile).equals(currentDepth)) {
                    for (Aim aim : Aim.values()) {
                        Tile candidate = getAnts().getTile(tile, aim);

                        if (!avoidMap.containsKey(candidate) && ants.getIlk(candidate).isPassable()) {
                            tmpMap.put(candidate, (currentDepth - 1));
                        }
                    }
                }
            }
            avoidMap.putAll(tmpMap);

            currentDepth--;
        }

        for (Tile t : avoidMap.keySet()) {
            Integer i = map.get(t);
            if (i == null) {
                i = 0;
            }
            i = i + avoidMap.get(t);

            map.put(t, i);
        }
    }

    protected void attack(Tile ant) {
        attack(ant, (viewDistance1 * 3) / 2);
    }

    protected void attack(Tile ant, int currentDepth) {
        Map<Tile, Integer> attackMap = new HashMap<Tile, Integer>();

        attackMap.put(ant, -currentDepth);

        while (currentDepth > 1) {
            Map<Tile, Integer> tmpMap = new HashMap<Tile, Integer>();

            for (Tile tile : attackMap.keySet()) {
                if (attackMap.get(tile).equals(-currentDepth)) {
                    for (Aim aim : Aim.values()) {
                        Tile candidate = getAnts().getTile(tile, aim);

                        if (!attackMap.containsKey(candidate) && ants.getIlk(candidate).isPassable()) {
                            tmpMap.put(candidate, -(currentDepth - 1));
                        }
                    }
                }
            }

            attackMap.putAll(tmpMap);

            currentDepth--;
        }

        for (Tile t : attackMap.keySet()) {
            Integer i = attackAnts.get(t);
            if (i == null) {
                i = attackMap.get(t);
            }
//            else {
//                i = Math.max(i, attackMap.get(t));
//            }

            attackAnts.put(t, i);
        }
    }

    protected void moveToFood(Tile ant) {
        Map<Tile, Integer> attackMap = new HashMap<Tile, Integer>();

        int delta = 0;
        int currentDepth = 40;

        attackMap.put(ant, -currentDepth);

        while (currentDepth > 1) {
            Map<Tile, Integer> tmpMap = new HashMap<Tile, Integer>();

            for (Tile tile : attackMap.keySet()) {
                if (attackMap.get(tile).equals(-currentDepth)) {
                    for (Aim aim : Aim.values()) {
                        Tile candidate = getAnts().getTile(tile, aim);

                        if (!attackMap.containsKey(candidate) && ants.getIlk(candidate).isPassable()) {
                            tmpMap.put(candidate, -(currentDepth - 1));

                            if (ants.getMyAnts().contains(candidate)) {
                                // done
                                delta = currentDepth - 2;
                                currentDepth = 0;
                            }
                        }
                    }
                }
            }

            attackMap.putAll(tmpMap);

            currentDepth--;
        }

        Map<Tile, Integer> finalMap = new HashMap<Tile, Integer>();
        for (Tile tile : attackMap.keySet()) {
            finalMap.put(tile, attackMap.get(tile) + delta);
        }

//        foodTiles.putAll(finalMap);

        for (Tile t : finalMap.keySet()) {
            Integer i = foodTiles.get(t);
            if (i == null || i == 0) {
                foodTiles.put(t, finalMap.get(t));
            } else {
                foodTiles.put(t, i + finalMap.get(t));
            }
        }
    }

    protected Map<Tile, Integer> mapDistance(Tile ant, int distance) {
        Map<Tile, Integer> result = new HashMap<Tile, Integer>();
        int currentDistance = 0;

        for (Aim aim : Aim.values()) {
            Tile target = ants.getTile(ant, aim);

            if (ants.getIlk(target).isPassable()) {
                result.put(target, currentDistance);
            }
        }

        //result.put(ant, currentDistance);

        while (currentDistance < distance) {
            Map<Tile, Integer> tmpMap = new HashMap<Tile, Integer>();

            for (Tile tile : result.keySet()) {
                if (result.get(tile).equals(currentDistance)) {
                    for (Aim aim : Aim.values()) {
                        Tile candidate = getAnts().getTile(tile, aim);

                        if (!result.containsKey(candidate) && ants.getIlk(candidate).isPassable()) {
                            tmpMap.put(candidate, (currentDistance + 1));
                        }
                    }
                }
            }
            result.putAll(tmpMap);

            currentDistance++;
        }

        return result;
    }

}
