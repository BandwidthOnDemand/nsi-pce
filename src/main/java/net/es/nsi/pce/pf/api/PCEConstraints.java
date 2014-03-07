/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api;

import java.util.HashSet;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.path.jaxb.ConstraintListType;
import net.es.nsi.pce.path.jaxb.ConstraintType;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.NumAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;

/**
 *
 * @author hacksaw
 */
public class PCEConstraints {
    // These are fixed element definitions within the findPath request.
    public static final String STARTTIME = "startTime";
    public static final String ENDTIME = "endTime";
    public static final String SERVICETYPE = "serviceType";

    /**
     * Build a routing constraint set from the standard API parameters.
     * 
     * @param startTime
     * @param endTime
     * @param serviceType
     * @param constraints
     * @return 
     */
    public static Set<Constraint> getConstraints(XMLGregorianCalendar startTime,
            XMLGregorianCalendar endTime, String serviceType,
            ConstraintListType constraints) {

        Set<Constraint> results = new HashSet<>();
        
        // Add the start time.
        if (startTime != null) {
            NumAttrConstraint start = new NumAttrConstraint();
            start.setAttrName(STARTTIME);
            start.setValue(startTime.toGregorianCalendar().getTimeInMillis());
            results.add(start);
        }
        
        // Add the end time.
        if (endTime != null) {
            NumAttrConstraint end = new NumAttrConstraint();
            end.setAttrName(ENDTIME);
            end.setValue(endTime.toGregorianCalendar().getTimeInMillis());
            results.add(end);
        }
        
        // Add the serviceType as a constriant.
        if (serviceType != null && !serviceType.isEmpty()) {
            StringAttrConstraint st = new StringAttrConstraint();
            st.setAttrName(SERVICETYPE);
            st.setValue(serviceType);
            results.add(st);
        }
        
        // Add the inclusion/exclusion contraints.
        if (constraints != null) {
            for (ConstraintType include : constraints.getInclude()) {
                StringAttrConstraint inc = new StringAttrConstraint();
                inc.setAttrName(include.getType());
                inc.setValue(include.getValue());
                results.add(inc);
            }
            for (ConstraintType exclude : constraints.getExclude()) {
                StringAttrConstraint ex = new StringAttrConstraint();
                ex.setAttrName(exclude.getType());
                ex.setValue(exclude.getValue());
                results.add(ex);                
            }
        }
        
        return results;
    }
}
