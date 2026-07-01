package etiya.omniAutomation.request;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GeneralPageRequest {

    private int offset;
    private int limit;
    private List<GeneralFilter> filterList = new ArrayList<>();

    public GeneralPageRequest(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }
}
