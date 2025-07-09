package com.goodda.jejuday.auth.dto.register.request;

import com.goodda.jejuday.common.annotation.valid.ValidNickname;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalAppRegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank(message = "닉네임은 필수입니다.")
    @ValidNickname
    private String nickname;

    private List<String> themes;

    private String profile;

    @NotBlank(message = "출생연도는 필수입니다.")
    @Pattern(regexp = "^(19[0-9]{2}|20[0-2][0-9])$", message = "1900~2029 사이의 연도를 입력해주세요")
    private String birthYear;
}
