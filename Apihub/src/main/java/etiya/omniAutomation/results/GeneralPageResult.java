package etiya.omniAutomation.results;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class GeneralPageResult<T> implements Serializable {

    private int page;
    private int size;
    private int totalElements;
    private int totalPages;
    private T content;
}
