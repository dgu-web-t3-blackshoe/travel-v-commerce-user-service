package com.travelvcommerce.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import net.bytebuddy.asm.Advice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
public class SellerDto {
    private String email;
    private String password;
    private String sellerName;
    private byte[] sellerLogo;
    private String sellerId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String formattedCreatedAt;
    private String formattedUpdatedAt;

    @Data
    public static class SellerRegisterRequestDto {
        private String email;
        private String password;
        private String sellerName;
        private byte[] sellerLogo;
    }
    @Data
    public static class SellerLoginRequestDto {
        private String email;
        private String password;
    }

    @Data
    public static class SellerUpdateRequestDto{
        private String sellerName;
    }

    @Data
    public static class SellerJoinRequestDto{
        private String email;
        private String password;
        private String sellerName;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SellerInfoDto {
        private String email;
        private String sellerName;
        private byte[] sellerLogo;
        private String sellerId;
    }

    @Data
    @Builder
    public static class SellerRegisterResponseDto{
        private String sellerId;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class SellerUpdateResponseDto{
        private String sellerId;
        private LocalDateTime updatedAt;
    }
    @Data
    public static class SellerUpdatePasswordRequestDto{
        private String email;
        private String oldPassword;
        private String newPassword;
    }

    @Data
    public static class SellerFindPasswordRequestDto{
        private String email;
        private String password;
    }

    @Data
    public static class SellerDeleteRequestDto {
        private String password;
    }
}
