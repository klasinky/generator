package com.kalosoftware.gen20;

import com.kalosoftware.gen20.utils.Utils;
import com.kalosoftware.gen20.constants.Constants;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.swing.JProgressBar;

/**
 *
 * @author Carlos Cercado
 * @email cercadocarlos@gmail.com
 */
public class Generator {

    private final static Logger LOGGER = Logger.getLogger(Generator.class.getName());

    private Connection con;
    private DatabaseMetaData meta;
    private String schema = "";
    private String path = "";
    private String groupId = "com.kalosoftware";
    private List<Class> classes;
    private String artifactId = "";
    private String PATH = "";

    public Generator(final String pathProject, final Connection con) throws SQLException {
        this.con = con;
        meta = con.getMetaData();
        schema = "public";
        this.path = pathProject;
    }

    public Generator(final String PATH, final String pathProject, final String groupId, final String artifactId, final Connection con) throws SQLException {
        this.con = con;
        meta = con.getMetaData();
        schema = "public";
        this.path = pathProject;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.PATH = PATH;
    }

    public void start(final String URL, final String USERNAME, final String PASSWORD, final JProgressBar bar) throws SQLException, IOException {
        final List<Table> tables = new ArrayList();

        final List<String> tablesFromDb = this.getTables();
        final int sum = ((30 / tablesFromDb.size()) == 0) ? 1 : (int) (30 / tablesFromDb.size());
        for (String table : tablesFromDb) {
            final List<Column> columns = this.getColumns(table);
            final Table tableFromDb = new Table(table, columns);

            if (bar.getValue() + sum < 70) {
                bar.setValue(bar.getValue() + sum);
            }
            tables.add(tableFromDb);
        }

        bar.setValue(70);

        this.generateModel(tables);

        bar.setValue(75);

        this.classes = Class.getList(tables);

        bar.setValue(80);

        this.generateDAO(classes);

        bar.setValue(82);

        this.generateDTO(classes);

        bar.setValue(88);

        this.generateServices(classes);

        bar.setValue(90);

        this.generateTestCases(classes);

        bar.setValue(95);

        final List<String> classStr = classes.stream().map(class_ -> this.groupId + ".models." + class_.getName()).collect(Collectors.toList());
        Project.generatePersitence(PATH + "\\" + this.artifactId + "\\src\\main\\resources\\META-INF\\", classStr, URL, USERNAME, PASSWORD);

        bar.setValue(100);
    }

    private void generateModel(final List<Table> tables) throws IOException {
        LOGGER.info("Generando el modelo...");
        for (Table table : tables) {
            final List<FieldSpec> props = new ArrayList();
            final List<MethodSpec> methods = new ArrayList();

            final ClassName entity = ClassName.get("javax.persistence", "Entity");

            final AnnotationSpec tableA = AnnotationSpec
                    .builder(ClassName.get("javax.persistence", "Table"))
                    .addMember("name", "$S", table.getName())
                    .build();

            final AnnotationSpec sequenceGenerator = AnnotationSpec
                    .builder(ClassName.get("javax.persistence", "SequenceGenerator"))
                    .addMember("name", "$S", table.getName() + "_id_seq")
                    .addMember("sequenceName", "$S", table.getName() + "_id_seq")
                    .build();

            final AnnotationSpec generatedValue = AnnotationSpec
                    .builder(ClassName.get("javax.persistence", "GeneratedValue"))
                    .addMember("strategy", "$T.AUTO", ClassName.get("javax.persistence", "GenerationType"))
                    .addMember("generator", "$S", table.getName() + "_id_seq")
                    .build();

            for (Column column : table.getColumns()) {

                String name = Utils.toCamelCase(column.getName(), false);
                if (Constants.RESERVED_WORD.contains(name)) {
                    LOGGER.warning(String.format("La propiedad '%s' de la tabla '%s' no puede ser usada porque es una palabra reservada. Se le agregara un '_' al final.", name, table.getName()));
                    name = name + "_";
                }

                final FieldSpec.Builder param;

                if (column.isIsFk()) {
                    name = Utils.toCamelCase(column.getTableFk(), false);
                    if (Constants.RESERVED_WORD.contains(name)) {
                        name = name + "_";
                    }
                    param = FieldSpec.builder(ClassName.get("", Utils.toCamelCase(column.getTableFk(), true)), name, Modifier.PRIVATE);
                    param.addAnnotation(ClassName.get("javax.persistence", "ManyToOne"));
                    final AnnotationSpec join = AnnotationSpec
                            .builder(ClassName.get("javax.persistence", "JoinColumn"))
                            .addMember("name", "$S", column.getName())
                            .addMember("referencedColumnName", "$S", "id")
                            .build();
                    param.addAnnotation(join);
                } else if (column.getName().equals("id")) {
                    param = FieldSpec.builder(Utils.getType(column.getType()), name, Modifier.PRIVATE);
                    param.addAnnotation(ClassName.get("javax.persistence", "Id"));
                    final AnnotationSpec col = AnnotationSpec
                            .builder(ClassName.get("javax.persistence", "Column"))
                            .addMember("name", "$S", column.getName())
                            .build();
                    param.addAnnotation(col);
                    param.addAnnotation(sequenceGenerator);
                    param.addAnnotation(generatedValue);
                } else {
                    param = FieldSpec.builder(Utils.getType(column.getType()), name, Modifier.PRIVATE);
                    final AnnotationSpec col = AnnotationSpec
                            .builder(ClassName.get("javax.persistence", "Column"))
                            .addMember("name", "$S", column.getName())
                            .build();
                    param.addAnnotation(col);
                }

                if (!column.isIsNull()) {
                    final AnnotationSpec col = AnnotationSpec
                            .builder(ClassName.get("javax.persistence", "Basic"))
                            .addMember("optional", "false")
                            .build();
                    param.addAnnotation(col);
                }

                props.add(param.build());
            }

            for (Column column : table.getColumns()) {
                methods.add(this.getGetter(column));
                methods.add(this.getSetter(column));
            }

            final TypeSpec model = TypeSpec.classBuilder(Utils.toCamelCase(table.getName(), true))
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(entity)
                    .addAnnotation(tableA)
                    .addSuperinterface(ClassName.get(Serializable.class))
                    .addFields(props)
                    .addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .build())
                    .addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(Long.class, "id", Modifier.FINAL)
                            .addStatement("this.id = id")
                            .build())
                    .addMethods(methods)
                    .build();

            final JavaFile javaFile = JavaFile.builder(this.groupId + ".models", model)
                    .build();

            javaFile.writeTo(new File(this.path + "\\src\\main\\java"));
        }

    }

    private void generateDAO(final List<Class> classes) throws IOException {
        LOGGER.info("Generando el DAO...");
        for (Class class_ : classes) {
            //final List<MethodSpec> methods = new ArrayList();

            final ClassName daoFacade = ClassName.get(this.groupId + ".dao", "DAOFacade");
            final ClassName model = ClassName.get(this.groupId + ".models", class_.getName());

            final TypeSpec dao = TypeSpec.classBuilder(class_.getName() + "DAO")
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(ParameterizedTypeName.get(daoFacade, model))
                    //.addMethods(methods)
                    .addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement("super($L.class)", model)
                            .build()
                    )
                    .build();

            final JavaFile javaFile = JavaFile.builder(this.groupId + ".dao", dao)
                    .build();

            javaFile.writeTo(new File(this.path + "\\src\\main\\java"));

        }

    }

    private void generateDTO(final List<Class> classes) throws IOException {
        LOGGER.info("Generando el DTO...");

        for (Class class_ : classes) {
            final List<FieldSpec> props = new ArrayList();
            final List<MethodSpec> methods = new ArrayList();

            for (Props prop : class_.getProps()) {
                final FieldSpec.Builder param;
                if (prop.isIsFk()) {
                    param = FieldSpec.builder(Long.class, "id_" + prop.getName(), Modifier.PRIVATE);
                } else {
                    String name = prop.getName();

                    if (Constants.RESERVED_WORD.contains(name)) {
                        name = name + "_";
                    }
                    param = FieldSpec.builder(prop.getType(), name, Modifier.PRIVATE);
                }

                if (!prop.isIsNull()) {
                    final AnnotationSpec notNull = AnnotationSpec
                            .builder(ClassName.get("javax.validation.constraints", "NotNull"))
                            .build();
                    param.addAnnotation(notNull);
                }

                props.add(param.build());
            }

            for (Props prop : class_.getProps()) {
                methods.add(this.getGetter(prop));
                methods.add(this.getSetter(prop));
            }

            final ClassName daoFacade = ClassName.get(this.groupId + ".services.dto", "DTOFacade");
            final ClassName dtoClass = ClassName.get("", Utils.toCamelCase(class_.getName(), true) + "DTO");

            final TypeSpec dto = TypeSpec.classBuilder(Utils.toCamelCase(class_.getName(), true) + "DTO")
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(ParameterizedTypeName.get(daoFacade, dtoClass))
                    .addFields(props)
                    .addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .build())
                    .addMethods(methods)
                    .build();

            final JavaFile javaFile = JavaFile.builder(this.groupId + ".services.dto", dto)
                    .build();

            javaFile.writeTo(new File(this.path + "\\src\\main\\java"));
        }

    }

    private void generateServices(final List<Class> classes) throws IOException {
        LOGGER.info("Generando los servicios REST...");
        for (Class class_ : classes) {
            final List<MethodSpec> methods = new ArrayList();
            //final List<FieldSpec> props = new ArrayList();

            final ClassName model = ClassName.get(this.groupId + ".models", class_.getName());

            final AnnotationSpec path = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "Path"))
                    .addMember("value", "$S", "/" + Utils.toCamelCase(class_.getName(), false) + "s")
                    .build();

            final AnnotationSpec tag = AnnotationSpec
                    .builder(ClassName.get("io.swagger.v3.oas.annotations.tags", "Tag"))
                    .addMember("name", "$S", "Recurso de " + class_.getName())
                    .build();

            final AnnotationSpec produces = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "Produces"))
                    .addMember("value", "$T.APPLICATION_JSON", ClassName.get("javax.ws.rs.core", "MediaType"))
                    .build();

            final AnnotationSpec consumes = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "Consumes"))
                    .addMember("value", "$T.APPLICATION_JSON", ClassName.get("javax.ws.rs.core", "MediaType"))
                    .build();

            final AnnotationSpec getAn = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "GET"))
                    .build();

            final ClassName response = ClassName.get("javax.ws.rs.core", "Response");

            final MethodSpec get = MethodSpec.methodBuilder("get")
                    .addAnnotation(getAn)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(response)
                    .addStatement("final $T<$T> list = dao.getAll()", ClassName.get("java.util", "List"), model)
                    .addStatement("return $T.ok(list).build()", response)
                    .build();

            methods.add(get);

            final AnnotationSpec getOneAnn = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "GET"))
                    .build();

            final AnnotationSpec pathOne = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "Path"))
                    .addMember("value", "$S", "/{id}")
                    .build();

            final ParameterSpec idParam = ParameterSpec.builder(Long.class, "id", Modifier.FINAL).addAnnotation(AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "PathParam"))
                    .addMember("value", "$S", "id")
                    .build()).build();

            final MethodSpec getOne = MethodSpec.methodBuilder("get")
                    .addAnnotation(getOneAnn)
                    .addAnnotation(pathOne)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(idParam)
                    .returns(response)
                    .addStatement("final $T $L = dao.getById(id)", model, Utils.toCamelCase(class_.getName(), false))
                    .addStatement("return $T.ok($L).build()", response, Utils.toCamelCase(class_.getName(), false))
                    .build();

            methods.add(getOne);

            final AnnotationSpec removeAnn = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "DELETE"))
                    .build();

            final MethodSpec remove = MethodSpec.methodBuilder("remove")
                    .addAnnotation(removeAnn)
                    .addAnnotation(pathOne)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(idParam)
                    .returns(response)
                    .addStatement("final $T $L = dao.getById(id)", model, Utils.toCamelCase(class_.getName(), false))
                    .addStatement("dao.delete($L)", Utils.toCamelCase(class_.getName(), false))
                    .addStatement("return $T.ok().build()", response)
                    .build();

            methods.add(remove);

            final AnnotationSpec postAnn = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "POST"))
                    .build();

            final ParameterSpec dto = ParameterSpec.builder(ClassName.get(this.groupId + ".services.dto", class_.getName() + "DTO"), "body", Modifier.FINAL)
                    .build();

            final MethodSpec.Builder createBuilder = MethodSpec.methodBuilder("create")
                    .addAnnotation(postAnn)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(dto)
                    .returns(response)
                    .addException(ClassName.get(this.groupId + ".exceptions", "ServicesException"))
                    .addStatement("body.validate()")
                    .addStatement("final $T $L = new $T()", model, Utils.toCamelCase(class_.getName(), false), model);

            class_.getProps().stream().forEach(prop -> {
                if (prop.isIsFk()) {
                    createBuilder.addStatement("$L.set$L(new $T(body.get$L()))", Utils.toCamelCase(class_.getName(), false), prop.getClassFk(), ClassName.get(this.groupId + ".models", prop.getClassFk()), "Id_" + prop.getName());
                }else if(prop.getName().toLowerCase().equals("id")){
                    //
                } else {
                    createBuilder.addStatement("$L.set$L(body.get$L())", Utils.toCamelCase(class_.getName(), false), Utils.toCamelCase(prop.getName(), true), Utils.toCamelCase(prop.getName(), true));
                }
            });

            createBuilder.addStatement("dao.create($L)", Utils.toCamelCase(class_.getName(), false));
            createBuilder.addStatement("return $T.ok($L).build()", response, Utils.toCamelCase(class_.getName(), false));

            methods.add(createBuilder.build());

            final AnnotationSpec putAnn = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "PUT"))
                    .build();

            final MethodSpec.Builder editBuilder = MethodSpec.methodBuilder("update")
                    .addAnnotation(putAnn)
                    .addAnnotation(pathOne)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(idParam)
                    .addParameter(dto)
                    .returns(response)
                    .addException(ClassName.get(this.groupId + ".exceptions", "ServicesException"))
                    .addStatement("body.validate()")
                    .addStatement("final $T $L = dao.getById(id)", model, Utils.toCamelCase(class_.getName(), false));

            class_.getProps().stream().forEach(prop -> {
                if (prop.isIsFk()) {
                    editBuilder.addStatement("$L.set$L(new $T(body.get$L()))", Utils.toCamelCase(class_.getName(), false), prop.getClassFk(), ClassName.get(this.groupId + ".models", prop.getClassFk()), "Id_" + prop.getName());
                } else {
                    editBuilder.addStatement("$L.set$L(body.get$L())", Utils.toCamelCase(class_.getName(), false), Utils.toCamelCase(prop.getName(), true), Utils.toCamelCase(prop.getName(), true));
                }
            });

            editBuilder.addStatement("dao.update($L)", Utils.toCamelCase(class_.getName(), false));
            editBuilder.addStatement("return $T.ok($L).build()", response, Utils.toCamelCase(class_.getName(), false));

            methods.add(editBuilder.build());

            /*class_.getProps().stream().filter(prop -> prop.isIsFk()).forEach(x -> {

            });*/
            final FieldSpec dao = FieldSpec.builder(ClassName.get(this.groupId + ".dao", class_.getName() + "DAO"), "dao", Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $T()", ClassName.get(this.groupId + ".dao", class_.getName() + "DAO")).build();

            final TypeSpec service = TypeSpec.classBuilder(class_.getName() + "Service")
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(ParameterizedTypeName.get(Serializable.class))
                    .addAnnotation(path)
                    .addAnnotation(tag)
                    .addAnnotation(produces)
                    .addAnnotation(consumes)
                    .addField(dao)
                    .addMethods(methods)
                    .build();

            final JavaFile javaFile = JavaFile.builder(this.groupId + ".services", service)
                    .build();

            javaFile.writeTo(new File(this.path + "\\src\\main\\java"));
        }
    }

    private void generateTestCases(final List<Class> classes) throws IOException {
        LOGGER.info("Generando los Casos de pruebas de servicios REST...");
        for (Class class_ : classes) {
            final List<MethodSpec> methods = new ArrayList();
            //final List<FieldSpec> props = new ArrayList();

            final AnnotationSpec path = AnnotationSpec
                    .builder(ClassName.get("javax.ws.rs", "Path"))
                    .addMember("value", "$S", "/" + Utils.toCamelCase(class_.getName(), false) + "s")
                    .build();

            final AnnotationSpec test = AnnotationSpec
                    .builder(ClassName.get("org.junit", "Test"))
                    .build();

            final ClassName model = ClassName.get(this.groupId + ".models", Utils.toCamelCase(class_.getName(), true));
            final ClassName dto = ClassName.get(this.groupId + ".services.dto", Utils.toCamelCase(class_.getName(), true) + "DTO");
            final ClassName service = ClassName.get(this.groupId + ".services", class_.getName() + "Service");
            final ClassName response = ClassName.get("javax.ws.rs.core", "Response");
            final ClassName application = ClassName.get("javax.ws.rs.core", "Application");
            final ClassName resourceConfig = ClassName.get("org.glassfish.jersey.server", "ResourceConfig");
            final ClassName genericType = ClassName.get("javax.ws.rs.core", "GenericType");
            final ClassName entity = ClassName.get("javax.ws.rs.client", "Entity");
            final ClassName assert_ = ClassName.get("org.junit", "Assert");

            final String endPoint = class_.getName().toLowerCase() + "s";

            final MethodSpec configure = MethodSpec.methodBuilder("configure")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(application)
                    .addStatement("return new $T($T.class)", resourceConfig, service)
                    .build();

            final MethodSpec testGetAll = MethodSpec.methodBuilder("testGetAll")
                    .addModifiers(Modifier.PUBLIC)
                    .addException(IOException.class)
                    .addAnnotation(test)
                    .addStatement("//final $T response = target($S).request().get()", response, endPoint)
                    .addStatement("//final $T<$T> res = response.readEntity(new $T<List<$T>>(){});", List.class, model, genericType, model)
                    .addStatement("//$T.assertEquals(response.getStatus(), 200)", assert_)
                    .build();

            final MethodSpec testGetOne = MethodSpec.methodBuilder("testGetOne")
                    .addModifiers(Modifier.PUBLIC)
                    .addException(IOException.class)
                    .addAnnotation(test)
                    .addStatement("//final $T model = new $T()", model, model)
                    .addStatement("//set data to model")
                    .addStatement("//this.manager.getTransaction().begin()")
                    .addStatement("//this.manager.persist(model)")
                    .addStatement("//this.manager.getTransaction().commit()")
                    .addStatement("//final Response response = target($S).property(\"id\", model.getId()).request().get()", class_.getName().toLowerCase() + "s")
                    .addStatement("//final $T obj = this.getEntity(response, $T.class)", model, model)
                    .addStatement("//assertEquals(response.getStatus(), 200)")
                    .addStatement("//assertEquals(model.getId(), obj.getId())")
                    .build();

            final MethodSpec testPost = MethodSpec.methodBuilder("testPost")
                    .addModifiers(Modifier.PUBLIC)
                    .addException(IOException.class)
                    .addAnnotation(test)
                    .addStatement("//final $T dto = new $T()", dto, dto)
                    .addStatement("//set data to dto")
                    .addStatement("//final Response response = target($S).request().post($T.json(dto))", endPoint, entity)
                    .addStatement("//final $T obj = this.getEntity(response, $T.class)", model, model)
                    .addStatement("//assertEquals(response.getStatus(), 200)")
                    .addStatement("//assertEquals(dto.getId(), obj.getId())")
                    .build();

            final MethodSpec testPut = MethodSpec.methodBuilder("testPut")
                    .addModifiers(Modifier.PUBLIC)
                    .addException(IOException.class)
                    .addAnnotation(test)
                    .addStatement("//final $T model = new $T()", model, model)
                    .addStatement("//set data to model")
                    .addStatement("//this.manager.getTransaction().begin()")
                    .addStatement("//this.manager.persist(model)")
                    .addStatement("//this.manager.getTransaction().commit()")
                    .addStatement("//final $T dto = new $T()", dto, dto)
                    .addStatement("//set data to dto")
                    .addStatement("//final Response response = target($S).property(\"id\", model.getId()).request().put(Entity.json(dto))", endPoint)
                    .addStatement("//final $T obj = this.getEntity(response, $T.class)", model, model)
                    .addStatement("//assertEquals(response.getStatus(), 200)")
                    .addStatement("//assertEquals(dto.getId(), obj.getId())")
                    .build();

            final MethodSpec testDelete = MethodSpec.methodBuilder("testDelete")
                    .addModifiers(Modifier.PUBLIC)
                    .addException(IOException.class)
                    .addAnnotation(test)
                    .addStatement("//final $T model = new $T()", model, model)
                    .addStatement("//set data to model")
                    .addStatement("//this.manager.getTransaction().begin()")
                    .addStatement("//this.manager.persist(model)")
                    .addStatement("//this.manager.getTransaction().commit()")
                    .addStatement("//final Response response = target($S).property(\"id\", persona.getId()).request().delete()", endPoint)
                    .addStatement("//assertEquals(response.getStatus(), 200)")
                    .build();

            final TypeSpec testCase = TypeSpec.classBuilder("Test" + class_.getName() + "Service")
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(ClassName.get("", "TestCase"))
                    .addMethod(configure)
                    .addMethod(testGetAll)
                    .addMethod(testGetOne)
                    .addMethod(testPost)
                    .addMethod(testPut)
                    .addMethod(testDelete)
                    .build();

            final JavaFile javaFile = JavaFile.builder("", testCase).addStaticImport(assert_, "assertEquals")
                    .build();

            javaFile.writeTo(new File(this.path + "\\src\\test\\java"));
        }
    }

    private MethodSpec getGetter(final Column column) {
        if (column.isIsFk()) {
            String name = Utils.toCamelCase(column.getTableFk(), false);
            String nameFk = Utils.toCamelCase(column.getTableFk(), true);

            final ClassName returnType = ClassName.get("", nameFk);

            if (Constants.RESERVED_WORD.contains(name)) {
                name = name + "_";
                nameFk = nameFk + "_";
            }
            return MethodSpec.methodBuilder("get" + nameFk)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnType)
                    .addStatement("return this.$L", name)
                    .build();
        } else {
            String name = Utils.toCamelCase(column.getName(), false);
            String nameProp = Utils.toCamelCase(column.getName(), true);

            if (Constants.RESERVED_WORD.contains(name)) {
                name = name + "_";
            }

            return MethodSpec.methodBuilder("get" + nameProp)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(Utils.getType(column.getType()))
                    .addStatement("return this.$L", name)
                    .build();
        }
    }

    private MethodSpec getSetter(final Column column) {

        if (column.isIsFk()) {
            String name = Utils.toCamelCase(column.getTableFk(), false);
            String nameFk = Utils.toCamelCase(column.getTableFk(), true);

            final ClassName table = ClassName.get("", nameFk);

            if (Constants.RESERVED_WORD.contains(name)) {
                name = name + "_";
                nameFk = nameFk + "_";
            }

            return MethodSpec.methodBuilder("set" + nameFk)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(table, name, Modifier.FINAL).build())
                    .addStatement("this.$L = $L", name, name)
                    .build();
        } else {
            String name = Utils.toCamelCase(column.getName(), false);
            String nameProp = Utils.toCamelCase(column.getName(), true);

            if (Constants.RESERVED_WORD.contains(name)) {
                name = name + "_";
            }
            return MethodSpec.methodBuilder("set" + nameProp)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(Utils.getType(column.getType()), name, Modifier.FINAL).build())
                    .addStatement("this.$L = $L", name, name)
                    .build();
        }

    }

    private MethodSpec getGetter(final Props prop) {
        if (prop.isIsFk()) {

            return MethodSpec.methodBuilder("get" + "Id_" + prop.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(Long.class)
                    .addStatement("return this.$L", "id_" + prop.getName())
                    .build();
        } else {
            String name = Utils.toCamelCase(prop.getName(), false);
            String nameProp = Utils.toCamelCase(prop.getName(), true);

            if (Constants.RESERVED_WORD.contains(name)) {
                name = name + "_";
                nameProp = nameProp + "_";
            }

            return MethodSpec.methodBuilder("get" + nameProp)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(prop.getType())
                    .addStatement("return this.$L", name)
                    .build();
        }
    }

    private MethodSpec getSetter(final Props prop) {

        if (prop.isIsFk()) {
            String name = "id_" + prop.getName();
            String nameFk = "Id_" + prop.getName();

            return MethodSpec.methodBuilder("set" + nameFk)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(Long.class, name, Modifier.FINAL).build())
                    .addStatement("this.$L = $L", name, name)
                    .build();
        } else {
            String name = prop.getName();
            String nameProp = Utils.toCamelCase(prop.getName(), true);

            if (Constants.RESERVED_WORD.contains(name)) {
                name = name + "_";
                nameProp = nameProp + "_";
            }
            return MethodSpec.methodBuilder("set" + nameProp)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(prop.getType(), name, Modifier.FINAL).build())
                    .addStatement("this.$L = $L", name, name)
                    .build();
        }

    }

    public List<String> getTables() throws SQLException {
        LOGGER.info("Buscando informacion de las tablas...");
        final ResultSet res = meta.getTables(null, schema, "%", new String[]{"TABLE"});
        final List<String> tables = new ArrayList();
        while (res.next()) {
            tables.add(res.getString("TABLE_NAME"));
        }
        return tables;
    }

    public List<Column> getColumns(final String table) throws SQLException {
        final ResultSet res = meta.getColumns(null, schema, table, null);
        final List<Column> attributes = new ArrayList();

        while (res.next()) {
            final boolean nulleable = (res.getInt("NULLABLE") == DatabaseMetaData.columnNullable) ? true : false;
            final Column column = new Column(res.getString("COLUMN_NAME"), res.getString("TYPE_NAME"), res.getInt("COLUMN_SIZE"), nulleable);
            attributes.add(column);
        }
        return this.getMetaColumns(table, attributes);
    }

    private List<Column> getMetaColumns(final String table, final List<Column> columns) throws SQLException {
        final ResultSet res = con.prepareStatement("SELECT kcu.COLUMN_NAME,\n"
                + "ccu.table_name AS FK_TABLE_NAME FROM information_schema.table_constraints AS \n"
                + "tc JOIN information_schema.key_column_usage AS kcu ON \n"
                + "tc.constraint_name = kcu.constraint_name AND tc.table_schema =\n"
                + "kcu.table_schema JOIN information_schema.constraint_column_usage \n"
                + "AS ccu ON ccu.constraint_name = tc.constraint_name AND \n"
                + "ccu.table_schema = tc.table_schema WHERE tc.constraint_type = \n"
                + "'FOREIGN KEY' AND tc.table_name='" + table + "';").executeQuery();

        while (res.next()) {
            final String COLUMN_NAME = res.getString("COLUMN_NAME");
            final String FK_TABLE_NAME = res.getString("FK_TABLE_NAME");
            if (columns.contains(new Column(COLUMN_NAME))) {
                Column column = columns.stream().filter(col -> col.equals(COLUMN_NAME)).findFirst().get();
                column.setIsFk(true);
                column.setTableFk(FK_TABLE_NAME);

                columns.remove(new Column(COLUMN_NAME));
                columns.add(column);
            }
        }
        return columns;
    }

    public List<Class> getClasses() {
        return classes;
    }

    public void setClasses(List<Class> classes) {
        this.classes = classes;
    }

    public Connection getCon() {
        return con;
    }

    public void setCon(Connection con) {
        this.con = con;
    }

}
