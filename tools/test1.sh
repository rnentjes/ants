#!/usr/bin/env sh
./playgame.py -So --player_seed 41 --end_wait=0.25 --verbose --log_dir game_logs --turns 2500 --map_file maps/random_walk/random_walk_02p_01.map "$@" \
        "./mybot.sh" \
	"./mybot.sh" | java -jar visualizer.jar
