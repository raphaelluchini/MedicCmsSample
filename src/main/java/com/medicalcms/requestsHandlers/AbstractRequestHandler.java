package com.medicalcms.requestsHandlers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.medicalcms.Answer;
import com.medicalcms.RequestHandler;
import com.medicalcms.Validable;
import com.medicalcms.EmptyPayload;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public abstract class AbstractRequestHandler<V extends Validable> implements RequestHandler<V>, Route {

    private Class<V> valueClass;

    private static final int HTTP_BAD_REQUEST = 400;

    public AbstractRequestHandler(Class<V> valueClass){
        this.valueClass = valueClass;
    }

    private static boolean shouldReturnHtml(Request request) {
        String accept = request.headers("Accept");
        return accept != null && accept.contains("text/html");
    }

    public static String toHandlebars(ModelAndView modelAndView){
        return new HandlebarsTemplateEngine().render(modelAndView);
    }

    public static String dataToJson(Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, data);
            return sw.toString();
        } catch (IOException e){
            throw new RuntimeException("IOException from a StringWriter?");
        }
    }

    public final Answer process(V value, Map<String, String> urlParams, boolean shouldReturnHtml) {
        if (value != null && !value.isValid()) {
            return new Answer(HTTP_BAD_REQUEST);
        } else {
            return processImpl(value, urlParams, shouldReturnHtml);
        }
    }

    protected abstract Answer processImpl(V value, Map<String, String> urlParams, boolean shouldReturnHtml);

    @Override
    public Object handle(Request request, Response response) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            V value = null;
            if (valueClass != EmptyPayload.class) {
                value = objectMapper.readValue(request.body(), valueClass);
            }
            Map<String, String> urlParams = request.params();
            Answer answer = process(value, urlParams, shouldReturnHtml(request));
            response.status(answer.getCode());
            if (shouldReturnHtml(request)) {
                response.type("text/html");
            } else {
                response.type("application/json");
            }
            response.body(answer.getBody());
            return answer.getBody();
        } catch (JsonMappingException e) {
            response.status(400);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }

}
