## Web Search Engine

## Build instructions

Use Spring Boot to build

## Usage instructions

### Crawler 
For crawler, add seed urls in the seed_urls.txt file in the application resources. (src/main/resources)
One URL per line.
You can run the crawler with Command Line Interface CrawlerScheduler.java in `src/main/java/com/example/main` which takes following arguments:

@max_depth, @max_docs, @leaf_domain_boolean, @numberOfThreadsToSpawn

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
@jdbc url, @user, @pass, @query, @k, @typeOfSearch (conjunctive or disjunctive)

### CLI to schedule task for tf_idf score update in DB
It starts a daemon thread to refresh the tf_idf scores in the DB after some interval
Path: `src/main/java/com/example/main/CLIIndexing`

Arguments:
size of interval in milliseconds



