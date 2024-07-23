package org.uwdigi.who.l3.cqltesting.matcher;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.param.UriParam;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR4;

import java.util.ArrayList;
import java.util.List;

public class L3ResourceMatcherR4 extends ResourceMatcherR4 {

    @Override
    public boolean matches(String name, List<IQueryParameterType> params, IBaseResource resource) {
        boolean match;

        var context = getContext();
        var s = context.getResourceDefinition(resource).getSearchParam(name);
        if (s == null) {
            throw new RuntimeException(String.format(
                    "The SearchParameter %s for Resource %s is not supported.", name, resource.fhirType()));
        }

        var path = s.getPath();

        // System search parameters...
        if (path.isEmpty() && name.startsWith("_")) {
            path = name.substring(1);
        }

        List<IBase> pathResult;
        try {
            var parsed = getPathCache().computeIfAbsent(new SPPathKey(resource.fhirType(), path), p -> {
                try {
                    return getEngine().parse(p.path());
                } catch (Exception e) {
                    throw new RuntimeException(
                            String.format(
                                    "Parsing SearchParameter %s for Resource %s resulted in an error.",
                                    name, resource.fhirType()),
                            e);
                }
            });
            pathResult = getEngine().evaluate(resource, parsed, IBase.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "Evaluating SearchParameter %s for Resource %s resulted in an error.",
                            name, resource.fhirType()),
                    e);
        }

        if (pathResult == null || pathResult.isEmpty()) {
            return false;
        }

        for (IQueryParameterType param : params) {
            for (var r : pathResult) {
                if (param instanceof ReferenceParam) {
                    match = isMatchReference(param, r);
                } else if (param instanceof DateParam) {
                    match = isMatchDate((DateParam) param, r);
                } else if (param instanceof TokenParam) {
                    match = isMatchToken((TokenParam) param, r);
                    if (!match) {
                        match = isMatchCoding((TokenParam) param, r);
                    }
                } else if (param instanceof UriParam) {
                    match = isMatchUri((UriParam) param, r);
                } else if (param instanceof StringParam) {
                    match = isMatchString((StringParam) param, r);
                } else {
                    throw new NotImplementedException("Resource matching not implemented for search params of type "
                            + param.getClass().getSimpleName());
                }

                if (match) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isMatchCoding(TokenParam param, IBase pathResult) {
        if (param.getModifier() == TokenParamModifier.IN) {
            var valueSet = getValueSet(pathResult);
            if (valueSet == null || valueSet.isEmpty()) {
                return false;
            }

            return isMatchListOfCodes(param.getSystem(), param.getValue(), valueSet);
        }

        var codes = getCodes(pathResult);
        if (codes == null || codes.isEmpty()) {
            return false;
        }

        return isMatchListOfCodes(param.getSystem(), param.getValue(), codes);
    }

    protected boolean isMatchListOfCodes(String system, String value, List<TokenParam> codes) {
        for (var c : codes) {
            var matches = value.equals(c.getValue())
                    && (system == null || system.equals(c.getSystem()));
            if (matches) {
                return true;
            }
        }

        return false;
    }

    protected List<TokenParam> getValueSet(IBase pathResult) {
        var resolvedCodes = new ArrayList<TokenParam>();
        if (pathResult instanceof ValueSet) {
            if (!((ValueSet) pathResult).hasCompose()) {
                return resolvedCodes;
            }

            for (var include : ((ValueSet) pathResult).getCompose().getInclude()) {
                if (!(include.hasConcept())) {
                    continue;
                }

                String system = include.getSystem();

                for (var concept : include.getConcept()) {
                    resolvedCodes.add(new TokenParam(system, concept.getCode()));
                }
            }
        }

        return resolvedCodes;
    }
}
