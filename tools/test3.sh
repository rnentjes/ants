#!/usr/bin/env sh
./playgame.py -So --player_seed 41 --end_wait=0.25 --verbose --log_dir game_logs --turns 2500 --map_file maps/maze/maze_08p_01.map "$@" \
        "./mybot.sh" \
        "./stupidbot.sh" \
        "./stupidbot.sh" \
        "./stupidbot.sh" \
        "./stupidbot.sh" \
        "./stupidbot.sh" \
        "./stupidbot.sh" \
        "./stupidbot.sh" | java -jar visualizer.jar
