package ru.mirea.aquarium.backend.team.Skvorcsov.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Adapter to the team's future authentication filter. Never trusts roles or author names
 * supplied in headers/body. With no authenticated servlet principal, access fails closed.
 */
@Component
public class FishPermissions {
    private final HttpServletRequest request;

    public FishPermissions(HttpServletRequest request) { this.request = request; }

    public String viewer() { return require("VIEWER", "EDITOR", "ADMIN"); }
    public String editor() { return require("EDITOR", "ADMIN"); }
    public String admin() { return require("ADMIN"); }

    private String require(String... roles) {
        if (request.getUserPrincipal() == null) throw new FishFailure(401, "Требуется авторизация");
        for (String role : roles) {
            if (request.isUserInRole(role) || request.isUserInRole("ROLE_" + role))
                return request.getUserPrincipal().getName();
        }
        throw new FishFailure(403, "Недостаточно прав");
    }
}
