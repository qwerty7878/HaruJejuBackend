package com.goodda.jejuday.auth.dto.register.request;

import com.goodda.jejuday.auth.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "출생연도는 필수입니다.")
    @Pattern(regexp = "^(19[0-9]{2}|20[0-2][0-9])$", message = "1900~2029 사이의 연도를 입력해주세요")
    private String birthYear;
}

