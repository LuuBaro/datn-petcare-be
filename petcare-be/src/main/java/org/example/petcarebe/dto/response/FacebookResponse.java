package org.example.petcarebe.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FacebookResponse {
    @JsonProperty("id")
    private String id;    // ID từ Facebook

    @JsonProperty("name")
    private String name;  // Tên từ Facebook

    @JsonProperty("email")
    private String email; // Email từ Facebook

    @JsonProperty("accessToken")
    private String accessToken; // Token từ Facebook
}
