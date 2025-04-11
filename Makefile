build:
	cd generation && ./gradlew clean build

run:
	cd generation && java -jar build/libs/generation-develop.jar

start-container:
	docker-compose up -d

stop-container:
	docker-compose down

run-docker:
	docker exec graph-generation sh -c "java -jar generation/build/libs/generation-develop.jar --source=datasets/java/patterns/adapter --language=java --neo4j-host=neo4j:"

