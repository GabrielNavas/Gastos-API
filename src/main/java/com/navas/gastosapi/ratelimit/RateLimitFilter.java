package com.navas.gastosapi.ratelimit;

import com.navas.gastosapi.auth.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rotas /api/auth/** (login, registro, refresh): limite por IP, para frear forca bruta.
 * Demais rotas /api/**: limite por usuario autenticado (ou por IP se nao autenticado).
 * Usa getRemoteAddr() de proposito: confiar em X-Forwarded-For sem um proxy confiavel
 * permitiria burlar o limite forjando o header.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter generalLimiter;
    private final RateLimiter authLimiter;

    public RateLimitFilter(RateLimiter generalLimiter, RateLimiter authLimiter) {
        this.generalLimiter = generalLimiter;
        this.authLimiter = authLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        RateLimiter.Result result;
        if (request.getRequestURI().startsWith("/api/auth/")) {
            result = authLimiter.tryAcquire("ip:" + request.getRemoteAddr());
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String key = (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user)
                    ? "user:" + user.id()
                    : "ip:" + request.getRemoteAddr();
            result = generalLimiter.tryAcquire(key);
        }

        if (result.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":429,\"message\":\"Muitas requisições. Tente novamente em "
                + result.retryAfterSeconds() + " segundos.\"}");
    }
}
