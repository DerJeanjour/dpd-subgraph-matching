package de.haw.misc.pipe;

import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor( staticName = "empty" )
public class PipeContext {

    public static String PROCESS_NAME = "process_name";

    public static String PIPE_BENCHMARKS = "pipe_benchmarks";

    public static String CPG_DATASET_KEY = "dataset";

    public static String CPG_DEPTH_KEY = "depth";

    public static String CPG_MIN_DEPTH_KEY = "minDepth";

    public static String CPG_REPOSITORY_PURGE_KEY = "purge_repo";

    public static String CPG_DESIGN_PATTERNS_EXISTS = "cpg_dp_exists";

    public static String CPG_DESIGN_PATTERNS = "cpg_dp";

    /**
     * Node value has to be double
     */
    public static String NODE_SIZE_ATTR = "nodeSizeAttr";

    /**
     * Node value has to be double
     */
    public static String NODE_SIZE_SCALE = "nodeSizeScale";

    public static String RECORD_PATHS = "ssspRecords";

    public static String TOTAL_PROCESSING_TIME = "totalTime";

    public static String PROCESS_COUNT = "processCount";

    private final Map<String, Object> ctx = new HashMap<>();

    public void set( final String key, final Object value ) {
        this.ctx.put( key, value );
    }

    public String get( final String key ) {
        return this.get( key, null, String.class );
    }

    public <T> T get( final String key, final T defaultValue, final Class<T> clazz ) {
        return this.get( key, clazz ).orElse( defaultValue );
    }

    public <T> Optional<T> get( final String key, final Class<T> clazz ) {
        T value = null;
        try {
            value = clazz.cast( this.ctx.getOrDefault( key, null ) );
        } catch ( Exception e ) {
            // eat it
        }
        return value != null ? Optional.of( value ) : Optional.empty();
    }

}
