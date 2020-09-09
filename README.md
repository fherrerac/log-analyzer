
## I. OVERVIEW

The application is divided in two parts:

### **Part One: Log Processing**

This part consists of processing the log files and inserting the information into a MySql DB. At this point, 
this process must be triggered manually by running "ProcessLog.main()". This process is divided in two steps:
                                                                        
    private void processLogFiles() {
      processRequests(); <-- Step Two
      processSessions(); <-- Step Two
    }
    
    Step One)
    Processing requests from file: xaa
    Processing requests from file: xab
    Processing requests from file: xae
    Processing requests from file: xad
    Processing requests from file: xac
    Time to process logs: 0 mins 22 secs

    Step Two)
    Processing sessions for user: 71f28176
    Processing sessions for user: b3a60c78
    ...
    Time to process sessions: 0 mins 27 secs
            
**Step One)** 

Each log entry is analyzed and inserted into the "requests" table. Turns out not all log entries are 
valid user requests (eg. GET /).

**Step Two)** 

For each user, his list of requests is analyzed and the table sessions is populated:

            // get all userId requests sorted by time
            List<Request> requests = logAnalyzerDaoMySql.getUserRequests(userId, keepDbConnection);
            
            // loop over userId requests
            for (Request request : requests) {
            
                Session lastUserSession = logAnalyzerDaoMySql.getLastUserSession(userId, keepDbConnection);
                if (lastUserSession == null) {

                    // if there is no session yet create one with duration as zero
                    Session session = new Session(userId, request.getDatetime());
                    logAnalyzerDaoMySql.addSession(session, keepDbConnection);
                } else {

                    // calculate the delta with the last request
                    Long requestDelta = request.getDatetime().getTime() -
                            (lastUserSession.getStartDatetime().getTime() + lastUserSession.getDuration());

                    if (requestDelta <= MAX_SESSION_DURATION) {
                        // the last request is less than MAX_SESSION_DURATION  apart (10 minutes)
                        // then increase session duration with delta
                        lastUserSession.setDuration(lastUserSession.getDuration() + requestDelta);
                        logAnalyzerDaoMySql.updateSession(lastUserSession, keepDbConnection);
                    } else {
                        // the last request is more than MAX_SESSION_DURATION  apart (10 minutes)
                        // then create a new session
                        Session session = new Session(userId, request.getDatetime());
                        logAnalyzerDaoMySql.addSession(session, keepDbConnection);
                    }
                }
            }

### **Part Two: Report Creation**

The second part is about creating the report. The logic happens in LogAnalyzerService. For now, this 
part is exposed via a Rest API. To build and run the Rest API service:

    mvn clean package
    mvn jetty:run

    http://localhost:8080/log/report

    export MAVEN_OPTS="$MAVEN_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n"

You can use the POSTMAN collection to see the following response:

        GET http://localhost:8080/log/report

        RESPONSE:
        {
            "uniqueUsers": 38,
            "userReports": [
                {
                    "longestSession": 860,
                    "numberOfSessions": 1,
                    "pagesVisited": 14555,
                    "shortestSession": 860,
                    "userId": "489f3e87"
                },
                {
                    "longestSession": 860,
                    "numberOfSessions": 1,
                    "pagesVisited": 8835,
                    "shortestSession": 860,
                    "userId": "71f28176"
                },
                {
                    "longestSession": 860,
                    "numberOfSessions": 1,
                    "pagesVisited": 4732,
                    "shortestSession": 860,
                    "userId": "95c2fa37"
                },
                {
                    "longestSession": 860,
                    "numberOfSessions": 1,
                    "pagesVisited": 4312,
                    "shortestSession": 860,
                    "userId": "eaefd399"
                },
                {
                    "longestSession": 409,
                    "numberOfSessions": 3,
                    "pagesVisited": 3926,
                    "shortestSession": 0,
                    "userId": "43a81873"
                }
            ]
        }
        
It is also possible to get it from mysql:

        mysql> with topN as 
            -> (select user_id, count(user_id) as num_pages 
            -> from requests group by user_id 
            -> order by num_pages desc limit 5) 
            -> select 
            -> topN.user_id, 
            -> topN.num_pages, 
            -> count(id) as num_sessions, 
            -> max(duration) as longest, 
            -> min(duration) as shortest 
            -> from sessions as s right 
            -> join topN on (topN.user_id=s.user_id) 
            -> group by topN.user_id 
            -> order by topN.num_pages desc
            -> ;
        +----------+-----------+--------------+----------+----------+
        | user_id  | num_pages | num_sessions | longest  | shortest |
        +----------+-----------+--------------+----------+----------+
        | 489f3e87 |     14555 |            1 | 51625000 | 51625000 |
        | 71f28176 |      8835 |            1 | 51635000 | 51635000 |
        | 95c2fa37 |      4732 |            1 | 51621000 | 51621000 |
        | eaefd399 |      4312 |            1 | 51623000 | 51623000 |
        | 43a81873 |      3926 |            3 | 24543000 |        0 |
        +----------+-----------+--------------+----------+----------+
        5 rows in set (0.03 sec)

        
## **II. Database**

The database is in MySQL and consists of two tables:

    CREATE TABLE `requests` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `user_id` varchar(100) NOT NULL,
      `datetime` bigint NOT NULL,
      `httpMethod` varchar(100) NOT NULL,
      `path` varchar(100) NOT NULL,
      PRIMARY KEY (`id`)
    );
    
    CREATE TABLE `sessions` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `user_id` varchar(100) NOT NULL,
      `start_datetime` bigint NOT NULL,
      `duration` bigint NOT NULL,
      PRIMARY KEY (`id`)
    );