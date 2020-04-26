package com.kalosoftware.gen20;

import com.kalosoftware.gen20.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Carlos Cercado
 * @email cercadocarlos@gmail.com
 */
public class Class {

    private String name;
    private List<Props> props;

    public Class() {
    }

    public Class(String name, List<Props> props) {
        this.name = name;
        this.props = props;
    }

    public Class(final Table table) {
        this.name = Utils.toCamelCase(table.getName(), true);
        this.props = new ArrayList();
        table.getColumns().stream().forEach(col -> {
            final Props prop = new Props(Utils.toCamelCase(col.getName(), false), Utils.getType(col.getType()), col.isIsFk(), col.isIsNull());
            if(col.isIsFk()){
                prop.setClassFk(Utils.toCamelCase(col.getTableFk(), true));
            }
            props.add(prop);
        });
    }

    public static List<Class> getList(final List<Table> tables) {
        final List<Class> list = new ArrayList();
        for (Table table : tables) {
            final Class class_ = new Class(table);
            list.add(class_);
        }
        return list;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Props> getProps() {
        return props;
    }

    public void setProps(List<Props> props) {
        this.props = props;
    }

}
