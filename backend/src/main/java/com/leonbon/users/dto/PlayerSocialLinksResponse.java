package com.leonbon.users.dto;

public record PlayerSocialLinksResponse(
        String twitchUrl, String youtubeUrl, String xUrl, String instagramUrl, String discord
) {}
