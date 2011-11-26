#!/usr/bin/env sh
./playgame.py --player_seed 42 --end_wait=0.25 --verbose --log_dir game_logs --turns 1000 --map_file maps/random_walk/random_walk_08p_02.map "$@" \
        "./mybot.sh" \
    	"./mybotv1.sh" \
    	"./mybotv2.sh" \
    	"./mybotv3.sh" \
    	"./mybotv4.sh" \
    	"./mybotv1.sh" \
    	"./mybotv1.sh" \
    	"./mybotv1.sh"
