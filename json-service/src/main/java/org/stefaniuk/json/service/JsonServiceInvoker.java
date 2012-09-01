package org.stefaniuk.json.service;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * JSON service invoker.
 * </p>
 * <p>
 * This class is responsible for building <a
 * href="http://livedocs.dojotoolkit.org/dojox/rpc/smd">Service Mapping
 * Description</a>, processing request and calling the method. It is created by
 * the {@link JsonServiceRegistry} as a wrapper of JSON-RPC class and stored in
 * the registry.
 * </p>
 * 
 * @author Daniel Stefaniuk
 * @version 1.0.0
 * @since 2010/09/20
 */
public class JsonServiceInvoker {

    private final Logger logger = LoggerFactory.getLogger(JsonServiceInvoker.class);

    /**
     * This object provides functionality for conversion between Java objects
     * and JSON.
     */
    private static ObjectMapper mapper = new ObjectMapper();

    /** Indicates if object has been initialised. */
    private boolean isInitialised = false;

    /** This is registered JSON-RPC class. */
    private final Class<?> clazz;

    /** This is registered JSON-RPC object. */
    private Object context;

    /** Collection of all JSON-RPC method exposed to a client. */
    private Map<String, Method> methods = new HashMap<String, Method>();;

    /**
     * The transport property defines the transport mechanism to be used to
     * deliver service calls to server.
     */
    private Transport transport = Transport.POST;

    /** This is the expected content type of the content returned by a service. */
    private ContentType contentType = ContentType.APPLICATION_JSON;

    /**
     * Envelope defines how a service message string is created from the
     * provided parameters.
     */
    private Envelope envelope = Envelope.JSON_RPC_2_0;

    /** Version of Service Mapping Description */
    private Version version = Version.SMD_2_0;

    /** Service Mapping Description */
    private ObjectNode smd;

    /**
     * JSON-RPC transport type.
     * 
     * @author Daniel Stefaniuk
     */
    public static enum Transport {

        UNDEFINED(""),
        POST("POST");

        private final String name;

        Transport(String name) {

            this.name = name;
        }

        @Override
        public String toString() {

            return this.name;
        }
    }

    /**
     * JSON-RPC response content type.
     * 
     * @author Daniel Stefaniuk
     */
    public static enum ContentType {

        UNDEFINED(""),
        APPLICATION_JSON("application/json");

        private final String name;

        ContentType(String name) {

            this.name = name;
        }

        @Override
        public String toString() {

            return this.name;
        }
    }

    /**
     * JSON-RPC envelope type.
     * 
     * @author Daniel Stefaniuk
     */
    public static enum Envelope {

        UNDEFINED(""),
        JSON_RPC_2_0("JSON-RPC-2.0");

        private final String name;

        Envelope(String name) {

            this.name = name;
        }

        @Override
        public String toString() {

            return this.name;
        }
    }

    /**
     * SMD version.
     * 
     * @author Daniel Stefaniuk
     */
    public static enum Version {

        UNDEFINED(""),
        SMD_2_0("2.0");

        private final String name;

        Version(String name) {

            this.name = name;
        }

        @Override
        public String toString() {

            return this.name;
        }
    }

    /**
     * JSON-RPC supported data types.
     * 
     * @author Daniel Stefaniuk
     */
    public static enum DataType {

        ARRAY(new Class<?>[] { ArrayList.class }),
        OBJECT(new Class<?>[] { LinkedHashMap.class }),
        NUMBER(new Class<?>[] { Float.class, Double.class }),
        INTEGER(new Class<?>[] { Integer.class, Long.class }),
        BOOLEAN(new Class<?>[] { Boolean.class }),
        STRING(new Class<?>[] { String.class });

        private final Class<?>[] classes;

        private DataType(Class<?>[] classes) {

            this.classes = classes;
        }

        public static String getName(Class<?> clazz) {

            // check all data types
            for(DataType type: DataType.values()) {
                for(Class<?> c: type.classes) {
                    if(clazz.equals(c)) {
                        return type.toString();
                    }
                }
            }

            // non of the above, so this must be POJO object
            return DataType.OBJECT.toString();
        }

        @Override
        public String toString() {

            return name().toLowerCase();
        }
    }

    /**
     * Constructor
     * 
     * @param clazz Class
     */
    public JsonServiceInvoker(Class<?> clazz) {

        this.clazz = clazz;
    }

    /**
     * Constructor
     * 
     * @param obj Object
     */
    public JsonServiceInvoker(Object obj) {

        this.clazz = obj.getClass();
        this.context = obj;
    }

    /**
     * Sets transport.
     * 
     * @param transport
     * @return Returns {@link JsonServiceInvoker} object.
     */
    public JsonServiceInvoker setTransport(Transport transport) {

        this.transport = transport;

        return this;
    }

    /**
     * Gets transport.
     * 
     * @return Returns {@link Transport} object.
     */
    public Transport getTransport() {

        return transport;
    }

    /**
     * Sets content type.
     * 
     * @param contentType
     * @return Returns {@link JsonServiceInvoker} object.
     */
    public JsonServiceInvoker setContentType(ContentType contentType) {

        this.contentType = contentType;

        return this;
    }

    /**
     * Gets content type.
     * 
     * @return Returns {@link ContentType} object.
     */
    public ContentType getContentType() {

        return contentType;
    }

    /**
     * Sets envelope.
     * 
     * @param envelope
     * @return Returns {@link JsonServiceInvoker} object.
     */
    public JsonServiceInvoker setEnvelope(Envelope envelope) {

        this.envelope = envelope;

        return this;
    }

    /**
     * Gets envelope.
     * 
     * @return Returns {@link Envelope} object.
     */
    public Envelope getEnvelope() {

        return envelope;
    }

    /**
     * Sets version.
     * 
     * @param version
     * @return Returns {@link JsonServiceInvoker} object.
     */
    public JsonServiceInvoker setVersion(Version version) {

        this.version = version;

        return this;
    }

    /**
     * Gets version.
     * 
     * @return Returns {@link Version} object.
     */
    public Version getVersion() {

        return version;
    }

    /**
     * This method creates an instance of registered JSON-RPC class and produces
     * Service Mapping Description.
     */
    private void init() {

        try {

            // create an instance
            if(context == null) {
                Constructor<?> ctor = clazz.getConstructor();
                context = ctor.newInstance();
            }

            // get methods
            for(Method method: clazz.getMethods()) {
                if(isService(clazz, method)) {
                    methods.put(method.getName(), method);
                }
            }

            // produce Service Mapping Description
            smd = mapper.createObjectNode();
            smd.put("transport", transport.toString());
            smd.put("contentType", contentType.toString());
            smd.put("envelope", envelope.toString());
            smd.put("SMDVersion", version.toString());
            smd.put("additionalParameters", false);

            ObjectNode services = mapper.createObjectNode();
            for(Method method: methods.values()) {

                ObjectNode temp = mapper.createObjectNode();

                JsonService annotation = method.getAnnotation(JsonService.class);
                if(annotation == null) {
                    annotation = clazz.getAnnotation(JsonService.class);
                }

                // transport
                Transport transport = annotation.transport();
                if(transport != Transport.UNDEFINED && transport != this.transport) {
                    temp.put("transport", transport.toString());
                }
                // contentType
                ContentType contentType = annotation.contentType();
                if(contentType != ContentType.UNDEFINED && contentType != this.contentType) {
                    temp.put("contentType", contentType.toString());
                }
                // envelope
                Envelope envelope = annotation.envelope();
                if(envelope != Envelope.UNDEFINED && envelope != this.envelope) {
                    temp.put("envelope", envelope.toString());
                }
                // target
                String target = annotation.target();
                if(!target.equals("")) {
                    temp.put("target", target);
                }
                // description
                String description = annotation.description();
                if(!description.equals("")) {
                    temp.put("description", description);
                }
                // parameters
                ArrayNode parameters = mapper.createArrayNode();
                for(Class<?> type: method.getParameterTypes()) {
                    ObjectNode node = mapper.createObjectNode();
                    node.put("type", DataType.getName(type));
                    parameters.add(node);
                }
                temp.put("parameters", parameters);
                // returns
                Class<?> returnType = method.getReturnType();
                if(!"void".equals(returnType.toString())) {
                    ObjectNode node = mapper.createObjectNode();
                    node.put("type", DataType.getName(returnType));
                    temp.put("returns", node);
                }

                services.put(method.getName(), temp);
            }
            smd.put("services", services);

        }
        catch(Exception e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Checks if a given method is defined as JSON-RPC method.
     * 
     * @param clazz Class
     * @param method Method
     * @return Returns true or false.
     */
    private boolean isService(Class<?> clazz, Method method) {

        boolean hasAnnotation = clazz.getAnnotation(JsonService.class) != null
            || method.getAnnotation(JsonService.class) != null;

        return hasAnnotation && Modifier.isPublic(method.getModifiers());
    }

    /**
     * Returns Service Mapping Description as JSON object.
     * 
     * @return Returns JSON object.
     */
    protected JsonNode getServiceMap() {

        if(!isInitialised) {
            init();
        }

        return smd;
    }

    /**
     * Processes JSON-RPC request.
     * 
     * @param request HTTP request
     * @param requestNode JSON-RPC request
     * @return Returns JSON object.
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    protected JsonNode process(HttpServletRequest request, ObjectNode requestNode) throws IllegalAccessException,
            InvocationTargetException, JsonParseException, JsonMappingException, IOException {

        // make sure this object has been initialised
        if(!isInitialised) {
            init();
        }

        JsonNode response = null;

        try {

            // get method name
            if(!requestNode.has("method") || "".equals(requestNode.get("method"))) {
                throw new JsonServiceException(JsonServiceError.INVALID_REQUEST);
            }
            String name = requestNode.get("method").getTextValue();

            // get reference of the method
            Method method = methods.get(name);
            if(method == null) {
                throw new JsonServiceException(JsonServiceError.METHOD_NOT_FOUND);
            }

            // get parameters
            ArrayNode params = ArrayNode.class.cast(requestNode.get("params"));
            ArrayList<Object> temp = new ArrayList<Object>();
            Type[] types = method.getGenericParameterTypes();
            int i = 0;
            for(Type type: types) {
                if(type.equals(HttpServletRequest.class)) {
                    temp.add(request);
                }
                else {
                    JsonNode node = params.get(i++);
                    temp.add(mapper.treeToValue(node, TypeFactory.type(type).getRawClass()));
                }
            }
            Object[] args = new Object[temp.size()];
            temp.toArray(args);

            // invoke method
            logger.debug("JSON-RPC method call: " + method.getClass().getName() + "." + method.getName());
            try {
                response = getJsonRpcSuccessResponse(requestNode.get("id").getIntValue(), method.invoke(context, args));
            }
            catch(IllegalArgumentException e) {
                throw new JsonServiceException(JsonServiceError.INVALID_PARAMS);
            }
        }
        catch(InvocationTargetException e) {
            Throwable t = e.getCause();
            if(t instanceof JsonServiceException) {
                JsonServiceException jse = (JsonServiceException) t;
                response = getJsonRpcErrorResponse(requestNode.get("id").getIntValue(), jse.getError());
            }
            else {
                throw e;
            }
        }
        catch(JsonServiceException e) {
            response = getJsonRpcErrorResponse(requestNode.get("id").getIntValue(), e.getError());
        }

        return response;
    }

    /**
     * Processes request.
     * 
     * @param request HTTP request
     * @param method Method name
     * @return Returns JSON object.
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    protected JsonNode process(HttpServletRequest request, String method, Object... args)
            throws IllegalAccessException,
            InvocationTargetException, JsonParseException, JsonMappingException, IOException {

        // make sure this object has been initialised
        if(!isInitialised) {
            init();
        }

        JsonNode response = null;

        try {

            // get reference of the method
            Method m = methods.get(method);
            if(m == null) {
                throw new JsonServiceException(JsonServiceError.METHOD_NOT_FOUND);
            }

            // get parameters
            ArrayList<Object> temp = new ArrayList<Object>();
            Type[] types = m.getGenericParameterTypes();
            int i = 0;
            for(Type type: types) {
                if(type.equals(HttpServletRequest.class)) {
                    temp.add(request);
                }
                else {
                    temp.add(args[i++]);
                }
            }
            Object[] arguments = new Object[temp.size()];
            temp.toArray(arguments);

            // invoke method
            logger.debug("JSON-RPC method call: " + m.getClass().getName() + "." + m.getName());
            try {
                response = getJsonSuccessResponse(m.invoke(context, arguments));
            }
            catch(IllegalArgumentException e) {
                throw new JsonServiceException(JsonServiceError.INVALID_PARAMS);
            }
        }
        catch(InvocationTargetException e) {
            Throwable t = e.getCause();
            if(t instanceof JsonServiceException) {
                JsonServiceException jse = (JsonServiceException) t;
                response = getJsonErrorResponse(jse.getError());
            }
            else {
                throw e;
            }
        }
        catch(JsonServiceException e) {
            response = getJsonErrorResponse(e.getError());
        }

        return response;
    }

    /**
     * Returns response.
     * 
     * @param result Result object
     * @return Returns response object as JSON.
     */
    private JsonNode getJsonSuccessResponse(Object result) {

        // set result
        if(result instanceof Map<?, ?>) {
            return toJson((Map<?, ?>) result);
        }
        else {
            return new POJONode(result);
        }
    }

    /**
     * Returns error response.
     * 
     * @param error Error object
     * @return Returns response object as JSON.
     */
    private JsonNode getJsonErrorResponse(JsonServiceError error) {

        ObjectNode errorNode = mapper.createObjectNode();
        errorNode.put("code", error.getCode());
        errorNode.put("message", error.getMessage());

        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("error", errorNode);

        return responseNode;
    }

    /**
     * Returns successful JSON-RPC response.
     * 
     * @param id JSON-RPC request ID
     * @param result Result object
     * @return Returns response object as JSON.
     */
    private JsonNode getJsonRpcSuccessResponse(Integer id, Object result) {

        ObjectNode responseNode = mapper.createObjectNode();

        // set id
        if(id != null) {
            responseNode.put("id", id);
        }
        else {
            responseNode.putNull("id");
        }

        // set result
        if(result instanceof List<?>) {
            responseNode.put("result", toJson((List<?>) result));
        }
        else if(result instanceof Map<?, ?>) {
            responseNode.put("result", toJson((Map<?, ?>) result));
        }
        else {
            try {
                responseNode.put("result", new POJONode(result));
            }
            catch(Exception e) {
                responseNode.put("result", result != null ? result.toString() : null);
            }
        }

        responseNode.putNull("error");
        responseNode.put("jsonrpc", envelope.toString());

        return responseNode;
    }

    /**
     * Returns error JSON-RPC response.
     * 
     * @param id JSON-RPC request ID
     * @param error Error object
     * @return Returns response object as JSON.
     */
    private JsonNode getJsonRpcErrorResponse(Integer id, JsonServiceError error) {

        ObjectNode errorNode = mapper.createObjectNode();
        errorNode.put("code", error.getCode());
        errorNode.put("message", error.getMessage());

        ObjectNode responseNode = mapper.createObjectNode();
        if(id != null) {
            responseNode.put("id", id);
        }
        else {
            responseNode.putNull("id");
        }
        responseNode.putNull("result");
        responseNode.put("error", errorNode);
        responseNode.put("jsonrpc", envelope.toString());

        return responseNode;
    }

    /**
     * Converts a list to JSON array.
     * 
     * @param list List
     * @return Returns JSON array.
     */
    private ArrayNode toJson(List<?> list) {

        ArrayNode node = mapper.createArrayNode();

        for(Object obj: list) {
            if(obj instanceof Number) {
                // most probable
                if(obj instanceof Integer) {
                    node.add((Integer) obj);
                }
                else if(obj instanceof Double) {
                    node.add((Double) obj);
                }
                // others
                else if(obj instanceof BigDecimal) {
                    node.add((BigDecimal) obj);
                }
                else if(obj instanceof Float) {
                    node.add((Float) obj);
                }
                else if(obj instanceof Long) {
                    node.add((Long) obj);
                }
                else if(obj instanceof Short) {
                    node.add((Short) obj);
                }
                else if(obj instanceof Byte) {
                    node.add((Byte) obj);
                }
            }
            else if(obj instanceof Boolean) {
                node.add((Boolean) obj);
            }
            else if(obj instanceof String) {
                node.add((String) obj);
            }
            else if(obj instanceof List<?>) {
                node.add(toJson((List<?>) obj));
            }
            else if(obj instanceof Map<?, ?>) {
                node.add(toJson((Map<?, ?>) obj));
            }
            else {
                try {
                    node.addPOJO(obj);
                }
                catch(Exception e) {
                    node.add(obj != null ? obj.toString() : null);
                }
            }
        }

        return node;
    }

    /**
     * Converts a map to JSON object.
     * 
     * @param map Map
     * @return Returns JSON object.
     */
    private ObjectNode toJson(Map<?, ?> map) {

        ObjectNode node = mapper.createObjectNode();

        for(Object key: map.keySet()) {
            String name = (String) key;
            Object obj = map.get(name);

            if(obj instanceof Number) {
                // most probable
                if(obj instanceof Integer) {
                    node.put(name, (Integer) obj);
                }
                else if(obj instanceof Double) {
                    node.put(name, (Double) obj);
                }
                // others
                else if(obj instanceof BigDecimal) {
                    node.put(name, (BigDecimal) obj);
                }
                else if(obj instanceof Float) {
                    node.put(name, (Float) obj);
                }
                else if(obj instanceof Long) {
                    node.put(name, (Long) obj);
                }
                else if(obj instanceof Short) {
                    node.put(name, (Short) obj);
                }
                else if(obj instanceof Byte) {
                    node.put(name, (Byte) obj);
                }
            }
            else if(obj instanceof Boolean) {
                node.put(name, (Boolean) obj);
            }
            else if(obj instanceof String) {
                node.put(name, (String) obj);
            }
            else if(obj instanceof List<?>) {
                node.put(name, toJson((List<?>) obj));
            }
            else if(obj instanceof Map<?, ?>) {
                node.put(name, toJson((Map<?, ?>) obj));
            }
            else {
                try {
                    node.putPOJO(name, obj);
                }
                catch(Exception e) {
                    node.put(name, obj != null ? obj.toString() : null);
                }
            }
        }

        return node;
    }

}
