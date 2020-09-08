package com.log;

import java.util.List;

public interface LogAnalyzerDao {
    List<Request> getRequests(boolean keepDbConnection);

    Request getRequest(Long requestId, boolean keepDbConnection);

    Request getLastUserRequest(String userId, boolean keepDbConnection);

    Long addRequest(Request request, boolean keepDbConnection);

    Long addSession(Session session, boolean keepDbConnection);

    Session getLastUserSession(String userId, boolean keepDbConnection);

    void updateSession(Session session, boolean keepDbConnection);

    List<String> getUsers(boolean keepDbConnection);

    void closeDbConnection();

    List<Request> getUserRequests(String userId, boolean keepDbConnection);

    List<String> getUniqueUsers(boolean keepDbConnection);

    List<Report.UserReport> getTopUsers(Integer numberOfUsers, boolean keepDbConnection);

    List<Report.UserReport> getTopUsersWithJoin(Integer numberOfUsers, boolean keepDbConnection);

    List<Integer> getUserSessionIds(String userId, boolean keepDbConnection);

    Long getUserLongestSession(String userId, boolean keepDbConnection);

    Long getUserShortestSession(String userId, boolean keepDbConnection);
}
