package io.resttestgen.core.openapi;

import io.resttestgen.core.TestingOperationGenerator;
import io.resttestgen.core.datatype.HTTPMethod;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.resttestgen.core.openapi.Helper.getJSONMap;
import static org.junit.jupiter.api.Assertions.*;

public class TestParameters {

    private static final Logger logger = LogManager.getLogger(TestParameters.class);

    private static Map<String, Object> mapWalk(Map<String, Object> map, LinkedList<String> fields) {
        if (fields.isEmpty()) {
            return map;
        }

        return mapWalk((Map<String, Object>) map.get(fields.pollFirst()), fields);
    }

    private static ParameterElement getParameterByName(String name, Collection<ParameterElement> collection) {
        return collection.stream().filter(e -> e.getName().toString().equals(name)).findFirst().get();
    }

    @BeforeAll
    public static void setNormalizer() /*throws NoSuchFieldException*/ {
        Helper.setNormalizer();
        /*
        // Make accessible private fields of ParameterElement instances
        ParameterElement.class.getDeclaredField("name").setAccessible(true);
        ParameterElement.class.getDeclaredField("normalizedName").setAccessible(true);
        ParameterElement.class.getDeclaredField("required").setAccessible(true);
        ParameterElement.class.getDeclaredField("type").setAccessible(true);
        ParameterElement.class.getDeclaredField("format").setAccessible(true);
        ParameterElement.class.getDeclaredField("location").setAccessible(true);
        ParameterElement.class.getDeclaredField("defaultValue").setAccessible(true);
        ParameterElement.class.getDeclaredField("enumValues").setAccessible(true);
        ParameterElement.class.getDeclaredField("examples").setAccessible(true);
        ParameterElement.class.getDeclaredField("operation").setAccessible(true);
        ParameterElement.class.getDeclaredField("parent").setAccessible(true);*/
    }

    @Test
    public void testPetJSONParameterEquals() throws InvalidOpenAPIException, IOException {
        logger.info("Test equals/hashCode functions on petstore JSON parameters");
        String endpoint = "/pets";
        HTTPMethod method = HTTPMethod.GET;

        Operation op1 = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/getPets.json"));
        Operation op2 = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/getPets.json"));
        Operation op3 = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/getPets_unordered.json"));
        Operation op4; // Operation with differences

        // Prepare for custom modification to operation map
        Map<String, Object> modOperation = getJSONMap("build/resources/test/operations/getPets.json");
        String[] walk2Props = {"responses", "200", "content", "application/json", "schema", "items", "properties"};
        LinkedList<String> fields;

        // Same object
        assertEquals(op1.getOutputParameters().get("200"), op1.getOutputParameters().get("200"));
        assertEquals(op1.getOutputParameters().get("200").hashCode(), op1.getOutputParameters().get("200").hashCode());

        // Different parsings
        assertEquals(op1.getOutputParameters().get("200"), op2.getOutputParameters().get("200"));
        assertEquals(op1.getOutputParameters().get("200").hashCode(), op2.getOutputParameters().get("200").hashCode());

        // Unordered
        assertEquals(op1.getOutputParameters().get("200"), op3.getOutputParameters().get("200"));
        assertEquals(op1.getOutputParameters().get("200").hashCode(), op3.getOutputParameters().get("200").hashCode());

        // Equals transitivity
        assertEquals(op2.getOutputParameters().get("200"), op3.getOutputParameters().get("200"));
        assertEquals(op2.getOutputParameters().get("200").hashCode(), op3.getOutputParameters().get("200").hashCode());

        // Types must be the same in equals for parameters
        fields = new LinkedList<>(Arrays.asList(walk2Props));
        fields.add("id");
        Map<String, Object> id = mapWalk(modOperation, fields);
        id.put("type", "string");
        op4 = new Operation(endpoint, method, modOperation);
        assertNotEquals(op1.getOutputParameters().get("200"), op4.getOutputParameters().get("200"));
        fields = new LinkedList<>(Arrays.asList(walk2Props));
        fields.add("id");
        id = mapWalk(modOperation, fields);
        id.put("type", "integer");

        // Must have same structure (missing one child)
        fields = new LinkedList<>(Arrays.asList(walk2Props));
        mapWalk(modOperation, fields).remove("category");
        op4 = new Operation(endpoint, method, modOperation);
        assertNotEquals(op1.getOutputParameters().get("200"), op4.getOutputParameters().get("200"));

    }

    @Test
    public void testPetParameterEquals() throws IOException, InvalidOpenAPIException {
        logger.info("Test equals/hashCode functions on petstore simple parameters");
        String endpoint = "/pet/{petId}";
        HTTPMethod method = HTTPMethod.DELETE;

        Operation op1 = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/deletePetId.json"));
        Operation op2 = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/deletePetId.json"));
        Operation op3 = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/deletePetId_unordered.json"));
        Operation op4;

        // Prepare for custom modification to operation map
        Map<String, Object> modOperation = getJSONMap("build/resources/test/operations/deletePetId.json");

        // Same object
        assertEquals(op1.getInputParametersSet(), op1.getInputParametersSet());
        assertEquals(op1.getInputParametersSet().hashCode(), op1.getInputParametersSet().hashCode());

        // Different parsings
        assertEquals(op1.getInputParametersSet(), op2.getInputParametersSet());
        assertEquals(op1.getInputParametersSet().hashCode(), op2.getInputParametersSet().hashCode());

        // Unordered
        assertEquals(op1.getInputParametersSet(), op3.getInputParametersSet());
        assertEquals(op1.getInputParametersSet().hashCode(), op3.getInputParametersSet().hashCode());

        // Equals transitivity
        assertEquals(op2.getInputParametersSet(), op3.getInputParametersSet());
        assertEquals(op2.getInputParametersSet().hashCode(), op3.getInputParametersSet().hashCode());

        // Parameters must be in the same place
        Map<String, Object> name = (Map<String, Object>) ((List) modOperation.get("parameters")).get(0);
        name.put("in", "query");
        op4 = new Operation(endpoint, method, modOperation);

        assertNotEquals(op1.getInputParametersSet(), op4.getInputParametersSet());

        name.put("in", "path");

        // Parameters must be all the same
        Map<String, Object> newParam = new HashMap<>();
        newParam.put("name", "newparam");
        newParam.put("in", "query");
        ((List) modOperation.get("parameters")).add(newParam);
        op4 = new Operation(endpoint, method, modOperation);

        assertNotEquals(op1.getInputParametersSet(), op4.getInputParametersSet());

        ((List) modOperation.get("parameters")).remove(newParam);

    }

    /**
     * FIXME: should use int values, not doubles!!
     * Currently Gson deserializes every number as a double, but this behavior could lead to test apis
     * in a different manner from what is expected.
     * https://stackoverflow.com/questions/17090589/gson-deserialize-integers-as-integers-and-not-as-doubles
     */
    @Test
    public void testParameterExamples() throws IOException, InvalidOpenAPIException {
        logger.info("Test parameter examples parsing");

        String endpoint = "/pets";
        HTTPMethod method = HTTPMethod.POST;
        Operation o = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/postPets_examples.json"));

        Set<Object> examples;
        HashMap<String, Object> objectExampleValue = new HashMap<>();
        List<Object> listExampleValue = new LinkedList<>();

        // Header parameters
        Set<ParameterElement> headerParameters = o.getHeaderParameters();
        examples = getParameterByName("api_key", headerParameters).getExamples();
        assertTrue(examples.contains("this_is_a_key_example"));
        assertTrue(examples.size() == 1);

        // Query parameters
        Set<ParameterElement> queryParameters = o.getQueryParameters();
        examples = getParameterByName("paramNumber", queryParameters).getExamples();
        assertTrue(examples.contains(1.0));
        assertTrue(examples.contains(2.0));
        assertTrue(examples.contains(3.0));
        assertTrue(examples.size() == 3);

        // Request body
        ParameterObject body = (ParameterObject) o.getRequestBody();

        // Root level object (request body)
        examples = body.getExamples();
        objectExampleValue.put("id", 123123.0);
        objectExampleValue.put("category",
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 234234.0),
                        new AbstractMap.SimpleEntry<>("name", "categoryName!")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        objectExampleValue.put("name", "firstLevelName");
        objectExampleValue.put("photoUrls", new LinkedList<>(Arrays.asList("root_url")));
        objectExampleValue.put("tags", new LinkedList<>());
        Map<String, Object> firstTag = new HashMap<>(
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 1.0),
                        new AbstractMap.SimpleEntry<>("name", "firstTag")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        Map<String, Object> secondTag = new HashMap<>(
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 2.0),
                        new AbstractMap.SimpleEntry<>("name", "secondTag")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        ((List) objectExampleValue.get("tags")).add(firstTag);
        ((List) objectExampleValue.get("tags")).add(secondTag);
        assertTrue(examples.contains(objectExampleValue));
        objectExampleValue.clear();

        // Root level id
        examples = getParameterByName("id", body.getProperties()).getExamples();
        assertTrue(examples.contains(5.0));
        assertTrue(examples.contains(123123.0));
        assertTrue(examples.size() == 2);

        // Root level category object
        ParameterObject category = (ParameterObject) getParameterByName("category", body.getProperties());
        examples = category.getExamples();
        objectExampleValue.put("id", 234234.0);
        objectExampleValue.put("name", "categoryName!");
        assertTrue(examples.contains(objectExampleValue));
        objectExampleValue.clear();

        // 2nd level, category -> id
        examples = getParameterByName("id", category.getProperties()).getExamples();
        assertTrue(examples.contains(234234.0));
        assertTrue(examples.size() == 1);

        // 2nd level, category -> name
        examples = getParameterByName("name", category.getProperties()).getExamples();
        assertTrue(examples.contains("categoryName!"));
        assertTrue(examples.contains("category_name"));
        assertTrue(examples.size() == 2);

        // Root level name string
        examples = getParameterByName("name", body.getProperties()).getExamples();
        assertTrue(examples.contains("firstLevelName"));
        assertTrue(examples.contains("doggie"));
        assertTrue(examples.size() == 2);

        // Root level photoUrls array
        ParameterArray photoUrls = (ParameterArray) getParameterByName("photoUrls", body.getProperties());
        examples = photoUrls.getExamples();
        assertTrue(examples.contains(new LinkedList<>(Arrays.asList("root_url"))));
        assertTrue(examples.contains(new LinkedList<>(Arrays.asList("url_1", "url_2"))));
        assertTrue(examples.size() == 2);

        // 2nd level, photoUrls -> photoUrl (reference element for photoUrls array)
        examples = photoUrls.getReferenceElement().getExamples();
        assertTrue(examples.contains("root_url"));
        assertTrue(examples.contains("url_1"));
        assertTrue(examples.contains("url_2"));
        assertTrue(examples.size() == 3);

        // Root level tags array
        ParameterArray tags = (ParameterArray) getParameterByName("tags", body.getProperties());
        examples = tags.getExamples();
        listExampleValue.add(firstTag);
        listExampleValue.add(secondTag);
        assertTrue(examples.contains(listExampleValue));
        listExampleValue.clear();
        Map<String, Object> bigTag = new HashMap<>(
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 999.0),
                        new AbstractMap.SimpleEntry<>("name", "bigTag")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        listExampleValue.add(bigTag);
        assertTrue(examples.contains(listExampleValue));
        listExampleValue.clear();
        assertTrue(examples.size() == 2);

        // 2nd level, tags -> tag (reference element for tags array)
        ParameterObject tag = (ParameterObject) tags.getReferenceElement();
        examples = tag.getExamples();
        assertTrue(examples.contains(firstTag));
        assertTrue(examples.contains(secondTag));
        assertTrue(examples.contains(bigTag));
        assertTrue(examples.size() == 3);

        // 3rd level, tags -> tag -> id
        examples = getParameterByName("id", tag.getProperties()).getExamples();
        assertTrue(examples.contains(1.0));
        assertTrue(examples.contains(2.0));
        assertTrue(examples.contains(999.0));
        assertTrue(examples.contains(333.0));
        assertTrue(examples.size() == 4);

        // 3rd level, tags -> tag -> name
        examples = getParameterByName("name", tag.getProperties()).getExamples();
        assertTrue(examples.contains("firstTag"));
        assertTrue(examples.contains("secondTag"));
        assertTrue(examples.contains("bigTag"));
        assertTrue(examples.size() == 3);

        // Root level status string
        examples = getParameterByName("status", body.getProperties()).getExamples();
        assertTrue(examples.isEmpty());
    }

    @Test
    public void testParameterEnumAtRootLevel() throws IOException, InvalidOpenAPIException {
        logger.info("Test parameter enums (at root level) parsing");

        String endpoint = "/pets";
        HTTPMethod method = HTTPMethod.POST;
        Operation o = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/postPets_enums.json"));

        Set<Object> enums;
        HashMap<String, Object> objectEnumValue = new HashMap<>();

        // Header parameters
        Set<ParameterElement> headerParameters = o.getHeaderParameters();
        enums = getParameterByName("api_key", headerParameters).getEnumValues();
        assertTrue(enums.contains("devKey"));
        assertTrue(enums.contains("prodKey"));
        assertTrue(enums.size() == 2);

        // Query parameters
        Set<ParameterElement> queryParameters = o.getQueryParameters();
        enums = getParameterByName("paramNumber", queryParameters).getEnumValues();
        assertTrue(enums.contains(1.0));
        assertTrue(enums.contains(2.0));
        assertTrue(enums.contains(3.0));
        assertTrue(enums.size() == 3);

        // Request body
        ParameterObject body = (ParameterObject) o.getRequestBody();
        enums = body.getEnumValues();
        objectEnumValue.put("id", 1.0);
        objectEnumValue.put("category",
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 2.0),
                        new AbstractMap.SimpleEntry<>("name", "cat")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        objectEnumValue.put("tags", new LinkedList<>());
        Map<String, Object> aTag = new HashMap<>(
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 1.0),
                        new AbstractMap.SimpleEntry<>("name", "aTag")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        Map<String, Object> bTag = new HashMap<>(
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 2.0),
                        new AbstractMap.SimpleEntry<>("name", "bTag")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        ((List) objectEnumValue.get("tags")).add(aTag);
        ((List) objectEnumValue.get("tags")).add(bTag);
        assertTrue(enums.contains(objectEnumValue));
        objectEnumValue.put("id", 2.0);
        Map<String, Object> cTag = new HashMap<>(
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 3.0),
                        new AbstractMap.SimpleEntry<>("name", "cTag")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        ((List) objectEnumValue.get("tags")).add(cTag);
        ((List) objectEnumValue.get("tags")).remove(bTag);
        assertTrue(enums.contains(objectEnumValue));
        assertTrue(enums.size() == 2);

        // Check empty enums for underlying fields
        enums = getParameterByName("id", body.getProperties()).getEnumValues();
        assertTrue(enums.isEmpty());
        ParameterObject category = (ParameterObject) getParameterByName("category", body.getProperties());
        enums = category.getEnumValues();
        assertTrue(enums.isEmpty());
        enums = getParameterByName("id", category.getProperties()).getEnumValues();
        assertTrue(enums.isEmpty());
        enums = getParameterByName("name", category.getProperties()).getEnumValues();
        assertTrue(enums.isEmpty());
        ParameterArray tags = (ParameterArray) getParameterByName("tags", body.getProperties());
        enums = tags.getEnumValues();
        assertTrue(enums.isEmpty());
        ParameterObject tag = (ParameterObject) tags.getReferenceElement();
        enums = tag.getEnumValues();
        assertTrue(enums.isEmpty());
        enums = getParameterByName("id", tag.getProperties()).getEnumValues();
        assertTrue(enums.isEmpty());
        enums = getParameterByName("name", tag.getProperties()).getEnumValues();
        assertTrue(enums.isEmpty());
    }

    @Test
    public void testParameterEnumAtLeafLevel() throws IOException, InvalidOpenAPIException {
        logger.info("Test parameter enums (at leaf level) parsing");

        String endpoint = "/pets";
        HTTPMethod method = HTTPMethod.PUT;
        Operation o = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/putPets_enums.json"));

        Set<Object> enums;
        HashMap<String, Object> objectEnumValue = new HashMap<>();

        // Header parameters
        Set<ParameterElement> headerParameters = o.getHeaderParameters();
        enums = getParameterByName("api_key", headerParameters).getEnumValues();
        assertTrue(enums.isEmpty());

        // Query parameters
        Set<ParameterElement> queryParameters = o.getQueryParameters();
        enums = getParameterByName("paramNumber", queryParameters).getEnumValues();
        assertTrue(enums.contains(12.0));
        assertTrue(enums.contains(21.0));
        assertTrue(enums.size() == 2);

        // Request body
        ParameterObject body = (ParameterObject) o.getRequestBody();

        // Root level object (request body)
        enums = body.getEnumValues();
        assertTrue(enums.isEmpty());

        // Root level id
        enums = getParameterByName("id", body.getProperties()).getEnumValues();
        assertTrue(enums.contains(1.0));
        assertTrue(enums.contains(2.0));
        assertTrue(enums.contains(3.0));
        assertTrue(enums.size() == 3);

        // Root level category object
        ParameterObject category = (ParameterObject) getParameterByName("category", body.getProperties());
        enums = category.getEnumValues();
        assertTrue(enums.isEmpty());

        // 2nd level, category -> id
        enums = getParameterByName("id", category.getProperties()).getEnumValues();
        assertTrue(enums.contains(4.0));
        assertTrue(enums.contains(5.0));
        assertTrue(enums.contains(6.0));
        assertTrue(enums.size() == 3);

        // 2nd level, category -> name
        enums = getParameterByName("name", category.getProperties()).getEnumValues();
        assertTrue(enums.contains("cat_1"));
        assertTrue(enums.contains("cat_2"));
        assertTrue(enums.contains("cat_3"));
        assertTrue(enums.size() == 3);

        // Root level tags array
        ParameterArray tags = (ParameterArray) getParameterByName("tags", body.getProperties());
        enums = tags.getEnumValues();
        assertTrue(enums.isEmpty());

        // 2nd level, tags -> tag (reference element for tags array)
        ParameterObject tag = (ParameterObject) tags.getReferenceElement();
        enums = tag.getEnumValues();
        assertTrue(enums.isEmpty());

        // 3rd level, tags -> tag -> id
        enums = getParameterByName("id", tag.getProperties()).getEnumValues();
        assertTrue(enums.contains(7.0));
        assertTrue(enums.contains(8.0));
        assertTrue(enums.contains(9.0));
        assertTrue(enums.size() == 3);

        // 3rd level, tags -> tag -> name
        enums = getParameterByName("name", tag.getProperties()).getEnumValues();
        assertTrue(enums.contains("aTag"));
        assertTrue(enums.contains("bTag"));
        assertTrue(enums.contains("cTag"));
        assertTrue(enums.size() == 3);
    }

    @Test
    public void testParameterDefault() throws IOException, InvalidOpenAPIException {
        logger.info("Test parameter default parsing");

        String endpoint = "/pets";
        HTTPMethod method = HTTPMethod.POST;
        Operation o = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/postPets_defaults.json"));

        Object def;
        Object defaultValue;

        // Header parameters
        Set<ParameterElement> headerParameters = o.getHeaderParameters();
        def = getParameterByName("api_key", headerParameters).getDefaultValue();
        assertTrue(def.equals("default_api_key"));

        // Query parameters
        Set<ParameterElement> queryParameters = o.getQueryParameters();
        def = getParameterByName("paramNumber", queryParameters).getDefaultValue();
        assertTrue(def.equals(909.0));

        // Request body
        ParameterObject body = (ParameterObject) o.getRequestBody();

        // Root level object (request body)
        def = body.getDefaultValue();
        defaultValue = new HashMap<String, Object>();
        ((Map) defaultValue).put("id", 664466.0);
        ((Map) defaultValue).put("category",
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 880088.0),
                        new AbstractMap.SimpleEntry<>("name", "categoryDefaultName")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        ((Map) defaultValue).put("name", "firstLevelDefault");
        ((Map) defaultValue).put("photoUrls", new LinkedList<>(Arrays.asList("url_1", "url_2")));
        ((Map) defaultValue).put("tags", new LinkedList<>());
        Map<String, Object> tag1 = new HashMap<>(
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 1.0),
                        new AbstractMap.SimpleEntry<>("name", "Tag1")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        Map<String, Object> tag2 = new HashMap<>(
                Stream.of(
                        new AbstractMap.SimpleEntry<>("id", 2.0),
                        new AbstractMap.SimpleEntry<>("name", "Tag2")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        ((List) ((Map) defaultValue).get("tags")).add(tag1);
        ((List) ((Map) defaultValue).get("tags")).add(tag2);
        assertTrue(def.equals(defaultValue));

        // Root level id
        def = getParameterByName("id", body.getProperties()).getDefaultValue();
        assertTrue(def.equals(5.0));

        // Root level category object
        ParameterObject category = (ParameterObject) getParameterByName("category", body.getProperties());
        def = category.getDefaultValue();
        defaultValue = new HashMap<String, Object>();
        ((Map) defaultValue).put("id", 3434.0);
        ((Map) defaultValue).put("name", "cat_def");
        assertTrue(def.equals(defaultValue));

        // 2nd level, category -> id
        def = getParameterByName("id", category.getProperties()).getDefaultValue();
        assertTrue(def.equals(33.0));

        // 2nd level, category -> name
        def = getParameterByName("name", category.getProperties()).getDefaultValue();
        assertTrue(def.equals("category_name"));

        // Root level name string
        def = getParameterByName("name", body.getProperties()).getDefaultValue();
        assertTrue(def.equals("doggie"));

        // Root level photoUrls array
        ParameterArray photoUrls = (ParameterArray) getParameterByName("photoUrls", body.getProperties());
        def = photoUrls.getDefaultValue();
        assertTrue(def.equals(new LinkedList<>(Arrays.asList("url_a", "url_b"))));

        // 2nd level, photoUrls -> photoUrl (reference element for photoUrls array)
        def = photoUrls.getReferenceElement().getDefaultValue();
        assertTrue(def == null);

        // Root level tags array
        ParameterArray tags = (ParameterArray) getParameterByName("tags", body.getProperties());
        def = tags.getDefaultValue();
        defaultValue = new LinkedList<Map<String, Object>>();
        ((List) defaultValue).add(
                new HashMap<>(
                        Stream.of(
                                new AbstractMap.SimpleEntry<>("id", 999.0),
                                new AbstractMap.SimpleEntry<>("name", "highTag")
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                )
        );
        assertTrue(def.equals(defaultValue));

        // 2nd level, tags -> tag (reference element for tags array)
        ParameterObject tag = (ParameterObject) tags.getReferenceElement();
        def = tag.getDefaultValue();
        assertTrue(def == null);

        // 3rd level, tags -> tag -> id
        def = getParameterByName("id", tag.getProperties()).getDefaultValue();
        assertTrue(def.equals(333.0));

        // 3rd level, tags -> tag -> name
        def = getParameterByName("name", tag.getProperties()).getDefaultValue();
        assertTrue(def.equals("aTag"));

        // Root level status string
        def = getParameterByName("status", body.getProperties()).getDefaultValue();
        assertTrue(def == null);
    }

    @Test
    public void testParameterElementDeepClone() throws IOException, InvalidOpenAPIException {
        logger.info("Test parameter deepClone function");

        String endpoint = "/pets";
        HTTPMethod method = HTTPMethod.POST;
        Operation o = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/postPets_full.json"));

        Object default1, default2;
        Set<Object> enum1, enum2;
        Set<Object> example1, example2;

        // Header parameters
        Set<ParameterElement> headerParameters = o.getHeaderParameters();
        ParameterElement header1 = getParameterByName("api_key", headerParameters);
        ParameterElement header2 = header1.deepClone();
        assertEquals(header1, header2);
        default1 = header1.getDefaultValue();
        enum1 = header1.getEnumValues();
        example1 = header1.getExamples();
        /*logger.debug("default1: " + default1);
        logger.debug("enum1: " + enum1);
        logger.debug("example1: " + example1);*/
        default2 = header2.getDefaultValue();
        enum2 = header2.getEnumValues();
        example2 = header2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // Query parameters
        Set<ParameterElement> queryParameters = o.getQueryParameters();
        ParameterElement query1 = getParameterByName("paramNumber", queryParameters);
        ParameterElement query2 = query1.deepClone();
        assertEquals(query1, query2);
        default1 = query1.getDefaultValue();
        enum1 = query1.getEnumValues();
        example1 = query1.getExamples();
        default2 = query2.getDefaultValue();
        enum2 = query2.getEnumValues();
        example2 = query2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // Request body
        ParameterObject body1 = (ParameterObject) o.getRequestBody();
        ParameterObject body2 = body1.deepClone();
        assertEquals(body1, body2);

        // Root level object (request body)
        default1 = body1.getDefaultValue();
        enum1 = body1.getEnumValues();
        example1 = body1.getExamples();
        default2 = body2.getDefaultValue();
        enum2 = body2.getEnumValues();
        example2 = body2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // Root level id
        ParameterElement id1 = getParameterByName("id", body1.getProperties());
        ParameterElement id2 = id1.deepClone();
        assertEquals(id1, id2);
        default1 = id1.getDefaultValue();
        enum1 = id1.getEnumValues();
        example1 = id1.getExamples();
        default2 = id2.getDefaultValue();
        enum2 = id2.getEnumValues();
        example2 = id2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // Root level category object
        ParameterObject category1 = (ParameterObject) getParameterByName("category", body1.getProperties());
        ParameterObject category2 = category1.deepClone();
        assertEquals(category1, category2);
        default1 = category1.getDefaultValue();
        enum1 = category1.getEnumValues();
        example1 = category1.getExamples();
        default2 = category2.getDefaultValue();
        enum2 = category2.getEnumValues();
        example2 = category2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // 2nd level, category -> id
        ParameterElement categoryId1 = getParameterByName("id", category1.getProperties());
        ParameterElement categoryId2 = categoryId1.deepClone();
        assertEquals(categoryId1, categoryId2);
        default1 = categoryId1.getDefaultValue();
        enum1 = categoryId1.getEnumValues();
        example1 = categoryId1.getExamples();
        default2 = categoryId2.getDefaultValue();
        enum2 = categoryId2.getEnumValues();
        example2 = categoryId2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // 2nd level, category -> name
        ParameterElement categoryName1 = getParameterByName("name", category1.getProperties());
        ParameterElement categoryName2 = categoryName1.deepClone();
        assertEquals(categoryName1, categoryName2);
        default1 = categoryName1.getDefaultValue();
        enum1 = categoryName1.getEnumValues();
        example1 = categoryName1.getExamples();
        default2 = categoryName2.getDefaultValue();
        enum2 = categoryName2.getEnumValues();
        example2 = categoryName2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // Root level name string
        ParameterElement name1 = getParameterByName("name", body1.getProperties());
        ParameterElement name2 = name1.deepClone();
        assertEquals(name1, name2);
        default1 = name1.getDefaultValue();
        enum1 = name1.getEnumValues();
        example1 = name1.getExamples();
        default2 = name2.getDefaultValue();
        enum2 = name2.getEnumValues();
        example2 = name2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // Root level photoUrls array
        ParameterArray photoUrls1 = (ParameterArray) getParameterByName("photoUrls", body1.getProperties());
        ParameterArray photoUrls2 = photoUrls1.deepClone();
        assertEquals(photoUrls1, photoUrls2);
        default1 = photoUrls1.getDefaultValue();
        enum1 = photoUrls1.getEnumValues();
        example1 = photoUrls1.getExamples();
        default2 = photoUrls2.getDefaultValue();
        enum2 = photoUrls2.getEnumValues();
        example2 = photoUrls2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // 2nd level, photoUrls -> photoUrl (reference element for photoUrls array)
        ParameterElement photoUrl1 = photoUrls1.getReferenceElement();
        ParameterElement photoUrl2 = photoUrl1.deepClone();
        assertEquals(photoUrl1, photoUrl2);
        default1 = photoUrl1.getDefaultValue();
        enum1 = photoUrl1.getEnumValues();
        example1 = photoUrl1.getExamples();
        default2 = photoUrl2.getDefaultValue();
        enum2 = photoUrl2.getEnumValues();
        example2 = photoUrl2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // Root level tags array
        ParameterArray tags1 = (ParameterArray) getParameterByName("tags", body1.getProperties());
        ParameterArray tags2 = tags1.deepClone();
        assertEquals(tags1, tags2);
        default1 = tags1.getDefaultValue();
        enum1 = tags1.getEnumValues();
        example1 = tags1.getExamples();
        default2 = tags2.getDefaultValue();
        enum2 = tags2.getEnumValues();
        example2 = tags2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // 2nd level, tags -> tag (reference element for tags array)
        ParameterObject tag1 = (ParameterObject) tags1.getReferenceElement();
        ParameterObject tag2 = tag1.deepClone();
        assertEquals(tag1, tag2);
        default1 = tag1.getDefaultValue();
        enum1 = tag1.getEnumValues();
        example1 = tag1.getExamples();
        default2 = tag2.getDefaultValue();
        enum2 = tag2.getEnumValues();
        example2 = tag2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // 3rd level, tags -> tag -> id
        ParameterElement tagId1 = getParameterByName("id", tag1.getProperties());
        ParameterElement tagId2 = tagId1.deepClone();
        assertEquals(tagId1, tagId2);
        default1 = tagId1.getDefaultValue();
        enum1 = tagId1.getEnumValues();
        example1 = tagId1.getExamples();
        default2 = tagId2.getDefaultValue();
        enum2 = tagId2.getEnumValues();
        example2 = tagId2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // 3rd level, tags -> tag -> name
        ParameterElement tagName1 = getParameterByName("name", tag1.getProperties());
        ParameterElement tagName2 = tagName1.deepClone();
        assertEquals(tagName1, tagName2);
        default1 = tagName1.getDefaultValue();
        enum1 = tagName1.getEnumValues();
        example1 = tagName1.getExamples();
        default2 = tagName2.getDefaultValue();
        enum2 = tagName2.getEnumValues();
        example2 = tagName2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);

        // Root level status string
        ParameterElement status1 = getParameterByName("status", body1.getProperties());
        ParameterElement status2 = status1.deepClone();
        assertEquals(status1, status2);
        default1 = status1.getDefaultValue();
        enum1 = status1.getEnumValues();
        example1 = status1.getExamples();
        default2 = status2.getDefaultValue();
        enum2 = status2.getEnumValues();
        example2 = status2.getExamples();
        assertEquals(default1, default2);
        assertEquals(enum1, enum2);
        assertEquals(example1, example2);
    }

    @Test
    public void testNullParameter() throws IOException {
        logger.info("Test NullParameter creation and manipulation");

        String endpoint = "/pets";
        HTTPMethod method = HTTPMethod.POST;
        Operation o = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/postPets_full.json"));

        // Header parameters
        Set<ParameterElement> headerParameters = o.getHeaderParameters();
        ParameterLeaf apiKey = (ParameterLeaf) getParameterByName("api_key", headerParameters);
        NullParameter nullApiKey = new NullParameter(apiKey);

        assertEquals(apiKey.getName(), nullApiKey.getName());
        assertEquals(apiKey.getLocation(), nullApiKey.getLocation());
        assertEquals(apiKey.getDefaultValue(), nullApiKey.getDefaultValue());
        assertEquals("null", nullApiKey.getValue());
        assertFalse(nullApiKey.isObjectTypeCompliant(new Object()));
        assertFalse(nullApiKey.isObjectTypeCompliant(null));
        assertEquals("null", nullApiKey.generateCompliantValue());
        assertEquals(nullApiKey, nullApiKey.deepClone());
        assertEquals(nullApiKey, new NullParameter(new StringParameter(nullApiKey)));
    }

    @Test
    public void testParameterSchemaName() throws InvalidOpenAPIException, CannotParseOpenAPIException {
        logger.info("Test schema name field in parameters");

        File specification = new File("build/resources/test/specifications/small_petstore.json");
        OpenAPIParser openAPIParser = new OpenAPIParser(Paths.get(specification.getAbsolutePath()));
        OpenAPI openapi = openAPIParser.parse();

        Operation operation = openapi.getOperations().stream()
                .filter(o -> o.getEndpoint().equals("/pet") && o.getMethod().equals(HTTPMethod.POST))
                .findFirst().get();

        ParameterObject body = (ParameterObject) operation.getRequestBody();
        assertEquals(body.getSchemaName(), "Pet");

        ParameterElement category = getParameterByName("category", body.getProperties());
        assertEquals(category.getSchemaName(), "Category");

        ParameterArray tags = (ParameterArray) getParameterByName("tags", body.getProperties());
        ParameterElement tag = tags.getReferenceElement();
        assertEquals(tag.getSchemaName(), "Tag");

        specification = new File("build/resources/test/specifications/itemsRef.json");
        openAPIParser = new OpenAPIParser(Paths.get(specification.getAbsolutePath()));
        openapi = openAPIParser.parse();

        operation = openapi.getOperations().stream().findAny().get();
        ParameterElement headerParameter = operation.getHeaderParameters().stream().findAny().get();
        assertEquals("CustomString", headerParameter.getSchemaName());

        ParameterElement pathParameter = operation.getPathParameters().stream().findAny().get();
        assertEquals("CustomString", pathParameter.getSchemaName());

        ParameterElement queryParameter = operation.getPathParameters().stream().findAny().get();
        assertEquals("CustomString", queryParameter.getSchemaName());
    }

    @Test
    public void testTypeCompliant() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvalidOpenAPIException {
        logger.info("Test Parameters functions 'isObjectTypeCompliant' and 'castToParameterValueType'");

        Object defaultValue;
        Set<Object> enumValues, exampleValues;
        Operation o = TestingOperationGenerator.getTestingOperation();
        o = o.deepClone();

        Map<String, Object> map = getJSONMap("build/resources/test/parameters/integer_not_valid.json");
        ParameterElement intParameter = new NumberParameter(null, map, o, "intParameter");
        assertTrue(intParameter.isObjectTypeCompliant(10));
        assertFalse(intParameter.isObjectTypeCompliant(true));
        assertFalse(intParameter.isObjectTypeCompliant(""));
        defaultValue = intParameter.getDefaultValue();
        enumValues = intParameter.getEnumValues();
        exampleValues = intParameter.getExamples();
        assertEquals(defaultValue, 1.0);
        assertTrue(exampleValues.contains(5.0));
        assertTrue(enumValues.contains(1.0));
        assertTrue(enumValues.contains(2.0));
        assertTrue(enumValues.contains(3.0));
        assertTrue(enumValues.size() == 3);

        map = getJSONMap("build/resources/test/parameters/string_not_valid.json");
        ParameterElement stringParameter = new StringParameter(null, map, o, "stringParameter");
        assertTrue(stringParameter.isObjectTypeCompliant(""));
        assertFalse(stringParameter.isObjectTypeCompliant(10));
        assertFalse(stringParameter.isObjectTypeCompliant(true));
        defaultValue = stringParameter.getDefaultValue();
        enumValues = stringParameter.getEnumValues();
        exampleValues = stringParameter.getExamples();
        assertEquals(defaultValue, "a");
        assertTrue(exampleValues.contains("true"));
        assertTrue(enumValues.contains("1.0"));
        assertTrue(enumValues.contains("2.5"));
        assertTrue(enumValues.contains("a"));
        assertTrue(enumValues.size() == 3);

        map = getJSONMap("build/resources/test/parameters/boolean_not_valid.json");
        ParameterElement booleanParameter = new BooleanParameter(null, map, o, "booleanParameter");
        assertTrue(booleanParameter.isObjectTypeCompliant(true));
        assertFalse(booleanParameter.isObjectTypeCompliant(10));
        assertFalse(booleanParameter.isObjectTypeCompliant("true"));
        defaultValue = booleanParameter.getDefaultValue();
        enumValues = booleanParameter.getEnumValues();
        exampleValues = booleanParameter.getExamples();
        assertEquals(null, defaultValue);
        assertTrue(exampleValues.contains(true));
        assertTrue(exampleValues.contains(false));
        assertTrue(exampleValues.size() == 2);
        assertTrue(enumValues.contains(true));
        assertTrue(enumValues.contains(false));
        assertTrue(enumValues.size() == 2);

        map = getJSONMap("build/resources/test/parameters/object_not_valid.json");
        ParameterObject objectParameter = new ParameterObject(null, map, o, "objectParameter");
        assertTrue(objectParameter.isObjectTypeCompliant(new HashMap<>()));
        assertFalse(objectParameter.isObjectTypeCompliant(10));
        assertFalse(objectParameter.isObjectTypeCompliant(true));
        assertFalse(objectParameter.isObjectTypeCompliant("true"));
        defaultValue = objectParameter.getDefaultValue();
        enumValues = objectParameter.getEnumValues();
        exampleValues = objectParameter.getExamples();
        assertEquals(null, defaultValue);
        assertTrue(exampleValues.size() == 1);
        assertTrue(enumValues.isEmpty());

        map = getJSONMap("build/resources/test/parameters/array_not_valid.json");
        ParameterArray arrayParameter = new ParameterArray(null, map, o, "arrayParameter");
        assertTrue(arrayParameter.isObjectTypeCompliant(new LinkedList<>()));
        assertFalse(arrayParameter.isObjectTypeCompliant(10));
        assertFalse(arrayParameter.isObjectTypeCompliant(true));
        assertFalse(arrayParameter.isObjectTypeCompliant("true"));
        defaultValue = arrayParameter.getDefaultValue();
        enumValues = arrayParameter.getEnumValues();
        exampleValues = arrayParameter.getExamples();
        assertEquals(null, defaultValue);
        assertTrue(exampleValues.contains(new LinkedList<>(Arrays.asList(1.0, 2.0))));
        assertTrue(exampleValues.size() == 1);
        assertTrue(enumValues.isEmpty());
    }

    @Test
    public void testStyleExplodeParsing() throws InvalidOpenAPIException, CannotParseOpenAPIException {
        logger.info("Test parsing of style and explode fields of Parameters");

        File specification = new File("build/resources/test/specifications/style_explode.json");
        OpenAPIParser openAPIParser = new OpenAPIParser(Paths.get(specification.getAbsolutePath()));
        OpenAPI openapi = openAPIParser.parse();

        Operation operation = openapi.getOperations().stream()
                .filter(o -> o.getEndpoint().equals("/pet/{petId}/{anotherId}") && o.getMethod().equals(HTTPMethod.GET))
                .findFirst().get();
        operation = operation.deepClone();

        NumberParameter petId = (NumberParameter) operation.getPathParameters().stream()
                .filter(p -> p.getName().equals(new ParameterName("petId")))
                .findAny().get();
        petId.setValue(5);
        assertEquals("5", petId.getValueAsFormattedString());

        NumberParameter anotherId = (NumberParameter) operation.getPathParameters().stream()
                .filter(p -> p.getName().equals(new ParameterName("anotherId")))
                .findAny().get();
        anotherId.setValue(8);
        assertEquals(".8", anotherId.getValueAsFormattedString());
        assertEquals("8", anotherId.getValueAsFormattedString(ParameterStyle.SIMPLE));

        ParameterObject anObject = (ParameterObject) operation.getQueryParameters().stream()
                .filter(p -> p.getName().equals(new ParameterName("anObject")))
                .findAny().get();
        ((StringParameter) anObject.getProperties().stream()
                .filter(p -> p.getName().equals(new ParameterName("first"))).findFirst().get()).setValue("test");
        ((NumberParameter) anObject.getProperties().stream()
                .filter(p -> p.getName().equals(new ParameterName("second"))).findFirst().get()).setValue(12);
        assertEquals("first=test&second=12", anObject.getValueAsFormattedString());
        assertEquals("anObject=first,test,second,12", anObject.getValueAsFormattedString(ParameterStyle.FORM, false));
        assertEquals(";first=test;second=12", anObject.getValueAsFormattedString(ParameterStyle.MATRIX, true));
        assertEquals(";anObject=first,test,second,12", anObject.getValueAsFormattedString(ParameterStyle.MATRIX, false));
        assertEquals(".first=test.second=12", anObject.getValueAsFormattedString(ParameterStyle.LABEL, true));
        assertEquals(".first.test.second.12", anObject.getValueAsFormattedString(ParameterStyle.LABEL, false));
        assertEquals("first=test,second=12", anObject.getValueAsFormattedString(ParameterStyle.SIMPLE, true));
        // Use defined explode, so it should be exactly like previous
        assertEquals("first=test,second=12", anObject.getValueAsFormattedString(ParameterStyle.SIMPLE));
        assertEquals("first,test,second,12", anObject.getValueAsFormattedString(ParameterStyle.SIMPLE, false));
        assertEquals("first%20test%20second%2012", anObject.getValueAsFormattedString(ParameterStyle.SPACE_DELIMITED, true));
        // Explode has no effect
        assertEquals("first%20test%20second%2012", anObject.getValueAsFormattedString(ParameterStyle.SPACE_DELIMITED, false));
        assertEquals("first|test|second|12", anObject.getValueAsFormattedString(ParameterStyle.PIPE_DELIMITED, true));
        // Explode has no effect
        assertEquals("first|test|second|12", anObject.getValueAsFormattedString(ParameterStyle.PIPE_DELIMITED, false));
        assertEquals("anObject[first]=test&anObject[second]=12", anObject.getValueAsFormattedString(ParameterStyle.DEEP_OBJECT, true));
        // Explode has no effect
        assertEquals("anObject[first]=test&anObject[second]=12", anObject.getValueAsFormattedString(ParameterStyle.DEEP_OBJECT, false));

        ParameterArray anArray = (ParameterArray) operation.getQueryParameters().stream()
                .filter(p -> p.getName().equals(new ParameterName("anArray")))
                .findAny().get();
        anArray.clearElements();
        for (int i = 0; i < 3; ++i) {
            StringParameter sp = (StringParameter) anArray.getReferenceElement().deepClone();
            sp.setValue("val" + i);
            anArray.addElement(sp);
        }
        assertEquals("val0|val1|val2", anArray.getValueAsFormattedString());
        assertEquals(";anArray=val0;anArray=val1;anArray=val2", anArray.getValueAsFormattedString(ParameterStyle.MATRIX, true));
        assertEquals(";anArray=val0,val1,val2", anArray.getValueAsFormattedString(ParameterStyle.MATRIX, false));
        assertEquals(".val0.val1.val2", anArray.getValueAsFormattedString(ParameterStyle.LABEL, true));
        assertEquals(".val0.val1.val2", anArray.getValueAsFormattedString(ParameterStyle.LABEL, false));
        assertEquals("anArray=val0&anArray=val1&anArray=val2", anArray.getValueAsFormattedString(ParameterStyle.FORM, true));
        assertEquals("anArray=val0,val1,val2", anArray.getValueAsFormattedString(ParameterStyle.FORM, false));
        assertEquals("val0,val1,val2", anArray.getValueAsFormattedString(ParameterStyle.SIMPLE, true));
        assertEquals("val0,val1,val2", anArray.getValueAsFormattedString(ParameterStyle.SIMPLE, false));
        assertEquals("val0%20val1%20val2", anArray.getValueAsFormattedString(ParameterStyle.SPACE_DELIMITED, false));

    }
}
