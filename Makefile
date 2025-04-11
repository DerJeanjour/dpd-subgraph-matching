start-container:
	docker-compose up -d

stop-container:
	docker-compose down

run-generation:
	docker exec graph-generation sh -c "java -jar generation/build/libs/generation-develop.jar --source=datasets/java/patterns/abstract-factory --name=abstract-factory --language=java --neo4j-host=neo4j:"

run-matching:
	docker exec graph-matching sh -c "python matching/main.py --name=abstract-factory --neo4j_host=neo4j:"

run:
	python cli.py --name=abstract-factory --language=java --source=datasets/java/patterns/abstract-factory
