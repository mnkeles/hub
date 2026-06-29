package etiya.omniAutomation.business.dto;

import com.jayway.jsonpath.JsonPath;
import etiya.omniAutomation.service.UtilityService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ExpressionInput extends AbstractDto {

    private Object value;
    private UtilityService utilityService;
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    public ExpressionInput(Object value) {
        this.value = value;
        this.utilityService = null;
    }

    public String executeRegex(String regex, String source) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher((source));
        if (matcher.find()) {
            return matcher.group(1);
        }
        this.logger.warning("regex null");
        return null;
    }

    public String executeJsonPath(String path, String source) {
        return JsonPath.read(StringUtils.deleteWhitespace(source), path);
    }
}
