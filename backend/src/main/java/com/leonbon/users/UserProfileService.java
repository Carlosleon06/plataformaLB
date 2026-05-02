package com.leonbon.users;

import com.leonbon.auth.ConflictException;
import com.leonbon.auth.JwtPrincipal;
import com.leonbon.infra.mongo.SequenceService;
import com.leonbon.users.dto.MeResponse;
import com.leonbon.tournaments.GameTitle;
import com.leonbon.users.dto.PatchMyProfileRequest;
import com.leonbon.users.dto.PlayerSocialLinksResponse;
import com.leonbon.web.BadRequestException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    private final UserRepository userRepository;
    private final SequenceService sequenceService;

    public UserProfileService(UserRepository userRepository, SequenceService sequenceService) {
        this.userRepository = userRepository;
        this.sequenceService = sequenceService;
    }

    public MeResponse toMe(User user) {
        ensureLeonNumberAssigned(user);

        Map<String, String> ranksCopy =
                user.getRankLabelByGame() == null
                        ? Map.of()
                        : Map.copyOf(user.getRankLabelByGame());

        PlayerSocialLinksResponse social =
                new PlayerSocialLinksResponse(
                        user.getTwitchProfileUrl(),
                        user.getYoutubeChannelUrl(),
                        user.getXProfileUrl(),
                        user.getInstagramProfileUrl(),
                        user.getDiscordHandle());

        UserRole role = user.getRole() == null ? UserRole.PLAYER : user.getRole();
        GameTitle pg = user.getPreferredGame();

        return new MeResponse(
                user.getId(),
                user.getLeonPlayerNumber(),
                user.getUsername(),
                user.getEmailNormalized(),
                user.getNickname(),
                user.getFullName(),
                user.isProfileShowFullName(),
                user.getCountry(),
                social,
                pg == null ? null : pg.name(),
                ranksCopy,
                user.getStatus(),
                role,
                user.getLeonCoinsBalance());
    }

    public void ensureLeonNumberAssigned(User user) {
        if (user.getLeonPlayerNumber() != null) {
            return;
        }
        long n = sequenceService.nextUserLeonPlayerNumber();
        user.setLeonPlayerNumber(n);
        user.setUpdatedAt(Instant.now());
        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("unable to allocate player number");
        }
    }

    public User patchProfile(JwtPrincipal principal, PatchMyProfileRequest body) {
        User user =
                userRepository.findById(principal.userId()).orElseThrow(() -> new BadRequestException("user not found"));
        Instant now = Instant.now();

        if (body.getNickname() != null) {
            String nn = trimToNull(body.getNickname());
            user.setNickname(nn);
        }

        if (body.getEmail() != null) {
            String normalized = normalizeEmail(body.getEmail());
            userRepository.findByEmailNormalized(normalized).ifPresent(other -> {
                if (!Objects.equals(other.getId(), user.getId())) {
                    throw new ConflictException("email already registered");
                }
            });
            user.setEmailNormalized(normalized);
        }

        if (body.getFullName() != null) {
            user.setFullName(trimToNull(body.getFullName()));
        }
        if (body.getProfileShowFullName() != null) {
            user.setProfileShowFullName(body.getProfileShowFullName());
        }
        if (body.getCountry() != null) {
            user.setCountry(trimToNull(body.getCountry()));
        }
        if (body.getTwitchProfileUrl() != null) user.setTwitchProfileUrl(trimToNull(body.getTwitchProfileUrl()));
        if (body.getYoutubeChannelUrl() != null)
            user.setYoutubeChannelUrl(trimToNull(body.getYoutubeChannelUrl()));
        if (body.getXProfileUrl() != null) user.setXProfileUrl(trimToNull(body.getXProfileUrl()));
        if (body.getInstagramProfileUrl() != null)
            user.setInstagramProfileUrl(trimToNull(body.getInstagramProfileUrl()));
        if (body.getDiscordHandle() != null) user.setDiscordHandle(trimToNull(body.getDiscordHandle()));

        if (body.getPreferredGame() != null) {
            user.setPreferredGame(body.getPreferredGame());
        }

        if (body.getRankLabelsByGame() != null) {
            LinkedHashMap<String, String> next = new LinkedHashMap<>();
            for (Map.Entry<String, String> row : body.getRankLabelsByGame().entrySet()) {
                if (row.getKey() == null) continue;
                String k = row.getKey().trim().toUpperCase();
                String v = row.getValue() == null ? "" : row.getValue().trim();
                if (k.isEmpty()) continue;
                if (v.isEmpty()) continue;
                next.put(k, v);
            }
            user.setRankLabelByGame(next);
        }

        user.setUpdatedAt(now);
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("unique profile constraint conflict");
        }
    }

    public static PlayerSocialLinksResponse socialOf(User user) {
        return new PlayerSocialLinksResponse(
                user.getTwitchProfileUrl(),
                user.getYoutubeChannelUrl(),
                user.getXProfileUrl(),
                user.getInstagramProfileUrl(),
                user.getDiscordHandle());
    }

    public static String normalizeEmail(String raw) {
        String s = raw.trim().toLowerCase();
        if (s.length() > 120) {
            throw new BadRequestException("email too long");
        }
        if (!s.matches("^[\\w.+\\-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BadRequestException("invalid email");
        }
        return s;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
