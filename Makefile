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

####### FOR DEMO #######

run-generation-local:
	java -jar generation/build/libs/generation-develop.jar --source=datasets/java/patterns/factory-method --name=factory-method --language=java --neo4j-host=localhost:

run-matching-local:
	matching/venv/bin/python matching/main.py --name=factory-method --directed --neo4j_host=localhost:

run-demo:
	python run.py --name="$(name)" --language=java --source="datasets/java/$(j_path)" --local --use_cache

####### DEMOS #######

run-demo-factory-method:
	make run-demo name="factory-method" j_path="patterns/factory-method"

run-demo-abstract-factory:
	make run-demo name="abstract-factory" j_path="patterns/abstract-factory"

run-demo-adapter:
	make run-demo name="adapter" j_path="patterns/adapter"

run-demo-builder:
	make run-demo name="builder" j_path="patterns/builder"

run-demo-decorator: # good
	make run-demo name="decorator" j_path="patterns/decorator"

run-demo-observer:
	make run-demo name="observer" j_path="patterns/observer"

run-demo-quickuml:
	make run-demo name="quickuml" j_path="p-mart/1 - QuickUML 2001"

run-demo-jrefactor:
	make run-demo name="jrefactor" j_path="p-mart/3 - JRefactory v2.6.24"

run-demo-junit:
	make run-demo name="junit" j_path="p-mart/5 - JUnit v3.7"

run-demo-jhotdraw:
	make run-demo name="jhotdraw" j_path="p-mart/6 - JHotDraw v5.1"

run-demo-mapperxml:
	make run-demo name="mapperxml" j_path="p-mart/8 - MapperXML v1.9.7"