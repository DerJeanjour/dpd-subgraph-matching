package de.haw.processing.module;

import de.haw.dataset.model.DatasetDesignPatterns;
import de.haw.dataset.model.DesignPattern;
import de.haw.misc.pipe.PipeContext;
import de.haw.misc.pipe.PipeModule;
import de.haw.misc.utils.NameUtils;
import de.haw.processing.GraphService;
import de.haw.translation.CpgConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.*;

@Slf4j
@RequiredArgsConstructor( staticName = "instance" )
public class MarkPatternsModule<Target> extends PipeModule<Graph, Graph, Target> {

    private final static String TOTAL_STAT = "total";

    private final GraphService GS = GraphService.instance();

    private final boolean ONLY_MAJOR_PATTERN_ROLES = false;

    @Override
    protected Graph processImpl( final Graph graph, final PipeContext ctx ) {

        if ( !ctx.get( PipeContext.CPG_DESIGN_PATTERNS_EXISTS, false, Boolean.class ) ) {
            log.info( "Can't mark non existing design pattern in cpg." );
            return graph;
        }

        final DatasetDesignPatterns dps = ctx.get( PipeContext.CPG_DESIGN_PATTERNS, null, DatasetDesignPatterns.class );
        final Map<String, Integer> stats = new HashMap<>();

        graph.nodes().forEach( node -> {

            if ( !this.GS.hasLabel( node, CpgConst.NODE_LABEL_DECLARATION_RECORD ) ) {
                return;
            }

            final String className = this.getClassName( node );
            final List<DesignPattern> patterns = this.getPatterns( className, dps );
            patterns.forEach( dp -> {
                this.GS.addLabel( node, dp.getType().name() );
                node.setAttribute( CpgConst.NODE_ATTR_PATTERN_ID, dp.getId() );
                this.incrementStat( stats, this.formatStatKey( dp ) );
                this.incrementStat( stats, TOTAL_STAT );
            } );

        } );

        final Map<String, Pair<Integer, Integer>> resultStats = this.getStatResults( dps, stats );
        log.info( "Result stats of marking design patterns: {}", resultStats );

        return graph;
    }

    private String getClassName( final Node node ) {
        if ( this.GS.hasAttr( node, CpgConst.NODE_ATTR_NAME_FULL ) ) {
            return node.getAttribute( CpgConst.NODE_ATTR_NAME_FULL, String.class );
        }
        return null;
    }

    private List<DesignPattern> getPatterns( final String scopedClassname, final DatasetDesignPatterns datasetDps ) {
        final List<DesignPattern> patterns = new ArrayList<>();
        if ( StringUtils.isBlank( scopedClassname ) || !NameUtils.isScopedClassName( scopedClassname, "." ) ) {
            return patterns;
        }
        datasetDps.getPatterns().values().forEach( dps -> dps.forEach( dp -> {
            if ( NameUtils.extractClassName( scopedClassname ).equals( dp.getClassName() ) ) {
                patterns.add( dp );
            }
        } ) );
        if( ONLY_MAJOR_PATTERN_ROLES ) {
            return patterns.stream().filter( DesignPattern::isMajorRole ).toList();
        }
        return patterns;
    }

    private Map<String, Pair<Integer, Integer>> getStatResults(
            final DatasetDesignPatterns dps, final Map<String, Integer> stats ) {
        final Map<String, Integer> groundTruthStats = this.getGroundTruthStats( dps );
        final Map<String, Pair<Integer, Integer>> resultStats = new TreeMap<>();
        groundTruthStats.keySet().forEach( key -> {
            final int groundTruthValue = groundTruthStats.get( key );
            final int actualValue = stats.getOrDefault( key, 0 );
            resultStats.put( key, Pair.create( groundTruthValue, actualValue ) );
        } );
        return resultStats;
    }

    private Map<String, Integer> getGroundTruthStats( final DatasetDesignPatterns datasetDps ) {
        final Map<String, Integer> gt = new HashMap<>();
        datasetDps.getPatterns().values().forEach( dps -> dps.forEach( dp -> {
            this.incrementStat( gt, this.formatStatKey( dp ) );
            this.incrementStat( gt, TOTAL_STAT );
        } ) );
        return gt;

    }

    private void incrementStat( final Map<String, Integer> stats, final String key ) {
        this.incrementStat( stats, key, 1 );
    }

    private void incrementStat( final Map<String, Integer> stats, final String key, final int inc ) {
        if ( !stats.containsKey( key ) ) {
            this.setStat( stats, key, inc );
            return;
        }
        this.setStat( stats, key, stats.get( key ) + inc );
    }

    private void setStat( final Map<String, Integer> stats, final String key, final int value ) {
        stats.put( key, value );
    }

    private String formatStatKey( final DesignPattern pattern ) {
        return pattern.getType().name();
    }

}
