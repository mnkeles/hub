package etiya.omniAutomation.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HarAnalysisRequest {

    private String projectShortCode;
    private String systemShortCode;
    private String harContent;
}
