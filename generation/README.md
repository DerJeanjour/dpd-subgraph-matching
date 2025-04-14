# Dataset Generation
The graph generation component has the purpose of translating and processing source code into a structured graph representation. 
For this, the component is divided into three tasks. 

- First, the source code is translated into an external Code Property Graph model defined by the external translation library. This model is then mapped to the internal CPG representation and is optionally marked with design pattern annotations for training and testing purposes. 
- In the second task, the CPG model is transformed into the RIG representation using a specific processing pipeline. 
- The last task maps the RIG to a graph database model and uses a dedicated persistence interface to write the graph into the database.

## Additional Info
For large graphs, use vm options to extend heap size.
e.g.: "-Xmx8g -Xms8g" for 8GB of heap memory.