#!/usr/bin/env sh
./playgame.py -So --player_seed 41 --end_wait=0.25 --verbose --log_dir game_logs --turns 2500 --map_file maps/maze/maze_02p_02.map "$@" \
        "./mybot.sh" \
	"python sample_bots/python/HunterBot.py" | java -jar visualizer.jar
