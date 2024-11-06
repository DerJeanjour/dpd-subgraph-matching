build:
	#cd generation && mvn clean package
	cd generation && ./gradlew clean build
run:
	cd generation && java -jar build/libs/generation-develop.jar # not working ...

start-container:
	docker-compose up -d

stop-container:
	docker-compose down

#run-docker:
#	docker exec dynamicgraphs sh -c "java -jar target/generation-develop.jar"

