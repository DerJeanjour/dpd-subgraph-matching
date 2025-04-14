# Design Pattern Detection

This project implements design pattern detection in source code using Code Property Graphs (CPGs) and Subgraph Matching techniques. It focuses on identifying common design patterns in Java, C++, and Python codebases.

## Overview

The system works by:
1. Building Code Property Graphs from source code
2. Defining design patterns as graph templates
3. Using neural subgraph matching to detect pattern instances

## Project Structure
* [datasets](datasets/) - Contains sample code repositories organized by language
* [generation](generation/) - Tools for generating and processing CPGs from source code
* [matching](matching/) - Design pattern matching implementation using GLeMA Net

## Usage
* Any annotated dataset must be present and generated with generation process (e.g., [P-MARt](./datasets/java/p-mart/), [DPDf](./datasets/java/dpdf/))
* The source code projects to analyze must be located in the language-specific folder in [datasets](datasets/)
* The dependencies for the generation component must be installed, and the jar has to be built with Gradle (see [Makefile](generation/Makefile))
* Docker must be installed and the [docker-compose](docker-compose.yml) has to be running (see [Makefile](Makefile))
* To run the detection process on a specific dataset, use the run command in the [CLI tool](run.py)