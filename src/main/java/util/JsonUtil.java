package util;

import com.google.gson.Gson;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServletResponse;

public class JsonUtil {
    private static final Gson gson = new Gson();

    public static void enviarComoJson(HttpServletResponse response, Object data) throws Exception {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonString = gson.toJson(data);
        PrintWriter out = response.getWriter();
        out.print(jsonString);
        out.flush();
    }
}