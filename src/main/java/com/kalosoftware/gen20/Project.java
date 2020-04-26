package com.kalosoftware.gen20;

import com.kalosoftware.gen20.constants.Constants;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.json.JsonObject;
import javax.lang.model.element.Modifier;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *
 * @author Carlos Cercado
 * @email cercadocarlos@gmail.com
 */
public class Project {

    private final static Logger LOGGER = Logger.getLogger(Project.class.getName());

    public static void startStructure(final String PATH, final String groupId, final String projectName, final String database_url, final String database_username, final String database_password) throws Exception {
        final File dir = new File(PATH);
        final String groupIdSlash = groupId.replace(".", "\\");

        if (!dir.isDirectory()) {
            throw new Exception("Directorio no valido: " + PATH);
        }

        boolean isWindows = System.getProperty("os.name").toUpperCase().startsWith(Constants.WIN);

        if (!isWindows) {
            throw new Exception("Sistema operativo debe ser WINDOWS");
        }

        Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\test\\java", projectName), null, dir);
        Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\main\\resources", projectName), null, dir);
        Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\main\\webapp", projectName), null, dir);
        Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\main\\java\\%s\\models", projectName, groupIdSlash), null, dir);
        Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\main\\java\\%s\\dao", projectName, groupIdSlash), null, dir);
        Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\main\\java\\%s\\services\\dto", projectName, groupIdSlash), null, dir);
        //Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\main\\java\\%s\\constants", projectName, groupIdSlash), null, dir);
        Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\main\\java\\%s\\exceptions", projectName, groupIdSlash), null, dir);
        Runtime.getRuntime().exec(String.format("cmd.exe /c mkdir %s\\src\\main\\java\\%s\\utils", projectName, groupIdSlash), null, dir);

        generatePom(PATH + "\\" + projectName, groupId, projectName);

        generateFacades(PATH + "\\" + projectName + "\\src\\main\\java\\", groupId);
        
        generateUtils(PATH + "\\" + projectName + "\\src\\main\\java\\", groupId + ".utils", database_url, database_username, database_password);

        generateExceptionsClass(PATH + "\\" + projectName + "\\src\\main\\java\\", groupId + ".exceptions");

        generateApplicationRest(PATH + "\\" + projectName + "\\src\\main\\java\\", groupId + ".services");

        generateHomeHTML(PATH + "\\" + projectName + "\\src\\main\\webapp\\", projectName);

        generateYAML(PATH + "\\" + projectName + "\\src\\main\\webapp\\WEB-INF\\", projectName);
        
        generateTestCase(PATH + "\\" + projectName + "\\src\\test\\java\\", groupId);

    }

    private static void generatePom(final String path, final String groupId, final String artifactId) throws IOException {
        LOGGER.info("Generando el archivo POM...");
        final Model model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion("1.0.0");
        model.setPackaging("war");
        model.setName(artifactId);
        model.setModelVersion("4.0.0");

        final Properties prop = new Properties();
        prop.setProperty("java.version", Constants.JAVA_VERSION);
        model.setProperties(prop);

        prop.setProperty("mvn-compiler-plugin.version", "3.7.0");
        model.setProperties(prop);

        prop.setProperty("maven-dependency-plugin.version", "3.1.1");
        model.setProperties(prop);

        prop.setProperty("replacer.version", "1.5.3");
        model.setProperties(prop);

        prop.setProperty("javax.servlet-api.version", "4.0.1");
        model.setProperties(prop);

        prop.setProperty("jersey.version", "2.29.1");
        model.setProperties(prop);

        prop.setProperty("swagger.version", "2.1.1");
        model.setProperties(prop);

        prop.setProperty("swagger-ui.version", "3.17.0");
        model.setProperties(prop);

        prop.setProperty("jackson.version", "2.10.1");
        model.setProperties(prop);

        prop.setProperty("hibernate.version", "5.2.4.Final");
        model.setProperties(prop);

        prop.setProperty("maven-war-plugin.version", "3.2.2");
        model.setProperties(prop);

        final List<Dependency> dependencies = new ArrayList();

        final Dependency java = new Dependency();
        java.setGroupId("javax");
        java.setArtifactId("javaee-web-api");
        java.setVersion("7.0");
        java.setScope("provided");

        dependencies.add(java);

        final Dependency jerseyCore = new Dependency();

        jerseyCore.setGroupId("org.glassfish.jersey.core");
        jerseyCore.setArtifactId("jersey-server");
        jerseyCore.setVersion("${jersey.version}");

        dependencies.add(jerseyCore);

        final Dependency jerseyContainer = new Dependency();

        jerseyContainer.setGroupId("org.glassfish.jersey.containers");
        jerseyContainer.setArtifactId("jersey-container-servlet");
        jerseyContainer.setVersion("${jersey.version}");

        dependencies.add(jerseyContainer);

        final Dependency jerseyJson = new Dependency();

        jerseyJson.setGroupId("com.sun.jersey");
        jerseyJson.setArtifactId("jersey-json");
        jerseyJson.setVersion("1.19.4");

        dependencies.add(jerseyJson);

        final Dependency glassfishJson = new Dependency();

        glassfishJson.setGroupId("org.glassfish");
        glassfishJson.setArtifactId("javax.json");
        glassfishJson.setVersion("1.1");

        dependencies.add(glassfishJson);

        final Dependency jacksonMedia = new Dependency();

        jacksonMedia.setGroupId("org.glassfish.jersey.media");
        jacksonMedia.setArtifactId("jersey-media-json-jackson");
        jacksonMedia.setVersion("2.29.1");

        dependencies.add(jacksonMedia);

        final Dependency validation = new Dependency();

        validation.setGroupId("org.hibernate");
        validation.setArtifactId("hibernate-validator");
        validation.setVersion("${hibernate.version}");

        dependencies.add(validation);

        final Dependency swaggerCore = new Dependency();

        swaggerCore.setGroupId("io.swagger.core.v3");
        swaggerCore.setArtifactId("swagger-jaxrs2");
        swaggerCore.setVersion("${swagger.version}");
        final Exclusion jsr310 = new Exclusion();
        jsr310.setGroupId("org.jboss.logging");
        jsr310.setArtifactId("jboss-logging");
        swaggerCore.addExclusion(jsr310);

        dependencies.add(swaggerCore);

        final Dependency jbossLogging = new Dependency();

        jbossLogging.setGroupId("org.jboss.logging");
        jbossLogging.setArtifactId("jboss-logging");
        jbossLogging.setVersion("3.3.0.Final");

        dependencies.add(jbossLogging);

        final Dependency jacksonDatatype = new Dependency();

        jacksonDatatype.setGroupId("com.fasterxml.jackson.datatype");
        jacksonDatatype.setArtifactId("jackson-datatype-jsr310");
        jacksonDatatype.setVersion("${jackson.version}");

        dependencies.add(jacksonDatatype);

        final Dependency hinernate = new Dependency();

        hinernate.setGroupId("org.hibernate");
        hinernate.setArtifactId("hibernate-core");
        hinernate.setVersion("${hibernate.version}");
        final Exclusion exclusionJboss = new Exclusion();
        exclusionJboss.setGroupId("org.jboss.logging");
        exclusionJboss.setArtifactId("jboss-logging");
        hinernate.addExclusion(exclusionJboss);

        dependencies.add(hinernate);

        final Dependency hinernateEntityManager = new Dependency();

        hinernateEntityManager.setGroupId("org.hibernate");
        hinernateEntityManager.setArtifactId("hibernate-entitymanager");
        hinernateEntityManager.setVersion("${hibernate.version}");
        hinernateEntityManager.addExclusion(exclusionJboss);

        dependencies.add(hinernateEntityManager);

        final Dependency persistence = new Dependency();

        persistence.setGroupId("javax.persistence");
        persistence.setArtifactId("javax.persistence-api");
        persistence.setVersion("2.2");

        dependencies.add(persistence);

        final Dependency postgres = new Dependency();

        postgres.setGroupId("postgresql");
        postgres.setArtifactId("postgresql");
        postgres.setVersion("9.1-901.jdbc4");

        dependencies.add(postgres);

        final Dependency hkr = new Dependency();

        hkr.setGroupId("org.glassfish.jersey.inject");
        hkr.setArtifactId("jersey-hk2");
        hkr.setVersion("2.29.1");
        //hkr.setScope("test");

        dependencies.add(hkr);

        final Dependency jerseyTest = new Dependency();

        jerseyTest.setGroupId("org.glassfish.jersey.test-framework");
        jerseyTest.setArtifactId("jersey-test-framework-core");
        jerseyTest.setVersion("2.29.1");
        jerseyTest.setScope("test");

        dependencies.add(jerseyTest);

        final Dependency jerseyTestProvider = new Dependency();

        jerseyTestProvider.setGroupId("org.glassfish.jersey.test-framework.providers");
        jerseyTestProvider.setArtifactId("jersey-test-framework-provider-grizzly2");
        jerseyTestProvider.setVersion("2.29.1");
        jerseyTestProvider.setScope("test");

        dependencies.add(jerseyTestProvider);

        model.setDependencies(dependencies);

        final Build build = new Build();
        final Plugin mavenCompilerPlugin = new Plugin();
        mavenCompilerPlugin.setArtifactId("maven-compiler-plugin");
        mavenCompilerPlugin.setVersion("${mvn-compiler-plugin.version}");

        final Xpp3Dom config = new Xpp3Dom("configuration");
        final Xpp3Dom source = new Xpp3Dom("source");
        source.setValue("${java.version}");
        config.addChild(source);

        final Xpp3Dom target = new Xpp3Dom("target");
        target.setValue("${java.version}");
        config.addChild(target);

        mavenCompilerPlugin.setConfiguration(config);

        final Plugin mavenDependency = new Plugin();
        mavenDependency.setArtifactId("maven-dependency-plugin");
        mavenDependency.setVersion("${maven-dependency-plugin.version}");

        final PluginExecution execution = new PluginExecution();

        execution.setPhase("prepare-package");
        execution.setGoals(Arrays.asList("unpack"));

        final Xpp3Dom configuration = new Xpp3Dom("configuration");
        final Xpp3Dom artifactItems = new Xpp3Dom("artifactItems");
        final Xpp3Dom artifactItem = new Xpp3Dom("artifactItem");
        final Xpp3Dom groupIdDom = new Xpp3Dom("groupId");
        groupIdDom.setValue("org.webjars");
        final Xpp3Dom artifactIdDom = new Xpp3Dom("artifactId");
        artifactIdDom.setValue("swagger-ui");
        final Xpp3Dom version = new Xpp3Dom("version");
        version.setValue("${swagger-ui.version}");

        final Xpp3Dom outputDirectory = new Xpp3Dom("outputDirectory");
        outputDirectory.setValue("${project.build.directory}/swagger-ui");

        artifactItem.addChild(groupIdDom);
        artifactItem.addChild(artifactIdDom);
        artifactItem.addChild(version);

        artifactItems.addChild(artifactItem);
        configuration.addChild(artifactItems);
        configuration.addChild(outputDirectory);

        execution.setConfiguration(configuration);

        mavenDependency.setExecutions(Arrays.asList(execution));

        final Plugin mavenPlugin = new Plugin();
        mavenPlugin.setGroupId("org.apache.maven.plugins");
        mavenPlugin.setArtifactId("maven-war-plugin");
        mavenPlugin.setVersion("${maven-war-plugin.version}");

        final Xpp3Dom configMavenPlugin = new Xpp3Dom("configuration");
        final Xpp3Dom webResources = new Xpp3Dom("webResources");
        webResources.setAttribute("combine.children", "append");
        final Xpp3Dom resource = new Xpp3Dom("resource");
        final Xpp3Dom directory = new Xpp3Dom("directory");
        directory.setValue("${project.build.directory}/swagger-ui/META-INF/resources/webjars/swagger-ui/${swagger-ui.version}");
        final Xpp3Dom includes = new Xpp3Dom("includes");
        final Xpp3Dom include = new Xpp3Dom("include");
        include.setValue("**/*.*");
        includes.addChild(include);

        final Xpp3Dom targetPath = new Xpp3Dom("targetPath");
        targetPath.setValue("swagger-ui");

        resource.addChild(directory);
        resource.addChild(includes);
        resource.addChild(targetPath);
        webResources.addChild(resource);
        configMavenPlugin.addChild(webResources);

        mavenPlugin.setConfiguration(configMavenPlugin);

        final Plugin mavenReplacer = new Plugin();
        mavenReplacer.setGroupId("com.google.code.maven-replacer-plugin");
        mavenReplacer.setArtifactId("replacer");
        mavenReplacer.setVersion("${replacer.version}");

        final PluginExecution executionReplacer = new PluginExecution();
        executionReplacer.setPhase("prepare-package");
        executionReplacer.setGoals(Arrays.asList("replace"));

        mavenReplacer.setExecutions(Arrays.asList(executionReplacer));

        final Xpp3Dom configurationReplacer = new Xpp3Dom("configuration");
        final Xpp3Dom file = new Xpp3Dom("file");
        file.setValue("${project.build.directory}/swagger-ui/META-INF/resources/webjars/swagger-ui/${swagger-ui.version}/index.html");
        final Xpp3Dom replacements = new Xpp3Dom("replacements");
        final Xpp3Dom replacement = new Xpp3Dom("replacement");
        final Xpp3Dom token = new Xpp3Dom("token");
        token.setValue("http://petstore.swagger.io/v2/swagger.json");
        final Xpp3Dom value = new Xpp3Dom("value");
        value.setValue("/${project.name}/rest/openapi.json");

        replacement.addChild(token);
        replacement.addChild(value);

        replacements.addChild(replacement);

        configurationReplacer.addChild(file);
        configurationReplacer.addChild(replacements);

        mavenReplacer.setConfiguration(configurationReplacer);

        build.setPlugins(Arrays.asList(mavenCompilerPlugin, mavenDependency, mavenPlugin, mavenReplacer));

        model.setBuild(build);

        final Writer writer = new FileWriter(String.format("%s/pom.xml", path));

        new MavenXpp3Writer().write(writer, model);
    }

    private static void generateFacades(final String path, String pack) throws IOException {
        LOGGER.info("Generando los facades...");
        generateDTOFacade(path, pack + ".services.dto");
        generateDAOFacade(path, pack + ".dao");
    }

    private static void generateDTOFacade(final String path, String pack) throws IOException {
        final ClassName validation = ClassName.get("javax.validation", "Validation");
        final ClassName constraintViolation = ClassName.get("javax.validation", "ConstraintViolation");
        final ClassName validatorFactory = ClassName.get("javax.validation", "ValidatorFactory");
        final ClassName validator = ClassName.get("javax.validation", "Validator");
        final ClassName jsonObjectBuilder = ClassName.get("javax.json", "JsonObjectBuilder");
        final ClassName json = ClassName.get("javax.json", "Json");
        final ClassName jsonIgnore = ClassName.get("com.fasterxml.jackson.annotation", "JsonIgnore");
        final ClassName response = ClassName.get("javax.ws.rs.core", "Response");
        final ClassName servicesException = ClassName.get(pack.replace("services.dto", "exceptions"), "ServicesException");

        final TypeName wildcard = ParameterizedTypeName.get(Object.class);
        final TypeName classOfAny = ParameterizedTypeName.get(constraintViolation, wildcard);
        final ParameterizedTypeName type = ParameterizedTypeName.get(ClassName.get(Set.class), classOfAny);

        final MethodSpec getViolaciones = MethodSpec.methodBuilder("getViolaciones")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(jsonIgnore)
                .returns(type)
                .addStatement("final $T factory = $T.buildDefaultValidatorFactory()", validatorFactory, validation)
                .addStatement("final $T validator = factory.getValidator()", validator)
                .addStatement("return validator.validate(this)")
                .build();

        final MethodSpec getErrors = MethodSpec.methodBuilder("getErrors")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(jsonIgnore)
                .returns(JsonObject.class)
                .addStatement("final $T builder = $T.createObjectBuilder()", jsonObjectBuilder, json)
                .addCode("this.getViolaciones().stream().forEach(x -> {\n"
                        + "    builder.add(x.getPropertyPath().toString(), x.getMessage());\n"
                        + "});\n")
                .addStatement("return builder.build()")
                .build();

        final MethodSpec validate = MethodSpec.methodBuilder("validate")
                .addException(servicesException)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addStatement("final JsonObject errors = this.getErrors()")
                .addCode("if(!errors.isEmpty()){\n"
                        + "   throw new $T($T.Status.BAD_REQUEST, errors.toString());\n"
                        + "}", servicesException, response)
                .build();

        final TypeSpec dtoFacade = TypeSpec.classBuilder("DTOFacade")
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addMethod(getViolaciones)
                .addMethod(getErrors)
                .addMethod(validate)
                .build();

        final JavaFile javaFile = JavaFile.builder(pack, dtoFacade)
                .build();

        javaFile.writeTo(new File(path + "\\"));

    }

    private static void generateUtils(final String path, final String pack, final String URL, final String USERNAME, final String PASSWORD) throws IOException {
        final ClassName entityManagerFactory = ClassName.get("javax.persistence", "EntityManagerFactory");
        final ClassName persistence = ClassName.get("javax.persistence", "Persistence");

        final TypeSpec hibernate = TypeSpec.classBuilder("Utils")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.methodBuilder("isTestEnv")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(Boolean.class)
                        .addStatement("final $T[] stackTrace = $T.currentThread().getStackTrace()", StackTraceElement.class, Thread.class)
                        .addStatement("final $T<StackTraceElement> list = $T.asList(stackTrace)", List.class, Arrays.class)
                        .beginControlFlow("for (StackTraceElement element : list)")
                        .beginControlFlow("if (element.getClassName().startsWith(\"org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer\"))")
                        .addStatement("return true")
                        .endControlFlow()
                        .endControlFlow()
                        .addStatement("return false")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getEntityManagerFactoryForTest")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(entityManagerFactory)
                        .addStatement("final $T<String, String> persistenceMap = new $T<String, String>()", Map.class, HashMap.class)
                        .addStatement("persistenceMap.put(\"hibernate.dialect\", $S)", "org.hibernate.dialect.PostgreSQLDialect")
                        .addStatement("persistenceMap.put(\"hibernate.connection.driver_class\", $S)", Database.DRIVER)
                        .addStatement("persistenceMap.put(\"hibernate.connection.username\", $S)", USERNAME)
                        .addStatement("persistenceMap.put(\"hibernate.connection.password\", $S)", PASSWORD)
                        .addStatement("persistenceMap.put(\"hibernate.connection.url\", $S)", URL)
                        .addStatement("return $T.createEntityManagerFactory(\"Persistencia\", persistenceMap)", persistence)
                        .build()
                )
                .build();

        final JavaFile javaFile = JavaFile.builder(pack, hibernate)
                .build();

        javaFile.writeTo(new File(path + "\\"));
    }

    private static void generateDAOFacade(final String path, String pack) throws IOException {
        final ClassName entityManager = ClassName.get("javax.persistence", "EntityManager");
        final ClassName entityManagerFactory = ClassName.get("javax.persistence", "EntityManagerFactory");
        final ClassName persistence = ClassName.get("javax.persistence", "Persistence");
        final ClassName criteriaQuery = ClassName.get("javax.persistence.criteria", "CriteriaQuery");
        final ClassName criteriaBuilder = ClassName.get("javax.persistence.criteria", "CriteriaBuilder");

        MethodSpec create = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(TypeVariableName.get("T"), "obj", Modifier.FINAL)
                .addStatement("this.manager.getTransaction().begin()")
                .addStatement("this.manager.persist(obj)")
                .addStatement("this.manager.getTransaction().commit()")
                .build();

        final MethodSpec update = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(TypeVariableName.get("T"), "obj", Modifier.FINAL)
                .addStatement("this.manager.getTransaction().begin()")
                .addStatement("this.manager.merge(obj)")
                .addStatement("this.manager.getTransaction().commit()")
                .build();

        final MethodSpec delete = MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(TypeVariableName.get("T"), "obj", Modifier.FINAL)
                .addStatement("this.manager.getTransaction().begin()")
                .addStatement("this.manager.remove(obj)")
                .addStatement("this.manager.getTransaction().commit()")
                .build();

        final ParameterizedTypeName typeReturn = ParameterizedTypeName.get(ClassName.get(List.class), TypeVariableName.get("T"));

        final MethodSpec getAll = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC)
                .returns(typeReturn)
                .addStatement("final $T criteria = this.manager.getCriteriaBuilder().createQuery()", criteriaQuery)
                .addStatement("criteria.select(criteria.from(this.entityClass))")
                .addStatement("return this.manager.createQuery(criteria).getResultList()")
                .build();

        final MethodSpec findBy = MethodSpec.methodBuilder("findBy")
                .addModifiers(Modifier.PUBLIC)
                .returns(typeReturn)
                .addParameter(ParameterSpec.builder(String.class, "attribute", Modifier.FINAL).build())
                .addParameter(ParameterSpec.builder(Object.class, "value", Modifier.FINAL).build())
                .addStatement("final $T cb = this.manager.getCriteriaBuilder()", criteriaBuilder)
                .addStatement("final $T<T> cq = cb.createQuery(this.entityClass)", criteriaQuery)
                .addStatement("final $T<T> root = cq.from(this.entityClass)", ClassName.get("javax.persistence.criteria", "Root"))
                .addStatement("final $T predicate = cb.equal(root.get(attribute), value)", ClassName.get("javax.persistence.criteria", "Predicate"))
                .addStatement("cq.where(predicate)")
                .addStatement("return this.manager.createQuery(cq).getResultList()")
                .build();

        final MethodSpec getById = MethodSpec.methodBuilder("getById")
                .addParameter(Long.class, "id", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeVariableName.get("T"))
                .addStatement("return this.manager.find(entityClass, id)")
                .build();

        final MethodSpec getParameterizedClass = MethodSpec.methodBuilder("getParameterizedClass")
                .addModifiers(Modifier.PUBLIC)
                .returns(java.lang.Class.class)
                .addStatement("return (Class<T>) (($T) getClass().getGenericSuperclass()).getActualTypeArguments()[0]", ParameterizedType.class)
                .build();

        final FieldSpec.Builder emf = FieldSpec.builder(entityManagerFactory, "emf", Modifier.PRIVATE, Modifier.FINAL);
        final FieldSpec.Builder manager = FieldSpec.builder(entityManager, "manager", Modifier.PRIVATE, Modifier.FINAL);
        final FieldSpec.Builder entityClass = FieldSpec.builder(ParameterizedTypeName.get(ClassName.get("", "Class"), TypeVariableName.get("T")), "entityClass", Modifier.PRIVATE, Modifier.FINAL);

        final TypeSpec daoFacade = TypeSpec.classBuilder("DAOFacade")
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addField(emf.build())
                .addField(manager.build())
                .addField(entityClass.build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get("", "Class"), TypeVariableName.get("T")), "entityClass")
                        .beginControlFlow("if ($T.isTestEnv())", ClassName.get(pack.replace("dao", "utils"), "Utils"))
                        .addStatement("emf = Utils.getEntityManagerFactoryForTest()")
                        .endControlFlow()
                        .beginControlFlow("else")
                        .addStatement("emf = $T.createEntityManagerFactory(\"Persistencia\")", persistence)
                        .endControlFlow()
                        .addStatement("manager = emf.createEntityManager()")
                        .addStatement("this.entityClass = entityClass")
                        .build()
                )
                .addMethod(create)
                .addMethod(update)
                .addMethod(delete)
                .addMethod(getAll)
                .addMethod(getById)
                .addMethod(findBy)
                .addMethod(getParameterizedClass)
                .build();

        final JavaFile javaFile = JavaFile.builder(pack, daoFacade)
                .build();

        javaFile.writeTo(new File(path + "\\"));
    }

    private static void generateExceptionsClass(final String path, String pack) throws IOException {
        LOGGER.info("Generando las excepciones...");
        final ClassName reponseStatus = ClassName.get("javax.ws.rs.core", "Response.Status");
        final ClassName response = ClassName.get("javax.ws.rs.core", "Response");

        final List<FieldSpec> props = new ArrayList();

        final FieldSpec.Builder status = FieldSpec.builder(reponseStatus, "status", Modifier.PRIVATE);
        final FieldSpec.Builder error = FieldSpec.builder(Integer.class, "error", Modifier.PRIVATE);

        props.add(status.build());
        props.add(error.build());

        final MethodSpec.Builder getError = MethodSpec.methodBuilder("getError")
                .addModifiers(Modifier.PUBLIC)
                .returns(Integer.class)
                .addStatement("return this.$L", "error");

        final MethodSpec.Builder getStatus = MethodSpec.methodBuilder("getStatus")
                .addModifiers(Modifier.PUBLIC)
                .returns(reponseStatus)
                .addStatement("return this.$L", "status");

        final TypeSpec servicesException = TypeSpec.classBuilder("ServicesException")
                .superclass(Exception.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(reponseStatus, "status", Modifier.FINAL)
                        .addStatement("this.$L = $L", "status", "status")
                        .build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(reponseStatus, "status", Modifier.FINAL)
                        .addParameter(String.class, "msg", Modifier.FINAL)
                        .addStatement("super($L)", "msg")
                        .addStatement("this.$L = $L", "status", "status")
                        .build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(response, "response", Modifier.FINAL)
                        .build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(reponseStatus, "status", Modifier.FINAL)
                        .addParameter(String.class, "msg", Modifier.FINAL)
                        .addParameter(Integer.class, "error", Modifier.FINAL)
                        .addStatement("super($L)", "msg")
                        .addStatement("this.$L = $L", "status", "status")
                        .addStatement("this.$L = $L", "error", "error")
                        .build()
                )
                .addFields(props)
                .addMethod(getError.build())
                .addMethod(getStatus.build())
                .build();

        final JavaFile javaFile = JavaFile.builder(pack, servicesException)
                .build();

        javaFile.writeTo(new File(path + "\\"));

        final ClassName exceptionMapper = ClassName.get("javax.ws.rs.ext", "ExceptionMapper");
        final ClassName mediaType = ClassName.get("javax.ws.rs.core", "MediaType");

        final ClassName exception = ClassName.get(pack, "ServicesException");

        final ClassName json = ClassName.get("javax.json", "Json");
        final ClassName jsonObject = ClassName.get("javax.json", "JsonObject");

        final MethodSpec.Builder toResponse = MethodSpec.methodBuilder("toResponse")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(exception, "exception", Modifier.FINAL)
                .returns(response)
                .beginControlFlow("if (exception.getMessage() != null)")
                .beginControlFlow("if (ConstantErrors.DATA_VALIDATION_ERROR.equals(exception.getError()))")
                .addStatement("return Response.status(exception.getStatus()).type($T.APPLICATION_JSON).entity(exception.getMessage()).build()", mediaType)
                .endControlFlow()
                .addStatement("final $T json = $T.createObjectBuilder().add(\"message\", exception.getMessage()).build()", jsonObject, json)
                .addStatement("return Response.status(exception.getStatus()).type($T.APPLICATION_JSON).entity(json.toString()).build()", mediaType)
                .endControlFlow()
                .addStatement("return Response.status(exception.getStatus()).build()");

        final TypeSpec servicesExceptionMapper = TypeSpec.classBuilder("ServicesExceptionMapper")
                .addSuperinterface(ParameterizedTypeName.get(exceptionMapper, exception))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(toResponse.build())
                .build();

        final JavaFile javaFile2 = JavaFile.builder(pack, servicesExceptionMapper)
                .build();

        javaFile2.writeTo(new File(path + "\\"));

        final FieldSpec DATA_VALIDATION_ERROR = FieldSpec.builder(Integer.class, "DATA_VALIDATION_ERROR")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer("1")
                .build();

        final TypeSpec constantErrors = TypeSpec.classBuilder("ConstantErrors")
                .addModifiers(Modifier.PUBLIC)
                .addField(DATA_VALIDATION_ERROR)
                .build();

        final JavaFile javaFile3 = JavaFile.builder(pack, constantErrors)
                .build();

        javaFile3.writeTo(new File(path + "\\"));
    }

    private static void generateApplicationRest(final String path, String pack) throws IOException {
        LOGGER.info("Generando el ApplicationRest...");

        final AnnotationSpec applicationPath = AnnotationSpec
                .builder(ClassName.get("javax.ws.rs", "ApplicationPath"))
                .addMember("value", "$S", "rest")
                .build();

        final TypeSpec application = TypeSpec.classBuilder("Application")
                .superclass(ClassName.get("org.glassfish.jersey.server", "ResourceConfig"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(applicationPath)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("packages(\"io.swagger.v3.jaxrs2.integration.resources,$L\")", pack)
                        .addStatement("register($T.class)", ClassName.get(pack.replace("services", "exceptions"), "ServicesExceptionMapper"))
                        .build()
                )
                .build();

        final JavaFile javaFile = JavaFile.builder(pack, application)
                .build();

        javaFile.writeTo(new File(path + "\\"));
    }

    private static void generateTestCase(final String path, String pack) throws IOException {
        LOGGER.info("Generando el TestCase Base...");

        final ClassName entityManager = ClassName.get("javax.persistence", "EntityManager");
        final ClassName entityManagerFactory = ClassName.get("javax.persistence", "EntityManagerFactory");

        final TypeVariableName t = TypeVariableName.get("T");
        
        final TypeSpec testCase = TypeSpec.classBuilder("TestCase")
                .superclass(ClassName.get("org.glassfish.jersey.test", "JerseyTest"))
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(entityManagerFactory, "emf", Modifier.FINAL)
                        .initializer("$T.getEntityManagerFactoryForTest()", ClassName.get(pack + ".utils", "Utils"))
                        .build())
                .addField(FieldSpec.builder(entityManager, "manager", Modifier.FINAL)
                        .initializer("emf.createEntityManager()")
                        .build())
                .addMethod(MethodSpec.methodBuilder("tearDown")
                        .addModifiers(Modifier.PUBLIC)
                        .addException(Exception.class)
                        .addAnnotation(Override.class)
                        .addStatement("this.cleanDatabase()")
                        .addStatement("super.tearDown()")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getEntity")
                        .addException(IOException.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.get("javax.ws.rs.core", "Response"), "response", Modifier.FINAL)
                        .addParameter(ParameterizedTypeName.get(ClassName.get("", "Class"), ClassName.get("", "T")), "class_", Modifier.FINAL)
                        .addTypeVariable(t)
                        .returns(ClassName.get("", "T"))
                        .addStatement("final String res = response.readEntity(String.class)")
                        .addStatement("final $T mapper = new ObjectMapper()", ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper"))
                        .addStatement("return mapper.readValue(res.replaceAll(\"(\\\\[|\\\\])\", \"\"), class_)")
                        .build()
                )
                .addMethod(MethodSpec.methodBuilder("cleanDatabase")
                        .addModifiers(Modifier.PUBLIC)
                        .addException(Exception.class)
                        .addStatement("//manager.getTransaction().begin()")
                        .addStatement("//manager.createQuery(\"DELETE FROM tableA\").executeUpdate()")
                        .addStatement("//manager.createQuery(\"DELETE FROM tableB\").executeUpdate()")
                        .addStatement("//manager.getTransaction().commit()")
                        .build())
                .build();

        final JavaFile javaFile = JavaFile.builder("", testCase)
                .build();

        javaFile.writeTo(new File(path));
    }

    public static void generateHomeHTML(final String path, final String project) throws IOException {

        final File htmlTemplateFile = new File("src/main/resources/index.html");

        String htmlString = FileUtils.readFileToString(htmlTemplateFile, "UTF-8");
        htmlString = htmlString.replace("$title", project);
        final File newHtmlFile = new File(path + "index.html");

        FileUtils.writeStringToFile(newHtmlFile, htmlString, "UTF-8");
    }

    public static void generateYAML(final String path, final String project) throws IOException {

        final File yamlTemplateFile = new File("src/main/resources/openapi.yaml");

        String yamlString = FileUtils.readFileToString(yamlTemplateFile, "UTF-8");
        yamlString = yamlString.replace("$Path", "/" + project);
        final File newYamlFile = new File(path + "openapi.yaml");

        FileUtils.writeStringToFile(newYamlFile, yamlString, "UTF-8");
    }

    public static void generatePersitence(final String path, final List<String> classes, final String URL, final String USERNAME, final String PASSWORD) throws IOException {
        final File persistence = new File("src/main/resources/persistence.xml");

        final List<String> class_ = classes.stream().map(x -> "<class>" + x + "</class>\n").collect(Collectors.toList());
        String xmlString = FileUtils.readFileToString(persistence, "UTF-8");
        xmlString = xmlString.replace("$class", class_.toString().replace(",", "").replace("[", "").replace("]", ""));
        xmlString = xmlString.replace("$dialect", "org.hibernate.dialect.PostgreSQLDialect");
        xmlString = xmlString.replace("$driver", Database.DRIVER);
        xmlString = xmlString.replace("$username", USERNAME);
        xmlString = xmlString.replace("$password", PASSWORD);
        xmlString = xmlString.replace("$url", URL);
        final File newYamlFile = new File(path + "persistence.xml");

        FileUtils.writeStringToFile(newYamlFile, xmlString, "UTF-8");

    }
}
