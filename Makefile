start-container:
	docker-compose up -d

stop-container:
	docker-compose down

run-generation:
	docker exec graph-generation sh -c "java -jar generation/build/libs/generation-develop.jar --source=datasets/java/patterns/factory-method --name=factory-method --language=java --neo4j-host=neo4j:"

run-matching:
	docker exec graph-matching sh -c "python matching/main.py --name=factory-method --directed --neo4j_host=neo4j:"

run:
	python run.py --name=factory-method --language=java --source=datasets/java/patterns/factory-method
