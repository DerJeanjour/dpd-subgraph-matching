import argparse
import subprocess
import time

start = time.time()
print( "hello" )
end = time.time()
print( end - start )


def process( args ):
    print( args )

    generation_start = time.time()
    generation_cmd = (f'docker exec graph-generation sh -c "java -jar generation/build/libs/generation-develop.jar '
                      f'--source={args.source} --name={args.name} --language={args.language} --depth={args.depth} '
                      f'--neo4j-host={args.neo4j_host}"')
    p_generation = subprocess.Popen( generation_cmd, shell=True )
    p_generation.wait()
    generation_time = time.time() - generation_start

    matching_start = time.time()
    matching_cmd = (f'docker exec graph-matching sh -c "python matching/main.py --name={args.name} '
                    f'--pattern_dataset={args.patterns} --model={args.model} {"--directed" if args.directed else ""} '
                    f'--neo4j_host={args.neo4j_host}"')
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
