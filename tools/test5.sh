#!/usr/bin/env sh
./playgame.py -So --player_seed 41 --end_wait=0.25 --verbose --log_dir game_logs --turns 1500 --map_file maps/random_walk/random_walk_04p_02.map "$@" \
        "./mybot.sh" \
        "./mybotd3.sh" \
        "./mybotd4.sh" \
        "./mybotd6.sh" | java -jar visualizer.jar