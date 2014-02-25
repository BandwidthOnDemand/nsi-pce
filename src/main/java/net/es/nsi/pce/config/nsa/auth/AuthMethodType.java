/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.nsa.auth;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AuthMethodType.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AuthMethodType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="NONE"/>
 *     &lt;enumeration value="BASIC"/>
 *     &lt;enumeration value="OAUTH2"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "AuthMethodType")
@XmlEnum
public enum AuthMethodType {

    NONE("NONE"),
    BASIC("BASIC"),
    @XmlEnumValue("OAUTH2")
    OAUTH_2("OAUTH2");
    private final String value;

    AuthMethodType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AuthMethodType fromValue(String v) {
        for (AuthMethodType c: AuthMethodType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}