## Web Search Engine

####Readme change log:
17.12.2019 Added going live section

## Build instructions

Checkout the project repository. Build using Maven.<br/>
`mvn clean package -Dmaven.test.skip=true`
<br/>
Use the following command to run the specific part of the project:
`mvn spring-boot:run -Dspring-boot.run.profiles=#profiles`

Replace `#profiles` by any of the arguments shown in the table below. If you want to run multiple profiles at a time just list them comma separated. We recommend to run the crawler and indexer profile at the same time.

| Command | Description |
|---------|-------------|
|crawler  | Runs the crawler|
|indexer  | Runs the indexer. Note that you need to pass the arguments in the same command. See section 'CLI to schedule task' for further information.|
|cli     | Runs the command line. Note that you need to pass the arguments in the same command. See section 'Task3, CLI' for further information about parameters etc. |

## Usage instructions

### Crawler 
For crawler, add seed urls in the `seed_urls.txt` file in `src/main/resources` (one per line).
The crawler is started with the whole Spring Boot application and starts the crawler based on the properties file `main/resources/application.properties`.

You can set there the following properties:

|    Property         |             Description         |
|---------------------|---------------------------------|
|**crawler.max_depth**|	max depth to crawl |
|**crawler.max_docs**| maximum number of docs|
|**crawler.leave_domain_boolean**| flag that indicates to search only in the provided domains|
|**crawler.numberOfThreadsToSpawn**| number of threads to spawn|

### Connection
Edit relevant properties in the application.properties file which is located in the same resources folder as above.
The following properties must be only adjusted, because we are using postgres anyway:

|    Property         |             Description         |
|---------------------|---------------------------------|
|**spring.datasource.url**|	jdbc complete connection string |
|**spring.datasource.username**|username for db authentication|
|**spring.datasource.password**| password for db authentication. Should match the password of the set user via spring.datasource.username|

**Example Configuration:**
```
spring.datasource.url=jdbc:postgresql://localhost:5432/project
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### Task3, CLI
For the CLI requested in task 3, use the interface in `src/main/java/com/example/main/CLI`. 

Arguments are the following:
@query, @k, @typeOfSearch (conjunctive or disjunctive)

### CLI to schedule task for tf_idf score update in DB
It starts a daemon thread to refresh the tf_idf scores in the DB after some interval
Path: `src/main/java/com/example/main/CLIIndexing`

Arguments:

|Property Name| Description|
|-------------|------------|
|indexing.interval.milliseconds| Time between two indexing processes|

##Going live!
The TUgle web interface is accessible at:

[http://isproj-vm01.informatik.uni-kl.de:8080/is-project/index.html](http://isproj-vm01.informatik.uni-kl.de:8080/is-project/index.html)

The json interface is accessible at:

[http://isproj-vm01.informatik.uni-kl.de:8080/is-project/json?query=#1&k=#2&score=#3]()

Replace the argument placeholder #1,#2, and #3 by:

|Argument|Description|Example|
|--------|-----------|-------|
|#1      |Keywords separated by +|database+course|
|#2      |Integer that represents the maximum number of results| 10|
|#3      |Integer that represents the scoring function:| 3|
|        |1 - TF*IDF||
|        |2 - Okapi BM25||
|        |3 - Combined Score||

The TUgle IMAGES web interface is accesible at:
[http://isproj-vm01.informatik.uni-kl.de:8080/is-project/images.html](http://isproj-vm01.informatik.uni-kl.de:8080/is-project/images.html)




