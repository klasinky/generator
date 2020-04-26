package com.kalosoftware.gen20.utils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Logger;

/**
 *
 * @author Carlos Cercado
 * @email cercadocarlos@gmail.com
 */
public class Utils {

    private final static Logger LOGGER = Logger.getLogger(Utils.class.getName());

    public static String toCamelCase(final String str, final boolean isTable) {
        final String[] parts = str.split("_");
        String name = "";
        for (int i = 0; i < parts.length; i++) {
            if (!isTable && i == 0) {
                name += parts[i].substring(0, 1).toLowerCase() + parts[i].substring(1);
            } else {
                name += parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1);
            }
        }
        return name;
    }

    public static java.lang.Class getType(final String type) {
        switch (type) {
            case "text":
                return String.class;
            case "varchar":
                return String.class;
            case "int4":
                return Integer.class;
            case "int8":
                return Integer.class;
            case "bigint":
                return Long.class;
            case "bigserial":
                return Long.class;
            case "integer":
                return Integer.class;
            case "serial":
                return Long.class;
            case "bool":
                return Boolean.class;
            case "numeric":
                return BigDecimal.class;
            case "timestamp":
                return Date.class;
            case "date":
                return Date.class;
            case "char":
                return String.class;
            case "float8":
                return Long.class;
            case "timestamptz":
                return Date.class;
            default:
                LOGGER.warning("Tipo " + type + " no reconocido. Sera convertido a Object");
                return Object.class;
        }
    }

}
