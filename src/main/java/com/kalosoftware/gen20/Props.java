package com.kalosoftware.gen20;

import com.kalosoftware.gen20.utils.Utils;
import java.util.Objects;

/**
 *
 * @author Carlos Cercado
 * @email cercadocarlos@gmail.com
 */
public class Props {

    private String name;
    private java.lang.Class type;
    private boolean isFk;
    private String classFk;
    private boolean isNull;

    public Props() {
    }

    public Props(String name, java.lang.Class type, boolean isFk) {
        this.name = name;
        this.type = type;
        this.isFk = isFk;
    }
    
    public Props(String name, java.lang.Class type, boolean isFk,  boolean isNull) {
        this.name = name;
        this.type = type;
        this.isFk = isFk;
        this.isNull = isNull;
    }
    
    public Props(String name) {
        this.name = name;
    }
    
    public Props(Column column) {
        this.name = Utils.toCamelCase(column.getName(), false);
        this.type = Utils.getType(column.getType());
        this.isFk = column.isIsFk();
        if(this.isFk){
            this.classFk = Utils.toCamelCase(column.getTableFk(), true);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public java.lang.Class getType() {
        return type;
    }

    public void setType(java.lang.Class type) {
        this.type = type;
    }

    public boolean isIsFk() {
        return isFk;
    }

    public void setIsFk(boolean isFk) {
        this.isFk = isFk;
    }

    public String getClassFk() {
        return classFk;
    }

    public void setClassFk(String classFk) {
        this.classFk = classFk;
    }

    public boolean isIsNull() {
        return isNull;
    }

    public void setIsNull(boolean isNull) {
        this.isNull = isNull;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Props other = (Props) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
    
    public boolean equals(String name) {
       return this.name.equals(name);
       
    }
    
    
}
