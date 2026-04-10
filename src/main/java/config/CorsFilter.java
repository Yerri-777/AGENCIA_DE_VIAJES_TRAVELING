package config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "CorsFilter", urlPatterns = {"/*"}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 1. Obtener el origen de la petición (Angular)
        String origin = req.getHeader("Origin");
        
        // 2. Si el origen es localhost:4200, lo devolvemos tal cual (NUNCA *)
        if (origin != null && origin.contains("localhost:4200")) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            System.out.println("[CorsFilter] setting Access-Control-Allow-Origin to " + origin);
        } else {
            // Fallback por si acaso, pero siempre específico
            resp.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
            System.out.println("[CorsFilter] origin missing or different, defaulting Access-Control-Allow-Origin to http://localhost:4200");
        }

        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Max-Age", "3600");

        // 3. Manejo de Preflight (Vital: el navegador pregunta antes de enviar el archivo)
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig f) {}
    @Override public void destroy() {}
}