from unittest.mock import patch, MagicMock

import networkx as nx
import pytest

import matching.misc.import_repository_datasets as import_repository_datasets


@pytest.fixture
def mock_args():
    args = MagicMock()
    args.neo4j_protocol = "bolt://"
    args.neo4j_host = "localhost"
    args.neo4j_port = ":7687"
    args.neo4j_user = "neo4j"
    args.neo4j_pw = "password"
    return args


@pytest.fixture
def mock_graph_data():
    return [
        {
            "source_id": "1",
            "source_labels": [ "Person" ],
            "source_properties": { "node.dataset": "test_dataset", "name": "Alice" },
            "target_id": "2",
            "target_labels": [ "Person" ],
            "target_properties": { "node.dataset": "test_dataset", "name": "Bob" },
            "edge_type": "KNOWS"
        },
        {
            "source_id": "1",
            "source_labels": [ "Person" ],
            "source_properties": { "node.dataset": "test_dataset", "name": "Alice" },
            "target_id": "3",
            "target_labels": [ "Person" ],
            "target_properties": { "node.dataset": "test_dataset", "name": "Charlie" },
            "edge_type": "KNOWS"
        }
    ]


def test_add_record_to_graph():
    G = nx.MultiDiGraph()
    record = {
        "source_id": "1",
        "source_labels": [ "Person" ],
        "source_properties": { "name": "Alice" },
        "target_id": "2",
        "target_labels": [ "Person" ],
        "target_properties": { "name": "Bob" },
        "edge_type": "KNOWS"
    }

    import_repository_datasets.add_record_to_graph( G, record )

    assert len( G.nodes ) == 2
    assert len( G.edges ) == 1
    assert G.nodes[ "1" ][ "name" ] == "Alice"
    assert G.nodes[ "2" ][ "name" ] == "Bob"
    assert G.edges[ "1", "2", 0 ][ "type" ] == "KNOWS"


def test_transform_list_to_flags():
    data = { "labels": [ "Person", "User" ] }
    import_repository_datasets.transform_list_to_flags( data, "labels" )

    assert "labels" not in data
    assert data[ "labels.Person" ] is True
    assert data[ "labels.User" ] is True


def test_transform_list_attributes():
    data = { "names": [ "Alice", "Bob" ], "age": 30 }
    import_repository_datasets.transform_list_attributes( data )

    assert data[ "names" ] == "Alice,Bob"
    assert data[ "age" ] == 30


def test_replace_dots():
    data = { "node.name": "Alice", "user.id": 123 }
    import_repository_datasets.replace_dots( data )

    assert "node.name" not in data
    assert "user.id" not in data
    assert data[ "node_name" ] == "Alice"
    assert data[ "user_id" ] == 123


def test_prepare_graph_attributes():
    G = nx.MultiDiGraph()
    G.add_node( "1", **{ "labels": [ "Person" ], "node.name": "Alice", "tags": [ "user", "admin" ] } )
    G.add_edge( "1", "2", type="KNOWS", properties=[ "p1", "p2" ] )

    import_repository_datasets.prepare_graph_attributes( G )

    assert "labels" not in G.nodes[ "1" ]
    assert G.nodes[ "1" ][ "labels_Person" ] is True
    assert G.nodes[ "1" ][ "node_name" ] == "Alice"
    assert G.nodes[ "1" ][ "tags" ] == "user,admin"
    assert G.edges[ "1", "2", 0 ][ "properties" ] == "p1,p2"


@patch( 'networkx.write_gml' )
@patch( 'matching.misc.utils.get_abs_file_path' )
@patch( 'os.path.exists' )
@patch( 'os.makedirs' )
def test_write_as_gml( mock_makedirs, mock_exists, mock_get_path, mock_write_gml ):
    G = nx.MultiDiGraph()
    G.add_node( "1" )
    G.add_edge( "1", "2" )

    mock_exists.return_value = False
    mock_get_path.return_value = "/path/to/datasets/test.gml"

    import_repository_datasets.write_as_gml( G, "test" )

    mock_get_path.assert_called_once_with( "datasets/test.gml" )
    mock_exists.assert_called_once()
    mock_makedirs.assert_called_once()
    mock_write_gml.assert_called_once_with( G, "/path/to/datasets/test.gml" )


@patch( 'matching.misc.import_repository_datasets.write_as_gml' )
@patch( 'matching.misc.import_repository_datasets.prepare_graph_attributes' )
def test_import_dataset( mock_prepare, mock_write, mock_args, mock_graph_data ):
    mock_session = MagicMock()
    mock_driver = MagicMock()
    mock_driver.session.return_value.__enter__.return_value = mock_session
    mock_session.execute_read.return_value = mock_graph_data

    with patch( 'neo4j.GraphDatabase.driver', return_value=mock_driver ):
        import_repository_datasets.import_dataset( mock_args )

    assert mock_prepare.call_count == 1
    assert mock_write.call_count == 1
    mock_driver.close.assert_called_once()


def test_fetch_graph_data():
    mock_tx = MagicMock()
    mock_tx.run.return_value = [ "result1", "result2" ]

    result = import_repository_datasets.fetch_graph_data( mock_tx )

    assert len( result ) == 2
    assert result == [ "result1", "result2" ]
    mock_tx.run.assert_called_once()
