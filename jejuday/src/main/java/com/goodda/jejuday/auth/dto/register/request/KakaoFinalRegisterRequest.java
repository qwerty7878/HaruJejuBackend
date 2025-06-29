package com.goodda.jejuday.auth.dto.register.request;

import com.goodda.jejuday.auth.entity.Gender;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoFinalRegisterRequest {
    private String code;
    private String nickname;
    private List<String> themes;
    private Gender gender;
}

