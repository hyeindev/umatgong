package com.umatgong.domain.club.dto;

/**
 * @param inviteCode 초대 코드. 프론트가 초대 링크를 만들어 공유한다
 */
public record InviteResponse(String inviteCode, long memberCount, int maxMembers) {
}
