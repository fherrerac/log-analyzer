package com.log;

import java.util.List;

public class LogAnalzerService {
    private LogAnalyzerDao logAnalyzerDaoMySql = new LogAnalyzerDaoMySql();

    public List<Request> getRequests() {
        return logAnalyzerDaoMySql.getRequests(false);
    }

    public Request getRequest(final Long requestId) {
        return logAnalyzerDaoMySql.getRequest(requestId, false);
    }

    public Report getReport() {
        /*
        Total unique users: 27
        Top 5 users:
        id              # pages # sess  longest shortest
        71f28176        75      3       35      1
        41f58122        65      4       60      10
        58122233        44      2       121     3

        1. unique users - select distinct user_id from requests
        2. top 5 users and #pages - select user_id, count(user_id) as num_requests from requests group by user_id order by num_requests
        desc limit 5
        3. for each above
        a) longest session - select duration from sessions where user_id = ? order by duration desc limit 1
        b) shortest session - select duration from sessions where user_id = ? order by duration asc limit 1
        */
        boolean keepDbConnection = true;

        Report report = new Report();
        List<String> uniqueUsers = logAnalyzerDaoMySql.getUniqueUsers(keepDbConnection);
        report.setUniqueUsers(uniqueUsers.size());

        List<Report.UserReport> topFiveUsers = logAnalyzerDaoMySql.getTopUsersWithJoin(5, keepDbConnection);
        report.setUserReports(topFiveUsers);

        logAnalyzerDaoMySql.closeDbConnection();
        return report;
    }
}
