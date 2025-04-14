import networkx as nx

import matching.glema.common.utils.graph_utils as graph_utils


class TestGraphUtils:

    def test_is_iso_subgraph_identical_graphs( self ):
        # Create two identical graphs
        G1 = nx.Graph()
        G1.add_node( 1, anchor=0, label="A" )
        G1.add_node( 2, anchor=0, label="B" )
        G1.add_edge( 1, 2, label="edge" )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=0, label="A" )
        G2.add_node( 2, anchor=0, label="B" )
        G2.add_edge( 1, 2, label="edge" )

        assert graph_utils.is_iso_subgraph( G1, G2 )

    def test_is_iso_subgraph_different_node_labels( self ):
        # Create two graphs with different node labels
        G1 = nx.Graph()
        G1.add_node( 1, anchor=0, label="A" )
        G1.add_node( 2, anchor=0, label="B" )
        G1.add_edge( 1, 2, label="edge" )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=0, label="A" )
        G2.add_node( 2, anchor=0, label="C" )  # Different label
        G2.add_edge( 1, 2, label="edge" )

        assert not graph_utils.is_iso_subgraph( G1, G2 )

    def test_is_iso_subgraph_different_edge_labels( self ):
        # Create two graphs with different edge labels
        G1 = nx.Graph()
        G1.add_node( 1, anchor=0, label="A" )
        G1.add_node( 2, anchor=0, label="B" )
        G1.add_edge( 1, 2, label="edge1" )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=0, label="A" )
        G2.add_node( 2, anchor=0, label="B" )
        G2.add_edge( 1, 2, label="edge2" )  # Different label

        assert not graph_utils.is_iso_subgraph( G1, G2 )

    def test_is_iso_subgraph_different_anchors( self ):
        # Create two graphs with different anchor values
        G1 = nx.Graph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, anchor=0, label="B" )
        G1.add_edge( 1, 2, label="edge" )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=0, label="A" )  # Different anchor
        G2.add_node( 2, anchor=0, label="B" )
        G2.add_edge( 1, 2, label="edge" )

        assert not graph_utils.is_iso_subgraph( G1, G2 )

    def test_is_iso_subgraph_with_subgraph( self ):
        # Test with a proper subgraph
        G = nx.Graph()
        G.add_node( 1, anchor=0, label="A" )
        G.add_node( 2, anchor=0, label="B" )
        G.add_node( 3, anchor=0, label="C" )
        G.add_edge( 1, 2, label="edge1" )
        G.add_edge( 2, 3, label="edge2" )

        subG = nx.Graph()
        subG.add_node( 1, anchor=0, label="A" )
        subG.add_node( 2, anchor=0, label="B" )
        subG.add_edge( 1, 2, label="edge1" )

        assert graph_utils.is_iso_subgraph( G, subG )

    def test_is_iso_subgraph_directed_graphs( self ):
        # Test with directed graphs
        G1 = nx.DiGraph()
        G1.add_node( 1, anchor=0, label="A" )
        G1.add_node( 2, anchor=0, label="B" )
        G1.add_edge( 1, 2, label="edge" )

        G2 = nx.DiGraph()
        G2.add_node( 1, anchor=0, label="A" )
        G2.add_node( 2, anchor=0, label="B" )
        G2.add_edge( 1, 2, label="edge" )

        assert graph_utils.is_iso_subgraph( G1, G2 )

    def test_generate_graph( self ):
        G = graph_utils.generate_graph( 10, False )
        assert len( G.nodes ) == 10
        assert not G.is_directed()

        G_directed = graph_utils.generate_graph( 10, True )
        assert len( G_directed.nodes ) == 10
        assert G_directed.is_directed()

    def test_subgraph( self ):
        G = nx.Graph()
        for i in range( 10 ):
            G.add_node( i )
        for i in range( 9 ):
            G.add_edge( i, i + 1 )

        sub_G = graph_utils.subgraph( G, 5, 2 )
        assert 5 in sub_G.nodes
        assert len( sub_G.nodes ) <= 5

    def test_get_anchor( self ):
        G = nx.Graph()
        G.add_node( 1, label="A" )
        G.add_node( 2, label="B", anchor=1 )
        G.add_node( 3, label="C" )

        assert graph_utils.get_anchor( G ) == 2

        G_no_anchor = nx.Graph()
        G_no_anchor.add_node( 1, label="A" )
        G_no_anchor.add_node( 2, label="B" )

        assert graph_utils.get_anchor( G_no_anchor ) == -1

    def test_subgraph_from_anchor_of_size( self ):
        G = nx.Graph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_node( 4, label="D" )
        G.add_edge( 1, 2 )
        G.add_edge( 2, 3 )
        G.add_edge( 3, 4 )

        sub_G = graph_utils.subgraph_from_anchor_of_size( G, 3 )
        assert len( sub_G.nodes ) == 3
        assert 1 in sub_G.nodes

    def test_top_pr_ranked_node( self ):
        G = nx.DiGraph()
        G.add_node( 1 )
        G.add_node( 2 )
        G.add_node( 3 )
        G.add_edge( 1, 2 )
        G.add_edge( 1, 3 )
        G.add_edge( 2, 3 )

        assert graph_utils.top_pr_ranked_node( G ) == 3

    def test_is_connected( self ):
        G_connected = nx.Graph()
        G_connected.add_node( 1 )
        G_connected.add_node( 2 )
        G_connected.add_edge( 1, 2 )

        G_disconnected = nx.Graph()
        G_disconnected.add_node( 1 )
        G_disconnected.add_node( 2 )

        assert graph_utils.is_connected( G_connected )
        assert not graph_utils.is_connected( G_disconnected )

        # Test directed graphs
        G_weakly = nx.DiGraph()
        G_weakly.add_node( 1 )
        G_weakly.add_node( 2 )
        G_weakly.add_edge( 1, 2 )

        assert graph_utils.is_connected( G_weakly, weak=True )
        assert not graph_utils.is_connected( G_weakly, weak=False )

    def test_max_spanning_radius( self ):
        G = nx.Graph()
        G.add_node( 1 )
        G.add_node( 2 )
        G.add_node( 3 )
        G.add_node( 4 )
        G.add_edge( 1, 2 )
        G.add_edge( 2, 3 )
        G.add_edge( 3, 4 )

        assert graph_utils.max_spanning_radius( G, 1 ) == 3
        assert graph_utils.max_spanning_radius( G, 2 ) == 2
        assert graph_utils.max_spanning_radius( G, 4 ) == 3

    def test_get_all_norm_paths_undirected( self ):
        # Create a simple undirected graph with an anchor
        G = nx.Graph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_edge( 1, 2 )
        G.add_edge( 1, 3 )

        paths = graph_utils.get_all_norm_paths( G )

        # Should have two paths: [1,2] and [1,3]
        assert len( paths ) == 2

        # Check paths content
        path_strings = [ p[ 1 ] for p in paths ]
        assert "AB" in path_strings
        assert "AC" in path_strings

        # Check node ids in paths
        path_ids = [ p[ 0 ] for p in paths ]
        assert [ 1, 2 ] in path_ids or [ 1, 2 ] in path_ids
        assert [ 1, 3 ] in path_ids or [ 1, 3 ] in path_ids

    def test_get_all_norm_paths_directed( self ):
        # Create a directed graph
        G = nx.DiGraph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_edge( 1, 2 )  # 1 -> 2
        G.add_edge( 3, 1 )  # 3 -> 1

        paths = graph_utils.get_all_norm_paths( G )

        # Should have two paths: [1,2] and [1,3]
        assert len( paths ) == 2

        # Check paths content with prefix indicators
        path_strings = [ p[ 1 ] for p in paths ]
        assert "A>B" in path_strings  # outgoing edge from 1 to 2
        assert "A<C" in path_strings  # incoming edge from 3 to 1

    def test_get_all_norm_paths_tree( self ):
        # Create a tree-like graph
        G = nx.Graph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_node( 4, label="D" )
        G.add_node( 5, label="E" )
        G.add_edge( 1, 2 )
        G.add_edge( 1, 3 )
        G.add_edge( 2, 4 )
        G.add_edge( 2, 5 )

        paths = graph_utils.get_all_norm_paths( G )

        # Should have 4 paths from root to leaves
        assert len( paths ) == 3

        # Check path strings
        path_strings = [ p[ 1 ] for p in paths ]
        assert "ABD" in path_strings
        assert "ABE" in path_strings
        assert "AC" in path_strings

        # Check that all paths start from the anchor
        for path_ids, _ in paths:
            assert path_ids[ 0 ] == 1

    def test_normalize_graph_undirected( self ):
        # Create a simple undirected graph with an anchor
        G = nx.Graph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_edge( 1, 2 )
        G.add_edge( 1, 3 )

        G_norm, furthest_distance = graph_utils.normalize_graph( G )

        # Check graph properties
        assert not G_norm.is_directed()
        assert len( G_norm.nodes ) == 3
        assert furthest_distance == 1

        # Check node attributes
        for n in G_norm.nodes:
            assert "presence" in G_norm.nodes[ n ]
            assert "distance" in G_norm.nodes[ n ]

        # Check structure
        anchor_node = None
        for n, data in G_norm.nodes( data=True ):
            if data.get( "anchor" ) == 1:
                anchor_node = n
                break

        assert anchor_node is not None
        assert len( list( G_norm.neighbors( anchor_node ) ) ) == 2

    def test_normalize_graph_with_max_distance( self ):
        # Create a chain graph
        G = nx.Graph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_node( 4, label="D" )
        G.add_node( 5, label="E" )
        G.add_edge( 1, 2 )
        G.add_edge( 2, 3 )
        G.add_edge( 3, 4 )
        G.add_edge( 4, 5 )

        # With max_distance=2, only nodes up to distance 2 should be included
        G_norm, furthest_distance = graph_utils.normalize_graph( G, max_distance=2 )

        assert len( G_norm.nodes ) <= 3  # A, B, C only
        assert furthest_distance <= 2

    def test_normalize_graph_with_cycles( self ):
        # Create a graph with a cycle
        G = nx.Graph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_edge( 1, 2 )
        G.add_edge( 2, 3 )
        G.add_edge( 3, 1 )  # Creates a cycle

        G_norm, _ = graph_utils.normalize_graph( G )

        # Normalized graph should be a tree
        # Each unique path from anchor gets its own node
        assert len( G_norm.nodes ) >= 3

        # No cycles in normalized graph
        assert nx.is_tree( G_norm )

    def test_normalize_di_graph_simple( self ):
        # Create a simple directed graph with an anchor
        G = nx.DiGraph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_edge( 1, 2 )  # 1 -> 2
        G.add_edge( 3, 1 )  # 3 -> 1

        G_norm, furthest_distance = graph_utils.normalize_di_graph( G )

        # Check graph properties
        assert G_norm.is_directed()
        assert len( G_norm.nodes ) == 3
        assert furthest_distance == 1

        # Check node attributes
        for n in G_norm.nodes:
            assert "presence" in G_norm.nodes[ n ]
            assert "distance" in G_norm.nodes[ n ]

        # Verify edge directions
        anchor_node = None
        for n, data in G_norm.nodes( data=True ):
            if data.get( "anchor" ) == 1:
                anchor_node = n
                break

        assert anchor_node is not None
        assert len( list( G_norm.successors( anchor_node ) ) ) == 1  # One outgoing edge
        assert len( list( G_norm.predecessors( anchor_node ) ) ) == 1  # One incoming edge

    def test_normalize_di_graph_with_max_distance( self ):
        # Create a chain directed graph
        G = nx.DiGraph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_node( 4, label="D" )
        G.add_edge( 1, 2 )
        G.add_edge( 2, 3 )
        G.add_edge( 3, 4 )

        # With max_distance=2, only nodes up to distance 1 should be included
        G_norm, furthest_distance = graph_utils.normalize_di_graph( G, max_distance=2 )

        assert len( G_norm.nodes ) <= 3  # Should include A, B, C nodes
        assert furthest_distance <= 1

    def test_normalize_di_graph_with_cycles( self ):
        # Create a directed graph with a cycle
        G = nx.DiGraph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_node( 3, label="C" )
        G.add_edge( 1, 2 )
        G.add_edge( 2, 3 )
        G.add_edge( 3, 1 )  # Creates a cycle

        G_norm, _ = graph_utils.normalize_di_graph( G )

        # Check for properties of normalized directed graph with cycles
        assert G_norm.is_directed()

        # Should create distinct paths for each direction
        assert len( G_norm.nodes ) >= 3

    def test_normalize_di_graph_bidirectional_edges( self ):
        # Create a directed graph with bidirectional edges
        G = nx.DiGraph()
        G.add_node( 1, anchor=1, label="A" )
        G.add_node( 2, label="B" )
        G.add_edge( 1, 2 )  # 1 -> 2
        G.add_edge( 2, 1 )  # 2 -> 1

        G_norm, _ = graph_utils.normalize_di_graph( G )

        # Should have separate nodes for each direction
        assert len( G_norm.nodes ) >= 3  # At least anchor + two directions

        # Check that both directions are preserved
        anchor_node = None
        for n, data in G_norm.nodes( data=True ):
            if data.get( "anchor" ) == 1:
                anchor_node = n
                break

        assert anchor_node is not None
        assert len( list( G_norm.successors( anchor_node ) ) ) >= 1
        assert len( list( G_norm.predecessors( anchor_node ) ) ) >= 1

    def test_norm_graphs_are_equal_identical_graphs( self ):
        # Create two identical graphs
        G1 = nx.Graph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_node( 3, label="C" )
        G1.add_edge( 1, 2 )
        G1.add_edge( 1, 3 )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 2, label="B" )
        G2.add_node( 3, label="C" )
        G2.add_edge( 1, 2 )
        G2.add_edge( 1, 3 )

        assert graph_utils.norm_graphs_are_equal( G1, G2 )

    def test_norm_graphs_are_equal_different_node_ids( self ):
        # Create graphs with same structure but different node IDs
        G1 = nx.Graph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_node( 3, label="C" )
        G1.add_edge( 1, 2 )
        G1.add_edge( 1, 3 )

        G2 = nx.Graph()
        G2.add_node( 10, anchor=1, label="A" )  # Different node ID
        G2.add_node( 20, label="B" )  # Different node ID
        G2.add_node( 30, label="C" )  # Different node ID
        G2.add_edge( 10, 20 )
        G2.add_edge( 10, 30 )

        assert graph_utils.norm_graphs_are_equal( G1, G2 )

    def test_norm_graphs_are_equal_different_structure( self ):
        # Create graphs with different structures
        G1 = nx.Graph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_node( 3, label="C" )
        G1.add_edge( 1, 2 )
        G1.add_edge( 1, 3 )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 2, label="B" )
        G2.add_node( 3, label="C" )
        G2.add_edge( 1, 2 )
        G2.add_edge( 2, 3 )  # Different edge connection

        assert not graph_utils.norm_graphs_are_equal( G1, G2 )

    def test_norm_graphs_are_equal_different_labels( self ):
        # Create graphs with different node labels
        G1 = nx.Graph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_node( 3, label="C" )
        G1.add_edge( 1, 2 )
        G1.add_edge( 1, 3 )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 2, label="D" )  # Different label
        G2.add_node( 3, label="C" )
        G2.add_edge( 1, 2 )
        G2.add_edge( 1, 3 )

        assert not graph_utils.norm_graphs_are_equal( G1, G2 )

    def test_norm_graphs_are_equal_directed_graphs( self ):
        # Create two identical directed graphs
        G1 = nx.DiGraph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_node( 3, label="C" )
        G1.add_edge( 1, 2 )  # A -> B
        G1.add_edge( 1, 3 )  # A -> C

        G2 = nx.DiGraph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 2, label="B" )
        G2.add_node( 3, label="C" )
        G2.add_edge( 1, 2 )  # A -> B
        G2.add_edge( 1, 3 )  # A -> C

        assert graph_utils.norm_graphs_are_equal( G1, G2 )

    def test_norm_graphs_are_equal_reverse_edges( self ):
        # Create directed graphs with edges in opposite directions
        G1 = nx.DiGraph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_edge( 1, 2 )  # A -> B

        G2 = nx.DiGraph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 2, label="B" )
        G2.add_edge( 2, 1 )  # B -> A (reversed direction)

        assert not graph_utils.norm_graphs_are_equal( G1, G2 )

    def test_combine_graph_basic( self ):
        # Create two simple graphs
        source = nx.Graph()
        source.add_node( 1, label="A" )
        source.add_node( 2, label="B" )
        source.add_edge( 1, 2 )

        query = nx.Graph()
        query.add_node( 2, label="B" )
        query.add_node( 3, label="C" )
        query.add_edge( 2, 3 )

        # Combine the graphs
        combined, node_matching, edge_matching = graph_utils.combine_graph( source, query )

        # Check combined graph structure
        assert len( combined.nodes ) == 3
        assert len( combined.edges ) == 2
        assert 1 in combined.nodes
        assert 2 in combined.nodes
        assert 3 in combined.nodes
        assert (1, 2) in combined.edges
        assert (2, 3) in combined.edges

        # Check node matching
        assert node_matching[ list( combined.nodes ).index( 1 ) ] == 0  # Only in source
        assert node_matching[ list( combined.nodes ).index( 2 ) ] == 1  # In both
        assert node_matching[ list( combined.nodes ).index( 3 ) ] == -1  # Only in query

        # Check edge matching
        edge_list = list( combined.edges )
        assert edge_matching[ edge_list.index( (1, 2) ) ] == 0  # Only in source
        assert edge_matching[ edge_list.index( (2, 3) ) ] == -1  # Only in query

    def test_combine_graph_with_anchor( self ):
        # Create two graphs with anchor node
        source = nx.Graph()
        source.add_node( 1, anchor=1, label="A" )
        source.add_node( 2, label="B" )
        source.add_edge( 1, 2 )

        query = nx.Graph()
        query.add_node( 1, anchor=1, label="A" )
        query.add_node( 3, label="C" )
        query.add_edge( 1, 3 )

        # Combine the graphs with anchor
        combined, node_matching, edge_matching = graph_utils.combine_graph( source, query, anchor=1 )

        # Check node matching with anchor
        assert node_matching[ list( combined.nodes ).index( 1 ) ] == 2  # Anchor node
        assert node_matching[ list( combined.nodes ).index( 2 ) ] == 0  # Only in source
        assert node_matching[ list( combined.nodes ).index( 3 ) ] == -1  # Only in query

    def test_combine_graph_directed( self ):
        # Test with directed graphs
        source = nx.DiGraph()
        source.add_node( 1, label="A" )
        source.add_node( 2, label="B" )
        source.add_edge( 1, 2 )

        query = nx.DiGraph()
        query.add_node( 2, label="B" )
        query.add_node( 3, label="C" )
        query.add_edge( 2, 3 )

        # Combine the graphs
        combined, node_matching, edge_matching = graph_utils.combine_graph( source, query )

        # Check edge direction is preserved
        assert combined.is_directed()
        assert (1, 2) in combined.edges
        assert (2, 1) not in combined.edges
        assert (2, 3) in combined.edges
        assert (3, 2) not in combined.edges

    def test_combine_graph_overlapping_edges( self ):
        # Test with edges present in both graphs
        source = nx.Graph()
        source.add_node( 1, label="A" )
        source.add_node( 2, label="B" )
        source.add_edge( 1, 2, weight=1 )

        query = nx.Graph()
        query.add_node( 1, label="A" )
        query.add_node( 2, label="B" )
        query.add_edge( 1, 2, weight=2 )  # Same edge but different attribute

        # Combine the graphs
        combined, node_matching, edge_matching = graph_utils.combine_graph( source, query )

        # The edge should be marked as present in both
        edge_list = list( combined.edges )
        assert edge_matching[ edge_list.index( (1, 2) ) ] == 1

        # Check that edge attributes from the second graph take precedence
        assert combined.edges[ (1, 2) ][ "weight" ] == 2

    def test_combine_normalized_partial_overlap( self ):
        # Create graphs with partial path overlap
        source = nx.Graph()
        source.add_node( 1, anchor=1, label="A" )
        source.add_node( 2, label="B" )
        source.add_node( 3, label="C" )
        source.add_edge( 1, 2 )
        source.add_edge( 1, 3 )

        query = nx.Graph()
        query.add_node( 1, anchor=1, label="A" )
        query.add_node( 2, label="B" )
        query.add_node( 3, label="D" )  # Different label
        query.add_edge( 1, 2 )
        query.add_edge( 1, 3 )

        combined, node_matching, _ = graph_utils.combine_normalized( source, query )

        # A and B nodes should match, but C and D should be separate
        assert len( combined.nodes ) == 4

        # Find nodes with each label
        nodes_by_label = { }
        for n, data in combined.nodes( data=True ):
            nodes_by_label[ data[ 'label' ] ] = n

        # Check node matching for each label
        assert node_matching[ list( combined.nodes ).index( nodes_by_label[ 'A' ] ) ] == 2  # Anchor
        assert node_matching[ list( combined.nodes ).index( nodes_by_label[ 'B' ] ) ] == 1  # In both
        # C is only in source, D is only in query
        assert 0 in node_matching  # At least one node only in source
        assert -1 in node_matching  # At least one node only in query

    def test_combine_normalized_no_overlap( self ):
        # Create graphs with no path overlap
        source = nx.Graph()
        source.add_node( 1, anchor=1, label="A" )
        source.add_node( 2, label="B" )
        source.add_edge( 1, 2 )

        query = nx.Graph()
        query.add_node( 1, anchor=1, label="C" )  # Different anchor label
        query.add_node( 2, label="D" )
        query.add_edge( 1, 2 )

        combined, node_matching, _ = graph_utils.combine_normalized( source, query )

        # Should have 4 separate nodes (no matching)
        assert len( combined.nodes ) == 4

        # The rest should be either source-only (0) or query-only (-1)
        assert sum( 1 for m in node_matching if m == 0 ) >= 1
        assert sum( 1 for m in node_matching if m == -1 ) >= 1

    def test_combine_normalized_with_colors( self ):
        # Test with custom matching colors
        source = nx.Graph()
        source.add_node( 1, anchor=1, label="A" )
        source.add_node( 2, label="B" )
        source.add_edge( 1, 2 )

        query = nx.Graph()
        query.add_node( 1, anchor=1, label="A" )
        query.add_node( 2, label="C" )
        query.add_edge( 1, 2 )

        colors = { 2: "red", 1: "blue", 0: "green", -1: "yellow" }

        combined, node_colors, edge_colors = graph_utils.combine_normalized( source, query, matching_colors=colors )

        # Check that colors were correctly assigned
        assert "red" in node_colors  # Anchor node
        assert "green" in node_colors  # Source-only nodes
        assert "yellow" in node_colors  # Query-only nodes

        # Edge colors should be consistent with node presence
        assert all( color in [ "blue", "green", "yellow" ] for color in edge_colors )

    def test_combine_normalized_directed_graphs( self ):
        # Test with directed graphs
        source = nx.DiGraph()
        source.add_node( 1, anchor=1, label="A" )
        source.add_node( 2, label="B" )
        source.add_edge( 1, 2 )  # A -> B

        query = nx.DiGraph()
        query.add_node( 1, anchor=1, label="A" )
        query.add_node( 2, label="B" )
        query.add_edge( 1, 2 )  # A -> B

        combined, node_matching, edge_matching = graph_utils.combine_normalized( source, query )

        # Should match all nodes and edges
        assert combined.is_directed()
        assert len( combined.nodes ) == 2
        assert all( matching in [ 1, 2 ] for matching in node_matching )  # All nodes match
        assert all( matching == 1 for matching in edge_matching )  # All edges match

    def test_get_norm_graph_intersection_identical_graphs( self ):
        # Create two identical graphs
        G1 = nx.Graph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_node( 3, label="C" )
        G1.add_edge( 1, 2 )
        G1.add_edge( 1, 3 )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 2, label="B" )
        G2.add_node( 3, label="C" )
        G2.add_edge( 1, 2 )
        G2.add_edge( 1, 3 )

        intersection = graph_utils.get_norm_graph_intersection( G1, G2 )

        # The intersection should be the entire graph
        assert len( intersection.nodes ) == 3
        assert len( intersection.edges ) == 2
        assert all( n in intersection.nodes for n in [ 1, 2, 3 ] )
        assert all( e in intersection.edges for e in [ (1, 2), (1, 3) ] )

    def test_get_norm_graph_intersection_partial_overlap( self ):
        # Create graphs with partial overlap
        G1 = nx.Graph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_node( 3, label="C" )
        G1.add_edge( 1, 2 )
        G1.add_edge( 1, 3 )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 2, label="B" )
        G2.add_node( 4, label="D" )  # Different node
        G2.add_edge( 1, 2 )
        G2.add_edge( 1, 4 )  # Different edge

        intersection = graph_utils.get_norm_graph_intersection( G1, G2 )

        # Should only contain nodes/edges present in both graphs
        assert len( intersection.nodes ) == 2  # Only A and B match
        assert len( intersection.edges ) == 1  # Only (1,2) matches

        # Check for specific nodes and edges
        node_labels = { data[ 'label' ] for _, data in intersection.nodes( data=True ) }
        assert 'A' in node_labels
        assert 'B' in node_labels
        assert 'C' not in node_labels
        assert 'D' not in node_labels

    def test_get_norm_graph_intersection_no_overlap( self ):
        # Create graphs with no overlap except anchor
        G1 = nx.Graph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_edge( 1, 2 )

        G2 = nx.Graph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 3, label="C" )
        G2.add_edge( 1, 3 )

        intersection = graph_utils.get_norm_graph_intersection( G1, G2 )

        # Should only contain the anchor node
        assert len( intersection.nodes ) == 1
        assert len( intersection.edges ) == 0

        # Check that the only node is the anchor
        assert any( data.get( 'anchor' ) == 1 for _, data in intersection.nodes( data=True ) )

    def test_get_norm_graph_intersection_directed_graphs( self ):
        # Test with directed graphs
        G1 = nx.DiGraph()
        G1.add_node( 1, anchor=1, label="A" )
        G1.add_node( 2, label="B" )
        G1.add_edge( 1, 2 )  # A -> B

        G2 = nx.DiGraph()
        G2.add_node( 1, anchor=1, label="A" )
        G2.add_node( 2, label="B" )
        G2.add_edge( 1, 2 )  # A -> B

        intersection = graph_utils.get_norm_graph_intersection( G1, G2 )

        # Should preserve graph type and match all elements
        assert intersection.is_directed()
        assert len( intersection.nodes ) == 2
        assert len( intersection.edges ) == 1
        assert (1, 2) in intersection.edges

        # Test with reverse edge direction
        G3 = nx.DiGraph()
        G3.add_node( 1, anchor=1, label="A" )
        G3.add_node( 2, label="B" )
        G3.add_edge( 2, 1 )  # B -> A (reverse direction)

        intersection2 = graph_utils.get_norm_graph_intersection( G1, G3 )

        # Should only contain the anchor node since edges don't match
        assert len( intersection2.nodes ) == 1
        assert len( intersection2.edges ) == 0

    def test_relabel_nodes_complete_mapping( self ):
        # Test when all nodes in G are in the mapping
        G = nx.Graph()
        G.add_node( 1, label="A" )
        G.add_node( 2, label="B" )
        G.add_edge( 1, 2, weight=5 )

        mapping = { 1: 10, 2: 20 }

        G_relabeled = graph_utils.relabel_nodes( G, mapping )

        # Check that nodes are correctly relabeled
        assert 10 in G_relabeled.nodes
        assert 20 in G_relabeled.nodes
        assert 1 not in G_relabeled.nodes
        assert 2 not in G_relabeled.nodes

        # Check that node attributes are preserved
        assert G_relabeled.nodes[ 10 ][ "label" ] == "A"
        assert G_relabeled.nodes[ 20 ][ "label" ] == "B"

        # Check that edge is preserved with attributes
        assert (10, 20) in G_relabeled.edges
        assert G_relabeled.edges[ 10, 20 ][ "weight" ] == 5

    def test_encode_pattern_id_with_valid_inputs( self ):
        # Test with valid inputs
        result = graph_utils.encode_pattern_id( "Pattern", "123" )
        assert result == "Pattern#123"

        # Test with different pattern name and ID
        result = graph_utils.encode_pattern_id( "Factory", "456" )
        assert result == "Factory#456"

    def test_encode_pattern_id_with_edge_cases( self ):
        # Test with None name
        result = graph_utils.encode_pattern_id( None, "123" )
        assert result is None

        # Test with None pid
        result = graph_utils.encode_pattern_id( "Pattern", None )
        assert result == "Pattern"

        # Test with "None" name
        result = graph_utils.encode_pattern_id( "None", "123" )
        assert result == "None"

        # Test with empty strings
        result = graph_utils.encode_pattern_id( "", "123" )
        assert result == "#123"

        result = graph_utils.encode_pattern_id( "Pattern", "" )
        assert result == "Pattern#"

    def test_decode_pattern_id_with_valid_inputs( self ):
        # Test with valid encoded pattern ID
        result = graph_utils.decode_pattern_id( "Pattern#123" )
        assert result == "123"

        # Test with different pattern name and ID
        result = graph_utils.decode_pattern_id( "Factory#456" )
        assert result == "456"

    def test_decode_pattern_id_with_edge_cases( self ):
        # Test with None
        result = graph_utils.decode_pattern_id( None )
        assert result is None

        # Test with no separator
        result = graph_utils.decode_pattern_id( "Pattern123" )
        assert result is None

        # Test with empty pattern ID
        result = graph_utils.decode_pattern_id( "Pattern#" )
        assert result == ""

        # Test with empty pattern name
        result = graph_utils.decode_pattern_id( "#123" )
        assert result == "123"
