package com.jokerhub.paper.plugin.orzmc.events;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;

public class QQBotEvent implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            InputStream is = exchange.getRequestBody();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine()) != null) {
                sb.append(line);
            }
            // 请求的JSON参数解析
            String jsonString = sb.toString();
            JSONParser jsonParser = new JSONParser();
            JSONObject json = (JSONObject) jsonParser.parse(jsonString);
            OrzMC.logger().info(json.toString());
            OutputStream os = exchange.getResponseBody();
            exchange.sendResponseHeaders(200,0);
            exchange.getResponseBody().close();
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }
}
