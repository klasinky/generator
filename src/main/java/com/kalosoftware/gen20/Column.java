package com.kalosoftware.gen20;

import java.util.Objects;

/**
 *
 * @author Carlos Cercado
 * @email cercadocarlos@gmail.com
 */
public class Column {

    private String name;
    private String type;
    private int size;
    private boolean isNull;
    private boolean isFk;
    private String tableFk;

    public Column() {
    }

    public Column(String name, String type, int size, boolean isNull) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.isNull = isNull;
    }
    
    public Column(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isIsNull() {
        return isNull;
    }

    public void setIsNull(boolean isNull) {
        this.isNull = isNull;
    }

    public boolean isIsFk() {
        return isFk;
    }

    public void setIsFk(boolean isFk) {
        this.isFk = isFk;
    }

    public String getTableFk() {
        return tableFk;
    }

    public void setTableFk(String tableFk) {
        this.tableFk = tableFk;
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
        final Column other = (Column) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
    
    public boolean equals(String name) {
       return this.name.equals(name);
       
    }
    
    
}
