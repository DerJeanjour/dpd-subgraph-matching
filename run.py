import argparse
import subprocess
import time

DOCKER_QUOTE = '\"'

def process( args ):
    print( args )

    generation_start = time.time()
    generation_cmd = (
        f'{f"docker exec graph-generation sh -c {DOCKER_QUOTE}" if not args.local else ""}java -jar generation/build/libs/generation-develop.jar '
        f'--source="{args.source}" --name="{args.name}" --language={args.language} --depth={args.depth} '
        f'--neo4j-host={"localhost:" if args.local else args.neo4j_host}{f"{DOCKER_QUOTE}" if not args.local else ""}')
    print( generation_cmd )
    p_generation = subprocess.Popen( generation_cmd, shell=True )
    p_generation.wait()
    generation_time = time.time() - generation_start

    matching_start = time.time()
    matching_cmd = (
        f'{f"docker exec graph-matching sh -c {DOCKER_QUOTE}python matching/main.py" if not args.local else "matching/venv/bin/python matching/main.py"} --name="{args.name}" '
        f'--pattern_dataset={args.patterns} --model={args.model} {"--directed" if args.directed else ""} {"--use_cache" if args.use_cache else ""} '
        f'--neo4j_host={"localhost:" if args.local else args.neo4j_host}{f"{DOCKER_QUOTE}" if not args.local else ""}')
    print( matching_cmd )
    p_matching = subprocess.Popen( matching_cmd, shell=True )
    p_matching.wait()
    matching_time = time.time() - matching_start

    print( f"TOTAL TIME: {generation_time + matching_time:.2f}s "
           f"[generation={generation_time:.2f}s] [matching={matching_time:.2f}s]" )


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    # general
    parser.add_argument( "--name",
                         help="Name of the project.",
                         type=str )
    parser.add_argument( "--neo4j_host",
                         help="Name of the project.",
                         type=str, default="neo4j:" )
    parser.add_argument( "--local",
                         help="If the services should run locally.",
                         action="store_true", default=False )

    # generation
    parser.add_argument( "--source",
                         help="Path to the source code.",
                         type=str )
    parser.add_argument( "--language",
                         help="Language of the source code.",
                         type=str )
    parser.add_argument( "--depth",
                         help="Depth of the source code translation.",
                         type=int, default=10 )

    # matching
    parser.add_argument( "--use_cache",
                         help="Used cache generated data.",
                         action="store_true", default=False )
    parser.add_argument( "--model",
                         help="The training model.",
                         type=str, default="CPG_augm_large" )
    parser.add_argument( "--patterns",
                         help="The pattern dataset to match with.",
                         type=str, default="dpdf" )
    parser.add_argument( "--directed",
                         help="If the matching should be performed on a directed graph.",
                         action="store_true", default=True )

    args = parser.parse_args()
    process( args )
