package cn.allbs.mybatis.filter;

import cn.allbs.common.constant.DateConstant;
import cn.allbs.mybatis.properties.MybatisProperties;
import cn.allbs.mybatis.utils.TableConsoleUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.filter.FilterEventAdapter;
import com.alibaba.druid.proxy.jdbc.JdbcParameter;
import com.alibaba.druid.proxy.jdbc.ResultSetProxy;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * 类 DruidSqlLogFilter
 * </p>
 *
 * @author ChenQi
 * @since 2023/3/23 9:55
 */
@Slf4j
public class DruidSqlLogFilter extends FilterEventAdapter {

    private static final SQLUtils.FormatOption FORMAT_OPTION = new SQLUtils.FormatOption(false, false);

    private final MybatisProperties mybatisProperties;

    private boolean first = true;

    private static final Set<Integer> BLOB_TYPES = new HashSet<>();

    private ResultSet rs;
    private final Set<Integer> blobColumns = new HashSet<>();

    private int rows;

    private List<String> rowList = new LinkedList<>();

    private boolean STATEMENT_CLOSE_RUN = true;

    static {
        BLOB_TYPES.add(Types.BINARY);
        BLOB_TYPES.add(Types.BLOB);
        BLOB_TYPES.add(Types.CLOB);
        BLOB_TYPES.add(Types.LONGNVARCHAR);
        BLOB_TYPES.add(Types.LONGVARBINARY);
        BLOB_TYPES.add(Types.LONGVARCHAR);
        BLOB_TYPES.add(Types.NCLOB);
        BLOB_TYPES.add(Types.VARBINARY);
    }

    public DruidSqlLogFilter(MybatisProperties mybatisProperties) {
        this.mybatisProperties = mybatisProperties;
    }

    @Override
    protected void statementExecuteBefore(StatementProxy statement, String sql) {
        statement.setLastExecuteStartNano();
    }

    @Override
    protected void statementExecuteBatchBefore(StatementProxy statement) {
        statement.setLastExecuteStartNano();
    }

    @Override
    protected void statementExecuteUpdateBefore(StatementProxy statement, String sql) {
        statement.setLastExecuteStartNano();
    }

    @Override
    protected void statementExecuteQueryBefore(StatementProxy statement, String sql) {
        statement.setLastExecuteStartNano();
    }

    @Override
    protected void statementExecuteAfter(StatementProxy statement, String sql, boolean firstResult) {
        statement.setLastExecuteTimeNano();
    }

    @Override
    protected void statementExecuteBatchAfter(StatementProxy statement, int[] result) {
        statement.setLastExecuteTimeNano();
    }

    @Override
    protected void statementExecuteQueryAfter(StatementProxy statement, String sql, ResultSetProxy resultSet) {
        statement.setLastExecuteTimeNano();
    }

    @Override
    protected void statementExecuteUpdateAfter(StatementProxy statement, String sql, int updateCount) {
        statement.setLastExecuteTimeNano();
    }

    @Override
    public void statement_close(FilterChain chain, StatementProxy statement) throws SQLException {
        super.statement_close(chain, statement);
        // 支持动态开启
        if (!mybatisProperties.isShowSql()) {
            return;
        }

        // 是否开启调试
        if (!log.isInfoEnabled()) {
            return;
        }
        // 如果当前执行sql有结果则不执行 或者是新增语句，因为有id返回也不执行
//        Token token = new SQLStatementParser(statement.getLastExecuteSql()).getExprParser().getLexer().token();
//        if (statement.isFirstResultSet() || token.equals(Token.INSERT)) {
//            return;
//        }
        // 防止有结果时打印两次sql语句
        if (!STATEMENT_CLOSE_RUN) {
            STATEMENT_CLOSE_RUN = true;
            return;
        }
        // 打印可执行的 sql
        String sql = statement.getBatchSql();
        // sql 为空直接返回
        if (StringUtils.isEmpty(sql)) {
            return;
        }
        String executeSql = statement(statement);
        log.info(executeSql);
    }

    public String statement(StatementProxy statement) {
        // 打印可执行的 sql
        String sql = statement.getBatchSql();
        // sql 为空直接返回
        if (StringUtils.isEmpty(sql)) {
            return "";
        }
        int parametersSize = statement.getParametersSize();
        List<Object> parameters = new ArrayList<>(parametersSize);
        for (int i = 0; i < parametersSize; ++i) {
            // 转换参数，处理 java8 时间
            parameters.add(getJdbcParameter(statement.getParameter(i)));
        }
        String dbType = statement.getConnectionProxy().getDirectDataSource().getDbType();
        String formattedSql = SQLUtils.format(sql, DbType.of(dbType), parameters, FORMAT_OPTION);
        return printSql(formattedSql, statement);
    }

    private static Object getJdbcParameter(JdbcParameter jdbcParam) {
        if (jdbcParam == null) {
            return null;
        }
        Object value = jdbcParam.getValue();
        // 处理 java8 时间
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        return value;
    }

    private static String printSql(String sql, StatementProxy statement) {
        // 打印 sql
        String sqlLogger = "\n--------------------------------[ %s Sql Log ]---------------------------------" + "\n%s" + "\n--------------------------------[ Sql Execute Time: %s  ]---------------------------------\n";
        return String.format(sqlLogger, LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateConstant.NORM_DATETIME_PATTERN)), sql.trim(), format(statement.getLastExecuteTimeNano()));
    }

    /**
     * 格式化执行时间，单位为 ms 和 s，保留三位小数
     *
     * @param nanos 纳秒
     * @return 格式化后的时间
     */
    private static String format(long nanos) {
        if (nanos < 1) {
            return "0ms";
        }
        double millis = (double) nanos / (1000 * 1000);
        // 不够 1 ms，最小单位为 ms
        if (millis > 1000) {
            return String.format("%.3fs", millis / 1000);
        } else {
            return String.format("%.3fms", millis);
        }
    }

    @Override
    public boolean resultSet_next(FilterChain chain, ResultSetProxy resultSet) throws SQLException {
        boolean next = super.resultSet_next(chain, resultSet);
        // 支持动态开启
        if (!mybatisProperties.isShowSql()) {
            return next;
        }

        // 是否开启调试
        if (!log.isInfoEnabled()) {
            return next;
        }
        if (!next) {
            String querySql = statement(resultSet.getStatementProxy());
            String sqlLogger = querySql + "{}" + "--------------------------------[ Results Total {} ]---------------------------------\n";
            String results = "";
            if (rowList.size() > 0) {
                results = TableConsoleUtil.printResult(rowList, mybatisProperties.getChineRate());
            }
            log.info(sqlLogger, results, rows);
            rows = 0;
            rowList = new LinkedList<>();
            first = true;
            STATEMENT_CLOSE_RUN = false;
            return false;
        }
        STATEMENT_CLOSE_RUN = true;
        rows++;
        rs = resultSet.getResultSetRaw();
        ResultSetMetaData rsmd = resultSet.getMetaData();
        final int columnCount = rsmd.getColumnCount();
        if (first) {
            first = false;
            printColumnHeaders(rsmd, columnCount);
        }
        printColumnValues(columnCount);
        return true;
    }

    private void printColumnHeaders(ResultSetMetaData rsmd, int columnCount) throws SQLException {
        StringJoiner row = new StringJoiner(",");
        for (int i = 1; i <= columnCount; i++) {
            if (BLOB_TYPES.contains(rsmd.getColumnType(i))) {
                blobColumns.add(i);
            }
            row.add(rsmd.getColumnLabel(i));
        }
        rowList.add(row.toString());
    }

    private void printColumnValues(int columnCount) {
        StringJoiner row = new StringJoiner(",");
        for (int i = 1; i <= columnCount; i++) {
            try {
                if (blobColumns.contains(i)) {
                    row.add("<<BLOB>>");
                } else {
                    row.add(rs.getString(i));
                }
            } catch (SQLException e) {
                // generally can't call getString() on a BLOB column
                row.add("<<Cannot Display>>");
            }
        }
        rowList.add(row.toString());
    }
}
