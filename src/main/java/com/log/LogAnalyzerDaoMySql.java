package com.log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogAnalyzerDaoMySql implements LogAnalyzerDao {
    private Connection connect = null;

    enum Sql {
        CONNECTION("jdbc:mysql://localhost/log_analyzer?serverTimezone=UTC&user=demo&password=demo"),
        GET_ALL_REQUESTS("select * from requests"),
        GET_REQUEST("select * from requests where id = ?"),
        GET_LAST_USER_REQUEST("select * from requests where user_id = ? order by datetime asc limit 1"),
        ADD_REQUEST("insert into requests values (default, ?, ?, ?, ?)"),
        ADD_SESSION("INSERT INTO sessions VALUES (default, ?, ?, 0)"),
        GET_LAST_USER_SESSION("SELECT * from sessions WHERE user_id = ? order by start_datetime asc limit 1"),
        UPDATE_SESSION("UPDATE sessions SET duration = ? where user_id = ?"),
        GET_USER_IDS("select distinct user_id from requests"),
        GET_USER_REQUESTS("select * from requests where user_id = ? order by datetime asc"),
        GET_UNIQUE_USERS("select distinct user_id from requests"),
        GET_TOP_USERS_WITH_JOIN(
                "with topN as (select user_id, count(user_id) as num_pages from requests group by user_id order by num_pages desc limit ?) " +
                        "select topN.user_id, topN.num_pages, count(id) as num_sessions, max(duration) as longest, min(duration) as shortest " +
                        "from sessions as s right join topN on (topN.user_id=s.user_id) " +
                        "group by topN.user_id order by topN.num_pages desc"),
        GET_TOP_USERS("select user_id, count(user_id) as num_requests from requests group by user_id order by num_requests desc limit ?"),
        GET_USER_SESSION_IDS("select id from sessions where user_id = ?"),
        GET_USER_LONGEST_SESSION("select duration from sessions where user_id = ? order by duration desc limit 1"),
        GET_USER_SHORTEST_SESSION("select duration from sessions where user_id = ? order by duration asc limit 1");

        private String statement;

        Sql(final String statement) {
            this.statement = statement;
        }

        public String getStatement() {
            return statement;
        }
    }

    @Override
    public List<Request> getRequests(final boolean keepDbConnection) {
        List<Request> requests = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_ALL_REQUESTS.statement);
            resultSet = preparedStatement.executeQuery();

            requests = new ArrayList<>();
            while (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String userId = resultSet.getString("user_id");
                Date requestDateTime = new Date(resultSet.getLong("datetime"));
                String httpMethod = resultSet.getString("httpMethod");
                String path = resultSet.getString("path");

                Request request = new Request(id, userId, requestDateTime, httpMethod, path);
                requests.add(request);
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return requests;
    }

    @Override
    public Request getRequest(final Long requestId, final boolean keepDbConnection) {
        Request request = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_REQUEST.statement);
            preparedStatement.setLong(1, requestId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String userId = resultSet.getString("user_id");
                Date requestDateTime = new Date(resultSet.getLong("datetime"));
                String httpMethod = resultSet.getString("httpMethod");
                String path = resultSet.getString("path");

                request = new Request(id, userId, requestDateTime, httpMethod, path);
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return request;
    }

    @Override
    public Request getLastUserRequest(final String userId, final boolean keepDbConnection) {
        Request request = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_LAST_USER_REQUEST.statement);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String userIdFromDb = resultSet.getString("user_id");
                Date requestDateTime = new Date(resultSet.getLong("datetime"));
                String httpMethod = resultSet.getString("httpMethod");
                String path = resultSet.getString("path");

                request = new Request(id, userIdFromDb, requestDateTime, httpMethod, path);
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return request;
    }

    @Override
    public Long addRequest(final Request request, final boolean keepDbConnection) {
        Long newRequestId = null;

        Statement statement = null;
        try {
            connect();

            statement = connect.createStatement();
            PreparedStatement preparedStatement =
                    connect.prepareStatement(Sql.ADD_REQUEST.statement, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, request.getUserId());

            Date requestDatetime = request.getDatetime();
            preparedStatement.setLong(2, requestDatetime.getTime());
            preparedStatement.setString(3, request.getHttpMethod());
            preparedStatement.setString(4, request.getPath());

            preparedStatement.executeUpdate();

            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            newRequestId = rs.getLong(1);
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }

        return newRequestId;
    }

    @Override
    public Long addSession(final Session session, final boolean keepDbConnection) {
        Long newSessionId = null;

        Statement statement = null;
        try {
            connect();

            statement = connect.createStatement();
            PreparedStatement preparedStatement =
                    connect.prepareStatement(Sql.ADD_SESSION.statement, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, session.getUserId());

            Date startDatetime = session.getStartDatetime();
            preparedStatement.setLong(2, startDatetime.getTime());

            preparedStatement.executeUpdate();

            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            newSessionId = rs.getLong(1);
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }

        return newSessionId;
    }

    @Override
    public Session getLastUserSession(final String userId, final boolean keepDbConnection) {
        Session session = null;
        connect();


        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_LAST_USER_SESSION.statement);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String userIdFromDb = resultSet.getString("user_id");
                Date startDateTime = new Date(resultSet.getLong("start_datetime"));
                Long duration = resultSet.getLong("duration");

                session = new Session(id, userIdFromDb, startDateTime, duration);
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return session;
    }

    @Override
    public void updateSession(final Session session, final boolean keepDbConnection) {
        Statement statement = null;
        try {
            connect();

            statement = connect.createStatement();
            PreparedStatement preparedStatement = connect.prepareStatement(Sql.UPDATE_SESSION.statement);
            preparedStatement.setLong(1, session.getDuration());
            preparedStatement.setString(2, session.getUserId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<String> getUsers(final boolean keepDbConnection) {
        List<String> userIds = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_USER_IDS.statement);
            resultSet = preparedStatement.executeQuery();

            userIds = new ArrayList<>();
            while (resultSet.next()) {
                userIds.add(resultSet.getString("user_id"));
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return userIds;
    }

    @Override
    public List<Request> getUserRequests(final String userId, final boolean keepDbConnection) {
        List<Request> requests = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            // the requests come sorted by datetime ascending
            preparedStatement = connect.prepareStatement(Sql.GET_USER_REQUESTS.statement);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();

            requests = new ArrayList<>();
            while (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String userIdFromDb = resultSet.getString("user_id");
                Date requestDateTime = new Date(resultSet.getLong("datetime"));
                String httpMethod = resultSet.getString("httpMethod");
                String path = resultSet.getString("path");

                Request request = new Request(id, userIdFromDb, requestDateTime, httpMethod, path);
                requests.add(request);
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return requests;
    }

    @Override
    public List<String> getUniqueUsers(final boolean keepDbConnection) {
        List<String> userIds = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_UNIQUE_USERS.statement);
            resultSet = preparedStatement.executeQuery();

            userIds = new ArrayList<>();
            while (resultSet.next()) {
                userIds.add(resultSet.getString("user_id"));
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return userIds;
    }

    @Override
    public List<Report.UserReport> getTopUsers(final Integer numberOfUsers, final boolean keepDbConnection) {
        List<Report.UserReport> topUsers = new ArrayList<>();
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_TOP_USERS.statement);
            preparedStatement.setInt(1, numberOfUsers);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String userId = resultSet.getString("user_id");

                Report.UserReport userReport = new Report.UserReport();
                userReport.setUserId(userId);
                userReport.setPagesVisited(resultSet.getInt("num_requests"));

                List<Integer> userSessionIds = getUserSessionIds(userId, keepDbConnection);
                if (userSessionIds != null) {
                    userReport.setNumberOfSessions(userSessionIds.size());
                }

                Long longestSessionMillis = getUserLongestSession(userId, keepDbConnection);
                Duration longestSessionDuration = Duration.ofMillis(longestSessionMillis);
                userReport.setLongestSession(longestSessionDuration.toMinutes());

                Long shortestSessionMillis = getUserShortestSession(userId, keepDbConnection);
                Duration shortestSessionDuration = Duration.ofMillis(shortestSessionMillis);
                userReport.setShortestSession(shortestSessionDuration.toMinutes());

                topUsers.add(userReport);
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return topUsers;
    }

    @Override
    public List<Report.UserReport> getTopUsersWithJoin(final Integer numberOfUsers, final boolean keepDbConnection) {
        List<Report.UserReport> topUsers = new ArrayList<>();
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_TOP_USERS_WITH_JOIN.statement);
            preparedStatement.setInt(1, numberOfUsers);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                //  user_id  | num_pages | num_sessions | longest  | shortest
                String userId = resultSet.getString("user_id");
                Integer numPagesVisited = resultSet.getInt("num_pages");
                Integer numSessions = resultSet.getInt("num_sessions");
                Long longestSessionMillis = resultSet.getLong("longest");
                Long shortestSessionMillis = resultSet.getLong("shortest");

                Report.UserReport userReport = new Report.UserReport();
                userReport.setUserId(userId);
                userReport.setPagesVisited(numPagesVisited);
                userReport.setNumberOfSessions(numSessions);

                Duration longestSessionDuration = Duration.ofMillis(longestSessionMillis);
                userReport.setLongestSession(longestSessionDuration.toMinutes());
                Duration shortestSessionDuration = Duration.ofMillis(shortestSessionMillis);
                userReport.setShortestSession(shortestSessionDuration.toMinutes());

                topUsers.add(userReport);
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return topUsers;
    }

    @Override
    public List<Integer> getUserSessionIds(final String userId, final boolean keepDbConnection) {
        List<Integer> userSessionIds = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_USER_SESSION_IDS.statement);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();

            userSessionIds = new ArrayList<>();
            while (resultSet.next()) {
                userSessionIds.add(resultSet.getInt("id"));
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return userSessionIds;
    }

    @Override
    public Long getUserLongestSession(final String userId, final boolean keepDbConnection) {
        Long longestSessionDuration = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_USER_LONGEST_SESSION.statement);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                longestSessionDuration = resultSet.getLong("duration");
            }

        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return longestSessionDuration;
    }

    @Override
    public Long getUserShortestSession(final String userId, final boolean keepDbConnection) {
        Long shortestSessionDuration = null;
        connect();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connect.prepareStatement(Sql.GET_USER_SHORTEST_SESSION.statement);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                shortestSessionDuration = resultSet.getLong("duration");
            }
        } catch (SQLException e) {
            // LOGGER e
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }

                if (preparedStatement != null) {
                    preparedStatement.close();
                }

                if (!keepDbConnection) {
                    closeDbConnection();
                }
            } catch (Exception e) {
                // LOGGER e
                e.printStackTrace();
            }
        }
        return shortestSessionDuration;
    }

    @Override
    public void closeDbConnection() {
        try {
            if (connect != null) {
                connect.close();
            }
            connect = null;
        } catch (Exception e) {
            // LOGGER e
            e.printStackTrace();
        }
    }

    private void connect() {
        if (connect == null) {
            try {
                connect = DriverManager.getConnection(Sql.CONNECTION.statement);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}