package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ProcessFlowDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepParmDto;
import etiya.omniAutomation.common.ScriptHelper;
import etiya.omniAutomation.entity.DatabaseConfigEntity;
import etiya.omniAutomation.repository.DatabaseConfigRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatabaseHelper {

    private final DatabaseConfigRepository databaseConfigRepository;
    private final SshTunnelService sshTunnelService;
    private JdbcTemplate jdbcTemplate;
    private transient final ExpressionParser expressionParser = new SpelExpressionParser();

    @PostConstruct
    public void createDefaultTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        dataSource.setUrl("jdbc:oracle:thin:@ttc-multi-type2-internal-db.etiya.com:1521/OMNI");
        dataSource.setUsername("OMNI_BSS_REGR");
        dataSource.setPassword("SxrGhL161N");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Ortak veri kaynağı ve JdbcTemplate yapılandırma metodu
    private JdbcTemplate createJdbcTemplate(DatabaseConfigEntity databaseConfig) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        String dbUrl = databaseConfig.getUrl();

        // SSH tunnel gerekiyorsa
        if (Boolean.TRUE.equals(databaseConfig.getSshEnabled())) {
            // URL'den host ve port parse et
            String[] hostPort = parseHostPort(dbUrl);
            String remoteHost = hostPort[0];
            int remotePort = Integer.parseInt(hostPort[1]);

            Integer localPort = sshTunnelService.createTunnel(
                    databaseConfig.getSshHost(),
                    databaseConfig.getSshPort(),
                    databaseConfig.getSshUser(),
                    databaseConfig.getSshPassword(),
                    remoteHost,
                    remotePort
            );

            // JDBC URL'i localhost ve local port ile güncelle
            dbUrl = dbUrl.replace(remoteHost + ":" + remotePort, "localhost:" + localPort);
            //Alltaki iki if bloğu OMNI projesinde driverClass ve Schmea kullanılacağı zaman ssh bloğundan çıkarılıp altta kullanılmalı
            if (databaseConfig.getDriver() != null && !databaseConfig.getDriver().isEmpty()) {
                dataSource.setDriverClassName(databaseConfig.getDriver());
            }

            if (databaseConfig.getSchema() != null && !databaseConfig.getSchema().isEmpty()) {
                dataSource.setSchema(databaseConfig.getSchema());
            }
        }
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(databaseConfig.getUsername());
        dataSource.setPassword(databaseConfig.getPassword());
        return new JdbcTemplate(dataSource);
    }

    private String[] parseHostPort(String jdbcUrl) {
        // jdbc:postgresql://host:port/database veya jdbc:oracle:thin:@host:port/service formatından host:port parse et
        String pattern = "//([^:/]+):(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(jdbcUrl);

        if (m.find()) {
            return new String[]{m.group(1), m.group(2)};
        }

        // Oracle thin format için alternatif: @host:port
        pattern = "@([^:/]+):(\\d+)";
        p = java.util.regex.Pattern.compile(pattern);
        m = p.matcher(jdbcUrl);

        if (m.find()) {
            return new String[]{m.group(1), m.group(2)};
        }

        throw new RuntimeException("Cannot parse host and port from JDBC URL: " + jdbcUrl);
    }

    // Veritabanından son REF_CODE değerinin SMS_VAL değerini alır
    // Veritabanından son REF_CODE değerinin SMS_VAL değerini alır (Oracle 11g uyumlu)
    public String getLatestSmsValByRefCode(String environment, Long projectId, ProcessFlowStepParmDto stepParameter) {
        JdbcTemplate template = getJdbcTemplate(environment, projectId);

        String sql = stepParameter.getSql();

        try {
            String encodedSmsVal = template.queryForObject(sql, String.class);

            if (encodedSmsVal != null && !encodedSmsVal.isEmpty()) {
                return encodedSmsVal;  // Base64 çözülmüş değeri döndür
            } else {
                return null;  // Kayıt yoksa null döner
            }
        } catch (EmptyResultDataAccessException e) {
            return null;  // Sonuç yoksa null döner
        } catch (Exception e) {
            return "Database Error";  // Diğer hatalarda null döner
        }
    }

    private JdbcTemplate getJdbcTemplate(String environment, Long projectId) {
        DatabaseConfigEntity config = this.databaseConfigRepository.findByShortCodeAndProjectIdAndIsActv(environment, projectId, true);
        JdbcTemplate template = this.createJdbcTemplate(config);
        return template;
    }

    // Base64 çözme işlemi
    private String decodeBase64(String encodedValue) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedValue);
            return new String(decodedBytes);
        } catch (IllegalArgumentException e) {
            return encodedValue;  // Çözülemiyorsa şifreli değeri döndür
        }
    }
    public String getRandomFirstName() {
        String sql = "SELECT FRST_NAME FROM PARTY p " +
                "WHERE FRST_NAME IS NOT NULL " +
                "ORDER BY DBMS_RANDOM.VALUE " +
                "FETCH FIRST 1 ROWS ONLY";

        try {
            String randomFirstName = jdbcTemplate.queryForObject(sql, String.class);

            if (randomFirstName != null && !randomFirstName.isEmpty()) {
                return randomFirstName;
            } else {
                return null;
            }
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    public String getCustomerAccountId(String txnId, String environment, Long projectId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(environment, projectId);
        String sql = "SELECT XMLCast(" +
                "XMLQuery('declare namespace ns0=\"http://www.avea.com.tr/om\"; " +
                "declare namespace ns1=\"http://www.avea.com.tr/AveaProduct\"; " +
                "declare namespace ns2=\"http://www.avea.com.tr/AveaFrameWork\"; " +
                "declare namespace ns3=\"http://www.avea.com.tr/AccountContact\"; " +
                "/ns0:FulfillOrderRequest/ns0:RequestBody/ns3:CustomerAccount/ns3:Account_Id/ns3:CustomerAccountId/text()' " +
                "PASSING XMLType(PL_OUT) RETURNING CONTENT) AS VARCHAR2(100)) " +
                "AS customer_account_id " +
                "FROM gnl_ext_syst_call_log " +
                "WHERE TXN_ID = ?";

        try {
            String custAcctId = jdbcTemplate.queryForObject(sql, new Object[]{txnId}, String.class);
            return !custAcctId.isEmpty() ? custAcctId : null;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getRandomLastName() {
        String sql = "SELECT LST_NAME FROM (" +
                "SELECT LST_NAME " +
                "FROM PARTY p " +
                "WHERE LST_NAME IS NOT NULL " +
                "ORDER BY DBMS_RANDOM.VALUE" +
                ") WHERE ROWNUM = 1";

        try {
            String randomLastName = jdbcTemplate.queryForObject(sql, String.class);

            if (!randomLastName.isEmpty()) {
                return randomLastName;  // Rastgele soyisim döndür
            } else {
                return null;
            }
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    public String getCustomerBillAccountId(String txnId, String environment, Long projectId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(environment, projectId);
        String sql = "SELECT XMLCast(" +
                "XMLQuery('declare namespace ns0=\"http://www.avea.com.tr/om\"; " +
                "declare namespace ns1=\"http://www.avea.com.tr/AveaProduct\"; " +
                "declare namespace ns2=\"http://www.avea.com.tr/AveaFrameWork\"; " +
                "declare namespace ns3=\"http://www.avea.com.tr/AccountContact\"; " +
                "/ns0:FulfillOrderRequest/ns0:RequestBody/ns3:CustomerAccount/ns3:Account_Id/ns3:CustomerBillAccountId/text()' " +
                "PASSING XMLType(PL_OUT) RETURNING CONTENT) AS VARCHAR2(100)) " +
                "AS customer_bill_account_id " +
                "FROM gnl_ext_syst_call_log " +
                "WHERE TXN_ID = ?";

        try {
            String custBillAcctId = jdbcTemplate.queryForObject(sql, new Object[]{txnId}, String.class);
            return !custBillAcctId.isEmpty() ? custBillAcctId : null;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public String executeDynamicQuery(ProcessFlowDto processFlow, ProcessFlowStepParmDto stepParam, Long projectId) {
        String environment = processFlow.getSystemShortCode();
        JdbcTemplate jdbcTemplate = getJdbcTemplate(environment, projectId);
        String[] rawSql = stepParam.getSql().split("&");
        String sql = rawSql[0];
        String args = rawSql[1];

        if (StringUtils.isNotEmpty(args)) {
            for (String item : args.split(",")) {
                String key = item.replaceFirst("\\$\\{(\\w+)\\}", "$1");
                sql = sql.replace(item, processFlow.getParameterContext().get(key));
            }
        }

        try {
            String response = jdbcTemplate.queryForObject(sql, String.class);
            return !response.isEmpty() ? response : null;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String executeDynamicCode(ProcessFlowStepParmDto stepParam) {
        String response = "";
        String code = stepParam.getCode();
        int index = code.indexOf('&');
        if (index > 0 && index < code.length() - 1) {
            String key = code.substring(0, index);
            String path = code.substring(index + 1);
            if(path.charAt(0) == '#'){
                response = processScript(path.substring(1));
            }
        }
        return !response.isEmpty() ? response : null;
    }

    private String processScript(String parameterExtractor) {
        Expression expression = expressionParser.parseExpression(parameterExtractor);
        EvaluationContext context = new StandardEvaluationContext();
        context.setVariable("engine", new ScriptHelper());
        return expression.getValue(context, String.class);
    }

    public String getGuidValue(String txnId, String environment, Long projectId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(environment, projectId);
        String sql = "SELECT XMLCast(" +
                "XMLQuery('declare namespace ns0=\"http://www.avea.com.tr/om\"; " +
                "declare namespace ns1=\"http://www.avea.com.tr/AveaProduct\"; " +
                "declare namespace ns2=\"http://www.avea.com.tr/AveaFrameWork\"; " +
                "declare namespace ns3=\"http://www.avea.com.tr/AccountContact\"; " +
                "/ns0:FulfillOrderRequest/ns2:RequestHeader/ns2:RequestId/ns2:GUID/text()' " +
                "PASSING XMLType(PL_OUT) RETURNING CONTENT) AS VARCHAR2(100)) " +
                "AS guid_value " +
                "FROM gnl_ext_syst_call_log " +
                "WHERE TXN_ID = ?";

        try {
            String guid = jdbcTemplate.queryForObject(sql, new Object[]{txnId}, String.class);
            return (guid != null && !guid.isEmpty()) ? guid : null;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public String getContactId(String txnId, String environment, Long projectId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(environment, projectId);
        String sql = "SELECT XMLCast(" +
                "XMLQuery('declare namespace ns0=\"http://www.avea.com.tr/om\"; " +
                "declare namespace ns1=\"http://www.avea.com.tr/AveaProduct\"; " +
                "declare namespace ns2=\"http://www.avea.com.tr/AveaFrameWork\"; " +
                "declare namespace ns3=\"http://www.avea.com.tr/AccountContact\"; " +
                "/ns0:FulfillOrderRequest/ns0:RequestBody/ns3:CustomerAccount/ns3:Contact/ns3:ContactId/text()' " +
                "PASSING XMLType(PL_OUT) RETURNING CONTENT) AS VARCHAR2(100)) " +
                "AS contact_id " +
                "FROM gnl_ext_syst_call_log " +
                "WHERE TXN_ID = ?";

        try {
            String contactId = jdbcTemplate.queryForObject(sql, new Object[]{txnId}, String.class);
            return (contactId != null && !contactId.isEmpty()) ? contactId : null;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object executeSql(String sql, String environment, Long projectId) {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(environment, projectId);
            String normalizedSql = sql == null ? "" : sql.trim().toLowerCase();
            String sqlOperation = detectSqlOperation(normalizedSql);

            if ("SELECT".equals(sqlOperation)) {
                return jdbcTemplate.queryForList(sql);
            }

            if ("INSERT".equals(sqlOperation) || "UPDATE".equals(sqlOperation) || "DELETE".equals(sqlOperation)) {
                int affectedRows = jdbcTemplate.update(sql);
                Map<String, Object> result = new HashMap<>();
                result.put("operation", sqlOperation);
                result.put("affectedRows", affectedRows);
                result.put("success", true);
                return result;
            }

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return result;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("SQL execution error: " + e.getMessage(), e);
        }
    }

    private String extractSqlOperation(String normalizedSql) {
        if (normalizedSql.startsWith("insert")) {
            return "INSERT";
        }
        if (normalizedSql.startsWith("update")) {
            return "UPDATE";
        }
        if (normalizedSql.startsWith("delete")) {
            return "DELETE";
        }
        return "UNKNOWN";
    }

    private String detectSqlOperation(String normalizedSql) {
        if (normalizedSql.startsWith("with")) {
            if (normalizedSql.matches("(?s)^with\\b.*\\bselect\\b.*")) {
                return "SELECT";
            }
            if (normalizedSql.matches("(?s)^with\\b.*\\binsert\\b.*")) {
                return "INSERT";
            }
            if (normalizedSql.matches("(?s)^with\\b.*\\bupdate\\b.*")) {
                return "UPDATE";
            }
            if (normalizedSql.matches("(?s)^with\\b.*\\bdelete\\b.*")) {
                return "DELETE";
            }
        }
        return extractSqlOperation(normalizedSql.startsWith("select") ? "select" : normalizedSql);
    }
}
