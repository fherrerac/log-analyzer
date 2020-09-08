
#### I. OVERVIEW

The application is divided in two parts:

##### **Part 1) Log Processing**

This part consists of processing the log files and isnerting the information into a MySql DB. At this point, 
this process must be triggered manually by running "ProcessLog.main()". This process is divided in two steps:
                                                                        
    private void processLogFiles() {
      processRequests(); <-- Step Two
      processSessions(); <-- Step Two
    }
    
    Step One)
    Processing requests from file: xaa
    Processing requests from file: xab
    ...
    Time to process logs: 0 mins 22 secs

    Step One)
    Processing sessions for user: 71f28176
    Processing sessions for user: b3a60c78
    ...
    Time to process sessions: 0 mins 32 secs

            
**Step One)** First, each log entry is analyzed and inserted into the "requests" table. Turns out not all log entries are 
valid user requests (eg. GET /).

**Step Two)** Second, for each user, his list of requests is analyzed and the table sessions is populated:

            List<Request> requests = logAnalyzerDaoMySql.getUserRequests(userId, keepDbConnection);
            for (Request request : requests) {
                Session lastUserSession = logAnalyzerDaoMySql.getLastUserSession(userId, keepDbConnection);
                if (lastUserSession == null) {
                    Session session = new Session(userId, request.getDatetime());
                    logAnalyzerDaoMySql.addSession(session, keepDbConnection);
                } else {
                    Long requestDelta = request.getDatetime().getTime() -
                            (lastUserSession.getStartDatetime().getTime() + lastUserSession.getDuration());
                    if (requestDelta <= MAX_SESSION_DURATION) {
                        lastUserSession.setDuration(lastUserSession.getDuration() + requestDelta);
                        logAnalyzerDaoMySql.updateSession(lastUserSession, keepDbConnection);
                    } else {
                        Session session = new Session(userId, request.getDatetime());
                        logAnalyzerDaoMySql.addSession(session, keepDbConnection);
                    }
                }
            }

##### **Part 2) Report Creation**

The second part is about creating the report. The logic happens in LogAnalzerService. And for now, this 
part is exposed via a small Rest API. To build and run the Rest API service:

    mvn clean package
    mvn jetty:run

    http://localhost:8080/log/report

    export MAVEN_OPTS="$MAVEN_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n"

You can use the POSTMAN collection to see the above response:

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
                    "longestSession": 0,
                    "numberOfSessions": 3924,
                    "pagesVisited": 3926,
                    "shortestSession": 0,
                    "userId": "43a81873"
                }
            ]
        }
        
#### II. DATABASE

There is a MySQL database consisting of two tables:

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