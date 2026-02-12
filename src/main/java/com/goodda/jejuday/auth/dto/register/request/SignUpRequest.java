package com.goodda.jejuday.auth.dto.register.request;

import com.goodda.jejuday.auth.entity.Gender;
import com.goodda.jejuday.common.annotation.valid.ValidNickname;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class SignUpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 12, message = "비밀번호는 8자 이상 12자 이하여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z\\d])(?=.*[!_@-])[a-zA-Z\\d!_@-]{8,12}$",
            message = "비밀번호는 8~12자 이내이며, 영문 또는 숫자를 포함하고, 특수문자(!, _, @, -)를 하나 이상 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    @ValidNickname
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @NotBlank(message = "출생연도는 필수입니다.")
    @Pattern(regexp = "^(19[0-9]{2}|20[0-2][0-9])$", message = "1900~2029 사이의 연도를 입력해주세요")
    private String birthYear;

    // 선택 사항
    private List<String> themes;

    // 추천인 닉네임 (선택사항)
    @Size(max = 20, message = "추천인 닉네임은 20자 이하여야 합니다.")
    @Builder.Default
    private String referrerNickname = "제주데이";
}
