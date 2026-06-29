package etiya.omniAutomation.request;

import lombok.Data;

@Data
public class AuthRequest {

    private String username;
    private String password;
    private String authType;

}
