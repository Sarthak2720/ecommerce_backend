package com.styliste.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // DEBUG: Trace which URL is being hit
            String requestURI = request.getRequestURI();

            // Skip logging for public static files to keep console clean
            if (!requestURI.contains("/api/")) {
                filterChain.doFilter(request, response);
                return;
            }

            System.out.println("--- FILTER START: " + requestURI + " ---");

            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                System.out.println("1. JWT Found: " + jwt.substring(0, Math.min(jwt.length(), 15)) + "...");

                // Validate
                boolean isValid = tokenProvider.validateToken(jwt);

                if (isValid) {
                    System.out.println("2. Token is VALID");
                    String username = tokenProvider.getUsernameFromJwt(jwt);
                    System.out.println("3. Username extracted: " + username);

                    var userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // CRITICAL STEP: Actually logging the user in
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("4. Security Context Set: SUCCESS");

                } else {
                    System.out.println("!!! TOKEN INVALID !!! (Check JwtTokenProvider logs for exact reason)");
                }
            } else {
                System.out.println("!!! NO TOKEN FOUND !!!");
                System.out.println("Header Value: " + request.getHeader("Authorization"));
            }
        } catch (Exception ex) {
            System.out.println("!!! FILTER EXCEPTION !!!");
            ex.printStackTrace();
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
        System.out.println("--- FILTER END ---");
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/uploads/");
    }


    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}