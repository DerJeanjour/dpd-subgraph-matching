# Pattern Matching

The pattern matching component is responsible for reading the persisted graph datasets from the database and preprocessing them for training and testing. 
The preprocessing phase includes the partitioning of the datasets into smaller graph samples, 
generating both positive and negative subgraphs for each sample, and synthesizing additional training data. 
Using the preprocessed data, the GNN model (GLeMA Net) is trained and tested on the generated graph samples. 
The pattern detection task performs graph normalization steps on the graph data and extracts pattern examples for the final pattern voting mechanism.

## Install
1. Install python (3.9): [install](https://www.python.org/downloads/release/python-390/)
2. Install torch (2.4.1): [install](https://pytorch.org/get-started/locally/)
3. Install torch-geometric (2.6.1): [install](https://pytorch-geometric.readthedocs.io/en/latest/notes/installation.html)
4. Install additional requirements: ```pip install -r requirements.txt```

## Sources
- xNeuSM [git](https://github.com/martinakaduc/xNeuSM)
- neural-subgraph-learning-GNN [(git)](https://github.com/snap-stanford/neural-subgraph-learning-GNN)